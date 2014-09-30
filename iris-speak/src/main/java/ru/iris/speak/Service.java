package ru.iris.speak;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ro.fortsoft.pf4j.Plugin;
import ro.fortsoft.pf4j.PluginWrapper;
import ru.iris.common.Config;
import ru.iris.common.Speak;
import ru.iris.speak.google.GoogleSpeakService;

import javax.jms.JMSException;
import java.net.URISyntaxException;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 05.12.12
 * Time: 21:32
 */

public class Service extends Plugin {

    private static Logger log = LogManager.getLogger(Service.class);

    public Service (PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start()
    {
        log.info("[Plugin] iris-speak plugin started!");

        Config cfg = new Config();
        Speak speak = new Speak();

        if (cfg.getConfig().get("ttsEngine").equals("google")) {
            new GoogleSpeakService();
            try {
                speak.say("Модуль синтеза речи Гугл запущен!");
            } catch (JMSException | URISyntaxException e) {
                log.error(e.toString());
            }
        } else {
            log.info("No TTS feed specified in config file");
        }
    }

    @Override
    public void stop() {
        log.info("[Plugin] iris-speak plugin stopped!");
    }
}
