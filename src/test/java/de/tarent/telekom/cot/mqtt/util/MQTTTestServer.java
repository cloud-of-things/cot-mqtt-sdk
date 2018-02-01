package de.tarent.telekom.cot.mqtt.util;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttTopicSubscription;

import java.util.ArrayList;
import java.util.List;


public class MQTTTestServer {

    Vertx vertx = Vertx.vertx();
    Logger logger = LoggerFactory.getLogger(MQTTTestServer.class);

    final static String USER = "User";
    final static String PASSWORD = "Password";
    final static String ENCRYPTWITH = "";


    public void startServer(){
        MqttServer server = MqttServer.create(vertx);
        server.endpointHandler(endpoint ->{
            logger.info("MQTT-Client "+ endpoint.clientIdentifier()+" connected!");
            endpoint.subscribeHandler(sub ->{
                List<MqttQoS> grantedQosLevels = new ArrayList<>();
                for (MqttTopicSubscription s: sub.topicSubscriptions()) {
                    System.out.println("Subscription for " + s.topicName() + " with QoS " + s.qualityOfService());
                    grantedQosLevels.add(s.qualityOfService());
                }
                // ack the subscriptions request
                endpoint.subscribeAcknowledge(sub.messageId(), grantedQosLevels);
            });

        }).listen(ar ->{
            if (ar.succeeded()){
                logger.info("MQTT server is listening on port " + ar.result().actualPort());
            }
        });
    }


}
