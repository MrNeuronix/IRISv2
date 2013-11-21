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
public class ZWavePolling extends ZWaveNode {

    @Expose
    private boolean state;

    /**
     * Default constructor for de-serialisation.
     */
    public ZWavePolling(ZWaveDevice device, boolean state) {
        super(device);
        this.state = state;
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
