package ru.iris.common.devices;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 26.10.13
 * Time: 16:01
 */

import org.zwave4j.ValueId;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ZWaveDevice extends Device implements Serializable {

    public ZWaveDevice() throws IOException, SQLException {
        super();
        this.source = "zwave";
    }

    public HashMap<String, Object> getValueIDs() {
        return LabelsValues;
    }

    public void setValueID(String label, Object value) {

        if (label == null)
            label = i18n.message("none.set");

        this.LabelsValues.put(label, value);
    }

    public void updateValueID(String label, Object value) {

        HashMap<String, Object> zDv = new HashMap<>();

        Iterator it = LabelsValues.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();

            String olabel = String.valueOf(pairs.getKey());
            ValueId ovalue = (ValueId) pairs.getValue();

            if (label.equals(olabel)) {
                zDv.put(label, value);
            } else {
                zDv.put(olabel, ovalue);
            }
        }

        LabelsValues = zDv;
    }

    public void removeValueID(String label) {

        if (label == null)
            label = i18n.message("none.set");

        HashMap<String, Object> zDv = new HashMap<>();

        Iterator it = LabelsValues.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();

            String olabel = String.valueOf(pairs.getKey());
            ValueId ovalue = (ValueId) pairs.getValue();

            if (label.equals(olabel)) {
                continue;
            } else {
                zDv.put(olabel, ovalue);
            }
            try {
                save();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        LabelsValues = zDv;
    }

    public void save() throws SQLException {

        if (this.type == null)
            this.type = i18n.message("undefined");

        if (this.manufName == null)
            this.manufName = i18n.message("undefined");

        if (this.status == null)
            this.status = i18n.message("unknown");

        if (this.name == null)
            this.name = i18n.message("not.set");

        sql.doQuery("DELETE FROM DEVICES WHERE UUID='" + this.uuid + "'");
        sql.doQuery("INSERT INTO DEVICES (SOURCE, UUID, internaltype, TYPE, MANUFNAME, NODE, STATUS, NAME, ZONE) VALUES ('zwave','" + uuid + "','" + internalType + "','" + type + "','" + manufName + "','" + node + "','" + status + "','" + name + "','" + zone + "')");
    }
}
