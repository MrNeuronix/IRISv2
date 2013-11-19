package ru.iris.common.messaging.model;

import com.google.gson.annotations.Expose;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 19.11.13
 * Time: 11:34
 * License: GPL v3
 */
public class Advertisement {
    /**
     * Internal device identificator (i.e. dimmer/1)
     */
    @Expose
    private String deviceInternal;

    /**
     * Constructor for initializing value object fields.
     *
     * @param deviceInternal         the device internal name
     */
    public Advertisement(String deviceInternal) {
        this.deviceInternal = deviceInternal;;
    }

    /**
     * Default constructor for de-serialisation.
     */
    public Advertisement() {
    }

    public String getDeviceInternal() {
        return deviceInternal;
    }

    public void setDeviceInternal(String deviceInternal) {
        this.deviceInternal = deviceInternal;
    }

    @Override
    public String toString() {
        return "Advertisement{" +
                "deviceInternal=" + deviceInternal +
                '}';
    }
}
