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

import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.events.Init;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.ai.witai.WitAiService;
import ru.iris.common.Config;
import ru.iris.common.SQL;
import ru.iris.common.messaging.ServiceCheckEmitter;
import ru.iris.common.messaging.model.service.ServiceStatus;

@PluginImplementation
public class Service implements AIPlugin {

    private static Logger log = LogManager.getLogger(Service.class);
    public static ServiceCheckEmitter serviceCheckEmitter;
    private static SQL sql = new SQL();

    public static SQL getSQL() {
        return sql;
    }

    @Init
    public void init() throws Exception {

        serviceCheckEmitter = new ServiceCheckEmitter("AI");
        serviceCheckEmitter.setState(ServiceStatus.STARTUP);

        log.info("AI service starting");

        Config cfg = new Config();

        if (cfg.getConfig().get("witaiEnabled").equals("1")) {
            new WitAiService();
        } else {
            log.info("No AI specified in config file");
            serviceCheckEmitter.setState(ServiceStatus.ERROR);
        }
    }
}
