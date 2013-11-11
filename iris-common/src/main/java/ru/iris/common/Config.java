/**
 * Copyright 2013 Nikolay A. Viguro, Tommi S.E. Laukkanen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.iris.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Class for loading configuration from properties files and database.
 */
public class Config {
    /**
     * The logger.
     */
    private static Logger LOGGER = LoggerFactory.getLogger(Config.class);
    /**
     * The map of loaded properties.
     */
    private static HashMap<String, String> propertyMap = null;

    /**
     * Default constructor which loads properties from different storages.
     */
    public Config() {
        synchronized (Config.class) {

            if (propertyMap != null) {
                return;
            }
            propertyMap = new HashMap<String, String>();
            loadPropertiesFromClassPath("/conf/iris-default.properties");
            if (!loadPropertiesFromClassPath("/conf/iris-extended.properties")) {
                if (!loadPropertiesFromFileSystem("/conf/iris-extended.properties")) {
                    loadPropertiesFromFileSystem("./conf/main.property");
                }
            }
            loadPropertiesFromDatabase();
            LOGGER.debug("Loaded configuration: ");
            final List<String> keys = new ArrayList<String>(propertyMap.keySet());
            Collections.sort(keys);
            for (final String key : keys) {
                if (key.toLowerCase().contains("password")) {
                    LOGGER.debug(key + " = <HIDDEN>");
                } else {
                    LOGGER.debug(key + " = " + propertyMap.get(key));
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
    private boolean loadPropertiesFromClassPath(final String propertiesFileName) {
        final InputStream inputStream = Config.class.getResourceAsStream(propertiesFileName);
        if (inputStream == null) {
            LOGGER.debug("Properties not found from classpath: " + propertiesFileName);
            return false;
        }
        try {
            final Properties properties = new Properties();
            properties.load(inputStream);
            final Enumeration enumeration = properties.keys();
            while (enumeration.hasMoreElements()) {
                final String key = (String) enumeration.nextElement();
                propertyMap.put(key, (String) properties.get(key));
            }
        } catch (final IOException e) {
            LOGGER.debug("Error loading properties from classpath: " + propertiesFileName, e);
            return false;
        }

        LOGGER.debug("Loaded properties from classpath: " + propertiesFileName);
        return true;
    }

    /**
     * Loads given properties file from file system.
     *
     * @param propertiesFileName the property file name
     * @return true if file was found and loaded successfully.
     */
    private boolean loadPropertiesFromFileSystem(final String propertiesFileName) {
        try {
            final InputStream inputStream = new FileInputStream(propertiesFileName);
            if (inputStream == null) {
                return false;
            }

            final Properties properties = new Properties();
            properties.load(inputStream);
            final Enumeration enumeration = properties.keys();
            while (enumeration.hasMoreElements()) {
                final String key = (String) enumeration.nextElement();
                propertyMap.put(key, (String) properties.get(key));
            }

            LOGGER.debug("Loaded properties from file system: " + propertiesFileName);
            return true;
        } catch (final IOException e) {
            LOGGER.debug("Error loading properties from file system: " + propertiesFileName + " : " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads properties files from database.
     *
     * @return true if properties were loaded from database successfully.
     */
    private boolean loadPropertiesFromDatabase() {
        try {
            final SQL sql = new SQL();
            final ResultSet rs = sql.select("SELECT name, param FROM config");
            if (rs == null) {
                LOGGER.debug("Error loading properties from database.");
                return false;
            }
            while (rs.next()) {
                String name = rs.getString("name");
                String val = rs.getString("param");

                propertyMap.put(name, val);
            }
            rs.close();
            sql.close();
            LOGGER.debug("Loaded properties from database.");
            return true;
        } catch (final IOException e) {
            LOGGER.debug("Error loading properties from database.", e);
            return false;
        } catch (final SQLException e) {
            LOGGER.debug("Error loading properties from database: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns the configuration properties map containing key value pairs.
     *
     * @return the configuration properties.
     */
    public Map<String, String> getConfig() {
        return Collections.unmodifiableMap(propertyMap);
    }
}
