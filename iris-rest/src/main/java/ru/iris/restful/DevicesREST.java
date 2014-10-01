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

package ru.iris.restful;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.devices.*;
import ru.iris.common.messaging.model.devices.noolite.ResponseNooliteDeviceInventoryAdvertisement;
import ru.iris.common.messaging.model.devices.zwave.ResponseZWaveDeviceInventoryAdvertisement;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/rest/device") class DevicesREST
{
	private static final SetDeviceLevelAdvertisement setDeviceLevelAdvertisement = new SetDeviceLevelAdvertisement();
	private static final SetDeviceNameAdvertisement setDeviceNameAdvertisement = new SetDeviceNameAdvertisement();
	private static final SetDeviceZoneAdvertisement setDeviceZoneAdvertisement = new SetDeviceZoneAdvertisement();
	private static final GetInventoryAdvertisement getInventoryAdvertisement = new GetInventoryAdvertisement();
	private static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();

	// Инвентаризация одного устройства по UUID
	@GET
	@Path("/{uuid}")
	@Produces(MediaType.TEXT_PLAIN + ";charset=utf-8")
	public String device(@PathParam("uuid") String uuid)
	{
		JsonMessaging messaging = new JsonMessaging(UUID.randomUUID());

		try
		{

			messaging.subscribe("event.devices.responseinventory");
			messaging.start();

			messaging.broadcast("event.devices.getinventory", getInventoryAdvertisement.set(uuid));

			final JsonEnvelope envelope = messaging.receive(5000);
			if (envelope != null)
			{
				if (envelope.getObject() instanceof ResponseDeviceInventoryAdvertisement)
				{
					messaging.close();
					ResponseDeviceInventoryAdvertisement advertisement = envelope.getObject();
					return (gson.toJson(advertisement.getDevices()));
				}
				else if (envelope.getObject() instanceof ResponseZWaveDeviceInventoryAdvertisement)
				{
					messaging.close();
					ResponseZWaveDeviceInventoryAdvertisement advertisement = envelope.getObject();
					return (gson.toJson(advertisement.getDevice()));
				}
				else if (envelope.getObject() instanceof ResponseNooliteDeviceInventoryAdvertisement)
				{
					messaging.close();
					ResponseNooliteDeviceInventoryAdvertisement advertisement = envelope.getObject();
					return (gson.toJson(advertisement.getDevice()));
				}
				else
				{
					messaging.close();
					return ("{ \"error\": \"Unknown response! Class: " + envelope.getObject().getClass() + " Response: " + envelope.getObject() + "\" }");
				}
			}
		}
		catch (final Throwable t)
		{
			messaging.close();
			return ("{ \"error\": \"" + t.toString() + "\" }");
		}

		messaging.close();
		return ("{ \"error\": \"no answer\" }");
	}

	// Установка уровня
	@GET
	@Path("/{uuid}/{label}/{level}")
	@Consumes(MediaType.TEXT_PLAIN + ";charset=utf-8")
	public String devSetLevel(@PathParam("uuid") String uuid, @PathParam("label") String label, @PathParam("level") String level)
	{
		JsonMessaging messaging = new JsonMessaging(UUID.randomUUID());

		try
		{
			messaging.broadcast("event.devices.setvalue", setDeviceLevelAdvertisement.set(uuid, label, level));
			messaging.close();

			return "{ status: \"sent\" }";

		}
		catch (final Throwable t)
		{
			messaging.close();
			return "{ status: \"error: " + t.toString() + " }";
		}
	}

	// Установка имени
	@GET
	@Path("/{uuid}/name/{name}")
	@Consumes(MediaType.TEXT_PLAIN + ";charset=utf-8")
	public String devSetName(@PathParam("uuid") String uuid, @PathParam("name") String name)
	{
		JsonMessaging messaging = new JsonMessaging(UUID.randomUUID());
		messaging.broadcast("event.devices.setname", setDeviceNameAdvertisement.set(uuid, name));
		messaging.close();

		return ("{ status: \"sent\" }");
	}

	// Установка зоны
	@GET
	@Path("/{uuid}/zone/{zone}")
	@Consumes(MediaType.TEXT_PLAIN + ";charset=utf-8")
	public String devSetZone(@PathParam("uuid") String uuid, @PathParam("zone") int zone)
	{
		JsonMessaging messaging = new JsonMessaging(UUID.randomUUID());
		messaging.broadcast("event.devices.setzone", setDeviceZoneAdvertisement.set(uuid, zone));
		messaging.close();

		return ("{ status: \"sent\" }");
	}
}
