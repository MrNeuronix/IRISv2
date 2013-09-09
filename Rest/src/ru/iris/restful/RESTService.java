package ru.iris.restful;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.zwave.ZWaveDevice;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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
    }
