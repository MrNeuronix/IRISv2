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

import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.speak.SpeakAdvertisement;
import ru.iris.common.messaging.model.speak.SpeakRecognizedAdvertisement;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/rest")
public class CommonREST
{
	public CommonREST()
	{
	}

	@GET
	@Path("/recognized/{device}/{text}")
	@Consumes(MediaType.TEXT_PLAIN + ";charset=utf-8")
	public String cmd(@PathParam("device") String device, @PathParam("text") String text)
	{
		JsonMessaging messaging = new JsonMessaging(UUID.randomUUID());

		try
		{
			messaging.broadcast("event.speak.recognized", new SpeakRecognizedAdvertisement().set(text, 100, device));
			return ("{ status: \"sent\" }");
		}
		catch (final Throwable t)
		{
			messaging.close();
			return "{ status: \"error: " + t.toString() + " }";
		}
		finally
		{
			messaging.close();
		}
	}

	@GET
	@Path("/speak/say/{device}/{text}")
	@Consumes(MediaType.TEXT_PLAIN + ";charset=utf-8")
	public String speak(@PathParam("device") String device, @PathParam("text") String text)
	{
		JsonMessaging messaging = new JsonMessaging(UUID.randomUUID());

		try
		{
			messaging.broadcast("event.speak", new SpeakAdvertisement().set(text, 100, device));
			return ("{ status: \"sent\" }");
		}
		catch (final Throwable t)
		{
			messaging.close();
			return "{ status: \"error: " + t.toString() + " }";
		}
		finally
		{
			messaging.close();
		}
	}
}
