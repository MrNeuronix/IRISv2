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
import org.apache.qpid.AMQException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.Config;
import ru.iris.common.I18N;
import ru.iris.common.Messaging;
import ru.iris.common.SQL;
import ru.iris.common.messaging.ServiceChecker;
import ru.iris.common.messaging.model.ServiceAdvertisement;
import ru.iris.common.messaging.model.ServiceCapability;
import ru.iris.common.messaging.model.ServiceStatus;
import ru.iris.devices.zwave.ZWaveService;

import javax.jms.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

public class Service {

    public static Map<String, String> config;
    public static SQL sql;
    public static MessageConsumer messageConsumer;
    public static MessageProducer messageProducer;
    public static Messaging msg;
    public static Session session;
    public static ServiceChecker ServiceState;
    public static UUID serviceId = UUID.fromString("444b3e75-7c0c-4d6e-a1f3-f373ef7f6002");

    private static Logger log = LoggerFactory.getLogger(Service.class);

    public static void main(String[] args) throws IOException, SQLException, AMQException, JMSException, URISyntaxException {
        DOMConfigurator.configure("conf/etc/log4j.xml");
        I18N i18n = new I18N();

        ServiceState = new ServiceChecker(serviceId, new ServiceAdvertisement(
                "Devices", serviceId, ServiceStatus.STARTUP,
                new ServiceCapability[]{ServiceCapability.CONTROL, ServiceCapability.SENSE}));

        config = new Config().getConfig();
        sql = new SQL();

        msg = new Messaging();
        messageConsumer = msg.getConsumer();
        messageProducer = msg.getProducer();
        session = msg.getSession();

        log.info("[iris] ----------------------------------");
        log.info(i18n.message("iris.devices.service.starting"));
        log.info("[iris] ----------------------------------");

        if (config.get("zwaveEnabled").equals("1")) {
            log.info(i18n.message("zwave.zwave.support.is.enabled.starting"));
            new ZWaveService();
        }
        if (config.get("onewireEnabled").equals("1")) {
            log.info(i18n.message("1.wire.1.wire.support.is.enabled.starting"));
        }
    }
}
