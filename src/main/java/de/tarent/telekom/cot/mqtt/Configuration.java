package de.tarent.telekom.cot.mqtt;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.function.Consumer;


public class Configuration extends AbstractVerticle {

    Logger logger = LoggerFactory.getLogger(Configuration.class);
    public final String CLIENT_PROPS = "client.properties";
    public final String DEVICE_PROPS = "device.properties";


    ConfigRetriever retriever;

    @Override
    public void start(){
        String clientPropFile = System.getProperty("client.prop.path", CLIENT_PROPS);
        String devicePropFile = System.getProperty("client.prop.path", DEVICE_PROPS);

        EventBus eb = vertx.eventBus();
        eb.consumer("config", msg -> {
            JsonObject o = (JsonObject)msg.body();
            getConfig(o, b -> {
                msg.reply(b);
            });
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
        ConfigStoreOptions clOpt = new ConfigStoreOptions().setType("file").setFormat("properties")
                .setConfig(new JsonObject().put("path",clientPropFile));
        ConfigStoreOptions devOpt = new ConfigStoreOptions().setType("file").setFormat("properties")
                .setConfig(new JsonObject().put("path",devicePropFile));
        opt.addStore(clOpt).addStore(devOpt);
        retriever = ConfigRetriever.create(vertx, opt);
        logger.info("Configuration deployed");
    }

    public void getConfig(JsonObject in, Consumer<JsonObject> consumer){
        JsonObject out = new JsonObject();
        retriever.getConfig(c -> {
            if (c.succeeded()) {
                JsonObject conf = c.result();
                if (in.containsKey("keys")) {
                    in.getJsonArray("keys").forEach(o -> {
                        JsonObject jso = (JsonObject) o;
                        out.put(jso.getString("key"), conf.getValue(jso.getString("key")));
                    });
                } else if (in.containsKey("key")) {
                    out.put(in.getString("key"), conf.getValue(in.getString("key")));
                }
            }
            consumer.accept(out);
        });
    }
}
