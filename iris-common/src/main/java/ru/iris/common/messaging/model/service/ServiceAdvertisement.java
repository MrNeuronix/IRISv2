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
package ru.iris.common.messaging.model.service;

import com.google.gson.annotations.Expose;

import java.util.UUID;

/**
 * Service advertisement providers information about a service.
 *
 * @author Tommi S.E. Laukkanen
 */
public class ServiceAdvertisement {
    /**
     * The service name for example "My custom speak service 1.0"
     */
    @Expose
    private String name;
    /**
     * The instance ID.
     */
    @Expose
    private UUID instanceId;
    /**
     * The service status.
     */
    @Expose
    private ServiceStatus status;

    public ServiceAdvertisement set(String name, UUID instanceId, ServiceStatus status) {
        this.name = name;
        this.instanceId = instanceId;
        this.status = status;

        return this;
    }

    /**
     * Default constructor for de-serialisation.
     */
    public ServiceAdvertisement() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ServiceStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceStatus status) {
        this.status = status;
    }

    public UUID getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(UUID instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceAdvertisement that = (ServiceAdvertisement) o;

        if (instanceId != null ? !instanceId.equals(that.instanceId) : that.instanceId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return instanceId != null ? instanceId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ServiceAdvertisement{" +
                "type='" + name + '\'' +
                ", instanceId=" + instanceId +
                ", status=" + status +
                '}';
    }
}
