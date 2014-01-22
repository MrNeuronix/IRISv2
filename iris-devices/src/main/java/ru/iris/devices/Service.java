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

import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.events.Init;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.Config;
import ru.iris.common.SQL;
import ru.iris.common.messaging.ServiceChecker;
import ru.iris.common.messaging.model.service.ServiceAdvertisement;
import ru.iris.common.messaging.model.service.ServiceStatus;
import ru.iris.devices.noolite.NooliteRXService;
import ru.iris.devices.noolite.NooliteTXService;
import ru.iris.devices.zwave.ZWaveService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

@PluginImplementation
public class Service implements DevicesPlugin {

    public static ServiceChecker serviceChecker;
    public static final UUID serviceId = UUID.fromString("444b3e75-7c0c-4d6e-a1f3-f373ef7f6002");
    public static ServiceAdvertisement advertisement = new ServiceAdvertisement();

    private static Logger log = LogManager.getLogger(Service.class);

    private static SQL sql = new SQL();

    public static SQL getSQL()
    {
        return sql;
    }

    @Init
    public void init() throws IOException, SQLException {

        serviceChecker = new ServiceChecker(serviceId, advertisement.set(
                "Devices-Common", serviceId, ServiceStatus.STARTUP));

        Map<String, String> config = new Config().getConfig();

        log.info("Device module starting");

        // Generic device functions
        new CommonDeviceService();

        if (config.get("zwaveEnabled").equals("1")) {
            log.info("ZWave support is enabled. Starting");
            new ZWaveService();
        }
        if (config.get("nooliteEnabled").equals("1")) {
            log.info("NooLite support is enabled. Starting");
            if(config.get("nooliteTXPresent").equals("1"))
                new NooliteTXService();
            if(config.get("nooliteRXPresent").equals("1"))
                new NooliteRXService();
        }
    }
}
