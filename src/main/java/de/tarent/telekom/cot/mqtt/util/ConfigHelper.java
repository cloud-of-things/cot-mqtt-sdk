package de.tarent.telekom.cot.mqtt.util;

import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static de.tarent.telekom.cot.mqtt.util.Bootstrapped.BOOTSTRAPPED;

public class ConfigHelper {

    private ConfigHelper() {
        // Private constructor to prevent instantiation.
    }

    public static Future<JsonObject> getConfigFuture(final EventBus eb) {

        final Future<JsonObject> future = Future.future();
        final JsonObject params = new JsonObject().put("keys",
            new JsonArray()
                .add(new JsonObject().put("key", "secret"))
                .add(new JsonObject().put("key", BOOTSTRAPPED)));

        eb.send("config", params, result -> {
            if (result.succeeded()) {
                future.complete((JsonObject) result.result().body());
            } else {
                future.fail(result.cause());
            }
        });

        return future;
    }
}
