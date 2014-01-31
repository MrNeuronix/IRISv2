package ru.iris.record;

import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.events.Init;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.Config;

import javax.jms.JMSException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 05.12.12
 * Time: 21:32
 */


@PluginImplementation
public class Service implements RecordPlugin {

    private Config config = new Config();
    private static Logger log = LogManager.getLogger(Service.class);

    @Init
    public void init() throws IOException, SQLException, JMSException, URISyntaxException {

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
}
