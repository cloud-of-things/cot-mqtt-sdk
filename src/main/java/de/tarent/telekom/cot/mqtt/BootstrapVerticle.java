package de.tarent.telekom.cot.mqtt;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
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

    private String initialUserName = "devicebootstrap";
    private String initialPassword = "Fhdt1bb1f";
    private int BROKER_PORT = 1883;
    private String BROKER_HOST = "";
    private byte[] key;

    @Override
    public void start() throws Exception {
        EventBus eb = vertx.eventBus();

        eb.consumer("register", msg -> {
            registerDevice((JsonObject) msg.body(), msg);
        });
    }

    void registerDevice(JsonObject msg, Message handle) {

        MqttClientOptions options = new MqttClientOptions().setPassword(initialPassword).setUsername(initialUserName);
        MqttClient client = MqttClient.create(vertx, options);

        Arrays.fill(key, (byte) 6);

        client.publishHandler(s -> {
            try {
                String message = new String(s.payload().getBytes(), "UTF-8");
                System.out.println(String.format("Receive message with content: \"%s\" from topic \"%s\"", message, s.topicName()));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }).subscribe(SmartRestMqttTopics.getBootstrapResponseTopic(), 0);

        //connect and publish on /iccid
        client.connect(BROKER_PORT, BROKER_HOST, ch -> {
            if (ch.succeeded()) {
                System.out.println("Connected to a server");
                client.publish(
                    SmartRestMqttTopics.getBootstrapRequestTopic(),
                    Buffer.buffer(key),
                    MqttQoS.EXACTLY_ONCE,
                    false,
                    false,
                    s -> System.out.println("Publish sent to a server"));
            } else {
                System.out.println("Failed to connect to a server");
                System.out.println(ch.cause());
            }
        });

        JsonObject replyObject = new JsonObject();
        replyObject.put("status", "registered");
        handle.reply(replyObject);

    }

}
