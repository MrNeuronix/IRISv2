package ru.iris.restful;

import org.apache.qpid.AMQException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;
import ru.iris.common.Module;
import ru.iris.common.Speak;
import ru.iris.common.messaging.JsonMessaging;

import javax.jms.JMSException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

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
    private static JsonMessaging messaging;

    @GET
    @Path("/cmd/{text}")
    @Consumes(MediaType.TEXT_PLAIN + ";charset=utf-8")
    public String cmd(@PathParam("text") String text) throws JMSException, SQLException, URISyntaxException, AMQException {

        log.info(i18n.message("rest.get.cmd.0", text));

        messaging = new JsonMessaging(UUID.randomUUID());
        messaging.broadcast("event.command", text);
        messaging.close();

        ResultSet rs = Service.sql.select("SELECT name, command, param FROM modules");

        while (rs.next()) {
            String name = rs.getString("name");
            String comm = rs.getString("command");
            String param = rs.getString("param");

            if (text.contains(comm)) {
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

        return "{ status: " + i18n.message("done") + " }";
    }

    @GET
    @Path("/speak/{text}")
    @Consumes(MediaType.TEXT_PLAIN + ";charset=utf-8")
    public String speak(@PathParam("text") String text) throws JMSException, URISyntaxException, AMQException {

        log.info(i18n.message("rest.get.speak.0", text));

        new Speak().add(text);

        return "{ status: " + i18n.message("done") + " }";
    }
}
