package ru.iris.events;

import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;
import ru.iris.common.messaging.ServiceChecker;
import ru.iris.common.messaging.model.ServiceAdvertisement;
import ru.iris.common.messaging.model.ServiceCapability;
import ru.iris.common.messaging.model.ServiceStatus;

import java.util.UUID;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 02.10.13
 * Time: 16:32
 */

public class Service {
    private static I18N i18n = new I18N();
    public static ServiceChecker serviceChecker;
    private static Logger log = LoggerFactory.getLogger(Service.class);
    public static final UUID serviceId = UUID.fromString("444b3e75-7c0c-4d6e-a1f3-f373ef7f6003");

    public static void main(String[] args) throws Exception {

        DOMConfigurator.configure("conf/etc/log4j.xml");

        serviceChecker = new ServiceChecker(serviceId, new ServiceAdvertisement(
                "Events", serviceId, ServiceStatus.STARTUP,
                new ServiceCapability[]{ServiceCapability.CONTROL}));

        log.info(i18n.message("iris.events.engine.starting"));

        new EventsService();
    }
}
