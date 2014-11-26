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

package ru.iris.common.database;

import com.avaje.ebean.Ebean;
import ru.iris.common.database.model.ScriptLock;

import java.sql.Timestamp;
import java.util.Calendar;

/**
 * Created by nikolay.viguro on 10.10.2014.
 */
public class Lock
{
	private Timestamp startlock;
	private Timestamp endlock;
	private String title;

	public Lock(String title)
	{
		this.title = title;

		// set lock release in year+1 )
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, (calendar.get(Calendar.YEAR) + 1));
		this.endlock = new Timestamp(calendar.getTime().getTime());
	}

	public Lock(String title, Timestamp end)
	{
		this.title = title;
		this.endlock = end;
	}

	public static boolean isLocked(String title)
	{
		return Ebean.find(ScriptLock.class).where().eq("title", title).findUnique() != null;
	}

	public static void release(String title)
	{
		Ebean.delete(Ebean.find(ScriptLock.class).where().eq("title", title).findUnique());
	}

	public void lock()
	{
		ScriptLock lock = new ScriptLock();

		lock.setTitle(title);
		lock.setStartlock(startlock);
		lock.setEndlock(endlock);

		lock.save();
	}
}
