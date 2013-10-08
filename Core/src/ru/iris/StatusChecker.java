package ru.iris;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;

/**
 * Created with IntelliJ IDEA.
 * Author: Nikolay A. Viguro
 * Date: 04.10.12
 * Time: 13:57
 * License: GPL v3
 */
public class StatusChecker implements Runnable
{
    private static Logger log = LoggerFactory.getLogger (StatusChecker.class.getName ());

    public StatusChecker()
    {
        Thread t = new Thread (this);
        t.start ();
    }

    @Override
    public synchronized void run()
    {
        Message mess;
        MapMessage m = null;

        try {

            while ((mess = Launcher.messageConsumer.receive (0)) != null)
            {
                m = (MapMessage) mess;

                    if(m.getStringProperty("qpid.subject").equals ("status.answer"))
                    {
                        String module = m.getStringProperty("alive");
                        log.info ("[status] Got status answer from "+module);

                        Launcher.sql.doQuery("DELETE FROM MODULESTATUS WHERE NAME='"+module+"'");
                        Launcher.sql.doQuery("INSERT INTO MODULESTATUS (NAME, LASTSEEN) VALUES ('"+module+"',NOW())");
                    }
            }

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }
}
