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
import ru.iris.common.database.model.SensorData;
import ru.iris.common.database.model.devices.Device;
import ru.iris.common.database.model.devices.DeviceValue;
import ru.iris.common.helpers.DBLogger;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.devices.noolite.*;
import ru.iris.noolite4j.receiver.RX2164;
import ru.iris.noolite4j.watchers.BatteryState;
import ru.iris.noolite4j.watchers.Notification;
import ru.iris.noolite4j.watchers.SensorType;
import ru.iris.noolite4j.watchers.Watcher;

import java.util.UUID;

public class NooliteRXService
{
	private final Logger LOGGER = LogManager.getLogger(NooliteRXService.class.getName());
	private JsonMessaging messaging;
	private boolean shutdown = false;
	private RX2164 rx;

	public NooliteRXService()
	{
		messaging = new JsonMessaging(UUID.randomUUID(), "devices-noolite-rx");

		new InternalCommands();

		rx = new RX2164();
		rx.open();

		Watcher watcher = new Watcher() {
			@Override
			public void onNotification(Notification notification) {

				byte channel = notification.getChannel();
				SensorType sensor = (SensorType) notification.getValue("sensortype");

				Device device = loadByChannel(channel);

				if (device == null) {
					device = new Device();
					device.setSource("noolite");
					device.setInternalName("noolite/channel/" + channel);
					device.setStatus("listening");
					device.setType("Noolite Device");
					device.setManufName("Nootechnika");
					device.setNode((short) (1000 + channel));
					device.setUuid(UUID.randomUUID().toString());

					device.save();

					// device is not sensor
					if (sensor == null) {
						device.setInternalType("switch");
						new DeviceValue("channel", String.valueOf(channel), "", "", device.getUuid(), true).save();
						new DeviceValue("type", "switch", "", "", device.getUuid(), false).save();
					} else {
						device.setInternalType("sensor");
						new DeviceValue("channel", String.valueOf(channel), "", "", device.getUuid(), true).save();
						new DeviceValue("type", "sensor", "", "", device.getUuid(), false).save();
						new DeviceValue("sensorname", sensor.name(), "", "", device.getUuid(), false).save();
					}
				}

				// turn off
				switch (notification.getType()) {
					case TURN_OFF:
						LOGGER.info("Channel " + channel + ": Got OFF command");
						updateValue(device, "Level", "0");
						DBLogger.info("Device is OFF", device.getUuid());
						SensorData.log(device.getUuid(), "Switch", "OFF");
						messaging.broadcast("event.devices.noolite.value.set", new NooliteDeviceLevelSetAdvertisement().set(device.getUuid(), "Level", "0"));
						break;

					case SLOW_TURN_OFF:
						LOGGER.info("Channel " + channel + ": Got DIM command");
						// we only know, that the user hold OFF button
						updateValue(device, "Level", "0");
						DBLogger.info("Device is DIM", device.getUuid());
						SensorData.log(device.getUuid(), "Switch", "DIM");
						messaging.broadcast("event.devices.noolite.value.dim", new NooliteDeviceLevelDimAdvertisement().set(device.getUuid()));
						break;

					case TURN_ON:
						LOGGER.info("Channel " + channel + ": Got ON command");
						updateValue(device, "Level", "255");
						DBLogger.info("Device is ON", device.getUuid());
						SensorData.log(device.getUuid(), "Switch", "ON");
						messaging.broadcast("event.devices.noolite.value.set", new NooliteDeviceLevelSetAdvertisement().set(device.getUuid(), "Level", "255"));
						break;

					case SLOW_TURN_ON:
						LOGGER.info("Channel " + channel + ": Got BRIGHT command");
						// we only know, that the user hold ON button
						updateValue(device, "Level", "255");
						DBLogger.info("Device is BRIGHT", device.getUuid());
						SensorData.log(device.getUuid(), "Switch", "BRIGHT");
						messaging.broadcast("event.devices.noolite.value.bright", new NooliteDeviceLevelBrightAdvertisement().set(device.getUuid()));
						break;

					case SET_LEVEL:
						LOGGER.info("Channel " + channel + ": Got SETLEVEL command.");
						updateValue(device, "Level", (String) notification.getValue("level"));
						DBLogger.info("Device get SETLEVEL: " + notification.getValue("level"), device.getUuid());
						SensorData.log(device.getUuid(), "SetLevel", String.valueOf(notification.getValue("level")));
						messaging.broadcast("event.devices.noolite.value.setlevel", new NooliteDeviceLevelSetAdvertisement().set(device.getUuid(), "Level", (String) notification.getValue("level")));
						break;

					case STOP_DIM_BRIGHT:
						LOGGER.info("Channel " + channel + ": Got STOPDIMBRIGHT command.");
						DBLogger.info("Device is STOPDIMBRIGHT", device.getUuid());
						SensorData.log(device.getUuid(), "Switch", "STOPDIMBRIGHT");
						messaging.broadcast("event.devices.noolite.value.stopdimbright", new NooliteDeviceLevelStopDimBrightAdvertisement().set(device.getUuid()));
						break;

					case TEMP_HUMI:
						BatteryState battery = (BatteryState) notification.getValue("battery");
						LOGGER.info("Channel " + channel + ": Got TEMP_HUMI command.");
						DBLogger.info("Device got TEMP_HUMI", device.getUuid());
						updateValue(device, "Temperature", String.valueOf(notification.getValue("temp")));
						SensorData.log(device.getUuid(), "Temperature", String.valueOf(notification.getValue("temp")));
						DBLogger.info("Temperature is " + notification.getValue("temp"), device.getUuid());
						updateValue(device, "Humidity", String.valueOf(notification.getValue("humi")));
						SensorData.log(device.getUuid(), "Humidity", String.valueOf(notification.getValue("humi")));
						DBLogger.info("Humidity is " + notification.getValue("humi"), device.getUuid());
						updateValue(device, "Battery", battery.name());
						SensorData.log(device.getUuid(), "Battery", String.valueOf(notification.getValue("battery")));
						DBLogger.info("Battery is " + battery.name(), device.getUuid());
						messaging.broadcast("event.devices.noolite.value.temphumi", new NooliteDeviceTempHumiAdvertisement().set(device.getUuid()));
						break;

					default:
						LOGGER.info("Unknown command: " + notification.getType().name());
				}
			}
		};

		rx.addWatcher(watcher);
		rx.start();
	}

