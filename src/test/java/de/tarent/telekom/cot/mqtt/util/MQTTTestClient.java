package de.tarent.telekom.cot.mqtt.util;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;

public class MQTTTestClient extends AbstractVerticle {

    private static final String MQTT_TESTSUBSCRIPTION = "/ss/testDevice";
    private static final String MQTT_TESTPUBLISH = "/sr/testDevice";
    private static final String TESTPW = "testPW";
    private static final String BROKER_HOST = "localhost";
    private static final int BROKER_PORT = 1883;

    Logger logger = LoggerFactory.getLogger(MQTTTestClient.class);

    public static void main(String[] args){
        Vertx.vertx().deployVerticle(new MQTTTestClient());
    }

    @Override
    public void start() throws Exception {
        MqttClientOptions option = new MqttClientOptions();
        option.setUsername("testuser");
        option.setPassword("initPW");
        MqttClient mqttClient = MqttClient.create(vertx, option);
        mqttClient.publishHandler(h->{
            if (h.topicName().equals(MQTT_TESTSUBSCRIPTION)) {
                logger.info("Message for "+ MQTT_TESTSUBSCRIPTION + " received.");
                Buffer plkey = h.payload();
                Secret secret = new Secret(plkey.getBytes());
                EncryptionHelper encHelper = new EncryptionHelper();
                byte[] toSend = encHelper.encrypt(secret, TESTPW.getBytes());
                mqttClient.publish(MQTT_TESTPUBLISH,
                        Buffer.buffer(toSend),
                        MqttQoS.AT_LEAST_ONCE,
                        false,
                        false,
                        finishHandler -> {
                            if (finishHandler.succeeded()) {
                                logger.info(finishHandler.result());
                            } else {
                                logger.error("Error during publishing password", finishHandler.cause());
                            }
                        });
            }
        });
        mqttClient.connect(BROKER_PORT, BROKER_HOST, ch -> {
            if (ch.succeeded()) {
                logger.info("Connected to a server");
                mqttClient.subscribe(MQTT_TESTSUBSCRIPTION, MqttQoS.AT_LEAST_ONCE.value());
            } else {
                logger.error("Failed to connect to a server", ch.cause());
            }
        });

    }


}
