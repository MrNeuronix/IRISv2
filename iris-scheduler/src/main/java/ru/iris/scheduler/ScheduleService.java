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

import com.avaje.ebean.Ebean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import ru.iris.common.database.model.DataSource;
import ru.iris.common.database.model.Task;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.tasks.TaskChangesAdvertisement;
import ru.iris.common.messaging.model.tasks.TaskSourcesChangesAdvertisement;
import ru.iris.common.messaging.model.tasks.TasksStartAdvertisement;
import ru.iris.common.messaging.model.tasks.TasksStopAdvertisement;
import ru.iris.common.source.googlecal.GoogleCalendarSource;
import ru.iris.common.source.vk.VKSource;
import ru.iris.scheduler.jobs.SendCommandAdvertisementJob;

import java.util.List;
import java.util.UUID;

public class ScheduleService implements Runnable
{
	private final Logger LOGGER = LogManager.getLogger(ScheduleService.class);
	private List<Task> events = Ebean.find(Task.class).where().eq("enabled", true).findList();
	private List<DataSource> sources = Ebean.find(DataSource.class).where().eq("enabled", true).findList();
	private Thread t = null;
	// Creating scheduler factory and scheduler
	private SchedulerFactory factory = new StdSchedulerFactory();
	private Scheduler scheduler = null;
	private boolean shutdown = false;

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
		try
		{
			scheduler = factory.getScheduler();

			// run
			readSources();
			readAndScheduleTasks();

			final JsonMessaging jsonMessaging = new JsonMessaging(UUID.randomUUID(), "events");
			jsonMessaging.subscribe("event.scheduler.reload.tasks");
			jsonMessaging.subscribe("event.scheduler.reload.sources");
			jsonMessaging.subscribe("event.scheduler.stop");
			jsonMessaging.subscribe("event.scheduler.start");
			jsonMessaging.start();

			while (!shutdown)
			{
				JsonEnvelope envelope = jsonMessaging.receive(100);

				if (envelope != null)
				{
					if (envelope.getObject() instanceof TasksStopAdvertisement
							|| envelope.getObject() instanceof TasksStartAdvertisement)
					{
						LOGGER.info("Start/stop scheduler service!");

						// take pause to save/remove new entity
						Thread.sleep(1000);

						// reload events
						events = null;
						events = Ebean.find(Task.class).findList();

						// take pause to save/remove new entity
						Thread.sleep(1000);

						readAndScheduleTasks();

						LOGGER.info("Loaded " + events.size() + " tasks.");
					}
					else if (envelope.getObject() instanceof TaskChangesAdvertisement)
					{
						LOGGER.info("Reload tasks list");

						// take pause to save/remove new entity
						Thread.sleep(1000);

						// reload events
						events = null;
						events = Ebean.find(Task.class).findList();

						// take pause to save/remove new entity
						Thread.sleep(1000);

						readAndScheduleTasks();

						LOGGER.info("Loaded " + events.size() + " tasks.");

					}
					else if (envelope.getObject() instanceof TaskSourcesChangesAdvertisement)
					{
						LOGGER.info("Reload sources list");

						// take pause to save/remove new entity
						Thread.sleep(1000);

						// reload sources
						events = null;
						sources = Ebean.find(DataSource.class).where().eq("enabled", true).findList();

						// take pause to save/remove new entity
						Thread.sleep(1000);

						readSources();

						LOGGER.info("Loaded " + events.size() + " sources.");

					}
				}
			}

			// Close JSON messaging.
			jsonMessaging.close();

		}
		catch (final Throwable t)
		{
			t.printStackTrace();
			LOGGER.error("Unexpected exception in Events", t);
		}
	}

	private void readSources()
	{
		for (DataSource source : sources)
		{
			// google calendar
			if (source.getType().equals("google-cal"))
			{
				GoogleCalendarSource.getInstance().populateCalendar(source.getObj());
			}
			// VK.com
			else if (source.getType().equals("vk"))
			{
				VKSource.getInstance().populateBirthDayCalendar(source.getObj());
			}
			else
			{
				LOGGER.info("Unknown data source: " + source.getType() + "!");
			}
		}
	}

	private void readAndScheduleTasks()
	{
		try
		{
			//Start scheduler
			if (!scheduler.isStarted())
			{
				LOGGER.info("Scheduling tasks!");
				scheduler.start();
			}
			else
			{
				LOGGER.info("Rescheduling tasks!");
				// restart
				scheduler.shutdown();
				scheduler.start();
			}

			for (Task event : events)
			{
				JobDetailImpl jobDetail = new JobDetailImpl();
				jobDetail.setName("Job with subject " + event.getSubject());
				jobDetail.setJobClass(SendCommandAdvertisementJob.class);

				jobDetail.getJobDataMap().put("subject", event.getSubject());
				jobDetail.getJobDataMap().put("obj", event.getObj());

				//Creating schedule time with trigger
				SimpleTriggerImpl trigger = new SimpleTriggerImpl();
				trigger.setStartTime(event.getStartdate());
				trigger.setEndTime(event.getEnddate());

				if (!event.getPeriod().isEmpty())
					trigger.setRepeatInterval(Integer.valueOf(event.getPeriod()));

				trigger.setName("trigger");

				scheduler.scheduleJob(jobDetail, trigger);
			}
		}
		catch (SchedulerException e)
		{
			LOGGER.error("Scheduler error: ", e);
		}
	}
}