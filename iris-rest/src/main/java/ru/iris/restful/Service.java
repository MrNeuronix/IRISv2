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
import ru.iris.common.Config;
import ru.iris.common.messaging.ServiceChecker;
import ru.iris.common.messaging.model.ServiceAdvertisement;
import ru.iris.common.messaging.model.ServiceStatus;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

public class Service {

    private static Map<String, String> config;
    public static ServiceChecker serviceChecker;
    public static ServiceAdvertisement advertisement = new ServiceAdvertisement();
    public static final UUID serviceId = UUID.fromString("444b3e75-7c0c-4d6e-a1f3-f373ef7f6005");

    public static void main(String[] args) throws IOException, SQLException, URISyntaxException {

        DOMConfigurator.configure("conf/log4j.xml");

        serviceChecker = new ServiceChecker(serviceId, advertisement.set(
                "Rest", serviceId, ServiceStatus.STARTUP));

        Config cfg = new Config();
        config = cfg.getConfig();

        try {
            ResourceConfig rc = new PackagesResourceConfig("ru.iris.restful");
            rc.getProperties().put("com.sun.jersey.spi.container.ContainerRequestFilters", "ru.iris.restful.AuthFilter");
            HttpServer server = HttpServerFactory.create("http://" + config.get("httpHost") + ":" + config.get("httpPort") + "/", rc);
            server.start();

            serviceChecker.setAdvertisment(advertisement.set("Rest", serviceId, ServiceStatus.AVAILABLE));

        } catch (IllegalArgumentException | IOException e) {
            e.printStackTrace();
        }
    }
}
