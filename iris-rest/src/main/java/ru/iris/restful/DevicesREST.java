package ru.iris.restful;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;
import ru.iris.common.devices.ZWaveDevice;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.SetDeviceLevelAdvertisement;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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

    private static Logger log = LoggerFactory.getLogger(CommonREST.class.getName());
    private static I18N i18n = new I18N();
    private static Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().setPrettyPrinting().create();

    @GET
    @Path("/get/{uuid}")
    @Produces(MediaType.TEXT_PLAIN + ";charset=utf-8")
    public String device(@PathParam("uuid") String uuid) throws IOException, SQLException {

        log.info(i18n.message("rest.get.device.get.0", uuid));

         ResultSet rs = Service.sql.select("SELECT * FROM DEVICES");
        ArrayList<ZWaveDevice> zDevices = new ArrayList<ZWaveDevice>();

        try {
            while (rs.next()) {

                ZWaveDevice zdevice = new ZWaveDevice();

                zdevice.setManufName(rs.getString("manufname"));
                zdevice.setProductName(rs.getString("productname"));
                zdevice.setName(rs.getString("name"));
                zdevice.setNode((short) rs.getInt("node"));
                zdevice.setStatus(rs.getString("status"));
                zdevice.setInternalType(rs.getString("internaltype"));
                zdevice.setType(rs.getString("type"));
                zdevice.setUUID(rs.getString("uuid"));
                zdevice.setZone(rs.getInt("zone"));

                if (rs.getString("uuid").equals(uuid))
                    return gson.toJson(zdevice);

                zDevices.add(zdevice);
            }

            rs.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return gson.toJson(zDevices);
    }

    @GET
    @Path("/{uuid}/{label}/{level}")
    @Consumes(MediaType.TEXT_PLAIN + ";charset=utf-8")
    public String devSetLevel(@PathParam("uuid") String uuid, @PathParam("uuid") String label, @PathParam("level") String level)
    {
        log.info(i18n.message("rest.set.level.0.on.1.device", level, uuid));
        return "{ status: " + sendMessage(uuid, label, level) + " }";
    }

    private String sendMessage(String uuid, String label, String value)
    {
        try {

            final JsonMessaging jsonMessaging = new JsonMessaging(UUID.randomUUID());
            jsonMessaging.broadcast("event.devices.setvalue", new SetDeviceLevelAdvertisement(uuid, label, value));
            jsonMessaging.close();

            return i18n.message("done");

        } catch (final Throwable t) {
            log.error("Unexpected exception in DevicesREST", t);
            return "Something goes wrong: "+t.toString();
        }
    }

}
