/*
 * Copyright 2012-2016 Nikolay A. Viguro
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.iris.common.voice;

import com.amazonaws.auth.AWSCredentials;
import com.ivona.services.tts.IvonaSpeechCloudClient;
import com.ivona.services.tts.model.CreateSpeechRequest;
import com.ivona.services.tts.model.CreateSpeechResult;
import com.ivona.services.tts.model.Input;
import com.ivona.services.tts.model.Voice;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.Config;

import java.io.IOException;
import java.io.InputStream;

public class IvonaSynthesiser implements Synthesiser {

    final Config cfg = Config.getInstance();
    private final Logger LOGGER = LogManager.getLogger(IvonaSynthesiser.class);

    public void setLanguage(String languageCode) {
    }

    public InputStream getMP3Data(String synthText) throws IOException {

        AWSCredentials credentials = new AWSCredentials() {
            @Override
            public String getAWSAccessKeyId() {
                return cfg.get("ivonaAccessKey");
            }

            @Override
            public String getAWSSecretKey() {
                return cfg.get("ivonaSecretKey");
            }
        };

        IvonaSpeechCloudClient speechCloud = new IvonaSpeechCloudClient(credentials);
        speechCloud.setEndpoint("https://tts.eu-west-1.ivonacloud.com");

        CreateSpeechRequest createSpeechRequest = new CreateSpeechRequest();
        Input input = new Input();
        Voice voice = new Voice();

        voice.setName(cfg.get("ivonaVoice"));
        input.setData(synthText);

        createSpeechRequest.setInput(input);
        createSpeechRequest.setVoice(voice);
        InputStream in = null;

        try {

            CreateSpeechResult createSpeechResult = speechCloud.createSpeech(createSpeechRequest);

            LOGGER.debug("Success sending request:");
            LOGGER.debug(" content type:\t" + createSpeechResult.getContentType());
            LOGGER.debug(" request id:\t" + createSpeechResult.getTtsRequestId());
            LOGGER.debug(" request chars:\t" + createSpeechResult.getTtsRequestCharacters());
            LOGGER.debug(" request units:\t" + createSpeechResult.getTtsRequestUnits());

            System.out.println("\nStarting to retrieve audio stream:");

            in = createSpeechResult.getBody();

            return in;

        } finally {
            if (in != null) {
                in.close();
            }
        }


    }
}