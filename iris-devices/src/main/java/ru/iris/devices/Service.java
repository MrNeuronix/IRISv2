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

import org.apache.log4j.xml.DOMConfigurator;
import org.apache.qpid.AMQException;
import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.Config;
import ru.iris.common.I18N;
import ru.iris.common.Messaging;
import ru.iris.common.SQL;
import ru.iris.devices.zwave.ZWaveService;

import javax.jms.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class Service {

    @NonNls
    public static Map<String, String> config;
    @NonNls
    public static SQL sql;
    public static MessageConsumer messageConsumer;
    public static MessageProducer messageProducer;
    @NonNls
    public static Messaging msg;
    public static Session session;

    private static Logger log = LoggerFactory.getLogger (Service.class);

    public static void main(String[] args) throws IOException, SQLException, AMQException, JMSException, URISyntaxException
    {
        DOMConfigurator.configure("conf/etc/log4j.xml");
        I18N i18n = new I18N();

        Config cfg = new Config ();
        config = cfg.getConfig ();
        sql = new SQL ();

        msg = new Messaging ();
        messageConsumer = msg.getConsumer ();
        messageProducer = msg.getProducer ();
        session = msg.getSession ();

        log.info ("[iris] ----------------------------------");
        log.info (i18n.message("iris.devices.service.starting"));
        log.info ("[iris] ----------------------------------");

        if(config.get("zwaveEnabled").equals("1"))
        {
            log.info(i18n.message("zwave.zwave.support.is.enabled.starting"));
            new ZWaveService();
        }
        if(config.get("onewireEnabled").equals("1"))
        {
            log.info(i18n.message("1.wire.1.wire.support.is.enabled.starting"));
        }

        // Check module status

        Message mess;
        @NonNls MapMessage m = null;

        msg.simpleSendMessage("status.answer", "alive", "devices");

        while ((mess = Service.messageConsumer.receive (0)) != null)
        {
            m = (MapMessage) mess;

            if(m.getStringProperty("qpid.subject").equals ("status.devices") || m.getStringProperty("qpid.subject").equals ("status.all"))
            {
                log.info (i18n.message("devices.got.status.query"));
                msg.simpleSendMessage("status.answer", "alive", "devices");
            }
        }

    }
}
