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
import org.zwave4j.Manager;
import org.zwave4j.ValueId;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ZWaveDevice extends Device implements Serializable {

    @Expose
    private HashMap<String, Object> valueIDs = new HashMap<String, Object>();

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
        this.valueIDs.put(label, getValue((ValueId) value));
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

        HashMap<String, Object> zDvDigits = new HashMap<>();

        Iterator itDigits = LabelsValues.entrySet().iterator();
        while (itDigits.hasNext()) {
            Map.Entry pairs = (Map.Entry) itDigits.next();

            String olabel = String.valueOf(pairs.getKey());
            ValueId ovalue = (ValueId) pairs.getValue();

            if (label.equals(olabel)) {
                zDvDigits.put(label, value);
            } else {
                zDvDigits.put(olabel, getValue(ovalue));
            }
        }

        valueIDs = zDvDigits;
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
        }

        LabelsValues = zDv;

        HashMap<String, Object> zDvDigits = new HashMap<>();

        Iterator itDigits = LabelsValues.entrySet().iterator();
        while (itDigits.hasNext()) {
            Map.Entry pairs = (Map.Entry) itDigits.next();

            String olabel = String.valueOf(pairs.getKey());
            ValueId ovalue = (ValueId) pairs.getValue();

            if (label.equals(olabel)) {
                continue;
            } else {
                zDvDigits.put(olabel, getValue(ovalue));
            }
        }

        valueIDs = zDvDigits;
    }

    public void save() throws SQLException {

        if (this.type == null)
            this.type = i18n.message("undefined");

        if (this.manufName == null || this.manufName.isEmpty())
            this.manufName = i18n.message("undefined");

        if (this.status == null)
            this.status = i18n.message("unknown");

        if (this.name == null)
            this.name = i18n.message("not.set");

        sql.doQuery("DELETE FROM DEVICES WHERE UUID='" + this.uuid + "'");
        sql.doQuery("INSERT INTO DEVICES (SOURCE, UUID, internaltype, TYPE, MANUFNAME, NODE, STATUS, NAME, ZONE, PRODUCTNAME) VALUES ('zwave','" + uuid + "','" + internalType + "','" + type + "','" + manufName + "','" + node + "','" + status + "','" + name + "','" + zone + "','" + productName + "')");
    }

    private static Object getValue(ValueId valueId) {
        switch (valueId.getType()) {
            case BOOL:
                AtomicReference<Boolean> b = new AtomicReference<>();
                Manager.get().getValueAsBool(valueId, b);
                return b.get();
            case BYTE:
                AtomicReference<Short> bb = new AtomicReference<>();
                Manager.get().getValueAsByte(valueId, bb);
                return bb.get();
            case DECIMAL:
                AtomicReference<Float> f = new AtomicReference<>();
                Manager.get().getValueAsFloat(valueId, f);
                return f.get();
            case INT:
                AtomicReference<Integer> i = new AtomicReference<>();
                Manager.get().getValueAsInt(valueId, i);
                return i.get();
            case LIST:
                return null;
            case SCHEDULE:
                return null;
            case SHORT:
                AtomicReference<Short> s = new AtomicReference<>();
                Manager.get().getValueAsShort(valueId, s);
                return s.get();
            case STRING:
                AtomicReference<String> ss = new AtomicReference<>();
                Manager.get().getValueAsString(valueId, ss);
                return ss.get();
            case BUTTON:
                return null;
            case RAW:
                AtomicReference<short[]> sss = new AtomicReference<>();
                Manager.get().getValueAsRaw(valueId, sss);
                return sss.get();
            default:
                return null;
        }
    }

}
