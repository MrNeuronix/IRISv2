package ru.iris.common.messaging.model.command;

import com.google.gson.annotations.Expose;
import ru.iris.common.messaging.model.Advertisement;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 19.11.13
 * Time: 11:34
 * License: GPL v3
 */
public class CommandAdvertisement extends Advertisement {
    /**
     * Command
     */
    @Expose
    private String data;

    @Expose
    private String script;

    public CommandAdvertisement set(String script, String data) {
        this.data = data;
        this.script = script;
        return this;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    @Override
    public String toString() {
        return "CommandAdvertisement{" +
                "data='" + data + '\'' +
                ", script='" + script + '\'' +
                '}';
    }
}
