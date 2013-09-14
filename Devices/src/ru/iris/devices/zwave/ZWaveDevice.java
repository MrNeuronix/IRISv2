package ru.iris.devices.zwave;

/**
 * IRIS-X Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 09.09.13
 * Time: 9:52
 */

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import ru.iris.common.SQL;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ZWaveDevice {

    @Expose
    private String name = "not set";
    @Expose
    private short node = 0;
    @Expose
    private int zone = 0;
    @Expose
    private String type = "unknown";
    @Expose
    private String internalType = "unknown";
    @Expose
    private String manufName = "unknown";
    @Expose
    private String uuid = "unknown";
    @Expose
    private String status = "unknown";
    @Expose
    private String source = "zwave";
    @Expose
    private HashMap<String, Object> LabelsValues = new HashMap<String, Object>();

    private SQL sql;

        public ZWaveDevice() throws IOException, SQLException {
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
                label = "none set";

            try {
                return this.LabelsValues.get(label).toString();
            } catch (NullPointerException e)
            {
                return "none set";
            }

        }

        public void setValue(String label, Object value) {

            if (label == null)
                label = "none set";

            this.LabelsValues.put(label, value);
        }

        public void updateValue(String label, Object value) {

            HashMap<String, Object> zDv = new HashMap<>();

            Iterator it = LabelsValues.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry)it.next();

                String olabel = String.valueOf(pairs.getKey());
                String ovalue = String.valueOf(pairs.getValue());

                if(label.equals(olabel))
                {
                    zDv.put(label, value);
                }
                else
                {
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
                label = "none set";

            HashMap<String, Object> zDv = new HashMap<>();

            Iterator it = LabelsValues.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry)it.next();

                String olabel = String.valueOf(pairs.getKey());
                String ovalue = String.valueOf(pairs.getValue());

                if(label.equals(olabel))
                {
                    continue;
                }
                else
                {
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
        public String toString()
        {
            Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().create();
            return gson.toJson(this);
        }

        public void save() throws SQLException {

            if (this.type == null)
                this.type = "undefined";

            if (this.manufName == null)
                this.manufName = "undefined";

            if (this.status == null)
                this.status = "unknown";

            if (this.name == null)
                this.name = "not set";

            sql.doQuery("DELETE FROM DEVICES WHERE UUID='"+this.uuid+"'");
            sql.doQuery("DELETE FROM DEVICELABELS WHERE UUID='"+this.uuid+"'");

            sql.doQuery("INSERT INTO DEVICES (SOURCE, UUID, internaltype, TYPE, MANUFNAME, NODE, STATUS, NAME, ZONE) VALUES ('zwave','"+uuid+"','"+internalType+"','"+type+"','"+manufName+"','"+node+"','"+status+"','"+name+"','"+zone+"')");

            Iterator it = LabelsValues.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry)it.next();

                String label = String.valueOf(pairs.getKey());
                String value = String.valueOf(pairs.getValue());

                if(label.isEmpty())
                    label = "none";

                if(value.isEmpty())
                    value = "none";

                sql.doQuery("INSERT INTO DEVICELABELS (UUID, LABEL, VALUE) VALUES ('"+this.uuid+"','"+label+"','"+value+"')");
            }
        }
}
