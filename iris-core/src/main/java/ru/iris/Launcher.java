package ru.iris;

import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;
import ru.iris.common.SQL;

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

        // Launch events module
        runModule("events");

        // Launch capture sound module
        runModule("record");

        // Launch speak synth module
        runModule("speak");

        // Launch module for work with devices
        runModule("devices");

        // Launch schedule module
        runModule("scheduler");

        // Lauch REST service
        runModule("rest");
    }

    private static void runModule(String module) throws IOException {

        ProcessBuilder builder = new ProcessBuilder("java -jar iris-" + module + ".jar").redirectOutput(ProcessBuilder.Redirect.INHERIT).redirectErrorStream(true);
        builder.start();
    }
}
