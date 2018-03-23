package de.tarent.telekom.cot.mqtt.util;

import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.mqtt.MqttClientOptions;
import org.junit.Test;

import java.util.Properties;

import static de.tarent.telekom.cot.mqtt.util.JsonHelper.*;
import static io.netty.handler.codec.mqtt.MqttQoS.*;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class JsonHelperTest {

    @Test
    public void testFromParsesAllValues() {
        final Properties properties = new Properties();
        properties.setProperty("integer", "1");
        properties.setProperty("string", "letter");
        properties.setProperty("boolean", "true");

        final JsonObject msg = JsonHelper.from(properties);

        assertEquals(3, msg.size());
        assertEquals("letter", msg.getString("string"));
        assertEquals(1, Integer.parseInt(msg.getString("integer")));
        assertEquals(true, Boolean.parseBoolean(msg.getString("boolean")));
    }

    @Test
    public void testGetQoSValue() {
        final JsonObject msg = new JsonObject();
        assertEquals(AT_MOST_ONCE.value(), getQoSValue(msg));

        msg.put("QoS", "asdfghjkl");
        assertEquals(AT_MOST_ONCE.value(), getQoSValue(msg));

        msg.put("QoS", "0");
        assertEquals(AT_MOST_ONCE.value(), getQoSValue(msg));

        msg.put("QoS", "1");
        assertEquals(AT_LEAST_ONCE.value(), getQoSValue(msg));

        msg.put("QoS", "2");
        assertEquals(EXACTLY_ONCE.value(), getQoSValue(msg));

        msg.put("QoS", "3");
        assertEquals(AT_MOST_ONCE.value(), getQoSValue(msg));
    }

    @Test
    public void testGetSslValue() {
        final JsonObject msg = new JsonObject();
        assertTrue(getSslValue(msg));

        msg.put("ssl", "true");
        assertTrue(getSslValue(msg));

        msg.put("ssl", "false");
        assertFalse(getSslValue(msg));

        msg.put("ssl", "asdfghjkl");
        assertFalse(getSslValue(msg));
    }

    @Test
    public void testSetSslOptions() {
        final JsonObject msg = new JsonObject();
        MqttClientOptions options = new MqttClientOptions();

        // Default values
        setSslOptions(options, msg);
        assertTrue(options.isSsl());
        assertEquals(DEFAULT_KEYSTORE_PATH, ((JksOptions) options.getTrustOptions()).getPath());
        assertEquals(DEFAULT_KEYSTORE_PASSWORD, ((JksOptions) options.getTrustOptions()).getPassword());

        // SSL false
        options = new MqttClientOptions(); // reset the options
        msg.put("ssl", false);
        setSslOptions(options, msg);
        assertFalse(options.isSsl());
        assertNull(options.getTrustOptions());

        // SSL true with default key store path and password
        options = new MqttClientOptions(); // reset the options
        msg.put("ssl", true);
        setSslOptions(options, msg);
        assertTrue(options.isSsl());
        assertEquals(DEFAULT_KEYSTORE_PATH, ((JksOptions) options.getTrustOptions()).getPath());
        assertEquals(DEFAULT_KEYSTORE_PASSWORD, ((JksOptions) options.getTrustOptions()).getPassword());

        // SSL false with set credentials
        options = new MqttClientOptions(); // reset the options
        msg.put("ssl", false);
        msg.put("keyStorePath", "some/hidden/path/to/client.jks");
        msg.put("keyStorePassword", "superAwesomePassword");
        setSslOptions(options, msg);
        assertFalse(options.isSsl());
        assertNull(options.getTrustOptions());

        // SSL true with set credentials
        options = new MqttClientOptions(); // reset the options
        msg.put("ssl", true);
        msg.put("keyStorePath", "some/hidden/path/to/client.jks");
        msg.put("keyStorePassword", "superAwesomePassword");
        setSslOptions(options, msg);
        assertTrue(options.isSsl());
        assertEquals("some/hidden/path/to/client.jks", ((JksOptions) options.getTrustOptions()).getPath());
        assertEquals("superAwesomePassword", ((JksOptions) options.getTrustOptions()).getPassword());
    }
}
