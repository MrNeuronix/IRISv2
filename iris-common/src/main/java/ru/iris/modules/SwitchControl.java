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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;
import ru.iris.common.Messaging;
import ru.iris.common.Module;
import ru.iris.common.SQL;

import javax.jms.*;
import java.net.URISyntaxException;

// For testing module system

public class SwitchControl implements Module {

    private static Logger log = LoggerFactory.getLogger(SwitchControl.class.getName());
    private static SQL sql;
    private static MessageConsumer messageConsumer;
    private static MessageProducer messageProducer;
    private static Messaging msg;
    private static Session session;

    private static I18N i18n = new I18N();

    public SwitchControl()
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

    public void run(@NonNls String arg) throws JMSException {
        if (arg.equals("enable")) enableSW();
        if (arg.equals("disable")) disableSW();
    }

    private void enableSW() throws JMSException {

        log.info(i18n.message("switchcontol.switch.all.devices.to.on.state"));

        @NonNls MapMessage message = session.createMapMessage();

        message.setStringProperty("command", "allon");
        message.setStringProperty ("qpid.subject", "event.devices.setvalue");

        messageProducer.send (message);
    }

    private void disableSW() throws JMSException {

        log.info(i18n.message("switchcontol.switch.all.devices.to.off.state"));

        @NonNls MapMessage message = session.createMapMessage();

        message.setStringProperty("command", "alloff");
        message.setStringProperty ("qpid.subject", "event.devices.setvalue");

        messageProducer.send (message);
    }
}