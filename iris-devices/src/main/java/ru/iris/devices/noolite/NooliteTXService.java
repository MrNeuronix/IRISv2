package ru.iris.devices.noolite;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ailis.usb4java.libusb.Context;
import de.ailis.usb4java.libusb.DeviceHandle;
import de.ailis.usb4java.libusb.LibUsb;
import de.ailis.usb4java.libusb.LibUsbException;
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

public class NooliteTXService implements Runnable {

    private Logger log = LogManager.getLogger(NooliteTXService.class.getName());
    private boolean shutdown = false;
    private static final Map<String, NooliteDevice> nooDevices = new HashMap<>();
    private JsonMessaging messaging;
    private SQL sql = Service.getSQL();
    private Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().setPrettyPrinting().create();
    private final Context context = new Context();

    // Noolite PC USB TX HID
    static final int VENDOR_ID = 5824; //0x16c0;
    static final int PRODUCT_ID = 1503; //0x05df;

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

        // Initialize the libusb context
        int result = LibUsb.init(context);
        if (result < 0)
            try {
                throw new LibUsbException("Unable to initialize libusb", result);
            } catch (LibUsbException e) {
                e.printStackTrace();
            }

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
            LibUsb.exit(context);

        } catch (final Throwable t) {
            t.printStackTrace();
            log.error("Unexpected exception in NooliteTX", t);
        }
    }

    private void writeToHID(ByteBuffer command) throws IOException {

        DeviceHandle handle = LibUsb.openDeviceWithVidPid(context, VENDOR_ID, PRODUCT_ID);

        if(handle == null)
        {
            log.error("Noolite TX device not found!");
            shutdown = true;
            return;
        }

        if (LibUsb.kernelDriverActive(handle, 0) == 1)
            LibUsb.detachKernelDriver(handle, 0);

        int ret = LibUsb.setConfiguration(handle, 1);

        if (ret < 0)
        {
            log.error("Configuration error");
            LibUsb.close(handle);
            if (ret == LibUsb.ERROR_BUSY)
                log.error("Device busy");
            return;
        }

        LibUsb.claimInterface(handle, 0);

        //send
        LibUsb.controlTransfer(handle, LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE, 0x9, 0x300, 0, command, 8);

        LibUsb.attachKernelDriver(handle, 0);
        LibUsb.close(handle);
    }


}
