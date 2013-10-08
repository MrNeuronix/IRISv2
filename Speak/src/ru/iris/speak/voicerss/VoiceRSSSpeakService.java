package ru.iris.speak.voicerss;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 05.12.12
 * Time: 21:32
 * License: GPL v3
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.Messaging;
import ru.iris.speak.Service;

import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VoiceRSSSpeakService implements Runnable
{

    Thread t = null;
    private static Logger log = LoggerFactory.getLogger (VoiceRSSSpeakService.class.getName ());

    public VoiceRSSSpeakService()
    {
        t = new Thread (this);
        t.start ();
    }

    public Thread getThread()
    {
        return t;
    }

    @Override
    public synchronized void run()
    {
        log.info ("[speak] Service started (TTS: VoiceRSS)");

        Message message = null;
        MapMessage m = null;
        ExecutorService exs = Executors.newFixedThreadPool (10);

        try
        {
            MessageConsumer messageConsumer = new Messaging().getConsumer();

            while ((message = messageConsumer.receive (0)) != null)
            {
                m = (MapMessage) message;

                if(m.getStringProperty("qpid.subject").equals ("event.speak"))
                {
                    if(Service.config.get("silence").equals("0"))
                    {
                        log.info ("[speak] -----------------------");
                        log.info ("[speak] Confidence: " + m.getDoubleProperty("confidence"));
                        log.info ("[speak] Text: " + m.getStringProperty("text"));
                        log.info ("[speak] -----------------------");

                        VoiceRSSSynthesizer Voice = new VoiceRSSSynthesizer(exs);
                        Voice.setAnswer (m.getStringProperty("text"));
                        exs.submit (Voice).get ();
                    }
                    else
                    {
                        log.info("[speak] Silence mode enabled! Ignore speak request");
                    }
                }
            }

            Service.msg.close ();

        } catch (Exception e)
        {
            e.printStackTrace ();  //To change body of catch statement use File | Settings | File Templates.
            log.info ("Get error! " + m);
        }
    }
}
