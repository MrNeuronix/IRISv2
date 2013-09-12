package ru.iris.restful;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.devices.zwave.ZWaveDevice;

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
 * Date: 06.09.12
 * Time: 17:24
 * License: GPL v3
 */



@Path("/")
public class RESTService
{
    private static Logger log = LoggerFactory.getLogger(RESTService.class.getName());

        @GET
        @Path("/device/get/{uuid}")
        @Produces(MediaType.TEXT_PLAIN)
        public String device(@PathParam("uuid") String uuid) throws IOException, SQLException {

            ResultSet rs = Service.sql.select("SELECT * FROM DEVICES");
            ArrayList zDevices = new ArrayList<ZWaveDevice>();
            Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().setPrettyPrinting().create();

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

                    ResultSet rsv = Service.sql.select("SELECT * FROM devicelabels WHERE UUID='"+rs.getString("uuid")+"'");
                        while (rsv.next()) {
                            zdevice.setValue(rsv.getString("label"), rsv.getString("value"));
                        }

                    if(rs.getString("uuid").equals(uuid))
                        return gson.toJson(zdevice);

                    zDevices.add(zdevice);
                }

                rs.close();

            } catch (SQLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            };

            return gson.toJson(zDevices);
        }

    /////////////////////////////////////
    // Cинтез речи
    /////////////////////////////////////

    @GET
    @Path("/speak/{text}")
    @Consumes(MediaType.TEXT_PLAIN)
    public String speak(@PathParam("text") String text) throws JMSException {

        MapMessage message = Service.session.createMapMessage();

        message.setStringProperty("text", text);
        message.setDoubleProperty("confidence", 100);
        message.setStringProperty ("qpid.subject", "event.speak");

        Service.messageProducer.send (message);

        return "done";
    }

    /////////////////////////////////////
    // Управление значениями устройств
    /////////////////////////////////////

    @GET
    @Path("/device/{uuid}/enable")
    @Consumes(MediaType.TEXT_PLAIN)
    public String devEnable(@PathParam("uuid") String uuid) throws JMSException {

        MapMessage message = Service.session.createMapMessage();

        message.setStringProperty("command", "enable");
        message.setStringProperty("uuid", uuid);
        message.setStringProperty ("qpid.subject", "event.devices.setvalue");

        Service.messageProducer.send (message);

        return "done";
    }

    @GET
    @Path("/device/{uuid}/disable")
    @Consumes(MediaType.TEXT_PLAIN)
    public String devDisable(@PathParam("uuid") String uuid) throws JMSException {

        MapMessage message = Service.session.createMapMessage();

        message.setStringProperty("command", "disable");
        message.setStringProperty("uuid", uuid);
        message.setStringProperty ("qpid.subject", "event.devices.setvalue");

        Service.messageProducer.send (message);

        return "done";
    }

    @GET
    @Path("/device/{uuid}/level/{level}")
    @Consumes(MediaType.TEXT_PLAIN)
    public String devSetLevel(@PathParam("uuid") String uuid, @PathParam("level") short level) throws JMSException {

        MapMessage message = Service.session.createMapMessage();

        message.setStringProperty("command", "setlevel");
        message.setShortProperty("level", level);
        message.setStringProperty("uuid", uuid);
        message.setStringProperty ("qpid.subject", "event.devices.setvalue");

        Service.messageProducer.send (message);

        return "done";
    }

    @GET
    @Path("/device/all/{state}")
    @Consumes(MediaType.TEXT_PLAIN)
    public String devAllState(@PathParam("state") String state) throws JMSException {

        MapMessage message = Service.session.createMapMessage();

        message.setStringProperty("command", "all"+state);
        message.setStringProperty ("qpid.subject", "event.devices.setvalue");

        Service.messageProducer.send (message);

        return "done";
    }


}
