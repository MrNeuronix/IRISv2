package ru.iris;

import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 08.09.13
 * Time: 22:52
 * License: GPL v3
 */
public class Launcher {

    public static HashMap<String, String> config;
    private static Logger log = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) throws IOException, SQLException {

        log.info ("----------------------------------------");
        log.info ("----       IRISv2 is starting       ----");
        log.info ("----------------------------------------");

        // Запускаем TCP сервер H2
        Server server = Server.createTcpServer().start();

        // Запускаем сервис REST;
        runModule("java -jar Rest.jar");

        // Запускаем захват звука
        runModule("java -jar Record.jar");

        // Запускаем синтез звука
        runModule("java -jar Speak.jar");
    }

    private static void runModule(String cmd) throws IOException {

        String[] splited = cmd.split("\\s+");

        ProcessBuilder builder = new ProcessBuilder(splited).redirectOutput(ProcessBuilder.Redirect.INHERIT).redirectErrorStream(true);;
        Process process = builder.start();

        final InputStream stdout = process.getInputStream();

        new Thread(new Runnable(){
            public void run(){

                BufferedReader reader = new BufferedReader (new InputStreamReader(stdout));
                String line = null;

                try {

                    while ((line = reader.readLine()) != null) {
                        log.info(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }).start();
    }
}
