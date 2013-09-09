package ru.iris.restful;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.zwave.ZWaveDevice;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

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
        public String device() {

            ZWaveDevice dev = new ZWaveDevice();
            dev.setValue("test", 255);
            dev.setValue("more", 12);
            dev.setName("test device");
            dev.setNode(134);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonOutput = gson.toJson(dev);

            return jsonOutput;
        }

    /////////////////////////////////////
    // Тут - синтез речи
    /////////////////////////////////////

    @GET
    @Path("/speak/{text}")
    @Consumes(MediaType.TEXT_PLAIN)
    public String speak(@PathParam("text") String text) throws JMSException {

        MapMessage message = Service.session.createMapMessage();

        message.setString ("text", text);
        message.setDouble ("confidence", 100);
        message.setStringProperty ("qpid.subject", "event.speak");

        Service.messageProducer.send (message);

        return "done";
    }


}
