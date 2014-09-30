package ru.iris.record;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ro.fortsoft.pf4j.Plugin;
import ro.fortsoft.pf4j.PluginWrapper;
import ru.iris.common.Config;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 05.12.12
 * Time: 21:32
 */


public class Service extends Plugin {

    private Config config = new Config();
    private static Logger log = LogManager.getLogger(Service.class);

    public Service (PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start()
    {
        log.info("[Plugin] iris-record plugin started!");

        if (config.getConfig().get("recordOnServer").equals("1")) {
            if (config.getConfig().get("recordMethod").equals("internal")) {
                log.info("Internal record service started");
                new DirectRecordService();
            } else if (config.getConfig().get("recordOnServer").equals("external")) {
                log.info("External record service started");
                new ExternalRecordService();
            } else {
                log.error("No record method specified!");
            }
        }
    }

    @Override
    public void stop() {
        log.info("[Plugin] iris-record plugin stopped!");
    }
}
