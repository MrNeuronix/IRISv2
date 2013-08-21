package ru.phsystems.irisv2.speak;
/**
 * IRIS-X Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 08.09.12
 * Time: 18:36
 * License: GPL v3
 */

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Класс отвечает за озвучивание переданной в POST фразы

public class SpeakHandler extends HttpServlet
{

    public Properties prop = null;

    public SpeakHandler() throws IOException
    {

        prop = new Properties ();
        InputStream is = new FileInputStream ("./conf/main.property");
        prop.load (is);

    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.setContentType ("text/html");
        response.setStatus (HttpServletResponse.SC_OK);
        request.setCharacterEncoding ("UTF-8");
        response.setCharacterEncoding ("UTF-8");

        ExecutorService exs = Executors.newFixedThreadPool (10);
        Synthesizer speak = new Synthesizer (exs);

        String words = request.getParameter ("words");

        System.err.println ("[voice] Speaking: " + words);
        speak.setAnswer (words);

        try
        {
            exs.submit (speak).get ();
        } catch (InterruptedException e)
        {
            e.printStackTrace ();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ExecutionException e)
        {
            e.printStackTrace ();  //To change body of catch statement use File | Settings | File Templates.
        }

        response.getWriter ().println ("done");
    }
}