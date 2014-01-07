package ru.iris.common.messaging.model.devices.zwave;

import com.google.gson.annotations.Expose;
import ru.iris.common.devices.zwave.ZWaveDevice;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 19.11.13
 * Time: 11:34
 * License: GPL v3
 */
public class ZWavePolling extends ZWaveNode {

    @Expose
    private boolean state;

    /**
     * Default constructor for de-serialisation.
     */
    public ZWavePolling set(ZWaveDevice device, boolean state) {
        super.device = device;
        this.state = state;
        return this;
    }

    public boolean getState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "ZWavePolling{ node: " + super.device.getInternalName() + " state: " + state + " }";
    }
}
