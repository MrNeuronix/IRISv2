package ru.iris.restful;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 30.09.14
 * Time: 14:06
 * License: GPL v3
 */

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.net.httpserver.HttpServer;
import ro.fortsoft.pf4j.Plugin;
import ro.fortsoft.pf4j.PluginWrapper;
import ru.iris.common.Config;

import java.io.IOException;
import java.util.Map;

public class Service extends Plugin
{

    public Service (PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start()
    {
        log.info("[Plugin] iris-rest plugin started!");

		Config cfg = new Config();
		Map<String, String> config = cfg.getConfig();

		try
		{
			ResourceConfig rc = new PackagesResourceConfig("ru.iris.restful");
			rc.getProperties().put("com.sun.jersey.spi.container.ContainerRequestFilters", "ru.iris.restful.AuthFilter");
			HttpServer server = HttpServerFactory.create("http://" + config.get("httpHost") + ":" + config.get("httpPort") + "/", rc);
			server.start();

		}
		catch (IllegalArgumentException | IOException e)
		{
			log.error(e.toString());
		}
	}

    @Override
    public void stop() {
        log.info("[Plugin] iris-rest plugin stopped!");
    }
}
