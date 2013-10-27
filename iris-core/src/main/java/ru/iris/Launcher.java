package ru.iris;

import org.apache.qpid.server.Broker;
import org.apache.qpid.server.BrokerOptions;
import org.apache.qpid.server.Main;
import org.h2.tools.Server;
import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.Config;
import ru.iris.common.I18N;
import ru.iris.common.Messaging;
import ru.iris.common.SQL;

import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import java.io.IOException;
import java.util.HashMap;

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

    public static HashMap<String, String> config;
    private static Logger log = LoggerFactory.getLogger(Launcher.class);
    public static SQL sql;
    public static MessageConsumer messageConsumer;
    public static MessageProducer messageProducer;
    public static Messaging msg;
    public static Session session;

    public static void main(String[] args) throws Exception {

        // Launch H2 TCP server
        Server.createTcpServer().start();

        // Launch Apache Qpid broker

        BrokerOptions brokerOptions = new BrokerOptions();
        brokerOptions.setConfigProperty("qpid.home_dir", "conf/");
        Broker qpid = new Broker();
        qpid.startup(brokerOptions);

        msg = new Messaging();
        messageConsumer = msg.getConsumer ();
        messageProducer = msg.getProducer ();
        session = msg.getSession ();

        // Enable internationalization
        I18N i18n = new I18N();

        log.info ("----------------------------------------");
        log.info (i18n.message("irisv2.is.starting"));
        log.info ("----------------------------------------");

        // Load configuration
        Config cfg = new Config ();
        config = cfg.getConfig ();
        sql = new SQL();

        // Modules poll
        new StatusChecker();

        // Lauch REST service
        runModule("java -jar iris-rest.jar");

        // Launch capture sound module
        runModule("java -jar iris-record.jar");

        // Launch speak synth module
        runModule("java -jar iris-speak.jar");

        // Launch module for work with devices
        runModule("java -jar iris-devices.jar");

        // Launch schedule module
        runModule("java -jar iris-scheduler.jar");

        // Launch events module
        runModule("java -jar iris-events.jar");
    }

    private static void runModule(@NonNls String cmd) throws IOException {

        ProcessBuilder builder = new ProcessBuilder(cmd.split("\\s+")).redirectOutput(ProcessBuilder.Redirect.INHERIT).redirectErrorStream(true);
        builder.start();
    }
}
