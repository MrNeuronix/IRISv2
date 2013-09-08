package ru.iris;

import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.SQL;

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
    public static SQL sql;
    private static Logger log = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) throws IOException, SQLException, InstantiationException, IllegalAccessException {

        // Запускаем TCP сервер H2
        Server server = Server.createTcpServer().start();

        // Запускаем сервис REST
        Process rest = Runtime.getRuntime().exec("java -jar Rest.jar");

        // Запускаем синтез и захват звука
        Process speak = Runtime.getRuntime().exec("java -jar Speak.jar");
    }
}
