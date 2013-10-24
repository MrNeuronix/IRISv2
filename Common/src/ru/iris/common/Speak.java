package ru.iris.common;

import org.apache.qpid.AMQException;
import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.MessageProducer;
import javax.jms.Session;
import java.net.URISyntaxException;

/**
 * Created with IntelliJ IDEA.
 * User: nikolay.viguro
 * Date: 10.09.13
 * Time: 10:49
 * To change this template use File | Settings | File Templates.
 */
public class Speak {

    private static MessageProducer messageProducer;
    private static Session session;
    private static Logger log = LoggerFactory.getLogger(Speak.class);

    public static void add(String text) throws AMQException, JMSException, URISyntaxException {

        I18N i18n = new I18N();
        Messaging msg = new Messaging ();
        @NonNls MapMessage message = null;
        session = msg.getSession ();
        messageProducer = msg.getProducer ();

        try {
            message = session.createMapMessage();

            message.setStringProperty("text", text);
            message.setDoubleProperty("confidence", 100);
            message.setStringProperty ("qpid.subject", "event.speak");

            messageProducer.send(message);
        } catch (JMSException e) {
            log.info(i18n.message("error.failed.speak.0", message));  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
