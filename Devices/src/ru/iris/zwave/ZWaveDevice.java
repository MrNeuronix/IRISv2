package ru.iris.zwave;

/**
 * IRIS-X Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 09.09.13
 * Time: 9:52
 */

import java.util.HashMap;
import java.util.Map;

public class ZWaveDevice {

        private String name;
        private int node = 0;
        private int zone = 0;
        private String type;
        private String manufName;
        private String uuid;

        private Map<String, Integer> LabelsValues = new HashMap<String, Integer>();

        public ZWaveDevice() {
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public void setNode(int node) {
            this.node = node;
        }

        public int getNode() {
            return this.node;
        }

        public String getUUID() {
            return this.uuid;
        }

        public int getZone() {
            return this.zone;
        }

        public String getValue(String label) {
            return this.LabelsValues.get(label).toString();
        }

        public void setValue(String label, Integer value) {
            this.LabelsValues.put(label, value);
        }

        public Map<String, Integer> getLabelsValues() {
            return this.LabelsValues;
        }

        public String getType() {
            return this.type;
        }

        public String getManufName() {
            return this.manufName;
        }

        public String getStatus() {
            return "Онлайн";
        }

}
