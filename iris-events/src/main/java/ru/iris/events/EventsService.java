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
import ru.iris.common.database.model.Event;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.JsonNotification;
import ru.iris.common.messaging.model.command.CommandAdvertisement;
import ru.iris.common.messaging.model.events.*;
import ru.iris.common.modulestatus.Status;

import javax.script.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Author: Nikolay A. Viguro
 * Date: 19.11.13
 * Time: 11:25
 */

public class EventsService
{
	private final Logger LOGGER = LogManager.getLogger(EventsService.class.getName());
	private final Compilable engine = (Compilable) new ScriptEngineManager().getEngineByName("nashorn");
	private Map<String, CompiledScript> compiledScriptMap;
	private Map<String, CompiledScript> compiledCommandScriptMap = new HashMap<>();
	private final JsonMessaging jsonMessaging = new JsonMessaging(UUID.randomUUID(), "events");
	private final Logger scriptLogger = LogManager.getLogger(EventsService.class.getName());
	private List<Event> events;

	public EventsService()
	{
		Status status = new Status("Events");

		if (status.checkExist()) {
			status.running();
		} else {
			status.addIntoDB("Events", "Service that listen for events and exec scripts as needed");
		}

		try {

			events = Ebean.find(Event.class).findList();

			// take pause to save/remove new entity
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// load all scripts, compile and put into map
			compiledScriptMap = loadAndCompile(events);

			// Pass jsonmessaging instance to js engine
			Bindings bindings = new SimpleBindings();
			bindings.put("jsonMessaging", jsonMessaging);
			bindings.put("out", System.out);
			bindings.put("LOGGER", scriptLogger);

			// subscribe to events from db
			for (Event event : events) {
				jsonMessaging.subscribe(event.getSubject());
				LOGGER.debug("Subscribe to subject: " + event.getSubject());
			}

			// command launch
			jsonMessaging.subscribe("event.command");

			// scripts
			jsonMessaging.subscribe("event.script.get");
			jsonMessaging.subscribe("event.script.save");
			jsonMessaging.subscribe("event.script.delete");
			jsonMessaging.subscribe("event.script.list");
			jsonMessaging.subscribe("event.reload");

			jsonMessaging.setNotification(new JsonNotification() {

				@Override
				public void onNotification(JsonEnvelope envelope) {

					LOGGER.debug("Got envelope with subject: " + envelope.getSubject());

					try {

						// Get script content
						if (envelope.getObject() instanceof EventGetScriptAdvertisement) {
							LOGGER.debug("Return JS script to: " + envelope.getReceiverInstance());
							EventGetScriptAdvertisement advertisement = envelope.getObject();
							File jsFile;

							if (advertisement.isCommand())
								jsFile = new File("./scripts/command/" + advertisement.getName());
							else
								jsFile = new File("./scripts/" + advertisement.getName());

							jsonMessaging.response(envelope, new EventResponseGetScriptAdvertisement(FileUtils.readFileToString(jsFile)));

						}
						// Save new/existing script
						else if (envelope.getObject() instanceof EventResponseSaveScriptAdvertisement) {

							EventResponseSaveScriptAdvertisement advertisement = envelope.getObject();
							LOGGER.debug("Request to save changes: " + advertisement.getName());
							File jsFile;

							if (advertisement.isCommand())
								jsFile = new File("./scripts/command/" + advertisement.getName());
							else
								jsFile = new File("./scripts/" + advertisement.getName());

							FileUtils.writeStringToFile(jsFile, advertisement.getBody());
							LOGGER.info("Restart event service (reason: script change)");
							reloadService();
						}
						// Remove script
						else if (envelope.getObject() instanceof EventRemoveScriptAdvertisement) {

							EventRemoveScriptAdvertisement advertisement = envelope.getObject();
							LOGGER.debug("Request to remove script: " + advertisement.getName());
							File jsFile;

							if (advertisement.isCommand())
								jsFile = new File("./scripts/command/" + advertisement.getName());
							else
								jsFile = new File("./scripts/" + advertisement.getName());

							FileUtils.forceDelete(jsFile);
							LOGGER.info("Restart event service (reason: script removed)");
							reloadService();
						}
						// List available scripts
						else if (envelope.getObject() instanceof EventListScriptsAdvertisement) {

							EventListScriptsAdvertisement advertisement = envelope.getObject();
							File jsFile;

							if (advertisement.isCommand())
								jsFile = new File("./scripts/command/");
							else
								jsFile = new File("./scripts/");

							EventResponseListScriptsAdvertisement response = new EventResponseListScriptsAdvertisement();
							response.setScripts((List<File>) FileUtils.listFiles(jsFile, new String[]{"js"}, false));

							jsonMessaging.response(envelope, response);
						}
						// Check command and launch script
						else if (envelope.getObject() instanceof CommandAdvertisement) {

							CommandAdvertisement advertisement = envelope.getObject();
							bindings.put("advertisement", envelope.getObject());

							if (compiledCommandScriptMap.get(advertisement.getScript()) == null) {

								LOGGER.debug("Compile command script: " + advertisement.getScript());

								File jsFile = new File("./scripts/command/" + advertisement.getScript());
								CompiledScript compile = engine.compile(FileUtils.readFileToString(jsFile));
								compiledCommandScriptMap.put(advertisement.getScript(), compile);

								LOGGER.debug("Launch compiled command script: " + advertisement.getScript());
								compile.eval(bindings);
							} else {
								LOGGER.info("Launch compiled command script: " + advertisement.getScript());
								compiledCommandScriptMap.get(advertisement.getScript()).eval(bindings);
							}
						} else if (envelope.getObject() instanceof EventChangesAdvertisement) {
							reloadService();
						} else {
							for (Event event : events) {
								if (envelope.getSubject().equals(event.getSubject()) || wildCardMatch(event.getSubject(), envelope.getSubject())) {

									LOGGER.debug("Run compiled script: " + event.getScript());

									try {
										bindings.put("advertisement", envelope.getObject());
										CompiledScript script = compiledScriptMap.get(event.getScript());

										if (script != null)
											script.eval(bindings);
										else
											LOGGER.error("Error! Script " + event.getScript() + " is NULL!");

									} catch (ScriptException e) {
										LOGGER.error("Error in script scripts/command/" + event.getScript() + ".js: " + e.toString());
										e.printStackTrace();
									}
								}
							}
						}
					} catch (ScriptException | IOException e) {
						LOGGER.error("Error in script: " + e.toString());
						e.printStackTrace();
					}
				}
			});

			jsonMessaging.start();
		} catch (final RuntimeException t) {
			LOGGER.error("Error in Events!");
			status.crashed();
			t.printStackTrace();
		}
	}

