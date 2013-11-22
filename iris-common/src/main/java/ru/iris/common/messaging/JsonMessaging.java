/**
 * Copyright 2013 Tommi S.E. Laukkanen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.iris.common.messaging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.qpid.AMQException;
import org.apache.qpid.client.AMQAnyDestination;
import org.apache.qpid.client.AMQConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.net.URISyntaxException;
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
public class JsonMessaging {
    /**
     * The logger.
     */
    private static Logger LOGGER = LoggerFactory.getLogger(JsonMessaging.class);
    /**
     * The AMQ connection.
     */
    private Connection connection;
    /**
     * The message session.
     */
    private Session session;
    /**
     * The message destination.
     */
    private Destination destination;
    /**
     * The message producer.
     */
    private MessageProducer messageProducer;
    /**
     * The message consumer.
     */
    private MessageConsumer messageConsumer;

    /**
     * The instance ID.
     */
    private UUID instanceId;
    /**
     * Boolean flag reflecting whether threads should be close.
     */
    private boolean shutdownThreads = false;
    /**
     * The subjects that has been registered to receive JSON encoded messages.
     */
    private Set<String> jsonSubjects = Collections.synchronizedSet(new HashSet<String>());
    /**
     * The receive queue for JSON objects.
     */
    private BlockingQueue<JsonEnvelope> jsonReceiveQueue = new ArrayBlockingQueue<JsonEnvelope>(100);
    /**
     * The JSON broadcast listen thread.
     */
    private Thread jsonBroadcastListenThread;

    /**
     * Public constructor which setups connectivity and session to AMQP message broker.
     *
     * @throws JMSException
     * @throws URISyntaxException
     * @throws AMQException
     */
    public JsonMessaging(final UUID instanceId)
            throws JMSException, URISyntaxException, AMQException {
        this.instanceId = instanceId;
        connection = new AMQConnection("amqp://admin:admin@localhost/?brokerlist='tcp://localhost:5672'");
        connection.start();

        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        destination = new AMQAnyDestination("ADDR:iris; {create: always, node: {type: topic}}");
        messageConsumer = session.createConsumer(destination);
        messageProducer = session.createProducer(destination);
    }


    /**
     * Starts the messaging to listen for JSON objects.
     */
    public void start() {
        // Startup listen thread.
        jsonBroadcastListenThread = new Thread(new Runnable() {
            @Override
            public void run() {
                listenBroadcasts();
            }
        }, "json-broascast-listen");
        jsonBroadcastListenThread.start();

        // Add close hook to close the listen thread when JVM exits.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                shutdownThreads = true;
                jsonBroadcastListenThread.interrupt();
                try {
                    jsonBroadcastListenThread.join();
                } catch (final InterruptedException e) {
                    LOGGER.debug(e.toString());
                }
            }
        });
    }

    /**
     * Closes connection to AMQP message broker.
     *
     * @throws JMSException
     */
    public void close() {
        try {
            messageConsumer.close();
            messageProducer.close();
            session.close();
            connection.close();
            shutdownThreads = true;
            if (jsonBroadcastListenThread != null) {
                jsonBroadcastListenThread.interrupt();
                jsonBroadcastListenThread.join();
            }
        } catch (final Exception e) {
            LOGGER.error("Error shutting down JsonMessaging.", e);
        }
    }

    /**
     * Sends object as JSON encoded message with given subject.
     *
     * @param subject the subject
     * @param object  the object
     * @param <T>     the message class
     */
    public <T> void broadcast(final String subject, final Object object) {
        try {
            final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
            final String className = object.getClass().getName();
            final String jsonString = gson.toJson(object);

            final MapMessage message = session.createMapMessage();
            message.setJMSCorrelationID("ID:" + UUID.randomUUID().toString());
            message.setStringProperty("sender", instanceId.toString());
            message.setStringProperty("class", className);
            message.setStringProperty("json", jsonString);
            message.setStringProperty("qpid.subject", subject);

            messageProducer.send(message);
        } catch (JMSException e) {
            throw new RuntimeException("Error sending JSON message: " + object + " to subject: " + object, e);
        }
    }

    /**
     * Blocking receive to listen for JOSN messages arriving to given topic.
     *
     * @return the JSON message
     */
    public JsonEnvelope receive() throws InterruptedException {
        return jsonReceiveQueue.take();
    }

    /**
     * Blocking receive to listen for JOSN messages arriving to given topic.
     *
     * @return the JSON message
     */
    public JsonEnvelope receive(final int timeoutMillis) throws InterruptedException {
        return jsonReceiveQueue.poll(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Gets the JSON message received to subject or null if nothing has been received
     *
     * @return the JSON message or null
     */
    public JsonEnvelope getJsonObject() {
        synchronized (jsonReceiveQueue) {
            if (jsonReceiveQueue.size() > 0) {
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
    public int hasJsonObject() {
        synchronized (jsonReceiveQueue) {
            return jsonReceiveQueue.size();
        }
    }

    /**
     * Method for receiving subscribing to listen JSON objects on a subject.
     *
     * @param subject the subject
     */
    public void subscribe(final String subject) {
        jsonSubjects.add(subject);
    }

    private void listenBroadcasts() {
        final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        try {
            while (!shutdownThreads) {
                try {
                    final MapMessage message = (MapMessage) messageConsumer.receive(100);
                    if (message == null) {
                        continue;
                    }
                    final String subject = message.getStringProperty("qpid.subject");
                    final String jsonString = message.getStringProperty("json");
                    final String className = message.getStringProperty("class");
                    if (jsonSubjects.contains(subject)
                            && !StringUtils.isEmpty(className)
                            && !StringUtils.isEmpty(jsonString)) {
                        final Class clazz = Class.forName(className);
                        final Object object = gson.fromJson(jsonString, clazz);
                        final JsonEnvelope envelope = new JsonEnvelope(
                                UUID.fromString(message.getStringProperty("sender")),
                                message.getStringProperty("receiver") != null ?
                                        UUID.fromString(message.getStringProperty("receiver")) : null,
                                message.getJMSCorrelationID(), message.getJMSReplyTo(), subject, object);

                        LOGGER.debug("Received message with ID: " + message.getJMSMessageID()
                                + " with correlation ID: " + message.getJMSCorrelationID()
                                + " sender: " + envelope.getSenderInstanceId()
                                + " receiver: " + envelope.getReceiverInstanceId()
                                + " to subject: "
                                + envelope.getSubject() + " (" + envelope.getClass().getSimpleName() + ")");
                        jsonReceiveQueue.put(envelope);
                    }
                } catch (final JMSException e) {
                    LOGGER.error("Error receiving JSON message.", e);
                } catch (final ClassNotFoundException e) {
                    LOGGER.error("Error deserializing JSON message.", e);
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.toString());
        }
    }
}
