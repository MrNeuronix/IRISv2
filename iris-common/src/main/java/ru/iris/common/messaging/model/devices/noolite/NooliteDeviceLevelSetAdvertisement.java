package ru.iris.common.messaging.model.devices.noolite;

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
public class NooliteDeviceLevelSetAdvertisement extends Advertisement {

    /**
     * Device UUID
     */
    @Expose
    private String deviceUUID;

    /**
     * Label, what value we want change.
     */
    @Expose
    private String label;

    /**
     * Label value.
     */
    @Expose
    private String value;

    public NooliteDeviceLevelSetAdvertisement set(String deviceUUID, String label, String value)
    {
        this.label = label;
        this.value = value;
        this.deviceUUID = deviceUUID;
        return this;
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

    public String getDeviceUUID() {
        return deviceUUID;
    }

    public void setDeviceUUID(String deviceUUID) {
        this.deviceUUID = deviceUUID;
    }

    @Override
    public String toString() {
        return "NooliteDeviceLevelSetAdvertisement { UUID: " + deviceUUID + ", label: " + label + ", value: " + value + " }";
    }
}
