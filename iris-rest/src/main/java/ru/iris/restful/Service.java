package ru.iris.restful;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 06.09.12
 * Time: 17:24
 * License: GPL v3
 */

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.net.httpserver.HttpServer;
import org.apache.log4j.xml.DOMConfigurator;
import org.apache.qpid.AMQException;
import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.Config;
import ru.iris.common.I18N;
import ru.iris.common.Messaging;
import ru.iris.common.SQL;
import ru.iris.common.messaging.ServiceChecker;
import ru.iris.common.messaging.model.ServiceAdvertisement;
import ru.iris.common.messaging.model.ServiceCapability;
import ru.iris.common.messaging.model.ServiceStatus;

import javax.jms.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

public class Service {

    public static Map<String, String> config;
    public static SQL sql;
    private static Logger log = LoggerFactory.getLogger(Service.class);
    public static MessageConsumer messageConsumer;
    public static MessageProducer messageProducer;
    public static Messaging msg;
    public static Session session;
    private static I18N i18n = new I18N();

    public static void main(String[] args) throws IOException, SQLException, AMQException, JMSException, URISyntaxException {

        DOMConfigurator.configure("conf/etc/log4j.xml");

        Config cfg = new Config();
        config = cfg.getConfig();
        sql = new SQL();

        msg = new Messaging();
        messageConsumer = msg.getConsumer();
        messageProducer = msg.getProducer();
        session = msg.getSession();

        try {
            ResourceConfig rc = new PackagesResourceConfig("ru.iris.restful");
            rc.getProperties().put("com.sun.jersey.spi.container.ContainerRequestFilters", "ru.iris.restful.AuthFilter");
            HttpServer server = HttpServerFactory.create("http://" + config.get("httpHost") + ":" + config.get("httpPort") + "/", rc);
            server.start();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Check module status
        new ServiceChecker().start(UUID.fromString("444b3e75-7c0c-4d6e-a1f3-f373ef7f6005"), new ServiceAdvertisement(
                "Rest", UUID.randomUUID(), ServiceStatus.AVAILABLE,
                new ServiceCapability[]{ServiceCapability.CONTROL}));
    }
}