	private Device loadByChannel(int channel)
	{
		for (Device device : Ebean.find(Device.class).where().eq("source", "noolite").findList())
		{
			if (device.getInternalName().equals("noolite/channel/" + channel))
			{
				return device;
			}

		}

		return null;
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

	///
	///  For intenal commands
	///

	private class InternalCommands implements Runnable
	{
		public InternalCommands()
		{
			Thread t = new Thread(this);
			t.start();
		}

		@Override
		public synchronized void run()
		{
			try
			{
				JsonMessaging jsonMessaging = new JsonMessaging(UUID.randomUUID(), "devices-noolite-rx-internal");

				jsonMessaging.subscribe("event.devices.noolite.rx.bindchannel");
				jsonMessaging.subscribe("event.devices.noolite.rx.unbindchannel");
				jsonMessaging.subscribe("event.devices.noolite.rx.unbindallchannel");

				jsonMessaging.start();

				while (!shutdown)
				{

					// Lets wait for 100 ms on json messages and if nothing comes then proceed to carry out other tasks.
					final JsonEnvelope envelope = jsonMessaging.receive(100);
					if (envelope != null)
					{
						if (envelope.getObject() instanceof BindRXChannelAdvertisment)
						{
							LOGGER.debug("Get BindRXChannel advertisement");

							final BindRXChannelAdvertisment advertisement = envelope.getObject();
							byte channel = (byte) advertisement.getChannel();

							rx.bindChannel(channel);

						}
						else if (envelope.getObject() instanceof UnbindRXChannelAdvertisment)
						{
							LOGGER.debug("Get UnbindRXChannel advertisement");

							final UnbindRXChannelAdvertisment advertisement = envelope.getObject();
							byte channel = (byte) advertisement.getChannel();

							rx.unbindChannel(channel);

						}
						else if (envelope.getObject() instanceof UnbindAllRXChannelAdvertisment)
						{
							LOGGER.debug("Get UnbindAllRXChannel advertisement");

							rx.unbindAllChannels();

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
				}

				// Close JSON messaging.
				jsonMessaging.close();
				messaging.close();

			}
			catch (final Throwable t)
			{
				t.printStackTrace();
				LOGGER.error("Unexpected exception in NooliteRX-Internal", t);
			}
		}
	}
}
