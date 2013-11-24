package ru.iris.record;

import javaFlacEncoder.FLAC_FileEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.Config;
import ru.iris.common.I18N;
import ru.iris.common.httpPOST;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.ServiceStatus;
import ru.iris.common.messaging.model.SpeakRecognizedAdvertisement;

import javax.jms.JMSException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Random;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * Author: Nikolay A. Viguro
 * Date: 09.09.12
 * Time: 13:57
 * License: GPL v3
 */
public class RecordService implements Runnable {

    private Logger log = LoggerFactory.getLogger(RecordService.class.getName());
    private boolean busy = false;
    private I18N i18n = new I18N();
    private JsonMessaging messaging;
    private Config config = new Config();
    private static SpeakRecognizedAdvertisement advertisement = new SpeakRecognizedAdvertisement();

    public RecordService() {
        Thread t = new Thread(this);
        t.start();
    }

    @Override
    public synchronized void run() {

        int threads = Integer.valueOf(config.getConfig().get("recordStreams"));
        int micro = Integer.valueOf(config.getConfig().get("microphones"));

            messaging = new JsonMessaging(UUID.randomUUID());

        log.info(i18n.message("record.configured.to.run.0.threads.on.1.microphones", threads, micro));

        Service.serviceChecker.setAdvertisment(Service.advertisement.set("Record", Service.serviceId, ServiceStatus.AVAILABLE));

        for (int m = 1; m <= micro; m++) {
            final int finalM = m;

            // Запускам потоки с записью с промежутком в 1с
            for (int i = 1; i <= threads; i++) {
                log.info(i18n.message("record.start.thread.0.on.microphone.1", i, finalM));

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            Random randomGenerator = new Random();
                            String strFilename = "infile-" + randomGenerator.nextInt(1000) + ".wav";
                            File outputFile = new File("./data/" + strFilename);

                            ProcessBuilder procBuilder = null;

                            if (finalM == 1) {
                                procBuilder = new ProcessBuilder("rec", "-q", "-c", "1", "-r", "16000", "./data/" + strFilename, "trim", "0", config.getConfig().get("recordDuration"));
                            } else {
                                procBuilder = new ProcessBuilder("rec", "-q", "-c", "1", "-r", "16000", "-d", config.getConfig().get("microphoneDevice" + finalM), "./data/" + strFilename, "trim", "0", config.getConfig().get("recordDuration"));
                            }

                            httpPOST SendFile = new httpPOST();

                            Process process = null;
                            try {
                                process = procBuilder.start();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            try {
                                process.waitFor();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            FLAC_FileEncoder encoder1 = new FLAC_FileEncoder();
                            File infile = outputFile;
                            File outfile = new File("./data/" + strFilename + ".flac");
                            encoder1.useThreads(true);
                            encoder1.encode(infile, outfile);

                            String googleSpeechAPIResponse = SendFile.postFile(System.getProperty("user.dir") + "/data/" + strFilename + ".flac");

                            // debug
                            if (!googleSpeechAPIResponse.contains("\"utterance\":")) {
                                // System.err.println("[record] Recognizer: No Data");
                            } else {
                                // Include -> System.out.println(wGetResponse); // to view the Raw output
                                int startIndex = googleSpeechAPIResponse.indexOf("\"utterance\":") + 13; //Account for term "utterance":"<TARGET>","confidence"
                                int stopIndex = googleSpeechAPIResponse.indexOf(",\"confidence\":") - 1; //End position
                                String text = googleSpeechAPIResponse.substring(startIndex, stopIndex);

                                // Determine Confidence
                                startIndex = stopIndex + 15;
                                stopIndex = googleSpeechAPIResponse.indexOf("}]}") - 1;
                                double confidence = Double.parseDouble(googleSpeechAPIResponse.substring(startIndex, stopIndex));

                                log.info(i18n.message("data.utterance.0", text.toUpperCase()));
                                log.info(i18n.message("data.confidence.level.0", confidence * 100));

                                if (confidence * 100 > 65) {
                                    if (text.contains(config.getConfig().get("systemName"))) {
                                        log.info(i18n.message("record.system.name.detected"));

                                        if (busy) {
                                            log.info(i18n.message("command.system.is.busy.skipping"));
                                            break;
                                        }

                                        busy = true;

                                        log.info(i18n.message("command.got.0.command", text));

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
                            }

                            try {
                                outputFile.delete();
                                outfile.delete();
                                infile.delete();
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }).start();

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}