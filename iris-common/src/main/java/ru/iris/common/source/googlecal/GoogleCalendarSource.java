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

package ru.iris.common.source.googlecal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by nikolay.viguro on 10.10.2014.
 */
public class GoogleCalendarSource
{
	private static final Logger LOGGER = LogManager.getLogger(GoogleCalendarSource.class);
	private static final Gson gson = new GsonBuilder().create();

	/**
	 * Singleton (On Demand Holder)
	 */
	public static class SingletonHolder
	{
		public static final GoogleCalendarSource HOLDER_INSTANCE = new GoogleCalendarSource();
	}

	public static GoogleCalendarSource getInstance()
	{
		return SingletonHolder.HOLDER_INSTANCE;
	}

	private GoogleCalendarSource()
	{
	}

	public void populateCalendar(String obj)
	{
		// TODO
	}
}
