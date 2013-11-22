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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.Config;
import ru.iris.common.I18N;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.ServiceAdvertisement;
import ru.iris.common.messaging.model.ServiceCapability;
import ru.iris.common.messaging.model.ServiceStatus;
import ru.iris.common.messaging.model.SpeakAdvertisement;
import ru.iris.speak.Service;

import javax.sound.sampled.*;
import java.io.File;
import java.util.UUID;

public class GoogleSpeakService implements Runnable {

    private Thread t = null;
    private Logger log = LoggerFactory.getLogger(GoogleSpeakService.class.getName());
    private I18N i18n = new I18N();
    private boolean shutdown = false;
    private Config config = new Config();

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

        Clip clip = null;
        AudioInputStream audioIn;


        Service.serviceChecker.setAdvertisment(
                new ServiceAdvertisement("Speak", Service.serviceId, ServiceStatus.AVAILABLE,
                        new ServiceCapability[]{ServiceCapability.SPEAK}));

        if (config.getConfig().get("silence").equals("0")) {
            try {
                audioIn = AudioSystem.getAudioInputStream(new File("./conf/beep.wav"));
                AudioFormat format = audioIn.getFormat();
                DataLine.Info info = new DataLine.Info(Clip.class, format);
                clip = (Clip) AudioSystem.getLine(info);
                clip.open(audioIn);
                clip.start();

                while (clip.isRunning()) {
                    Thread.yield();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            // Make sure we exit the wait loop if we receive shutdown signal.
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    shutdown = true;
                }
            }));

            JsonMessaging jsonMessaging = new JsonMessaging(UUID.randomUUID());
            jsonMessaging.subscribe("event.speak");
            jsonMessaging.start();

            while (!shutdown) {

                // Lets wait for 100 ms on json messages and if nothing comes then proceed to carry out other tasks.
                final JsonEnvelope envelope = jsonMessaging.receive(100);
                if (envelope != null) {
                    if (envelope.getObject() instanceof SpeakAdvertisement) {

                        SpeakAdvertisement advertisement = envelope.getObject();

                        if (config.getConfig().get("silence").equals("0")) {
                            log.info(i18n.message("speak.confidence.0", advertisement.getConfidence()));
                            log.info(i18n.message("speak.text.0", advertisement.getText()));

                            if (!config.getConfig().get("silence").equals("1")) {
                                clip.setFramePosition(0);
                                clip.start();
                                clip.start();
                            }

                            Synthesiser synthesiser = new Synthesiser(config.getConfig().get("language"));
                            final Player player = new Player(synthesiser.getMP3Data(advertisement.getText()));
                            player.play();
                            player.close();
                        } else {
                            log.info(i18n.message("speak.silence.mode.enabled.ignore.speak.request"));
                        }

                    } else {
                        // We received unknown request message. Lets make generic log entry.
                        log.info("Received request "
                                + " from " + envelope.getSenderInstanceId()
                                + " to " + envelope.getReceiverInstanceId()
                                + " at '" + envelope.getSubject()
                                + ": " + envelope.getObject());
                    }
                }
            }

            Service.serviceChecker.setAdvertisment(
                    new ServiceAdvertisement("Speak", Service.serviceId, ServiceStatus.SHUTDOWN,
                            new ServiceCapability[]{ServiceCapability.SPEAK}));

            // Close JSON messaging.
            jsonMessaging.close();

        } catch (final Throwable t) {

            log.error("Unexpected exception in Speak", t);

            Service.serviceChecker.setAdvertisment(
                    new ServiceAdvertisement("Speak", Service.serviceId, ServiceStatus.SHUTDOWN,
                            new ServiceCapability[]{ServiceCapability.SPEAK}));
            t.printStackTrace();
        }
    }
}
