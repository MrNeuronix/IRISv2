/*
 * Copyright 2012-2014 Nikolay A. Viguro
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.iris.common.messaging.model.devices;

import ru.iris.common.messaging.model.Advertisement;

import java.util.HashMap;
import java.util.Map;

public class GenericAdvertisement extends Advertisement {
    /**
     * Advertisement label
     */
    protected String label;

	/**
     * Data map
     */
    protected Map<String, Object> data = new HashMap<>();

    public GenericAdvertisement() {
    }

    public GenericAdvertisement(String label, Map<String, Object> data) {
		this.label = label;
        this.data = data;
    }

    public GenericAdvertisement(String label, Object data) {
        this.label = label;
        this.data.put("data", data);
    }

    public GenericAdvertisement(String label, String key, String value) {
        this.label = label;
        this.data.put(key, value);
    }

    public GenericAdvertisement(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Object getValue() {
        return data.get("data");
    }

    public void setValue(Object value) {
        data.put("data", value);
    }

    public Object getValue(String key) {
        return data.get(key);
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "GenericAdvertisement{" +
                "label='" + label + '\'' +
                ", data=" + data +
                '}';
    }
}
