package de.tarent.telekom.cot.mqtt;

import de.tarent.telekom.cot.mqtt.util.JsonHelper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Properties;
import java.util.function.Consumer;

/**
 * Helper class that starts our {@link io.vertx.core.Verticle}s and offers various methods for registering devices
 * and publishing messages on topics.
 */
public class MQTTHelper extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(MQTTHelper.class);
    private static MQTTHelper helper;


    /**
     * Starts all the included {@link io.vertx.core.Verticle}s ({@link Configuration}, {@link BootstrapVerticle},
     * {@link MessageVerticle} and {@link MQTTHelper}).
     */
    private static void initAPI() {
        final Vertx vertx = Vertx.vertx();

        final Configuration config = new Configuration();
        vertx.deployVerticle(config);

        final BootstrapVerticle bootstrapVerticle = new BootstrapVerticle();
        vertx.deployVerticle(bootstrapVerticle);

        final MessageVerticle messageVerticle = new MessageVerticle();
        vertx.deployVerticle(messageVerticle);

        helper = new MQTTHelper();
        vertx.deployVerticle(helper);

        logger.info("Verticles started");
    }

    /**
     * Returns the {@link MQTTHelper} instance if it was created and creates a new one, returning that if it was
     * null.
     *
     * @return the {@link MQTTHelper} instance
     */
    public static MQTTHelper getInstance() {
        if (helper == null) {
            initAPI();
        }

        return helper;
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        helper = null;
    }

    /**
     * Registers the given deviceId (iccID) with the given {@link Properties}. The result is then sent back with the
     * given callback once/if it's retrieved from the server.
     *
     * @param deviceId the iccID of the device
     * @param prop     the {@link Properties} contains connection parameters (Eg. URI, port, credentials...)
     * @param callback the callback function to receive the created credentials
     */
    public void registerDevice(final String deviceId, final Properties prop, final Consumer callback) {
        final EventBus eventBus = vertx.eventBus();
        final JsonObject msg = JsonHelper.from(prop);
        eventBus.publish("setConfig", msg);
        msg.put("deviceId", deviceId);
        eventBus.send("register", msg, result -> {
            if (result.succeeded()) {
                final JsonObject registeredResult = (JsonObject) result.result().body();
                eventBus.publish("setConfig", registeredResult);
                //ToDo:prepare ReturnMSG
                callback.accept(registeredResult.encodePrettily());
            } else {
                logger.error("Registration failed - ", result.cause());
            }
        });
    }

    /**
     * Publishes a given message on the given topic with the given {@link Properties}. The result is then sent back with
     * the given callback once/if it's retrieved from the server.
     *
     * @param topic    the given topic on which to publish the message
     * @param message  the given message which should be published
     * @param prop     the {@link Properties} contains connection parameters (Eg. URI, port, credentials...)
     * @param callback the callback function to receive the created credentials
     */
    public void publishMessage(final String topic, final String message, final Properties prop,
        final Consumer callback) {

        final EventBus eventBus = vertx.eventBus();
        final JsonObject msg = JsonHelper.from(prop);
        eventBus.publish("setConfig", msg);
        msg.put("publishTopic", topic);
        msg.put("message", message);
        eventBus.send("publish", msg, result -> {
            if (result.succeeded()) {
                final JsonObject registeredResult = (JsonObject) result.result().body();
                //ToDo:prepare ReturnMSG
                callback.accept(registeredResult.encodePrettily());
            } else {
                logger.error("Sending message failed - ", result.cause());
            }
        });
    }

    /**
     * Subscribes to a given topic with the given {@link Properties}.
     *
     * @param topic    the given topic on which to publish the message
     * @param prop     the {@link Properties} contains connection parameters (Eg. URI, port, credentials...)
     * @param subscriptionCallBack the callback to check if subscription is successful (needed for integration tests)
     * @param callback the callback function to receive the created credentials
     */
    public void subscribeToTopic(final String topic, final Properties prop, final Consumer subscriptionCallBack, final Consumer callback) {
        final EventBus eventBus = vertx.eventBus();
        eventBus.consumer("received", h -> {
            final JsonObject registeredResult = (JsonObject) h.body();
            callback.accept(registeredResult.encodePrettily());
        });
        final JsonObject msg = JsonHelper.from(prop);
        msg.put("subscribeTopic", topic);
        eventBus.send("subscribe", msg, messageHandler ->{
        		if (messageHandler.succeeded()) {
        			subscriptionCallBack.accept(messageHandler.result().body());
        		}else {
        			subscriptionCallBack.accept(messageHandler.cause().getMessage());
        		}
        });
       
    }

}
