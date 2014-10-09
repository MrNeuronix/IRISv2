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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.database.model.DataSource;
import ru.iris.common.datasource.model.GoogleCalendar;
import ru.iris.common.datasource.model.VKCalendar;
import ru.iris.common.messaging.model.command.CommandAdvertisement;
import ru.iris.common.source.vk.VKConnector;
import ru.iris.common.source.vk.VKConnectorImpl;
import ru.iris.common.source.vk.VKTokenProvider;
import ru.iris.common.source.vk.VKTokenProviderImpl;
import ru.iris.common.source.vk.entities.User;

import java.io.IOException;
import java.util.List;

class ScheduleService implements Runnable
{

	private static final CommandAdvertisement commandAdvertisement = new CommandAdvertisement();
	private final Logger LOGGER = LogManager.getLogger(ScheduleService.class);
	private final Gson gson = new GsonBuilder().create();
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
				getGoogleCalendar(source.getObj());
			}
			// VK.com calendar
			else if (source.getType().equals("vk-cal"))
			{
				getVKCalendar(source.getObj());
			}
			else
			{
				LOGGER.info("Unknown data source: " + source.getType() + "!");
			}
		}
	}

	// Google calendar
	///////////////////////////////////

	private void getGoogleCalendar(String obj)
	{
		GoogleCalendar source = gson.fromJson(obj, GoogleCalendar.class);

		// TODO
	}

	// Vkontakte calendar
	///////////////////////////////////

	private void getVKCalendar(String obj)
	{
		try
		{
			VKCalendar source = gson.fromJson(obj, VKCalendar.class);

			VKConnector vkConnector = VKConnectorImpl.createInstance();

			// token not found
			if (source.getAccesstoken().isEmpty())
			{
				String accessToken = vkConnector.getToken(source.getClientid(), source.getSecretkey(), source.getUsername(), source.getPassword());

				if (accessToken != null && !accessToken.isEmpty())
				{
					source.setAccesstoken(accessToken);
					Ebean.update(source);
				}
				else
				{
					LOGGER.error("Cant get accesstoken from VK. Use debug.");
					return;
				}
			}

			VKTokenProvider vkTokenProvider = VKTokenProviderImpl.createInstance(source.getAccesstoken());

			User me = vkConnector.getUsers(null, vkTokenProvider.getToken()).get(0);

			List<User> users = vkConnector.getUsers(null, vkTokenProvider.getToken());
			for (User user : users)
			{
				// TODO
				LOGGER.info("User: " + user.getFirstName() + " " + user.getLastName());
			}
		}
		catch (IOException e)
		{
			LOGGER.error("VK error: ", e);
		}
	}
}