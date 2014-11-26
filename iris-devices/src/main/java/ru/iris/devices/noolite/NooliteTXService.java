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

package ru.iris.devices.noolite;

import com.avaje.ebean.Ebean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.database.model.devices.Device;
import ru.iris.common.database.model.devices.DeviceValue;
import ru.iris.common.helpers.DBLogger;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.JsonNotification;
import ru.iris.common.messaging.model.devices.noolite.*;
import ru.iris.noolite4j.sender.PC1132;

import java.util.UUID;

public class NooliteTXService
{
	private final Logger LOGGER = LogManager.getLogger(NooliteTXService.class.getName());

	public NooliteTXService()
	{
		// Initialize the libusb context
		final PC1132 pc = new PC1132();
		pc.open();

		final JsonMessaging jsonMessaging = new JsonMessaging(UUID.randomUUID(), "devices-noolite-tx");

			jsonMessaging.subscribe("event.devices.noolite.setvalue");
			jsonMessaging.subscribe("event.devices.noolite.tx.bindchannel");
			jsonMessaging.subscribe("event.devices.noolite.tx.unbindchannel");

		jsonMessaging.setNotification(new JsonNotification() {

			@Override
			public void onNotification(JsonEnvelope envelope) {

					if (envelope.getObject() instanceof NooliteDeviceLevelSetAdvertisement)
					{
						LOGGER.debug("Get SetDeviceLevel advertisement");

						// We know of service advertisement
						final NooliteDeviceLevelSetAdvertisement advertisement = envelope.getObject();

						byte level;

						if (Integer.parseInt(advertisement.getValue()) == 255)
							level = 100;
						else
							level = Byte.valueOf(advertisement.getValue());

						Device device = Ebean.find(Device.class).where().eq("uuid", advertisement.getDeviceUUID()).findUnique();

						byte channel = Byte.valueOf(device.getValue("channel").getValue());

						//if noolite device dimmer (user set)
						if (device.getValue("type") != null && device.getValue("type").getValue().contains("dimmer"))
						{
							if (level > 99 || level == 99)
							{

								LOGGER.info("Turn on device on channel " + channel);
								updateValue(device, "Level", "255");
								DBLogger.info("Device is ON", device.getUuid());

								pc.turnOn(channel);

							}
							else if (level < 0)
							{

								LOGGER.info("Turn off device on channel " + channel);
								updateValue(device, "Level", "0");
								DBLogger.info("Device is OFF", device.getUuid());

								pc.turnOff(channel);
							}
							else
							{
								updateValue(device, "Level", String.valueOf(level));
								DBLogger.info("Device level set: " + level, device.getUuid());

								LOGGER.info("Setting device on channel " + channel + " to level " + level);

								pc.setLevel(channel, level);
							}
						}
						else
						{
							if (level < 0 || level == 0)
							{
								LOGGER.info("Turn off device on channel " + channel);
								updateValue(device, "Level", "0");
								DBLogger.info("Device is OFF", device.getUuid());

								pc.turnOff(channel);
							}
							else
							{
								// turn on
								LOGGER.info("Turn on device on channel " + channel);
								updateValue(device, "Level", "255");
								DBLogger.info("Device is ON", device.getUuid());

								pc.turnOn(channel);
							}
						}

					}
					else if (envelope.getObject() instanceof BindTXChannelAdvertisment)
					{

						LOGGER.debug("Get BindTXChannel advertisement");

						final BindRXChannelAdvertisment advertisement = envelope.getObject();
						byte channel = (byte) advertisement.getChannel();

						LOGGER.info("Binding device to channel " + channel);
						DBLogger.info("Binding device to channel " + channel);

						pc.bindChannel(channel);

					}
					else if (envelope.getObject() instanceof UnbindTXChannelAdvertisment)
					{

						LOGGER.debug("Get UnbindTXChannel advertisement");

						final UnbindRXChannelAdvertisment advertisement = envelope.getObject();
						byte channel = (byte) advertisement.getChannel();

						LOGGER.info("Unbinding device from channel " + channel);
						DBLogger.info("Unbinding device from channel " + channel);

						pc.unbindChannel(channel);

					}
					else if (envelope.getReceiverInstance() == null)
					{
						// We received unknown broadcast message. Lets make generic log entry.
						LOGGER.info("Received broadcast "
								+ " from " + envelope.getSenderInstance()
								+ " to " + envelope.getReceiverInstance()
								+ " at '" + envelope.getSubject()
								+ ": " + envelope.getObject());
					}
					else
					{
						// We received unknown request message. Lets make generic log entry.
						LOGGER.info("Received request "
								+ " from " + envelope.getSenderInstance()
								+ " to " + envelope.getReceiverInstance()
								+ " at '" + envelope.getSubject()
								+ ": " + envelope.getObject());
					}

				}
		});

		jsonMessaging.start();
	}

	private void updateValue(Device device, String label, String value)
	{
		DeviceValue deviceValue = device.getValue(label);

		if (deviceValue == null)
		{
			deviceValue = new DeviceValue();

			deviceValue.setLabel(label);
			deviceValue.setUuid(device.getUuid());
			deviceValue.setReadonly(false);
			deviceValue.setValueId("{ }");
		}

		deviceValue.setValue(value);

		deviceValue.save();
	}
}
