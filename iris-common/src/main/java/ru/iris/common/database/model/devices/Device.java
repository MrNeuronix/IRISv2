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

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "devices")
public class Device implements Serializable
{

	@Id
	private Long id;

	@Expose private String name = "not set";

	@Expose private short node = 0;

	@Expose private int zone = 0;

	@Expose private String type = "unknown";

	@Expose
	@Column(name = "internaltype") private String internalType = "unknown";

	@Expose
	@Column(name = "manufname") private String manufName = "unknown";

	@Expose
	@Column(name = "productname") private String productName = "unknown";

	@Expose private String uuid = "unknown";

	@Expose private String status = "unknown";

	@Expose
	@Column(name = "internalname") private String internalName = "unknown";

	@Expose
	private String source = "unknown";

	@Expose
	@OneToMany(cascade = CascadeType.ALL)
	private List<DeviceValue> values = new ArrayList<>();

	public Device()
	{
	}

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	public String getName()
	{
		return this.name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getProductName()
	{
		return this.productName;
	}

	public void setProductName(String productName)
	{
		this.productName = productName;
	}

	public short getNode()
	{
		return this.node;
	}

	public void setNode(short node)
	{
		this.node = node;
	}

	public String getUuid()
	{
		return this.uuid;
	}

	public void setUuid(String uuid)
	{
		this.uuid = uuid;
	}

	public int getZone()
	{
		return this.zone;
	}

	public void setZone(int zone)
	{
		this.zone = zone;
	}

	public String getType()
	{
		return this.type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String getInternalName()
	{
		return internalName;
	}

	public void setInternalName(String internalName)
	{
		this.internalName = internalName;
	}

	public String getInternalType()
	{
		return this.internalType;
	}

	public void setInternalType(String type)
	{
		this.internalType = type;
	}

	public String getManufName()
	{
		return this.manufName;
	}

	public void setManufName(String manufName)
	{
		this.manufName = manufName;
	}

	public String getStatus()
	{
		return this.status;
	}

	public void setStatus(String status)
	{
		this.status = status;
	}

	public List<DeviceValue> getValues()
	{
		return values;
	}

	public void setValues(List<DeviceValue> values)
	{
		this.values = values;
	}

	public String getSource()
	{
		return source;
	}

	public void setSource(String source)
	{
		this.source = source;
	}

	@Deprecated
	public List<DeviceValue> getValueIDs()
	{
		return values;
	}

	public DeviceValue getValue(String value)
	{

		for (DeviceValue zvalue : values)
		{
			if (zvalue.getLabel().equals(value))
			{
				return zvalue;
			}
		}
		return null;
	}

	public synchronized void updateValue(DeviceValue value)
	{

		// bi-directional relationship
		value.setDevice(this);

		List<DeviceValue> zDv = values;
		boolean flag = false;

		for (DeviceValue zvalue : new ArrayList<>(values))
		{
			if (zvalue.getLabel().equals(value.getLabel()))
			{
				zDv.remove(zvalue);
				zDv.add(value);
				flag = true;
			}
		}

		if (!flag)
		{
			zDv.add(value);
		}

		values = zDv;
		zDv = null;
	}

	public synchronized void removeValue(DeviceValue value)
	{

		List<DeviceValue> zDv = values;

		for (DeviceValue zvalue : new ArrayList<>(values))
		{
			if (zvalue.getLabel().equals(value.getLabel()))
			{
				zDv.remove(zvalue);
			}
		}

		values = zDv;
		zDv = null;
	}

	@Override
	public String toString()
	{
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().create();
		return gson.toJson(this);
	}
}
