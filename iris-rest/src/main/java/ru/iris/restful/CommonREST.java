package ru.iris.restful;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;
import ru.iris.common.Module;
import ru.iris.common.devices.ZWaveDevice;
import ru.iris.scheduler.Task;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

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
public class CommonREST {

    private static Logger log = LoggerFactory.getLogger(DevicesREST.class.getName());
    private static I18N i18n = new I18N();
    private static Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().setPrettyPrinting().create();

    @GET
    @Path("/cmd/{text}")
    @Consumes(MediaType.TEXT_PLAIN + ";charset=utf-8")
    public String cmd(@PathParam("text") String text) throws JMSException, SQLException {

        log.info(i18n.message("rest.get.cmd.0", text));

        Service.msg.simpleSendMessage("event.command", "cmd", text);

        Message mess;
         MapMessage m = null;

        while ((mess = Service.messageConsumer.receive(0)) != null) {
            m = (MapMessage) mess;

            if (m.getStringProperty("qpid.subject").equals("event.command")) {
                log.info(i18n.message("rest.got.0.command", m.getStringProperty("cmd")));

                 ResultSet rs = Service.sql.select("SELECT name, command, param FROM modules");

                while (rs.next()) {
                    String name = rs.getString("name");
                    String comm = rs.getString("command");
                    String param = rs.getString("param");

                    if (m.getStringProperty("cmd").contains(comm)) {
                        try {

                            Class cl = Class.forName("modules." + name);
                            Module execute = (Module) cl.newInstance();
                            execute.run(param);

                        } catch (Exception e) {
                            log.info(i18n.message("module.error.at.loading.module.0.with.params.1", name, param));
                            e.printStackTrace();
                        }
                    }
                }

                rs.close();
            }
        }

        return "{ status: " + i18n.message("done") + " }";
    }

    @GET
    @Path("/speak/{text}")
    @Consumes(MediaType.TEXT_PLAIN + ";charset=utf-8")
    public String speak(@PathParam("text") String text) throws JMSException {

        log.info(i18n.message("rest.get.speak.0", text));

         MapMessage message = Service.session.createMapMessage();

        message.setStringProperty("text", text);
        message.setDoubleProperty("confidence", 100);
        message.setStringProperty("qpid.subject", "event.speak");

        Service.messageProducer.send(message);

        return "{ status: " + i18n.message("done") + " }";
    }
}
