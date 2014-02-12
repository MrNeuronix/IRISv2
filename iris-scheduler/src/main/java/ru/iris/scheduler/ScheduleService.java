package ru.iris.scheduler;

/**
 * IRIS-X Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 10.09.12
 * Time: 13:26
 */

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.database.model.Task;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.ServiceCheckEmitter;
import ru.iris.common.messaging.model.command.CommandAdvertisement;
import ru.iris.common.messaging.model.service.ServiceStatus;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ScheduleService implements Runnable {

    private Thread t = null;
    private Logger log = LogManager.getLogger(ScheduleService.class);
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

            List<Task> tasks = Ebean.find(Task.class)
                    .where().and(Expr.lt("taskdate", new Timestamp(System.currentTimeMillis())), Expr.eq("enabled", true)).findList();

            messaging = new JsonMessaging(UUID.randomUUID());

            for (Task task : tasks)
            {

                if (task.getType() == 1) {
                    log.info("Actualizing task. Next run: " + task.nextRun());
                    task.setTaskdate(task.nextRun());
                    Ebean.update(task);
                } else if (task.getType() == 3) {
                    if (task.getValidto().before(task.nextRun())) {
                        log.info("Actualizing task. Set task to disable");
                        task.setEnabled(false);
                    } else {
                        log.info("Actualizing task. Next run: " + task.nextRun());
                        task.setTaskdate(task.nextRun());
                    }
                    Ebean.update(task);
                } else {
                    log.info("Skip task");
                }
            }

        } catch (Exception e) {
            log.info("Error while actualizing task");
            e.printStackTrace();
        }

        // Запускаем выполнение тасков
        serviceCheckEmitter.setState(ServiceStatus.AVAILABLE);

        while (true) {
            try {

                List<Task> tasks = Ebean.find(Task.class)
                        .where().eq("enabled", true).findList();

                for (Task task : tasks) {

                    if (new Timestamp(new Date().getTime()).equals(task.getTaskdate())) {
                        log.info("Executing task " + task.getId() + " (class: " + task.getEclass() + ", command: " + task.getCommand() + ")");

                        messaging.broadcast("event.command", commandAdvertisement.set(task.getEclass(), task.getCommand()));

                        if (task.getType() == 1) {
                            log.info("Next run: " + task.nextRun());
                            task.setTaskdate(task.nextRun());
                            Ebean.update(task);
                        } else if (task.getType() == 2) {
                            log.info("Set task to disable");
                            task.setEnabled(false);
                            Ebean.update(task);
                        } else {
                            if (task.getValidto().before(task.nextRun())) {
                                log.info("Set task to disable");
                                task.setEnabled(false);
                                Ebean.update(task);
                            } else {
                                log.info("Next run: " + task.nextRun());
                                task.setTaskdate(task.nextRun());
                                Ebean.update(task);
                            }
                        }
                    }
                }

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