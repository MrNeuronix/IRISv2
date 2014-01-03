package ru.iris;

import net.xeoh.plugins.base.PluginManager;
import net.xeoh.plugins.base.impl.PluginManagerFactory;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;
import ru.iris.common.SQL;

import java.io.File;
import java.io.IOException;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 08.09.13
 * Time: 22:52
 * License: GPL v3
 */
public class Launcher {

    private static Logger log = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) throws Exception {

        DOMConfigurator.configure("conf/log4j.xml");

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
