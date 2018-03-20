package de.tarent.telekom.cot.mqtt;

import de.tarent.telekom.cot.mqtt.util.ConfigHelper;
import de.tarent.telekom.cot.mqtt.util.SmartREST;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;

public class ManagedObjectHelperVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapVerticle.class);

    private MqttClient client;

    private EventBus eb;

    @Override
    public void start() throws Exception {
        eb = vertx.eventBus();

        eb.consumer("createManagedObject", msg -> {
            createManagedObject((JsonObject) msg.body(), msg);
        });
    }

    private void createManagedObject(JsonObject msg, Message handle) {
        LOGGER.info("----------------------------------------------------");

                final String deviceId = msg.getString("deviceId");
                final String password = msg.getString("cloudPassword");

                final MqttClientOptions options = new MqttClientOptions()
                    .setPassword(password)
                    .setUsername(deviceId)
                    .setAutoKeepAlive(true)
                    .setSsl(true)
                    .setTrustOptions(new JksOptions().setPath("certificates/client.jks").setPassword("kVJEgEVwn3TB9BPA"));
                final int port = 8883;
                client = MqttClient.create(vertx, options);

                client.publishHandler(h -> {
                        LOGGER.info("Message with topic " + h.topicName() + " with QOS " + h.qosLevel().name() + " received");
                        if (h.topicName().equals(msg.getString("moSubscribeTopic"))) {

                            String[] parsedPayload = SmartREST.parseResponsePayload(h.payload());
                            //object doesnt exist
                            if (parsedPayload[0].equals("50") && parsedPayload[2].equals("404")) {
                                String message = SmartREST.getPayloadSelfCreationRequest(msg.getString("xId"), msg.getString("deviceId"), "deviceName");
                                MOPublish(client, msg.getString("moPublishTopic"), message);
                            }
                            //object already exists
                            else if (parsedPayload[0].equals("601")) {
                                //601,1,mascot3,2817383;
                                //what to do when object for iccid already exists?

                            }
                            //object created we
                            if (parsedPayload[0].equals("603")) {
                                final JsonObject managedObject = new JsonObject();
                                managedObject.put("managedObjectId", parsedPayload[2]);
                                eb.publish("setConfig", managedObject);

                                String registerICCIDString = SmartREST.getPayloadRegisterICCIDasExternalId(msg.getString("xId"), parsedPayload[2], msg.getString("deviceId"));
                                MOPublish(client, msg.getString("moPublishTopic"), registerICCIDString);
                                String updateOperationsString = SmartREST.getPayloadUpdateOperations(msg.getString("xId"), parsedPayload[2]);
                                MOPublish(client, msg.getString("moPublishTopic"), updateOperationsString);
                                client.unsubscribe(msg.getString("moSubscribeTopic"));
                                client.disconnect();

                                eb.publish("managedObjectCreated", true);
                            }
                        }
                    });


                client.connect(port, "nb-iot.int2-ram.m2m.telekom.com", ch -> {
                    if (ch.succeeded()) {
                        LOGGER.info("Connected to a server");
                        client.subscribe(msg.getString("moSubscribeTopic"), MqttQoS.AT_MOST_ONCE.value(),
                            d -> {
                            });
                        MOPublish(client, msg.getString("moPublishTopic"), SmartREST.getPayloadCheckManagedObject("mascot-testdevices1", msg.getString("deviceId")));
                    } else {
                        LOGGER.error("Failed to connect to a server", ch.cause());
                    }
                });
            }


    private void MOPublish(MqttClient client, String topic, String message) {
        client.publish(
            topic,
            Buffer.buffer(message),
            MqttQoS.AT_MOST_ONCE,
            false,
            false,
            k -> {
            }
        );
    }

}
