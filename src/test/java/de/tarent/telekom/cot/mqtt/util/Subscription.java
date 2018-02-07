package de.tarent.telekom.cot.mqtt.util;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.mqtt.MqttEndpoint;

public class Subscription {

    MqttEndpoint endpoint;
    String topic;
    MqttQoS qos;

    public Subscription(String topic, MqttEndpoint endpoint, MqttQoS qos){
        this.topic = topic;
        this.endpoint = endpoint;
        this.qos = qos;
    }

    public MqttEndpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(MqttEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public MqttQoS getQos() {
        return qos;
    }

    public void setQos(MqttQoS qos) {
        this.qos = qos;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
