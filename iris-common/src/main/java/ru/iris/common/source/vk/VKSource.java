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

package ru.iris.common.source.vk;

import com.avaje.ebean.Ebean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.database.model.Task;
import ru.iris.common.datasource.model.VKModel;
import ru.iris.common.messaging.model.command.CommandAdvertisement;
import ru.iris.common.source.vk.entities.User;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by nikolay.viguro on 10.10.2014.
 */
public class VKSource
{
	private static final Logger LOGGER = LogManager.getLogger(VKSource.class);
	private static final Gson gson = new GsonBuilder().create();

	/**
	 * Singleton (On Demand Holder)
	 */
	public static class SingletonHolder
	{
		public static final VKSource HOLDER_INSTANCE = new VKSource();
	}

	public static VKSource getInstance()
	{
		return SingletonHolder.HOLDER_INSTANCE;
	}

	private VKSource()
	{
	}

	public void populateBirthDayCalendar(String obj)
	{
		LOGGER.info("Populate calendar events from VK source");

		try
		{
			Calendar cal = Calendar.getInstance();

			VKModel source = gson.fromJson(obj, VKModel.class);

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

			User me = vkConnector.getUsers(null, "nom", vkTokenProvider.getToken()).get(0);

			List<User> users = vkConnector.getUsers(vkConnector.getFriends(me, vkTokenProvider.getToken()), "nom", vkTokenProvider.getToken());
			for (User user : users)
			{
				LOGGER.debug("User: " + user.getFirstName() + " " + user.getLastName() + " BirthDay: " + user.getBdate());

				String bdate = user.getBdate();

				// оторвать яйца тем кто придумал в таком виде отдавать даты!!!
				if (bdate != null)
				{
					if (!bdate.matches("\\d+\\.\\d+\\.\\d+"))
					{
						bdate += "." + Calendar.getInstance().get(Calendar.YEAR);
					}

					SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
					dateFormat.setLenient(true);
					cal.setTime(dateFormat.parse(bdate));

					cal.set(Calendar.YEAR, 2014);
					cal.set(Calendar.HOUR_OF_DAY, 10);
					cal.set(Calendar.MINUTE, 0);

					Task saved = Ebean.find(Task.class).where().eq("title", "День рождения " + user.getFirstName() + " " + user.getLastName()).findUnique();

					if (saved == null)
					{
						Task task = new Task();
						task.setStartdate(new Timestamp(cal.getTime().getTime()));

						cal.set(Calendar.HOUR, 10);

						Map<String, String> map = new HashMap<>();
						map.put("text", "Сегодня празднует день рождения ваш друг - " + user.getFirstName() + " " + user.getLastName());

						CommandAdvertisement adv = new CommandAdvertisement();
						adv.setData(map);
						adv.setScript("vk-birthday-say.js");

						task.setEnddate(new Timestamp(cal.getTime().getTime()));
						task.setTitle("День рождения " + user.getFirstName() + " " + user.getLastName());
						task.setText("Сегодня празднует день рождения ваш друг - " + user.getFirstName() + " " + user.getLastName());
						task.setSource("vk");
						task.setObj(gson.toJson(adv));
						task.setSubject("event.command");

						// every hour
						task.setPeriod("" + 60L * 60L * 1000L);

						task.setShowInCalendar(true);
						task.setEnabled(true);

						task.save();
					}
					else
					{
						saved.setStartdate(new Timestamp(cal.getTime().getTime()));
						cal.set(Calendar.HOUR, 10);
						saved.setEnddate(new Timestamp(cal.getTime().getTime()));

						saved.save();
					}
				}
			}

			LOGGER.info("Populate calendar events from VK source - done! Parsed " + users.size() + " events");
		}
		catch (Exception e)
		{
			LOGGER.error("VK error: ", e);
		}

		// Manually run GC
		System.gc();
	}
}
