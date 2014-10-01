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

package ru.iris.speak.google;

import com.avaje.ebean.Ebean;
import com.darkprograms.speech.synthesiser.Synthesiser;
import javazoom.jl.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.Config;
import ru.iris.common.database.model.Speaks;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.speak.SpeakAdvertisement;

import java.util.Map;
import java.util.UUID;

public class GoogleSpeakService implements Runnable
{

	private final Logger log = LogManager.getLogger(GoogleSpeakService.class.getName());
	private final Config config = new Config();
	private Thread t = null;
	private boolean shutdown = false;

	public GoogleSpeakService()
	{
		t = new Thread(this);
		t.setName("Google Speak Service");
		t.start();
	}

	public Thread getThread()
	{
		return t;
	}

	@Override
	public synchronized void run()
	{
		log.info("Speak service started (TTS: Google)");

		Map<String, String> conf = config.getConfig();
		final Synthesiser synthesiser = new Synthesiser(conf.get("language"));

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
			jsonMessaging.subscribe("event.speak");
			jsonMessaging.start();

			while (!shutdown)
			{

				// Lets wait for 100 ms on json messages and if nothing comes then proceed to carry out other tasks.
				final JsonEnvelope envelope = jsonMessaging.receive(100);
				if (envelope != null)
				{
					if (envelope.getObject() instanceof SpeakAdvertisement)
					{

						SpeakAdvertisement advertisement = envelope.getObject();

						Speaks speak = new Speaks();
						speak.setText(advertisement.getText());
						speak.setConfidence(advertisement.getConfidence());
						speak.setDevice(advertisement.getDevice());
						speak.setActive(true);

						Ebean.save(speak);

						if (conf.get("silence").equals("0"))
						{

							log.info("Confidence: " + advertisement.getConfidence());
							log.info("Text: " + advertisement.getText());
							log.info("Device: " + advertisement.getDevice());

							if (advertisement.getDevice().equals("all"))
							{
								Player player = new Player(synthesiser.getMP3Data(advertisement.getText()));
								player.play();
								player.close();

								// force to be null for GC
								player = null;
							}
							// TODO play on other devices
						}
						else
						{
							log.info("Silence mode enabled. Ignoring speak request.");
						}

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

			// Close JSON messaging.
			jsonMessaging.close();

		}
		catch (final Throwable t)
		{
			log.error("Unexpected exception in Speak", t);
			t.printStackTrace();
		}
	}
}
