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

package ru.iris.devices;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ro.fortsoft.pf4j.Plugin;
import ro.fortsoft.pf4j.PluginWrapper;
import ru.iris.common.Config;
import ru.iris.devices.noolite.NooliteRXService;
import ru.iris.devices.noolite.NooliteTXService;
import ru.iris.devices.zwave.ZWaveService;

import java.util.Map;

public class Service extends Plugin
{

	private static final Logger log = LogManager.getLogger(Service.class);

	public Service(PluginWrapper wrapper)
	{
		super(wrapper);
	}

	@Override
	public void start()
	{
		Map<String, String> config = new Config().getConfig();

		log.info("[Plugin] iris-devices plugin started!");

		// Generic device functions
		new CommonDeviceService();

		if (config.get("zwaveEnabled").equals("1"))
		{
			log.info("ZWave support is enabled. Starting");
			new ZWaveService();
		}
		if (config.get("nooliteEnabled").equals("1"))
		{
			log.info("NooLite support is enabled. Starting");
			if (config.get("nooliteTXPresent").equals("1"))
			{
				log.info("NooLite TX support is enabled. Starting");
				new NooliteTXService();
			}
			if (config.get("nooliteRXPresent").equals("1"))
			{
				log.info("NooLite RX support is enabled. Starting");
				new NooliteRXService();
			}
		}
	}

	@Override
	public void stop()
	{
		log.info("[Plugin] iris-devices plugin stopped!");
	}
}
