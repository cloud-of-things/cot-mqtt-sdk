package de.tarent.telekom.cot.mqtt;

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
import org.bouncycastle.util.Arrays;

import java.io.UnsupportedEncodingException;


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
            try {
                if (s.topicName().equals(msg.getString("subscribe_topic"))) {
                    String message = new String(s.payload().getBytes(), "UTF-8");
                    LOGGER.info(String.format("Receive message with content: \"%s\" from topic \"%s\"", message, s.topicName()));
                    //client.disconnect();
                    JsonObject replyObject = new JsonObject();
                    replyObject.put("status", "registered");
                    replyObject.put("credentials", message);
                    handle.reply(replyObject);
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
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
            } else {
                LOGGER.error("Failed to connect to a server", ch.cause());

            }
            client.subscribe(msg.getString("subscribe_topic"), MqttQoS.AT_LEAST_ONCE.value());

        });
    }

}
