package ru.iris;

import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;

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
public class StatusChecker implements Runnable {
    private static Logger log = LoggerFactory.getLogger(StatusChecker.class.getName());

    public StatusChecker() {
        Thread t = new Thread(this);
        t.start();
    }

    @Override
    public synchronized void run() {
        Message mess;
        @NonNls MapMessage m = null;

        I18N i18n = new I18N();

        try {

            while ((mess = Launcher.messageConsumer.receive(0)) != null) {
                m = (MapMessage) mess;

                if (m.getStringProperty("qpid.subject").equals("status.answer")) {
                    String module = m.getStringProperty("alive");
                    log.info(i18n.message("status.got.status.answer.from.0", module));

                    Launcher.sql.doQuery("DELETE FROM MODULESTATUS WHERE NAME='" + module + "'");
                    Launcher.sql.doQuery("INSERT INTO MODULESTATUS (NAME, LASTSEEN) VALUES ('" + module + "',NOW())");
                }
            }

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }
}
