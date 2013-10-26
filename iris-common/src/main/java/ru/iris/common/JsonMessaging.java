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
import javax.jms.Queue;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

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
    /** The reply queue. */
    private Queue replyQueue;
    /** The message producer. */
    private MessageProducer messageProducer;
    /** The message consumer. */
    private MessageConsumer messageConsumer;
    /** The reply producer. */
    private MessageProducer replyProducer;
    /** The reply consumer. */
    private MessageConsumer replyConsumer;


    /** Boolean flag reflecting whether threads should be shutdown. */
    private boolean shutdownThreads = false;
    /** The subjects that has been registered to receive JSON encoded messages. */
    private Set<String> jsonSubjects = Collections.synchronizedSet(new HashSet<String>());
    /** The receive queue for JSON objects. */
    private BlockingQueue<Envelope> jsonReceiveQueue = new ArrayBlockingQueue<Envelope>(100);
    /** The JSON broadcast listen thread. */
    private Thread jsonBroadcastListenThread;
    /** The JSON reply listen thread. */
    private Thread jsonReplyListenThread;
    /** The replies. */
    private Map<String, Envelope> replies = Collections.synchronizedMap(new HashMap<String, Envelope>());

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
        replyQueue = session.createTemporaryQueue();
        messageConsumer = session.createConsumer(destination);
        messageProducer = session.createProducer(destination);
        replyConsumer = session.createConsumer(replyQueue);
        replyProducer = session.createProducer(null);
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
            if (jsonBroadcastListenThread != null) {
                jsonBroadcastListenThread.interrupt();
                jsonBroadcastListenThread.join();
            }
        } catch (final Exception e) {
            LOGGER.error("Error shutting down JsonMessaging.", e);
        }
    }

    /**
     * Inner value object class to encapsulate topic name and Java object together before sending
     * or after receiving.
     */
    public static class Envelope {
        /**
         * The correlation ID.
         */
        private String correlationId;
        /**
         * The reply destination.
         */
        private Destination replyDestination;
        /**
         * The subject.
         */
        private String subject;
        /**
         * The object.
         */
        private Object object;

        public Envelope(String subject, Object object) {
            this.subject = subject;
            this.object = object;
        }

        public Envelope(String correlationId, Destination replyDestination, String subject, Object object) {
            this.correlationId = correlationId;
            this.replyDestination = replyDestination;
            this.subject = subject;
            this.object = object;
        }

        public String getCorrelationId() {
            return correlationId;
        }

        public Destination getReplyDestination() {
            return replyDestination;
        }

        public String getSubject() {
            return subject;
        }

        public Object getObject() {
            return object;
        }

        @Override
        public String toString() {
            return "Envelope{" +
                    "correlationId='" + correlationId + '\'' +
                    ", replyDestination=" + replyDestination +
                    ", subject='" + subject + '\'' +
                    ", object=" + object +
                    '}';
        }
    }

    /**
     * Sends object as JSON encoded message with given subject.
     * @param subject the subject
     * @param object the object
     * @param <T> the message class
     */
    public <T> void broadcast(final String subject, final Object object) {
        try {
            final Gson gson = new Gson();
            final String className = object.getClass().getName();
            final String jsonString = gson.toJson(object);
            final MapMessage message = session.createMapMessage();
            message.setJMSMessageID("ID:" + UUID.randomUUID().toString());
            message.setJMSReplyTo(replyQueue);
            message.setStringProperty("class", className);
            message.setStringProperty("json", jsonString);
            message.setStringProperty ("qpid.subject", subject);
            messageProducer.send(message);
        } catch (JMSException e) {
            throw new RuntimeException("Error sending JSON message: " + object + " to subject: " + object, e);
        }
    }

    /**
     * Sends request as JSON encoded message with given subject and waits for response.
     * @param subject the subject
     * @param object the object
     * @param timeout the time in millis response is waited for
     * @param <REQ> the request class
     * @param <RESP> the response class
     */
    public <REQ,RESP> RESP request(final String subject, final REQ object, long timeout)
            throws InterruptedException, TimeoutException {
        try {
            final Gson gson = new Gson();
            final String className = object.getClass().getName();
            final String jsonString = gson.toJson(object);
            final MapMessage message = session.createMapMessage();
            final String jmsCorrelationId = "ID:" + UUID.randomUUID().toString();
            message.setJMSCorrelationID(jmsCorrelationId);
            message.setJMSReplyTo(replyQueue);
            message.setStringProperty("class", className);
            message.setStringProperty("json", jsonString);
            message.setStringProperty ("qpid.subject", subject);

            replies.put(jmsCorrelationId, null);

            LOGGER.info("Sending request with correlation ID: " + message.getJMSCorrelationID() + " to subject: "
                    + subject + " (" + object.getClass().getSimpleName() + ")");
            messageProducer.send(message);

            synchronized (jmsCorrelationId) {
                jmsCorrelationId.wait(timeout);
            }

            final Envelope replyEnvelope = replies.remove(jmsCorrelationId);
            if (replyEnvelope == null) {
                throw new TimeoutException();
            }

            if (replyEnvelope.getObject() instanceof Throwable) {
                throw new RuntimeException("Exception in remote component:" + (Throwable) replyEnvelope.getObject());
            }

            return (RESP) replyEnvelope.getObject();
        } catch (JMSException e) {
            throw new RuntimeException("Error sending JSON request: " + object + " to subject: " + subject, e);
        }
    }

    /**
     * Replies object as JSON encoded message to the reply queue and subject contained in
     * received envelope.
     * @param receivedEnvelope the received envelope
     * @param replyObject the reply object
     * @param <T> the message class
     */
    public <T> void reply(final Envelope receivedEnvelope, final Object replyObject) {
        try {
            final Gson gson = new Gson();
            final String className = replyObject.getClass().getName();
            final String jsonString = gson.toJson(replyObject);
            final MapMessage message = session.createMapMessage();
            //message.setJMSMessageID("ID:" + UUID.randomUUID().toString());
            message.setJMSCorrelationID(receivedEnvelope.getCorrelationId());
            message.setJMSReplyTo(replyQueue);
            message.setStringProperty("class", className);
            message.setStringProperty("json", jsonString);
            message.setStringProperty ("qpid.subject", receivedEnvelope.getSubject());
            LOGGER.info("Sending response with correlation ID: " + message.getJMSCorrelationID()
                    + " to subject: "
                    + receivedEnvelope.getSubject() + " (" + replyObject.getClass().getSimpleName() + ")");
            replyProducer.send(receivedEnvelope.getReplyDestination(), message);
        } catch (JMSException e) {
            LOGGER.error(
                    "Error replying to JSON message: " + replyObject + " to received envelope: " + receivedEnvelope, e);
        }
    }

    /**
     * Blocking receive to listen for JOSN messages arriving to given topic.
     * @return the JSON message
     */
    public Envelope receive() throws InterruptedException {
        return (Envelope) jsonReceiveQueue.take();
    }

    /**
     * Gets the JSON message received to subject or null if nothing has been received
     * @return the JSON message or null
     */
    public Envelope getJsonObject() {
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
     * Method for receiving subscribing to listen JSON objects on a subject.
     * @param subject the subject
     */
    public void subscribeJsonSubject(final String subject) {
        jsonSubjects.add(subject);
    }

    /**
     * Starts the messaging to listen for JSON objects.
     */
    public void listenJson() {
        // Startup listen thread.
        jsonBroadcastListenThread = new Thread(new Runnable() {
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
                            final String subject = message.getStringProperty("qpid.subject");
                            final String jsonString = message.getStringProperty("json");
                            final String className = message.getStringProperty("class");
                            if (jsonSubjects.contains(subject)
                                    && !StringUtils.isEmpty(className)
                                    && !StringUtils.isEmpty(jsonString)) {
                                final Class clazz = Class.forName(className);
                                final Object object = gson.fromJson(jsonString, clazz);
                                final Envelope envelope = new Envelope(
                                        message.getJMSCorrelationID(), message.getJMSReplyTo(), subject, object);

                                LOGGER.info("Received message with ID: " + message.getJMSMessageID()
                                        + " with correlation ID: " + message.getJMSCorrelationID()
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
                } catch (InterruptedException e) {
                    LOGGER.debug(e.toString());
                }
            }
        });
        jsonBroadcastListenThread.start();

        // Startup listen thread.
        jsonReplyListenThread = new Thread(new Runnable() {
            @Override
            public void run() {
                final Gson gson = new Gson();
                while (!shutdownThreads) {
                    try {
                        final MapMessage message = (MapMessage) replyConsumer.receive();
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
                            final Envelope envelope = new Envelope(
                                    message.getJMSMessageID(), message.getJMSReplyTo(), subject, object);
                            if (replies.containsKey(message.getJMSCorrelationID())) {
                                String jmsCorrelationId = null;
                                for (final String jmsCorrelationCandidateId : replies.keySet()) {
                                    if (jmsCorrelationCandidateId.equals(message.getJMSCorrelationID())) {
                                        jmsCorrelationId = jmsCorrelationCandidateId;
                                    }
                                }
                                replies.put(jmsCorrelationId, envelope);
                                synchronized (jmsCorrelationId) {
                                    jmsCorrelationId.notify();
                                }
                                LOGGER.info("Received response with ID: " + message.getJMSMessageID()
                                        + " with correlation ID: " + message.getJMSCorrelationID()
                                        + " to subject: "
                                        + envelope.getSubject() + " (" + envelope.getClass().getSimpleName() + ")");
                            } else {
                                LOGGER.warn("Received response to unknown request ID:"
                                        + message.getJMSCorrelationID());
                            }
                        }
                    } catch (final JMSException e) {
                        LOGGER.error("Error receiving JSON message.", e);
                    } catch (final ClassNotFoundException e) {
                        LOGGER.error("Error deserializing JSON message.", e);
                    }
                }
            }
        });
        jsonReplyListenThread.start();

        // Add shutdown hook to shutdown the listen thread when JVM exits.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                shutdownThreads = true;
                jsonBroadcastListenThread.interrupt();
                try {
                    jsonBroadcastListenThread.join();
                } catch (final InterruptedException e) {
                    LOGGER.debug(e.toString());
                }
                jsonReplyListenThread.interrupt();
                try {
                    jsonReplyListenThread.join();
                } catch (final InterruptedException e) {
                    LOGGER.debug(e.toString());
                }
            }
        });
    }


}
