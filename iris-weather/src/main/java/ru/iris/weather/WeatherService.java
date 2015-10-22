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

package ru.iris.weather;

import com.github.dvdme.ForecastIOLib.FIOCurrently;
import com.github.dvdme.ForecastIOLib.ForecastIO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.Config;
import ru.iris.common.helpers.DBLogger;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.devices.GenericAdvertisement;
import ru.iris.common.modulestatus.Status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class WeatherService
{
    private Logger LOGGER = LogManager.getLogger(WeatherService.class);
    private final Config conf = Config.getInstance();

    public WeatherService() {
        Status status = new Status("Weather");

        if (status.checkExist()) {
            status.running();
        } else {
            status.addIntoDB("Weather", "Service that getting current weather");
        }

        LOGGER.info("Weather service started (LAT: " + conf.get("weatherLatitude") + ", LON: " + conf.get("weatherLongitude") + ")");

        JsonMessaging jsonMessaging = new JsonMessaging(UUID.randomUUID(), "weather");
        jsonMessaging.subscribe("event.weather.get");

        jsonMessaging.setNotification(envelope -> {

            if (envelope.getObject() instanceof GenericAdvertisement) {
                GenericAdvertisement advertisement = envelope.getObject();

                switch (advertisement.getLabel()) {

                    case "GetWeatherBroadcast":
                        LOGGER.info("Getting weather...");
                        DBLogger.info("Getting weather...");

                        GenericAdvertisement broadcast = new GenericAdvertisement();
                        broadcast.setData(getWeather());

                        LOGGER.info("Broadcasting weather info...");
                        jsonMessaging.broadcast("event.weather", broadcast);
                        break;

                    case "GetWeather":
                        LOGGER.info("Getting weather...");
                        DBLogger.info("Getting weather...");

                        GenericAdvertisement response = new GenericAdvertisement();
                        response.setData(getWeather());

                        LOGGER.info("Responce weather info...");
                        jsonMessaging.response(envelope, response);
                        break;

                    default:
                        LOGGER.error("Unknown label: " + advertisement.getLabel());
                }

            } else {
                // We received unknown request message. Lets make generic log entry.
                LOGGER.info("Received request "
                        + " from " + envelope.getSenderInstance()
                        + " to " + envelope.getReceiverInstance()
                        + " at '" + envelope.getSubject()
                        + ": " + envelope.getObject());
            }
        });

        jsonMessaging.start();

    }

    private Map<String, Object> getWeather() {
        ForecastIO fio = new ForecastIO(conf.get("weatherApi"));
        fio.setUnits(ForecastIO.UNITS_SI);
        fio.getForecast(conf.get("weatherLatitude"), conf.get("weatherLongitude"));
        fio.setLang(ForecastIO.LANG_RUSSIAN);

        FIOCurrently currently = new FIOCurrently(fio);

        Map<String, Object> data = new HashMap<>();

        data.put("temperature", currently.get().temperature());
        data.put("humidity", currently.get().humidity());
        data.put("icon", currently.get().icon());
        data.put("time", currently.get().time());
        data.put("clouds", currently.get().cloudCover());
        data.put("pressure", currently.get().pressure());
        data.put("sunrise", currently.get().sunriseTime());
        data.put("sunset", currently.get().sunsetTime());

        return data;
    }
}
