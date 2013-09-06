package ru.iris.restful;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 06.09.12
 * Time: 17:24
 * License: GPL v3
 */

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.Config;
import ru.iris.common.SQL;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;

public class Service
{
    public static HashMap<String, String> config;
    public static SQL sql;
    private static Logger log = LoggerFactory.getLogger (Service.class);
    static final String BASE_URI = "http://192.168.10.150:16101/";

    public static void main(String[] args) throws IOException, SQLException {

        Config cfg = new Config ();
        config = cfg.getConfig ();
        sql = new SQL ();

        try {
            HttpServer server = HttpServerFactory.create(BASE_URI);
            server.start();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
}
}
