package ru.iris.common.database.model.devices;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import org.zwave4j.ValueId;

import javax.persistence.*;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 27.11.13
 * Time: 15:33
 * License: GPL v3
 */

@Entity
@Table(name = "devicesvalues")
public class DeviceValue {

    @Id
    private Long id;

    @ManyToOne
    private Device device;

    @Expose
    private String label = "unknown";

    @Expose
    private String value = "unknown";

    @Expose
    @Column(name = "type")
    private String valueType = "unknown";

    @Expose
    @Column(name = "units")
    private String valueUnits = "unknown";

    @Expose
    @Transient
    private boolean isReadonly = false;

    private String valueId = "{ }";

    @Transient
    private final Gson gson = new GsonBuilder().create();

    public DeviceValue() {
    }

    public DeviceValue(String label, String value, boolean isReadonly) {
        this.label = label;
        this.value = value;
        this.isReadonly = isReadonly;
    }

    public DeviceValue(String label, String value, String valueType, String valueUnits, boolean isReadonly) {
        this.label = label;
        this.value = value;
        this.valueType = valueType;
        this.valueUnits = valueUnits;
        this.isReadonly = isReadonly;
    }

    public DeviceValue(String label, String value, String valueType, String valueUnits, ValueId valueId, boolean isReadonly) {

        this(label, value, valueType, valueUnits, isReadonly);
        this.valueId = gson.toJson(valueId);
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValueType() {
        return valueType;
    }

    public String getValueUnits() {
        return valueUnits;
    }

    public void setValueUnits(String valueUnits) {
        this.valueUnits = valueUnits;
    }

    public boolean isReadonly() {
        return isReadonly;
    }

    public void setReadonly(boolean isReadonly) {
        this.isReadonly = isReadonly;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public void setValueId(String valueId) {
        this.valueId = valueId;
    }

    public String getValueId() {
        return valueId;
    }

    public void setValueId(ValueId valueId) {
        this.valueId = gson.toJson(valueId);
    }

    /////////////////////////////////

    @Override
    public String toString() {
        return "DeviceValue{" +
                "id=" + id +
                ", device=" + device +
                ", label='" + label + '\'' +
                ", value='" + value + '\'' +
                ", valueType='" + valueType + '\'' +
                ", valueUnits='" + valueUnits + '\'' +
                ", isReadonly=" + isReadonly +
                ", valueId='" + valueId + '\'' +
                '}';
    }
}
