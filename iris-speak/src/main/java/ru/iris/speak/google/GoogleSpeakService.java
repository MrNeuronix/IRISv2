package ru.iris.speak.google;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 05.12.12
 * Time: 21:32
 * License: GPL v3
 */

import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;
import ru.iris.common.Messaging;
import ru.iris.speak.Service;

import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoogleSpeakService implements Runnable
{

    Thread t = null;
    private static Logger log = LoggerFactory.getLogger (GoogleSpeakService.class.getName ());
    private static I18N i18n = new I18N();

    public GoogleSpeakService()
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
        log.info (i18n.message("speak.service.started.tts.google"));

        Message message = null;
        @NonNls MapMessage m = null;
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
                        log.info (i18n.message("speak.confidence.0", m.getDoubleProperty("confidence")));
                        log.info (i18n.message("speak.text.0", m.getStringProperty("text")));
                        log.info ("[speak] -----------------------");

                        GoogleSynthesizer Voice = new GoogleSynthesizer (exs);
                        Voice.setAnswer (m.getStringProperty("text"));
                        exs.submit (Voice).get ();
                    }
                    else
                    {
                        log.info(i18n.message("speak.silence.mode.enabled.ignore.speak.request"));
                    }
                }
            }

            Service.msg.close ();

        } catch (Exception e)
        {
            e.printStackTrace ();  //To change body of catch statement use File | Settings | File Templates.
            log.info (i18n.message("speak.get.error.0", m));
        }
    }
}
