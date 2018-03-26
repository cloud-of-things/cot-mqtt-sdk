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

import static de.tarent.telekom.cot.mqtt.util.Bootstrapped.BOOTSTRAPPED;
import static de.tarent.telekom.cot.mqtt.util.JsonHelper.*;

/**
 * Helper class that starts our {@link io.vertx.core.Verticle}s and offers various methods for registering devices
 * and publishing messages on topics.
 */
public class MQTTHelper extends AbstractVerticle {

    static final String DEVICE_NOT_BOOTSTRAPPED = "Device is not bootstrapped! Please bootstrap the device before trying to subscribe.";
    private static final String ERROR_RETRIEVING_CONFIG = "Error retrieving config!";

    private static final Logger logger = LoggerFactory.getLogger(MQTTHelper.class);
    private static final String REGISTER_SUBSCRIBE_PREFIX = "sr/";
    private static final String REGISTER_PUBLISH_PREFIX = "ss/";
    private static final String MESSAGE_SUBSCRIBE_PREFIX = "mr/";
    private static final String MESSAGE_PUBLISH_PREFIX = "ms/";
    private static MQTTHelper helper;

    private final Configuration config = new Configuration();
    private final BootstrapVerticle bootstrapVerticle = new BootstrapVerticle();
    private final MessageVerticle messageVerticle = new MessageVerticle();
    private final ManagedObjectHelperVerticle managedObjectHelperVerticle = new ManagedObjectHelperVerticle();

    private final List<String> deploymentIds = new ArrayList<>();

    public static void main(final String[] arg) {
        final Vertx v = Vertx.vertx();
        initAPI(v);
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
        vertx.deployVerticle(managedObjectHelperVerticle, dh -> {
            if (dh.succeeded()) {
                deploymentIds.add(dh.result());
            }
        });

        logger.info("Verticles started");
    }

    /**
     * Deploys the {@link MQTTHelper}.
     */
    private static void initAPI(Vertx v) {
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
            final Vertx v = Vertx.vertx();
            initAPI(v);
        }

