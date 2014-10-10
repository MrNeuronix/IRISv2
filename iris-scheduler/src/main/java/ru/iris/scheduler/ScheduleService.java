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
import ru.iris.common.database.model.DataSource;
import ru.iris.common.messaging.model.command.CommandAdvertisement;
import ru.iris.common.source.googlecal.GoogleCalendarSource;
import ru.iris.common.source.vk.VKSource;

import java.util.List;

class ScheduleService implements Runnable
{

	private static final CommandAdvertisement commandAdvertisement = new CommandAdvertisement();
	private final Logger LOGGER = LogManager.getLogger(ScheduleService.class);
	private Thread t = null;

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
		// Подключаем все активные источники данных
		List<DataSource> sources = Ebean.find(DataSource.class).where().eq("enabled", "1").findList();

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
}