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

import fi.ceci.model.Element;
import fi.ceci.model.RecordSet;
import fi.ceci.model.RecordType;
import org.apache.log4j.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.List;

/**
 * Data access object for RecordSet.
 *
 * @author Tommi S.E. Laukkanen
 */
public final class RecordSetDao {

    /** The logger. */
    private static final Logger LOG = Logger.getLogger(RecordSetDao.class);

    /**
     * Saves recordSets to database.
     * @param entityManager the entity manager
     * @param recordSets the recordSets
     */
    public static void saveRecordSets(final EntityManager entityManager, final List<RecordSet> recordSets) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            for (final RecordSet recordSet : recordSets) {
                recordSet.setModified(new Date());
                entityManager.persist(recordSet);
            }
            transaction.commit();
        } catch (final Exception e) {
            LOG.error("Error in add recordSet.", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes recordSet from database.
     * @param entityManager the entity manager
     * @param recordSet the recordSet
     */
    public static void removeRecordSet(final EntityManager entityManager, final RecordSet recordSet) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            entityManager.remove(recordSet);
            transaction.commit();
        } catch (final Exception e) {
            LOG.error("Error in remove recordSet.", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets given recordSet.
     * @param entityManager the entity manager.
     * @param element the element
     * @param name the name of the recordSet
     * @return the recordSet
     */
    public static RecordSet getRecordSet(final EntityManager entityManager, final Element element, final String name) {
        final TypedQuery<RecordSet> query = entityManager.createQuery(
                "select e from RecordSet as e where e.element=:element and e.name=:name",
                RecordSet.class);
        query.setParameter("element", element);
        query.setParameter("name", name);
        final List<RecordSet> recordSets = query.getResultList();
        if (recordSets.size() == 1) {
            return recordSets.get(0);
        } else if (recordSets.size() == 0) {
            return null;
        } else {
            throw new RuntimeException("Multiple recordSets with same element and name in database. Constraint is missing.");
        }
    }

    /**
     * Gets given recordSet.
     * @param entityManager the entity manager.
     * @param element the element
     * @param type the type of the record
     * @return the recordSet
     */
    public static RecordSet getRecordSet(final EntityManager entityManager, final Element element, final RecordType type) {
        final TypedQuery<RecordSet> query = entityManager.createQuery(
                "select e from RecordSet as e where e.element=:element and e.type=:type",
                RecordSet.class);
        query.setParameter("element", element);
        query.setParameter("type", type);
        final List<RecordSet> recordSets = query.getResultList();
        if (recordSets.size() == 1) {
            return recordSets.get(0);
        } else if (recordSets.size() == 0) {
            return null;
        } else {
            throw new RuntimeException("Multiple recordSets with same element and name in database. Constraint is missing.");
        }
    }


    /**
     * Gets given recordSets.
     * @param entityManager the entity manager.
     * @param element the element
     * @return the recordSet
     */
    public static List<RecordSet> getRecordSets(final EntityManager entityManager, final Element element) {
        final TypedQuery<RecordSet> query = entityManager.createQuery(
                "select e from RecordSet as e where e.element=:element",
                RecordSet.class);
        query.setParameter("element", element);
        return query.getResultList();
    }



    /**
     * Gets given recordSets.
     * @param entityManager the entity manager.
     * @param parent the element
     * @param type the record type
     * @return the recordSet
     */
    public static List<RecordSet> getRecordSetsByParent(final EntityManager entityManager, final Element parent,
                                                        final RecordType type) {
        final TypedQuery<RecordSet> query = entityManager.createQuery(
                "select e from RecordSet as e where e.element.parent=:parent and e.type=:type",
                RecordSet.class);
        query.setParameter("parent", parent);
        query.setParameter("type", type);
        return query.getResultList();
    }

    /**
     * Gets given recordSet.
     * @param entityManager the entity manager.
     * @param id the name of the recordSet
     * @return the recordSet
     */
    public static RecordSet getRecordSet(final EntityManager entityManager, final String id) {
        try {
            return entityManager.getReference(RecordSet.class, id);
        } catch (final EntityNotFoundException e) {
            return null;
        }
    }


}
