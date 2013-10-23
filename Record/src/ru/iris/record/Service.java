package ru.iris.record;

import org.apache.qpid.AMQException;
import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.Config;
import ru.iris.common.I18N;
import ru.iris.common.Messaging;
import ru.iris.common.SQL;

import javax.jms.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 05.12.12
 * Time: 21:32
 */

public class Service
{
    public static HashMap<String, String> config;
    public static SQL sql;
    public static MessageConsumer messageConsumer;
    public static MessageProducer messageProducer;
    @NonNls
    public static Messaging msg;
    public static Session session;
    private static I18N i18n = new I18N();

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
        log.info (i18n.message("iris.record.service.starting"));
        log.info ("[iris] ----------------------------------");

        new RecordService ();

        // Check module status

        Message mess;
        @NonNls MapMessage m = null;

        msg.simpleSendMessage("status.answer", "alive", "record");

        while ((mess = Service.messageConsumer.receive (0)) != null)
        {
            m = (MapMessage) mess;

            if(m.getStringProperty("qpid.subject").equals ("status.record") || m.getStringProperty("qpid.subject").equals ("status.all"))
            {
                log.info (i18n.message("record.got.status.query"));
                msg.simpleSendMessage("status.answer", "alive", "record");
            }
        }
    }
}
