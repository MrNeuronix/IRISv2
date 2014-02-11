package ru.iris.common.database;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;
import ru.iris.common.Config;
import ru.iris.common.database.model.*;
import ru.iris.common.database.model.devices.Device;
import ru.iris.common.database.model.devices.DeviceValue;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 10.02.14
 * Time: 15:08
 * License: GPL v3
 */
public class DatabaseConnection {

    private final Config conf = new Config();
    private EbeanServer server;

    public DatabaseConnection()
    {
        ServerConfig config = new ServerConfig();
        config.setName("iris");

        // Define DataSource parameters
        DataSourceConfig db = new DataSourceConfig();
        db.setDriver("com.mysql.jdbc.Driver");
        db.setUsername(conf.getConfig().get("dbUsername"));
        db.setPassword(conf.getConfig().get("dbPassword"));
        db.setUrl("jdbc:mysql://"+conf.getConfig().get("dbHost")+"/"+conf.getConfig().get("dbName")
                + "?zeroDateTimeBehavior=convertToNull&useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&dontTrackOpenResources=true");
        //db.setHeartbeatSql("select count(*) from log");

        config.setDataSourceConfig(db);

        // set DDL options...
        config.setDdlGenerate(true);
        config.setDdlRun(true);

        config.setDefaultServer(true);
        config.setRegister(true);

        // specify entity classes
        config.addClass(Event.class);
        config.addClass(Log.class);
        config.addClass(ModuleStatus.class);
        config.addClass(Speaks.class);
        config.addClass(Task.class);
        config.addClass(Device.class);
        config.addClass(DeviceValue.class);

        // create the EbeanServer instance
        server = EbeanServerFactory.create(config);
    }

    public EbeanServer getServer() {
        return server;
    }
}
