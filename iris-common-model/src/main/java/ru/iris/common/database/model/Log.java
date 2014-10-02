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

package ru.iris.common.database.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "log")
public class Log
{

	@Id
	private long id;

	@Column(columnDefinition = "timestamp")
	private Timestamp logdate;

	private String level;
	private String message;
	private String uuid;
	private String event;

	// Default
	public Log()
	{
	}

	public Log(String level, String message, String uuid)
	{
		this.level = level;
		this.message = message;
		this.uuid = uuid;
	}

	public Log(String level, String message, String uuid, String event)
	{
		this.level = level;
		this.message = message;
		this.uuid = uuid;
		this.event = event;
	}

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public Timestamp getLogdate()
	{
		return logdate;
	}

	public void setLogdate(Timestamp logdate)
	{
		this.logdate = logdate;
	}

	public String getLevel()
	{
		return level;
	}

	public void setLevel(String level)
	{
		this.level = level;
	}

	public String getMessage()
	{
		return message;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}

	public String getUuid()
	{
		return uuid;
	}

	public void setUuid(String uuid)
	{
		this.uuid = uuid;
	}

	public String getEvent()
	{
		return event;
	}

	public void setEvent(String event)
	{
		this.event = event;
	}

	@Override public String toString()
	{
		return "Log{" +
				"id=" + id +
				", logdate=" + logdate +
				", level='" + level + '\'' +
				", message='" + message + '\'' +
				", uuid='" + uuid + '\'' +
				", event='" + event + '\'' +
				'}';
	}
}