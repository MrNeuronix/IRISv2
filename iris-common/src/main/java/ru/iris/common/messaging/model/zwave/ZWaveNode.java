package ru.iris.common.messaging.model.zwave;

import com.google.gson.annotations.Expose;
import ru.iris.common.devices.ZWaveDevice;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 19.11.13
 * Time: 11:34
 * License: GPL v3
 */
public class ZWaveNode {

    @Expose
    protected ZWaveDevice device;

    public ZWaveNode(ZWaveDevice device) {
        this.device = device;
    }

    public ZWaveDevice getDevice() {
        return device;
    }

    public void setDevice(ZWaveDevice device) {
        this.device = device;
    }

    @Override
    public String toString() {
        return "ZWaveNode{}";
    }
}
