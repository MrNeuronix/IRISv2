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

package ru.iris.common.database.model.devices;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import org.zwave4j.ValueId;

import javax.persistence.*;

@Entity
@Table(name = "devicesvalues")
public class DeviceValue
{

	@Transient
	private final Gson gson = new GsonBuilder().create();
	@Id
	private Long id;
	@ManyToOne
	private Device device;
	@Expose
	private String label = "unknown";
	@Expose
	private String uuid = "unknown";
	@Expose
	private String value = "unknown";
	@Expose
	@Column(name = "type")
	private String valueType = "unknown";
	@Expose
	@Column(name = "units")
	private String valueUnits = "unknown";
	@Expose
	private boolean isReadonly = false;
	private String valueId = "{ }";

	public DeviceValue()
	{
	}

	public DeviceValue(String label, String value, boolean isReadonly)
	{
		this.label = label;
		this.value = value;
		this.isReadonly = isReadonly;
	}

	public DeviceValue(String label, String value, String valueType, String valueUnits, boolean isReadonly)
	{
		this.label = label;
		this.value = value;
		this.valueType = valueType;
		this.valueUnits = valueUnits;
		this.isReadonly = isReadonly;
	}

	public DeviceValue(String label, String value, String valueType, String valueUnits, String uuid, boolean isReadonly)
	{
		this.label = label;
		this.value = value;
		this.valueType = "";
		this.valueUnits = valueUnits;
		this.isReadonly = isReadonly;
		this.uuid = uuid;
	}

	public DeviceValue(String label, String value, String valueType, String valueUnits, ValueId valueId, boolean isReadonly)
	{

		this(label, value, valueType, valueUnits, isReadonly);
		this.valueId = gson.toJson(valueId);
	}

	public DeviceValue(String label, String uuid, String value, String valueType, String valueUnits, ValueId valueId, boolean isReadonly)
	{

		this(label, value, valueType, valueUnits, isReadonly);
		this.valueId = gson.toJson(valueId);
		this.uuid = uuid;
	}

	public DeviceValue(Device device, String label, String uuid, String value, String valueType, String valueUnits, ValueId valueId, boolean isReadonly)
	{

		this(label, value, valueType, valueUnits, isReadonly);
		this.valueId = gson.toJson(valueId);
		this.uuid = uuid;
		this.device = device;
	}

	public Device getDevice()
	{
		return device;
	}

	public void setDevice(Device device)
	{
		this.device = device;
	}

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	public String getUuid()
	{
		return uuid;
	}

	public void setUuid(String uuid)
	{
		this.uuid = uuid;
	}

	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public String getValueType()
	{
		return valueType;
	}

	public void setValueType(String valueType)
	{
		this.valueType = valueType;
	}

	public String getValueUnits()
	{
		return valueUnits;
	}

	public void setValueUnits(String valueUnits)
	{
		this.valueUnits = valueUnits;
	}

	public boolean isReadonly()
	{
		return isReadonly;
	}

	public void setReadonly(boolean isReadonly)
	{
		this.isReadonly = isReadonly;
	}

	public void setValueId(String valueId)
	{
		this.valueId = valueId;
	}

	public String getValueId()
	{
		return valueId;
	}

	public void setValueId(ValueId valueId)
	{
		this.valueId = gson.toJson(valueId);
	}

	/////////////////////////////////

	@Override
	public String toString()
	{
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
