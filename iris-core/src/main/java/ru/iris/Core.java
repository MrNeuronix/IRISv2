package ru.iris;

import org.apache.activemq.broker.BrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.XMLConfigurationFactory;
import ro.fortsoft.pf4j.DefaultPluginManager;
import ro.fortsoft.pf4j.PluginManager;
import ru.iris.common.Config;
import ru.iris.common.database.DatabaseConnection;

import java.io.File;
import java.util.Properties;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 08.09.13
 * Time: 22:52
 * License: GPL v3
 */
public class Core {

    // Specify log4j2 configuration file
    static {
        System.setProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "./conf/log4j2.xml");
    }

    private static Logger log = LogManager.getLogger(Core.class.getName());
    private static Config config = new Config();

    public static void main(String[] args) throws Exception {

        log.info("----------------------------------------");
        log.info("--        IRISv2 is starting          --");
        log.info("----------------------------------------");

        Properties props = System.getProperties();
        props.setProperty("org.apache.activemq.UseDedicatedTaskRunner", "false");

        // start AMPQ broker
        BrokerService broker = new BrokerService();

        // configure the broker
        broker.setBrokerName("iris");
		broker.setPersistent(false);
		broker.addConnector("tcp://" + config.getConfig().get("AMQPhost") + ":" + config.getConfig().get("AMQPport") + "?jms.prefetchPolicy.all=10");
        broker.start();

        // ORM
        DatabaseConnection dbc = new DatabaseConnection();

        // Modules poll
        new StatusChecker();

        // load plugins
        PluginManager pluginManager = new DefaultPluginManager(new File("extensions"));
        pluginManager.loadPlugins();
        pluginManager.startPlugins();

    }
}
