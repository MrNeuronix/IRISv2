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
public class SpeakRecognizedAdvertisement extends Advertisement {
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

    public SpeakRecognizedAdvertisement(String text, double confidence) {
        this.text = text;
        this.confidence = confidence;
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
}
