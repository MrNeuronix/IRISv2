package ru.iris.video;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 26.10.13
 * Time: 20:32
 * License: GPL v3
 */

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ro.fortsoft.pf4j.Plugin;
import ro.fortsoft.pf4j.PluginWrapper;

public class Service extends Plugin {

    private static Logger log = LogManager.getLogger(Service.class);

    public Service (PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start()
    {
        log.info("[Plugin] iris-video plugin started!");

        new VideoService();
    }

    @Override
    public void stop() {
        log.info("[Plugin] iris-video plugin stopped!");
    }
}
