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
package ru.iris.common.messaging.model;

import java.util.Arrays;
import java.util.UUID;

/**
 * Service advertisement providers information about a service.
 * @author Tommi S.E. Laukkanen
 */
public class ServiceAdvertisement {
    /** The service type for example "My custom speak service 1.0" */
    private String type;
    /** The instance ID. */
    private UUID instanceId;
    /** The service status. */
    private ServiceStatus status;
    /** The service capabilities. */
    private ServiceCapability[] capabilities;

    /**
     * Constructor for initializing value object fields.
     * @param type the service type
     * @param instanceId the instance ID
     * @param status the service status
     * @param capabilities the service capabilities
     */
    public ServiceAdvertisement(String type, UUID instanceId, ServiceStatus status, ServiceCapability[] capabilities) {
        this.type = type;
        this.instanceId = instanceId;
        this.status = status;
        this.capabilities = capabilities;
    }

    /**
     * Default constructor for de-serialisation.
     */
    public ServiceAdvertisement() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UUID getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(UUID instanceId) {
        this.instanceId = instanceId;
    }

    public ServiceStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceStatus status) {
        this.status = status;
    }

    public ServiceCapability[] getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(ServiceCapability[] capabilities) {
        this.capabilities = capabilities;
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
                "type='" + type + '\'' +
                ", instanceId=" + instanceId +
                ", status=" + status +
                ", capabilities=" + Arrays.toString(capabilities) +
                '}';
    }
}
