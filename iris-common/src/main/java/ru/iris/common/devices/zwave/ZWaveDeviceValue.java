package ru.iris.common.devices.zwave;

import org.zwave4j.ValueId;
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
public class ZWaveDeviceValue extends DeviceValue {

    private ValueId valueId;

    public ZWaveDeviceValue(String label, String value, String valueType, String valueUnits, ValueId valueId, boolean isReadonly) {

        super(label, value, valueType, valueUnits, isReadonly);
        this.valueId = valueId;
    }

    public ZWaveDeviceValue(String label, String value, String valueType, String valueUnits, boolean isReadonly) {
        super(label, value, valueType, valueUnits, isReadonly);
    }

    public ValueId getValueId() {
        return valueId;
    }

    public void setValueId(ValueId valueId) {
        this.valueId = valueId;
    }

    @Override
    public String toString() {
        return "ZWaveDeviceValue{" +
                "label='" + label + '\'' +
                ", value='" + value + '\'' +
                ", valueType='" + valueType + '\'' +
                ", valueUnits='" + valueUnits + '\'' +
                ", valueId=" + valueId +
                ", isReadonly=" + isReadonly +
                '}';
    }
}
