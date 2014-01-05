package ru.iris.ai.witai;

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

    private String msg_id;
    private String msg_body;
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

        private String intent;
        private HashMap<String, Entity> entities;
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

        private Integer start;
        private Integer end;
        private String value;
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
