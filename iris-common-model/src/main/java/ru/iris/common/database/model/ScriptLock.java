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

/**
 * Created by nikolay.viguro on 10.10.2014.
 */
@Entity
@Table(name = "scriptlock")
public class ScriptLock
{

	@Id
	private long id;

	// Время начала
	@Column(columnDefinition = "timestamp")
	private Timestamp startlock;

	// Время конца
	@Column(columnDefinition = "timestamp")
	private Timestamp endlock;

	// Заголовок
	private String title;

	public ScriptLock()
	{
	}

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public Timestamp getStartlock()
	{
		return startlock;
	}

	public void setStartlock(Timestamp startlock)
	{
		this.startlock = startlock;
	}

	public Timestamp getEndlock()
	{
		return endlock;
	}

	public void setEndlock(Timestamp endlock)
	{
		this.endlock = endlock;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}
}
