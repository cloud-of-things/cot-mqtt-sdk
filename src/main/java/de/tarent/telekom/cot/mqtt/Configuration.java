package de.tarent.telekom.cot.mqtt;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.config.spi.utils.JsonObjectHelper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Properties;
import java.util.function.Consumer;


public class Configuration extends AbstractVerticle {

    Logger logger = LoggerFactory.getLogger(Configuration.class);

    JsonObject conf;

    ConfigRetriever retriever;


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
        ConfigRetrieverOptions opt = new ConfigRetrieverOptions();
        opt
                .addStore(
                        new ConfigStoreOptions().setType("json")
                                .setConfig(vertx.getOrCreateContext().config()))
                .addStore(
                        new ConfigStoreOptions().setType("sys").setConfig(new JsonObject().put("cache", false))
                )
                .addStore(new ConfigStoreOptions().setType("env")
                );
        retriever = ConfigRetriever.create(vertx, opt);
        retriever.getConfig(c -> {
            if (c.succeeded()){
                conf = c.result();
            }
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
