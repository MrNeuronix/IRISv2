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

import com.google.gson.Gson;
import fi.ceci.dao.ElementDao;
import fi.ceci.dao.EventDao;
import fi.ceci.model.Bus;
import fi.ceci.model.Element;
import fi.ceci.model.ElementType;
import fi.ceci.model.Event;
import org.apache.log4j.Logger;
import ru.iris.common.Config;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.ServiceAdvertisement;
import ru.iris.common.messaging.model.ServiceCapability;
import ru.iris.common.messaging.model.ServiceStatus;

import javax.jms.*;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

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
    /** The service type for IRIS service integration. */
    public static final String SERVICE_TYPE = "Ceci Web UI";
    /** The service capabilities for IRIS service integration. */
    private static final ServiceCapability[] CAPABILITIES = new ServiceCapability[]{};

    /**
     * The entityManagerFactory.
     */
    private final EntityManagerFactory entityManagerFactory;
    /**
     * The bus this client is connected to.
     */
    private final Bus bus;

    /** Boolean flag reflecting whether shutdown is in progress. */
    private boolean shutdown = false;
    private final JsonMessaging jsonMessaging;
    private final UUID instanceId;
    private final Thread busThread;

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

        final Map<String, String > config = new Config().getConfig();
        instanceId = UUID.fromString(bus.getBusId());

        jsonMessaging = new JsonMessaging(instanceId, config.get("keystore-path"), config.get("keystore-password"));
        jsonMessaging.subscribe("service.status");

        busThread = new Thread(new Runnable() {
            @Override
            public void run() {
                process();
            }
        });
    }

    /**
     * Starts client.
     */
    public final void start() {
        jsonMessaging.start();
        busThread.start();

        jsonMessaging.broadcast("service.status",
                new ServiceAdvertisement(SERVICE_TYPE, instanceId, ServiceStatus.STARTUP, CAPABILITIES));
    }

    private final void process() {
        final EntityManager entityManager = entityManagerFactory.createEntityManager();
        long lastStatusBroadcastMillis = System.currentTimeMillis();
        while(!shutdown) {
            try {
                // Lets wait for 100 ms on json messages and if nothing comes then proceed to carry out other tasks.
                final JsonEnvelope envelope = jsonMessaging.receive(100);
                if (envelope != null) {
                    if (envelope.getObject() instanceof ServiceAdvertisement) {
                        // We know of service advertisement. Lets log it properly.
                        final ServiceAdvertisement serviceAdvertisement = envelope.getObject();
                        LOGGER.info("Service '" + serviceAdvertisement.getType()
                                + "' status: '" + serviceAdvertisement.getStatus()
                                + "' capabilities: " + Arrays.asList(serviceAdvertisement.getCapabilities())
                                + " instance: '" + serviceAdvertisement.getInstanceId()
                                + "'"
                        );
                        updateService(entityManager, serviceAdvertisement);
                        saveEvent(entityManager, serviceAdvertisement);
                    } else if (envelope.getReceiverInstanceId() == null) {
                        // We received unknown broadcast message. Lets make generic log entry.
                        LOGGER.info("Received broadcast "
                                + " from " + envelope.getSenderInstanceId()
                                + " to " + envelope.getReceiverInstanceId()
                                + " at '" +  envelope.getSubject()
                                + ": " + envelope.getObject());
                    } else {
                        // We received unknown request message. Lets make generic log entry.
                        LOGGER.info("Received request "
                                + " from " + envelope.getSenderInstanceId()
                                + " to " + envelope.getReceiverInstanceId()
                                + " at '" +  envelope.getSubject()
                                + ": " + envelope.getObject());
                        saveEvent(entityManager, envelope.getObject());
                    }
                }

                // If there is more than 60 seconds from last availability broadcasts then lets redo this.
                if (60000L < System.currentTimeMillis() - lastStatusBroadcastMillis) {
                    jsonMessaging.broadcast("service.status",
                            new ServiceAdvertisement(SERVICE_TYPE, instanceId, ServiceStatus.AVAILABLE, CAPABILITIES));
                    lastStatusBroadcastMillis = System.currentTimeMillis();
                }
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted while waiting JSON message.");
            } catch (final Exception e) {
                LOGGER.error("Unexpected error in bus client process loop.", e);
            }
        }
    }

    /**
     * Updates service information to database based on service advertisement.
     *
     * @param entityManager the entity manager to update with
     * @param serviceAdvertisement the service advertisement
     */
    private final void updateService(final EntityManager entityManager,
                                     final ServiceAdvertisement serviceAdvertisement) {
        Element element = ElementDao.getElement(entityManager, serviceAdvertisement.getInstanceId().toString());
        if (element == null) {
            element = new Element(
                serviceAdvertisement.getInstanceId().toString(),
                null,
                bus.getOwner(),
                ElementType.SERVICE,
                serviceAdvertisement.getType(),
                serviceAdvertisement.getType());
        } else {
            element.setName(serviceAdvertisement.getType());
            element.setCategory(serviceAdvertisement.getType());
            element.setModified(new Date());;
        }
        ElementDao.saveElements(entityManager, Arrays.asList(element));
    }

    private void saveEvent(final EntityManager entityManager, final Object object) {
        Gson gson = new Gson();
        final Event event = new Event(bus.getOwner(), gson.toJson(object), object.getClass().getCanonicalName(), new Date());
        EventDao.saveEvents(entityManager, Arrays.asList(event));
    }

    /**
     * Closes client.
     */
    public final void close() {
        shutdown = true;
        try {
            busThread.join();
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted while waiting bus thread to exit.");
        }

        // Broadcast that this service is shutdown.
        jsonMessaging.broadcast("service.status",
                new ServiceAdvertisement(SERVICE_TYPE, instanceId, ServiceStatus.SHUTDOWN, CAPABILITIES));

        // Close JSON messaging.
        jsonMessaging.close();
    }

    /**
     * Synchronizes inventory.
     * @return true if inventory synchronization succeeded.
     */
    public final boolean synchronizeInventory() {
        return true;
    }

    /**
     * Save element.
     * @param element the element
     * @return true if element save was success.
     */
    public final boolean saveElement(final Element element) {
        return true;
    }

    /**
     * Removes element.
     * @param element the element
     * @return true if element was success.
     */
    public final boolean removeElement(final Element element) {
        return true;
    }

    /**
     * Sends event message.
     *
     * @param eventMessage the eventMessage
     * @return the reply
     */
    public final void sendEvent(final MapMessage eventMessage) {
        return;
    }

    /**
     * Sends command message and waits for response.
     *
     * @param commandMessage the commandMessage
     * @return the reply
     */
    public final Message sendCommand(final MapMessage commandMessage) {
        return null;
    }


    /**
     * Request inventory.
     *
     * @param entityManager the entityManager
     * @param owner the owning company
     *
     * @throws Exception exception occurs in inventory request message sending.
     */
    /*private void requestInventory(final EntityManager entityManager, final Company owner) throws Exception {
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
                if (element.getEventType() == ElementType.BUILDING) {
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
                        element.setEventType(ElementType.DEVICE);
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
    }*/


}
