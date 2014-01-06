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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.SQL;

import java.sql.ResultSet;
import java.sql.SQLException;
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
 * @author Nikolay A. Viguro
 */

public class JsonMessaging {

    private static Logger LOGGER = LogManager.getLogger(JsonMessaging.class);
    private UUID sender;
    private boolean shutdownThreads = false;
    private Set<String> jsonSubjects = Collections.synchronizedSet(new HashSet<String>());
    private Thread jsonBroadcastListenThread;
    private BlockingQueue<JsonEnvelope> jsonReceiveQueue = new ArrayBlockingQueue<JsonEnvelope>(50);
    private int myLastID = 0;
    private SQL sql = new SQL();
    private final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    public JsonMessaging(final UUID sender) {
        this.sender = sender;
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
     * Closes connection to SQL message broker.
     */
    public void close() {
        try {
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
     */
    public void broadcast(final String subject, final Object object) {

        final String className = object.getClass().getName();
        final String jsonString = gson.toJson(object);

        sql.doQuery("INSERT INTO messages (time, subject, sender, class, json) " +
                "VALUES (CURRENT_TIMESTAMP(), '" + subject + "', '" + sender + "', '" + className + "', '" + jsonString + "')");
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
     * Method for receiving subscribing to listen JSON objects on a subject.
     *
     * @param subject the subject
     */
    public void subscribe(final String subject) {
        jsonSubjects.add(subject);
    }

    private void listenBroadcasts() {

        try {
            while (!shutdownThreads) {
                try {
                    ResultSet rs = sql.select("SELECT * FROM messages WHERE time > (now() - INTERVAL 3 SECOND)");

                    while (rs.next()) {

                        String subject = rs.getString("subject");

                        // Check wildcard
                        for (String jsonSubj : jsonSubjects) {
                            if (wildCardMatch(jsonSubj, subject) || jsonSubjects.contains(subject)) {
                                String jsonString = rs.getString("json");
                                String className = rs.getString("class");
                                int id = rs.getInt("id");

                                if (!StringUtils.isEmpty(className)
                                        && !StringUtils.isEmpty(jsonString)
                                        && id != myLastID) {

                                    final Class clazz = Class.forName(className);
                                    Object object = gson.fromJson(jsonString, clazz);
                                    JsonEnvelope envelope = new JsonEnvelope(rs.getString("sender"), null, subject, object);

                                    LOGGER.debug("Received message: "
                                            + " sender: " + envelope.getSenderInstance()
                                            + " receiver: " + envelope.getReceiverInstance()
                                            + " to subject: "
                                            + envelope.getSubject() + " (" + envelope.getClass().getSimpleName() + ")"
                                            + " JSON: " + jsonString);
                                    myLastID = id;
                                    jsonReceiveQueue.put(envelope);
                                }
                            }
                        }
                    }

                    rs.close();

                    Thread.sleep(500L);

                } catch (final SQLException e) {
                    LOGGER.debug("Error receiving JSON message ", e);
                } catch (final ClassNotFoundException e) {
                    LOGGER.error("Error deserializing JSON message ", e);
                } catch (final InterruptedException e) {
                    //LOGGER.error("Interrupt error in JSON message ", e);
                }

            }
        } catch (Exception e) {
            LOGGER.error("Error JsonMessaging: " + e.toString());
            e.printStackTrace();
        }
    }


    /**
     * Performs a wildcard matching for the text and pattern
     * provided.
     *
     * @param text    the text to be tested for matches.
     * @param pattern the pattern to be matched for.
     *                This can contain the wildcard character '*' (asterisk).
     * @return <tt>true</tt> if a match is found, <tt>false</tt>
     *         otherwise.
     */

    private boolean wildCardMatch(String pattern, String text) {
        // add sentinel so don't need to worry about *'s at end of pattern
        text += '\0';
        pattern += '\0';

        int N = pattern.length();

        boolean[] states = new boolean[N + 1];
        boolean[] old = new boolean[N + 1];
        old[0] = true;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            states = new boolean[N + 1];       // initialized to false
            for (int j = 0; j < N; j++) {
                char p = pattern.charAt(j);

                // hack to handle *'s that match 0 characters
                if (old[j] && (p == '*')) old[j + 1] = true;

                if (old[j] && (p == c)) states[j + 1] = true;
                if (old[j] && (p == '*')) states[j] = true;
                if (old[j] && (p == '*')) states[j + 1] = true;
            }
            old = states;
        }
        return states[N];
    }
}
