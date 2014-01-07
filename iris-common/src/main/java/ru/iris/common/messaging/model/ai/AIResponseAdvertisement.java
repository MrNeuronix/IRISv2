package ru.iris.common.messaging.model.ai;

import com.google.gson.annotations.Expose;
import ru.iris.common.ai.WitAiResponse;
import ru.iris.common.messaging.model.Advertisement;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 07.01.14
 * Time: 14:42
 * License: GPL v3
 */
public class AIResponseAdvertisement extends Advertisement {
    /**
     * Confidence
     */
    @Expose
    private WitAiResponse response;

    public AIResponseAdvertisement set(WitAiResponse response) {
        this.response = response;
        return this;
    }

    public WitAiResponse getResponse() {
        return response;
    }

    public void setResponse(WitAiResponse response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return "AIResponseAdvertisement{" +
                "response=" + response +
                '}';
    }
}
