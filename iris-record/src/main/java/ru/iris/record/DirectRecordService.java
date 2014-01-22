package ru.iris.record;

import com.darkprograms.speech.microphone.MicrophoneAnalyzer;
import com.darkprograms.speech.recognizer.GoogleResponse;
import com.darkprograms.speech.recognizer.Recognizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.Config;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.service.ServiceStatus;
import ru.iris.common.messaging.model.speak.SpeakRecognizedAdvertisement;

import javax.sound.sampled.AudioFileFormat;
import java.io.File;
import java.util.Random;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * Author: Nikolay A. Viguro
 * Date: 09.09.12
 * Time: 13:57
 * License: GPL v3
 */
public class DirectRecordService implements Runnable {

    private Logger log = LogManager.getLogger(DirectRecordService.class.getName());
    private boolean busy = false;
    private JsonMessaging messaging;
    private Config config = new Config();
    private static SpeakRecognizedAdvertisement advertisement = new SpeakRecognizedAdvertisement();

    public DirectRecordService() {
        Thread t = new Thread(this);
        t.start();
    }

    @Override
    public synchronized void run() {

        final int startThreshold = Integer.valueOf(config.getConfig().get("startThreshold"));
        final int stopThreshold = Integer.valueOf(config.getConfig().get("stopThreshold"));

        messaging = new JsonMessaging(UUID.randomUUID());

        Service.serviceChecker.setAdvertisment(Service.advertisement.set("Record", Service.serviceId, ServiceStatus.AVAILABLE));

        Recognizer rec = new Recognizer("ru");

        boolean shutdown = false;

        while (!shutdown) {
            final MicrophoneAnalyzer mic = new MicrophoneAnalyzer(AudioFileFormat.Type.WAVE);
            mic.open();

            int avgVolume;
            boolean speaking = false;
            long captureStartMillis = System.currentTimeMillis();

            Random randomGenerator = new Random();
            String strFilename = "infile-" + randomGenerator.nextInt(1000) + ".wav";
            File filename = new File("./data/" + strFilename);

            try {

                avgVolume = mic.getAudioVolume();

                for (int i = 0; i < 1000 || speaking; i++) {
                    int volume = mic.getAudioVolume();
                    avgVolume = (2 * avgVolume + 1 * volume) / 3;

                    log.debug("Current volume: " + volume + " Average: " + avgVolume);

                    if (!speaking && avgVolume >= startThreshold) {
                        try {
                            mic.captureAudioToFile(filename);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        speaking = true;
                        log.info("Speaking detected");
                        captureStartMillis = System.currentTimeMillis();
                    }
                    if (System.currentTimeMillis() - captureStartMillis > 1000 && speaking && stopThreshold >= avgVolume) {
                        log.info("Done speaking to " + filename);
                        break;
                    }
                    Thread.sleep(100);

                }
                mic.close();

                GoogleResponse response = rec.getRecognizedDataForWave(filename);
                String text = response.getResponse();
                double confidence = Double.valueOf(response.getConfidence());

                log.info("Utterance: " + text.toUpperCase());
                log.info("Confidence: " + confidence * 100);

                if (confidence * 100 > 65) {
                    if (text.contains(config.getConfig().get("systemName"))) {
                        log.info("System name detected");

                        if (busy) {
                            log.info("System is busy. Skipping");
                            break;
                        }

                        busy = true;

                        log.info("Got command: " + text);

                        try {
                            messaging.broadcast("event.speak.recognized", advertisement.set(text, confidence));
                            Thread.sleep(1000);
                            busy = false;
                        } catch (Exception e) {
                            log.info("Failed to send recognized event");
                            e.printStackTrace();
                        }
                    }
                }


                try {
                    filename.delete();
                } catch (Exception ignored) {
                }


            } catch (Exception ignored) {
            }
        }
    }
}