package de.tarent.telekom.cot.mqtt;

import de.tarent.telekom.cot.mqtt.util.*;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
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


public class BootstrapVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapVerticle.class);
    private final String bootstrappedKey = "bootstrapped";
    private final String subscribeTopicKey = "subscribeTopic";

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
            .setPassword(msg.getString("initialPassword"))
            .setUsername(msg.getString("initialUser"))
            .setAutoKeepAlive(true);

        JsonHelper.setSslOptions(options, msg);

        client = MqttClient.create(vertx, options);

        config.setHandler(s -> {

            if (s.succeeded()) {
                Bootstrapped bootstrapped;

                try {
                    bootstrapped = Bootstrapped.valueOf(s.result().getString(bootstrappedKey));
                } catch (final NullPointerException e) {
                    bootstrapped = null;
                } catch (final IllegalArgumentException e) {
                    LOGGER.error(
                        "Illegal bootstrapped status value. Setting bootstrapped to null to start the bootstrapping process...");
                    bootstrapped = null;
                }

                final String secret = s.result().getString("secret");

                if (bootstrapped == null) {
                    final String newSecret = EncryptionHelper.generatePassword();
                    setPublishHandler(msg, newSecret);
                    LOGGER.info("no bootstrapping request sent. connect and publish.");
                    connectAndPublish(msg, newSecret);
                } else if (bootstrapped == ONGOING) {
                    setPublishHandler(msg, secret);
                    LOGGER.info("bootstrapping request already sent. just connect and resubscribe.");
                    final int port = Integer.parseInt(msg.getString("brokerPort"));
                    client.connect(port, msg.getString("brokerURI"), ch -> {
                        if (ch.succeeded()) {
                            LOGGER.info("Connected to a server");
                            client.subscribe(msg.getString(subscribeTopicKey),
                                MqttQoS.valueOf(msg.getInteger("QoS")).value());
                        }
                    });
                }
            }
        });

    }

    private void setPublishHandler(final JsonObject msg, final String secret) {
        client.publishHandler(s -> {
            if (s.topicName().equals(msg.getString("subscribeTopic"))) {
                LOGGER.info(String.format("Receive message with content: \"%s\" from topic \"%s\"",
                    s.payload().toString("utf-8"),
                    s.topicName()));
                final EncryptionHelper ech = new EncryptionHelper();
                final byte[] pass = ech.decrypt(new Secret(secret), s.payload().getBytes());
                client.unsubscribe(msg.getString("subscribeTopic"));
                client.disconnect();

                final JsonObject replyObject = new JsonObject();
                replyObject.put("status", "registered");
                replyObject.put("password", new String(pass));

                //write to config that bootstrap process is done
                final JsonObject bootStrapDoneMessage = new JsonObject();
                bootStrapDoneMessage.put(bootstrappedKey, BOOTSTRAPPED);
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
        final int port = Integer.parseInt(msg.getString("brokerPort"));
        client.connect(port, msg.getString("brokerURI"), ch -> {
            if (ch.succeeded()) {
                LOGGER.info("Connected to a server");
                client.subscribe(msg.getString("subscribeTopic"), MqttQoS.valueOf(msg.getInteger("QoS")).value());

                final JsonObject configParams = new JsonObject();

                client.publish(msg.getValue("publishTopic").toString(),
                    Buffer.buffer(secret),
                    MqttQoS.valueOf(msg.getInteger("QoS")),
                    false,
                    false,
                    s -> {
                        LOGGER.info("Publish sent to a server");
                        //write to config that bootstrap process has started
                        configParams.put(bootstrappedKey, ONGOING);
                        configParams.put("secret", secret);
                        eb.publish("setConfig", configParams);
                    });
            } else {
                LOGGER.error("Failed to connect to a server", ch.cause());
            }
        });
    }
}
