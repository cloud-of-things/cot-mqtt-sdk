package de.tarent.telekom.cot.mqtt;

import de.tarent.telekom.cot.mqtt.util.EncryptionHelper;
import de.tarent.telekom.cot.mqtt.util.Secret;
import de.tarent.telekom.cot.mqtt.util.SmartREST;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;

import java.nio.charset.Charset;

import static de.tarent.telekom.cot.mqtt.util.Bootstrapped.BOOTSTRAPPED;
import static de.tarent.telekom.cot.mqtt.util.Bootstrapped.ONGOING;


public class BootstrapVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapVerticle.class);

    private MqttClient client;

    private EventBus eb;

    @Override
    public void start() throws Exception {
        eb = vertx.eventBus();

        eb.consumer("register", msg -> {
            registerDevice((JsonObject) msg.body());
        });
    }

    private void registerDevice(final JsonObject msg) {
        final Future<JsonObject> config = getConfig();
        LOGGER.info(msg.encodePrettily());
        final MqttClientOptions options = new MqttClientOptions()
            .setPassword(msg.getString("initialPassword"))
            .setUsername(msg.getString("initialUser"))
            .setAutoKeepAlive(true);
        client = MqttClient.create(vertx, options);

        config.setHandler(s -> {

            if (s.succeeded()) {
                final String bootstrapped = s.result().getString("bootstrapped");
                final String secret = s.result().getString("secret");

                if (bootstrapped == null) {
                    final String newSecret = EncryptionHelper.generatePassword();
                    setPublishHandler(msg, newSecret);
                    LOGGER.info("no bootstrapping request sent. connect and publish.");
                    connectAndPublish(msg, newSecret);
                } else if (bootstrapped.equals(ONGOING)) {
                    setPublishHandler(msg, secret);
                    LOGGER.info("bootstrapping request already sent. just connect and resubscribe.");
                    final int port = Integer.parseInt(msg.getString("brokerPort"));
                    client.connect(port, msg.getString("brokerURI"), ch -> {
                        if (ch.succeeded()) {
                            System.out.println("Connected to a server");
                            client.subscribe(msg.getString("subscribeTopic"), MqttQoS.AT_LEAST_ONCE.value());
                        }
                    });
                }
            }
        });

    }

    private Future<JsonObject> getConfig() {
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

                eb.publish("bootstrapComplete", replyObject);

                //write to config that bootstrap process is done
                final JsonObject bootStrapDoneMessage = new JsonObject();
                bootStrapDoneMessage.put("bootstrapped", BOOTSTRAPPED);
                //bootStrapDoneMessage.put("password", new String(pass));
                eb.publish("setConfig", bootStrapDoneMessage);
            }
        });
    }

    private void connectAndPublish(final JsonObject msg, final String secret) {
        //connect and publish on /iccid
        final int port = Integer.parseInt(msg.getString("brokerPort"));
        client.connect(port, msg.getString("brokerURI"), ch -> {
            if (ch.succeeded()) {
                System.out.println("Connected to a server");
                client.subscribe(msg.getString("subscribeTopic"), MqttQoS.AT_LEAST_ONCE.value());

                final JsonObject configParams = new JsonObject();

                client.publish(msg.getValue("publishTopic").toString(),
                    Buffer.buffer(secret),
                    MqttQoS.AT_LEAST_ONCE,
                    false,
                    false,
                    s -> {
                        LOGGER.info("Publish sent to a server");
                        //write to config that bootstrap process has started
                        configParams.put("bootstrapped", ONGOING);
                        configParams.put("secret", secret);
                        eb.publish("setConfig", configParams);
                    });
            } else {
                LOGGER.error("Failed to connect to a server", ch.cause());
            }
        });
    }

    private void createManagedObject(){
        final Future<JsonObject> config = getConfig();

        config.setHandler(s -> {

                if (s.succeeded()) {
                    JsonObject configObject = s.result();

                    final String deviceId = s.result().getString("deviceId");
                    final String password = s.result().getString("password");

                    final MqttClientOptions options = new MqttClientOptions()
                        .setPassword(password)
                        .setUsername(deviceId)
                        .setAutoKeepAlive(true);
                    final int port = Integer.parseInt(configObject.getString("brokerPort"));
                    final MqttClient MOclient = MqttClient.create(vertx, options);

                    MOclient.publishHandler(h -> {
                        LOGGER.info("Message with topic " + h.topicName() + " with QOS " + h.qosLevel().name() + " received");
                        if (h.topicName().equals(s.result().getString("subscribeTopic"))) {

                            String[] parsedPayload= SmartREST.parseResponsePayload(h.payload());
                            //object doesnt exist
                            if(parsedPayload[0].equals("50")&&parsedPayload[2].equals("404")){
                                String message = SmartREST.getPayloadSelfCreationRequest("mascot-testdevices1",configObject.getString("deviceId"),"deviceName");
                                MOclient.connect(port, s.result().getString("brokerURI"), ch -> {
                                    if (ch.succeeded()) {
                                        MOPublish(MOclient, configObject.getString("publishTopic"), message);
                                    } else {
                                        LOGGER.error("Failed to connect to a server", ch.cause());
                                    }
                                });
                            }
                            //object already exists
                            else if(parsedPayload[0].equals("601")){
                                //601,1,mascot3,2817383;
                            }
                            //object created we
                            if(parsedPayload[0].equals("603")){
                                final JsonObject managedObject = new JsonObject();
                                managedObject.put("managedObjectId", parsedPayload[2]);
                                eb.publish("setConfig", managedObject);

                                String registerICCIDString = SmartREST.getPayloadRegisterICCIDasExternalId("mascot-testdevices1",parsedPayload[2],configObject.getString("deviceId"));
                                MOclient.connect(port, s.result().getString("brokerURI"), ch -> {
                                    if (ch.succeeded()) {
                                        MOPublish(MOclient, configObject.getString("publishTopic"), registerICCIDString);
                                    } else {
                                        LOGGER.error("Failed to connect to a server", ch.cause());
                                    }
                                });

                                String updateOperationsString = SmartREST.getPayloadUpdateOperations("mascot-testdevices1",parsedPayload[2]);
                                MOclient.connect(port, s.result().getString("brokerURI"), ch -> {
                                    if (ch.succeeded()) {
                                        MOPublish(MOclient, configObject.getString("publishTopic"), updateOperationsString);
                                    } else {
                                        LOGGER.error("Failed to connect to a server", ch.cause());
                                    }
                                });
                                MOclient.unsubscribe(configObject.getString("subscribeTopic"));
                            }
                        }
                    });

                    MOclient.connect(port, s.result().getString("brokerURI"), ch -> {
                        if (ch.succeeded()) {
                            LOGGER.info("Connected to a server");
                            MOclient.subscribe(configObject.getString("subscribeTopic"), MqttQoS.AT_MOST_ONCE.value(),
                                d -> {});
                            MOPublish(MOclient, configObject.getString("publishTopic"),SmartREST.getPayloadCheckManagedObject("mascot-testdevices1", s.result().getString("deviceId")));
                        } else {
                            LOGGER.error("Failed to connect to a server", ch.cause());
                        }
                    });
            }
        });

    }

    private void MOPublish(MqttClient client, String topic, String message){
        client.publish(
            topic,
            Buffer.buffer(message),
            MqttQoS.AT_MOST_ONCE,
            false,
            false,
            k -> {}
        );
    }

}
