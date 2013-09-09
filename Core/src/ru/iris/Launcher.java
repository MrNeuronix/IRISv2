package ru.iris;

import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

        // Запускаем сервис REST
        Process rest = Runtime.getRuntime().exec("java -jar Rest.jar");

        // Запускаем захват звука
        Process record = Runtime.getRuntime().exec("java -jar Record.jar");

        // Запускаем синтез звука
        Process speak = Runtime.getRuntime().exec("java -jar Speak.jar");
    }
}
