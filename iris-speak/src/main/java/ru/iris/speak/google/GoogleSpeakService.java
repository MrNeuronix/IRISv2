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
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.Config;
import ru.iris.common.database.model.Speaks;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.JsonNotification;
import ru.iris.common.messaging.model.speak.ChangeVolumeAdvertisement;
import ru.iris.common.messaging.model.speak.SpeakAdvertisement;
import ru.iris.common.modulestatus.Status;

import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class GoogleSpeakService
{
	private final Logger LOGGER = LogManager.getLogger(GoogleSpeakService.class.getName());
	private final ArrayBlockingQueue<Speaks> speakqueue = new ArrayBlockingQueue<>(50);

	public GoogleSpeakService()
	{
		Status status = new Status("Speak");

		if (status.checkExist()) {
			status.running();
		} else {
			status.addIntoDB("Speak", "Service that synthesize text to speak");
		}

		try {

			LOGGER.info("Speak service started (TTS: Google)");

			final Config conf = Config.getInstance();
			final Synthesiser synthesiser = new Synthesiser(conf.get("language"));

			JsonMessaging jsonMessaging = new JsonMessaging(UUID.randomUUID(), "speak");
			jsonMessaging.subscribe("event.speak");
			jsonMessaging.subscribe("event.speak.volume.set");

			// fetch all cached speaks for this server
			final List<Speaks> speaksList = Ebean.find(Speaks.class)
					.where()
					.and(Expr.ne("cache", 0),
							Expr.or(
									Expr.eq("device", "all"),
									Expr.eq("device", "server")
							)
					).findList();

			jsonMessaging.setNotification(new JsonNotification() {
				@Override
				public void onNotification(JsonEnvelope envelope) throws IOException, JavaLayerException {

					if (envelope.getObject() instanceof SpeakAdvertisement) {
						SpeakAdvertisement advertisement = envelope.getObject();

						Speaks speak = new Speaks();
						speak.setText(advertisement.getText());
						speak.setConfidence(advertisement.getConfidence());
						speak.setDevice(advertisement.getDevice());

						// push to speak queue
						speakqueue.add(speak);

					} else if (envelope.getObject() instanceof ChangeVolumeAdvertisement) {
						ChangeVolumeAdvertisement advertisement = envelope.getObject();

						if (advertisement.getDevice().equals("server") || advertisement.getDevice().equals("all")) {

							LOGGER.info("Setting sound volume level to " + advertisement.getLevel());

							if (advertisement.getLevel() == 0) {
								conf.set("silence", "1");
							} else {
								conf.set("silence", "0");
							}
						}

					} else {
						// We received unknown request message. Lets make generic log entry.
						LOGGER.info("Received request "
								+ " from " + envelope.getSenderInstance()
								+ " to " + envelope.getReceiverInstance()
								+ " at '" + envelope.getSubject()
								+ ": " + envelope.getObject());
					}
				}
			});

			jsonMessaging.start();

			// check speak queue and play if needed
			new Thread(new Runnable() {
				@Override
				public void run() {

					InputStream result;
					Player player;
					Speaks speak;

					try {
						while (true) {
							speak = speakqueue.poll(1000, TimeUnit.MILLISECONDS);

							if (speak == null)
								continue;

							if (conf.get("silence").equals("0")) {
								// Here we speak only if destination - all
								if (speak.getDevice().equals("all") || speak.getDevice().equals("server")) {
									LOGGER.debug("Confidence: " + speak.getConfidence());
									LOGGER.debug("Text: " + speak.getText());
									LOGGER.debug("Device: " + speak.getDevice());

									long cacheId = 0;

									for (Speaks speaks : speaksList) {
										if (
												speaks.getText().equals(speak.getText())
														&& speaks.getConfidence().equals(100D)
												)
											cacheId = speaks.getCache();
									}

									// Cache result
									if (cacheId == 0) {
										long cacheIdent = new Date().getTime();

										OutputStream outputStream = new FileOutputStream(new File("data/cache-" + cacheIdent + ".mp3"));
										result = synthesiser.getMP3Data(speak.getText());

										byte[] byteArray = IOUtils.toByteArray(result);
										InputStream resultForPlay = new ByteArrayInputStream(byteArray);
										InputStream resultForWrite = new ByteArrayInputStream(byteArray);

										player = new Player(resultForPlay);
										player.play();
										player.close();

										int read;
										byte[] bytes = new byte[1024];

										while ((read = resultForWrite.read(bytes)) != -1) {
											outputStream.write(bytes, 0, read);
										}

										speak.setCache(cacheIdent);
										speak.save();

										resultForPlay.close();
										resultForWrite.close();
										result.close();

										speaksList.add(speak);
									}
									// cache found - play local file
									else {
										LOGGER.info("Playing local file: " + "data/cache-" + cacheId + ".mp3");

										result = new FileInputStream("data/cache-" + cacheId + ".mp3");
										player = new Player(result);
										player.play();
										player.close();
										speak.save();
									}

									// sleep a little
									Thread.sleep(1000);

								} else {
									LOGGER.info("Ignored. Request to play on device: " + speak.getDevice());
								}
							} else {
								LOGGER.info("Silence mode enabled. Ignoring speak request.");
							}
						}
					} catch (final Throwable t) {
						LOGGER.error("Error in Speak: " + t);
						status.crashed();
						t.printStackTrace();
					}
				}
			}).start();
		} catch (final Throwable t) {
			LOGGER.error("Error in Speak: " + t);
			status.crashed();
			t.printStackTrace();
		}
	}
}
