package de.tarent.telekom.cot.mqtt;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


public class Configuration extends AbstractVerticle {

    Logger logger = LoggerFactory.getLogger(Configuration.class);

    JsonObject conf = new JsonObject();

    @Override
    public void start(){

        EventBus eb = vertx.eventBus();
        eb.consumer("config", msg -> {
            JsonObject o = (JsonObject)msg.body();
            msg.reply(getConfig(o));
        });
        eb.consumer("setConfig", h ->{
            JsonObject msg = (JsonObject)h.body();
            setConfig(msg);
        });

        logger.info("Configuration deployed");
    }

    public JsonObject getConfig(JsonObject in){
        JsonObject out = new JsonObject();

        if (in.containsKey("keys")) {
            in.getJsonArray("keys").forEach(o -> {
                JsonObject jso = (JsonObject) o;
                out.put(jso.getString("key"), conf.getString(jso.getString("key")));
            });
        } else if (in.containsKey("key")) {
            out.put(in.getString("key"), conf.getString(in.getString("key")));
        }
        return out;
    }

    public void setConfig(JsonObject obj){
        conf.mergeIn(obj);
    }
}
