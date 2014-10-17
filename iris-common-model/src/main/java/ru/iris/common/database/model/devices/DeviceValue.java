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
import org.zwave4j.ValueId;
import ru.iris.common.database.model.DBModel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "devicesvalues")
public class DeviceValue extends DBModel
{
	@Transient
	private transient final Gson gson = new GsonBuilder().create();

	private String label = "unknown";

	private String uuid = "unknown";

	private String value = "unknown";

	@Column(name = "type")
	private String valueType = "unknown";

	@Column(name = "units")
	private String valueUnits = "unknown";

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

	@Override public String toString()
	{
		return "DeviceValue{" +
				"id=" + id +
				", label='" + label + '\'' +
				", uuid='" + uuid + '\'' +
				", value='" + value + '\'' +
				", valueType='" + valueType + '\'' +
				", valueUnits='" + valueUnits + '\'' +
				", isReadonly=" + isReadonly +
				", valueId='" + valueId + '\'' +
				'}';
	}
}
