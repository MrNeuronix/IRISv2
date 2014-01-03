package ru.iris.record;

import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.events.Init;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;
import ru.iris.common.messaging.ServiceChecker;
import ru.iris.common.messaging.model.ServiceAdvertisement;
import ru.iris.common.messaging.model.ServiceStatus;

import javax.jms.JMSException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.UUID;

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

    private static I18N i18n = new I18N();
    public static ServiceChecker serviceChecker;
    public static ServiceAdvertisement advertisement = new ServiceAdvertisement();
    public static UUID serviceId = UUID.fromString("444b3e75-7c0c-4d6e-a1f3-f373ef7f6004");

    private static Logger log = LoggerFactory.getLogger(Service.class);

    @Init
    public void init() throws IOException, SQLException, JMSException, URISyntaxException {

        serviceChecker = new ServiceChecker(serviceId, advertisement.set(
                "Record", serviceId, ServiceStatus.STARTUP));

        log.info(i18n.message("iris.record.service.starting"));

        new RecordService();
    }
}
