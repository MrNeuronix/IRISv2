package ru.iris.restful;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;
import ru.iris.common.devices.ZWaveDevice;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.GetInventoryAdvertisement;
import ru.iris.common.messaging.model.SetDeviceLevelAdvertisement;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
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

    private static Logger log = LoggerFactory.getLogger(DevicesREST.class.getName());
    private static I18N i18n = new I18N();
    private static Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().setPrettyPrinting().create();

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

            final JsonMessaging jsonMessaging = new JsonMessaging(UUID.randomUUID());
            jsonMessaging.broadcast("event.devices.setvalue", new SetDeviceLevelAdvertisement(uuid, label, value));
            jsonMessaging.close();

            return i18n.message("done");

        } catch (final Throwable t) {
            log.error("Unexpected exception in DevicesREST", t);
            return "{ \"error\": \"Something goes wrong: " + t.toString() + "\" }";
        }
    }

    private String getDevices(String uuid) {
        final UUID InstanceId = UUID.randomUUID();
        JsonMessaging messaging = null;

        try {

            messaging = new JsonMessaging(InstanceId);
            messaging.subscribe("event.devices.responseinventory");
            messaging.start();

            messaging.broadcast("event.devices.getinventory", new GetInventoryAdvertisement(uuid));

            // Lets wait for 5000 ms on json messages and if nothing comes then proceed to carry out other tasks.
            final JsonEnvelope envelope = messaging.receive(5000);
            if (envelope != null) {
                if (envelope.getObject() instanceof ZWaveDevice) {
                    return gson.toJson(envelope.getObject());
                } else if (envelope.getObject() instanceof HashMap) {
                    return gson.toJson(envelope.getObject());
                } else {
                    log.info("Unknown response! " + envelope.getObject());
                }
            }


        } catch (final Throwable t) {
            log.error("Unexpected exception in DevicesREST", t);
            return "{ \"error\": \"" + t.toString() + "\" }";
        } finally {
            messaging.close();
        }

        return "{ \"error\": \"no answer\" }";
    }
}
