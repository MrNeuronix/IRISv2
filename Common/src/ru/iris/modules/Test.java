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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.Messaging;
import ru.iris.common.Module;
import ru.iris.common.SQL;

import javax.jms.*;
import java.net.URISyntaxException;

// For testing module system

public class Test implements Module {

    private static Logger log = LoggerFactory.getLogger(Test.class.getName());
    public static SQL sql;
    public static MessageConsumer messageConsumer;
    public static MessageProducer messageProducer;
    public static Messaging msg;
    public static Session session;

    public Test()
    {
        try {
            msg = new Messaging();
        } catch (JMSException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (URISyntaxException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (AMQException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        messageConsumer = msg.getConsumer ();
        messageProducer = msg.getProducer ();
        session = msg.getSession ();
    }

    public void run(String arg) throws JMSException {
        test();
    }

    private void test() throws JMSException {

        log.info("[test] Test");

        MapMessage message = session.createMapMessage();

        message.setStringProperty("text", "Тест модульной системы!");
        message.setDoubleProperty("confidence", 100);
        message.setStringProperty ("qpid.subject", "event.speak");

        messageProducer.send (message);
    }
}