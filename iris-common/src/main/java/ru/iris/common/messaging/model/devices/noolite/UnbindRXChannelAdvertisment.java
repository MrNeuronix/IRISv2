package ru.iris.common.messaging.model.devices.noolite;

import com.google.gson.annotations.Expose;
import ru.iris.common.messaging.model.Advertisement;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 18.01.13
 * Time: 21:31
 * License: GPL v3
 */
public class UnbindRXChannelAdvertisment extends Advertisement {
    /**
     * Device UUID
     */
    @Expose
    private String deviceUUID;

    /**
     * Channel.
     */
    @Expose
    private int channel;

    public UnbindRXChannelAdvertisment set(String deviceUUID, int channel) {
        this.channel = channel;
        this.deviceUUID = deviceUUID;
        return this;
    }

    public String getDeviceUUID() {
        return deviceUUID;
    }

    public void setDeviceUUID(String deviceUUID) {
        this.deviceUUID = deviceUUID;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    @Override
    public String toString() {
        return "UnbindRXChannelAdvertisment { UUID: " + deviceUUID + ", channel: " + channel + " }";
    }
}
