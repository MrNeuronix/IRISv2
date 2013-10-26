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
package ru.iris.common.messaging;

import javax.jms.Destination;
import java.util.UUID;

/**
 * Value object to contain JSON messaging envelope information.
 *
 * @author Tommi S.E. Laukkanen
 */
public class JsonEnvelope {
    /**
     * The sender instance ID.
     */
    private UUID senderInstanceId;
    /**
     * The receiver instance ID.
     */
    private UUID receiverInstanceId;
    /**
     * The correlation ID.
     */
    private String correlationId;
    /**
     * The reply destination.
     */
    private Destination replyDestination;
    /**
     * The subject.
     */
    private String subject;
    /**
     * The object.
     */
    private Object object;

    public JsonEnvelope(String subject, Object object) {
        this.subject = subject;
        this.object = object;
    }

    public JsonEnvelope(UUID senderInstanceId, UUID receiverInstanceId, String correlationId,
                    Destination replyDestination, String subject, Object object) {
        this.senderInstanceId = senderInstanceId;
        this.receiverInstanceId = receiverInstanceId;
        this.correlationId = correlationId;
        this.replyDestination = replyDestination;
        this.subject = subject;
        this.object = object;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Destination getReplyDestination() {
        return replyDestination;
    }

    public String getSubject() {
        return subject;
    }

    public Object getObject() {
        return object;
    }

    public UUID getSenderInstanceId() {
        return senderInstanceId;
    }

    public UUID getReceiverInstanceId() {
        return receiverInstanceId;
    }

    @Override
    public String toString() {
        return "Envelope{" +
                "senderInstanceId=" + senderInstanceId +
                ", receiverInstanceId=" + receiverInstanceId +
                ", correlationId='" + correlationId + '\'' +
                ", replyDestination=" + replyDestination +
                ", subject='" + subject + '\'' +
                ", object=" + object +
                '}';
    }
}