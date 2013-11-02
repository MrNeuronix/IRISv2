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

import fi.ceci.model.Record;
import fi.ceci.model.RecordSet;
import org.apache.log4j.Logger;
import org.vaadin.addons.sitekit.model.Company;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.List;

/**
 * Data access object for Record.
 * @author Tommi S.E. Laukkanen
 */
public final class RecordDao {

    /** The logger. */
    private static final Logger LOG = Logger.getLogger(RecordDao.class);

    /**
     * Saves records to database.
     * @param entityManager the entity manager
     * @param records the records
     */
    public static void saveRecords(final EntityManager entityManager, final List<Record> records) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            for (final Record record : records) {
                record.setModified(new Date());
                entityManager.persist(record);
            }
            transaction.commit();
        } catch (final Exception e) {
            LOG.error("Error in add record.", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes record from database.
     * @param entityManager the entity manager
     * @param record the record
     */
    public static void removeRecord(final EntityManager entityManager, final Record record) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            entityManager.remove(record);
            transaction.commit();
        } catch (final Exception e) {
            LOG.error("Error in remove record.", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets given record.
     * @param entityManager the entity manager.
     * @param owner the owning company
     * @param name the name of the record
     * @return the record
     */
    public static Record getRecord(final EntityManager entityManager, final Company owner, final String name) {
        final TypedQuery<Record> query = entityManager.createQuery("select e from Record as e where e.owner=:owner and e.name=:name",
                Record.class);
        query.setParameter("owner", owner);
        query.setParameter("name", name);
        final List<Record> records = query.getResultList();
        if (records.size() == 1) {
            return records.get(0);
        } else if (records.size() == 0) {
            return null;
        } else {
            throw new RuntimeException("Multiple records with same owner company and email address in database. Constraint is missing.");
        }
    }

    /**
     * Gets given records.
     * @param entityManager the entity manager.
     * @param owner the owning company
     * @return list of records.
     */
    public static List<Record> getRecords(final EntityManager entityManager, final Company owner) {
        final TypedQuery<Record> query = entityManager.createQuery("select e from Record as e where e.owner=:owner order by e.created",
                Record.class);
        query.setParameter("owner", owner);
        return query.getResultList();
    }

    /**
     * Gets given records.
     * @param entityManager the entity manager.
     * @param recordSet the record set
     * @return list of records.
     */
    public static List<Record> getRecords(final EntityManager entityManager, final RecordSet recordSet) {
        final TypedQuery<Record> query = entityManager.createQuery(
                "select e from Record as e where e.recordSet=:recordSet order by e.created desc",
                Record.class);
        query.setParameter("recordSet", recordSet);
        return query.getResultList();
    }

    /**
     * Gets given records.
     * @param entityManager the entity manager.
     * @param recordSet the record set
     * @param since the since after which records are to be included
     * @return list of records.
     */
    public static List<Record> getRecords(final EntityManager entityManager, final RecordSet recordSet,
                                          final Date since) {
        final TypedQuery<Record> query = entityManager.createQuery(
                "select e from Record as e where e.recordSet=:recordSet and e.created>=:since order by e.created desc",
                Record.class);
        query.setParameter("recordSet", recordSet);
        query.setParameter("since", since);
        return query.getResultList();
    }

    /**
     * Gets given records.
     * @param entityManager the entity manager.
     * @param recordSet the record set
     * @param maxResults max result sizle
     * @return list of records.
     */
    public static List<Record> getRecords(final EntityManager entityManager, final RecordSet recordSet,
                                          final int maxResults) {
        final TypedQuery<Record> query = entityManager.createQuery(
                "select e from Record as e where e.recordSet=:recordSet order by e.created desc",
                Record.class);
        query.setParameter("recordSet", recordSet);
        query.setMaxResults(maxResults);
        return query.getResultList();
    }

    /**
     * Gets given record.
     * @param entityManager the entity manager.
     * @param id the name of the record
     * @return the record
     */
    public static Record getRecord(final EntityManager entityManager, final String id) {
        try {
            return entityManager.getReference(Record.class, id);
        } catch (final EntityNotFoundException e) {
            return null;
        }
    }


}
