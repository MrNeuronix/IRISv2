package ru.iris.common.database.model;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 22.01.14
 * Time: 21:16
 * License: GPL v3
 */

import com.google.gson.annotations.Expose;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name="modulestatus")
public class ModuleStatus {

    @Id
    private long id;

    @Expose
    @Column(columnDefinition = "timestamp")
    private Timestamp lastseen;

    @Expose
    private String name;

    @Expose
    private String state;

    // Default
    public ModuleStatus() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Timestamp getLastseen() {
        return lastseen;
    }

    public void setLastseen(Timestamp lastseen) {
        this.lastseen = lastseen;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}