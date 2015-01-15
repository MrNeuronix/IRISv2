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

package ru.iris.common.messaging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rabbitmq.client.*;
import javazoom.jl.decoder.JavaLayerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.SchedulerException;

import java.io.IOException;
import java.util.*;

/**
 * JSON messaging
 *
 * @author Nikolay A. Viguro, Tommi S.E. Laukkanen
 */

public class JsonMessaging
{

	private static final Logger LOGGER = LogManager.getLogger(JsonMessaging.class);
	/**
	 * The instance ID.
	 */
	private UUID instanceId = null;
	/**
	 * The subjects that has been registered to receive JSON encoded messages.
	 */
	private final Set<String> jsonSubjects = Collections.synchronizedSet(new HashSet<>());

	private JsonNotification notification = null;
	private final Gson gson = new GsonBuilder().create();
	/**
	 * Boolean flag reflecting whether threads should be close.
	 */
	private boolean shutdownThreads = false;
	/**
	 * The JSON broadcast listen thread.
	 */
	private Thread jsonBroadcastListenThread;

	private Channel channel = JsonConnection.getInstance().getChannel();

	public JsonMessaging(final UUID instanceId)
	{
		this.instanceId = instanceId;
	}

	public JsonMessaging(final UUID instanceId, String queueName)
	{
		this.instanceId = instanceId;
	}

	public JsonNotification getNotification() {
		return notification;
	}

	public void setNotification(JsonNotification notification) {
		this.notification = notification;
	}

