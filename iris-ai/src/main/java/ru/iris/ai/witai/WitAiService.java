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
import ru.iris.common.messaging.model.ai.AIResponseAdvertisement;
import ru.iris.common.messaging.model.ai.WitAiResponse;
import ru.iris.common.messaging.model.speak.SpeakRecognizedAdvertisement;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.UUID;

public class WitAiService implements Runnable
{

	private final Logger LOGGER = LogManager.getLogger(WitAiService.class);
	private final AIResponseAdvertisement aiResponseAdvertisement = new AIResponseAdvertisement();
	private Thread t = null;
	private boolean shutdown = false;

	public WitAiService()
	{
		this.t = new Thread(this);
		t.setName("WitAI Service");
		this.t.start();
	}

	public Thread getThread()
	{
		return this.t;
	}

	public synchronized void run()
	{

		Config cfg = Config.getInstance();
		Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

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
			jsonMessaging.subscribe("event.speak.recognized");
			jsonMessaging.start();

			while (!shutdown)
			{

				// Lets wait for 100 ms on json messages and if nothing comes then proceed to carry out other tasks.
				final JsonEnvelope envelope = jsonMessaging.receive(100);
				if (envelope != null)
				{
					if (envelope.getObject() instanceof SpeakRecognizedAdvertisement)
					{

						SpeakRecognizedAdvertisement advertisement = envelope.getObject();

						/////////////////////////////////////////////////////////////////

						String url = "https://api.wit.ai/message?q=" + URLEncoder.encode(advertisement.getText(), "UTF-8");

						LOGGER.debug("URL is: " + url);

						CloseableHttpClient httpclient = HttpClients.createDefault();
						HttpGet httpget = new HttpGet(url);

						// auth on wit.ai
						httpget.addHeader("Authorization", "Bearer " + cfg.get("witaiKey"));

						try (CloseableHttpResponse response = httpclient.execute(httpget))
						{
							HttpEntity entity = response.getEntity();
							if (entity != null)
							{

								try (InputStream instream = entity.getContent())
								{
									String content = IOUtils.toString(instream, "UTF-8");

									LOGGER.debug("AI response: " + content);

									WitAiResponse json = gson.fromJson(content, WitAiResponse.class);

									Double confidence = json.getOutcome().getConfidence();

									LOGGER.debug("Confidence: " + confidence);

									if (confidence > 0.65)
									{
										String object = json.getOutcome().getEntities().get("object").getValue();

										if (object != null)
										{
											LOGGER.info("Get response from AI: " + json.getMsg_body() + " to object: " + object);
											jsonMessaging.broadcast("event.ai.response.object." + object, aiResponseAdvertisement.set(json));
										}
									}
								}
							}
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
			}

			// Close JSON messaging.
			jsonMessaging.close();

		}
		catch (final Throwable t)
		{

			LOGGER.error("Unexpected exception in AI", t);
			t.printStackTrace();
		}

	}
}
