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
package fi.ceci.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.ceci.dao.BusDao;
import fi.ceci.dao.ElementDao;
import fi.ceci.dao.EventDao;
import fi.ceci.model.Bus;
import fi.ceci.model.Element;
import fi.ceci.model.ElementType;
import fi.ceci.model.Event;
import org.apache.log4j.Logger;
import org.apache.qpid.client.AMQAnyDestination;
import org.apache.qpid.client.message.JMSBytesMessage;
import org.apache.qpid.messaging.Address;
import org.vaadin.addons.sitekit.model.Company;

import javax.jms.*;
import javax.jms.Queue;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Ago control qpid client.
 *
 * @author Tommi S.E. Laukkanen
 */
public class BusClient {
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(BusClient.class);
    /** Default bus name. */
    private static final String DEFAULT = "Default";
    /**
     * The entityManagerFactory.
     */
    private EntityManagerFactory entityManagerFactory;
    /**
     * JSON object mapper.
     * */
    private final ObjectMapper mapper = new ObjectMapper();
    /**
     * The context.
     */
    private final Context context;
    /**
     * The connection factory.
     */
    private final ConnectionFactory connectionFactory;
    /**
     * The connection.
     */
    private final Connection connection;
    /**
     * The session.
     */
    private final Session session;
    /**
     * The reply queue.
     */
    private final Queue replyQueue;
    /**
     * The message producer.
     */
    private final MessageProducer messageProducer;
    /**
     * The message consumer.
     */
    private final MessageConsumer messageConsumer;
    /**
     * The reply consumer.
     */
    private final MessageConsumer replyConsumer;
    /**
     * Set true to shutdown threads.
     */
    private boolean closeRequested = false;
    /**
     * Event handler thread.
     */
    private final Thread eventHandlerThread;
    /**
     * Reply handler thread.
     */
    private final Thread replyHandlerThread;
    /**
     * Inventory request thread.
     */
    private final Thread inventoryRequestThread;
    /**
     * The bus this client is connected to.
     */
    private final Bus bus;
    /**
     * Reply message queue.
     */
    private final BlockingQueue<Message> replyMessageQueue = new LinkedBlockingQueue<Message>(10);

