package de.tarent.telekom.cot.mqtt.util;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttServerOptions;

import java.util.ArrayList;
import java.util.List;


public class MQTTTestServer extends AbstractVerticle {

    Logger logger = LoggerFactory.getLogger(MQTTTestServer.class);
    // Convenience method so you can run it in your IDE
    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new MQTTTestServer());
    }

    @Override
    public void start() throws Exception {

        MqttServerOptions options = new MqttServerOptions()
                .setPort(11883)
                .setHost("0.0.0.0");

        MqttServer server = MqttServer.create(vertx, options);
        List<Subscription> subscriptions = new ArrayList<>();

        server.endpointHandler(endpoint -> {

            logger.info("connected client " + endpoint.clientIdentifier());
            if (endpoint.auth() != null){
                logger.info(endpoint.auth().toJson().encodePrettily());
            }
            endpoint.publishHandler(message -> {
                logger.info("Just received message on [" + message.topicName() + "] with QoS [" +
                        message.qosLevel() + "]");
                subscriptions.forEach(s ->{
                    if (s.getTopic().equals(message.topicName())){
                        s.getEndpoint().publish(message.topicName(),message.payload(), s.getQos(), false, false);
                    }
                });
            });
            endpoint.subscribeHandler(sh ->{
               sh.topicSubscriptions().forEach(ts ->{
                   Subscription s = new Subscription(ts.topicName(), endpoint, ts.qualityOfService());
                   subscriptions.add(s);
                   logger.info(ts.topicName()+" with QOS " + ts.qualityOfService().name() +" subscribed");
               });
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