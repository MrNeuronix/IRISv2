package ru.iris.speak.nuance;

/**
 * Created with IntelliJ IDEA.
 * User: nikolay.viguro
 * Date: 09.09.13
 * Time: 15:04
 * To change this template use File | Settings | File Templates.
 */

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.speak.Service;

import javax.jms.MapMessage;
import javax.jms.Message;
import java.net.URI;

public class NuanceSpeakService implements Runnable {

    Thread t = null;
    private static Logger log = LoggerFactory.getLogger(NuanceSpeakService.class.getName());

    public NuanceSpeakService() throws Exception {
        t = new Thread (this);
        t.start ();
    }

    public Thread getThread()
    {
        return t;
    }

    @Override
    public synchronized void run()
    {

        log.info ("[speak] Service started (TTS: Nuance)");

        Message message = null;
        MapMessage m = null;

        try
        {
            while ((message = Service.messageConsumer.receive (0)) != null)
            {
                m = (MapMessage) message;

                if(m.getString ("qpid.subject").equals ("event.speak"))
                {
                    log.info ("------------- Speak -------------");
                    log.info ("Confidence: " + m.getDouble ("confidence"));
                    log.info ("Text: " + m.getString ("text"));
                    log.info ("-------------------------------\n");

                    /////////////////////

                    // Standard HTTP parameters
                    TTSHTTPClient ttsHttpClient = new TTSHTTPClient();
                    ttsHttpClient.setText(m.getString ("text"));
                    ttsHttpClient.setAPP_ID(Service.config.get("nuanceID"));
                    ttsHttpClient.setAPP_KEY(Service.config.get("nuanceKey"));

                    try {
                        HttpClient httpclient = ttsHttpClient.getHttpClient();
                        URI uri = ttsHttpClient.getURI();
                        HttpPost httppost = ttsHttpClient.getHeader(uri);

                        log.info("executing request " + httppost.getRequestLine());

                        HttpResponse response = httpclient.execute(httppost);

                        ttsHttpClient.processResponse(response);
                    } finally {
                        // When HttpClient instance is no longer needed,
                        // shut down the connection manager to ensure
                        // immediate deallocation of all system resources
                        if(ttsHttpClient != null && ttsHttpClient.httpclient != null)
                            ttsHttpClient.httpclient.getConnectionManager().shutdown();
                    }

                    /////////////////////
                }
            }

            Service.msg.close ();

        } catch (Exception e)
        {
            e.printStackTrace ();  //To change body of catch statement use File | Settings | File Templates.
            log.info ("Get error! " + m);
        }
    }

}


