package de.tarent.telekom.cot.mqtt.util;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.mqtt.MqttClient;

public class MQTTTestClient extends AbstractVerticle {

    private static final String MQTT_TOPIC = "/my_topic";
    private static final String MQTT_MESSAGE = "Hello Vert.x MQTT Client";
    private static final String BROKER_HOST = "localhost";
    private static final int BROKER_PORT = 1883;

    Logger logger = LoggerFactory.getLogger(MQTTTestClient.class);

    public static void main(String[] args){
        Vertx.vertx().deployVerticle(new MQTTTestClient());
    }

    @Override
    public void start() throws Exception {
        MqttClient mqttClient = MqttClient.create(vertx);
        mqttClient.publishHandler(h ->{
            logger.info(h.payload().toString());
        });
        mqttClient.connect(BROKER_PORT, BROKER_HOST, ch -> {
            if (ch.succeeded()) {
                System.out.println("Connected to a server");
                mqttClient.subscribe(MQTT_TOPIC,MqttQoS.EXACTLY_ONCE.value());
                mqttClient.publish(
                        MQTT_TOPIC,
                        Buffer.buffer(MQTT_MESSAGE),
                        MqttQoS.AT_MOST_ONCE,
                        false,
                        false);
            } else {
                System.out.println("Failed to connect to a server");
                System.out.println(ch.cause());
            }
        });

    }


}
