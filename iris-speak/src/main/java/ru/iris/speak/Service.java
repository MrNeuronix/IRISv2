package ru.iris.speak;

import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.events.Init;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.Config;
import ru.iris.common.SQL;
import ru.iris.common.Speak;
import ru.iris.common.messaging.ServiceChecker;
import ru.iris.common.messaging.model.service.ServiceAdvertisement;
import ru.iris.common.messaging.model.service.ServiceStatus;
import ru.iris.speak.google.GoogleSpeakService;

import java.util.UUID;

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

    public static ServiceChecker serviceChecker;
    public static ServiceAdvertisement advertisement = new ServiceAdvertisement();
    public static final UUID serviceId = UUID.fromString("444b3e75-7c0c-4d6e-a1f3-f373ef7f6007");
    private static SQL sql = new SQL();

    public static SQL getSQL()
    {
        return sql;
    }

    private static Logger log = LogManager.getLogger(Service.class);

    @Init
    public void init() throws Exception {

        serviceChecker = new ServiceChecker(serviceId, advertisement.set(
                "Speak", serviceId, ServiceStatus.STARTUP));

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
