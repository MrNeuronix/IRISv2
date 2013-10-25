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
package ru.iris.common;

import com.google.gson.Gson;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Prototype for JSON message broadcasting.
 *
 * @author Nikolay A. Viguro, Tommi S.E. Laukkanen
 */
public class JsonMessaging {
    /** The logger. */
    private static Logger LOGGER = LoggerFactory.getLogger (JsonMessaging.class);
    /** The AMQ connection. */
    private Connection connection;
    /** The message session. */
    private Session session;
    /** The message destination. */
    private Destination destination;
    /** The message consumer. */
    private MessageConsumer messageConsumer;
    /** The message producer. */
    private MessageProducer messageProducer;


    /** Boolean flag reflecting whether threads should be shutdown. */
    private boolean shutdownThreads = false;
    /** The topics that has been registered to receive JSON encoded messages. */
    private Set<String> jsonTopics = Collections.synchronizedSet(new HashSet<String>());
    /** The receive queue for JSON objects. */
    private BlockingQueue<JsonMessage> jsonReceiveQueue = new ArrayBlockingQueue<JsonMessage>(100);
    /** The JSON listen thread. */
    private Thread jsonListenThread;

    /**
     * Public constructor which setups connectivity and session to AMQP message broker.
     * @throws JMSException
     * @throws URISyntaxException
     * @throws AMQException
     */
    public JsonMessaging() throws JMSException, URISyntaxException, AMQException {
        connection = new AMQConnection ("amqp://admin:admin@localhost/?brokerlist='tcp://localhost:5672'");
        connection.start ();

        session = connection.createSession (false, Session.AUTO_ACKNOWLEDGE);
        destination = new AMQAnyDestination ("ADDR:iris; {create: always, node: {type: topic}}");
        messageConsumer = session.createConsumer (destination);
        messageProducer = session.createProducer (destination);
    }

    /**
     * Closes connection to AMQP message broker.
     *
     * @throws JMSException
     */
    public void close() {
        try {
            messageConsumer.close ();
            messageProducer.close ();
            session.close ();
            connection.close ();
            shutdownThreads = true;
            if (jsonListenThread != null) {
                jsonListenThread.interrupt();
                jsonListenThread.join();
            }
        } catch (final Exception e) {
            LOGGER.error("Error shutting down JsonMessaging.", e);
        }
    }

    /**
     * Inner value object class to encapsulate topic name and Java object together before sending
     * or after receiving.
     */
    public static class JsonMessage {
        /**
         * The topic.
         */
        private String topic;
        /**
         * The object.
         */
        private Object object;

        /**
         * Constructor for setting the values.
         * @param topic the topic
         * @param object the object
         */
        public JsonMessage(String topic, Object object) {
            this.topic = topic;
            this.object = object;
        }

        /**
         * @return the topic
         */
        public String getTopic() {
            return topic;
        }

        /**
         * @param <T> the object class
         * @return the object
         */
        public <T> T getObject() {
            return (T) object;
        }

        @Override
        public String toString() {
            return "JsonMessage{" +
                    "topic='" + topic + '\'' +
                    ", object=" + object +
                    '}';
        }
    }

    /**
     * Sends message in JSON encoded format.
     * @param jsonMessage the JSON message
     */
    public <T> void sendJsonObject(final JsonMessage jsonMessage) {
        try {
            final Gson gson = new Gson();
            final String className = jsonMessage.getObject().getClass().getName();
            final String jsonString = gson.toJson(jsonMessage.getObject());
            final MapMessage message = session.createMapMessage();
            message.setStringProperty("class", className);
            message.setStringProperty("json", jsonString);
            message.setStringProperty ("qpid.subject", jsonMessage.getTopic());
            messageProducer.send (message);
        } catch (JMSException e) {
            LOGGER.error("Error sending JSON message: " + jsonMessage.toString(), e);
        }
    }

    /**
     * Blocking receive to listen for JOSN messages arriving to given topic.
     * @return the JSON message
     */
    public JsonMessage receiveJsonObject() throws InterruptedException {
        return (JsonMessage) jsonReceiveQueue.take();
    }

    /**
     * Gets the JSON message received to topic or null if nothing has been received
     * @return the JSON message or null
     */
    public JsonMessage getJsonObject() {
        synchronized (jsonReceiveQueue) {
            if (jsonReceiveQueue.size() > 0) {
                return jsonReceiveQueue.poll();
            }
        }
        return null;
    }

    /**
     * Checks whether JSON message has been received.
     * @return true if JSON message is available
     */
    public int hasJsonObject() {
        synchronized (jsonReceiveQueue) {
            return jsonReceiveQueue.size();
        }
    }

    /**
     * Method for receiving subscribing to listen JSON objects on a topic.
     * @param topic the topic
     */
    public void subscribeJsonTopic(final String topic) {
        jsonTopics.add(topic);
    }

    /**
     * Starts the messaging to listen for JSON objects.
     */
    public void listenJson() {
        // Startup listen thread.
        jsonListenThread = new Thread(new Runnable() {
            @Override
            public void run() {
                final Gson gson = new Gson();
                try {
                    while (!shutdownThreads) {
                        try {
                            final MapMessage message = (MapMessage) messageConsumer.receive();
                            if (message == null) {
                                continue;
                            }
                            final String topic = message.getStringProperty("qpid.subject");
                            final String jsonString = message.getStringProperty("json");
                            final String className = message.getStringProperty("class");
                            if (jsonTopics.contains(topic)
                                    && !StringUtils.isEmpty(className)
                                    && !StringUtils.isEmpty(jsonString)) {
                                final Class clazz = Class.forName(className);
                                final Object object = gson.fromJson(jsonString, clazz);
                                jsonReceiveQueue.put(new JsonMessage(topic, object));
                            }
                        } catch (final JMSException e) {
                            LOGGER.error("Error receiving JSON message.", e);
                        } catch (final ClassNotFoundException e) {
                            LOGGER.error("Error deserializing JSON message.", e);
                        }
                    }
                } catch (InterruptedException e) {
                    LOGGER.debug(e.toString());
                }
            }
        });
        jsonListenThread.start();

        // Add shutdown hook to shutdown the listen thread when JVM exits.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                shutdownThreads = true;
                jsonListenThread.interrupt();
                try {
                    jsonListenThread.join();
                } catch (final InterruptedException e) {
                    LOGGER.debug(e.toString());
                }
            }
        });
    }


}
