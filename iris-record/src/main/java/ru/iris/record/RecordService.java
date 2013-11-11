package ru.iris.record;

import com.darkprograms.speech.recognizer.GoogleResponse;
import com.darkprograms.speech.recognizer.Recognizer;
import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;
import ru.iris.common.Module;

import javax.jms.MapMessage;
import javax.sound.sampled.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * Author: Nikolay A. Viguro
 * Date: 09.09.12
 * Time: 13:57
 * License: GPL v3
 */
public class RecordService implements Runnable {

    private static Logger log = LoggerFactory.getLogger(RecordService.class.getName());
    private static boolean busy = false;
    private static I18N i18n = new I18N();

    public RecordService() {
        Thread t = new Thread(this);
        t.start();
    }

    @Override
    public synchronized void run() {

        log.info(i18n.message("record.service.started"));

        int threads = Integer.valueOf(Service.config.get("recordStreams"));
        int micro = Integer.valueOf(Service.config.get("microphones"));

        Clip clip = null;
        AudioInputStream audioIn = null;

        if (Service.config.get("silence").equals("0")) {
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

        log.info(i18n.message("record.configured.to.run.0.threads.on.1.microphones", threads, micro));


        for (int m = 1; m <= micro; m++) {
            final int finalM = m;

            // Запускам потоки с записью с промежутком в 1с
            for (int i = 1; i <= threads; i++) {
                log.info(i18n.message("record.start.thread.0.on.microphone.1", i, finalM));

                final Clip finalClip = clip;
                new Thread(new Runnable() {

                    @Override
                    public void run() {

                        while (1 == 1) {
                            try {

                                Random randomGenerator = new Random();
                                @NonNls String strFilename = "infile-" + randomGenerator.nextInt(1000) + ".wav";
                                File outputFile = new File("./data/" + strFilename);

                                @NonNls ProcessBuilder procBuilder = null;

                                if (finalM == 1) {
                                    procBuilder = new ProcessBuilder("rec", "-q", "-c", "1", "-r", "16000", "./data/" + strFilename, "trim", "0", Service.config.get("recordDuration")).redirectOutput(ProcessBuilder.Redirect.INHERIT).redirectErrorStream(true);
                                } else {
                                    procBuilder = new ProcessBuilder("rec", "-q", "-c", "1", "-r", "16000", "-d", Service.config.get("microphoneDevice" + finalM), "./data/" + strFilename, "trim", "0", Service.config.get("recordDuration")).redirectOutput(ProcessBuilder.Redirect.INHERIT).redirectErrorStream(true);
                                }

                                procBuilder.redirectErrorStream(true);

                                Process process = procBuilder.start();

                                InputStream stdout = process.getInputStream();
                                InputStreamReader isrStdout = new InputStreamReader(stdout);
                                BufferedReader brStdout = new BufferedReader(isrStdout);

                                String line = null;

                                while ((line = brStdout.readLine()) != null) {
                                    System.out.println(line);
                                }

                                process.waitFor();

                                Recognizer rec = new Recognizer(Service.config.get("language"));

                                GoogleResponse response = rec.getRecognizedDataForWave(outputFile);
                                String command = response.getResponse();
                                String confidence = response.getConfidence();

                                if (command != null && Float.parseFloat(confidence) > 0.6) {

                                    if (command.contains(Service.config.get("systemName"))) {
                                        log.info(i18n.message("record.system.name.detected"));

                                        @NonNls ResultSet rs = Service.sql.select("SELECT name, command, param FROM modules WHERE enabled='1' AND language='" + Service.config.get("language") + "'");

                                        while (rs.next()) {
                                            String name = rs.getString("name");
                                            String comm = rs.getString("command");
                                            String param = rs.getString("param");

                                            if (command.contains(comm)) {

                                                log.info(i18n.message("record.server.found.exec.command"));

                                                if (busy) {
                                                    log.info(i18n.message("command.system.is.busy.skipping"));
                                                    break;
                                                }

                                                busy = true;

                                                log.info(i18n.message("command.got.0.command", command));

                                                @NonNls MapMessage message = Service.session.createMapMessage();

                                                message.setString("text", command);
                                                message.setDouble("confidence", Float.parseFloat(confidence) * 100);
                                                message.setStringProperty("qpid.subject", "event.record.recognized");

                                                Service.messageProducer.send(message);

                                                try {

                                                    Class cl = Class.forName("ru.iris.modules." + name);
                                                    Module execute = (Module) cl.newInstance();
                                                    execute.run(param);

                                                    Thread.sleep(1000);

                                                    busy = false;

                                                } catch (Exception e) {
                                                    log.info(i18n.message("module.error.at.loading.module.0.with.params.11", name, param));
                                                    e.printStackTrace();
                                                }

                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

                // delay 1 sec
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }
}