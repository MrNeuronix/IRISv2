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
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "speaks")
public class Speaks extends DBModel
{
	@Expose
	@Column(columnDefinition = "timestamp")
	private Timestamp speakdate;

	@Expose
	@Column(columnDefinition = "TEXT")
	private String text;

	@Expose
	private Double confidence;

	@Expose
	private String device;

	@Expose
	private long cache;

	// Default
	public Speaks()
	{
	}

	public Timestamp getSpeakdate()
	{
		return speakdate;
	}

	public void setSpeakdate(Timestamp speakdate)
	{
		this.speakdate = speakdate;
	}

	public String getText()
	{
		return text;
	}

	public void setText(String text)
	{
		this.text = text;
	}

	public Double getConfidence()
	{
		return confidence;
	}

	public void setConfidence(Double confidence)
	{
		this.confidence = confidence;
	}

	public String getDevice()
	{
		return device;
	}

	public void setDevice(String device)
	{
		this.device = device;
	}

	public long getCache()
	{
		return cache;
	}

	public void setCache(long cache)
	{
		this.cache = cache;
	}
}