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
import com.google.gson.Gson;
import fi.ceci.dao.ElementDao;
import fi.ceci.dao.EventDao;
import fi.ceci.dao.RecordDao;
import fi.ceci.dao.RecordSetDao;
import fi.ceci.model.*;
import org.apache.log4j.Logger;
import org.vaadin.addons.sitekit.dao.CompanyDao;
import org.vaadin.addons.sitekit.model.Company;
import ru.iris.common.messaging.model.ServiceAdvertisement;
import ru.iris.common.messaging.model.ServiceStatus;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

/**
 * Ago control qpid client.
 *
 * @author Tommi S.E. Laukkanen
 */
public class EventProcessor {
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(EventProcessor.class);
    /** Default bus name. */
    private static final String DEFAULT = "";
    /**
     * The entityManagerFactory.
     */
    private EntityManagerFactory entityManagerFactory;
    /** JSON object mapper. */
    private final ObjectMapper mapper = new ObjectMapper();
    /**
     * Set true to shutdown threads.
     */
    private boolean closeRequested = false;
    /**
     * The event processor thread.
     */
    private final Thread eventProcessorThread;

    /**
     * Constructor which sets entityManagerFactory.
     *
     * @param entityManagerFactory the entityManagerFactory
     */
    public EventProcessor(final EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;

        eventProcessorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                final EntityManager entityManager = entityManagerFactory.createEntityManager();
                while (!closeRequested) {
                    try {
                        final List<Company> companies = CompanyDao.getCompanies(entityManager);
                        for (final Company company : companies) {
                            processEvent(entityManager, company);
                        }

                    } catch (final Throwable t) {
                        LOGGER.error("Error in processing events.", t);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (final InterruptedException e) {
                        LOGGER.debug("Processing events sleep interrupted.");
                    }
                }
                entityManager.close();
            }
        });
        eventProcessorThread.start();

    }

    /**
     * Processes unprocessed events.
     *
     * @param entityManager the entityManager
     * @param owner the owning company
     */
    private void processEvent(final EntityManager entityManager, final Company owner) {
        final List<Event> events = EventDao.getUnprocessedEvents(entityManager, owner);

        final Gson gson = new Gson();
        for (final Event event : events) {
            try {
                final Class clazz = Class.forName(event.getEventType());
                final ServiceAdvertisement serviceAdvertisement = (ServiceAdvertisement)
                        gson.fromJson(event.getContent(), clazz);
                final String elementId = serviceAdvertisement.getInstanceId().toString();
                final String name = "availability";
                final String unit = "";
                final double value = serviceAdvertisement.getStatus() == ServiceStatus.AVAILABLE ? 1 : 0;
                RecordType recordType = RecordType.AVAILABILITY;

                final Element element = ElementDao.getElement(entityManager, elementId);

                if (element != null && element.getOwner().equals(owner)) {

                    RecordSet recordSet = RecordSetDao.getRecordSet(entityManager, element, name);

                    if (recordSet == null) {
                        recordSet = new RecordSet(
                                owner,
                                element,
                                name,
                                recordType,
                                unit,
                                event.getCreated()
                        );
                        RecordSetDao.saveRecordSets(entityManager, Collections.singletonList(recordSet));
                    }

                    RecordDao.saveRecords(entityManager, Collections.singletonList(new Record(
                            owner,
                            recordSet,
                            value,
                            event.getCreated()
                    )));

                    event.setProcessingError(false);
                } else {
                    event.setProcessingError(true);
                }

            } catch (Throwable t) {
                LOGGER.warn("Error processing event: " + event.getEventId() + " (" + t.getMessage() + ")");
                event.setProcessingError(true);
            }
            event.setProcessed(new Date());
        }

        EventDao.saveEvents(entityManager, events);
    }

    /**
     * Closes processor.
     *
     * @throws Exception if exception occurs.
     */
    public final void close() throws Exception {
        closeRequested = true;
        eventProcessorThread.interrupt();
        eventProcessorThread.join();
    }


}
