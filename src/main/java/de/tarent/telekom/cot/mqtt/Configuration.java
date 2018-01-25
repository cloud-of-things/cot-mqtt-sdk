package de.tarent.telekom.cot.mqtt;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

import java.util.Properties;

public class Configuration extends AbstractVerticle {

    ConfigRetriever retreiver;

    public Configuration(){
        retreiver = ConfigRetriever.create(vertx);
    }

    @Override
    public void start(){
        EventBus eb = vertx.eventBus();
        eb.consumer("config", msg -> {
            JsonObject o = (JsonObject)msg.body();
            msg.reply (getConfig(o));
        });
    }

    public JsonObject getConfig(JsonObject in){
        JsonObject out = new JsonObject();
        JsonObject conf = new JsonObject;
        retreiver.getConfig(c -> {
            if (c.succeeded()){
                conf = c.result();
            }
        });
        if (in.containsKey("keys")){
            in.getJsonArray("keys").forEach(o -> {
                JsonObject jso = (JsonObject)o;
                out.put(jso.getString("key"), conf.getValue(jso.getString("key")));
            });
        }else if (in.containsKey("key")){
            out.put(in.getString("key"), conf.getValue(in.getString("key")));
        }
        return out;
    }
}
