package ru.iris.devices.noolite;

import de.ailis.usb4java.libusb.Context;
import de.ailis.usb4java.libusb.DeviceHandle;
import de.ailis.usb4java.libusb.LibUsb;
import de.ailis.usb4java.libusb.LibUsbException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.SQL;
import ru.iris.common.devices.noolite.NooliteDevice;
import ru.iris.common.devices.noolite.NooliteDeviceValue;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.ServiceCheckEmitter;
import ru.iris.common.messaging.model.devices.noolite.*;
import ru.iris.common.messaging.model.service.ServiceStatus;
import ru.iris.devices.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
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

public class NooliteRXService implements Runnable {

    private Logger log = LogManager.getLogger(NooliteRXService.class.getName());
    private boolean shutdown = false;
    private static final Map<String, NooliteDevice> nooDevices = new HashMap<>();
    private JsonMessaging messaging;
    private SQL sql = Service.getSQL();
    protected final Context context = new Context();
    protected DeviceHandle handle;
    protected boolean pause = false;
    private ServiceCheckEmitter serviceCheckEmitter;

    private static final long READ_UPDATE_DELAY_MS = 500L;

    // Noolite PC USB RX HID
    static final int VENDOR_ID = 5824; // 0x16c0;
    static final int PRODUCT_ID = 1500; // 0x05dc;

    public NooliteRXService() {

        serviceCheckEmitter = new ServiceCheckEmitter("Devices-NooliteRX");
        serviceCheckEmitter.setState(ServiceStatus.STARTUP);

        Thread t = new Thread(this);
        t.start();
    }

