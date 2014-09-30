package ru.iris.restful;

import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.speak.SpeakAdvertisement;
import ru.iris.common.messaging.model.speak.SpeakRecognizedAdvertisement;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 06.09.12
 * Time: 17:24
 * License: GPL v3
 */

@Path("/rest")
public class CommonREST
{

	@GET
	@Path("/recognized/{device}/{text}")
	@Consumes(MediaType.TEXT_PLAIN + ";charset=utf-8")
	public String cmd(@PathParam("text") String device, @PathParam("text") String text)
	{
		JsonMessaging messaging = new JsonMessaging(UUID.randomUUID());
		messaging.broadcast("event.speak.recognized", new SpeakRecognizedAdvertisement().set(text, 100, device));
		messaging.close();

		return ("{ status: \"sent\" }");
	}

	@GET
	@Path("/speak/say/{device}/{text}")
	@Consumes(MediaType.TEXT_PLAIN + ";charset=utf-8")
	public String speak(@PathParam("text") String device, @PathParam("text") String text)
	{
		JsonMessaging messaging = new JsonMessaging(UUID.randomUUID());
		messaging.broadcast("event.speak", new SpeakAdvertisement().set(text, 100, device));
		messaging.close();

		return ("{ status: \"sent\" }");
	}
}
