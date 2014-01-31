package ru.iris.devices;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.Config;
import ru.iris.common.SQL;
import ru.iris.common.devices.noolite.NooliteDevice;
import ru.iris.common.devices.zwave.ZWaveDevice;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.ServiceCheckEmitter;
import ru.iris.common.messaging.model.devices.*;
import ru.iris.common.messaging.model.devices.noolite.ResponseNooliteDeviceInventoryAdvertisement;
import ru.iris.common.messaging.model.devices.zwave.ResponseZWaveDeviceInventoryAdvertisement;
import ru.iris.common.messaging.model.service.ServiceStatus;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 05.01.14
 * Time: 13:57
 * License: GPL v3
 */

public class CommonDeviceService implements Runnable {

    private Logger log = LogManager.getLogger(CommonDeviceService.class.getName());
    private boolean shutdown = false;
    private JsonMessaging messaging;
    private Map<String, String> config;
    private SQL sql = Service.getSQL();


    public CommonDeviceService() {
        Thread t = new Thread(this);
        t.setName("Common Device Service");
        t.start();
    }

    @Override
    public synchronized void run() {

        messaging = new JsonMessaging(UUID.randomUUID());
        config = new Config().getConfig();

        try {

            final ServiceCheckEmitter serviceCheckEmitter = new ServiceCheckEmitter("Devices-Common");
            serviceCheckEmitter.setState(ServiceStatus.STARTUP);


            // Make sure we exit the wait loop if we receive shutdown signal.
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    serviceCheckEmitter.setState(ServiceStatus.SHUTDOWN);
                    shutdown = true;
                }
            }));

            JsonMessaging jsonMessaging = new JsonMessaging(UUID.randomUUID());

            jsonMessaging.subscribe("event.devices.setvalue");
            jsonMessaging.subscribe("event.devices.getinventory");
            jsonMessaging.subscribe("event.devices.setname");
            jsonMessaging.subscribe("event.devices.setzone");

            jsonMessaging.start();

            serviceCheckEmitter.setState(ServiceStatus.AVAILABLE);

            while (!shutdown) {

                // Lets wait for 100 ms on json messages and if nothing comes then proceed to carry out other tasks.
                final JsonEnvelope envelope = jsonMessaging.receive(100);
                if (envelope != null) {


                    ////////////////////////////////////////////
                    //// Setting level to device            ////
                    ////////////////////////////////////////////

                    if (envelope.getObject() instanceof SetDeviceLevelAdvertisement) {

                        // We know of service advertisement
                        final SetDeviceLevelAdvertisement advertisement = envelope.getObject();

                        String uuid = advertisement.getDeviceUUID();
                        String label = advertisement.getLabel();
                        String level = advertisement.getValue();

                        Object device = getDeviceByUUID(uuid);

                        if (device == null) {
                            log.info("Cant find device with UUID " + uuid);
                            continue;
                        }

                        if (device instanceof ZWaveDevice) {
                            jsonMessaging.broadcast("event.devices.zwave.setvalue", new SetDeviceLevelAdvertisement().set(uuid, label, level));
                        } else if (device instanceof NooliteDevice) {
                            jsonMessaging.broadcast("event.devices.noolite.setvalue", new SetDeviceLevelAdvertisement().set(uuid, label, level));
                        }

                        ////////////////////////////////////////////
                        //// Get inventory                      ////
                        ////////////////////////////////////////////

                    } else if (envelope.getObject() instanceof GetInventoryAdvertisement) {

                        final GetInventoryAdvertisement advertisement = envelope.getObject();

                        String uuid = advertisement.getDeviceUUID();

                        // let send all devices (full inventory)
                        if (uuid.equals("all")) {

                            Map<String, Object> devices = new HashMap<>();

                            ResultSet rs = sql.select("SELECT * FROM devices");

                            try {
                                while (rs.next()) {

                                    // if device has zwave source
                                    if (rs.getString("source").equals("zwave")) {
                                        devices.put(rs.getString("internalname"), new ZWaveDevice().load(rs.getString("uuid")));
                                    }
                                    // if device has noolite source
                                    else if (rs.getString("source").equals("noolite")) {
                                        devices.put(rs.getString("internalname"), new NooliteDevice().load(rs.getString("uuid")));
                                    }
                                    // generic device
                                    else {
                                        log.error("Unknown device!");
                                    }
                                }

                                rs.close();

                            } catch (SQLException | IOException e) {
                                e.printStackTrace();
                            }

                            jsonMessaging.broadcast("event.devices.responseinventory", new ResponseDeviceInventoryAdvertisement().set(devices));

                            // send one device specified by UUID
                        } else {

                            Object device = getDeviceByUUID(uuid);

                            if (device == null) {
                                log.info("Cant find device with UUID " + uuid);
                                continue;
                            }

                            if (device instanceof ZWaveDevice) {
                                jsonMessaging.broadcast("event.devices.responseinventory", new ResponseZWaveDeviceInventoryAdvertisement().set((ZWaveDevice) device));
                            } else if (device instanceof NooliteDevice) {
                                jsonMessaging.broadcast("event.devices.responseinventory", new ResponseNooliteDeviceInventoryAdvertisement().set((NooliteDevice) device));
                            }
                        }

                        ////////////////////////////////////////////
                        //// Set device name                    ////
                        ////////////////////////////////////////////

                    } else if (envelope.getObject() instanceof SetDeviceNameAdvertisement) {

                        SetDeviceNameAdvertisement advertisement = envelope.getObject();

                        String uuid = advertisement.getDeviceUUID();
                        Object device = getDeviceByUUID(uuid);

                        if (device == null) {
                            log.info("Cant find device with UUID " + uuid);
                            continue;
                        }

                        log.info("Setting name \"" + advertisement.getName() + "\" to device " + uuid);

                        if (device instanceof ZWaveDevice) {
                            ZWaveDevice zWaveDevice = (ZWaveDevice) device;
                            zWaveDevice.setName(advertisement.getName());
                            zWaveDevice.save();
                        } else if (device instanceof NooliteDevice) {
                            NooliteDevice nooliteDevice = (NooliteDevice) device;
                            nooliteDevice.setName(advertisement.getName());
                            nooliteDevice.save();
                        }


                        ////////////////////////////////////////////
                        //// Set device zone                    ////
                        ////////////////////////////////////////////

                    } else if (envelope.getObject() instanceof SetDeviceZoneAdvertisement) {

                        SetDeviceZoneAdvertisement advertisement = envelope.getObject();

                        String uuid = advertisement.getDeviceUUID();
                        Object device = getDeviceByUUID(uuid);

                        if (device == null) {
                            log.info("Cant find device with UUID " + uuid);
                            continue;
                        }

                        log.info("Setting zone " + advertisement.getZone() + " to device " + uuid);

                        if (device instanceof ZWaveDevice) {
                            ZWaveDevice zWaveDevice = (ZWaveDevice) device;
                            zWaveDevice.setZone(advertisement.getZone());
                            zWaveDevice.save();
                        } else if (device instanceof NooliteDevice) {
                            NooliteDevice nooliteDevice = (NooliteDevice) device;
                            nooliteDevice.setZone(advertisement.getZone());
                            nooliteDevice.save();
                        }

                        ////////////////////////////////////////////
                        //// Unknown broadcast                  ////
                        ////////////////////////////////////////////

                    } else if (envelope.getReceiverInstance() == null) {
                        // We received unknown broadcast message. Lets make generic log entry.
                        log.info("Received broadcast "
                                + " from " + envelope.getSenderInstance()
                                + " to " + envelope.getReceiverInstance()
                                + " at '" + envelope.getSubject()
                                + ": " + envelope.getObject());

                        ////////////////////////////////////////////
                        //// Unknown request                    ////
                        ////////////////////////////////////////////

                    } else {
                        // We received unknown request message. Lets make generic log entry.
                        log.info("Received request "
                                + " from " + envelope.getSenderInstance()
                                + " to " + envelope.getReceiverInstance()
                                + " at '" + envelope.getSubject()
                                + ": " + envelope.getObject());
                    }
                }
            }

            jsonMessaging.close();
            serviceCheckEmitter.setState(ServiceStatus.SHUTDOWN);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //////////////////////////////////////////////////

    private Object getDeviceByUUID(String uuid) {
        ResultSet rs = sql.select("SELECT * FROM devices WHERE uuid='" + uuid + "'");

        try {
            while (rs.next()) {

                // if device has zwave source
                if (rs.getString("source").equals("zwave")) {
                    return new ZWaveDevice().load(uuid);
                }
                // if device has noolite source
                else if (rs.getString("source").equals("noolite")) {
                    return new NooliteDevice().load(uuid);
                }
                // generic device
                else {
                    log.error("Unknown device!");
                    return null;
                }

            }

            rs.close();

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
