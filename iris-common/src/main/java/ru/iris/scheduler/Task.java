package ru.iris.scheduler;

import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;
import ru.iris.common.SQL;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * IRIS-X Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 22.10.12
 * Time: 12:59
 */

public class Task {

    private static Logger log = LoggerFactory.getLogger(Task.class);
    private static I18N i18n = new I18N();

    private int id;
    private Date date;
    private String eclass;
    private String command;
    private int type;
    private Date validto;
    private String interval;
    private int enabled;
    private SQL sql;

    public Task() throws SQLException, IOException {

        sql = new SQL();

        ResultSet rs = sql.select("SELECT id FROM scheduler ORDER BY id DESC LIMIT 0,1");
        rs.next();
        int lastid = Integer.valueOf(rs.getInt("id"));
        this.id = lastid++;
        rs.close();

        log.debug(i18n.message("scheduler.create.new.task.instance.with.id.0", id));
    }

    public Task(int id) throws SQLException, IOException {

        sql = new SQL();

        log.info(i18n.message("scheduler.create.task.instance.from.id.0", id));

        @NonNls ResultSet rs = sql.select("SELECT * FROM scheduler WHERE id='" + id + "'");

        rs.next();

        this.id = id;
        this.date = rs.getTimestamp("date");
        this.eclass = rs.getString("class");
        this.command = rs.getString("command");
        this.type = Integer.valueOf(rs.getInt("type"));
        this.validto = rs.getTimestamp("validto");
        this.interval = rs.getString("interval");
        this.enabled = Integer.valueOf(rs.getInt("enabled"));

        rs.close();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
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

    public Date getValidto() {
        return validto;
    }

    public void setValidto(Date validto) {
        this.validto = validto;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public int getEnabled() {
        return enabled;
    }

    public void setEnabled(int enabled) {
        this.enabled = enabled;
    }

    public boolean save() {
        log.info(i18n.message("scheduler.saving.task.0", id));

        if (sql.doQuery("UPDATE scheduler" +
                "SET id = '" + id + "'," +
                "date='" + getSQLDate(date) + "'," +
                "class = '', command = '" + command + "'," +
                "type = '" + type + "'," +
                "validto = '" + getSQLDate(validto) + "'," +
                "interval = '" + interval + "' WHERE id='" + id + "'")) {
            return true;
        } else {
            return false;
        }
    }

    public String getSQLDate(Date date) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar c = Calendar.getInstance();
        c.setTime(date);

        return fmt.format(c.getTime());
    }

    public Date nextRunAsDate() throws ParseException {
        Date now = new Date();
        CronExpression cron = new CronExpression(interval);
        Date nextRunDate = cron.getNextValidTimeAfter(now);

        return nextRunDate;
    }

    public String nextRunAsString() throws ParseException {
        Date now = new Date();
        CronExpression cron = new CronExpression(interval);
        Date nextRunDate = cron.getNextValidTimeAfter(now);
        String nextRun = cron.getNextRun(nextRunDate);

        return nextRun;
    }

    public String getDateAsString(Date date) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return fmt.format(date);
    }

    public Date setDateAsString(String date) throws ParseException {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return fmt.parse(date);
    }
}
