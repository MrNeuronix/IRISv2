package ru.iris.common.devices;

import com.google.gson.annotations.Expose;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 27.11.13
 * Time: 15:33
 * License: GPL v3
 */

public class DeviceValue {

    @Expose
    protected String label = "unknown";
    @Expose
    protected String value = "unknown";
    @Expose
    protected String valueType = "unknown";
    @Expose
    protected String valueUnits = "unknown";
    @Expose
    protected boolean isReadonly = false;

    public DeviceValue(String label, String value, boolean isReadonly) {
        this.label = label;
        this.value = value;
        this.isReadonly = isReadonly;
    }

    public DeviceValue(String label, String value, String valueType, String valueUnits, boolean isReadonly) {
        this.label = label;
        this.value = value;
        this.valueType = valueType;
        this.valueUnits = valueUnits;
        this.isReadonly = isReadonly;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValueType() {
        return valueType;
    }

    public String getValueUnits() {
        return valueUnits;
    }

    public void setValueUnits(String valueUnits) {
        this.valueUnits = valueUnits;
    }

    public boolean isReadonly() {
        return isReadonly;
    }

    public void setReadonly(boolean isReadonly) {
        this.isReadonly = isReadonly;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    @Override
    public String toString() {
        return "DeviceValue{" +
                "label='" + label + '\'' +
                ", value='" + value + '\'' +
                ", valueType='" + valueType + '\'' +
                ", valueUnits='" + valueUnits + '\'' +
                ", isReadonly=" + isReadonly +
                '}';
    }
}
