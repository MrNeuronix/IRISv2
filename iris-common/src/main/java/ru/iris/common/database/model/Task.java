package ru.iris.common.database.model;

import com.google.gson.annotations.Expose;
import ru.iris.scheduler.CronExpression;

import javax.persistence.*;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;

/**
 * IRIS-X Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 22.10.12
 * Time: 12:59
 */

@Entity
@Table(name="scheduler")
public class Task {

    @Expose
    @Id
    private int id;

    @Expose
    @Column(columnDefinition = "timestamp")
    private Timestamp taskdate;

    @Expose
    @Column(name="class")
    private String eclass;

    @Expose
    private String command;

    @Expose
    private int type;

    @Expose
    @Column(columnDefinition = "timestamp")
    private Timestamp validto;

    @Expose
    private String intervalDate;

    @Expose
    private boolean enabled;

    @Expose
    private String lang;

    public Task() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEclass() {
        return eclass;
    }

    public void setEclass(String eclass) {
        this.eclass = eclass;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Timestamp getValidto() {
        return validto;
    }

    public void setValidto(Timestamp validto) {
        this.validto = validto;
    }

    public String getInterval() {
        return intervalDate;
    }

    public void setInterval(String interval) {
        this.intervalDate = interval;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Timestamp getTaskdate() {
        return taskdate;
    }

    public void setTaskdate(Timestamp taskdate) {
        this.taskdate = taskdate;
    }

    public String getIntervalDate() {
        return intervalDate;
    }

    public void setIntervalDate(String intervalDate) {
        this.intervalDate = intervalDate;
    }

    public Timestamp nextRun() throws ParseException {

        Date now = new Date();
        CronExpression cron = new CronExpression(intervalDate);
        Date nextRunDate = cron.getNextValidTimeAfter(now);

        return new Timestamp(nextRunDate.getTime());
    }
}
