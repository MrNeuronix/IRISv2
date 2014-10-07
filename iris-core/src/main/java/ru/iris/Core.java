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

package ru.iris;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.XMLConfigurationFactory;
import ro.fortsoft.pf4j.DefaultPluginManager;
import ro.fortsoft.pf4j.PluginManager;
import ru.iris.common.database.DatabaseConnection;

import java.io.File;

class Core
{
	// Specify log4j2 configuration file
	static
	{
		System.setProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "./conf/log4j2.xml");
	}

	private static final Logger LOGGER = LogManager.getLogger(Core.class.getName());

	public static void main(String[] args) throws Exception
	{

		LOGGER.info("----------------------------------------");
		LOGGER.info("--        IRISv2 is starting          --");
		LOGGER.info("----------------------------------------");

		// ORM
		new DatabaseConnection();

		// Modules poll
		new StatusChecker();

		// Load plugins
		PluginManager pluginManager = new DefaultPluginManager(new File("extensions"));
		pluginManager.loadPlugins();
		pluginManager.startPlugins();

	}
}
