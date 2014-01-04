package ru.iris.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static Logger log = LogManager.getLogger(Speak.class);
    private static SpeakAdvertisement advertisement = new SpeakAdvertisement();

    public void say(String text) throws JMSException, URISyntaxException {

        try {
            JsonMessaging messaging = new JsonMessaging(UUID.randomUUID());
            messaging.broadcast("event.speak", advertisement.set(text, 100.0));
            messaging.close();
        } catch (Exception e) {
            log.info("Error! Failed to speak: "+ text);
        }
    }
}
