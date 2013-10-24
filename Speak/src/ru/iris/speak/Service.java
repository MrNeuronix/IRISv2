package ru.iris.speak;

import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.*;
import ru.iris.speak.google.GoogleSpeakService;
import ru.iris.speak.voicerss.VoiceRSSSpeakService;

import javax.jms.*;
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
    @NonNls
    public static HashMap<String, String> config;
    public static SQL sql;
    public static MessageConsumer messageConsumer;
    public static MessageProducer messageProducer;
    @NonNls
    public static Messaging msg;
    public static Session session;
    private static I18N i18n = new I18N();

    private static Logger log = LoggerFactory.getLogger (Service.class);

    public static void main(String[] args) throws Exception {

        Config cfg = new Config ();
        config = cfg.getConfig ();
        sql = new SQL ();

        Speak speak = new Speak();

        msg = new Messaging ();
        messageConsumer = msg.getConsumer ();
        messageProducer = msg.getProducer ();
        session = msg.getSession ();

        log.info ("[iris] ----------------------------------");
        log.info (i18n.message("iris.speak.service.starting"));
        log.info ("[iris] ----------------------------------");

        if(config.get("ttsEngine").equals("google"))
        {
            new GoogleSpeakService();
            speak.add(i18n.message("syth.voice.launched"));
        }
        else if(config.get("ttsEngine").equals("voicerss"))
        {
            new VoiceRSSSpeakService();
            speak.add(i18n.message("voice.synth.voicerss.launched"));
        }
        else
        {
            log.info(i18n.message("speak.no.tts.system.specified.in.config.file"));
        }

        // Check module status

        Message mess;
        @NonNls MapMessage m = null;

        msg.simpleSendMessage("status.answer", "alive", "speak");

        while ((mess = Service.messageConsumer.receive (0)) != null)
        {
            m = (MapMessage) mess;

            if(m.getStringProperty("qpid.subject").equals ("status.speak") || m.getStringProperty("qpid.subject").equals ("status.all"))
            {
                log.info (i18n.message("speak.got.status.query"));
                msg.simpleSendMessage("status.answer", "alive", "speak");
            }
        }

    }
}
