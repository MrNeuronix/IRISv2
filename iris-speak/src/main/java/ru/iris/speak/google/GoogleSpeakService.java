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

import com.darkprograms.speech.synthesiser.Synthesiser;
import javazoom.jl.player.Player;
import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;
import ru.iris.common.Messaging;
import ru.iris.common.messaging.model.ServiceAdvertisement;
import ru.iris.common.messaging.model.ServiceCapability;
import ru.iris.common.messaging.model.ServiceStatus;
import ru.iris.speak.Service;

import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.sound.sampled.*;
import java.io.File;

public class GoogleSpeakService implements Runnable {

    Thread t = null;
    private static Logger log = LoggerFactory.getLogger(GoogleSpeakService.class.getName());
    private static I18N i18n = new I18N();

    public GoogleSpeakService() {
        t = new Thread(this);
        t.start();
    }

    public Thread getThread() {
        return t;
    }

    @Override
    public synchronized void run() {
        log.info(i18n.message("speak.service.started.tts.google"));

        Message message = null;
        @NonNls MapMessage m = null;

        Clip clip = null;
        AudioInputStream audioIn = null;

        Service.ServiceState.setAdvertisment(new ServiceAdvertisement(
                "Speak", Service.serviceId, ServiceStatus.AVAILABLE,
                new ServiceCapability[]{ServiceCapability.SPEAK}));

        if(Service.config.get("silence").equals("0"))
        {
            try {
                audioIn = AudioSystem.getAudioInputStream(new File("./conf/beep.wav"));
                AudioFormat format = audioIn.getFormat();
                DataLine.Info info = new DataLine.Info(Clip.class, format);
                clip = (Clip)AudioSystem.getLine(info);
                clip.open(audioIn);
                clip.start();

                while(clip.isRunning())
                {
                    Thread.yield();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            MessageConsumer messageConsumer = new Messaging().getConsumer();

            while ((message = messageConsumer.receive(0)) != null) {
                m = (MapMessage) message;

                if (m.getStringProperty("qpid.subject").equals("event.speak")) {
                    if (Service.config.get("silence").equals("0")) {
                        log.info("[speak] -----------------------");
                        log.info(i18n.message("speak.confidence.0", m.getDoubleProperty("confidence")));
                        log.info(i18n.message("speak.text.0", m.getStringProperty("text")));
                        log.info("[speak] -----------------------");

                        if(!Service.config.get("silence").equals("1"))
                        {
                            clip.setFramePosition(0);
                            clip.start();
                            clip.start();
                        }

                        Synthesiser synthesiser = new Synthesiser(Service.config.get("language"));
                        final Player player = new Player(synthesiser.getMP3Data(m.getStringProperty("text")));
                        player.play();
                        player.close();
                    } else {
                        log.info(i18n.message("speak.silence.mode.enabled.ignore.speak.request"));
                    }
                }
            }

            Service.msg.close();

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            log.info(i18n.message("speak.get.error.0", m));
        }
    }
}
