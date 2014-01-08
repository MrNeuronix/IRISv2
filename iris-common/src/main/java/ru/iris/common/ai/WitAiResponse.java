package ru.iris.common.ai;

import com.google.gson.annotations.Expose;

import java.util.HashMap;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 05.01.14
 * Time: 23:28
 * License: GPL v3
 */
public class WitAiResponse {

    @Expose
    private String msg_id;
    @Expose
    private String msg_body;
    @Expose
    private Outcome outcome;

    public String getMsg_id() {
        return msg_id;
    }

    public String getMsg_body() {
        return msg_body;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public class Outcome {

        @Expose
        private String intent;
        @Expose
        private HashMap<String, Entity> entities;
        @Expose
        private Double confidence;

        public HashMap<String, Entity> getEntities() {
            return entities;
        }

        public Double getConfidence() {
            return confidence;
        }

        public String getIntent() {
            return intent;
        }
    }

    public class Entity {

        @Expose
        private Integer start;
        @Expose
        private Integer end;
        @Expose
        private String value;
        @Expose
        private String body;

        public Integer getStart() {
            return start;
        }

        public String getValue() {
            return value;
        }

        public Integer getEnd() {
            return end;
        }

        public String getBody() {
            return body;
        }
    }
}
