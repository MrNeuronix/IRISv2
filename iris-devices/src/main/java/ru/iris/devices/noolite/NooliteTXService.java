package ru.iris.devices.noolite;

import com.codeminders.hidapi.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.Config;
import ru.iris.common.SQL;
import ru.iris.common.devices.noolite.NooliteDevice;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.devices.SetDeviceLevelAdvertisement;
import ru.iris.common.messaging.model.service.ServiceStatus;
import ru.iris.devices.Service;

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

public class NooliteTXService implements Runnable {

    private Logger log = LogManager.getLogger(NooliteTXService.class.getName());
    private boolean shutdown = false;
    private static final Map<String, NooliteDevice> nooDevices = new HashMap<>();
    private JsonMessaging messaging;
    private SQL sql = Service.getSQL();
    private Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().setPrettyPrinting().create();

    private static final long READ_UPDATE_DELAY_MS = 50L;

    static
    {
        System.loadLibrary("hidapi-jni");
    }

    // Noolite PC USB TX HID
    static final int VENDOR_ID = 0x16c0;
    static final int PRODUCT_ID = 0x05df;
    private static final int BUFSIZE = 2048;

    // byte array with command
    private byte[] command = {0x30,0x00,0x00,0x00,0x00,0x00,0x00,0x00};

    // Adverstiments


    public NooliteTXService() {
        Thread t = new Thread(this);
        t.start();
    }

    @Override
    public synchronized void run() {

        messaging = new JsonMessaging(UUID.randomUUID());
        Map<String, String> config = new Config().getConfig();

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

        Service.serviceChecker.setAdvertisment(Service.advertisement.set("Devices-NooliteTX", Service.serviceId, ServiceStatus.AVAILABLE));

        try {
            // Make sure we exit the wait loop if we receive shutdown signal.
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    shutdown = true;
                }
            }));

            JsonMessaging jsonMessaging = new JsonMessaging(UUID.randomUUID());

            jsonMessaging.subscribe("event.devices.noolite.setvalue");

            jsonMessaging.start();

            while (!shutdown) {

                // Lets wait for 100 ms on json messages and if nothing comes then proceed to carry out other tasks.
                final JsonEnvelope envelope = jsonMessaging.receive(100);
                if (envelope != null) {
                    if (envelope.getObject() instanceof SetDeviceLevelAdvertisement) {
                        // We know of service advertisement
                        final SetDeviceLevelAdvertisement advertisement = envelope.getObject();

                        String uuid = advertisement.getDeviceUUID();
                        String label = advertisement.getLabel();
                        String level = advertisement.getValue();

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
            Service.serviceChecker.setAdvertisment(Service.advertisement.set(
                    "Devices-NooliteTX", Service.serviceId, ServiceStatus.SHUTDOWN));

            // Close JSON messaging.
            jsonMessaging.close();
            messaging.close();

        } catch (final Throwable t) {
            t.printStackTrace();
            log.error("Unexpected exception in NooliteTX", t);
        }
    }

    private void writeToHID(byte[] command) throws IOException {

        HIDManager mgr = HIDManager.getInstance();
        HIDDevice dev = mgr.openById(VENDOR_ID, PRODUCT_ID, null);
        dev.disableBlocking();
        dev.write(command);
        dev.close();
        //mgr.release();
    }


}
