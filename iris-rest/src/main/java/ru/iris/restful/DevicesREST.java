package ru.iris.restful;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;
import ru.iris.common.devices.ZWaveDevice;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

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

        @NonNls ResultSet rs = Service.sql.select("SELECT * FROM DEVICES");
        ArrayList<ZWaveDevice> zDevices = new ArrayList<ZWaveDevice>();

        try {
            while (rs.next()) {

                ZWaveDevice zdevice = new ZWaveDevice();

                zdevice.setManufName(rs.getString("manufname"));
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
    @Path("/{uuid}/enable")
    @Consumes(MediaType.TEXT_PLAIN + ";charset=utf-8")
    public String devEnable(@PathParam("uuid") String uuid) throws JMSException {

        log.info(i18n.message("rest.enable.0.device", uuid));

        @NonNls MapMessage message = Service.session.createMapMessage();

        message.setStringProperty("command", "enable");
        message.setStringProperty("uuid", uuid);
        message.setStringProperty("qpid.subject", "event.devices.setvalue");

        Service.messageProducer.send(message);

        return "{ status: " + i18n.message("done") + " }";
    }

    @GET
    @Path("/{uuid}/disable")
    @Consumes(MediaType.TEXT_PLAIN + ";charset=utf-8")
    public String devDisable(@PathParam("uuid") String uuid) throws JMSException {

        log.info(i18n.message("rest.disable.0.device", uuid));

        @NonNls MapMessage message = Service.session.createMapMessage();

        message.setStringProperty("command", "disable");
        message.setStringProperty("uuid", uuid);
        message.setStringProperty("qpid.subject", "event.devices.setvalue");

        Service.messageProducer.send(message);

        return "{ status: " + i18n.message("done") + " }";
    }

    @GET
    @Path("/{uuid}/level/{level}")
    @Consumes(MediaType.TEXT_PLAIN + ";charset=utf-8")
    public String devSetLevel(@PathParam("uuid") String uuid, @PathParam("level") short level) throws JMSException {

        log.info(i18n.message("rest.set.level.0.on.1.device", level, uuid));

        @NonNls MapMessage message = Service.session.createMapMessage();

        message.setStringProperty("command", "setlevel");
        message.setStringProperty("level", String.valueOf(level));
        message.setStringProperty("uuid", uuid);
        message.setStringProperty("qpid.subject", "event.devices.setvalue");

        Service.messageProducer.send(message);

        return "{ status: " + i18n.message("done") + " }";
    }

    @GET
    @Path("/all/{state}")
    @Consumes(MediaType.TEXT_PLAIN + ";charset=utf-8")
    public String devAllState(@PathParam("state") String state) throws JMSException {
        log.info(i18n.message("rest.switch.all.devices.to.0.state", state));
        Service.msg.simpleSendMessage("event.devices.setvalue", "command", "all" + state);

        return "{ status: " + i18n.message("done") + " }";
    }
}
