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

import com.github.sarxos.webcam.WebcamUtils;
import com.github.sarxos.webcam.ds.ipcam.IpCamMode;
import com.github.sarxos.webcam.log.WebcamLogConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.ds.ipcam.IpCamDeviceRegistry;
import com.github.sarxos.webcam.ds.ipcam.IpCamDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class VideoService implements Runnable {

    private Thread t = null;
    private static Logger log = LoggerFactory.getLogger (VideoService.class);
    //private static I18N i18n = new I18N();

    static {
        Webcam.setDriver(new IpCamDriver());
    }

    public VideoService() {
        this.t = new Thread(this);
        this.t.start();
    }

    public Thread getThread() {
        return this.t;
    }

    public synchronized void run() {

        //WebcamLogConfigurator.configure("logback.xml");

        try {
            IpCamDeviceRegistry.register("Testcam", new URL("http://192.168.10.20/video.cgi"), IpCamMode.PUSH);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        List<Webcam> webc = null;

        try {
            webc = Webcam.getWebcams(5000);
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

        System.out.println("SIZE: "+webc.size());
        System.out.println("NAME: "+webc.get(0).getName());
        System.out.println("ISOPEN: "+webc.get(0).isOpen());
        webc.get(0).open();
        System.out.println("ISOPEN2: "+webc.get(0).isOpen());

        WebcamUtils.capture(webc.get(0), "test1", "jpg");
    }
}
