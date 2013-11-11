package ru.iris.common.devices;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 26.10.13
 * Time: 16:01
 */

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ZWaveDevice extends Device {

    public ZWaveDevice() throws IOException, SQLException {
        super();
        this.source = "zwave";
    }

    public void setValue(String label, Object value) {

        if (label == null)
            label = i18n.message("none.set");

        this.LabelsValues.put(label, value);
    }

    public void updateValue(String label, Object value) {

        HashMap<String, Object> zDv = new HashMap<>();

        Iterator it = LabelsValues.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();

            String olabel = String.valueOf(pairs.getKey());
            String ovalue = String.valueOf(pairs.getValue());

            if (label.equals(olabel)) {
                zDv.put(label, value);
            } else {
                zDv.put(olabel, ovalue);
            }
        }

        LabelsValues = zDv;

        try {
            save();
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void removeValue(String label) {

        if (label == null)
            label = i18n.message("none.set");

        HashMap<String, Object> zDv = new HashMap<>();

        Iterator it = LabelsValues.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();

            String olabel = String.valueOf(pairs.getKey());
            String ovalue = String.valueOf(pairs.getValue());

            if (label.equals(olabel)) {
                continue;
            } else {
                zDv.put(olabel, ovalue);
            }
            try {
                save();
            } catch (SQLException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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
        sql.doQuery("DELETE FROM DEVICELABELS WHERE UUID='" + this.uuid + "'");

        sql.doQuery("INSERT INTO DEVICES (SOURCE, UUID, internaltype, TYPE, MANUFNAME, NODE, STATUS, NAME, ZONE) VALUES ('zwave','" + uuid + "','" + internalType + "','" + type + "','" + manufName + "','" + node + "','" + status + "','" + name + "','" + zone + "')");

        for (Map.Entry<String, Object> stringObjectEntry : LabelsValues.entrySet()) {
            Map.Entry pairs = (Map.Entry) stringObjectEntry;

            String label = String.valueOf(pairs.getKey());
            String value = String.valueOf(pairs.getValue());

            if (label.isEmpty())
                label = i18n.message("none");

            if (value.isEmpty())
                value = i18n.message("none");

            sql.doQuery("INSERT INTO DEVICELABELS (UUID, LABEL, VALUE) VALUES ('" + this.uuid + "','" + label + "','" + value + "')");
        }
    }
}
