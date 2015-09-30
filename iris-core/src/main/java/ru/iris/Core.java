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

import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.avaje.agentloader.AgentLoader;
import ro.fortsoft.pf4j.DefaultPluginManager;
import ro.fortsoft.pf4j.PluginManager;
import ru.iris.common.database.model.*;
import ru.iris.common.database.model.devices.Device;
import ru.iris.common.database.model.devices.DeviceValue;

import java.io.File;

class Core
{
    // Specify log4j2 and ebean configuration files
    static
	{
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "conf/log4j2.xml");
        System.setProperty("ebean.props.file", "conf/database.properties");
    }

	private static final Logger LOGGER = LogManager.getLogger(Core.class.getName());

	public static void main(String[] args) throws Exception
	{

		LOGGER.info("----------------------------------------");
		LOGGER.info("--        IRISv2 is starting          --");
		LOGGER.info("----------------------------------------");

		// ORM
        AgentLoader.loadAgent("lib/avaje-ebeanorm-agent-4.7.1.jar");

        ServerConfig config = new ServerConfig();
        config.setName("iris");

        DataSourceConfig ds = new DataSourceConfig();
        ds.loadSettings("iris");

        config.loadFromProperties();
        config.setDataSourceConfig(ds);

        config.addClass(Log.class);
        config.addClass(Event.class);
        config.addClass(Speaks.class);
        config.addClass(Task.class);
        config.addClass(Device.class);
        config.addClass(DeviceValue.class);
        config.addClass(DataSource.class);
        config.addClass(ScriptLock.class);
        config.addClass(SensorData.class);
        config.addClass(ModuleStatus.class);

        config.setDefaultServer(true);
        config.setRegister(true);

        EbeanServerFactory.create(config);

		// Load plugins
		PluginManager pluginManager = new DefaultPluginManager(new File("extensions"));
		pluginManager.loadPlugins();
		pluginManager.startPlugins();

	}
}
