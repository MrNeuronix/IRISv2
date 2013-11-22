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

import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.messaging.ServiceChecker;
import ru.iris.common.messaging.model.ServiceAdvertisement;
import ru.iris.common.messaging.model.ServiceCapability;
import ru.iris.common.messaging.model.ServiceStatus;

import java.util.UUID;

public class Service {

    private static Logger log = LoggerFactory.getLogger(Service.class);
    private static ServiceChecker serviceChecker;
    public static final UUID serviceId = UUID.fromString("444b3e75-7c0c-4d6e-a1f3-f373ef7f6008");

    public static void main(String[] args) throws Exception {

        DOMConfigurator.configure("conf/etc/log4j.xml");

        serviceChecker = new ServiceChecker(serviceId, new ServiceAdvertisement(
                "Video", serviceId, ServiceStatus.STARTUP,
                new ServiceCapability[]{ServiceCapability.SEE}));

        new VideoService();
    }
}
