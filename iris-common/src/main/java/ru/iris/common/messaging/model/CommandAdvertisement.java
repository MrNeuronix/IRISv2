package ru.iris.common.messaging.model;

import com.google.gson.annotations.Expose;

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
    private String command;

    @Expose
    private String klazz;

    public CommandAdvertisement set(String klazz, String command) {
        this.command = command;
        this.klazz = klazz;
        return this;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getTaskClass() {
        return klazz;
    }

    public void setTaskClass(String klazz) {
        this.klazz = klazz;
    }

    @Override
    public String toString() {
        return "CommandAdvertisement{" +
                "command='" + command + '\'' +
                ", klazz='" + klazz + '\'' +
                '}';
    }
}
