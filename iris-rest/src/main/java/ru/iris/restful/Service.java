/*
 * Copyright 2012-2014 Nikolay A. Viguro
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.iris.restful;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.jboss.resteasy.plugins.server.sun.http.HttpContextBuilder;
import ro.fortsoft.pf4j.Plugin;
import ro.fortsoft.pf4j.PluginWrapper;
import ru.iris.common.Config;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

public class Service extends Plugin
{

	public Service(PluginWrapper wrapper)
	{
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
			HttpServer httpServer = HttpServer.create(new InetSocketAddress(17000), 10);
			HttpContextBuilder contextBuilder = new HttpContextBuilder();
			contextBuilder.getDeployment().getActualResourceClasses().add(DevicesREST.class);
			contextBuilder.getDeployment().getActualResourceClasses().add(CommonREST.class);
			contextBuilder.getDeployment().getActualProviderClasses().add(NotFoundExceptionMapper.class);
			HttpContext context = contextBuilder.bind(httpServer);
			httpServer.start();

		}
		catch (IllegalArgumentException | IOException e)
		{
			log.error(e.toString());
		}
	}

	@Override
	public void stop()
	{
		log.info("[Plugin] iris-rest plugin stopped!");
	}
}
