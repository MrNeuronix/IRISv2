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
import java.sql.SQLException;

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
        @Path("/get")
        @Produces(MediaType.APPLICATION_JSON)
        public String device() throws IOException, SQLException {

            ZWaveDevice dev = new ZWaveDevice();
            dev.setValue("test", 255);
            dev.setValue("more", 12);
            dev.setName("test device");

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonOutput = gson.toJson(dev);

            return jsonOutput;
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
