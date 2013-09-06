package ru.iris.speak;
/**
 * IRIS-X Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 08.09.12
 * Time: 18:36
 * License: GPL v3
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Класс отвечает за озвучивание переданной в POST фразы

public class SpeakHandler
{

    public Properties prop = null;

    public SpeakHandler() throws IOException
    {

        prop = new Properties ();
        InputStream is = new FileInputStream ("./conf/main.property");
        prop.load (is);

        // TODO Make qpid complaince
        ExecutorService exs = Executors.newFixedThreadPool (10);
        Synthesizer speak = new Synthesizer (exs);

        String words = "";

        System.err.println ("[voice] Speaking: " + words);
        speak.setAnswer (words);

        try
        {
            exs.submit (speak).get ();
        } catch (InterruptedException e)
        {
            e.printStackTrace ();
        } catch (ExecutionException e)
        {
            e.printStackTrace ();
        }
    }
}