package ru.iris.common.database.model;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 03.12.13
 * Time: 13:57
 * License: GPL v3
 */

import com.google.gson.annotations.Expose;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;


@Entity
@Table(name="speaks")
public class Speaks {

    @Id
    private long id;

    @Expose
    @Column(columnDefinition = "timestamp")
    private Timestamp speakdate;

    @Expose
    @Column(columnDefinition = "TEXT")
    private String text;

    @Expose
    private Double confidence;

    @Expose
    private String device;

    @Expose
    private boolean isActive;

    // Default
    public Speaks() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Timestamp getSpeakdate() {
        return speakdate;
    }

    public void setSpeakdate(Timestamp speakdate) {
        this.speakdate = speakdate;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
}