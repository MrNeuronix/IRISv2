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

package ru.iris.events;

import com.avaje.ebean.Ebean;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import ru.iris.common.database.model.Event;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.JsonNotification;
import ru.iris.common.messaging.model.command.CommandAdvertisement;
import ru.iris.common.messaging.model.events.EventChangesAdvertisement;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.UUID;

/**
 * Author: Nikolay A. Viguro
 * Date: 19.11.13
 * Time: 11:25
 */

public class EventsService
{
	private final Logger LOGGER = LogManager.getLogger(EventsService.class.getName());

    public EventsService()
	{
		final List<Event> events = Ebean.find(Event.class).findList();

            // take pause to save/remove new entity
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

			final JsonMessaging jsonMessaging = new JsonMessaging(UUID.randomUUID(), "events");
			final Logger scriptLogger = LogManager.getLogger(EventsService.class.getName());

			// subscribe to events from db
            for(Event event : events)
            {
                jsonMessaging.subscribe(event.getSubject());
                LOGGER.debug("Subscribe to subject: " + event.getSubject());
            }

			// command launch
			jsonMessaging.subscribe("event.command");

		jsonMessaging.setNotification(new JsonNotification() {
			@Override
			public void onNotification(JsonEnvelope envelope) {

				try {

					Context cx = Context.enter();
					Scriptable scope = new ImporterTopLevel(cx);

					// Pass jsonmessaging instance to js engine
					ScriptableObject.putProperty(scope, "jsonMessaging", Context.javaToJS(jsonMessaging, scope));
					ScriptableObject.putProperty(scope, "out", Context.javaToJS(System.out, scope));
					ScriptableObject.putProperty(scope, "LOGGER", Context.javaToJS(scriptLogger, scope));

					LOGGER.debug("Got envelope with subject: " + envelope.getSubject());

					// Check command and launch script
					if (envelope.getObject() instanceof CommandAdvertisement) {
						CommandAdvertisement advertisement = envelope.getObject();
						LOGGER.info("Launch command script: " + advertisement.getScript());

						File jsFile = new File("./scripts/command/" + advertisement.getScript() + ".js");

						try {
							ScriptableObject.putProperty(scope, "commandParams", Context.javaToJS(advertisement.getData(), scope));
							cx.evaluateString(scope, FileUtils.readFileToString(jsFile), jsFile.toString(), 1, null);
						} catch (FileNotFoundException e) {
							LOGGER.error("Script file scripts/command/" + advertisement.getScript() + ".js not found!");
						} catch (Exception e) {
							LOGGER.error("Error in script scripts/command/" + advertisement.getScript() + ".js: " + e.toString());
							e.printStackTrace();
						}
					} else if (envelope.getObject() instanceof EventChangesAdvertisement) {
						LOGGER.info("Restart event service");

						jsonMessaging.close();
						new EventsService();
					} else {
						for (Event event : events) {
							if (envelope.getSubject().equals(event.getSubject()) || wildCardMatch(event.getSubject(), envelope.getSubject())) {
								File jsFile = new File("./scripts/" + event.getScript());

								LOGGER.debug("Launch script: " + event.getScript());

								try {
									ScriptableObject.putProperty(scope, "advertisement", Context.javaToJS(envelope.getObject(), scope));
									cx.evaluateString(scope, FileUtils.readFileToString(jsFile), jsFile.toString(), 1, null);
								} catch (FileNotFoundException e) {
									LOGGER.error("Script file " + jsFile + " not found!");
								} catch (Exception e) {
									LOGGER.error("Error in script " + jsFile + ": " + e.toString());
									e.printStackTrace();
								}
							}
						}
					}

				} finally {
					Context.exit();
				}
				}
		});

		jsonMessaging.start();
	}

	private boolean wildCardMatch(String pattern, String text)
	{

		// add sentinel so don't need to worry about *'s at end of pattern
		text += '\0';
		pattern += '\0';

		int N = pattern.length();

		boolean[] states = new boolean[N + 1];
		boolean[] old = new boolean[N + 1];
		old[0] = true;

		for (int i = 0; i < text.length(); i++)
		{
			char c = text.charAt(i);
			states = new boolean[N + 1]; // initialized to false
			for (int j = 0; j < N; j++)
			{
				char p = pattern.charAt(j);

				// hack to handle *'s that match 0 characters
				if (old[j] && (p == '*'))
				{
					old[j + 1] = true;
				}

				if (old[j] && (p == c))
				{
					states[j + 1] = true;
				}
				if (old[j] && (p == '*'))
				{
					states[j] = true;
				}
				if (old[j] && (p == '*'))
				{
					states[j + 1] = true;
				}
			}
			old = states;
		}
        return states[N];
    }
}
