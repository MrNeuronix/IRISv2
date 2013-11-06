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

import fi.ceci.model.Bus;
import org.apache.log4j.Logger;
import org.vaadin.addons.sitekit.model.Company;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.List;

/**
 * Data access object for Bus.
 *
 * @author Tommi S.E. Laukkanen
 */
public final class BusDao {

    /** The logger. */
    private static final Logger LOG = Logger.getLogger(BusDao.class);

    /**
     * Saves buses to database.
     * @param entityManager the entity manager
     * @param buss the buses
     */
    public static void saveBuses(final EntityManager entityManager, final List<Bus> buss) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            for (final Bus bus : buss) {
                bus.setModified(new Date());
                entityManager.persist(bus);
            }
            transaction.commit();
        } catch (final Exception e) {
            LOG.error("Error in add bus.", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes bus from database.
     * @param entityManager the entity manager
     * @param bus the bus
     */
    public static void removeBus(final EntityManager entityManager, final Bus bus) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            entityManager.remove(bus);
            transaction.commit();
        } catch (final Exception e) {
            LOG.error("Error in remove bus.", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets given bus.
     * @param entityManager the entity manager.
     * @param owner the owning company
     * @param name the name of the bus
     * @return the bus
     */
    public static Bus getBus(final EntityManager entityManager, final Company owner, final String name) {
        final TypedQuery<Bus> query = entityManager.createQuery("select e from Bus as e where e.owner=:owner and e.name=:name",
                Bus.class);
        query.setParameter("owner", owner);
        query.setParameter("name", name);
        final List<Bus> buss = query.getResultList();
        if (buss.size() == 1) {
            return buss.get(0);
        } else if (buss.size() == 0) {
            return null;
        } else {
            throw new RuntimeException("Multiple buss with same owner company and email address in database. Constraint is missing.");
        }
    }

    /**
     * Gets given buses.
     * @param entityManager the entity manager.
     * @param owner the owning company
     * @return list of buses.
     */
    public static List<Bus> getBuses(final EntityManager entityManager, final Company owner) {
        final TypedQuery<Bus> query = entityManager.createQuery("select e from Bus as e where e.owner=:owner",
                Bus.class);
        query.setParameter("owner", owner);
        return query.getResultList();
    }

    /**
     * Gets given bus.
     * @param entityManager the entity manager.
     * @param id the name of the bus
     * @return the bus
     */
    public static Bus getBus(final EntityManager entityManager, final String id) {
        try {
            return entityManager.getReference(Bus.class, id);
        } catch (final EntityNotFoundException e) {
            return null;
        }
    }


}
