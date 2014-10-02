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

import com.google.gson.annotations.Expose;
import ru.iris.common.database.model.devices.Device;
import ru.iris.common.messaging.model.Advertisement;

import java.util.Map;

public class ResponseDeviceInventoryAdvertisement extends Advertisement
{
	/**
	 * Devices Map
	 */
	@Expose
	private Map<?, Device> devices;

	public ResponseDeviceInventoryAdvertisement set(Map<?, Device> devices)
	{
		this.devices = devices;
		return this;
	}

	public Map<?, Device> getDevices()
	{
		return devices;
	}

	public void setDevices(Map<String, Device> devices)
	{
		this.devices = devices;
	}
}
