package ru.iris;

import net.xeoh.plugins.base.PluginManager;
import net.xeoh.plugins.base.impl.PluginManagerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.XMLConfigurationFactory;
import ru.iris.common.I18N;
import ru.iris.common.SQL;

import java.io.File;

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

    public static void main(String[] args) throws Exception {

        // Enable internationalization
        I18N i18n = new I18N();

        log.info("----------------------------------------");
        log.info(i18n.message("irisv2.is.starting"));
        log.info("----------------------------------------");

        // clear all message data
        SQL sql = new SQL();
        sql.doQuery("TRUNCATE messages");
        sql.close();

        // Modules poll
        new StatusChecker();

        // load plugins
        PluginManager pm = PluginManagerFactory.createPluginManager();
        pm.addPluginsFrom(new File("extensions/").toURI());

    }
}
