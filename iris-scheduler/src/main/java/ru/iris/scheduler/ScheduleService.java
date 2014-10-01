/*
 * Copyright 2012-2014 Nikolay A. Viguro
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import ru.iris.common.messaging.model.command.CommandAdvertisement;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.UUID;

class ScheduleService implements Runnable
{

	private static final CommandAdvertisement commandAdvertisement = new CommandAdvertisement();
	private final Logger log = LogManager.getLogger(ScheduleService.class);
	private Thread t = null;
	private JsonMessaging messaging;

	public ScheduleService()
	{
		this.t = new Thread(this);
		t.setName("Scheduler Service");
		this.t.start();
	}

	public Thread getThread()
	{
		return this.t;
	}

	public synchronized void run()
	{

		// Актуализируем даты запуска всех тасков
		try
		{

			List<Task> tasks = Ebean.find(Task.class)
					.where().and(Expr.lt("taskdate", new Timestamp(System.currentTimeMillis())), Expr.eq("enabled", true)).findList();

			messaging = new JsonMessaging(UUID.randomUUID());

			for (Task task : tasks)
			{

				if (task.getType() == 1)
				{
					log.info("Actualizing task. Next run: " + task.nextRun());
					task.setTaskdate(task.nextRun());
					Ebean.update(task);
				}
				else if (task.getType() == 3)
				{
					if (task.getValidto().before(task.nextRun()))
					{
						log.info("Actualizing task. Set task to disable");
						task.setEnabled(false);
					}
					else
					{
						log.info("Actualizing task. Next run: " + task.nextRun());
						task.setTaskdate(task.nextRun());
					}
					Ebean.update(task);
				}
				else
				{
					log.info("Skip task");
				}
			}

		}
		catch (Exception e)
		{
			log.info("Error while actualizing task");
			e.printStackTrace();
		}

		//noinspection InfiniteLoopStatement
		while (true)
		{
			try
			{

				List<Task> tasks = Ebean.find(Task.class)
						.where().eq("enabled", true).findList();

				for (Task task : tasks)
				{

					if (new Timestamp(new Date().getTime()).equals(task.getTaskdate()))
					{
						log.info("Executing task " + task.getId() + " (class: " + task.getEclass() + ", command: " + task.getCommand() + ")");

						messaging.broadcast("event.command", commandAdvertisement.set(task.getEclass(), task.getCommand()));

						if (task.getType() == 1)
						{
							log.info("Next run: " + task.nextRun());
							task.setTaskdate(task.nextRun());
							Ebean.update(task);
						}
						else if (task.getType() == 2)
						{
							log.info("Set task to disable");
							task.setEnabled(false);
							Ebean.update(task);
						}
						else
						{
							if (task.getValidto().before(task.nextRun()))
							{
								log.info("Set task to disable");
								task.setEnabled(false);
								Ebean.update(task);
							}
							else
							{
								log.info("Next run: " + task.nextRun());
								task.setTaskdate(task.nextRun());
								Ebean.update(task);
							}
						}
					}
				}

			}
			catch (Exception e)
			{
				log.info("No scheduled tasks");
				//e.printStackTrace();
			}

			try
			{
				Thread.sleep(1000L);
			}
			catch (InterruptedException e)
			{
				log.error(e.toString());
			}
		}
	}
}