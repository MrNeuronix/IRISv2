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
import com.avaje.ebean.Expr;
import com.darkprograms.speech.synthesiser.Synthesiser;
import javazoom.jl.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.Config;
import ru.iris.common.database.model.Speaks;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.speak.SpeakAdvertisement;

import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class GoogleSpeakService implements Runnable
{

	private final Logger LOGGER = LogManager.getLogger(GoogleSpeakService.class.getName());
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
		LOGGER.info("Speak service started (TTS: Google)");

		Config conf = Config.getInstance();
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

			// fetch all cached speaks for this server
			List<Speaks> speaksList = Ebean.find(Speaks.class).where().and(Expr.ne("cache", 0), Expr.eq("device", "all")).findList();

			InputStream result;
			Player player;

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

						if (conf.get("silence").equals("0"))
						{
							// Here we speak only if destination - all
							if (advertisement.getDevice().equals("all"))
							{
								LOGGER.debug("Confidence: " + advertisement.getConfidence());
								LOGGER.debug("Text: " + advertisement.getText());
								LOGGER.debug("Device: " + advertisement.getDevice());

								long cacheId = 0;

								for (Speaks speaks : speaksList)
								{
									if (
											speaks.getText().equals(advertisement.getText())
													&& speaks.getConfidence().equals(100D)
											)
										cacheId = speaks.getCache();
								}

								// Cache result
								if (cacheId == 0)
								{
									long cacheIdent = new Date().getTime();

									OutputStream outputStream = new FileOutputStream(new File("data/cache-" + cacheIdent + ".mp3"));
									result = synthesiser.getMP3Data(advertisement.getText());

									int read;
									byte[] bytes = new byte[1024];

									while ((read = result.read(bytes)) != -1)
									{
										outputStream.write(bytes, 0, read);
									}

									speak.setCache(cacheIdent);

									player = new Player(result);
									player.play();
									player.close();

									Ebean.save(speak);

									speaksList.add(speak);
								}
								// cache found - play local file
								else
								{
									LOGGER.info("Playing local file: " + "data/cache-" + cacheId + ".mp3");

									result = new FileInputStream("data/cache-" + cacheId + ".mp3");

									player = new Player(result);
									player.play();
									player.close();

									Ebean.save(speak);
								}

								// force to be null for GC
								player = null;
							}
						}
						else
						{
							LOGGER.info("Silence mode enabled. Ignoring speak request.");
						}

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

		}
		catch (final Throwable t)
		{
			LOGGER.error("Unexpected exception in Speak", t);
			t.printStackTrace();
		}
	}
}
