package ru.iris.common.messaging.model.speak;

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
public class SpeakAdvertisement extends Advertisement {
    /**
     * Text to speak
     */
    @Expose
    private String text;

    /**
     * Confidence
     */
    @Expose
    private double confidence;

    /**
     * Device where we speak
     */
    @Expose
    private String device;

    public SpeakAdvertisement set(String text, double confidence) {
        this.text = text;
        this.confidence = confidence;
        this.device = "all";
        return this;
    }

    public SpeakAdvertisement set(String text, double confidence, String device) {
        this.text = text;
        this.confidence = confidence;
        this.device = device;
        return this;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    @Override
    public String toString() {
        return "SpeakAdvertisement{" +
                "text='" + text + '\'' +
                ", confidence=" + confidence +
                ", device='" + device + '\'' +
                '}';
    }
}
