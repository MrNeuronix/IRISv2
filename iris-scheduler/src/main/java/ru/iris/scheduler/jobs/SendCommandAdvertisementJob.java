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

package ru.iris.scheduler.jobs;

/**
 * Created by nikolay.viguro on 17.10.2014.
 */

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.command.CommandAdvertisement;

import java.util.UUID;

public class SendCommandAdvertisementJob implements Job
{

	private final Logger LOGGER = LogManager.getLogger(SendCommandAdvertisementJob.class);
	private final Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

	@Override
	public void execute(JobExecutionContext param) throws JobExecutionException
	{
		JobDataMap data = param.getJobDetail().getJobDataMap();

		String subject = data.getString("subject");
		String obj = data.getString("obj");

		LOGGER.debug("Run task with subject: " + subject);

		CommandAdvertisement advertisement = gson.fromJson(obj, CommandAdvertisement.class);
		new JsonMessaging(UUID.randomUUID()).broadcast(subject, advertisement);
	}

}