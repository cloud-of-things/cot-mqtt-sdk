package de.tarent.telekom.cot.mqtt.util;

import io.vertx.core.json.JsonObject;

import java.util.Properties;

public class JsonHelper {

    /**
     * Converts java.util.Properties in JsonObject as simple key-value-pairs. Alle Values are used as Strings
     * @param prop
     * @return
     */
    public static JsonObject from(Properties prop){
        JsonObject out = new JsonObject();
        prop.stringPropertyNames().forEach(k ->{
            out.put(k, prop.getProperty(k));
        });
        return out;
    }
}
