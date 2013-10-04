package ru.iris.devices;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 10.09.13
 * Time: 13:32
 * License: GPL v3
 */

import org.apache.qpid.AMQException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.Config;
import ru.iris.common.Messaging;
import ru.iris.common.SQL;
import ru.iris.devices.zwave.ZWaveService;

import javax.jms.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.HashMap;

public class Service {

    public static HashMap<String, String> config;
    public static SQL sql;
    public static MessageConsumer messageConsumer;
    public static MessageProducer messageProducer;
    public static Messaging msg;
    public static Session session;

    private static Logger log = LoggerFactory.getLogger (Service.class);

    public static void main(String[] args) throws IOException, SQLException, AMQException, JMSException, URISyntaxException
    {

        Config cfg = new Config ();
        config = cfg.getConfig ();
        sql = new SQL ();

        msg = new Messaging ();
        messageConsumer = msg.getConsumer ();
        messageProducer = msg.getProducer ();
        session = msg.getSession ();

        log.info ("[iris] ----------------------------------");
        log.info ("[iris] Devices service starting");
        log.info ("[iris] ----------------------------------");

        if(config.get("zwaveEnabled").equals("1"))
        {
            log.info("[zwave] ZWave support is enabled. Starting.");
            new ZWaveService();
        }
        if(config.get("onewireEnabled").equals("1"))
        {
            log.info("[1-wire] 1-wire support is enabled. Starting.");
        }

        // Check module status

        Message mess;
        MapMessage m = null;

        msg.simpleSendMessage("status.answer", "alive", "devices");

        while ((mess = Service.messageConsumer.receive (0)) != null)
        {
            m = (MapMessage) mess;

            if(m.getStringProperty("qpid.subject").equals ("status.devices") || m.getStringProperty("qpid.subject").equals ("status.all"))
            {
                log.info ("[devices] Got status query");
                msg.simpleSendMessage("status.answer", "alive", "devices");
            }
        }

    }
}
