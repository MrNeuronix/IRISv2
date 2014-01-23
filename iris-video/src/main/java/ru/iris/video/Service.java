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

import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.events.Init;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.messaging.ServiceCheckEmitter;

import java.util.UUID;

@PluginImplementation
public class Service implements VideoPlugin {

    private static Logger log = LogManager.getLogger(Service.class);
    public static ServiceCheckEmitter serviceCheckEmitter;
    public static final UUID serviceId = UUID.fromString("444b3e75-7c0c-4d6e-a1f3-f373ef7f6008");

    @Init
    public void init() throws Exception {

        serviceCheckEmitter = new ServiceCheckEmitter("Video");

        new VideoService();
    }
}
