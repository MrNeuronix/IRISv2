package ru.iris.record;

import javaFlacEncoder.FLAC_FileEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.Module;
import ru.iris.common.httpPOST;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * Author: Nikolay A. Viguro
 * Date: 09.09.12
 * Time: 13:57
 * License: GPL v3
 */
public class RecordService implements Runnable
{

    private static Logger log = LoggerFactory.getLogger (RecordService.class.getName ());
    private static boolean busy = false;

    public RecordService()
    {
        Thread t = new Thread (this);
        t.start ();
    }

    @Override
    public synchronized void run()
    {

        log.info ("[record] Service started");

        int threads = Integer.valueOf (Service.config.get ("recordStreams"));
        int micro = Integer.valueOf (Service.config.get ("microphones"));

        Clip clip = null;
        AudioInputStream audioIn = null;
        try {
            audioIn = AudioSystem.getAudioInputStream(new File("./conf/beep.wav"));
            clip = AudioSystem.getClip();
            clip.open(audioIn);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        log.info ("[record] Configured to run " + threads + " threads on " + micro + " microphones");

        for (int m = 1; m <= micro; m++)
        {
            final int finalM = m;

            // Запускам потоки с записью с промежутком в 1с
            for (int i = 1; i <= threads; i++)
            {
                log.info ("[record] Start thread " + i + " on microphone " + finalM);

                final Clip finalClip = clip;
                new Thread (new Runnable ()
                {

                    @Override
                    public void run()
                    {

                        while (1 == 1)
                        {

                            Random randomGenerator = new Random ();
                            String strFilename = "infile-" + randomGenerator.nextInt (1000) + ".wav";
                            File outputFile = new File ("./data/" + strFilename);

                            // Тут захват и обработка звука
                            //////////////////////////////////

                            // указываем в конструкторе ProcessBuilder,
                            // что нужно запустить программу  rec (из пакета sox)

                            ProcessBuilder procBuilder = null;

                            if(finalM == 1)
                            {
                                procBuilder = new ProcessBuilder ("rec", "-q", "-c", "1", "-r", "16000", "./data/" + strFilename, "trim", "0", Service.config.get ("recordDuration"));
                            } else
                            {
                                procBuilder = new ProcessBuilder ("rec", "-q", "-c", "1", "-r", "16000", "-d", Service.config.get ("microphoneDevice" + finalM), "./data/" + strFilename, "trim", "0", Service.config.get ("recordDuration"));
                            }

                            // перенаправляем стандартный поток ошибок на
                            // стандартный вывод
                            procBuilder.redirectErrorStream (true);

                            httpPOST SendFile = new httpPOST ();

                            // запуск программы
                            Process process = null;
                            try
                            {
                                process = procBuilder.start ();
                            } catch (IOException e)
                            {
                                e.printStackTrace ();  //To change body of catch statement use File | Settings | File Templates.
                            }

                            // читаем стандартный поток вывода
                            // и выводим на экран
                            InputStream stdout = process.getInputStream ();
                            InputStreamReader isrStdout = new InputStreamReader (stdout);
                            BufferedReader brStdout = new BufferedReader (isrStdout);

                            String line = null;
                            try
                            {
                                while ((line = brStdout.readLine ()) != null)
                                {
                                    System.out.println (line);
                                }
                            } catch (IOException e)
                            {
                                e.printStackTrace ();  //To change body of catch statement use File | Settings | File Templates.
                            }

                            // ждем пока завершится вызванная программа
                            // и сохраняем код, с которым она завершилась в
                            // в переменную exitVal
                            try
                            {
                                int exitVal = process.waitFor ();
                            } catch (InterruptedException e)
                            {
                                e.printStackTrace ();  //To change body of catch statement use File | Settings | File Templates.
                            }

                            // Перекодируем в FLAC
                            FLAC_FileEncoder encoder1 = new FLAC_FileEncoder ();
                            File infile = outputFile;
                            File outfile = new File ("./data/" + strFilename + ".flac");
                            encoder1.useThreads (true);
                            encoder1.encode (infile, outfile);

                            // передаем на обработку в гугл
                            String googleSpeechAPIResponse = SendFile.postFile (System.getProperty ("user.dir") + "/data/" + strFilename + ".flac");

                            // debug
                            if(!googleSpeechAPIResponse.contains ("\"utterance\":"))
                            {
                                // System.err.println("[record] Recognizer: No Data");
                            } else
                            {
                                // Include -> System.out.println(wGetResponse); // to view the Raw output
                                int startIndex = googleSpeechAPIResponse.indexOf ("\"utterance\":") + 13; //Account for term "utterance":"<TARGET>","confidence"
                                int stopIndex = googleSpeechAPIResponse.indexOf (",\"confidence\":") - 1; //End position
                                String command = googleSpeechAPIResponse.substring (startIndex, stopIndex);

                                // Determine Confidence
                                startIndex = stopIndex + 15;
                                stopIndex = googleSpeechAPIResponse.indexOf ("}]}") - 1;
                                double confidence = Double.parseDouble (googleSpeechAPIResponse.substring (startIndex, stopIndex));

                                log.info ("[data] Utterance : " + command.toUpperCase ());
                                log.info ("[data] Confidence Level: " + (confidence * 100));

                                if(confidence * 100 > 65)
                                {
                                    if(command.contains ("система"))
                                    {
                                        try
                                        {
                                            ResultSet rs = Service.sql.select("SELECT name, command, param FROM modules WHERE enabled='1'");

                                            while (rs.next())
                                            {
                                                String name = rs.getString("name");
                                                String comm = rs.getString("command");
                                                String param = rs.getString("param");

                                                if (command.contains(comm)) {

                                                    if (busy) {
                                                        log.info("[command] System is busy. Skipping.");
                                                        break;
                                                    }

                                                    busy = true;

                                                    if(!Service.config.get("silence").equals("1"))
                                                    {
                                                        finalClip.setFramePosition(0);
                                                        finalClip.start();
                                                    }

                                                    log.info("[command] Got \""+command+"\" command");

                                                    MapMessage message = Service.session.createMapMessage ();

                                                    message.setString ("text", command);
                                                    message.setDouble ("confidence", confidence * 100);
                                                    message.setStringProperty ("qpid.subject", "event.record.recognized");

                                                    Service.messageProducer.send (message);

                                                    try {

                                                        Class cl = Class.forName("ru.iris.modules." + name);
                                                        Module execute = (Module) cl.newInstance();
                                                        execute.run(param);

                                                        Thread.sleep(1000);

                                                        busy = false;

                                                    } catch (Exception e) {
                                                        log.info("[module] Error at loading module " + name + " with params \"" + param + "\"!");
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }

                                            rs.close();

                                        } catch (JMSException e)
                                        {
                                            e.printStackTrace ();  //To change body of catch statement use File | Settings | File Templates.
                                        } catch (SQLException e) {
                                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                                        }
                                    }
                                }
                            }

                            // Подчищаем за собой
                            try
                            {
                                outputFile.delete ();
                                outfile.delete ();
                                infile.delete ();
                            } catch (Exception e)
                            {
                            }

                            /////////////////////////////////
                        }
                    }
                }).start ();

                // Пауза в 1с перед запуском следующего потока
                try
                {
                    Thread.sleep (1000);
                } catch (InterruptedException e)
                {
                    e.printStackTrace ();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
    }
}
