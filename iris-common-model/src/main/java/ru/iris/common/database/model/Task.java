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

@Entity
@Table(name = "calendar")
public class Task
{

	@Expose
	@Id
	private int id;

	// Время начала
	@Expose
	@Column(columnDefinition = "timestamp")
	private Timestamp startdate;

	// Время конца
	@Expose
	@Column(columnDefinition = "timestamp")
	private Timestamp enddate;

	// Заголовок задачи
	@Expose
	private String title;

	// Текст задачи
	@Expose
	private String text;

	// Тип таска:
	// 1 - Однократный запуск
	// 2 - Многократный запуск от и до с интервалом
	@Expose
	private String type;

	// Адрес, куда слать (например, event.command)
	@Expose
	private String subject;

	// Тут хранится сериализованный в JSON advertisement
	@Expose
	private String obj;

	// Интервал, с которой будет запускаться задача
	@Expose
	private String period;

	// Источник данных
	@Expose
	private String source;

	// Показывать ли в календаре?
	@Expose
	@Column(name = "showInCalendar")
	private boolean showInCalendar;

	// Активна ли?
	@Expose
	private boolean enabled;

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

	public Timestamp getStartdate()
	{
		return startdate;
	}

	public void setStartdate(Timestamp startdate)
	{
		this.startdate = startdate;
	}

	public Timestamp getEnddate()
	{
		return enddate;
	}

	public void setEnddate(Timestamp enddate)
	{
		this.enddate = enddate;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getText()
	{
		return text;
	}

	public void setText(String text)
	{
		this.text = text;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String getSubject()
	{
		return subject;
	}

	public void setSubject(String subject)
	{
		this.subject = subject;
	}

	public String getObj()
	{
		return obj;
	}

	public void setObj(String object)
	{
		this.obj = object;
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	public String getPeriod()
	{
		return period;
	}

	public String getSource()
	{
		return source;
	}

	public void setSource(String source)
	{
		this.source = source;
	}

	public boolean isShowInCalendar()
	{
		return showInCalendar;
	}

	public void setShowInCalendar(boolean showInCalendar)
	{
		this.showInCalendar = showInCalendar;
	}

	public void setPeriod(String period)
	{
		this.period = period;
	}
}
