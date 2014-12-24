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
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "calendar")
public class Task extends DBModel
{
	// Время начала
	@Column(columnDefinition = "timestamp")
	private Timestamp startdate;

	// Время конца
	@Column(columnDefinition = "timestamp")
	private Timestamp enddate;

	// Заголовок задачи
	private String title;

	// Текст задачи
	private String text;

	// Запускаемый скрипт
	public String script;

	// Адрес, куда слать (например, event.command)
	private String subject;

	// Тут хранится сериализованный в JSON advertisement
	private String obj;

	// Интервал, с которой будет запускаться задача
	private String period;

	// Источник данных
	private String source;

	// Класс
	private String clazz;

	// Показывать ли в календаре?
	@Column(name = "showInCalendar")
	private boolean showInCalendar;

	// Активна ли?
	private boolean enabled;

	public Task()
	{
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

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public String getText()
	{
		return text;
	}

	public void setText(String text)
	{
		this.text = text;
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

	public String getClazz() {
		return clazz;
	}

	public void setClazz(String clazz) {
		this.clazz = clazz;
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
