package de.tarent.telekom.cot.mqtt;

public class SmartRestMqttTopics {

    private SmartRestMqttTopics() {
        // Private constructor to prevent instantiation.
    }

    private static final String BOOTSTRAP_REQUEST_TOPIC_PREFIX = "ss";
    private static final String BOOTSTRAP_RESPONSE_TOPIC_PREFIX = "sr";

    private static final String REQUEST_TOPIC_PREFIX = "ms";
    private static final String RESPONSE_TOPIC_PREFIX = "mr";

    public static String getBootstrapRequestTopicPrefix() {
        return BOOTSTRAP_REQUEST_TOPIC_PREFIX;
    }

    public static String getBootstrapResponseTopicPrefix() {
        return BOOTSTRAP_RESPONSE_TOPIC_PREFIX;
    }

    public static String getBootstrapRequestTopic(String iccid) {
        return BOOTSTRAP_REQUEST_TOPIC_PREFIX + "/" + iccid;
    }

    public static String getBootstrapResponseTopic(String iccid) {
        return BOOTSTRAP_RESPONSE_TOPIC_PREFIX + "/" + iccid;
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