        return helper;
    }

    /**
     * Returns the {@link MQTTHelper} instance if it was created and creates a new one, returning that if it was
     * null.
     *
     * @param v -Vertx instance - to use if consumer is an vertx.io application itself
     * @return the {@link MQTTHelper} instance
     */
    public static MQTTHelper getInstance(Vertx v) {
        if (helper == null) {
            initAPI(v);
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
     * @param prop     the {@link Properties} contains connection parameters (Eg. URI, port, credentials, QoS...)
     * @param callback the callback function to receive the created credentials
     */
    public void registerDevice(final String deviceId, final Properties prop, final Consumer<String> callback) {
        final EventBus eventBus = vertx.eventBus();
        final JsonObject msg = JsonHelper.from(prop);
        msg.put(PUBLISH_TOPIC_KEY, REGISTER_PUBLISH_PREFIX + deviceId);
        msg.put(SUBSCRIBE_TOPIC_KEY, REGISTER_SUBSCRIBE_PREFIX + deviceId);
        msg.put(MO_PUBLISH_TOPIC_KEY, MESSAGE_PUBLISH_PREFIX + deviceId);
        msg.put(MO_SUBSCRIBE_TOPIC_KEY, MESSAGE_SUBSCRIBE_PREFIX + deviceId);
        msg.put(DEVICE_ID_KEY, deviceId);
        msg.put(QOS_KEY, JsonHelper.getQoSValue(msg));
        msg.put(SSL_KEY, JsonHelper.getSslValue(msg));
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
     * @param prop     the {@link Properties} contains connection parameters (Eg. URI, port, credentials, QoS...)
     * @param callback the callback function to receive the created credentials
     */
    public void publishMessage(final String deviceId, final String message, final Properties prop,
        final Consumer<Boolean> callback) {

        final EventBus eventBus = vertx.eventBus();
        final JsonObject msg = JsonHelper.from(prop);
        eventBus.publish("setConfig", msg);
        msg.put(PUBLISH_TOPIC_KEY, MESSAGE_PUBLISH_PREFIX + deviceId);
        msg.put(MESSAGE_KEY, message);
        msg.put(QOS_KEY, JsonHelper.getQoSValue(msg));
        msg.put(SSL_KEY, JsonHelper.getSslValue(msg));

        eventBus.send("publish", msg, result -> {
            if (result.succeeded()) {
                final JsonObject registeredResult = (JsonObject) result.result().body();
                //ToDo:prepare ReturnMSG
                callback.accept(registeredResult.getBoolean("published"));
            } else {
                logger.error("Sending message failed - ", result.cause());
            }
        });
    }

    /**
     * Subscribes to a given topic with the given {@link Properties}.
     *
     * @param deviceId             the given device with which to subscribe to a topic
     * @param prop                 the {@link Properties} contains connection parameters (Eg. URI, port, credentials, QoS...)
     * @param subscriptionCallback the callback to check if subscription is successful (needed for integration tests)
     * @param callback             the callback function to receive the messages
     */
    public void subscribeToTopic(final String deviceId, final Properties prop,
        final Consumer<Object> subscriptionCallback, final Consumer<String> callback) {

        final EventBus eventBus = vertx.eventBus();
        eventBus.consumer("received", h -> {
            final JsonObject registeredResult = (JsonObject) h.body();
            callback.accept(registeredResult.getString("received"));
        });

        final JsonObject question = new JsonObject().put("key", "bootstrapped");

        eventBus.send("config", question, r -> {
            if (r.succeeded()) {
                final JsonObject bootstrappedProperty = (JsonObject) r.result().body();
                if (bootstrappedProperty != null && bootstrappedProperty.getString("bootstrapped") != null
                    && BOOTSTRAPPED.name().equals(bootstrappedProperty.getString("bootstrapped"))) {

                    final JsonObject msg = JsonHelper.from(prop);
                    msg.put(SUBSCRIBE_TOPIC_KEY, MESSAGE_SUBSCRIBE_PREFIX + deviceId);
                    msg.put(QOS_KEY, JsonHelper.getQoSValue(msg));
                    msg.put(SSL_KEY, JsonHelper.getSslValue(msg));

                    eventBus.send("subscribe", msg, messageHandler -> {
                        if (messageHandler.succeeded()) {
                            final JsonObject o = (JsonObject) messageHandler.result().body();
                            subscriptionCallback.accept(o.getBoolean("subscribed"));
                        } else {
                            logger.error(messageHandler.cause().getMessage(), messageHandler.cause());
                            subscriptionCallback.accept(false);
                        }
                    });
                } else {
                    subscriptionCallback.accept(DEVICE_NOT_BOOTSTRAPPED);
                }
            } else {
                subscriptionCallback.accept(ERROR_RETRIEVING_CONFIG);
            }
        });
    }

    /**
     * Unsubscribes to a given topic with the given {@link Properties}.
     *
     * @param deviceId               the given device with which to unsubscribe to a topic
     * @param prop                   the {@link Properties} contains connection parameters (Eg. URI, port, credentials, QoS...)
     * @param unsubscriptionCallback the callback to check if unsubscription is successful (needed for integration tests)
     */
    public void unsubscribeFromTopic(final String deviceId, final Properties prop,
        final Consumer<Boolean> unsubscriptionCallback) {

        final EventBus eventBus = vertx.eventBus();
        final JsonObject msg = JsonHelper.from(prop);
        msg.put(UNSUBSCRIBE_TOPIC_KEY, MESSAGE_SUBSCRIBE_PREFIX + deviceId);
        msg.put(QOS_KEY, JsonHelper.getQoSValue(msg));
        msg.put(SSL_KEY, JsonHelper.getSslValue(msg));

        eventBus.send("unsubscribe", msg, messageHandler -> {
            if (messageHandler.succeeded()) {
                JsonObject o = (JsonObject) messageHandler.result().body();
                unsubscriptionCallback.accept(o.getBoolean("unsubscribed"));
            } else {
                logger.error(messageHandler.cause().getMessage(), messageHandler.cause());
                unsubscriptionCallback.accept(false);
            }
        });
    }
}
