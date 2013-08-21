package ru.phsystems.irisv2.webservice;

import ru.phsystems.irisv2.common.Base64Coder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 05.12.12
 * Time: 21:32
 * License: GPL v3
 */

public class PagesContext {

    // Тут вроде должны обрабатываться данные для страниц
    public HashMap getContext(String url) throws IOException, FileNotFoundException {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map = (HashMap<String, Object>) Service.config.clone();

        // Главная страница
        if (url.equals("index")) {
            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss MM/dd/yyyy ");
            Date date = new Date();
            long uptime = System.currentTimeMillis() - Service.startTime;

            DateFormat formatter = new SimpleDateFormat("dd дней hh:mm:ss");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());

            map.put("irisUptime", String.format("%d мин, %d сек", TimeUnit.MILLISECONDS.toMinutes(uptime), TimeUnit.MILLISECONDS.toSeconds(uptime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(uptime))));
            map.put("serverUptime", String.valueOf(formatter.format(calendar.getTime())));
            map.put("memoryTotal", String.valueOf((Runtime.getRuntime().totalMemory()) / (1024 * 1024)));
            map.put("memoryFree", String.valueOf(((Runtime.getRuntime().totalMemory()) / (1024 * 1024)) - (Runtime.getRuntime().freeMemory()) / (1024 * 1024)));
            map.put("date", dateFormat.format(date));

            map.put("wwwState", true);
            map.put("zwaveState", true);
            map.put("sheduleState", true);
            map.put("captureState", true);
        }

        // Камеры
        else if (url.equals("cams")) {
            String authorization = String.valueOf(Base64Coder.encode((Service.config.get("httpUser") + ":" + Service.config.get("httpPassword")).getBytes("8859_1")));

            map.put("auth", authorization);
        }

        // Планировщик
        else if (url.equals("schedule")) {


            map.put("devicesList", "");
        }

        // Возвращаем значения
        return map;
    }

}
