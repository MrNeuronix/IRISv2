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

import fi.ceci.dao.BusDao;
import fi.ceci.model.Bus;
import fi.ceci.model.BusConnectionStatus;
import org.apache.log4j.Logger;
import org.vaadin.addons.sitekit.dao.CompanyDao;
import org.vaadin.addons.sitekit.model.Company;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

/**
 * Bus client manager class.
 *
 * @author Tommi S.E. Laukkanen
 */
public class BusClientManager {
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(BusClientManager.class);
    /**
     * The entityManagerFactory.
     */
    private EntityManagerFactory entityManagerFactory;
    /**
     * Set true to shutdown threads.
     */
    private boolean closeRequested = false;
    /**
     * The manager thread.
     */
    private final Thread managerThread;
    /**
     * The bus clients.
     */
    private Map<Bus, BusClient> clients = new HashMap<Bus, BusClient>();

    /**
     * Constructor which allows setting the entity manager factory.
     *
     * @param entityManagerFactory the entityManagerFactory
     */
    public BusClientManager(final EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;

        managerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                final EntityManager entityManager = entityManagerFactory.createEntityManager();
                while (!closeRequested) {
                    try {
                        manageClients(entityManager);
                    } catch (final Throwable t) {
                        LOGGER.error("Error in manager thread.", t);
                    }
                    try {
                        Thread.sleep(30000);
                    } catch (final InterruptedException t) {
                        LOGGER.debug("Interrupt in manager thread sleep.");
                    }
                }

                for (final Bus bus : clients.keySet()) {
                    try {
                        final BusClient client = clients.get(bus);
                        client.close();
                        entityManager.refresh(bus);
                        bus.setConnectionStatus(BusConnectionStatus.Disconnected);
                        BusDao.saveBuses(entityManager, Collections.singletonList(bus));
                    } catch (final Exception e) {
                        LOGGER.warn("Exception in bus client close.", e);
                    }
                }

                entityManager.close();
            }
        });

        managerThread.start();
    }

    /**
     * Closes the manager.
     */
    public final synchronized void close() {
        closeRequested = true;
        managerThread.interrupt();
        try {
            managerThread.join();
        } catch (InterruptedException e) {
            LOGGER.debug("BusClientManager close wait interrupted.");
        }
    }

    /**
     * Get bus client.
     * @param bus the bus
     * @return the bus client
     */
    public final synchronized BusClient getBusClient(final Bus bus) {
        return clients.get(bus);
    }

    /**
     * @return true if one or more bus clients is connected.
     */
    public final synchronized boolean isConnected() {
        for (final Bus bus : clients.keySet()) {
            if (bus.getConnectionStatus() == BusConnectionStatus.Connected ||
                    bus.getConnectionStatus() == BusConnectionStatus.Synchronizing) {
                return true;
            }
        }
        return false;
    }

    /**
     * Manages clients.
     * @param entityManager the entityManager
     */
    private void manageClients(final EntityManager entityManager) {
        final Set<Bus> activeBuses = new HashSet<Bus>();

        final List<Company> companies = CompanyDao.getCompanies(entityManager);
        for (final Company company : companies) {
            for (final Bus bus : BusDao.getBuses(entityManager, company)) {
                if (bus.getHost() != null && bus.getHost().length() > 0
                        && bus.getPort() != null && bus.getPort() > 0
                        && bus.getUserName() != null && bus.getUserName().length() > 0
                        && bus.getUserPassword() != null && bus.getUserPassword().length() > 0) {
                    activeBuses.add(bus);
                }
            }
        }

        for (final Bus bus : clients.keySet()) {
            if (!activeBuses.contains(bus)) {
                entityManager.refresh(bus);
                final BusClient client = clients.remove(bus);
                try {
                    client.close();
                    bus.setConnectionStatus(BusConnectionStatus.Disconnected);
                    BusDao.saveBuses(entityManager, Collections.singletonList(bus));
                } catch (final Exception e) {
                    LOGGER.warn("Exception in bus client close.", e);
                }
            }
        }

        for (final Bus bus : activeBuses) {
            if (!clients.containsKey(bus)) {
                try {
                    final BusClient busClient = new BusClient(entityManagerFactory, bus);
                    busClient.start();
                    clients.put(bus, busClient);
                    entityManager.refresh(bus);
                    bus.setConnectionStatus(BusConnectionStatus.Connected);
                    BusDao.saveBuses(entityManager, Collections.singletonList(bus));
                } catch (final Exception e) {
                    LOGGER.warn("Exception in bus client connect.", e);
                    bus.setConnectionStatus(BusConnectionStatus.Error);
                    BusDao.saveBuses(entityManager, Collections.singletonList(bus));
                }
            }
        }

    }
}
