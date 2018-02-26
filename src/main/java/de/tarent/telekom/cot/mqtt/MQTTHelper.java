package de.tarent.telekom.cot.mqtt;

import de.tarent.telekom.cot.mqtt.util.JsonHelper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Helper class that starts our {@link io.vertx.core.Verticle}s and offers various methods for registering devices
 * and publishing messages on topics.
 */
public class MQTTHelper extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(MQTTHelper.class);
    private static final String REGISTER_SUBSCRIBE_PREFIX = "sr/";
    private static final String REGISTER_PUBLISH_PREFIX = "ss/";
    private static final String MESSAGE_SUBSCRIBE_PREFIX = "mr/";
    private static final String MESSAGE_PUBLISH_PREFIX = "ms/";
    private static MQTTHelper helper;

    final Configuration config = new Configuration();
    final BootstrapVerticle bootstrapVerticle = new BootstrapVerticle();
    final MessageVerticle messageVerticle = new MessageVerticle();

    final List<String> deploymentIds = new ArrayList<>();

    public static void main(String[] arg) {
        initAPI();
    }

    @Override
    public void start() {
        vertx.deployVerticle(config, dh -> {
            if (dh.succeeded()) {
                deploymentIds.add(dh.result());
            }
        });
        vertx.deployVerticle(bootstrapVerticle, dh -> {
            if (dh.succeeded()) {
                deploymentIds.add(dh.result());
            }
        });
        vertx.deployVerticle(messageVerticle, dh -> {
            if (dh.succeeded()) {
                deploymentIds.add(dh.result());
            }
        });

        logger.info("Verticles started");
    }

    /**
     * Deploys the {@link MQTTHelper}).
     */
    private static void initAPI() {

        Vertx v = Vertx.vertx();
        helper = new MQTTHelper();
        v.deployVerticle(helper);

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
        deploymentIds.forEach(id -> {
            vertx.undeploy(id);
        });
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
        msg.put("publishTopic", REGISTER_PUBLISH_PREFIX + deviceId);
        msg.put("subscribeTopic", REGISTER_SUBSCRIBE_PREFIX + deviceId);
        msg.put("deviceId", deviceId);
        eventBus.publish("setConfig", msg);

        eventBus.consumer("bootstrapComplete", result -> {
            final JsonObject registeredResult = (JsonObject) result.body();
            eventBus.publish("setConfig", registeredResult);
            //ToDo:prepare ReturnMSG
            callback.accept(registeredResult.getString("password"));
        });

        eventBus.publish("register", msg);
    }

    /**
     * Publishes a given message on the given topic with the given {@link Properties}. The result is then sent back with
     * the given callback once/if it's retrieved from the server.
     *
     * @param deviceId the given device with which to publish the message
     * @param message  the given message which should be published
     * @param prop     the {@link Properties} contains connection parameters (Eg. URI, port, credentials...)
     * @param callback the callback function to receive the created credentials
     */
    public void publishMessage(final String deviceId, final String message, final Properties prop,
        final Consumer callback) {

        final EventBus eventBus = vertx.eventBus();
        final JsonObject msg = JsonHelper.from(prop);
        eventBus.publish("setConfig", msg);
        msg.put("publishTopic", MESSAGE_PUBLISH_PREFIX + deviceId);
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
     * @param deviceId             the given device with which to subscribe to a topic
     * @param prop                 the {@link Properties} contains connection parameters (Eg. URI, port, credentials...)
     * @param subscriptionCallback the callback to check if subscription is successful (needed for integration tests)
     * @param callback             the callback function to receive the messages
     */
    public void subscribeToTopic(final String deviceId, final Properties prop, final Consumer subscriptionCallback,
        final Consumer callback) {

        final EventBus eventBus = vertx.eventBus();
        eventBus.consumer("received", h -> {
            final JsonObject registeredResult = (JsonObject) h.body();
            callback.accept(registeredResult.getString("received"));
        });
        final JsonObject msg = JsonHelper.from(prop);
        msg.put("subscribeTopic", MESSAGE_SUBSCRIBE_PREFIX + deviceId);
        eventBus.send("subscribe", msg, messageHandler -> {
            if (messageHandler.succeeded()) {
                subscriptionCallback.accept(messageHandler.result().body());
            } else {
                subscriptionCallback.accept(messageHandler.cause().getMessage());
            }
        });
    }

    /**
     * Unsubscribes to a given topic with the given {@link Properties}.
     *
     * @param deviceId               the given device with which to unsubscribe to a topic
     * @param prop                   the {@link Properties} contains connection parameters (Eg. URI, port, credentials...)
     * @param unsubscriptionCallback the callback to check if unsubscription is successful (needed for integration tests)
     */
    public void unsubscribeFromTopic(final String deviceId, final Properties prop,
        final Consumer unsubscriptionCallback) {

        final EventBus eventBus = vertx.eventBus();
        final JsonObject msg = JsonHelper.from(prop);
        msg.put("unsubscribeTopic", MESSAGE_SUBSCRIBE_PREFIX + deviceId);
        eventBus.send("unsubscribe", msg, messageHandler -> {
            if (messageHandler.succeeded()) {
                unsubscriptionCallback.accept(messageHandler.result().body());
            } else {
                unsubscriptionCallback.accept(messageHandler.cause().getMessage());
            }
        });
    }
}
