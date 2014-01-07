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
public class CommandResult extends Advertisement {
    /**
     * Command
     */
    @Expose
    private String command;
    @Expose
    private Object result;

    public CommandResult set(String command, Object result) {
        this.command = command;
        this.result = result;
        return this;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "CommandResultAdvertisement { command: " + command + ", object: " + result + " }";
    }
}
