package ru.iris.common.devices.zwave;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 26.10.13
 * Time: 16:01
 */

import com.google.gson.annotations.Expose;
import ru.iris.common.devices.Device;

import java.io.IOException;
import java.sql.ResultSet;
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

    public ZWaveDeviceValue getValue(String value) {

        for (ZWaveDeviceValue zvalue : values) {
            if (zvalue.getLabel().equals(value)) {
                return zvalue;
            }
        }
        return null;
    }

    public void updateValue(ZWaveDeviceValue value) {

        ArrayList<ZWaveDeviceValue> zDv = (ArrayList<ZWaveDeviceValue>) values.clone();
        boolean flag = false;

        for (ZWaveDeviceValue zvalue : values) {
            if (zvalue.getLabel().equals(value.getLabel())) {
                zDv.remove(zvalue);
                zDv.add(value);
                flag = true;
            }
        }

        if (!flag) {
            zDv.add(value);
        }

        values = zDv;
        zDv = null;
    }

    public void removeValue(ZWaveDeviceValue value) {

        ArrayList<ZWaveDeviceValue> zDv = (ArrayList<ZWaveDeviceValue>) values.clone();

        for (ZWaveDeviceValue zvalue : values) {
            if (zvalue.getLabel().equals(value.getLabel())) {
                zDv.remove(zvalue);
            }
        }

        values = zDv;
        zDv = null;
    }

    public ZWaveDevice load(String uuid) {
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

        } catch (SQLException ignored) {
        }

        rs = sql.select("SELECT * FROM devicesvalues WHERE uuid='" + uuid + "'");

        try {
            while (rs.next()) {

                updateValue(new ZWaveDeviceValue(
                        rs.getString("label"),
                        rs.getString("value"),
                        rs.getString("type"),
                        rs.getString("units"),
                        // ValueID is unknown!
                        rs.getBoolean("isReadonly")
                ));
            }

            rs.close();

        } catch (SQLException ignored) {
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
            name = "not.set";

        sql.doQuery("DELETE FROM devices WHERE uuid='" + uuid + "'");
        sql.doQuery("DELETE FROM devicesvalues WHERE uuid='" + uuid + "'");
        sql.doQuery("INSERT INTO devices (source, uuid, internaltype, type, manufname, node, status, name, zone, productname, internalname) VALUES ('zwave','" + uuid + "','" + internalType + "','" + type + "','" + manufName + "','" + node + "','" + status + "','" + name + "','" + zone + "','" + productName + "','" + internalName + "')");

        for (ZWaveDeviceValue zvalue : values) {
            sql.doQuery("INSERT INTO devicesvalues (uuid, label, value, type, units, isReadonly)" +
                    " VALUES ('" + uuid + "','" + zvalue.getLabel() + "','" + zvalue.getValue() + "','" + zvalue.getValueType() + "','" + zvalue.getValueUnits() + "', " + zvalue.isReadonly() + ")");
        }
    }

    @Override
    public String toString() {
        return "ZWaveDevice{" +
                "values=" + values +
                '}';
    }
}
