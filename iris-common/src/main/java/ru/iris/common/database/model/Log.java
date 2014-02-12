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

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name="log")
public class Log {

    @Id
    private long id;

    @Column(columnDefinition = "timestamp")
    private Timestamp logdate;

    private String level;
    private String message;

    // Default
    public Log() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Timestamp getLogdate() {
        return logdate;
    }

    public void setLogdate(Timestamp logdate) {
        this.logdate = logdate;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}