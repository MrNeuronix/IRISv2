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
 * Bus.
 *
 * @author Tommi S.E. Laukkanen
 */
@Entity
@Table(name = "bus")
public final class Bus implements Serializable {
    /** Java serialization version UID. */
    private static final long serialVersionUID = 1L;

    /** Unique UUID of the entity. */
    @Id
    @GeneratedValue(generator = "uuid")
    private String busId;

    /** Owning company. */
    @JoinColumn(nullable = false)
    @ManyToOne(cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH }, optional = false)
    private Company owner;

    /** Name. */
    @Column(nullable = false)
    private String name;

    /** Host. */
    @Column(nullable = true)
    private String host;

    /** Port. */
    @Column(nullable = true)
    private Integer port;

    /** User name. */
    @Column(nullable = true)
    private String userName;

    /** User password. */
    @Column(nullable = true)
    private String userPassword;


    /** Connection Status. */
    @Column(nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private BusConnectionStatus connectionStatus;

    /** Inventory synchronized time of the task. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date inventorySynchronized;

    /** Created time of the task. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date created;

    /** Created time of the task. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date modified;

    /**
     * The default constructor for JPA.
     */
    public Bus() {
        super();
    }

    /**
     * @param owner the owning company
     * @param name the name
     * @param host the host
     * @param port the port
     * @param userName the user name
     * @param userPassword the user password
     * @param created the create time stamp
     */
    public Bus(final Company owner, final String name, final String host, final Integer port, final String userName
            , final String userPassword, final Date created) {
        this.owner = owner;
        this.name = name;
        this.host = host;
        this.port = port;
        this.userName = userName;
        this.userPassword = userPassword;
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
     * @return the busId
     */
    public String getBusId() {
        return busId;
    }

    /**
     * @param busId the busId to set
     */
    public void setBusId(final String busId) {
        this.busId = busId;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    public void setHost(final String host) {
        this.host = host;
    }

    /**
     * @return the port
     */
    public Integer getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(final Integer port) {
        this.port = port;
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @param userName the user name to set
     */
    public void setUserName(final String userName) {
        this.userName = userName;
    }

    /**
     * @return the userPassword
     */
    public String getUserPassword() {
        return userPassword;
    }

    /**
     * @param userPassword the userPassword to set
     */
    public void setUserPassword(final String userPassword) {
        this.userPassword = userPassword;
    }

    /**
     * @return the connectionStatus
     */
    public BusConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    /**
     * @param connectionStatus the connectionStatus to set
     */
    public void setConnectionStatus(final BusConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
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
     * @return the inventorySynchronized
     */
    public Date getInventorySynchronized() {
        return inventorySynchronized;
    }

    /**
     * @param inventorySynchronized the inventorySynchronized to set
     */
    public void setInventorySynchronized(final Date inventorySynchronized) {
        this.inventorySynchronized = inventorySynchronized;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return busId.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof Bus && busId.equals(((Bus) obj).getBusId());
    }

}