    /**
     * Constructor which sets entityManagerFactory.
     *
     * @param entityManagerFactory the entityManagerFactory
     * @param bus the bus to connect to
     *
     * @throws Exception if exception occurs in connecting to bus.
     */
    public BusClient(final EntityManagerFactory entityManagerFactory, final Bus bus) throws Exception {
        this.entityManagerFactory = entityManagerFactory;
        this.bus = bus;

        final String userName = bus.getUserName();
        final String password = bus.getUserPassword();
        final String host = bus.getHost();
        final Integer port = bus.getPort();
        //final String userName = "agocontrol";
        //final String password = "letmein";


        final Properties properties = new Properties();
        properties.put("java.naming.factory.initial",
                "org.apache.qpid.jndi.PropertiesFileInitialContextFactory");
        properties.put("connectionfactory.qpidConnectionfactory",
                "amqp://" + userName + ":" + password + "@agocontrolvaadinsite/client" +
                        "?brokerlist='tcp://" + host + ":" + port + "'");

        context = new InitialContext(properties);
        connectionFactory = (ConnectionFactory) context.lookup("qpidConnectionfactory");
        connection = connectionFactory.createConnection();

        connection.start();

        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final Destination sendDestination = new AMQAnyDestination(new Address("agocontrol", "", null));
        final Destination receiveDestination = new AMQAnyDestination(new Address("agocontrol", "#", null));

        replyQueue = session.createTemporaryQueue();

        messageProducer = session.createProducer(sendDestination);
        messageConsumer = session.createConsumer(receiveDestination);
        replyConsumer = session.createConsumer(replyQueue);

        eventHandlerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                final EntityManager entityManager = entityManagerFactory.createEntityManager();
                final Company company = entityManager.getReference(Company.class, bus.getOwner().getCompanyId());
                while (!closeRequested) {
                    try {
                        Thread.sleep(100);
                        handleEvent(entityManager, company);
                    } catch (final Throwable t) {
                        LOGGER.error("Error in handling events.", t);
                    }
                }
                entityManager.close();
            }
        });
        eventHandlerThread.start();

        replyHandlerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                final EntityManager entityManager = entityManagerFactory.createEntityManager();
                while (!closeRequested) {
                    try {
                        Thread.sleep(100);
                        handleReply(entityManager);
                    } catch (final Throwable t) {
                        LOGGER.error("Error in handling events.", t);
                    }
                }
                entityManager.close();
            }
        });
        replyHandlerThread.start();

        inventoryRequestThread = new Thread(new Runnable() {
            @Override
            public void run() {
                final EntityManager entityManager = entityManagerFactory.createEntityManager();
                final Company company = entityManager.getReference(Company.class, bus.getOwner().getCompanyId());
                while (!closeRequested) {
                    try {
                        requestInventory(entityManager, company);
                    } catch (final Throwable t) {
                        LOGGER.error("Error in inventory request message sending.", t);
                    }
                    try {
                        Thread.sleep(5 * 60 * 1000);
                    } catch (final Throwable t) {
                        LOGGER.debug("Interrupted inventory request message send wait.");
                    }
                }
                entityManager.close();
            }
        });
        inventoryRequestThread.start();
        LOGGER.info("Connected to bus: " + bus.getName());
    }


    /**
     * Closes client.
     *
     * @throws Exception if exception occurs.
     */
    public final void close() throws Exception {
        closeRequested = true;
        inventoryRequestThread.interrupt();
        inventoryRequestThread.join();
        replyHandlerThread.interrupt();
        replyHandlerThread.join();
        eventHandlerThread.interrupt();
        eventHandlerThread.join();
        connection.close();
        context.close();
        LOGGER.info("Disconnected from bus: " + bus.getName());
    }

    /**
     * Synchronizes inventory.
     * @return true if inventory synchronization succeeded.
     */
    public final boolean synchronizeInventory() {
        final EntityManager entityManager = entityManagerFactory.createEntityManager();
        final Company company = entityManager.getReference(Company.class, bus.getOwner().getCompanyId());
        try {
            requestInventory(entityManager, company);
            return true;
        } catch (final Throwable t) {
            LOGGER.error("Error in inventory synchronization.", t);
            return false;
        } finally {
            entityManager.close();
        }
    }

    /**
     * Save element.
     * @param element the element
     * @return true if element save was success.
     */
    public final boolean saveElement(final Element element) {
        try {
            switch (element.getType()) {
                case ROOM: {
                    final MapMessage commandMessage = createMapMessage();
                    commandMessage.setJMSMessageID("ID:" + UUID.randomUUID().toString());
                    commandMessage.setString("command", "setroomname");
                    commandMessage.setString("uuid", element.getElementId());
                    commandMessage.setString("name", element.getName());
                    final Message replyMessage = sendCommand(commandMessage);
                    LOGGER.error("Room set name response message: " + replyMessage.toString());
                    break;
                }
                case DEVICE: {
                    {
                        final MapMessage commandMessage = createMapMessage();
                        commandMessage.setJMSMessageID("ID:" + UUID.randomUUID().toString());
                        commandMessage.setString("command", "setdevicename");
                        commandMessage.setString("uuid", element.getElementId());
                        commandMessage.setString("name", element.getName());
                        final Message replyMessage = sendCommand(commandMessage);
                        LOGGER.error("Device set name response message: " + replyMessage.toString());
                    }
                    if (element.getParent() != null && element.getParent().getType() == ElementType.ROOM) {
                        final MapMessage commandMessage = createMapMessage();
                        commandMessage.setJMSMessageID("ID:" + UUID.randomUUID().toString());
                        commandMessage.setString("command", "setdeviceroom");
                        commandMessage.setString("uuid", element.getElementId());
                        commandMessage.setString("room", element.getParent().getElementId());
                        final Message replyMessage = sendCommand(commandMessage);
                        LOGGER.error("Set device room response message: " + replyMessage.toString());
                    }
                    break;
                }
                default:
            }
            return true;
        } catch (final Exception e) {
            LOGGER.error("Error saving element via bus.", e);
            return false;
        }
    }

    /**
     * Removes element.
     * @param element the element
     * @return true if element was success.
     */
    public final boolean removeElement(final Element element) {
        try {
            switch (element.getType()) {
                case ROOM: {
                    final MapMessage commandMessage = createMapMessage();
                    commandMessage.setJMSMessageID("ID:" + UUID.randomUUID().toString());
                    commandMessage.setString("command", "deleteroom");
                    commandMessage.setString("uuid", element.getElementId());
                    final Message replyMessage = sendCommand(commandMessage);
                    LOGGER.error("Room delete response message: " + replyMessage.toString());
                    break;
                }
                case DEVICE: {
                    final MapMessage commandMessage = createMapMessage();
                    commandMessage.setJMSMessageID("ID:" + UUID.randomUUID().toString());
                    commandMessage.setStringProperty("qpid.subject", "event.device.remove");
                    commandMessage.setString("uuid", element.getElementId());
                    sendEvent(commandMessage);
                    break;
                }
                default:
            }
            return true;
        } catch (final Exception e) {
            LOGGER.error("Error removing room via bus.", e);
            return false;
        }
    }

    /**
     * Sends event message.
     *
     * @param eventMessage the eventMessage
     * @return the reply
     */
    public final void sendEvent(final MapMessage eventMessage) {
        synchronized (messageProducer) {
            try {
                messageProducer.send(eventMessage);
            } catch (Exception e) {
                throw new RuntimeException("Error in event message sending.", e);
            }
        }
    }

    /**
     * Sends command message and waits for response.
     *
     * @param commandMessage the commandMessage
     * @return the reply
     */
    public final Message sendCommand(final MapMessage commandMessage) {
        synchronized (messageProducer) {
            try {
                replyMessageQueue.clear(); // clear reply queue to remove any unprocessed responses.

                commandMessage.setJMSReplyTo(replyQueue);
                messageProducer.send(commandMessage);

                final Message replyMessage = replyMessageQueue.poll(5, TimeUnit.SECONDS);
                if (replyMessage == null) {
                    throw new TimeoutException("Timeout in command processing.");
                }

                final Message replyMessageTwo = replyMessageQueue.poll(300, TimeUnit.MILLISECONDS);

                if (replyMessageTwo != null) {
                    LOGGER.debug("Received two commands responses.");
                }

                return replyMessageTwo != null ? replyMessageTwo : replyMessage;
            } catch (Exception e) {
                throw new RuntimeException("Error in command message sending.", e);
            }
        }
    }

    /**
     * @return the created message
     */
    public final MapMessage createMapMessage() {
        try {
            return session.createMapMessage();
        } catch (Exception e) {
            throw new RuntimeException("Error in message creation.", e);
        }
    }

    /**
     * Request inventory.
     *
     * @param entityManager the entityManager
     * @param owner the owning company
     *
     * @throws Exception exception occurs in inventory request message sending.
     */
    private void requestInventory(final EntityManager entityManager, final Company owner) throws Exception {
        synchronized (this) {
            final MapMessage commandMessage = session.createMapMessage();
            commandMessage.setJMSMessageID("ID:" + UUID.randomUUID().toString());
            commandMessage.setString("command", "inventory");

            final Message message = sendCommand(commandMessage);

            final MapMessage mapMessage;
            if (message instanceof MapMessage) {
                mapMessage = (MapMessage) message;
            } else {
                LOGGER.warn("Inventory request response was not map message." +
                        "Trying to read reply message queue once more");
                mapMessage = (MapMessage) replyMessageQueue.poll(5, TimeUnit.SECONDS);
            }

            final Map<String, Object> result = convertMapMessageToMap(mapMessage);

            final List<Element> elements = new ArrayList<Element>(ElementDao.getElements(entityManager, owner));
            final Map<String, Element> idElementMap = new HashMap<String, Element>();
            final Map<String, Element> nameBuildingMap = new HashMap<String, Element>();

            for (final Element element : elements) {
                if (element.getType() == ElementType.BUILDING) {
                    nameBuildingMap.put(element.getName(), element);
                }
                idElementMap.put(element.getElementId(), element);
            }

            if (!nameBuildingMap.containsKey(DEFAULT)) {
                final Element building = new Element(owner, ElementType.BUILDING, DEFAULT, "");
                nameBuildingMap.put(building.getName(), building);
                idElementMap.put(building.getElementId(), building);
                elements.add(building);
            }

            if (result.containsKey("rooms")) {
                final Map<String, Object> rooms = (Map) result.get("rooms");
                for (final String roomId : ((Map<String, Object>) result.get("rooms")).keySet()) {
                    final Map<String, Object> roomMessage = (Map) rooms.get(roomId);
                    final String roomName = (String) roomMessage.get("name");
                    final String roomLocation = (String) roomMessage.get("location");

                    final Element building;
                    if (nameBuildingMap.containsKey(roomLocation)) {
                        building = nameBuildingMap.get(roomLocation);
                    } else {
                        building = nameBuildingMap.get(DEFAULT);
                        nameBuildingMap.put(DEFAULT, building);
                        elements.add(building);
                        idElementMap.put(building.getElementId(), building);
                    }

                    final Element room;
                    if (idElementMap.containsKey(roomId)) {
                        room = idElementMap.get(roomId);
                        room.setBus(bus);
                        room.setParent(building);
                        room.setName(roomName);
                    } else {
                        room = new Element(roomId, building, owner, ElementType.ROOM, roomName, "");
                        room.setBus(bus);
                        elements.add(room);
                        idElementMap.put(room.getElementId(), room);
                    }
                }
            }

            if (result.containsKey("inventory")) {
                final Map<String, Object> inventory = (Map) result.get("inventory");
                for (final String elementId : ((Map<String, Object>) result.get("inventory")).keySet()) {
                    final Map<String, Object> elementMessage = (Map) inventory.get(elementId);
                    if (elementMessage == null) {
                        continue;
                    }
                    final String name = (String) elementMessage.get("name");
                    final String roomId = (String) elementMessage.get("room");
                    final String category = (String) elementMessage.get("devicetype");

                    final Element parent;
                    if (idElementMap.containsKey(roomId)) {
                        parent = idElementMap.get(roomId);
                    } else {
                        parent =  nameBuildingMap.get(DEFAULT);
                    }

                    final Element element;
                    if (idElementMap.containsKey(elementId)) {
                        element = idElementMap.get(elementId);
                        element.setBus(bus);
                        element.setParent(parent);
                        element.setName(name);
                        element.setCategory(category);
                        element.setType(ElementType.DEVICE);
                    } else {
                        element = new Element(elementId, parent, owner, ElementType.DEVICE, name, category);
                        element.setBus(bus);
                        elements.add(element);
                        idElementMap.put(element.getElementId(), element);
                    }
                }
            }

            final List<Element> roots = new ArrayList<Element>();
            final Map<Element, Set<Element>> treeMap = new HashMap<Element, Set<Element>>();

            for (final Element element : elements) {
                if (element.getParent() == null) {
                    element.setTreeDepth(0);
                    roots.add(element);
                } else {
                    final Element parent = idElementMap.get(element.getParent().getElementId());
                    if (parent != null) {
                        if (!treeMap.containsKey(parent)) {
                            treeMap.put(parent, new TreeSet<Element>());
                        }
                        treeMap.get(parent).add(element);
                    }
                }
            }

            final LinkedList<Element> elementsToIterate = new LinkedList<Element>();
            elementsToIterate.addAll(roots);

            int startTreeIndex = bus.getBusId().hashCode();
            int treeIndex = startTreeIndex;
            while (elementsToIterate.size() > 0) {
                final Element element = elementsToIterate.removeFirst();
                element.setTreeIndex(++treeIndex);
                final Set<Element> children = treeMap.get(element);
                if (children != null) {
                    for (final Element child : children) {
                        child.setTreeDepth(element.getTreeDepth() + 1);
                    }
                    elementsToIterate.addAll(0, children);
                }
            }

            ElementDao.saveElements(entityManager, elements);
            entityManager.clear();
            final Bus loadedBus = BusDao.getBus(entityManager, bus.getBusId());
            if (loadedBus != null) {
                loadedBus.setInventorySynchronized(new Date());
                BusDao.saveBuses(entityManager, Collections.singletonList(loadedBus));
                LOGGER.info("Synchronized inventory from bus: " + bus.getName());
            }

        }
    }

    /**
     * Handle reply.
     *
     * @param entityManager the entityManager
     * @throws Exception if exception occurs.
     */
    private void handleReply(final EntityManager entityManager) throws Exception  {
        final Message message = (Message) replyConsumer.receive();
        if (message == null) {
            return;
        }
        replyMessageQueue.put(message);
    }

    /**
     * Handle event.
     *
     * @param entityManager the entityManager
     * @param owner the owning company
     * @throws Exception if exception occurs.
     */
    private void handleEvent(final EntityManager entityManager, final Company owner) throws Exception  {
        final Message message = messageConsumer.receive();

        if (message instanceof MapMessage) {
            final MapMessage mapMessage = (MapMessage) message;
            if (mapMessage == null) {
                return;
            }

            final String subject = mapMessage.getStringProperty("qpid.subject");
            if (subject == null || !subject.startsWith("event")) {
                return;
            }
            if (subject.equals("event.environment.timechanged")) {
                return; // Ignore time changed events.
            }

            final Map<String, Object> map = convertMapMessageToMap(mapMessage);

            map.put("event", subject);

            final String eventJsonString = mapper.writeValueAsString(map);
            EventDao.saveEvents(entityManager, Collections.singletonList(
                    new Event(owner, eventJsonString, new Date())));
            return;
        }

        if (message instanceof JMSBytesMessage) {
            LOGGER.warn("Unhandled byte message: " + message.toString());
            return;
        }

        LOGGER.warn("Unhandled message type " + message.toString());
    }

    /**
     * Converts UTF-8 encoded byte to String.
     *
     * @param bytes the bytes
     * @return the string
     */
    private String bytesToString(final Object bytes) {
        if (bytes == null) {
            return null;
        }
        return new String((byte[]) bytes, Charset.forName("UTF-8"));
    }

    /**
     * Converts MapMessage to Map.
     *
     * @param message the message
     * @return the map
     * @throws JMSException if exception occurs in conversion.
     */
    private Map<String, Object> convertMapMessageToMap(final MapMessage message) throws JMSException {
        final Map<String, Object> map = new HashMap<String, Object>();

        final Enumeration<String> keys = message.getMapNames();
        while (keys.hasMoreElements()) {
            final String key = keys.nextElement();
            map.put(key, message.getObject(key));
        }
        convertByteArrayValuesToStrings(map);
        return map;
    }

    /**
     * Converts byte array values to string recursively.
     * @param map the map
     */
    private void convertByteArrayValuesToStrings(final Map<String, Object> map) {
        for (final String key : map.keySet()) {
            Object object = map.get(key);
            if (object instanceof Map) {
                convertByteArrayValuesToStrings((Map) object);
            }
            if (object instanceof byte[]) {
                map.put(key, bytesToString(object));
            }
        }
    }
}
