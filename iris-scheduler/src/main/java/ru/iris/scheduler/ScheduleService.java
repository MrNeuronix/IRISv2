package ru.iris.scheduler;

/**
 * IRIS-X Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 10.09.12
 * Time: 13:26
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;
import ru.iris.common.SQL;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.CommandAdvertisement;
import ru.iris.common.messaging.model.ServiceAdvertisement;
import ru.iris.common.messaging.model.ServiceCapability;
import ru.iris.common.messaging.model.ServiceStatus;

import java.sql.ResultSet;
import java.util.Date;
import java.util.UUID;

public class ScheduleService implements Runnable {

    private Thread t = null;
    private Logger log = LoggerFactory.getLogger(ScheduleService.class);
    private I18N i18n = new I18N();
    private SQL sql = new SQL();
    private JsonMessaging messaging;
    private static CommandAdvertisement commandAdvertisement = new CommandAdvertisement();

    public ScheduleService() {
        this.t = new Thread(this);
        this.t.start();
    }

    public Thread getThread() {
        return this.t;
    }

    public synchronized void run() {
        log.info(i18n.message("scheduler.service.starting"));

        // Актуализируем даты запуска всех тасков

        try {

            ResultSet rsActualize = sql.select("SELECT id FROM scheduler WHERE enabled='1' AND date < NOW()");
            messaging = new JsonMessaging(UUID.randomUUID());

            while (rsActualize.next()) {
                Task task = new Task(rsActualize.getInt("id"));

                if (task.getType() == 1) {
                    log.info(i18n.message("scheduler.actualize.task.time.next.run.at.0", task.nextRunAsString()));
                    sql.doQuery("UPDATE scheduler SET date='" + task.nextRunAsString() + "' WHERE id='" + task.getId() + "'");
                } else if (task.getType() == 3) {
                    if (task.getValidto().before(task.nextRunAsDate())) {
                        log.info(i18n.message("scheduler.actualize.task.time.set.task.to.disable"));
                        sql.doQuery("UPDATE scheduler SET enabled='0' WHERE id='" + task.getId() + "'");
                    } else {
                        log.info(i18n.message("scheduler.actualize.task.time.next.run.at.01", task.nextRunAsString()));
                        sql.doQuery("UPDATE scheduler SET date='" + task.nextRunAsString() + "' WHERE id='" + task.getId() + "'");
                    }
                } else {
                    log.info(i18n.message("scheduler.actualize.task.time.skip.task"));
                }
            }

            rsActualize.close();

        } catch (Exception e) {
            log.info(i18n.message("scheduler.error.at.actualizing.tasks"));
            e.printStackTrace();
        }

        // Запускаем выполнение тасков

        Service.serviceChecker.setAdvertisment(
                Service.advertisement.set("Scheduler", Service.serviceId, ServiceStatus.AVAILABLE));

        while (true) {
            try {
                ResultSet rs = sql.select("SELECT id FROM scheduler WHERE enabled='1'");
                Date now = new Date();

                while (rs.next()) {
                    Task task = new Task(rs.getInt("id"));

                    if (task.getDateAsString(now).equals(task.getDateAsString(task.getDate()))) {
                        log.info(i18n.message("scheduler.executing.task.0.1.2", task.getId(), task.getEclass(), task.getCommand()));

                        messaging.broadcast("event.command", commandAdvertisement.set(task.getCommand()));

                        if (task.getType() == 1) {
                            log.info(i18n.message("scheduler.next.run.at.0", task.nextRunAsString()));
                            sql.doQuery("UPDATE scheduler SET date='" + task.nextRunAsString() + "' WHERE id='" + task.getId() + "'");
                        } else if (task.getType() == 2) {
                            log.info(i18n.message("scheduler.set.task.to.disable"));
                            sql.doQuery("UPDATE scheduler SET enabled='0' WHERE id='" + task.getId() + "'");
                        } else {
                            if (task.getValidto().before(task.nextRunAsDate())) {
                                log.info(i18n.message("scheduler.set.task.to.disable"));
                                sql.doQuery("UPDATE scheduler SET enabled='0' WHERE id='" + task.getId() + "'");
                            } else {
                                log.info(i18n.message("scheduler.next.run.at.0", task.nextRunAsString()));
                                sql.doQuery("UPDATE scheduler SET date='" + task.nextRunAsString() + "' WHERE id='" + task.getId() + "'");
                            }
                        }
                    }
                }

                rs.close();

            } catch (Exception e) {
                log.info(i18n.message("scheduler.no.scheduled.tasks"));
                //e.printStackTrace();
            }

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                Service.serviceChecker.setAdvertisment(
                        Service.advertisement.set("Scheduler", Service.serviceId, ServiceStatus.SHUTDOWN));
            }
        }
    }
}