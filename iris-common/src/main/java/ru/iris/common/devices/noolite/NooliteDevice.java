package ru.iris.common.devices.noolite;

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

public class NooliteDevice extends Device {

    @Expose
    private ArrayList<NooliteDeviceValue> values = new ArrayList<>();

    public NooliteDevice() throws IOException, SQLException {
        super();
        this.source = "noolite";
    }

    public ArrayList<NooliteDeviceValue> getValueIDs() {
        return values;
    }

    public NooliteDeviceValue getValue(String val) {

        for (NooliteDeviceValue value : values) {
            if (value.getLabel().equals(val)) {
                return value;
            }
        }
        return null;
    }

    public void updateValue(NooliteDeviceValue value) {

        ArrayList<NooliteDeviceValue> zDv = (ArrayList<NooliteDeviceValue>) values.clone();
        boolean flag = false;

        for (NooliteDeviceValue zvalue : values) {
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

    public void removeValue(NooliteDeviceValue value) {

        ArrayList<NooliteDeviceValue> zDv = (ArrayList<NooliteDeviceValue>) values.clone();

        for (NooliteDeviceValue zvalue : values) {
            if (zvalue.getLabel().equals(value.getLabel())) {
                zDv.remove(zvalue);
            }
        }

        values = zDv;
        zDv = null;
    }

    public NooliteDevice loadByChannel(int channel) {
        ResultSet rs = sql.select("SELECT * FROM devices WHERE internalname='noolite/channel/" + channel + "'");
        try {

            while (rs.next()) {
                return load(rs.getString("uuid"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public NooliteDevice load(String uuid) {
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

                updateValue(new NooliteDeviceValue(
                        rs.getString("label"),
                        rs.getString("value"),
                        rs.getString("type"),
                        rs.getString("units")
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
            name = "not set";

        if (node == 0) {
            ResultSet rs = sql.select("SELECT node FROM devices ORDER BY node DESC LIMIT 0,1");
            while (rs.next()) {
                // noolite node numeration starting > 500
                if (rs.getShort("node") < 500) {
                    node = 500;
                } else {
                    node = (short) (rs.getShort("node") + 1);
                }
            }

            if (node == 0) {
                node = 500;
            }

            rs.close();
        }

        sql.doQuery("DELETE FROM devices WHERE uuid='" + uuid + "'");
        sql.doQuery("DELETE FROM devicesvalues WHERE uuid='" + uuid + "'");
        sql.doQuery("INSERT INTO devices (source, uuid, internaltype, type, manufname, node, status, name, zone, productname, internalname) VALUES ('noolite','" + uuid + "','" + internalType + "','" + type + "','" + manufName + "','" + node + "','" + status + "','" + name + "','" + zone + "','" + productName + "','" + internalName + "')");

        for (NooliteDeviceValue zvalue : values) {
            sql.doQuery("INSERT INTO devicesvalues (uuid, label, value, type, units)" +
                    " VALUES ('" + uuid + "','" + zvalue.getLabel() + "','" + zvalue.getValue() + "','" + zvalue.getValueType() + "','" + zvalue.getValueUnits() + "')");
        }
    }

    @Override
    public String toString() {
        return "NooliteDevice{" +
                "values=" + values +
                '}';
    }
}
