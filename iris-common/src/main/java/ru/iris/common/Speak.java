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

package ru.iris.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.speak.SpeakAdvertisement;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: nikolay.viguro
 * Date: 10.09.13
 * Time: 10:49
 * To change this template use File | Settings | File Templates.
 */
public class Speak
{

	private static final Logger LOGGER = LogManager.getLogger(Speak.class);
	private static final SpeakAdvertisement advertisement = new SpeakAdvertisement();

	public void say(String text)
	{

		try
		{
			JsonMessaging messaging = new JsonMessaging(UUID.randomUUID());
			messaging.broadcast("event.speak", advertisement.set(text, 100.0));
			messaging.close();
		}
		catch (Exception e)
		{
			LOGGER.info("Error! Failed to speak: " + text);
		}
	}
}
