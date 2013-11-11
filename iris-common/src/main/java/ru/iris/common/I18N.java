package ru.iris.common;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 23.10.13
 * Time: 22:12
 * License: GPL v3
 */

import org.jetbrains.annotations.PropertyKey;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class I18N {

    private static Map<String, String> config;

    public I18N() {
        Config cfg = new Config();
        config = cfg.getConfig();
    }

    public String message(@PropertyKey(resourceBundle = "ru.iris.language") String key, Object... params) {
        ResourceBundle bundle = ResourceBundle.getBundle("ru.iris.language", new Locale(config.get("language")));
        String value = bundle.getString(key);
        if (params.length > 0) return MessageFormat.format(value, params);
        return value;
    }
}
