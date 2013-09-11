package ru.iris.devices.zwave;

/**
 * IRIS-X Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 09.09.13
 * Time: 9:52
 */

import ru.iris.common.SQL;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ZWaveDevice {

        private String name;
        private short node = 0;
        private int zone = 0;
        private String type;
        private String internalType;
        private String manufName;
        private String uuid;
        private String status;
        private String source = "zwave";
        private SQL sql;

        private Map<String, Object> LabelsValues = new HashMap<String, Object>();

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
            return this.LabelsValues.get(label).toString();
        }

        public void setValue(String label, Object value) {

            if(label == null) label = "none";

            this.LabelsValues.put(label, value);
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
            if (this.type == null)
                this.type = "undefined";

            if (this.manufName == null)
                this.manufName = "undefined";

            if (this.status == null)
                this.status = "unknown";

            if (this.name == null)
                this.name = "not set";

           return "Device\n\t[\n\t\tName: " + name + "\n\t\tUUID: " + uuid + "\n\t\tNode: " + node + "\n\t\tZone: " + zone + "\n\t\tType: " + type +
                   "\n\t\tInternal type: " + internalType + "\n\t\tManufacture name: " + manufName + "\n\t\tStatus: " + status + "\n\t]\n";
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
                sql.doQuery("INSERT INTO DEVICELABELS (UUID, LABEL, VALUE) VALUES ('"+this.uuid+"','"+pairs.getKey()+"','"+pairs.getValue()+"')");
                it.remove();
            }
        }
}
