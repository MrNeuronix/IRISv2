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
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.Config;

import javax.jms.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Prototype for JSON message broadcasting.
 *
 * @author Nikolay A. Viguro, Tommi S.E. Laukkanen
 */

public class JsonMessaging
{

	private static final Logger LOGGER = LogManager.getLogger(JsonMessaging.class);
	/**
	 * The instance ID.
	 */
	private final UUID instanceId;
	/**
	 * The subjects that has been registered to receive JSON encoded messages.
	 */
	private final Set<String> jsonSubjects = Collections.synchronizedSet(new HashSet<String>());
	/**
	 * The receive queue for JSON objects.
	 */
	private final BlockingQueue<JsonEnvelope> jsonReceiveQueue = new ArrayBlockingQueue<>(100);
	private final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
	private UUID sender;
	/**
	 * The AMQ connection.
	 */
	private Connection connection;
	/**
	 * The message session.
	 */
	private Session session;
	/**
	 * The reply queue.
	 */
	private Queue replyQueue;
	/**
	 * The message producer.
	 */
	private MessageProducer messageProducer;
	/**
	 * The message consumer.
	 */
	private MessageConsumer messageConsumer;
	/**
	 * Boolean flag reflecting whether threads should be close.
	 */
	private boolean shutdownThreads = false;
	/**
	 * The JSON broadcast listen thread.
	 */
	private Thread jsonBroadcastListenThread;

	public JsonMessaging(final UUID instanceId)
	{

		// Create a ConnectionFactory
		ActiveMQConnectionFactory connectionFactory;

		this.instanceId = instanceId;

		// Create a Connection
		try
		{
			// Create a ConnectionFactory
			Config config = new Config();

			if (config.getConfig().get("AMQPuseVMconnection").equals("1"))
			{
				connectionFactory = new ActiveMQConnectionFactory("vm://iris?jms.prefetchPolicy.all=100");
			}
			else
			{
				connectionFactory = new ActiveMQConnectionFactory("tcp://" + config.getConfig().get("AMQPhost")
						+ ":" + config.getConfig().get("AMQPport") + "?jms.prefetchPolicy.all=100");
			}

			// set max 10 threads
			connectionFactory.setMaxThreadPoolSize(10);
			connectionFactory.setAlwaysSessionAsync(false);

			connection = connectionFactory.createTopicConnection();
			connection.start();

			// Create a Session
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

			// Create the destination (Topic)
			/*
	  The message destination.
     */
			Destination destination = session.createTopic("iris");
			replyQueue = session.createTemporaryQueue();
			messageConsumer = session.createConsumer(destination);
			messageProducer = session.createProducer(destination);
			//messageProducer.setDeliveryMode(DeliveryMode.PERSISTENT);

		}
		catch (JMSException e)
		{
			LOGGER.error(e.toString());
		}
	}

	/**
	 * Starts the messaging to listen for JSON objects.
	 */
	public void start()
	{
		// Startup listen thread.
		jsonBroadcastListenThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				listenBroadcasts();
			}
		}, "json-broascast-listen");
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
			messageConsumer.close();
			messageProducer.close();
			session.close();
			connection.close();
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
	 * Gets the JSON message received to subject or null if nothing has been received
	 *
	 * @return the JSON message or null
	 */
	public JsonEnvelope getJsonObject()
	{
		synchronized (jsonReceiveQueue)
		{
			if (jsonReceiveQueue.size() > 0)
			{
				return jsonReceiveQueue.poll();
			}
		}
		return null;
	}

	/**
	 * Checks whether JSON message has been received.
	 *
	 * @return true if JSON message is available
	 */
	public int hasJsonObject()
	{
		synchronized (jsonReceiveQueue)
		{
			return jsonReceiveQueue.size();
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

		String className = object.getClass().getName();
		String jsonString = gson.toJson(object);

		try
		{
			// Create a messages
			MapMessage message = session.createMapMessage();
			message.setJMSCorrelationID("ID:" + UUID.randomUUID().toString());
			message.setJMSReplyTo(replyQueue);
			message.setStringProperty("sender", instanceId.toString());
			message.setStringProperty("class", className);
			message.setStringProperty("subject", subject);
			message.setStringProperty("json", jsonString);
			messageProducer.send(message);

			message = null;

		}
		catch (JMSException e)
		{
			LOGGER.debug("Error sending JSON message: " + object + " to subject: " + subject, e);
		}
	}

	/**
	 * Blocking receive to listen for JOSN messages arriving to given topic.
	 *
	 * @return the JSON message
	 */
	public JsonEnvelope receive() throws InterruptedException
	{
		return jsonReceiveQueue.take();
	}

	/**
	 * Blocking receive to listen for JOSN messages arriving to given topic.
	 *
	 * @return the JSON message
	 */
	public JsonEnvelope receive(final int timeoutMillis) throws InterruptedException
	{
		return jsonReceiveQueue.poll(timeoutMillis, TimeUnit.MILLISECONDS);
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

	private void listenBroadcasts()
	{

		while (!shutdownThreads)
		{

			try
			{
				MapMessage message = (MapMessage) messageConsumer.receive();
				if (message == null)
				{
					continue;
				}
				String subject = message.getStringProperty("subject");
				String jsonString = message.getStringProperty("json");
				String className = message.getStringProperty("class");

				if (wildCardMatch(jsonSubjects, subject)
						&& !StringUtils.isEmpty(className)
						&& !StringUtils.isEmpty(jsonString)
						|| jsonSubjects.contains(subject)
						&& !StringUtils.isEmpty(className)
						&& !StringUtils.isEmpty(jsonString))
				{

					Class<?> clazz = Class.forName(className);
					Object object = gson.fromJson(jsonString, clazz);

					JsonEnvelope envelope = new JsonEnvelope(
							UUID.fromString(message.getStringProperty("sender")),
							message.getStringProperty("receiver") != null ?
									UUID.fromString(message.getStringProperty("receiver")) : null,
							message.getJMSCorrelationID(), message.getJMSReplyTo(), subject, object);

					LOGGER.debug("Received message with ID: " + message.getJMSMessageID()
							+ " with correlation ID: " + message.getJMSCorrelationID()
							+ " sender: " + envelope.getSenderInstance()
							+ " receiver: " + envelope.getReceiverInstance()
							+ " to subject: "
							+ envelope.getSubject() + " (" + envelope.getClass().getSimpleName() + ")");

					jsonReceiveQueue.put(envelope);

					// debug
					if (jsonReceiveQueue.size() > 10)
					{
						LOGGER.info("Queue is too big! " + jsonReceiveQueue.size());
					}
				}
			}
			catch (final JMSException e)
			{
				LOGGER.debug("Error receiving JSON message.", e);
			}
			catch (final ClassNotFoundException e)
			{
				LOGGER.debug("Error deserializing JSON message.", e);
			}
			catch (InterruptedException e)
			{
				LOGGER.debug("Error JSON message.", e);
			}
		}
	}

	/**
	 * Performs a wildcard matching for the text and pattern
	 * provided.
	 *
	 * @param text     the text to be tested for matches.
	 * @param patterns the patterns set to be matched for.
	 *                 This can contain the wildcard character '*' (asterisk).
	 * @return <tt>true</tt> if a match is found, <tt>false</tt>
	 * otherwise.
	 */

	private boolean wildCardMatch(Set<String> patterns, String text)
	{

		// add sentinel so don't need to worry about *'s at end of pattern
		for (String pattern : patterns)
		{
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
			if (states[N])
			{
				return true;
			}
		}
		return false;
	}
}
