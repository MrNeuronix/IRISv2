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
package fi.ceci.model;

import org.vaadin.addons.sitekit.model.Company;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * RecordSet.
 *
 * @author Tommi S.E. Laukkanen
 */
@Entity
@Table(name = "recordset")
public final class RecordSet implements Serializable {
    /** Java serialization version UID. */
    private static final long serialVersionUID = 1L;

    /** Unique UUID of the entity. */
    @Id
    @GeneratedValue(generator = "uuid")
    private String recordSetId;

    /** Owning company. */
    @JoinColumn(nullable = false)
    @ManyToOne(cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH }, optional = false)
    private Company owner;

    /** Element. */
    @JoinColumn(nullable = false)
    @ManyToOne(cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH }, optional = false)
    private Element element;

    /** Type. */
    @Column(nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private RecordType type;

    /** Content. */
    @Column(nullable = false)
    private String name;

    /** Unit. */
    @Column(nullable = false)
    private String unit;

    /** Created time of the recordSet. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date created;

    /** Created time of the recordSet. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date modified;

    /**
     * The default constructor for JPA.
     */
    public RecordSet() {
        super();
    }

    /**
     * @param owner the owning company
     * @param element the source element
     * @param name the name
     * @param type the content
     * @param unit unit
     * @param created the create time stamp
     */
    public RecordSet(final Company owner, final Element element, final String name,
                     final RecordType type, final String unit,
                     final Date created) {
        this.owner = owner;
        this.element = element;
        this.name = name;
        this.type = type;
        this.unit = unit;
        this.created = created;
        this.modified = created;
    }

    /**
     * @return the owner
     */
    public Company getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(final Company owner) {
        this.owner = owner;
    }

    /**
     * @return the recordSetId
     */
    public String getRecordSetId() {
        return recordSetId;
    }

    /**
     * @param recordSetId the recordSetId to set
     */
    public void setRecordSetId(final String recordSetId) {
        this.recordSetId = recordSetId;
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the recordSet name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the element
     */
    public Element getElement() {
        return element;
    }

    /**
     * @param element the element to set
     */
    public void setElement(final Element element) {
        this.element = element;
    }

    /**
     * @return the type
     */
    public RecordType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(final RecordType type) {
        this.type = type;
    }

    /**
     * @return the unit
     */
    public String getUnit() {
        return unit;
    }

    /**
     * @param unit the unit to set
     */
    public void setUnit(final String unit) {
        this.unit = unit;
    }

    /**
     * @return the created
     */
    public Date getCreated() {
        return created;
    }

    /**
     * @param created the created to set
     */
    public void setCreated(final Date created) {
        this.created = created;
    }

    /**
     * @return the modified
     */
    public Date getModified() {
        return modified;
    }

    /**
     * @param modified the modified to set
     */
    public void setModified(final Date modified) {
        this.modified = modified;
    }

    @Override
    public String toString() {
        return element.toString() + " > " + type + (unit.length() > 0 ?  " [" + unit + "]" : "")+
                (type == RecordType.OTHER ? " (" + name + ")" : "");
    }

    @Override
    public int hashCode() {
        return recordSetId.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof RecordSet && recordSetId.equals(((RecordSet) obj).getRecordSetId());
    }

}
