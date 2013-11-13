package ru.iris.common.devices;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 26.10.13
 * Time: 16:00
 */

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import ru.iris.common.I18N;
import ru.iris.common.SQL;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class Device implements Serializable {

    protected I18N i18n = new I18N();

    @Expose
    protected String name = i18n.message("not.set");
    @Expose
    protected short node = 0;
    @Expose
    protected int zone = 0;
    @Expose
    protected String type = i18n.message("unknown");
    @Expose
    protected String internalType = i18n.message("unknown");
    @Expose
    protected String manufName = i18n.message("unknown");
    @Expose
    protected String uuid = i18n.message("unknown");
    @Expose
    protected String status = i18n.message("unknown");
    @Expose
    protected String source = i18n.message("unknown");
    protected HashMap<String, Object> LabelsValues = new HashMap<String, Object>();

    protected SQL sql;

    public Device() throws IOException, SQLException {
        sql = new SQL();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public String getSource() {
        return this.source;
    }

    public void setNode(short node) {
        this.node = node;
    }

    public short getNode() {
        return this.node;
    }

    public String getUUID() {
        return this.uuid;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    public int getZone() {
        return this.zone;
    }

    public void setZone(int zone) {
        this.zone = zone;
    }

    public String getValue(String label) {

        if (label == null)
            label = i18n.message("none.set");

        try {
            return this.LabelsValues.get(label).toString();
        } catch (NullPointerException e) {
            return i18n.message("none.set");
        }

    }

    public Map<String, Object> getLabelsValues() {
        return this.LabelsValues;
    }

    public String getType() {
        return this.type;
    }

    public void setInternalType(String type) {
        this.internalType = type;
    }

    public String getInternalType() {
        return this.internalType;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getManufName() {
        return this.manufName;
    }

    public void setManufName(String manufName) {
        this.manufName = manufName;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().create();
        return gson.toJson(this);
    }
}
