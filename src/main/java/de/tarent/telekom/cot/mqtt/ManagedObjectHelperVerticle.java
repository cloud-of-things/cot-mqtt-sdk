package de.tarent.telekom.cot.mqtt;

import de.tarent.telekom.cot.mqtt.util.JsonHelper;
import de.tarent.telekom.cot.mqtt.util.SmartREST;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;

import static de.tarent.telekom.cot.mqtt.util.JsonHelper.*;

public class ManagedObjectHelperVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedObjectHelperVerticle.class);
    private static final String CLOUD_PASSWORD_KEY = "cloudPassword"; //NOSONAR - This is just a key, not a password.
    private static final String XID_KEY = "xId";

    private MqttClient client;

    private EventBus eb;

    @Override
    public void start() throws Exception {
        eb = vertx.eventBus();

        eb.consumer("createManagedObject", msg -> {
            createManagedObject((JsonObject) msg.body());
        });
    }

    private void createManagedObject(JsonObject msg) {

        final String deviceId = msg.getString(DEVICE_ID_KEY);
        final String password = msg.getString(CLOUD_PASSWORD_KEY);

        final MqttClientOptions options = new MqttClientOptions()
            .setPassword(password)
            .setUsername(deviceId)
            .setAutoKeepAlive(true);

        JsonHelper.setSslOptions(options, msg);

        final int port = Integer.parseInt(msg.getString(BROKER_PORT_KEY));
        client = MqttClient.create(vertx, options);

        client.publishHandler(h -> {
            LOGGER.info("Message with topic " + h.topicName() + " with QOS " + h.qosLevel().name() + " received");
            if (h.topicName().equals(msg.getString(MO_SUBSCRIBE_TOPIC_KEY))) {

                String[] parsedPayload = SmartREST.parseResponsePayload(h.payload());
                //object doesnt exist
                if (parsedPayload[0].equals("50") && parsedPayload[2].equals("404")) {
                    String message = SmartREST.getPayloadSelfCreationRequest(msg.getString(XID_KEY), msg.getString(DEVICE_ID_KEY),
                        DEVICE_NAME_KEY);
                    MOPublish(client, msg.getString(MO_PUBLISH_TOPIC_KEY), message);
                }
                //object already exists
                else if (parsedPayload[0].equals("601")) {
                    //601,1,mascot3,2817383;
                    //what to do when object for iccid already exists?
                    //TODO what does it mean, when the object already exists? how do we handle it?
                }
                //object created we
                if (parsedPayload[0].equals("603")) {
                    final JsonObject managedObject = new JsonObject();
                    managedObject.put("managedObjectId", parsedPayload[2]);
                    eb.publish("setConfig", managedObject);

                    String registerICCIDString = SmartREST.getPayloadRegisterICCIDasExternalId(msg.getString(XID_KEY), parsedPayload[2], msg.getString("deviceId"));
                    MOPublish(client, msg.getString(MO_PUBLISH_TOPIC_KEY), registerICCIDString);
                    String updateOperationsString = SmartREST.getPayloadUpdateOperations(msg.getString(XID_KEY), parsedPayload[2]);
                    MOPublish(client, msg.getString(MO_PUBLISH_TOPIC_KEY), updateOperationsString);
                    client.unsubscribe(msg.getString(MO_SUBSCRIBE_TOPIC_KEY));
                    client.disconnect();

                    eb.publish("managedObjectCreated", managedObject);
                }
            }
        });


        client.connect(port,  msg.getString(BROKER_URI_KEY), ch -> {
            if (ch.succeeded()) {
                LOGGER.info("Connected to a server");
                client.subscribe(msg.getString(MO_SUBSCRIBE_TOPIC_KEY), MqttQoS.AT_MOST_ONCE.value(),
                    d -> {
                    });
                MOPublish(client, msg.getString(MO_PUBLISH_TOPIC_KEY), SmartREST.getPayloadCheckManagedObject("mascot-testdevices1", msg.getString(DEVICE_ID_KEY)));
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
