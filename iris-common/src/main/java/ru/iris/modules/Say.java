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
import ru.iris.common.Module;
import ru.iris.common.Speak;

import javax.jms.JMSException;
import java.net.URISyntaxException;

public class Say implements Module {

    public Say() {
    }

    public void run(String arg) throws JMSException {

        Speak speak = new Speak();
        try {
            speak.add(arg);
        } catch (AMQException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}