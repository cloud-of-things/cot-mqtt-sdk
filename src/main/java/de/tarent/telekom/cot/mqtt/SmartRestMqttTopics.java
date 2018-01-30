package de.tarent.telekom.cot.mqtt;

public class SmartRestMqttTopics {

    private static final String BOOTSTRAP_REQUEST_TOPIC_PREFIX = "ss";
    private static final String BOOTSTRAP_RESPONSE_TOPIC_PREFIX = "sr";

    private static final String REQUEST_TOPIC_PREFIX = "ms";
    private static final String RESPONSE_TOPIC_PREFIX = "mr";

    //needs to be changed replaced with iccid from config
    private static final String DUMMY_ICCID = "151523234";

    public static String getBootstrapRequestTopicPrefix() {
        return BOOTSTRAP_REQUEST_TOPIC_PREFIX;
    }

    public static String getBootstrapResponseTopicPrefix() {
        return BOOTSTRAP_RESPONSE_TOPIC_PREFIX;
    }

    public static String getBootstrapRequestTopic() {
        return BOOTSTRAP_REQUEST_TOPIC_PREFIX + "/";
    }

    public static String getBootstrapResponseTopic() {
        return BOOTSTRAP_RESPONSE_TOPIC_PREFIX + "/";
    }

    public static String getRequestTopicPrefix() {
        return REQUEST_TOPIC_PREFIX;
    }

    public static String getResponseTopicPrefix() {
        return RESPONSE_TOPIC_PREFIX;
    }

    public static String getRequestTopic() {
        return REQUEST_TOPIC_PREFIX + "/";
    }

    public static String getResponseTopic() {
        return RESPONSE_TOPIC_PREFIX + "/";
    }
}
