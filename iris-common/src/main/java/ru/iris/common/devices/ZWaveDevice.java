package ru.iris.common.devices;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 26.10.13
 * Time: 16:01
 */

import com.google.gson.annotations.Expose;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

public class ZWaveDevice extends Device {

    @Expose
    private ArrayList<ZWaveDeviceValue> values = new ArrayList<>();

    public ZWaveDevice() throws IOException, SQLException {
        super();
        this.source = "zwave";
    }

    public ArrayList<ZWaveDeviceValue> getValueIDs() {
        return values;
    }

    public void addValue(ZWaveDeviceValue value) {
        values.add(value);
    }

    public void updateValue(ZWaveDeviceValue value) {

        ArrayList<ZWaveDeviceValue> zDv = new ArrayList<>();

        for (ZWaveDeviceValue zvalue : values) {
            if (zvalue.getLabel().equals(value.getLabel())) {
                zDv.remove(zvalue);
                zDv.add(value);
            }
        }

        values = zDv;
    }

    public void removeValue(ZWaveDeviceValue value) {

        ArrayList<ZWaveDeviceValue> zDv = new ArrayList<>();

        for (ZWaveDeviceValue zvalue : values) {
            if (zvalue.getLabel().equals(value.getLabel())) {
                zDv.remove(zvalue);
            }
        }

        values = zDv;
    }

    public void save() throws SQLException {

        if (type == null)
            type = i18n.message("undefined");

        if (manufName == null || manufName.isEmpty())
            manufName = i18n.message("undefined");

        if (status == null)
            status = i18n.message("unknown");

        if (name == null)
            name = i18n.message("not.set");

        sql.doQuery("DELETE FROM devices WHERE uuid='" + uuid + "'");
        sql.doQuery("DELETE FROM devicesvalues WHERE uuid='" + uuid + "'");
        sql.doQuery("INSERT INTO devices (source, uuid, internaltype, type, manufname, node, status, name, zone, productname, internalname) VALUES ('zwave','" + uuid + "','" + internalType + "','" + type + "','" + manufName + "','" + node + "','" + status + "','" + name + "','" + zone + "','" + productName + "','" + internalName + "')");

        for (ZWaveDeviceValue zvalue : values) {
            sql.doQuery("INSERT INTO devicesvalues (uuid, label, value, type, units)" +
                    " VALUES ('" + uuid + "','" + zvalue.getLabel() + "','" + zvalue.getValue() + "','" + zvalue.getValueType() + "','" + zvalue.getValueUnits() + "')");
        }
    }
}
