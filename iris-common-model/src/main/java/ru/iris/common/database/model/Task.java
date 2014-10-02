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

import com.google.gson.annotations.Expose;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * IRIS-X Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 22.10.12
 * Time: 12:59
 */

@Entity
@Table(name = "scheduler")
public class Task
{

	@Expose
	@Id
	private int id;

	@Expose
	@Column(columnDefinition = "timestamp")
	private Timestamp taskdate;

	@Expose
	@Column(name = "class")
	private String eclass;

	@Expose
	private String command;

	@Expose
	private int type;

	@Expose
	@Column(columnDefinition = "timestamp")
	private Timestamp validto;

	@Expose
	private String intervalDate;

	@Expose
	private boolean enabled;

	@Expose
	private String lang;

	public Task()
	{
	}

	public int getId()
	{
		return id;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public String getEclass()
	{
		return eclass;
	}

	public void setEclass(String eclass)
	{
		this.eclass = eclass;
	}

	public String getCommand()
	{
		return command;
	}

	public void setCommand(String command)
	{
		this.command = command;
	}

	public int getType()
	{
		return type;
	}

	public void setType(int type)
	{
		this.type = type;
	}

	public Timestamp getValidto()
	{
		return validto;
	}

	public void setValidto(Timestamp validto)
	{
		this.validto = validto;
	}

	public String getInterval()
	{
		return intervalDate;
	}

	public void setInterval(String interval)
	{
		this.intervalDate = interval;
	}

	public String getLang()
	{
		return lang;
	}

	public void setLang(String lang)
	{
		this.lang = lang;
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	public Timestamp getTaskdate()
	{
		return taskdate;
	}

	public void setTaskdate(Timestamp taskdate)
	{
		this.taskdate = taskdate;
	}

	public String getIntervalDate()
	{
		return intervalDate;
	}

	public void setIntervalDate(String intervalDate)
	{
		this.intervalDate = intervalDate;
	}
}
