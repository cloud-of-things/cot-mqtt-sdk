package de.tarent.telekom.cot.mqtt.util;

import io.vertx.core.json.JsonObject;

import java.util.Properties;

public class JsonHelper {

    /**
     * Converts the given {@link Properties} into a {@link JsonObject} as simple key value pairs. All values are used as
     * Strings.
     *
     * @param prop the {@link Properties} to convert
     * @return the converted {@link JsonObject}
     */
    public static JsonObject from(Properties prop) {
        JsonObject out = new JsonObject();
        prop.stringPropertyNames().forEach(k -> {
            out.put(k, prop.getProperty(k));
        });
        return out;
    }
}
