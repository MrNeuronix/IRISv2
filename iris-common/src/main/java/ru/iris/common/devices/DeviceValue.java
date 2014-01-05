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

    public DeviceValue(String label, String value) {
        this.label = label;
        this.value = value;
    }

    public DeviceValue(String label, String value, String valueType, String valueUnits) {
        this.label = label;
        this.value = value;
        this.valueType = valueType;
        this.valueUnits = valueUnits;
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

    @Override
    public String toString() {
        return "DeviceValue{" +
                "label='" + label + '\'' +
                ", value='" + value + '\'' +
                ", valueType='" + valueType + '\'' +
                ", valueUnits='" + valueUnits + '\'' +
                '}';
    }
}
