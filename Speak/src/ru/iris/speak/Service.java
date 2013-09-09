package ru.iris.speak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.Config;
import ru.iris.common.Messaging;
import ru.iris.common.SQL;
import ru.iris.speak.google.GoogleSpeakService;
import ru.iris.speak.voicerss.VoiceRSSSpeakService;

import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
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
    public static Messaging msg;
    public static Session session;

    private static Logger log = LoggerFactory.getLogger (Service.class);

    public static void main(String[] args) throws Exception {

        Config cfg = new Config ();
        config = cfg.getConfig ();
        sql = new SQL ();

        msg = new Messaging ();
        messageConsumer = msg.getConsumer ();
        messageProducer = msg.getProducer ();
        session = msg.getSession ();

        log.info ("[iris] ----------------------------------");
        log.info ("[iris] Speak service starting");
        log.info ("[iris] ----------------------------------");

        if(config.get("ttsEngine").equals("google"))
        {
            new GoogleSpeakService();
        }
        else if(config.get("ttsEngine").equals("voicerss"))
        {
            new VoiceRSSSpeakService();
        }
        else
        {
            log.info("[speak] No TTS system specified in config file!");
        }

    }
}
