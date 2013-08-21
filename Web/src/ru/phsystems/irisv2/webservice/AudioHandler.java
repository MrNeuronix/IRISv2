package ru.phsystems.irisv2.webservice;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 05.12.12
 * Time: 21:32
 * License: GPL v3
 *
 *    Этот класс является proxy для mjpeg потока с камер
 *
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class AudioHandler extends HttpServlet {

    private static Logger log = LoggerFactory.getLogger(AudioHandler.class.getName());

    public AudioHandler() {
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("audio/x-wav");
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "Keep-Alive");

        //////////////////////////////////////

        InputStream in = null;
        ServletOutputStream out = null;

        try {

            log.info("[audio] Get stream!");

            // URL = http://localhost:8080/control/audio?cam=10
            URL cam = new URL("http://192.168.10." + request.getParameter("cam") + "/audio.cgi");
            URLConnection uc = cam.openConnection();
            out = response.getOutputStream();

            in = uc.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(in);

            byte[] bytes = new byte[8192];
            int bytesRead;

            BufferedOutputStream bos = null;
            bos = new BufferedOutputStream(out);

            int count = 0;

            while ((bytesRead = bis.read(bytes)) != -1) {
                bos.write(bytes, 0, bytesRead);
                bos.flush();
                count++;
                log.info("COUNT: " + count + " BYTES: " + bytesRead);
            }

            System.err.println("READ = -1!");

        } catch (IOException ex) {
            // Disconnect detected
            log.info("[audio " + request.getParameter("cam") + "] Audio client disconnected");
            // Прерываем поток, иначе передача не будет остановена
            Thread.currentThread().interrupt();
        }
    }
}







