package de.tarent.telekom.cot.mqtt.util;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttServerOptions;
import io.vertx.mqtt.MqttTopicSubscription;

import java.util.ArrayList;
import java.util.List;


public class MQTTTestServer extends AbstractVerticle {

    // Convenience method so you can run it in your IDE
    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new MQTTTestServer());
    }

    @Override
    public void start() throws Exception {

        MqttServerOptions options = new MqttServerOptions()
                .setPort(1883)
                .setHost("0.0.0.0");

        MqttServer server = MqttServer.create(vertx, options);

        server.endpointHandler(endpoint -> {

            System.out.println("connected client " + endpoint.clientIdentifier());

            endpoint.publishHandler(message -> {

                System.out.println("Just received message on [" + message.topicName() + "] payload [" +
                        message.payload() + "] with QoS [" +
                        message.qosLevel() + "]");
                endpoint.publish(message.topicName(),message.payload(), message.qosLevel(), false, false);
            });

            endpoint.accept(false);
        });

        server.listen(ar -> {
            if (ar.succeeded()) {
                System.out.println("MQTT server started and listening on port " + server.actualPort());
            } else {
                System.err.println("MQTT server error on start" + ar.cause().getMessage());
            }
        });
    }
}