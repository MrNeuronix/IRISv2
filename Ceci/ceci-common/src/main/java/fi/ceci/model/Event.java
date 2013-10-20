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
 * Event.
 *
 * @author Tommi S.E. Laukkanen
 */
@Entity
@Table(name = "event")
public final class Event implements Serializable {
    /** Java serialization version UID. */
    private static final long serialVersionUID = 1L;

    /** Unique UUID of the entity. */
    @Id
    @GeneratedValue(generator = "uuid")
    private String eventId;

    /** Owning company. */
    @JoinColumn(nullable = false)
    @ManyToOne(cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH }, optional = false)
    private Company owner;

    /** Content. */
    @Column(length = 1024, nullable = false)
    private String content;

    /** True if processing error occurred. */
    @Column(nullable = true)
    private boolean processingError = false;

    /** Processed time of the event. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    private Date processed;

    /** Created time of the event. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date created;

    /** Created time of the event. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date modified;

    /**
     * The default constructor for JPA.
     */
    public Event() {
        super();
    }

    /**
     * @param owner the owning company
     * @param content the content
     * @param created the create time stamp
     */
    public Event(final Company owner, final String content, final Date created) {
        this.owner = owner;
        this.content = content;
        this.created = created;
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
     * @return the eventId
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * @param eventId the eventId to set
     */
    public void setEventId(final String eventId) {
        this.eventId = eventId;
    }

    /**
     * @return the content
     */
    public String getContent() {
        return content;
    }

    /**
     * @param content the content
     */
    public void setContent(final String content) {
        this.content = content;
    }

    /**
     * @return true if error in processing occurred
     */
    public boolean isProcessingError() {
        return processingError;
    }

    /**
     * @param processingError the processingError to set
     */
    public void setProcessingError(final boolean processingError) {
        this.processingError = processingError;
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

    /**
     * @return the processed
     */
    public Date getProcessed() {
        return processed;
    }

    /**
     * @param processed the processed to set
     */
    public void setProcessed(final Date processed) {
        this.processed = processed;
    }

    @Override
    public String toString() {
        return content;
    }

    @Override
    public int hashCode() {
        return eventId.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof Event && eventId.equals(((Event) obj).getEventId());
    }

}
