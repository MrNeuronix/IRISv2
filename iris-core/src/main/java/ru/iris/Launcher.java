package ru.iris;

import org.apache.qpid.server.Broker;
import org.apache.qpid.server.BrokerOptions;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;

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

        // Launch H2 TCP server
        Server.createTcpServer().start();

        // Launch Apache Qpid broker
        BrokerOptions brokerOptions = new BrokerOptions();
        brokerOptions.setConfigProperty("qpid.home_dir", "conf/");
        Broker qpid = new Broker();
        qpid.startup(brokerOptions);

        // Enable internationalization
        I18N i18n = new I18N();

        log.info("----------------------------------------");
        log.info(i18n.message("irisv2.is.starting"));
        log.info("----------------------------------------");

        // Modules poll
        new StatusChecker();

        // Launch events module
        runModule("java -jar iris-events.jar");

        // Launch capture sound module
        runModule("java -jar iris-record.jar");

        // Launch speak synth module
        runModule("java -jar iris-speak.jar");

        // Launch module for work with devices
        runModule("java -jar iris-devices.jar");

        // Launch schedule module
        runModule("java -jar iris-scheduler.jar");

        // Lauch REST service
        runModule("java -jar iris-rest.jar");
    }

    private static void runModule(String cmd) throws IOException {

        ProcessBuilder builder = new ProcessBuilder(cmd.split("\\s+")).redirectOutput(ProcessBuilder.Redirect.INHERIT).redirectErrorStream(true);
        builder.start();
    }
}
