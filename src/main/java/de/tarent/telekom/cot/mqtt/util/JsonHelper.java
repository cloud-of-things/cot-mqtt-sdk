package de.tarent.telekom.cot.mqtt.util;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.mqtt.MqttClientOptions;

import java.util.Properties;

/**
 * Helper classes for handling {@link JsonObject} key value pairs. Including but not limited to: converting a given
 * {@link Properties} to a complete {@link JsonObject}, setting default values for a key value pair, checking key
 * existence, etc.
 */
public class JsonHelper {
    static final String DEFAULT_KEYSTORE_PATH = "certificates/client.jks";
    static final String DEFAULT_KEYSTORE_PASSWORD = "kVJEgEVwn3TB9BPA";

    private static final Logger logger = LoggerFactory.getLogger(JsonHelper.class);

    /**
     * Converts the given {@link Properties} into a {@link JsonObject} as simple key value pairs. All values are used as
     * Strings.
     *
     * @param prop the {@link Properties} to convert
     * @return the converted {@link JsonObject}
     */
    public static JsonObject from(final Properties prop) {
        final JsonObject out = new JsonObject();
        prop.stringPropertyNames().forEach(k -> {
            out.put(k, prop.getProperty(k));
        });
        return out;
    }

    /**
     * Returns an integer value based on the given JsonObject message value of QoS.
     *
     * @param msg the JsonObject that might contain a "QoS" key value pair
     * @return the value of the "QoS" key or 0 if any invalid values are given
     */
    public static int getQoSValue(final JsonObject msg) {
        int qualityOfService = 0;

        try {
            qualityOfService = Integer.parseInt(msg.getString("QoS"));
        } catch (final NumberFormatException e) {
            logger.error("Error while parsing QoS value, using default value of 0.");
        }

        for (final MqttQoS mqttQoS : MqttQoS.values()) {
            if (qualityOfService == mqttQoS.value()) {
                return qualityOfService;
            }
        }

        return MqttQoS.AT_MOST_ONCE.value();
    }

    /**
     * Returns the correct SSL value based on what the "ssl" key value is in the JsonObject given.
     *
     * @param msg the JsonObject that might contain a "ssl" key value pair
     * @return false if "ssl" is set to false, otherwise true
     */
    public static boolean getSslValue(JsonObject msg) {
        final String ssl = msg.getString("ssl");

        // We don't need the user to set this, it should always be true. We only set it to false for tests.
        if (ssl == null) {
            return true;
        } else {
            return Boolean.parseBoolean(ssl);
        }
    }

    /**
     * Sets the options for SSL by first checking if SSL is set to true and then checking if a key store path and
     * password were given.
     *
     * @param options the {@link MqttClientOptions}
     * @param msg The JsonObject message that contains the information we want to check
     */
    public static void setSslOptions(final MqttClientOptions options, final JsonObject msg) {
        if (msg.getBoolean("ssl") == null || msg.getBoolean("ssl")) {
            options
                .setSsl(true)
                .setTrustOptions(new JksOptions()
                    .setPath(msg.containsKey("keyStorePath") ? msg.getString("keyStorePath") : DEFAULT_KEYSTORE_PATH)
                    .setPassword(msg.containsKey("keyStorePassword") ? msg.getString("keyStorePassword") :
                        DEFAULT_KEYSTORE_PASSWORD)
                );
        }
    }
}
