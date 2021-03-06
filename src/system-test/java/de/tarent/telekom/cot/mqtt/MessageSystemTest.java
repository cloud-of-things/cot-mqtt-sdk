package de.tarent.telekom.cot.mqtt;

import de.tarent.telekom.cot.mqtt.util.JsonHelper;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Properties;

import static de.tarent.telekom.cot.mqtt.util.Bootstrapped.BOOTSTRAPPED;
import static de.tarent.telekom.cot.mqtt.util.JsonHelper.*;

@RunWith(VertxUnitRunner.class)
public class MessageSystemTest {

    private static final Logger logger = LoggerFactory.getLogger(MessageSystemTest.class);

    private static final String MQTT_BROKER_HOST = "nb-iot.int2-ram.m2m.telekom.com";
    private static final String MQTT_BROKER_PORT = "8883";

    private final static String MSG_DEVICE = "mascot-systemtests-device";
    private final static String MSG_DEVICE_USER = "mascot-systemtests-device";
    private final static String MSG_DEVICE_PW = "nUvgV%8ijZ";
    final static String DEVICE_XID = "mascot-testdevices";
    final static String XID = "xId";

    private MQTTHelper helper;
    private SmartRESTHelper smartRESTHelper;


    @Before
    public void before() {
        helper = MQTTHelper.getInstance();
        smartRESTHelper = new SmartRESTHelper();
    }

    @Test
    public void publish(final TestContext context) {
        final Properties prop = new Properties();
        prop.put(BROKER_URI_KEY, MQTT_BROKER_HOST);
        prop.put(BROKER_PORT_KEY, MQTT_BROKER_PORT);
        prop.put(USER_KEY, MSG_DEVICE_USER);
        prop.put(PASSWORD_KEY, MSG_DEVICE_PW);

        String message = smartRESTHelper.getPayloadMeasurement("3884676", DEVICE_XID, "300");

        final Async async = context.async();
        helper.publishMessage(MSG_DEVICE, message, prop, back -> {
            logger.info("Back:" + back);
            context.assertTrue(back);
            async.complete();
        });
        async.awaitSuccess(30000);
    }

    @Test
    public void publishAndCheckSuccess(final TestContext context) {
        final Properties prop = new Properties();
        prop.put(BROKER_URI_KEY, MQTT_BROKER_HOST);
        prop.put(BROKER_PORT_KEY, MQTT_BROKER_PORT);
        prop.put(USER_KEY, MSG_DEVICE_USER);
        prop.put(PASSWORD_KEY, MSG_DEVICE_PW);

        String message = smartRESTHelper.getPayloadMeasurement("3884676", DEVICE_XID, "300");

        final Async async = context.async(30);
        for (int i = 0 ; i < 30; i++ ) {

            final int count = i;
            helper.publishMessage(MSG_DEVICE, message, prop, back -> {
                logger.info("Back"+ count+": " + back);
                context.assertTrue(back);
                async.countDown();
            });
        }
        async.awaitSuccess(30000);
    }

    @Test
    public void subscribeAndPublish(final TestContext context) throws InterruptedException {
        final Properties prop = new Properties();
        prop.put(BROKER_URI_KEY, MQTT_BROKER_HOST);
        prop.put(BROKER_PORT_KEY, MQTT_BROKER_PORT);
        prop.put(USER_KEY, MSG_DEVICE_USER);
        prop.put(PASSWORD_KEY, MSG_DEVICE_PW);

        final Properties configProp = new Properties();
        configProp.setProperty("bootstrapped", BOOTSTRAPPED.name());

        // Add the properties to the config so that the bootstrapped value is set
        final JsonObject conf = JsonHelper.from(configProp);
        final EventBus eb = helper.getVertx().eventBus();
        eb.publish("setConfig", conf);
        Thread.sleep(2000);
        final Async async = context.async();

        final Async async2 = context.async();

        String message = smartRESTHelper.getPayloadMeasurement("3884676", DEVICE_XID, "300");

        helper.subscribeToTopic(MSG_DEVICE, prop, back -> {
                helper.publishMessage(MSG_DEVICE, message, prop, back2 -> {
                    logger.info("Back:" + back2);
                    context.assertTrue(back2);
                    async2.complete();
                });
            },
            callback -> {
                logger.info("Back:" + callback);
                async.complete();
            });
        async.awaitSuccess(30000);
        async2.awaitSuccess();
    }
}
