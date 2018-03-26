package de.tarent.telekom.cot.mqtt;

import de.tarent.telekom.cot.mqtt.util.*;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;

import static de.tarent.telekom.cot.mqtt.util.Bootstrapped.BOOTSTRAPPED;
import static de.tarent.telekom.cot.mqtt.util.Bootstrapped.ONGOING;
import static de.tarent.telekom.cot.mqtt.util.JsonHelper.*;

public class BootstrapVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapVerticle.class);
    private static final String BOOTSTRAPPED_KEY = "bootstrapped";

    private MqttClient client;

    private EventBus eb;

    @Override
    public void start() throws Exception {
        eb = vertx.eventBus();

        eb.consumer("register", msg -> registerDevice((JsonObject) msg.body()));
    }

    private void registerDevice(final JsonObject msg) {
        final Future<JsonObject> config = ConfigHelper.getConfigFuture(eb);
        LOGGER.info(msg.encodePrettily());
        final MqttClientOptions options = new MqttClientOptions()
            .setPassword(msg.getString(INITIAL_PASSWORD_KEY))
            .setUsername(msg.getString(INITIAL_USER_KEY))
            .setAutoKeepAlive(true);

        JsonHelper.setSslOptions(options, msg);

        client = MqttClient.create(vertx, options);

        config.setHandler(s -> {
            if (s.succeeded()) {
                final Bootstrapped bootstrapped = getBootstrappedStatus(s);
                final String secret = s.result().getString("secret");

                if (bootstrapped == null) {
                    bootstrapDevice(msg);
                } else if (bootstrapped == ONGOING) {
                    bootstrapConnectAndSubscribe(msg, secret);
                }
            }
        });
    }

    private void bootstrapDevice(final JsonObject msg) {
        final String newSecret = EncryptionHelper.generatePassword();
        setPublishHandler(msg, newSecret);
        LOGGER.info("no bootstrapping request sent. connect and publish.");
        connectAndPublish(msg, newSecret);
    }

    private void bootstrapConnectAndSubscribe(final JsonObject msg, final String secret) {
        setPublishHandler(msg, secret);
        LOGGER.info("bootstrapping request already sent. just connect and resubscribe.");
        final int port = Integer.parseInt(msg.getString(BROKER_PORT_KEY));
        client.connect(port, msg.getString(BROKER_URI_KEY), ch -> {
            if (ch.succeeded()) {
                LOGGER.info("Connected to a server");
                client.subscribe(msg.getString(SUBSCRIBE_TOPIC_KEY),
                    MqttQoS.valueOf(msg.getInteger(QOS_KEY)).value());
            }
        });
    }

    private Bootstrapped getBootstrappedStatus(final AsyncResult<JsonObject> s) {
        try {
            return Bootstrapped.valueOf(s.result().getString(BOOTSTRAPPED_KEY));
        } catch (final NullPointerException e) {
            return null;
        } catch (final IllegalArgumentException e) {
            LOGGER.error(
                "Illegal bootstrapped status value. Setting bootstrapped to null to start the bootstrapping process...");
            return null;
        }
    }

    private void setPublishHandler(final JsonObject msg, final String secret) {
        client.publishHandler(s -> {
            if (s.topicName().equals(msg.getString(SUBSCRIBE_TOPIC_KEY))) {
                LOGGER.info(String.format("Receive message with content: \"%s\" from topic \"%s\"",
                    s.payload().toString("utf-8"),
                    s.topicName()));
                final EncryptionHelper ech = new EncryptionHelper();
                final byte[] pass = ech.decrypt(new Secret(secret), s.payload().getBytes());
                client.unsubscribe(msg.getString(SUBSCRIBE_TOPIC_KEY));
                client.disconnect();

                final JsonObject replyObject = new JsonObject();
                replyObject.put("status", "registered");
                replyObject.put("password", new String(pass));

                //write to config that bootstrap process is done
                final JsonObject bootStrapDoneMessage = new JsonObject();
                bootStrapDoneMessage.put(BOOTSTRAPPED_KEY, BOOTSTRAPPED);
                bootStrapDoneMessage.put("password", new String(pass));
                eb.publish("setConfig", bootStrapDoneMessage);

                eb.consumer("managedObjectCreated", result -> {
                    JsonObject moId= (JsonObject) result.body();
                    replyObject.put("managedObjectId",moId.getValue("managedObjectId"));
                    eb.publish("bootstrapComplete", replyObject);
                });

                JsonObject moMsg = msg.copy();
                moMsg.put("cloudPassword", new String(pass));
                eb.publish("createManagedObject", moMsg);
            }
        });
    }

    private void connectAndPublish(final JsonObject msg, final String secret) {
        //connect and publish on /iccid
        final int port = Integer.parseInt(msg.getString(BROKER_PORT_KEY));
        client.connect(port, msg.getString(BROKER_URI_KEY), ch -> {
            if (ch.succeeded()) {
                LOGGER.info("Connected to a server");
                client.subscribe(msg.getString(SUBSCRIBE_TOPIC_KEY), MqttQoS.valueOf(msg.getInteger(QOS_KEY)).value());

                final JsonObject configParams = new JsonObject();

                client.publish(msg.getValue(PUBLISH_TOPIC_KEY).toString(),
                    Buffer.buffer(secret),
                    MqttQoS.valueOf(msg.getInteger(QOS_KEY)),
                    false,
                    false,
                    s -> {
                        LOGGER.info("Publish sent to a server");
                        //write to config that bootstrap process has started
                        configParams.put(BOOTSTRAPPED_KEY, ONGOING);
                        configParams.put("secret", secret);
                        eb.publish("setConfig", configParams);
                    });
            } else {
                LOGGER.error("Failed to connect to a server", ch.cause());
            }
        });
    }
}
