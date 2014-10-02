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

package ru.iris.common.messaging.model.devices.zwave;

import com.google.gson.annotations.Expose;
import ru.iris.common.database.model.devices.Device;

public class ZWaveDeviceValueChanged
{
	/**
	 * Zwave device
	 */
	@Expose
	private Device device;

	@Expose
	private String label;

	@Expose
	private String value;

	/**
	 * Default constructor for de-serialisation.
	 */
	public ZWaveDeviceValueChanged()
	{
	}

	public ZWaveDeviceValueChanged set(Device device, String label, String value)
	{
		this.device = device;
		this.label = label;
		this.value = value;
		return this;
	}

	public Device getDevice()
	{
		return device;
	}

	public void setDevice(Device device)
	{
		this.device = device;
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

	@Override
	public String toString()
	{
		return "ZWaveDeviceValueChanged{" +
				"zwaveDevice=" + device.getInternalName() +
				'}';
	}
}
