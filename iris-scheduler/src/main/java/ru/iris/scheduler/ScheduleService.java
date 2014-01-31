package ru.iris.scheduler;

/**
 * IRIS-X Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 10.09.12
 * Time: 13:26
 */

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.SQL;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.ServiceCheckEmitter;
import ru.iris.common.messaging.model.command.CommandAdvertisement;
import ru.iris.common.messaging.model.service.ServiceStatus;

import java.sql.ResultSet;
import java.util.Date;
import java.util.UUID;

public class ScheduleService implements Runnable {

    private Thread t = null;
    private Logger log = LogManager.getLogger(ScheduleService.class);
    private SQL sql = Service.getSQL();
    private JsonMessaging messaging;
    private static CommandAdvertisement commandAdvertisement = new CommandAdvertisement();

    public ScheduleService() {
        this.t = new Thread(this);
        t.setName("Scheduler Service");
        this.t.start();
    }

    public Thread getThread() {
        return this.t;
    }

    public synchronized void run() {

        ServiceCheckEmitter serviceCheckEmitter = new ServiceCheckEmitter("Scheduler");
        serviceCheckEmitter.setState(ServiceStatus.STARTUP);

        // Актуализируем даты запуска всех тасков
        try {
            ResultSet rsActualize = sql.select("SELECT id FROM scheduler WHERE enabled='1' AND date < NOW()");
            messaging = new JsonMessaging(UUID.randomUUID());

            while (rsActualize.next()) {
                Task task = new Task(rsActualize.getInt("id"));

                if (task.getType() == 1) {
                    log.info("Actualizing task. Next run: " + task.nextRunAsString());
                    sql.doQuery("UPDATE scheduler SET date='" + task.nextRunAsString() + "' WHERE id='" + task.getId() + "'");
                } else if (task.getType() == 3) {
                    if (task.getValidto().before(task.nextRunAsDate())) {
                        log.info("Actualizing task. Set task to disable");
                        sql.doQuery("UPDATE scheduler SET enabled='0' WHERE id='" + task.getId() + "'");
                    } else {
                        log.info("Actualizing task. Next run: " + task.nextRunAsString());
                        sql.doQuery("UPDATE scheduler SET date='" + task.nextRunAsString() + "' WHERE id='" + task.getId() + "'");
                    }
                } else {
                    log.info("Skip task");
                }
            }

            rsActualize.close();

        } catch (Exception e) {
            log.info("Error while actualizing task");
            e.printStackTrace();
        }

        // Запускаем выполнение тасков
        serviceCheckEmitter.setState(ServiceStatus.AVAILABLE);

        while (true) {
            try {
                ResultSet rs = sql.select("SELECT id FROM scheduler WHERE enabled='1'");
                Date now = new Date();

                while (rs.next()) {
                    Task task = new Task(rs.getInt("id"));

                    if (task.getDateAsString(now).equals(task.getDateAsString(task.getDate()))) {
                        log.info("Executing task " + task.getId() + " (class: " + task.getEclass() + ", command: " + task.getCommand() + ")");

                        messaging.broadcast("event.command", commandAdvertisement.set(task.getEclass(), task.getCommand()));

                        if (task.getType() == 1) {
                            log.info("Next run: " + task.nextRunAsString());
                            sql.doQuery("UPDATE scheduler SET date='" + task.nextRunAsString() + "' WHERE id='" + task.getId() + "'");
                        } else if (task.getType() == 2) {
                            log.info("Set task to disable");
                            sql.doQuery("UPDATE scheduler SET enabled='0' WHERE id='" + task.getId() + "'");
                        } else {
                            if (task.getValidto().before(task.nextRunAsDate())) {
                                log.info("Set task to disable");
                                sql.doQuery("UPDATE scheduler SET enabled='0' WHERE id='" + task.getId() + "'");
                            } else {
                                log.info("Next run: " + task.nextRunAsString());
                                sql.doQuery("UPDATE scheduler SET date='" + task.nextRunAsString() + "' WHERE id='" + task.getId() + "'");
                            }
                        }
                    }
                }

                rs.close();

            } catch (Exception e) {
                log.info("No scheduled tasks");
                //e.printStackTrace();
            }

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                serviceCheckEmitter.setState(ServiceStatus.ERROR);
            }
        }
    }
}