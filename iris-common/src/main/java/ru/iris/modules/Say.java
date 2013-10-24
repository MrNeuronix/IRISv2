package ru.iris.modules;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 14.09.13
 * Time: 18:30
 * License: GPL v3
 */
import org.apache.qpid.AMQException;
import org.jetbrains.annotations.NonNls;
import ru.iris.common.Messaging;
import ru.iris.common.Module;

import javax.jms.*;
import java.net.URISyntaxException;

public class Say implements Module {

    public static MessageConsumer messageConsumer;
    public static MessageProducer messageProducer;
    public static Messaging msg;
    public static Session session;

    public Say()
    {
        try {
            msg = new Messaging();
        } catch (JMSException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (AMQException e) {
            e.printStackTrace();
        }
        messageConsumer = msg.getConsumer ();
        messageProducer = msg.getProducer ();
        session = msg.getSession ();
    }

    public void run(String arg) throws JMSException {

        @NonNls MapMessage message = session.createMapMessage();

        message.setStringProperty("text", arg);
        message.setDoubleProperty("confidence", 100);
        message.setStringProperty ("qpid.subject", "event.speak");

        messageProducer.send (message);
    }
}