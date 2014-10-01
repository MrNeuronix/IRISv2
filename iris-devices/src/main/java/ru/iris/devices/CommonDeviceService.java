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

package ru.iris.devices;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.database.model.devices.Device;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.devices.*;
import ru.iris.common.messaging.model.devices.noolite.ResponseNooliteDeviceInventoryAdvertisement;
import ru.iris.common.messaging.model.devices.zwave.ResponseZWaveDeviceInventoryAdvertisement;

import java.util.Map;
import java.util.UUID;

class CommonDeviceService implements Runnable
{

	private final Logger log = LogManager.getLogger(CommonDeviceService.class.getName());
	private boolean shutdown = false;

	public CommonDeviceService()
	{
		Thread t = new Thread(this);
		t.setName("Common Device Service");
		t.start();
	}

	@Override
	public synchronized void run()
	{
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

			JsonMessaging jsonMessaging = new JsonMessaging(UUID.randomUUID());

			jsonMessaging.subscribe("event.devices.setvalue");
			jsonMessaging.subscribe("event.devices.getinventory");
			jsonMessaging.subscribe("event.devices.setname");
			jsonMessaging.subscribe("event.devices.setzone");

			jsonMessaging.start();

			while (!shutdown)
			{

				// Lets wait for 100 ms on json messages and if nothing comes then proceed to carry out other tasks.
				final JsonEnvelope envelope = jsonMessaging.receive(100);
				if (envelope != null)
				{

					////////////////////////////////////////////
					//// Setting level to device            ////
					////////////////////////////////////////////

					if (envelope.getObject() instanceof SetDeviceLevelAdvertisement)
					{

						// We know of service advertisement
						final SetDeviceLevelAdvertisement advertisement = envelope.getObject();

						String uuid = advertisement.getDeviceUUID();
						String label = advertisement.getLabel();
						String level = advertisement.getValue();

						Device device = Ebean.find(Device.class)
								.where().eq("uuid", uuid).findUnique();

						if (device == null)
						{
							log.info("Cant find device with UUID " + uuid);
							continue;
						}

						if (device.getSource().equals("zwave"))
						{
							jsonMessaging.broadcast("event.devices.zwave.setvalue", new SetDeviceLevelAdvertisement().set(uuid, label, level));
						}
						else if (device.getSource().equals("noolite"))
						{
							jsonMessaging.broadcast("event.devices.noolite.setvalue", new SetDeviceLevelAdvertisement().set(uuid, label, level));
						}

						////////////////////////////////////////////
						//// Get inventory                      ////
						////////////////////////////////////////////

					}
					else if (envelope.getObject() instanceof GetInventoryAdvertisement)
					{

						final GetInventoryAdvertisement advertisement = envelope.getObject();

						String uuid = advertisement.getDeviceUUID();

						// let send all devices (full inventory)
						if (uuid.equals("all"))
						{

							Query<Device> query = Ebean.createQuery(Device.class);
							query.setMapKey("internalname");
							Map<?, Device> devices = query.findMap();

							jsonMessaging.broadcast("event.devices.responseinventory", new ResponseDeviceInventoryAdvertisement().set(devices));

							// send one device specified by UUID
						}
						else
						{

							Device device = Ebean.find(Device.class)
									.where().eq("uuid", uuid).findUnique();

							if (device == null)
							{
								log.info("Cant find device with UUID " + uuid);
								continue;
							}

							if (device.getSource().equals("zwave"))
							{
								jsonMessaging.broadcast("event.devices.responseinventory", new ResponseZWaveDeviceInventoryAdvertisement().set(device));
							}
							else if (device.getSource().equals("noolite"))
							{
								jsonMessaging.broadcast("event.devices.responseinventory", new ResponseNooliteDeviceInventoryAdvertisement().set(device));
							}
						}

						////////////////////////////////////////////
						//// Set device name                    ////
						////////////////////////////////////////////

					}
					else if (envelope.getObject() instanceof SetDeviceNameAdvertisement)
					{

						SetDeviceNameAdvertisement advertisement = envelope.getObject();

						String uuid = advertisement.getDeviceUUID();
						Device device = Ebean.find(Device.class)
								.where().eq("uuid", uuid).findUnique();

						if (device == null)
						{
							log.info("Cant find device with UUID " + uuid);
							continue;
						}

						log.info("Setting name \"" + advertisement.getName() + "\" to device " + uuid);

						device.setName(advertisement.getName());
						Ebean.update(device);

						////////////////////////////////////////////
						//// Set device zone                    ////
						////////////////////////////////////////////

					}
					else if (envelope.getObject() instanceof SetDeviceZoneAdvertisement)
					{

						SetDeviceZoneAdvertisement advertisement = envelope.getObject();

						String uuid = advertisement.getDeviceUUID();
						Device device = Ebean.find(Device.class)
								.where().eq("uuid", uuid).findUnique();

						if (device == null)
						{
							log.info("Cant find device with UUID " + uuid);
							continue;
						}

						log.info("Setting zone " + advertisement.getZone() + " to device " + uuid);

						device.setZone(advertisement.getZone());
						Ebean.update(device);

						////////////////////////////////////////////
						//// Unknown broadcast                  ////
						////////////////////////////////////////////

					}
					else if (envelope.getReceiverInstance() == null)
					{
						// We received unknown broadcast message. Lets make generic log entry.
						log.info("Received broadcast "
								+ " from " + envelope.getSenderInstance()
								+ " to " + envelope.getReceiverInstance()
								+ " at '" + envelope.getSubject()
								+ ": " + envelope.getObject());

						////////////////////////////////////////////
						//// Unknown request                    ////
						////////////////////////////////////////////

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

			jsonMessaging.close();
		}
		catch (InterruptedException e)
		{
			log.error(e.toString());
		}
	}
}
