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
import ru.iris.security.IrisSecurity;

import java.util.UUID;

/**
 * Test case for testing IRIS security.
 * @author Tommi S.E. Laukkanen
 */
public class IrisSecurityTest {

    @Test
    @Ignore
    public void testIrisSecurity() {
        final UUID testInstanceId = UUID.randomUUID();
        final String keystorePath = "target/" + testInstanceId + ".jks";
        final IrisSecurity irisSecurity = new IrisSecurity(testInstanceId, keystorePath, "changeit");
        final String testMessage = "test-message";
        final String signature = irisSecurity.calculateSignature(testMessage);
        final boolean signatureValid = irisSecurity.verifySignature(testMessage, signature, testInstanceId);
        Assert.assertTrue(signatureValid);
    }

}
