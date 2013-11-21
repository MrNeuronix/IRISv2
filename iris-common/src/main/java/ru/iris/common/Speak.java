package ru.iris.common;

import org.apache.qpid.AMQException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.SpeakAdvertisement;

import javax.jms.JMSException;
import java.net.URISyntaxException;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: nikolay.viguro
 * Date: 10.09.13
 * Time: 10:49
 * To change this template use File | Settings | File Templates.
 */
public class Speak {

    private static Logger log = LoggerFactory.getLogger(Speak.class);

    public void add(String text) throws AMQException, JMSException, URISyntaxException {

        I18N i18n = new I18N();

        try {
            new JsonMessaging(UUID.randomUUID()).broadcast("event.speak", new SpeakAdvertisement(text, 100.0));
        } catch (JMSException e) {
            log.info(i18n.message("error.failed.speak.0", text));
        }
    }
}
