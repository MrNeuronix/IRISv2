package ru.iris.ai;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 26.10.13
 * Time: 20:41
 * License: GPL v3
 */

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AIService implements Runnable {

    private Thread t = null;
    private Logger log = LogManager.getLogger(AIService.class);

    public AIService() {
        this.t = new Thread(this);
        this.t.start();
    }

    public Thread getThread() {
        return this.t;
    }

    public synchronized void run() {

        log.info("HELLO FROM AI PLUGIN");
    }
}
