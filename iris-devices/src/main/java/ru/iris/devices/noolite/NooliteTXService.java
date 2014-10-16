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
import com.avaje.ebean.Expr;
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

public class NooliteTXService implements Runnable
{

	// Noolite PC USB TX HID
	private static final int VENDOR_ID = 5824; //0x16c0;
	private static final int PRODUCT_ID = 1503; //0x05df;
	private final Logger LOGGER = LogManager.getLogger(NooliteTXService.class.getName());
	private final Context context = new Context();
	private boolean shutdown = false;

	public NooliteTXService()
	{
		Thread t = new Thread(this);
		t.setName("Noolite TX Service");
		t.start();
	}

	@Override
	public synchronized void run()
	{
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

		try
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

			JsonMessaging jsonMessaging = new JsonMessaging(UUID.randomUUID(), "devices-noolite-tx");

			jsonMessaging.subscribe("event.devices.noolite.setvalue");
			jsonMessaging.subscribe("event.devices.noolite.tx.bindchannel");
			jsonMessaging.subscribe("event.devices.noolite.tx.unbindchannel");

			jsonMessaging.start();

			while (!shutdown)
			{

				// Lets wait for 100 ms on json messages and if nothing comes then proceed to carry out other tasks.
				final JsonEnvelope envelope = jsonMessaging.receive(100);
				if (envelope != null)
				{
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
						int channel = Integer.valueOf(device.getValue("channel").getValue()) - 1;
						int channelView = channel + 1;

						ByteBuffer buf = ByteBuffer.allocateDirect(8);
						buf.put((byte) 0x30);

						DeviceValue dv = Ebean.find(DeviceValue.class).where().and(Expr.eq("device_id", device.getId()), Expr.eq("label", "Level")).findUnique();

						//if noolite device dimmer (user set)
						if (device.getValue("type") != null && device.getValue("type").getValue().contains("dimmer"))
						{

							buf.put((byte) 6);
							buf.put((byte) 1);

							if (level > 99 || level == 99)
							{

								LOGGER.info("Turn on device on channel " + channelView);
								updateValue(device, "Level", "255");
								DBLogger.info("Device is ON", device.getUuid());

								level = 100;

							}
							else if (level < 0)
							{

								LOGGER.info("Turn off device on channel " + channelView);
								updateValue(device, "Level", "0");
								DBLogger.info("Device is OFF", device.getUuid());

								level = 0;

							}
							else
							{
								updateValue(device, "Level", String.valueOf(level));
								DBLogger.info("Device level set: " + level, device.getUuid());

								LOGGER.info("Setting device on channel " + channelView + " to level " + level);
							}

							buf.position(5);
							buf.put(level);

							buf.position(4);
							buf.put((byte) channel);

							writeToHID(buf);
						}
						else
						{
							if (level < 0 || level == 0)
							{
								updateValue(device, "Level", "0");
								DBLogger.info("Device is OFF", device.getUuid());

								buf.put((byte) 0);
							}
							else
							{
								// turn on
								LOGGER.info("Turn on device on channel " + channelView);
								updateValue(device, "Level", "255");
								DBLogger.info("Device is ON", device.getUuid());

								buf.put((byte) 2);
							}

							buf.position(4);
							buf.put((byte) channel);

							writeToHID(buf);
						}

					}
					else if (envelope.getObject() instanceof BindTXChannelAdvertisment)
					{

						LOGGER.debug("Get BindTXChannel advertisement");

						final BindRXChannelAdvertisment advertisement = envelope.getObject();
						Device device = Ebean.find(Device.class).where().eq("uuid", advertisement.getDeviceUUID()).findUnique();
						int channel = Integer.valueOf(device.getValue("channel").getValue()) - 1;
						int channelView = channel + 1;

						ByteBuffer buf = ByteBuffer.allocateDirect(8);
						buf.put((byte) 0x30);
						buf.put((byte) 15);
						buf.position(4);
						buf.put((byte) channel);

						LOGGER.info("Binding device to channel " + channelView);
						DBLogger.info("Binding device to channel " + channelView);

						writeToHID(buf);

					}
					else if (envelope.getObject() instanceof UnbindTXChannelAdvertisment)
					{

						LOGGER.debug("Get UnbindTXChannel advertisement");

						final UnbindRXChannelAdvertisment advertisement = envelope.getObject();
						Device device = Ebean.find(Device.class).where().eq("uuid", advertisement.getDeviceUUID()).findUnique();
						int channel = Integer.valueOf(device.getValue("channel").getValue()) - 1;
						int channelView = channel + 1;

						ByteBuffer buf = ByteBuffer.allocateDirect(8);
						buf.put((byte) 0x30);
						buf.put((byte) 9);
						buf.position(4);
						buf.put((byte) channel);

						LOGGER.info("Unbinding device from channel " + channelView);
						DBLogger.info("Unbinding device from channel " + channelView);

						writeToHID(buf);

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
			LibUsb.exit(context);

		}
		catch (final Throwable t)
		{
			t.printStackTrace();
			LOGGER.error("Unexpected exception in NooliteTX", t);
		}
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

	private void writeToHID(ByteBuffer command)
	{

		DeviceHandle handle = LibUsb.openDeviceWithVidPid(context, VENDOR_ID, PRODUCT_ID);

		if (handle == null)
		{
			LOGGER.error("Noolite TX device not found!");
			DBLogger.info("Noolite TX device not found!");
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
			LOGGER.error("Noolite RX device configuration error");
			DBLogger.info("Noolite RX device configuration error");
			LibUsb.close(handle);
			if (ret == LibUsb.ERROR_BUSY)
			{
				LOGGER.error("Noolite RX device is busy");
				LOGGER.error("Noolite RX device is busy");
			}
			return;
		}

		LibUsb.claimInterface(handle, 0);

		LOGGER.debug("TX Buffer: " + command.get(0) + " " + command.get(1) + " " + command.get(2) + " " + command.get(3)
				+ " " + command.get(4) + " " + command.get(5) + " " + command.get(6)
				+ " " + command.get(7));

		//send
		LibUsb.controlTransfer(handle, LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE, 0x9, 0x300, 0, command, 100);

		LibUsb.attachKernelDriver(handle, 0);
		LibUsb.close(handle);
	}
}
