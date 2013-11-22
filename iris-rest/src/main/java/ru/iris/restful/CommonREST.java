package ru.iris.restful;

import org.apache.qpid.AMQException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;
import ru.iris.common.Speak;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.CommandAdvertisement;

import javax.jms.JMSException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import java.net.URISyntaxException;
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

    private Logger log = LoggerFactory.getLogger(DevicesREST.class.getName());
    private I18N i18n = new I18N();
    private JsonMessaging messaging;

    @GET
    @Path("/cmd/{text}")
    @Consumes(MediaType.TEXT_PLAIN + ";charset=utf-8")
    public String cmd(@PathParam("text") String text) throws JMSException, URISyntaxException, AMQException {

        log.info(i18n.message("rest.get.cmd.0", text));

        messaging = new JsonMessaging(UUID.randomUUID());
        messaging.broadcast("event.command", new CommandAdvertisement(text));
        messaging.close();

        return "{ status: " + i18n.message("done") + " }";
    }

    @GET
    @Path("/speak/{text}")
    @Consumes(MediaType.TEXT_PLAIN + ";charset=utf-8")
    public String speak(@PathParam("text") String text) throws JMSException, URISyntaxException, AMQException {

        log.info(i18n.message("rest.get.speak.0", text));

        new Speak().say(text);

        return "{ status: " + i18n.message("done") + " }";
    }
}
