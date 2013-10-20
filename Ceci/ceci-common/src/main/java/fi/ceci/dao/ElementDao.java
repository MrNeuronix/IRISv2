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
import fi.ceci.model.ElementType;
import org.apache.log4j.Logger;
import org.vaadin.addons.sitekit.model.Company;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.List;

/**
 * Data access object for Element.
 *
 * @author Tommi S.E. Laukkanen
 */
public final class ElementDao {

    /** The logger. */
    private static final Logger LOG = Logger.getLogger(ElementDao.class);

    /**
     * Saves elements to database.
     * @param entityManager the entity manager
     * @param elements the elements
     */
    public static final void saveElements(final EntityManager entityManager, final List<Element> elements) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            for (final Element element : elements) {
                element.setModified(new Date());
                entityManager.persist(element);
            }
            transaction.commit();
        } catch (final Exception e) {
            LOG.error("Error in add element.", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes element from database.
     * @param entityManager the entity manager
     * @param element the element
     */
    public static final void removeElement(final EntityManager entityManager, final Element element) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            entityManager.remove(element);
            transaction.commit();
        } catch (final Exception e) {
            LOG.error("Error in remove element.", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets given element.
     * @param entityManager the entity manager.
     * @param owner the owning company
     * @param name the name of the element
     * @return the element
     */
    public static final Element getElement(final EntityManager entityManager, final Company owner, final String name) {
        final TypedQuery<Element> query = entityManager.createQuery("select e from Element as e where e.owner=:owner and e.name=:name",
                Element.class);
        query.setParameter("owner", owner);
        query.setParameter("name", name);
        final List<Element> elements = query.getResultList();
        if (elements.size() == 1) {
            return elements.get(0);
        } else if (elements.size() == 0) {
            return null;
        } else {
            throw new RuntimeException("Multiple elements with same owner company and email address in database. Constraint is missing.");
        }
    }

    /**
     * Gets given element.
     * @param entityManager the entity manager.
     * @param owner the owning company
     * @return list of elements.
     */
    public static final List<Element> getElements(final EntityManager entityManager, final Company owner) {
        final TypedQuery<Element> query = entityManager.createQuery("select e from Element as e where e.owner=:owner " +
                "order by e.treeIndex",
                Element.class);
        query.setParameter("owner", owner);
        return query.getResultList();
    }

    /**
     * Gets given element.
     * @param entityManager the entity manager.
     * @param owner the owning company
     * @param type the element type
     * @return list of elements.
     */
    public static final List<Element> getElements(final EntityManager entityManager, final Company owner, final
    ElementType type) {
        final TypedQuery<Element> query = entityManager.createQuery("select e from Element as e where e.owner=:owner" +
                " and e.type=:type order by e.name",
                Element.class);
        query.setParameter("owner", owner);
        query.setParameter("type", type);
        return query.getResultList();
    }

    /**
     * Gets given element.
     * @param entityManager the entity manager.
     * @param id the name of the element
     * @return the element
     */
    public static final Element getElement(final EntityManager entityManager, final String id) {
        try {
            return entityManager.getReference(Element.class, id);
        } catch (final EntityNotFoundException e) {
            return null;
        }
    }


}
