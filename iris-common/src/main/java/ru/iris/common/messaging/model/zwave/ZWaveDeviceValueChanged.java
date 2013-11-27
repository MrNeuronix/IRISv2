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
public class ZWaveDeviceValueChanged {
    /**
     * Zwave device
     */
    @Expose
    private ZWaveDevice device;

    @Expose
    private String label;

    @Expose
    private String value;

    public ZWaveDeviceValueChanged set(ZWaveDevice device, String label, String value) {
        this.device = device;
        this.label = label;
        this.value = value;
        return this;
    }

    /**
     * Default constructor for de-serialisation.
     */
    public ZWaveDeviceValueChanged() {
    }

    public ZWaveDevice getDevice() {
        return device;
    }

    public void setDevice(ZWaveDevice device) {
        this.device = device;
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

    @Override
    public String toString() {
        return "ZWaveDeviceValueChanged{" +
                "zwaveDevice=" + device.getInternalName() +
                '}';
    }
}
