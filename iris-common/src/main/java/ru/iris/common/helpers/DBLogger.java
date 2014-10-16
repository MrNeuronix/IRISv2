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

package ru.iris.common.helpers;

import ru.iris.common.database.model.Log;

/**
 * Created by nikolay.viguro on 16.10.2014.
 */
public class DBLogger
{
	public static void debug(String message, String uuid)
	{
		Log log = new Log("DEBUG", message, uuid);
		log.save();
	}

	public static void info(String message, String uuid)
	{
		Log log = new Log("INFO", message, uuid);
		log.save();
	}

	public static void warn(String message, String uuid)
	{
		Log log = new Log("WARN", message, uuid);
		log.save();
	}

	public static void error(String message, String uuid)
	{
		Log log = new Log("ERROR", message, uuid);
		log.save();
	}

	public static void debug(String message)
	{
		Log log = new Log("DEBUG", message, "system");
		log.save();
	}

	public static void info(String message)
	{
		Log log = new Log("INFO", message, "system");
		log.save();
	}

	public static void warn(String message)
	{
		Log log = new Log("WARN", message, "system");
		log.save();
	}

	public static void error(String message)
	{
		Log log = new Log("ERROR", message, "system");
		log.save();
	}
}
