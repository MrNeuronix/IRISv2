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

package ru.iris.common.database;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;
import ru.iris.common.Config;
import ru.iris.common.database.model.*;
import ru.iris.common.database.model.devices.Device;
import ru.iris.common.database.model.devices.DeviceValue;

public class DatabaseConnection
{

	private final EbeanServer server;

	public DatabaseConnection()
	{
		ServerConfig config = new ServerConfig();
		config.setName("iris");

		// Define DataSource parameters
		DataSourceConfig db = new DataSourceConfig();
		db.setDriver("com.mysql.jdbc.Driver");
		Config conf = Config.getInstance();
		db.setUsername(conf.get("dbUsername"));
		db.setPassword(conf.get("dbPassword"));
		db.setUrl("jdbc:mysql://" + conf.get("dbHost") + "/" + conf.get("dbName")
				+ "?zeroDateTimeBehavior=convertToNull&useUnicode=true&characterEncoding=UTF-8");
		//db.setHeartbeatSql("select count(*) from log");

		config.setDataSourceConfig(db);

		// set DDL options...
		config.setDdlGenerate(Boolean.valueOf(conf.get("ddlGenerate")));
		config.setDdlRun(Boolean.valueOf(conf.get("ddlRun")));

		config.setDebugSql(Boolean.valueOf(conf.get("sqlDebug")));
		//config.setLoggingLevel(LogLevel.SQL);

		config.setDefaultServer(true);
		config.setRegister(true);

		// specify entity classes
		config.addClass(Event.class);
		config.addClass(Log.class);
		config.addClass(Speaks.class);
		config.addClass(Task.class);
		config.addClass(Device.class);
		config.addClass(DeviceValue.class);
		config.addClass(DataSource.class);
		config.addClass(ScriptLock.class);

		// create the EbeanServer instance
		server = EbeanServerFactory.create(config);
	}

	public EbeanServer getServer()
	{
		return server;
	}
}
