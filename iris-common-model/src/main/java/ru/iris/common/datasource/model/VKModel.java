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

package ru.iris.common.datasource.model;

public class VKModel
{
	private int clientid;
	private String secretkey;
	private String username;
	private String password;
	private String accesstoken;

	public VKModel()
	{
	}

	public int getClientid()
	{
		return clientid;
	}

	public void setClientid(int clientid)
	{
		this.clientid = clientid;
	}

	public String getSecretkey()
	{
		return secretkey;
	}

	public void setSecretkey(String secretkey)
	{
		this.secretkey = secretkey;
	}

	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public String getAccesstoken()
	{
		return accesstoken;
	}

	public void setAccesstoken(String accesstoken)
	{
		this.accesstoken = accesstoken;
	}
}
