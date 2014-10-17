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

package ru.iris.common.messaging.model.ai;

import java.util.HashMap;

public class WitAiResponse
{
	private String msg_id;
	private String msg_body;
	private Outcome outcome;

	public String getMsg_id()
	{
		return msg_id;
	}

	public String getMsg_body()
	{
		return msg_body;
	}

	public Outcome getOutcome()
	{
		return outcome;
	}

	public class Outcome
	{
		private String intent;
		private HashMap<String, Entity> entities;
		private Double confidence;

		public HashMap<String, Entity> getEntities()
		{
			return entities;
		}

		public Double getConfidence()
		{
			return confidence;
		}

		public String getIntent()
		{
			return intent;
		}
	}

	public class Entity
	{
		private Integer start;
		private Integer end;
		private String value;
		private String body;

		public Integer getStart()
		{
			return start;
		}

		public String getValue()
		{
			return value;
		}

		public Integer getEnd()
		{
			return end;
		}

		public String getBody()
		{
			return body;
		}
	}
}
