package ru.iris.restful;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.GetInventoryAdvertisement;
import ru.iris.common.messaging.model.SetDeviceLevelAdvertisement;
import ru.iris.common.messaging.model.zwave.ResponseZWaveDeviceArrayInventoryAdvertisement;
import ru.iris.common.messaging.model.zwave.ResponseZWaveDeviceInventoryAdvertisement;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 12.11.13
 * Time: 10:14
 * License: GPL v3
 */

@Path("/device")
public class DevicesREST {

    private Logger log = LoggerFactory.getLogger(DevicesREST.class.getName());
    private I18N i18n = new I18N();
    private final UUID InstanceId = UUID.randomUUID();
    private JsonMessaging messaging;
    private SetDeviceLevelAdvertisement setDeviceLevelAdvertisement = new SetDeviceLevelAdvertisement();
    private GetInventoryAdvertisement getInventoryAdvertisement = new GetInventoryAdvertisement();

    @GET
    @Path("/{uuid}")
    @Produces(MediaType.TEXT_PLAIN + ";charset=utf-8")
    public String device(@PathParam("uuid") String uuid) {
        log.info(i18n.message("rest.get.device.get.0", uuid));
        return getDevices(uuid);
    }

    @GET
    @Path("/{uuid}/{label}/{level}")
    @Consumes(MediaType.TEXT_PLAIN + ";charset=utf-8")
    public String devSetLevel(@PathParam("uuid") String uuid, @PathParam("label") String label, @PathParam("level") String level) {
        log.info(i18n.message("rest.set.level.0.on.1.device", level, uuid));
        return "{ status: " + sendLevelMessage(uuid, label, level) + " }";
    }

    private String sendLevelMessage(String uuid, String label, String value) {
        try {

            messaging = new JsonMessaging(InstanceId);
            messaging.broadcast("event.devices.setvalue", setDeviceLevelAdvertisement.set(uuid, label, value));
            messaging.close();

            return i18n.message("done");

        } catch (final Throwable t) {
            log.error("Unexpected exception in DevicesREST", t);
            messaging.close();
            return "{ \"error\": \"Something goes wrong: " + t.toString() + "\" }";
        }
    }

    private String getDevices(String uuid) {

        try {
            messaging = new JsonMessaging(InstanceId);

            messaging.subscribe("event.devices.responseinventory");
            messaging.start();

            messaging.broadcast("event.devices.getinventory", getInventoryAdvertisement.set(uuid));

            final JsonEnvelope envelope = messaging.receive(5000);
            if (envelope != null) {
                if (envelope.getObject() instanceof ResponseZWaveDeviceArrayInventoryAdvertisement) {
                    messaging.close();
                    return ((ResponseZWaveDeviceArrayInventoryAdvertisement) envelope.getObject()).getDevices().toString();
                } else if (envelope.getObject() instanceof ResponseZWaveDeviceInventoryAdvertisement) {
                    messaging.close();
                    return ((ResponseZWaveDeviceInventoryAdvertisement) envelope.getObject()).getDevice().toString();
                } else {
                    log.info("Unknown response! " + envelope.getObject());
                    messaging.close();
                    return "{ \"error\": \"Unknown response! Class: " + envelope.getObject().getClass() + " Response: " + envelope.getObject() + "\" }";
                }
            }

        } catch (final Throwable t) {
            log.error("Unexpected exception in DevicesREST", t);
            messaging.close();
            return "{ \"error\": \"" + t.toString() + "\" }";
        }

        messaging.close();
        return "{ \"error\": \"no answer\" }";
    }
}
