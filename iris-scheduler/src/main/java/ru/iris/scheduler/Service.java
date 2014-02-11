package ru.iris.scheduler;

import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.events.Init;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 02.10.13
 * Time: 16:32
 */

@PluginImplementation
public class Service implements SchedulerPlugin {

    private static Logger log = LogManager.getLogger(Service.class);

    @Init
    public void init() {

        log.info("Scheduler sevice started");
        new ScheduleService();
    }
}
