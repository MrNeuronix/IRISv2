package ru.phsystems.irisv2.webservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.phsystems.irisv2.common.Config;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 05.12.12
 * Time: 21:32
 */

public class Service {

    public static boolean shutdownCams = false;
    public static long startTime = 0;
    public static HashMap<String, String> config;

    private static Logger log = LoggerFactory.getLogger(Service.class);

    public static void main(String[] args) throws IOException, SQLException {

        startTime = System.currentTimeMillis();

        Config cfg = new Config();
        config = cfg.getConfig();

        log.info("[iris] ----------------------------------");
        log.info("[iris] Web service starting");
        log.info("[iris] ----------------------------------");

        new WebService();
    }
}
