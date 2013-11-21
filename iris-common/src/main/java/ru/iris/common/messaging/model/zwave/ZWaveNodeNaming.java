package ru.iris.common.messaging.model.zwave;

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
public class ZWaveNodeNaming extends ZWaveNode {

    public ZWaveNodeNaming(ZWaveDevice device) {
        super(device);
    }

    @Override
    public String toString() {
        return "ZWaveNodeNaming { node: " + super.device.getInternalName() + " }";
    }
}
