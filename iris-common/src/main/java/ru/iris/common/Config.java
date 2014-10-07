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
package ru.iris.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Class for loading configuration from properties files and database.
 */
public class Config
{
	/**
	 * The map of loaded properties.
	 */
	private static final Logger LOGGER = LogManager.getLogger(Config.class);
	private static Map<String, String> propertyMap = null;

	/**
	 * Singleton (On Demand Holder)
	 */
	public static class SingletonHolder
	{
		public static final Config HOLDER_INSTANCE = new Config();
	}

	public static Config getInstance()
	{
		return SingletonHolder.HOLDER_INSTANCE;
	}

	/**
	 * Default constructor which loads properties from different storages.
	 */
	private Config()
	{
		synchronized (Config.class)
		{

			if (propertyMap != null)
			{
				return;
			}
			propertyMap = new HashMap<>();
			loadPropertiesFromClassPath("/conf/iris-default.properties");
			if (!loadPropertiesFromClassPath("/conf/iris-extended.properties"))
			{
				if (!loadPropertiesFromFileSystem("/conf/iris-extended.properties"))
				{
					loadPropertiesFromFileSystem("./conf/main.property");
				}
			}
		}
	}

	/**
	 * Loads given properties file from class path.
	 *
	 * @param propertiesFileName the property file name
	 * @return true if file was found and loaded successfully.
	 */
	private boolean loadPropertiesFromClassPath(final String propertiesFileName)
	{
		final InputStream inputStream = Config.class.getResourceAsStream(propertiesFileName);
		if (inputStream == null)
		{
			return false;
		}
		try
		{
			final Properties properties = new Properties();
			properties.load(inputStream);
			final Enumeration enumeration = properties.keys();
			while (enumeration.hasMoreElements())
			{
				final String key = (String) enumeration.nextElement();
				propertyMap.put(key, (String) properties.get(key));
			}
		}
		catch (final IOException e)
		{
			return false;
		}

		return true;
	}

	/**
	 * Loads given properties file from file system.
	 *
	 * @param propertiesFileName the property file name
	 * @return true if file was found and loaded successfully.
	 */
	private boolean loadPropertiesFromFileSystem(final String propertiesFileName)
	{
		try
		{
			final InputStream inputStream = new FileInputStream(propertiesFileName);
			final Properties properties = new Properties();
			properties.load(inputStream);
			final Enumeration enumeration = properties.keys();

			while (enumeration.hasMoreElements())
			{
				final String key = (String) enumeration.nextElement();
				propertyMap.put(key, (String) properties.get(key));
			}

			return true;
		}
		catch (final IOException e)
		{
			return false;
		}
	}

	/**
	 *
	 * @return the configuration property value.
	 */
	public String get(String key)
	{
		if (propertyMap.get(key) != null)
		{
			return propertyMap.get(key);
		}

		LOGGER.error("Configuration key = " + key + " not found!");
		return null;
	}
}
