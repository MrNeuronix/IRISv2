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
package ru.iris;

import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.iris.common.JsonMessaging;
import ru.iris.common.Messaging;

/**
 * Simple test to verify JSON serialization and deserialization.
 * @author Tommi S.E. Laukkanen
 */
public class JsonMessagingTest {

    @Test
    public void testGson() {

        Gson gson = new Gson();

        final TestKeyValue testKeyValueOriginal = new TestKeyValue("test-key", "test-value");

        final String json = gson.toJson(testKeyValueOriginal);

        final TestKeyValue testKeyValueDeserialized = gson.fromJson(json, TestKeyValue.class);

        Assert.assertEquals(testKeyValueOriginal, testKeyValueDeserialized);
    }

    @Test
    @Ignore
    public void testJsonMessage() throws Exception {
        final TestKeyValue testKeyValueOriginal = new TestKeyValue("test-key", "test-value");

        final JsonMessaging messaging = new JsonMessaging();
        messaging.subscribeJsonTopic("test");
        messaging.listenJson();
        messaging.sendJsonObject(new JsonMessaging.JsonMessage("test", testKeyValueOriginal));

        final TestKeyValue testKeyValueDeserialized = messaging.receiveJsonObject().getObject();

        Assert.assertEquals(testKeyValueOriginal, testKeyValueDeserialized);
    }

}