package ru.phsystems.irisv2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 06.12.12
 * Time: 21:21
 * License: GPL v3
 */

public class Service
{

    private static Logger log = LoggerFactory.getLogger (Service.class);

    public static void main(String[] args) throws IOException
    {

        log.info ("----------------------------------------");
        log.info ("----       IRISv2 is starting       ----");
        log.info ("----------------------------------------");

        // Run a webserver
        Process web = Runtime.getRuntime ().exec ("java -jar Web.jar");
        Process speak = Runtime.getRuntime ().exec ("java -jar Speak.jar");
    }
}
