package ru.iris.speak;

import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.events.Init;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.Config;
import ru.iris.common.Speak;
import ru.iris.speak.google.GoogleSpeakService;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 05.12.12
 * Time: 21:32
 */

@PluginImplementation
public class Service implements SpeakPlugin {

    private static Logger log = LogManager.getLogger(Service.class);

    @Init
    public void init() throws Exception {

        Config cfg = new Config();
        Speak speak = new Speak();

        log.info("Speak service starting");

        if (cfg.getConfig().get("ttsEngine").equals("google")) {
            new GoogleSpeakService();
            speak.say("Модуль синтеза речи Гугл запущен!");
        } else {
            log.info("No TTS feed specified in config file");
        }
    }
}
