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

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Model;
import com.google.gson.annotations.Expose;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * Created by nikolay.viguro on 16.10.2014.
 */
@MappedSuperclass
public class DBModel extends Model
{
	@Id
	@GeneratedValue
	@Expose
	public Long id;

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	@Override
	public synchronized void save()
	{
		if (this.getId() == null)
		{
			Ebean.save(this);
		}
		else
		{
			Ebean.update(this);
		}
	}

	@Override
	public synchronized void delete()
	{
		if (this.getId() == null)
		{
			Ebean.delete(this);
		}
	}

}
