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
package fi.ceci.dao;

import fi.ceci.model.Event;
import org.apache.log4j.Logger;
import org.vaadin.addons.sitekit.model.Company;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.List;

/**
 * Data access object for Event.
 *
 *  @author Tommi S.E. Laukkanen
 */
public final class EventDao {

    /** The logger. */
    private static final Logger LOG = Logger.getLogger(EventDao.class);

    /**
     * Saves events to database.
     * @param entityManager the entity manager
     * @param events the events
     */
    public static void saveEvents(final EntityManager entityManager, final List<Event> events) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            for (final Event event : events) {
                event.setModified(new Date());
                entityManager.persist(event);
            }
            transaction.commit();
        } catch (final Exception e) {
            LOG.error("Error in add event.", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes event from database.
     * @param entityManager the entity manager
     * @param event the event
     */
    public static void removeEvent(final EntityManager entityManager, final Event event) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            entityManager.remove(event);
            transaction.commit();
        } catch (final Exception e) {
            LOG.error("Error in remove event.", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets given event.
     * @param entityManager the entity manager.
     * @param owner the owning company
     * @param name the name of the event
     * @return the event
     */
    public static Event getEvent(final EntityManager entityManager, final Company owner, final String name) {
        final TypedQuery<Event> query = entityManager.createQuery("select e from Event as e where e.owner=:owner and e.name=:name",
                Event.class);
        query.setParameter("owner", owner);
        query.setParameter("name", name);
        final List<Event> events = query.getResultList();
        if (events.size() == 1) {
            return events.get(0);
        } else if (events.size() == 0) {
            return null;
        } else {
            throw new RuntimeException("Multiple events with same owner company and email address in database. Constraint is missing.");
        }
    }

    /**
     * Gets given event.
     * @param entityManager the entity manager.
     * @param owner the owning company
     * @return list of events.
     */
    public static List<Event> getUnprocessedEvents(final EntityManager entityManager, final Company owner) {
        final TypedQuery<Event> query = entityManager.createQuery("select e from Event as e where e.owner=:owner" +
                " and e.processed is null order by e.created",
                Event.class);
        query.setParameter("owner", owner);
        return query.getResultList();
    }

    /**
     * Gets given event.
     * @param entityManager the entity manager.
     * @param id the name of the event
     * @return the event
     */
    public static Event getEvent(final EntityManager entityManager, final String id) {
        try {
            return entityManager.getReference(Event.class, id);
        } catch (final EntityNotFoundException e) {
            return null;
        }
    }


}
