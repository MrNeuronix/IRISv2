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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ro.fortsoft.pf4j.Plugin;
import ro.fortsoft.pf4j.PluginWrapper;
import ru.iris.common.Config;
import ru.iris.devices.noolite.NooliteRXService;
import ru.iris.devices.noolite.NooliteTXService;
import ru.iris.devices.zwave.ZWaveService;
import java.util.Map;

public class Service extends Plugin {

    private static Logger log = LogManager.getLogger(Service.class);

    public Service (PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start()
    {
        Map<String, String> config = new Config().getConfig();

        log.info("[Plugin] iris-devices plugin started!");

        // Generic device functions
        new CommonDeviceService();

        if (config.get("zwaveEnabled").equals("1")) {
            log.info("ZWave support is enabled. Starting");
            new ZWaveService();
        }
        if (config.get("nooliteEnabled").equals("1")) {
            log.info("NooLite support is enabled. Starting");
            if (config.get("nooliteTXPresent").equals("1")) {
                log.info("NooLite TX support is enabled. Starting");
                new NooliteTXService();
            }
            if (config.get("nooliteRXPresent").equals("1")) {
                log.info("NooLite RX support is enabled. Starting");
                new NooliteRXService();
            }
        }
    }

    @Override
    public void stop() {
        log.info("[Plugin] iris-devices plugin stopped!");
    }
}
