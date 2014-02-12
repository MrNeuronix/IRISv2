package ru.iris.common.database.model.devices;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 26.10.13
 * Time: 16:00
 */

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="devices")
public class Device implements Serializable {

    @Id
    private Long id;

    @Expose
    protected String name = "not set";

    @Expose
    protected short node = 0;

    @Expose
    protected int zone = 0;

    @Expose
    protected String type = "unknown";

    @Expose
    @Column(name="internaltype")
    protected String internalType = "unknown";

    @Expose
    @Column(name="manufname")
    protected String manufName = "unknown";

    @Expose
    @Column(name="productname")
    protected String productName = "unknown";

    @Expose
    protected String uuid = "unknown";

    @Expose
    protected String status = "unknown";

    @Expose
    @Column(name="internalname")
    protected String internalName = "unknown";

    @Expose
    private String source = "unknown";

    @Expose
    @OneToMany(cascade = CascadeType.ALL)
    private List<DeviceValue> values = new ArrayList<>();

    public Device() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setProductName(String productName) {
            this.productName = productName;
    }

    public String getProductName() {
        return this.productName;
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

    public String getType() {
        return this.type;
    }

    public void setInternalType(String type) {
        this.internalType = type;
    }

    public String getInternalName() {
        return internalName;
    }

    public void setInternalName(String internalName) {
        this.internalName = internalName;
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

    public List<DeviceValue> getValues() {
        return values;
    }

    public void setValues(List<DeviceValue> values) {
        this.values = values;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Deprecated
    public List<DeviceValue> getValueIDs() {
        return values;
    }

    public DeviceValue getValue(String value) {

        for (DeviceValue zvalue : values) {
            if (zvalue.getLabel().equals(value)) {
                return zvalue;
            }
        }
        return null;
    }

    public synchronized void updateValue(DeviceValue value) {

        // bi-directional relationship
        value.setDevice(this);

        List<DeviceValue> zDv = values;
        boolean flag = false;

        for (DeviceValue zvalue : new ArrayList<>(values)) {
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

    public synchronized void removeValue(DeviceValue value) {

        List<DeviceValue> zDv = values;

        for (DeviceValue zvalue : new ArrayList<>(values)) {
            if (zvalue.getLabel().equals(value.getLabel())) {
                zDv.remove(zvalue);
            }
        }

        values = zDv;
        zDv = null;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().create();
        return gson.toJson(this);
    }
}
