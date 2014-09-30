package ru.iris.ai;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 05.01.14
 * Time: 19:21
 * License: GPL v3
 */

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ro.fortsoft.pf4j.Plugin;
import ro.fortsoft.pf4j.PluginWrapper;
import ru.iris.ai.witai.WitAiService;
import ru.iris.common.Config;

public class Service extends Plugin {

    private static Logger log = LogManager.getLogger(Service.class);

    public Service (PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {

        log.info("[Plugin] iris-ai plugin started!");

        Config cfg = new Config();

        if (cfg.getConfig().get("witaiEnabled").equals("1")) {
            new WitAiService();
        } else {
            log.info("No AI specified in config file");
        }
    }

    @Override
    public void stop() {
        log.info("[Plugin] iris-ai plugin stopped!");
    }
}
