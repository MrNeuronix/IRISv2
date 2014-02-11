package ru.iris.common.messaging.model.devices;

import com.google.gson.annotations.Expose;
import ru.iris.common.database.model.devices.Device;
import ru.iris.common.messaging.model.Advertisement;

import java.util.Map;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 19.11.13
 * Time: 11:34
 * License: GPL v3
 */
public class ResponseDeviceInventoryAdvertisement extends Advertisement {
    /**
     * Devices Map
     */
    @Expose
    private Map<?, Device> devices;

    public ResponseDeviceInventoryAdvertisement set(Map<?, Device> devices) {
        this.devices = devices;
        return this;
    }

    public Map<?, Device> getDevices() {
        return devices;
    }

    public void setDevices(Map<String, Device> devices) {
        this.devices = devices;
    }
}
