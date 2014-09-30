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
import com.sun.jersey.api.core.ClassNamesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.net.httpserver.HttpServer;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.events.Init;
import ru.iris.common.Config;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Map;

@PluginImplementation
public class Service implements RestPlugin
{

	@Init
	public void init() throws IOException, SQLException, URISyntaxException
	{

		Config cfg = new Config();
		Map<String, String> config = cfg.getConfig();

		try
		{
			ResourceConfig rc = new ClassNamesResourceConfig("ru.iris.restful.DevicesREST");
			rc.getProperties().put("com.sun.jersey.spi.container.ContainerRequestFilters", "ru.iris.restful.AuthFilter");
			HttpServer server = HttpServerFactory.create("http://" + config.get("httpHost") + ":" + config.get("httpPort") + "/", rc);
			server.start();

		}
		catch (IllegalArgumentException | IOException e)
		{
			e.printStackTrace();
		}
	}
}
