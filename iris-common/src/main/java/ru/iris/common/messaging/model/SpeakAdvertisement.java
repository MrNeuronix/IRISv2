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

    public SpeakAdvertisement set(String text, double confidence) {
        this.text = text;
        this.confidence = confidence;
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

    @Override
    public String toString() {
        return "SpeakAdvertisement { text: " + text + ", confidence: " + confidence + " }";
    }
}
