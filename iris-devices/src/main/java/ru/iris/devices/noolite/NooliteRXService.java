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
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.ServiceCheckEmitter;
import ru.iris.common.messaging.model.devices.noolite.*;
import ru.iris.common.messaging.model.service.ServiceStatus;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 05.01.14
 * Time: 13:57
 * License: GPL v3
 */

public class NooliteRXService implements Runnable
{

	private Logger log = LogManager.getLogger(NooliteRXService.class.getName());
	private boolean shutdown = false;
	private JsonMessaging messaging;
	private List<Device> devices = new ArrayList<>();
	protected final Context context = new Context();
	protected DeviceHandle handle;
	protected boolean pause = false;
	private ServiceCheckEmitter serviceCheckEmitter;

	private static final long READ_UPDATE_DELAY_MS = 500L;

	// Noolite PC USB RX HID
	static final int VENDOR_ID = 5824; // 0x16c0;
	static final int PRODUCT_ID = 1500; // 0x05dc;

	public NooliteRXService()
	{

		serviceCheckEmitter = new ServiceCheckEmitter("Devices-NooliteRX");
		serviceCheckEmitter.setState(ServiceStatus.STARTUP);

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

		messaging = new JsonMessaging(UUID.randomUUID());

		devices = Ebean.find(Device.class)
				.where().eq("source", "noolite").findList();

		serviceCheckEmitter.setState(ServiceStatus.AVAILABLE);

		///////////////////////////////////////////////////////////////

		// Initialize the libusb context
		int result = LibUsb.init(context);
		if (result < 0)
			try
			{
				throw new LibUsbException("Unable to initialize libusb", result);
			}
			catch (LibUsbException e)
			{
				e.printStackTrace();
			}

		handle = LibUsb.openDeviceWithVidPid(context, VENDOR_ID, PRODUCT_ID);

		if (handle == null)
		{
			log.error("Noolite RX device not found!");
			shutdown = true;
			return;
		}

		if (LibUsb.kernelDriverActive(handle, 0) == 1)
			LibUsb.detachKernelDriver(handle, 0);

		int ret = LibUsb.setConfiguration(handle, 1);

		if (ret < 0)
		{
			log.error("Configuration error");
			LibUsb.close(handle);
			if (ret == LibUsb.ERROR_BUSY)
				log.error("Device busy");
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
				LibUsb.controlTransfer(handle, LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE | LibUsb.ENDPOINT_IN, 0x9, 0x300, 0, buf, 100);

			// buf filled by all nulls is correct!
			if (!buf.equals(tmpBuf) || !initComplete)
			{
				log.debug("RX Buffer: " + buf.get(0) + " " + buf.get(1) + " " + buf.get(2) + " " + buf.get(3) + " " + buf.get(4) + " " + buf.get(5) + " " + buf.get(6)
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
					device.setType("Generic Noolite Device");
					device.setManufName("Nootechnika");
					device.setUuid(UUID.randomUUID().toString());
					device.updateValue(new DeviceValue("channel", channel.toString(), "", "", true));
					device.updateValue(new DeviceValue("type", "generic", "", "", false));

					devices.add(device);
				}

				DeviceValue dv = Ebean.find(DeviceValue.class).where().and(Expr.eq("device_id", device.getId()), Expr.eq("label", "Level")).findUnique();

				// turn off
				if (action == 0)
				{

					log.info("Channel " + channel + ": Got OFF command");

					if (dv == null)
					{
						device.updateValue(new DeviceValue("Level", "0", "", "", false));
					}
					else
					{
						dv.setValue("0");
						Ebean.update(dv);
					}

					messaging.broadcast("event.devices.noolite.value.set", new NooliteDeviceLevelSetAdvertisement().set(device.getUuid(), "Level", "0"));
				}
				// dim
				else if (action == 1)
				{

					log.info("Channel " + channel + ": Got DIM command");
					// we only know, that the user hold OFF button

					if (dv == null)
					{
						device.updateValue(new DeviceValue("Level", "0", "", "", false));
					}
					else
					{
						dv.setValue("0");
						Ebean.update(dv);
					}

					messaging.broadcast("event.devices.noolite.value.set", new NooliteDeviceLevelDimAdvertisement().set(device.getUuid()));
				}
				// turn on
				else if (action == 2)
				{

					log.info("Channel " + channel + ": Got ON command");

					if (dv == null)
					{
						device.updateValue(new DeviceValue("Level", "100", "", "", false));
					}
					else
					{
						dv.setValue("100");
						Ebean.update(dv);
					}

					messaging.broadcast("event.devices.noolite.value.set", new NooliteDeviceLevelSetAdvertisement().set(device.getUuid(), "Level", "100"));
				}
				// bright
				else if (action == 3)
				{
					log.info("Channel " + channel + ": Got BRIGHT command");

					// we only know, that the user hold ON button
					if (dv == null)
					{
						device.updateValue(new DeviceValue("Level", "100", "", "", false));
					}
					else
					{
						dv.setValue("100");
						Ebean.update(dv);
					}

					messaging.broadcast("event.devices.noolite.value.set", new NooliteDeviceLevelBrightAdvertisement().set(device.getUuid()));
				}
				// set level
				else if (action == 6)
				{

					log.info("Channel " + channel + ": Got SETLEVEL command.");

					if (dv == null)
					{
						device.updateValue(new DeviceValue("Level", dimmerValue.toString(), "", "", false));
					}
					else
					{
						dv.setValue(dimmerValue.toString());
						Ebean.update(dv);
					}

					messaging.broadcast("event.devices.noolite.value.set", new NooliteDeviceLevelSetAdvertisement().set(device.getUuid(), "Level", dimmerValue.toString()));
				}
				// stop dim/bright
				else if (action == 10)
				{
					log.info("Channel " + channel + ": Got STOPDIMBRIGHT command.");
					messaging.broadcast("event.devices.noolite.value.set", new NooliteDeviceLevelStopDimBrightAdvertisement().set(device.getUuid()));
				}

				if (device.getId() == null)
				{
					log.info("Save new Noolite device");
					Ebean.save(device);
				}
				else
				{
					log.info("Update existing Noolite device");
					Ebean.update(device);
				}

				// reload from database for avoid Ebean.update() key duplicate error
				if (!initComplete)
				{
					log.info("Reloading noolite devices");
					devices = Ebean.find(Device.class)
							.where().eq("source", "noolite").findList();

					initComplete = true;
				}

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

		// Broadcast that this service is shutdown.
		serviceCheckEmitter.setState(ServiceStatus.SHUTDOWN);
	}

	private Device loadByChannel(int channel)
	{
		for (Device device : devices)
		{
			if (device.getInternalName().equals("noolite/channel/" + channel))
			{
				return device;
			}

		}

		return null;
	}

	///
	///  For intenal commands
	///

	private class InternalCommands implements Runnable
	{
		private ServiceCheckEmitter serviceCheckEmitter;

		public InternalCommands()
		{

			serviceCheckEmitter = new ServiceCheckEmitter("Devices-NooliteRX-Internal");
			serviceCheckEmitter.setState(ServiceStatus.STARTUP);

			Thread t = new Thread(this);
			t.start();
		}

		@Override
		public synchronized void run()
		{

			serviceCheckEmitter.setState(ServiceStatus.AVAILABLE);

			try
			{
				JsonMessaging jsonMessaging = new JsonMessaging(UUID.randomUUID());

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

							log.debug("Get BindRXChannel advertisement");

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

							log.debug("Get UnbindRXChannel advertisement");

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

							log.debug("Get UnbindAllRXChannel advertisement");

							ByteBuffer buf = ByteBuffer.allocateDirect(8);
							buf.put((byte) 4);

							pause = true;
							LibUsb.controlTransfer(handle, LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE | LibUsb.ENDPOINT_IN, 0x9, 0x300, 0, buf, 100);
							pause = false;

						}
						else if (envelope.getReceiverInstance() == null)
						{
							// We received unknown broadcast message. Lets make generic log entry.
							log.info("Received broadcast "
									+ " from " + envelope.getSenderInstance()
									+ " to " + envelope.getReceiverInstance()
									+ " at '" + envelope.getSubject()
									+ ": " + envelope.getObject());
						}
						else
						{
							// We received unknown request message. Lets make generic log entry.
							log.info("Received request "
									+ " from " + envelope.getSenderInstance()
									+ " to " + envelope.getReceiverInstance()
									+ " at '" + envelope.getSubject()
									+ ": " + envelope.getObject());
						}
					}
				}

				// Broadcast that this service is shutdown.
				serviceCheckEmitter.setState(ServiceStatus.SHUTDOWN);

				// Close JSON messaging.
				jsonMessaging.close();
				messaging.close();

			}
			catch (final Throwable t)
			{
				t.printStackTrace();
				log.error("Unexpected exception in NooliteRX-Internal", t);
			}
		}
	}
}