	private void reloadService() throws RuntimeException {
		LOGGER.info("Reload event service");

		// unsubscribe current events
		for (Event event : events) {
			jsonMessaging.unsubscribe(event.getSubject());
			LOGGER.debug("Unsubscribe from subject: " + event.getSubject());
		}

		events = Ebean.find(Event.class).findList();

		// subscribe to events from db
		for (Event event : events) {
			jsonMessaging.subscribe(event.getSubject());
			LOGGER.debug("Subscribe from subject: " + event.getSubject());
		}

		// take pause to save/remove new entity
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// load all scripts, compile and put into map
		compiledScriptMap = loadAndCompile(events);

		LOGGER.info("Reload event service done");
	}

	private Map<String, CompiledScript> loadAndCompile(List<Event> events) throws RuntimeException {
		Map<String, CompiledScript> compiledScriptMap = new HashMap<>();

		for (Event event : events) {
			File jsFile = new File("./scripts/" + event.getScript());
			CompiledScript compile = null;

			try {
				compile = engine.compile(FileUtils.readFileToString(jsFile));
			} catch (ScriptException | IOException e) {
				LOGGER.error("Compile error: " + e.getMessage());
			}

			compiledScriptMap.put(event.getScript(), compile);
		}

		return compiledScriptMap;
	}

	private boolean wildCardMatch(String pattern, String text) throws RuntimeException
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
