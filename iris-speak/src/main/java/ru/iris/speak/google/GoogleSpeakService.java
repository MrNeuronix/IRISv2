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

import com.avaje.ebean.Ebean;
import com.darkprograms.speech.synthesiser.Synthesiser;
import javazoom.jl.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.Config;
import ru.iris.common.database.model.Speaks;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.ServiceCheckEmitter;
import ru.iris.common.messaging.model.service.ServiceStatus;
import ru.iris.common.messaging.model.speak.SpeakAdvertisement;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
import java.util.Map;
import java.util.UUID;

public class GoogleSpeakService implements Runnable {

    private Thread t = null;
    private Logger log = LogManager.getLogger(GoogleSpeakService.class.getName());
    private boolean shutdown = false;
    private Config config = new Config();

    public GoogleSpeakService() {
        t = new Thread(this);
        t.setName("Google Speak Service");
        t.start();
    }

    public Thread getThread() {
        return t;
    }

    @Override
    public synchronized void run() {
        log.info("Speak service started (TTS: Google)");

        Clip clip = null;
        AudioInputStream audioIn;
        Map<String, String> conf = config.getConfig();
        final Synthesiser synthesiser = new Synthesiser(conf.get("language"));

        ServiceCheckEmitter serviceCheckEmitter = new ServiceCheckEmitter("Speak");
        serviceCheckEmitter.setState(ServiceStatus.AVAILABLE);

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

                        Speaks speak = new Speaks();
                        speak.setText(advertisement.getText());
                        speak.setConfidence(advertisement.getConfidence());
                        speak.setDevice(advertisement.getDevice());
                        speak.setActive(true);

                        Ebean.save(speak);

                        if (conf.get("silence").equals("0")) {

                            log.info("Confidence: " + advertisement.getConfidence());
                            log.info("Text: " + advertisement.getText());
                            log.info("Device: " + advertisement.getDevice());

                            if (advertisement.getDevice().equals("all")) {
                                final Player player = new Player(synthesiser.getMP3Data(advertisement.getText()));
                                player.play();
                                player.close();
                            }

                        } else {
                            log.info("Silence mode enabled. Ignoring speak request.");
                        }

                    } else {
                        // We received unknown request message. Lets make generic log entry.
                        log.info("Received request "
                                + " from " + envelope.getSenderInstance()
                                + " to " + envelope.getReceiverInstance()
                                + " at '" + envelope.getSubject()
                                + ": " + envelope.getObject());
                    }
                }
            }

            serviceCheckEmitter.setState(ServiceStatus.SHUTDOWN);

            // Close JSON messaging.
            jsonMessaging.close();

        } catch (final Throwable t) {

            log.error("Unexpected exception in Speak", t);
            serviceCheckEmitter.setState(ServiceStatus.ERROR);
            t.printStackTrace();
        }
    }
}
