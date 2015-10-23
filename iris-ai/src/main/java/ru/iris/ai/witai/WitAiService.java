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

package ru.iris.ai.witai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.Config;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.JsonNotification;
import ru.iris.common.messaging.model.ai.AIResponseAdvertisement;
import ru.iris.common.messaging.model.ai.WitAiResponse;
import ru.iris.common.messaging.model.speak.SpeakRecognizedAdvertisement;
import ru.iris.common.modulestatus.Status;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.UUID;

public class WitAiService
{
	private final Logger LOGGER = LogManager.getLogger(WitAiService.class);
    private JsonMessaging jsonMessaging;

	public WitAiService()
	{
		final Config cfg = Config.getInstance();
		final Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

		Status status = new Status("AI");

		if (status.checkExist()) {
			status.running();
		} else {
			status.addIntoDB("AI", "Wit.AI service recognition");
		}

		try
		{
            jsonMessaging = new JsonMessaging(UUID.randomUUID(), "witai");
            jsonMessaging.subscribe("event.speak.recognized");
			jsonMessaging.setNotification(new JsonNotification() {

				@Override
				public void onNotification(JsonEnvelope envelope) {

					if (envelope.getObject() instanceof SpeakRecognizedAdvertisement)
					{
						try {
							SpeakRecognizedAdvertisement advertisement = envelope.getObject();
							String url = "https://api.wit.ai/message?q=" + URLEncoder.encode(advertisement.getText(), "UTF-8");

							LOGGER.debug("URL is: " + url);

							CloseableHttpClient httpclient = HttpClients.createDefault();
							HttpGet httpget = new HttpGet(url);

							// auth on wit.ai
							httpget.addHeader("Authorization", "Bearer " + cfg.get("witaiKey"));

							CloseableHttpResponse response = httpclient.execute(httpget);
							HttpEntity entity = response.getEntity();
							if (entity != null)
								{
									InputStream instream = entity.getContent();
									String content = IOUtils.toString(instream, "UTF-8");

									LOGGER.debug("AI response: " + content);

									WitAiResponse json = gson.fromJson(content, WitAiResponse.class);
									Double confidence = json.getOutcome().getConfidence();

									LOGGER.debug("Confidence: " + confidence);

									if (confidence > 0.65)
										{
											String object = json.getOutcome().getEntities().get("object").getValue();

											if (object != null) {
												LOGGER.info("Get response from AI: " + json.getMsg_body() + " to object: " + object);
												jsonMessaging.broadcast("event.ai.response.object." + object, new AIResponseAdvertisement(json));
											}
										}
								}
						} catch (IOException e) {
							LOGGER.error("Error: ", e.getMessage());
							e.printStackTrace();
						}

						/////////////////////////////////////////////////////////////////

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
			});

			jsonMessaging.start();
		}
		catch (final Throwable t)
		{
			LOGGER.error("Unexpected exception in AI", t);
			status.crashed();
			t.printStackTrace();
		}
	}

    public void stop() {
        jsonMessaging.close();
    }
}
