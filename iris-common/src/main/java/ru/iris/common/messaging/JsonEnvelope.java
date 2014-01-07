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

/**
 * Value object to contain JSON messaging envelope information.
 *
 * @author Tommi S.E. Laukkanen
 */
public class JsonEnvelope {
    /**
     * The sender instance ID.
     */
    private String senderInstance;
    /**
     * The receiver instance ID.
     */
    private String receiverInstance;
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

    public JsonEnvelope(String senderInstance, String receiverInstance, String subject, Object object) {
        this.senderInstance = senderInstance;
        this.receiverInstance = receiverInstance;
        this.subject = subject;
        this.object = object;
    }

    public String getSubject() {
        return subject;
    }

    public <T> T getObject() {
        return (T) object;
    }

    public String getSenderInstance() {
        return senderInstance;
    }

    public String getReceiverInstance() {
        return receiverInstance;
    }

    @Override
    public String toString() {
        return "Envelope{" +
                "senderInstance=" + senderInstance +
                ", receiverInstance=" + receiverInstance +
                ", subject='" + subject + '\'' +
                ", object=" + object +
                '}';
    }
}