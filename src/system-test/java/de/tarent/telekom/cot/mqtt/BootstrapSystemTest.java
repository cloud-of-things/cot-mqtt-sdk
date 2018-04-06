package de.tarent.telekom.cot.mqtt;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static de.tarent.telekom.cot.mqtt.util.JsonHelper.*;

public class BootstrapSystemTest {

    private static final Logger logger = LoggerFactory.getLogger(BootstrapSystemTest.class);

    private static final String MQTT_BROKER_HOST = "nb-iot.int2-ram.m2m.telekom.com";
    private static final String MQTT_BROKER_PORT = "8883";
    private static final String BOOTSTRAP_USER = "devicebootstrap";
    private static final String BOOTSTRAP_PASSWORD = "Fhdt1bb1f";
    private static final String BOOTSTRAP_DEVICE = "mascot4";

    private static final String DEVICE_XID = "mascot-testdevices1";
    private static final String XID_KEY = "xId";

    private MQTTHelper helper;

    @Before
    public void before() {
        helper = MQTTHelper.getInstance();
    }

    @Test
    public void bootstrap(final TestContext context) {
        final Properties prop = new Properties();
        prop.put(BROKER_URI_KEY, MQTT_BROKER_HOST);
        prop.put(BROKER_PORT_KEY, MQTT_BROKER_PORT);
        prop.put(USER_KEY, BOOTSTRAP_USER);
        prop.put(PASSWORD_KEY, BOOTSTRAP_PASSWORD);
        prop.put(XID_KEY, DEVICE_XID);

        final Async async = context.async();
        helper.registerDevice(BOOTSTRAP_DEVICE, prop, back -> {
            logger.info("Back:" + back);
            context.assertNotNull(back);
            async.complete();
        });
        async.awaitSuccess(300000);
    }
}
