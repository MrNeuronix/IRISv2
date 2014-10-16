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
import de.ailis.usb4java.libusb.Context;
import de.ailis.usb4java.libusb.DeviceHandle;
import de.ailis.usb4java.libusb.LibUsb;
import de.ailis.usb4java.libusb.LibUsbException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.database.model.devices.Device;
import ru.iris.common.database.model.devices.DeviceValue;
import ru.iris.common.helpers.DBLogger;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.devices.noolite.*;

import java.nio.ByteBuffer;
import java.util.UUID;

public class NooliteRXService implements Runnable
{

	private static final long READ_UPDATE_DELAY_MS = 500L;
	// Noolite PC USB RX HID
	private static final int VENDOR_ID = 5824; // 0x16c0;
	private static final int PRODUCT_ID = 1500; // 0x05dc;
	private final Logger LOGGER = LogManager.getLogger(NooliteRXService.class.getName());
	private final Context context = new Context();
	private boolean shutdown = false;
	private JsonMessaging messaging;
	private DeviceHandle handle;
	private boolean pause = false;

	public NooliteRXService()
	{
		Thread t = new Thread(this);
		t.setName("Noolite RX Service");
		t.start();
	}

	@Override
	public synchronized void run()
	{

		// Make sure we exit the wait loop if we receive shutdown signal.
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				shutdown = true;
			}
		}));

		messaging = new JsonMessaging(UUID.randomUUID(), "devices-noolite-rx");

		///////////////////////////////////////////////////////////////

		// Initialize the libusb context
		int result = LibUsb.init(context);
		if (result < 0)
		{
			try
			{
				throw new LibUsbException("Unable to initialize libusb", result);
			}
			catch (LibUsbException e)
			{
				e.printStackTrace();
			}
		}

		handle = LibUsb.openDeviceWithVidPid(context, VENDOR_ID, PRODUCT_ID);

		if (handle == null)
		{
			LOGGER.error("Noolite RX device not found!");
			shutdown = true;
			return;
		}

		if (LibUsb.kernelDriverActive(handle, 0) == 1)
		{
			LibUsb.detachKernelDriver(handle, 0);
		}

		int ret = LibUsb.setConfiguration(handle, 1);

		if (ret < 0)
		{
			LOGGER.error("Configuration error");
			LibUsb.close(handle);
			if (ret == LibUsb.ERROR_BUSY)
			{
				LOGGER.error("Device busy");
			}
			return;
		}

		LibUsb.claimInterface(handle, 0);

		ByteBuffer tmpBuf = ByteBuffer.allocateDirect(8);

		new InternalCommands();

		boolean initComplete = false;

		while (!shutdown)
		{
			// receiving area
			ByteBuffer buf = ByteBuffer.allocateDirect(8);

			if (!pause)
			{
				LibUsb.controlTransfer(handle, LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE | LibUsb.ENDPOINT_IN, 0x9, 0x300, 0, buf, 100);
			}

			// buf filled by all nulls is correct!
			if (!buf.equals(tmpBuf))
			{
				LOGGER.debug("RX Buffer: " + buf.get(0) + " " + buf.get(1) + " " + buf.get(2) + " " + buf.get(3) + " " + buf.get(4) + " " + buf.get(5) + " " + buf.get(6)
						+ " " + buf.get(7));

				Integer channel = (buf.get(1) + 1);
				byte action = buf.get(2);
				Integer dimmerValue = (int) buf.get(4);

				Device device = loadByChannel(channel);

				if (device == null)
				{
					device = new Device();
					device.setSource("noolite");
					device.setInternalName("noolite/channel/" + channel);
					device.setStatus("listening");
					device.setType("Noolite Device");
					device.setManufName("Nootechnika");
					device.setInternalType("switch");
					device.setNode((short) (1000 + channel));
					device.setUuid(UUID.randomUUID().toString());

					device.save();

					new DeviceValue(device, "noolite", "channel", channel.toString(), "", "", device.getUuid(), true).save();
					new DeviceValue(device, "noolite", "type", "switch", "", "", device.getUuid(), false).save();
				}

				// turn off
				if (action == 0)
				{
					LOGGER.info("Channel " + channel + ": Got OFF command");
					updateValue(device, "Level", "0");
					DBLogger.info("Device is OFF", device.getUuid());

					messaging.broadcast("event.devices.noolite.value.set", new NooliteDeviceLevelSetAdvertisement().set(device.getUuid(), "Level", "0"));
				}
				// dim
				else if (action == 1)
				{

					LOGGER.info("Channel " + channel + ": Got DIM command");
					// we only know, that the user hold OFF button
					updateValue(device, "Level", "0");
					DBLogger.info("Device is DIM", device.getUuid());

					messaging.broadcast("event.devices.noolite.value.dim", new NooliteDeviceLevelDimAdvertisement().set(device.getUuid()));
				}
				// turn on
				else if (action == 2)
				{
					LOGGER.info("Channel " + channel + ": Got ON command");
					updateValue(device, "Level", "255");
					DBLogger.info("Device is ON", device.getUuid());

					messaging.broadcast("event.devices.noolite.value.set", new NooliteDeviceLevelSetAdvertisement().set(device.getUuid(), "Level", "255"));
				}
				// bright
				else if (action == 3)
				{
					LOGGER.info("Channel " + channel + ": Got BRIGHT command");
					// we only know, that the user hold ON button
					updateValue(device, "Level", "255");
					DBLogger.info("Device is BRIGHT", device.getUuid());

					messaging.broadcast("event.devices.noolite.value.bright", new NooliteDeviceLevelBrightAdvertisement().set(device.getUuid()));
				}
				// set level
				else if (action == 6)
				{
					LOGGER.info("Channel " + channel + ": Got SETLEVEL command.");
					updateValue(device, "Level", dimmerValue.toString());
					DBLogger.info("Device get SETLEVEL: " + dimmerValue.toString(), device.getUuid());

					messaging.broadcast("event.devices.noolite.value.setlevel", new NooliteDeviceLevelSetAdvertisement().set(device.getUuid(), "Level", dimmerValue.toString()));
				}
				// stop dim/bright
				else if (action == 10)
				{
					LOGGER.info("Channel " + channel + ": Got STOPDIMBRIGHT command.");
					DBLogger.info("Device is STOPDIMBRIGHT", device.getUuid());

					messaging.broadcast("event.devices.noolite.value.stopdimbright", new NooliteDeviceLevelStopDimBrightAdvertisement().set(device.getUuid()));
				}

				LOGGER.info("Update Noolite device (Node: " + device.getId() + ")");

				tmpBuf = buf;
			}

			try
			{
				Thread.sleep(READ_UPDATE_DELAY_MS);
			}
			catch (InterruptedException e)
			{
				//Ignore
				e.printStackTrace();
			}
		}

		LibUsb.attachKernelDriver(handle, 0);
		LibUsb.close(handle);
		LibUsb.exit(context);
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
			deviceValue.setSource("noolite");
			deviceValue.setUuid(device.getUuid());
			deviceValue.setDevice(device);
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
							Device device = Ebean.find(Device.class).where().eq("uuid", advertisement.getDeviceUUID()).findUnique();
							int channel = Integer.valueOf(device.getValue("channel").getValue());

							ByteBuffer buf = ByteBuffer.allocateDirect(8);
							buf.put((byte) 1);
							buf.put((byte) channel);

							pause = true;
							LibUsb.controlTransfer(handle, LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE | LibUsb.ENDPOINT_IN, 0x9, 0x300, 0, buf, 100);
							pause = false;

						}
						else if (envelope.getObject() instanceof UnbindRXChannelAdvertisment)
						{

							LOGGER.debug("Get UnbindRXChannel advertisement");

							final UnbindRXChannelAdvertisment advertisement = envelope.getObject();
							Device device = Ebean.find(Device.class).where().eq("uuid", advertisement.getDeviceUUID()).findUnique();
							int channel = Integer.valueOf(device.getValue("channel").getValue());

							ByteBuffer buf = ByteBuffer.allocateDirect(8);
							buf.put((byte) 3);
							buf.put((byte) channel);

							pause = true;
							LibUsb.controlTransfer(handle, LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE | LibUsb.ENDPOINT_IN, 0x9, 0x300, 0, buf, 100);
							pause = false;

						}
						else if (envelope.getObject() instanceof UnbindAllRXChannelAdvertisment)
						{

							LOGGER.debug("Get UnbindAllRXChannel advertisement");

							ByteBuffer buf = ByteBuffer.allocateDirect(8);
							buf.put((byte) 4);

							pause = true;
							LibUsb.controlTransfer(handle, LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE | LibUsb.ENDPOINT_IN, 0x9, 0x300, 0, buf, 100);
							pause = false;

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
