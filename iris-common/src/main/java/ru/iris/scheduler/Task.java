package ru.iris.scheduler;

import com.google.gson.annotations.Expose;
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
    private static SQL sql = new SQL();
    private boolean isNew = false;

    @Expose
    private int id;
    @Expose
    private Date date;
    @Expose
    private String eclass;
    @Expose
    private String command;
    @Expose
    private int type;
    @Expose
    private Date validto;
    @Expose
    private String interval;
    @Expose
    private int enabled;
    @Expose
    private String lang;

    public Task() throws SQLException, IOException {

        isNew = true;

        ResultSet rs = sql.select("SELECT id FROM scheduler ORDER BY id DESC LIMIT 0,1");
        rs.next();
        int lastid = rs.getInt("id");
        this.id = lastid++;
        rs.close();

        log.debug(i18n.message("scheduler.create.new.task.instance.with.id.0", id));
    }

    public Task(int id) throws SQLException, IOException {

        log.debug(i18n.message("scheduler.create.task.instance.from.id.0", id));

        ResultSet rs = sql.select("SELECT * FROM scheduler WHERE id='" + id + "'");

        rs.next();

        if(rs.getInt("id") > 0)
        {
            this.id = id;
            this.date = rs.getTimestamp("date");
            this.eclass = rs.getString("class");
            this.command = rs.getString("command");
            this.type = rs.getInt("type");
            this.validto = rs.getTimestamp("validto");
            this.interval = rs.getString("interval");
            this.enabled = rs.getInt("enabled");
            this.lang = rs.getString("language");
        }
        else
        {
          throw new SQLException("Task "+id+" not present in database!");
        }
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

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public boolean save() {
        log.info(i18n.message("scheduler.saving.task.0", id));

        if(isNew)
        {
            if (sql.doQuery("INSERT INTO scheduler VALUES (" +
                    id + "," +
                    getSQLDate(date) + "," +
                    eclass + "," +
                    command + "," +
                    type + "," +
                    validto + "," +
                    lang + ")"))
            {
                return true;
            } else {
                return false;
            }
        }
        else {
            if (sql.doQuery("UPDATE scheduler" +
                    "SET id = '" + id + "'," +
                    "date='" + getSQLDate(date) + "'," +
                    "class = '"+ eclass +"'," +
                    "command = '" + command + "'," +
                    "type = '" + type + "'," +
                    "validto = '" + getSQLDate(validto) + "'," +
                    "lang = '" + lang + "'," +
                    "interval = '" + interval + "' WHERE id='" + id + "'"))
            {
                return true;
            } else {
                return false;
            }
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
