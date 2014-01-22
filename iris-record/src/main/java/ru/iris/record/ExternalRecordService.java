package ru.iris.record;

import javaFlacEncoder.FLAC_FileEncoder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.Config;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.service.ServiceStatus;
import ru.iris.common.messaging.model.speak.SpeakRecognizedAdvertisement;

import java.io.*;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * Author: Nikolay A. Viguro
 * Date: 09.09.12
 * Time: 13:57
 * License: GPL v3
 */
public class ExternalRecordService implements Runnable {

    private Logger log = LogManager.getLogger(ExternalRecordService.class.getName());
    private boolean busy = false;
    private JsonMessaging messaging;
    private Config config = new Config();
    private static SpeakRecognizedAdvertisement advertisement = new SpeakRecognizedAdvertisement();

    public ExternalRecordService() {
        Thread t = new Thread(this);
        t.start();
    }

    @Override
    public synchronized void run() {

        int threads = Integer.valueOf(config.getConfig().get("recordStreams"));
        int micro = Integer.valueOf(config.getConfig().get("microphones"));

        messaging = new JsonMessaging(UUID.randomUUID());

        log.info("Configured to run" + threads + " on " + micro + " mics");

        Service.serviceChecker.setAdvertisment(Service.advertisement.set("Record", Service.serviceId, ServiceStatus.AVAILABLE));

        for (int m = 1; m <= micro; m++) {
            final int finalM = m;

            // Запускам потоки с записью с промежутком в 1с
            for (int i = 1; i <= threads; i++) {
                log.info("Start capture thread " + i + " on mic " + finalM);

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

                                log.info("Utterance: " + text.toUpperCase());
                                log.info("Confidence: " + confidence * 100);

                                if (confidence * 100 > 65) {
                                    if (text.contains(config.getConfig().get("systemName"))) {
                                        log.info("System name detected!");

                                        if (busy) {
                                            log.info("System is busy. Skipping");
                                            break;
                                        }

                                        busy = true;

                                        log.info("Get command: " + text);

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


    // HTTP POST

    private class httpPOST {

        public Map<String, String> config;

        /**
         * Constructor will setup httpclient, post request method and useragent information as required
         */
        public httpPOST() {
            Config cfg = new Config();
            config = cfg.getConfig();
            httpclient = new DefaultHttpClient();
            System.setProperty("http.agent", "");
            httppost = new HttpPost("http://www.google.com/speech-api/v1/recognize?xjerr=1&client=chromium&lang=" + config.get("locale"));
            HttpProtocolParams.setUserAgent(httpclient.getParams(), User_Agent);
            httppost.setHeader(HeaderType, HeaderContent);
        }

        /**
         * This file will post the flac file to google and store the Json String in jsonResponse data member
         */
        private void postFLAC() {
            try {
                //long start = System.currentTimeMillis();

                // Load the file stream from the given filename
                File file = new File(FLACFileName);

                InputStreamEntity reqEntity = new InputStreamEntity(
                        new FileInputStream(file), -1);

                // Set the content type of the request entity to binary octet stream.. Taken from the chunked post example HTTPClient
                reqEntity.setContentType("binary/octet-stream");
                //reqEntity.setChunked(true); // Uncomment this line, but I feel it slows stuff down... Quick Tests show no difference


                // set the POST request entity...
                httppost.setEntity(reqEntity);

                //System.out.println("executing request " + httppost.getRequestLine());

                // Create an httpResponse object and execute the POST
                HttpResponse response = httpclient.execute(httppost);

                // Capture the Entity and get content
                HttpEntity resEntity = response.getEntity();

                //System.out.println(System.currentTimeMillis()-start);

                String buffer;
                jsonResponse = "";

                br = new BufferedReader(new InputStreamReader(resEntity.getContent()));
                while ((buffer = br.readLine()) != null) {
                    jsonResponse += buffer;
                }


                //System.out.println("Content: "+jsonResponse);

                // Close Buffered Reader and content stream.
                EntityUtils.consume(resEntity);
                br.close();
            } catch (Exception ee) {
                // In the event this POST Request FAILED
                //ee.printStackTrace();
                jsonResponse = "_failed_";
            } finally {
                // Finally shut down the client
                httpclient.getConnectionManager().shutdown();
            }
        }

        /**
         * postFile - Only public facing method of HTTPPOST, requires that you pass to it the filename
         */
        public String postFile(String fileName) {

            // Assuming we have a valid file name we call private postFLAC method
            if (fileName == null || fileName.equals("") || !fileName.contains(".flac")) {
                jsonResponse = "_failed_";
            } else {
                FLACFileName = fileName;
                postFLAC();
            }
            return jsonResponse;
        }

        /**
         * Data Members
         */

        private HttpClient httpclient;
        private HttpPost httppost;
        private BufferedReader br;
        private String jsonResponse;
        private String FLACFileName;

        // Immutable data members
        private final String HeaderType = "Content-Type";
        private final String HeaderContent = "audio/x-flac; rate=16000";
        private final String User_Agent = "Mozilla/5.0";
    }
}