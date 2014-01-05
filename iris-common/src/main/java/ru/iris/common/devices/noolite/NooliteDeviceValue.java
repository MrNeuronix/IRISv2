package ru.iris.common.devices.noolite;

import ru.iris.common.devices.DeviceValue;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 27.11.13
 * Time: 15:33
 * License: GPL v3
 */

public class NooliteDeviceValue extends DeviceValue {

    public NooliteDeviceValue(String label, String value) {
        super(label, value);
    }

    public NooliteDeviceValue(String label, String value, String valueType, String valueUnits) {
        super(label, value, valueType, valueUnits);
    }

    @Override
    public String toString() {
        return "NooliteDeviceValue{" +
                "label='" + label + '\'' +
                ", value='" + value + '\'' +
                ", valueType='" + valueType + '\'' +
                ", valueUnits='" + valueUnits + '\'' +
                '}';
    }
}
