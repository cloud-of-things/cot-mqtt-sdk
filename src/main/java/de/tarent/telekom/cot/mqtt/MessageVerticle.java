package de.tarent.telekom.cot.mqtt;

import de.tarent.telekom.cot.mqtt.util.JsonHelper;
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

import static de.tarent.telekom.cot.mqtt.util.JsonHelper.*;

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

        eventBus.consumer("unsubscribe", msg -> {
            unsubscribeFromTopic((JsonObject) msg.body(), msg);
        });
    }


    /**
     * Publish a message with the given {@link JsonObject} that should contain the message, topic and connection Strings.
     *
     * @param msg the {@link JsonObject} that contains the necessary information to publish a message
     */
    private void publishMessage(final JsonObject msg, final Message handle) {

        final MqttClientOptions options = new MqttClientOptions()
            .setPassword(msg.getString(PASSWORD_KEY))
            .setUsername(msg.getString(USER_KEY))
            .setAutoKeepAlive(true);

        JsonHelper.setSslOptions(options, msg);

        //connect and publish on /iccid
        final int port = Integer.parseInt(msg.getString(BROKER_PORT_KEY));

        if (client == null) {
            client = MqttClient.create(vertx, options);
        }
        if (client.isConnected()) {
            publish(msg, handle);
        } else {
            client.connect(port, msg.getString(BROKER_URI_KEY), ch -> {
                if (ch.succeeded()) {
                    LOGGER.info("Connected to a server");
                    publish(msg, handle);
                } else {
                    LOGGER.error("Failed to connect to a server", ch.cause());
                }
            });
        }
    }

    private void publish(final JsonObject msg, final Message handle) {
        client.publish(msg.getValue(PUBLISH_TOPIC_KEY).toString(),
            Buffer.buffer(msg.getValue(MESSAGE_KEY).toString()),
            MqttQoS.valueOf(msg.getInteger(QOS_KEY)),
            false,
            false,
            s -> {
                LOGGER.info("Publish sent to a server");
                JsonObject jso = new JsonObject().put("published", true);
                handle.reply(jso);
            });
    }

    private void subscribeToTopic(final JsonObject msg, final Message handle) {
        final MqttClientOptions options = new MqttClientOptions()
            .setPassword(msg.getString(PASSWORD_KEY))
            .setUsername(msg.getString(USER_KEY))
            .setAutoKeepAlive(true);

        JsonHelper.setSslOptions(options, msg);

        //connect and subscribe on /iccid
        final int port = Integer.parseInt(msg.getString(BROKER_PORT_KEY));

        if (client == null) {
            client = MqttClient.create(vertx, options);
        }
        EventBus eventBus = vertx.eventBus();
        //Implementation of return
        client.publishHandler(h -> {
            LOGGER.info("Message with topic " + h.topicName() + " with QOS " + h.qosLevel().name() + " received");
            if (h.topicName().equals(msg.getString(SUBSCRIBE_TOPIC_KEY))) {
                String message = h.payload().toString();
                JsonObject toCallBack = new JsonObject().put("received", message);
                eventBus.publish("received", toCallBack);
            }
        });
        if (client.isConnected()) {
            subscribe(msg, handle);
        } else {
            client.connect(port, msg.getString(BROKER_URI_KEY), ch -> {
                if (ch.succeeded()) {
                    LOGGER.info("Connected to a server");
                    subscribe(msg, handle);
                } else {
                    LOGGER.error("Failed to connect to a server", ch.cause());
                }
            });
        }
    }

    private void unsubscribeFromTopic(final JsonObject msg, final Message handle) {
        final MqttClientOptions options = new MqttClientOptions()
            .setPassword(msg.getString(PASSWORD_KEY))
            .setUsername(msg.getString(USER_KEY))
            .setAutoKeepAlive(true);

        JsonHelper.setSslOptions(options, msg);

        //connect and subscribe on /iccid
        final int port = Integer.parseInt(msg.getString(BROKER_PORT_KEY));

        if (client == null) {
            client = MqttClient.create(vertx, options);
        }

        if (client.isConnected()) {
            unsubscribe(msg, handle);
        } else {
            client.connect(port, msg.getString(BROKER_URI_KEY), ch -> {
                if (ch.succeeded()) {
                    LOGGER.info("Connected to a server");
                    unsubscribe(msg, handle);
                } else {
                    LOGGER.error("Failed to connect to a server", ch.cause());
                }
            });
        }
    }

    private void subscribe(final JsonObject msg, final Message handle) {
        client.subscribe(msg.getString(SUBSCRIBE_TOPIC_KEY), MqttQoS.valueOf(msg.getInteger(QOS_KEY)).value(), s -> {
            LOGGER.info("Subscribe call sent to a server");
            JsonObject jso = new JsonObject().put("subscribed", true);
            handle.reply(jso);
        });
    }

    private void unsubscribe(final JsonObject msg, final Message handle) {
        client.unsubscribe(msg.getString(UNSUBSCRIBE_TOPIC_KEY), s -> {
            LOGGER.info("Unsubscribe call sent to a server");
            final JsonObject jso = new JsonObject().put("unsubscribed", true);
            handle.reply(jso);
        });
    }
}
