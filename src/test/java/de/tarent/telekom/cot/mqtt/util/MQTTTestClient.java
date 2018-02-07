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

    private static final String MQTT_TOPIC = "/my_topic";
    private static final String MQTT_TESTSUBSCRIPTION = "/ss/testDevice";
    private static final String MQTT_TESTPUBLISH = "/sr/testDevice";
    private static final String SECRETKEY = "simple1234567890";
    private static final String TESTPW = "testPW";
    private static final String MQTT_MESSAGE = "Hello Vert.x MQTT Client";
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
            logger.info(h.payload().toString());
            if (h.topicName().equals(MQTT_TESTSUBSCRIPTION)) {
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
            if (h.topicName().equals(MQTT_TESTPUBLISH)) {
                Buffer plkey = h.payload();
                Secret secret = new Secret(SECRETKEY);
                EncryptionHelper encHelper = new EncryptionHelper();
                byte[] received = encHelper.decrypt(secret, plkey.getBytes());
                logger.info("encrypted PW: "+new String(received));

            }
        });
        mqttClient.connect(BROKER_PORT, BROKER_HOST, ch -> {
            if (ch.succeeded()) {
                logger.info("Connected to a server");
                mqttClient.subscribe(MQTT_TOPIC,MqttQoS.AT_LEAST_ONCE.value());
                mqttClient.subscribe(MQTT_TESTSUBSCRIPTION, MqttQoS.AT_LEAST_ONCE.value());
//                mqttClient.subscribe(MQTT_TESTPUBLISH, MqttQoS.AT_LEAST_ONCE.value());
//                mqttClient.publish(
//                        MQTT_TOPIC,
//                        Buffer.buffer(MQTT_MESSAGE),
//                        MqttQoS.AT_MOST_ONCE,
//                        false,
//                        false);
//                mqttClient.publish(
//                        MQTT_TESTSUBSCRIPTION,
//                        Buffer.buffer(SECRETKEY),
//                        MqttQoS.AT_MOST_ONCE,
//                        false,
//                        false
//                );


            } else {
                logger.error("Failed to connect to a server", ch.cause());
            }
        });

    }


}
