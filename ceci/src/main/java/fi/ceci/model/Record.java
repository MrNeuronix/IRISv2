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
 * Record.
 *
 * @author Tommi S.E. Laukkanen
 */
@Entity
@Table(name = "record")
public final class Record implements Serializable {
    /** Java serialization version UID. */
    private static final long serialVersionUID = 1L;

    /** Unique UUID of the entity. */
    @Id
    @GeneratedValue(generator = "uuid")
    private String recordId;

    /** Owning company. */
    @JoinColumn(nullable = false)
    @ManyToOne(cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH }, optional = false)
    private Company owner;

    /** Value. */
    @Column(nullable = false)
    private double value;

    /** The record set. */
    @JoinColumn(nullable = false)
    @ManyToOne(cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH }, optional = false)
    private RecordSet recordSet;

    /** Created time of the record. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date created;

    /** Created time of the record. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date modified;

    /**
     * The default constructor for JPA.
     */
    public Record() {
        super();
    }

    /**
     * @param owner the owning company
     * @param recordSet the recordSet
     * @param value the content
     * @param created the create time stamp
     */
    public Record(final Company owner, final RecordSet recordSet,
                  final double value, final Date created) {
        this.owner = owner;
        this.recordSet = recordSet;
        this.value = value;
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
     * @return the recordId
     */
    public String getRecordId() {
        return recordId;
    }

    /**
     * @param recordId the recordId to set
     */
    public void setRecordId(final String recordId) {
        this.recordId = recordId;
    }

    /**
     * @return the recordSet
     */
    public RecordSet getRecordSet() {
        return recordSet;
    }

    /**
     * @param recordSet the recordSet to set
     */
    public void setRecordSet(final RecordSet recordSet) {
        this.recordSet = recordSet;
    }

    /**
     * @return the value
     */
    public double getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(final double value) {
        this.value = value;
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
        return created.toString() + ":" + value;
    }

    @Override
    public int hashCode() {
        return recordId.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof Record && recordId.equals(((Record) obj).getRecordId());
    }

}
