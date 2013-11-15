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
import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.Messaging;
import ru.iris.common.SQL;
import ru.iris.common.messaging.ServiceChecker;
import ru.iris.common.messaging.model.ServiceAdvertisement;
import ru.iris.common.messaging.model.ServiceCapability;
import ru.iris.common.messaging.model.ServiceStatus;

import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import java.util.HashMap;
import java.util.UUID;

public class Service {
    public static HashMap<String, String> config;
    public static SQL sql;
    public static MessageConsumer messageConsumer;
    public static MessageProducer messageProducer;
    @NonNls
    public static Messaging msg;
    public static Session session;

    private static Logger log = LoggerFactory.getLogger(Service.class);

    public static void main(String[] args) throws Exception {

        DOMConfigurator.configure("conf/etc/log4j.xml");

        new VideoService();

        // Check module status
        new ServiceChecker().start(UUID.fromString("444b3e75-7c0c-4d6e-a1f3-f373ef7f6008"), new ServiceAdvertisement(
                "Video", UUID.randomUUID(), ServiceStatus.AVAILABLE,
                new ServiceCapability[]{ServiceCapability.SEE}));


    }
}
