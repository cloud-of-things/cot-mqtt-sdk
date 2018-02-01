package de.tarent.telekom.cot.mqtt.util;

import io.vertx.core.json.JsonObject;

import java.util.Properties;

public class JsonHelper {

    public static JsonObject from(Properties prop){
        JsonObject out = new JsonObject();
        prop.stringPropertyNames().forEach(k ->{
            out.put(k, prop.getProperty(k));
        });
        return out;
    }
}
