package ru.iris.speak;

import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.Config;
import ru.iris.common.I18N;
import ru.iris.common.SQL;
import ru.iris.common.Speak;
import ru.iris.common.messaging.ServiceChecker;
import ru.iris.common.messaging.model.ServiceAdvertisement;
import ru.iris.common.messaging.model.ServiceCapability;
import ru.iris.common.messaging.model.ServiceStatus;
import ru.iris.speak.google.GoogleSpeakService;
import ru.iris.speak.voicerss.VoiceRSSSpeakService;

import java.util.Map;
import java.util.UUID;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 05.12.12
 * Time: 21:32
 */

public class Service {

    public static Map<String, String> config;
    public static SQL sql;
    private static I18N i18n = new I18N();
    public static ServiceChecker serviceChecker;
    public static UUID serviceId = UUID.fromString("444b3e75-7c0c-4d6e-a1f3-f373ef7f6007");

    private static Logger log = LoggerFactory.getLogger(Service.class);

    public static void main(String[] args) throws Exception {

        DOMConfigurator.configure("conf/etc/log4j.xml");

        serviceChecker = new ServiceChecker(serviceId, new ServiceAdvertisement(
                "Speak", serviceId, ServiceStatus.STARTUP,
                new ServiceCapability[]{ServiceCapability.SPEAK}));

        Config cfg = new Config();
        config = cfg.getConfig();
        sql = new SQL();

        Speak speak = new Speak();

        log.info(i18n.message("iris.speak.service.starting"));

        if (config.get("ttsEngine").equals("google")) {
            new GoogleSpeakService();
            speak.say(i18n.message("syth.voice.launched"));
        } else if (config.get("ttsEngine").equals("voicerss")) {
            new VoiceRSSSpeakService();
            speak.say(i18n.message("voice.synth.voicerss.launched"));
        } else {
            log.info(i18n.message("speak.no.tts.system.specified.in.config.file"));
        }
    }
}
