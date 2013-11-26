package ru.iris.devices;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 10.09.13
 * Time: 13:32
 * License: GPL v3
 */

import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.Config;
import ru.iris.common.I18N;
import ru.iris.common.messaging.ServiceChecker;
import ru.iris.common.messaging.model.ServiceAdvertisement;
import ru.iris.common.messaging.model.ServiceStatus;
import ru.iris.devices.zwave.ZWaveService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

public class Service {

    public static ServiceChecker serviceChecker;
    public static final UUID serviceId = UUID.fromString("444b3e75-7c0c-4d6e-a1f3-f373ef7f6002");
    public static ServiceAdvertisement advertisement = new ServiceAdvertisement();

    private static Logger log = LoggerFactory.getLogger(Service.class);

    public static void main(String[] args) throws IOException, SQLException {

        DOMConfigurator.configure("conf/log4j.xml");
        I18N i18n = new I18N();

        serviceChecker = new ServiceChecker(serviceId, advertisement.set(
                "Devices", serviceId, ServiceStatus.STARTUP));

        Map<String, String> config = new Config().getConfig();


        log.info(i18n.message("iris.devices.service.starting"));

        if (config.get("zwaveEnabled").equals("1")) {
            log.info(i18n.message("zwave.zwave.support.is.enabled.starting"));
            new ZWaveService();
        }
        if (config.get("onewireEnabled").equals("1")) {
            log.info(i18n.message("1.wire.1.wire.support.is.enabled.starting"));
        }
    }
}
