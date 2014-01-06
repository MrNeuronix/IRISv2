package ru.iris.scheduler;

import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.events.Init;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.SQL;
import ru.iris.common.messaging.ServiceChecker;
import ru.iris.common.messaging.model.ServiceAdvertisement;
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

@PluginImplementation
public class Service implements SchedulerPlugin {

    public static ServiceChecker serviceChecker;
    public static ServiceAdvertisement advertisement = new ServiceAdvertisement();
    public static UUID serviceId = UUID.fromString("444b3e75-7c0c-4d6e-a1f3-f373ef7f6006");
    private static Logger log = LogManager.getLogger(Service.class);
    private static SQL sql = new SQL();

    public static SQL getSQL()
    {
        return sql;
    }

    @Init
    public void init() throws Exception {

        serviceChecker = new ServiceChecker(serviceId, advertisement.set(
                "Scheduler", serviceId, ServiceStatus.STARTUP));

        log.info("Scheduler sevice started");

        new ScheduleService();
    }
}
