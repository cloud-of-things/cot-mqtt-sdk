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

/**
 * {@link io.vertx.core.Verticle} for publishing messages.
 */
public class MessageVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageVerticle.class);

    private MqttClient client;

    @Override
    public void start() {
        final EventBus eventBus = vertx.eventBus();

        eventBus.consumer("publish", msg -> {
            publishMessage((JsonObject) msg.body(), msg);
        });

        eventBus.consumer("subscribe", msg -> {
            subscribeToTopic((JsonObject) msg.body(), msg);
        });
    }

    /**
     * Publish a message with the given {@link JsonObject} that should contain the message, topic and connection Strings.
     *
     * @param msg the {@link JsonObject} that contains the necessary information to publish a message
     */
    private void publishMessage(final JsonObject msg, final Message handle) {

        final MqttClientOptions options = new MqttClientOptions()
            .setPassword(msg.getString("password"))
            .setUsername(msg.getString("user"))
            .setAutoKeepAlive(true);

        //connect and publish on /iccid
        final int port = Integer.parseInt(msg.getString("brokerPort"));

        client = MqttClient.create(vertx, options);
        client.connect(port, msg.getString("brokerURI"), ch -> {
            if (ch.succeeded()) {
                LOGGER.info("Connected to a server");
                client.publish(msg.getValue("publishTopic").toString(),
                    Buffer.buffer(msg.getValue("message").toString()),
                    MqttQoS.AT_MOST_ONCE,
                    false,
                    false,
                    s -> {
                        LOGGER.info("Publish sent to a server");
                        JsonObject jso = new JsonObject().put("published", true);
                        handle.reply(jso);
                    }).disconnect();
            } else {
                LOGGER.error("Failed to connect to a server", ch.cause());
            }
        });
    }

    private void subscribeToTopic(final JsonObject msg, final Message handle) {
        final MqttClientOptions options = new MqttClientOptions()
            .setPassword(msg.getString("password"))
            .setUsername(msg.getString("user"))
            .setAutoKeepAlive(true);

        //connect and subscribe on /iccid
        final int port = Integer.parseInt(msg.getString("brokerPort"));

        client = MqttClient.create(vertx, options);
        client.connect(port, msg.getString("brokerURI"), ch -> {
            if (ch.succeeded()) {
                LOGGER.info("Connected to a server");
                client.subscribe(msg.getString("subscribeTopic"), MqttQoS.AT_MOST_ONCE.value(),
                    s -> {
                        LOGGER.info("Subscribe call sent to a server");
                        JsonObject jso = new JsonObject().put("subscribed", true);
                        handle.reply(jso);
                    }).disconnect();
            } else {
                LOGGER.error("Failed to connect to a server", ch.cause());
            }
        });
    }
}
