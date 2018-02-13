package de.tarent.telekom.cot.mqtt;

import de.tarent.telekom.cot.mqtt.util.EncryptionHelper;
import de.tarent.telekom.cot.mqtt.util.Secret;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;


public class BootstrapVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapVerticle.class);

    private MqttClient client;

    @Override
    public void start() throws Exception {
        EventBus eb = vertx.eventBus();

        eb.consumer("register", msg -> {
            registerDevice((JsonObject) msg.body(), msg);
        });
    }

    void registerDevice(JsonObject msg, Message handle) {
        LOGGER.info(msg.encodePrettily());
        MqttClientOptions options = new MqttClientOptions().setPassword(msg.getString("initialPassword")).setUsername(msg.getString("initialUser")).setAutoKeepAlive(true);
        client = MqttClient.create(vertx, options);

        client.publishHandler(s -> {
                if (s.topicName().equals(msg.getString("subscribe_topic"))) {
                    LOGGER.info(String.format("Receive message with content: \"%s\" from topic \"%s\"", s.payload().toString("utf-8"), s.topicName()));
                    EncryptionHelper ech = new EncryptionHelper();
                    byte[] pass = ech.decrypt(new Secret(msg.getString("message")),s.payload().getBytes());
                    client.disconnect();
                    JsonObject replyObject = new JsonObject();
                    replyObject.put("status", "registered");
                    replyObject.put("credentials", new String(pass));
                    handle.reply(replyObject);
                    //write to config that bootstrap process is done
                    EventBus eventBus = vertx.eventBus();
                    JsonObject bootStrapDoneMessage = new JsonObject();
                    bootStrapDoneMessage.put("bootstrapped", true);
                    eventBus.publish("setConfig", bootStrapDoneMessage);
                }
        });

        //connect and publish on /iccid
        int port = Integer.parseInt(msg.getString("brokerPort"));
        client.connect(port, msg.getString("brokerURI"), ch -> {
            if (ch.succeeded()) {
                System.out.println("Connected to a server");
                client.publish(
                    msg.getValue("publish_topic").toString(),
                    Buffer.buffer(msg.getValue("message").toString()),
                    MqttQoS.AT_LEAST_ONCE,
                    false,
                    false,
                    s -> LOGGER.info("Publish sent to a server"));

                //write to config that bootstrap process has started
                EventBus eventBus = vertx.eventBus();
                JsonObject bootStrapPendingMessage = new JsonObject();
                bootStrapPendingMessage.put("bootstrapped", false);
                eventBus.publish("setConfig", bootStrapPendingMessage);

                client.subscribe(msg.getString("subscribe_topic"), MqttQoS.AT_LEAST_ONCE.value());

            } else {
                LOGGER.error("Failed to connect to a server", ch.cause());

            }

        });
    }

}
