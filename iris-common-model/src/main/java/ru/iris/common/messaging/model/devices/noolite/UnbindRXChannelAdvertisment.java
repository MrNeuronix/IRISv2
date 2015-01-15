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

package ru.iris.common.messaging.model.devices.noolite;

import ru.iris.common.messaging.model.Advertisement;

public class UnbindRXChannelAdvertisment extends Advertisement
{
	/**
	 * Device UUID
	 */

	private String deviceUUID;

	/**
	 * Channel.
	 */

	private int channel;

	public UnbindRXChannelAdvertisment() {
	}

	public UnbindRXChannelAdvertisment(String deviceUUID, int channel)
	{
		this.channel = channel;
		this.deviceUUID = deviceUUID;
	}

	public String getDeviceUUID()
	{
		return deviceUUID;
	}

	public void setDeviceUUID(String deviceUUID)
	{
		this.deviceUUID = deviceUUID;
	}

	public int getChannel()
	{
		return channel;
	}

	public void setChannel(int channel)
	{
		this.channel = channel;
	}

	@Override
	public String toString()
	{
		return "UnbindRXChannelAdvertisment { UUID: " + deviceUUID + ", channel: " + channel + " }";
	}
}