	/**
	 * Starts the messaging to listen for JSON objects.
	 */
	public void start()
	{
		// Startup listen thread.
		jsonBroadcastListenThread = new Thread(() -> listenBroadcasts(), "json-broadcast-listen");
		jsonBroadcastListenThread.setName("JSON Messaging Listen Thread");
		jsonBroadcastListenThread.start();

		// Add close hook to close the listen thread when JVM exits.
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			public void run()
			{
				shutdownThreads = true;
				jsonBroadcastListenThread.interrupt();
				try
				{
					jsonBroadcastListenThread.join();
				}
				catch (final InterruptedException e)
				{
					LOGGER.debug(e.toString());
				}
			}
		});
	}

	/**
	 * Closes connection to message broker.
	 */
	public void close()
	{
		try
		{
			shutdownThreads = true;

			if (jsonBroadcastListenThread != null)
			{
				jsonBroadcastListenThread.interrupt();
				jsonBroadcastListenThread.join();
			}
		}
		catch (final Exception e)
		{
			LOGGER.error("Error shutting down JsonMessaging.", e);
		}
	}

	/**
	 * Sends object as JSON encoded message with given subject.
	 *
	 * @param subject the subject
	 * @param object  the object
	 */
	public void broadcast(String subject, Object object)
	{
		LOGGER.debug("Broadcast to " + subject);

		String className = object.getClass().getName();
		String jsonString = gson.toJson(object);

		try
		{
			// Create a message headers
			Map<String, Object> headers = new HashMap<>();
			headers.put("sender", instanceId.toString());
			headers.put("class", className);

			// Publish message to topic
			channel.basicPublish(
					"iris",
					subject,
					new AMQP.BasicProperties.Builder()
							.correlationId(instanceId.toString())
							.headers(headers)
							.build(),
					jsonString.getBytes()
			);
		}
		catch (IOException e)
		{
			LOGGER.error("Error sending JSON message: " + object + " to subject: " + subject, e);
		}
	}

	public void response(JsonEnvelope envelope, Object object) {
		LOGGER.debug("Response to " + envelope.getReceiverInstance() + ", corrId is " + envelope.getCorrelationId());

		String className = object.getClass().getName();
		String jsonString = gson.toJson(object);

		try {
			// Create a message headers
			Map<String, Object> headers = new HashMap<>();
			headers.put("sender", instanceId.toString());
			headers.put("class", className);

			// Publish message to topic
			channel.basicPublish(
					"iris",
					envelope.getReceiverInstance(),
					new AMQP.BasicProperties.Builder()
							.correlationId(envelope.getCorrelationId())
							.headers(headers)
							.build(),
					jsonString.getBytes()
			);
		} catch (IOException e) {
			LOGGER.error("Error sending reply JSON message: " + object + " to queue: " + envelope.getReceiverInstance(), e);
		}
	}

	/**
	 * Sends object as JSON encoded message with given subject.
	 *
	 * @param subject the subject
	 * @param object  the object
	 */
	public JsonEnvelope request(String subject, Object object) {
		String className = object.getClass().getName();
		String jsonString = gson.toJson(object);
		JsonEnvelope envelope = null;

		try {
			String replyQueue = channel.queueDeclare().getQueue();
			channel.queueBind(replyQueue, "iris", replyQueue);

			QueueingConsumer consumer = new QueueingConsumer(channel);
			channel.basicConsume(replyQueue, true, consumer);
			String corrId = java.util.UUID.randomUUID().toString();

			// Create a message headers
			Map<String, Object> sendheaders = new HashMap<>();
			sendheaders.put("sender", instanceId.toString());
			sendheaders.put("class", className);

			LOGGER.debug("Request to " + subject + ", corrId is " + corrId);

			// Publish message to topic
			channel.basicPublish(
					"iris",
					subject,
					new AMQP.BasicProperties.Builder()
							.replyTo(replyQueue)
							.correlationId(corrId)
							.headers(sendheaders)
							.build(),
					jsonString.getBytes()
			);

			LOGGER.debug("Waiting for answer");

			while (true) {
				QueueingConsumer.Delivery delivery = consumer.nextDelivery(5000);

				if (delivery.getProperties().getCorrelationId().equals(corrId)) {
					LOGGER.debug("Got answer for " + corrId);

					// Get headers
					Map<String, Object> headers = delivery.getProperties().getHeaders();

					String message = new String(delivery.getBody());
					String subj = delivery.getEnvelope().getRoutingKey();
					String corrID = delivery.getProperties().getCorrelationId();
					String replyTo = delivery.getProperties().getReplyTo();
					String classNam = new String(((LongString) headers.get("class")).getBytes());
					String senderName = new String(((LongString) headers.get("sender")).getBytes());

					Class<?> clazz = Class.forName(classNam);
					Object obj = gson.fromJson(message, clazz);

					envelope = new JsonEnvelope(
							UUID.fromString(senderName),
							replyTo,
							corrID,
							subj,
							obj);
					break;
				}

				LOGGER.debug("Skip envelope with corrId: " + delivery.getProperties().getCorrelationId());
			}
		} catch (IOException | InterruptedException | ClassNotFoundException e) {
			LOGGER.error("Error sending JSON message: " + object + " to subject: " + subject, e);
		}

		return envelope;
	}

	/**
	 * Method for receiving subscribing to listen JSON objects on a subject.
	 *
	 * @param subject the subject
	 */
	public void subscribe(String subject)
	{
		jsonSubjects.add(subject);
	}

	public void unsubscribe(String subject) {
		jsonSubjects.remove(subject);
	}

	private void listenBroadcasts()
	{
		try
		{
            String queueName = channel.queueDeclare().getQueue();

			for (String subject : jsonSubjects)
			{
				channel.queueBind(queueName, "iris", subject);
			}

			QueueingConsumer consumer = new QueueingConsumer(channel);
			channel.basicConsume(queueName, true, consumer);

			while (!shutdownThreads)
			{
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();

				// Get headers
				Map<String, Object> headers = delivery.getProperties().getHeaders();

				String message = new String(delivery.getBody());
				String subject = delivery.getEnvelope().getRoutingKey();
				String corrID = delivery.getProperties().getCorrelationId();
				String replyTo = delivery.getProperties().getReplyTo();
				String className = new String(((LongString) headers.get("class")).getBytes());
				String senderName = new String(((LongString) headers.get("sender")).getBytes());

				Class<?> clazz = Class.forName(className);
				Object object = gson.fromJson(message, clazz);

				JsonEnvelope envelope = new JsonEnvelope(
						UUID.fromString(senderName),
						replyTo,
						corrID,
						subject,
						object);

				LOGGER.debug("Received message with ID: " + delivery.getProperties().getMessageId()
						+ " with correlation ID: " + corrID
						+ " sender: " + envelope.getSenderInstance()
						+ " to subject: "
						+ envelope.getSubject() + " (" + envelope.getClass().getSimpleName() + ")");

				notification.onNotification(envelope);

				headers.clear();
			}
		}
		catch (final ClassNotFoundException e)
		{
			LOGGER.error("Error deserializing JSON message.", e);
		}
		catch (ConsumerCancelledException e)
		{
			LOGGER.error("Consumer cancelled.", e);
		} catch (IOException | InterruptedException e)
		{
			LOGGER.debug("Error JSON message.", e);
		} catch (SchedulerException | JavaLayerException e) {
			e.printStackTrace();
		}
	}
}
