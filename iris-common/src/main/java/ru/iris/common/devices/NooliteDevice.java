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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class NooliteDevice extends Device {

    @Expose
    private ArrayList<DeviceValue> values = new ArrayList<>();

    public NooliteDevice() throws IOException, SQLException {
        super();
        this.source = "noolite";
    }

    public ArrayList<DeviceValue> getValueIDs() {
        return values;
    }

    public DeviceValue getValue(String val) {

        for (DeviceValue value : values) {
            if (value.getLabel().equals(val)) {
                return value;
            }
        }
        return null;
    }

    public void addValue(DeviceValue value) {
        values.add(value);
    }

    public void updateValue(DeviceValue value) {

        ArrayList<DeviceValue> zDv = (ArrayList<DeviceValue>) values.clone();

        for (DeviceValue zvalue : values) {
            if (zvalue.getLabel().equals(value.getLabel())) {
                zDv.remove(zvalue);
                zDv.add(value);
            }
        }

        values = zDv;
        zDv = null;
    }

    public void removeValue(DeviceValue value) {

        ArrayList<DeviceValue> zDv = (ArrayList<DeviceValue>) values.clone();

        for (DeviceValue zvalue : values) {
            if (zvalue.getLabel().equals(value.getLabel())) {
                zDv.remove(zvalue);
            }
        }

        values = zDv;
        zDv = null;
    }

    public NooliteDevice load(String uuid)
    {
        ResultSet rs = sql.select("SELECT * FROM devices WHERE uuid='" + uuid + "'");

        try {
            while (rs.next()) {

                this.manufName = rs.getString("manufname");
                this.name = rs.getString("name");
                this.node = rs.getShort("node");
                this.status = rs.getString("status");
                this.internalType = rs.getString("internaltype");
                this.type = rs.getString("type");
                this.uuid = rs.getString("uuid");
                this.zone = rs.getInt("zone");
                this.productName = rs.getString("productname");
                this.internalName = rs.getString("internalname");
                this.source = rs.getString("source");
            }

            rs.close();

        } catch (SQLException ignored)
        {
        }

        rs = sql.select("SELECT * FROM devicesvalues WHERE uuid='" + uuid + "'");

        try {
            while (rs.next()) {

                addValue(new DeviceValue(
                        rs.getString("label"),
                        rs.getString("value"),
                        rs.getString("type"),
                        rs.getString("units")
                ));
            }

            rs.close();

        } catch (SQLException ignored)
        {
        }

        return this;
    }

    public void save() throws SQLException {

        if (type == null)
            type = "undefined";

        if (manufName == null || manufName.isEmpty())
            manufName = "undefined";

        if (status == null)
            status = "unknown";

        if (name == null)
            name = "not set";

        sql.doQuery("DELETE FROM devices WHERE uuid='" + uuid + "'");
        sql.doQuery("DELETE FROM devicesvalues WHERE uuid='" + uuid + "'");
        sql.doQuery("INSERT INTO devices (source, uuid, internaltype, type, manufname, node, status, name, zone, productname, internalname) VALUES ('zwave','" + uuid + "','" + internalType + "','" + type + "','" + manufName + "','" + node + "','" + status + "','" + name + "','" + zone + "','" + productName + "','" + internalName + "')");

        for (DeviceValue zvalue : values) {
            sql.doQuery("INSERT INTO devicesvalues (uuid, label, value, type, units)" +
                    " VALUES ('" + uuid + "','" + zvalue.getLabel() + "','" + zvalue.getValue() + "','" + zvalue.getValueType() + "','" + zvalue.getValueUnits() + "')");
        }
    }

    @Override
    public String toString() {
        return "ZWaveDevice{" +
                "values=" + values +
                '}';
    }
}
