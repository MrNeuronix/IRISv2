package ru.iris.common.messaging.model.zwave;

import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;
import ru.iris.common.messaging.model.Advertisement;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 19.11.13
 * Time: 11:34
 * License: GPL v3
 */
public class ResponseZWaveDeviceInventoryAdvertisement extends Advertisement {
    /**
     * Device UUID
     */
    @Expose
    private JsonElement device;

    public ResponseZWaveDeviceInventoryAdvertisement(JsonElement device) {
        this.device = device;
    }

    public JsonElement getDevice() {
        return device;
    }

    public void setDevice(JsonElement device) {
        this.device = device;
    }

    @Override
    public String toString() {
        return "ResponseZWaveDeviceInventoryAdvertisement {" +
                "zwaveDevice=" + device +
                '}';
    }
}