    @Override
    public synchronized void run() {

        // Make sure we exit the wait loop if we receive shutdown signal.
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                shutdown = true;
            }
        }));

        messaging = new JsonMessaging(UUID.randomUUID());

        ResultSet rs = sql.select("SELECT uuid, internalname FROM devices WHERE source='noolite'");

        try {
            while (rs.next()) {

                log.info("Loading device " + rs.getString("internalname") + " from database");
                nooDevices.put(rs.getString("internalname"), new NooliteDevice().load(rs.getString("uuid")));
            }

            rs.close();

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }

        serviceCheckEmitter.setState(ServiceStatus.AVAILABLE);

        ///////////////////////////////////////////////////////////////

        // Initialize the libusb context
        int result = LibUsb.init(context);
        if (result < 0)
            try {
                throw new LibUsbException("Unable to initialize libusb", result);
            } catch (LibUsbException e) {
                e.printStackTrace();
            }

        handle = LibUsb.openDeviceWithVidPid(context, VENDOR_ID, PRODUCT_ID);

        if (handle == null) {
            log.error("Noolite TX device not found!");
            shutdown = true;
            return;
        }

        if (LibUsb.kernelDriverActive(handle, 0) == 1)
            LibUsb.detachKernelDriver(handle, 0);

        int ret = LibUsb.setConfiguration(handle, 1);

        if (ret < 0) {
            log.error("Configuration error");
            LibUsb.close(handle);
            if (ret == LibUsb.ERROR_BUSY)
                log.error("Device busy");
            return;
        }

        LibUsb.claimInterface(handle, 0);

        ByteBuffer tmpBuf = ByteBuffer.allocateDirect(8);

        new InternalCommands();

        while (!shutdown) {
            // receiving area
            ByteBuffer buf = ByteBuffer.allocateDirect(8);
            if (!pause)
                LibUsb.controlTransfer(handle, LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE | LibUsb.ENDPOINT_IN, 0x9, 0x300, 0, buf, 100);

            if (!buf.equals(tmpBuf)) {
                log.debug("RX Buffer: " + buf.get(0) + " " + buf.get(1) + " " + buf.get(2) + " " + buf.get(3) + " " + buf.get(4) + " " + buf.get(5) + " " + buf.get(6)
                        + " " + buf.get(7));

                Integer channel = (buf.get(1) + 1);
                byte action = buf.get(2);
                Integer dimmerValue = (int) buf.get(4);

                NooliteDevice device = null;

                try {
                    device = new NooliteDevice().loadByChannel(channel);
                } catch (IOException | SQLException e) {
                    e.printStackTrace();
                }

                if (device == null) {
                    try {
                        device = new NooliteDevice();
                        device.setInternalName("noolite/channel/" + channel);
                        device.setStatus("listening");
                        device.setType("Generic Noolite Device");
                        device.setManufName("Nootechnika");
                        device.setUUID(UUID.randomUUID().toString());
                        device.updateValue(new NooliteDeviceValue("channel", channel.toString(), "", "", true));
                        device.updateValue(new NooliteDeviceValue("type", "generic", "", "", false));

                        nooDevices.put("noolite/channel/" + channel, device);
                    } catch (IOException | SQLException e) {
                        e.printStackTrace();
                    }
                }

                // turn off
                if (action == 0) {

                    log.info("Channel " + channel + ": Got OFF command");

                    device.updateValue(new NooliteDeviceValue("Level", "0", "", "", false));
                    messaging.broadcast("event.devices.noolite.value.set", new NooliteDeviceLevelSetAdvertisement().set(device.getUUID(), "Level", "0"));
                    try {
                        device.save();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                // dim
                else if (action == 1) {

                    log.info("Channel " + channel + ": Got DIM command");
                    messaging.broadcast("event.devices.noolite.value.set", new NooliteDeviceLevelDimAdvertisement().set(device.getUUID()));
                }
                // turn on
                else if (action == 2) {
                    log.info("Channel " + channel + ": Got ON command");
                    device.updateValue(new NooliteDeviceValue("Level", "100", "", "", false));
                    messaging.broadcast("event.devices.noolite.value.set", new NooliteDeviceLevelSetAdvertisement().set(device.getUUID(), "Level", "100"));
                    try {
                        device.save();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                // bright
                else if (action == 3) {
                    log.info("Channel " + channel + ": Got BRIGHT command");
                    messaging.broadcast("event.devices.noolite.value.set", new NooliteDeviceLevelBrightAdvertisement().set(device.getUUID()));
                }
                // set level
                else if (action == 6) {
                    log.info("Channel " + channel + ": Got SETLEVEL command.");
                    device.updateValue(new NooliteDeviceValue("Level", dimmerValue.toString(), "", "", false));
                    messaging.broadcast("event.devices.noolite.value.set", new NooliteDeviceLevelSetAdvertisement().set(device.getUUID(), "Level", dimmerValue.toString()));
                    try {
                        device.save();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                // stop dim/bright
                else if (action == 10) {
                    log.info("Channel " + channel + ": Got STOPDIMBRIGHT command.");
                    messaging.broadcast("event.devices.noolite.value.set", new NooliteDeviceLevelStopDimBrightAdvertisement().set(device.getUUID()));
                }

                tmpBuf = buf;
            }

            try {
                Thread.sleep(READ_UPDATE_DELAY_MS);
            } catch (InterruptedException e) {
                //Ignore
                e.printStackTrace();
            }
        }

        LibUsb.attachKernelDriver(handle, 0);
        LibUsb.close(handle);
        LibUsb.exit(context);

        // Broadcast that this service is shutdown.
        serviceCheckEmitter.setState(ServiceStatus.SHUTDOWN);

    }

    ///
    ///  For intenal commands
    ///

    private class InternalCommands implements Runnable {
        private ServiceCheckEmitter serviceCheckEmitter;

        public InternalCommands() {

            serviceCheckEmitter = new ServiceCheckEmitter("Devices-NooliteRX-Internal");
            serviceCheckEmitter.setState(ServiceStatus.STARTUP);

            Thread t = new Thread(this);
            t.start();
        }

        @Override
        public synchronized void run() {

            serviceCheckEmitter.setState(ServiceStatus.AVAILABLE);

            try {
                JsonMessaging jsonMessaging = new JsonMessaging(UUID.randomUUID());

                jsonMessaging.subscribe("event.devices.noolite.rx.bindchannel");
                jsonMessaging.subscribe("event.devices.noolite.rx.unbindchannel");
                jsonMessaging.subscribe("event.devices.noolite.rx.unbindallchannel");

                jsonMessaging.start();

                while (!shutdown) {

                    // Lets wait for 100 ms on json messages and if nothing comes then proceed to carry out other tasks.
                    final JsonEnvelope envelope = jsonMessaging.receive(100);
                    if (envelope != null) {
                        if (envelope.getObject() instanceof BindRXChannelAdvertisment) {

                            log.debug("Get BindRXChannel advertisement");

                            final BindRXChannelAdvertisment advertisement = envelope.getObject();
                            NooliteDevice device = (NooliteDevice) getDeviceByUUID(advertisement.getDeviceUUID());
                            int channel = Integer.valueOf(device.getValue("channel").getValue());

                            ByteBuffer buf = ByteBuffer.allocateDirect(8);
                            buf.put((byte) 1);
                            buf.put((byte) channel);

                            pause = true;
                            LibUsb.controlTransfer(handle, LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE | LibUsb.ENDPOINT_IN, 0x9, 0x300, 0, buf, 100);
                            pause = false;

                        } else if (envelope.getObject() instanceof UnbindRXChannelAdvertisment) {

                            log.debug("Get UnbindRXChannel advertisement");

                            final UnbindRXChannelAdvertisment advertisement = envelope.getObject();
                            NooliteDevice device = (NooliteDevice) getDeviceByUUID(advertisement.getDeviceUUID());
                            int channel = Integer.valueOf(device.getValue("channel").getValue());

                            ByteBuffer buf = ByteBuffer.allocateDirect(8);
                            buf.put((byte) 3);
                            buf.put((byte) channel);

                            pause = true;
                            LibUsb.controlTransfer(handle, LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE | LibUsb.ENDPOINT_IN, 0x9, 0x300, 0, buf, 100);
                            pause = false;

                        } else if (envelope.getObject() instanceof UnbindAllRXChannelAdvertisment) {

                            log.debug("Get UnbindAllRXChannel advertisement");

                            ByteBuffer buf = ByteBuffer.allocateDirect(8);
                            buf.put((byte) 4);

                            pause = true;
                            LibUsb.controlTransfer(handle, LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE | LibUsb.ENDPOINT_IN, 0x9, 0x300, 0, buf, 100);
                            pause = false;

                        } else if (envelope.getReceiverInstance() == null) {
                            // We received unknown broadcast message. Lets make generic log entry.
                            log.info("Received broadcast "
                                    + " from " + envelope.getSenderInstance()
                                    + " to " + envelope.getReceiverInstance()
                                    + " at '" + envelope.getSubject()
                                    + ": " + envelope.getObject());
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

                // Broadcast that this service is shutdown.
                serviceCheckEmitter.setState(ServiceStatus.SHUTDOWN);

                // Close JSON messaging.
                jsonMessaging.close();
                messaging.close();

            } catch (final Throwable t) {
                t.printStackTrace();
                log.error("Unexpected exception in NooliteRX-Internal", t);
            }

        }

        private Object getDeviceByUUID(String uuid) {
            ResultSet rs = sql.select("SELECT * FROM devices WHERE uuid='" + uuid + "'");

            try {
                while (rs.next()) {

                    if (rs.getString("source").equals("noolite")) {
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
}
