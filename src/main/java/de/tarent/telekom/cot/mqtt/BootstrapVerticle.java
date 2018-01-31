package de.tarent.telekom.cot.mqtt;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public class BootstrapVerticle extends AbstractVerticle{

    @Override
    public void start() throws Exception {
        EventBus eb = vertx.eventBus();

        eb.consumer("register", msg -> {
            JsonObject status = registerDevice((JsonObject)msg.body());
            msg.reply(status);
        });

    }

    JsonObject registerDevice(JsonObject msg){
        JsonObject replyObject = new JsonObject();
        //ToDo: Implement MQTT-Access
        replyObject.put("status", "registered");
        return replyObject;
    }

}
