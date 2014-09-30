package ru.iris.events;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ro.fortsoft.pf4j.Plugin;
import ro.fortsoft.pf4j.PluginWrapper;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 02.10.13
 * Time: 16:32
 */

public class Service extends Plugin {

    private static Logger log = LogManager.getLogger(Service.class);

    public Service (PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start()
    {
        log.info("[Plugin] iris-events plugin started!");
        new EventsService();
    }

    @Override
    public void stop() {
        log.info("[Plugin] iris-events plugin stopped!");
    }
}
