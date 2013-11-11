package ru.iris.video;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 26.10.13
 * Time: 20:41
 * License: GPL v3
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VideoService implements Runnable {

    private Thread t = null;
    private static Logger log = LoggerFactory.getLogger(VideoService.class);
    //private static I18N i18n = new I18N();


    public VideoService() {
        this.t = new Thread(this);
        this.t.start();
    }

    public Thread getThread() {
        return this.t;
    }

    public synchronized void run() {
    }
}
