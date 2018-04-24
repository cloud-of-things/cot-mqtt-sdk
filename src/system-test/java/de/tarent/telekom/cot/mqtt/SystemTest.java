package de.tarent.telekom.cot.mqtt;

import de.tarent.telekom.cot.mqtt.util.JsonHelper;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Properties;

import static de.tarent.telekom.cot.mqtt.util.Bootstrapped.BOOTSTRAPPED;

@RunWith(VertxUnitRunner.class)
public class SystemTest {

    final static String MQTT_BROKER_HOST = "nb-iot.int2-ram.m2m.telekom.com";
    final static String MQTT_BROKER_PORT = "8883";
    final static String BOOTSTRAP_USER = "devicebootstrap";
    final static String BOOTSTRAP_PASSWORD = "Fhdt1bb1f";
    final static String BOOTSTRAP_DEVICE = "mascot-systemtests-device";

    final static String KEY_BOOTSTRAP_USER = "initialUser";
    final static String KEY_BOOTSTRAP_PW = "initialPassword";
    final static String KEY_BROKER_PORT = "brokerPort";
    final static String KEY_BROKER_URI = "brokerURI";

    final static String MSG_DEVICE = "mascot-systemtests-device";
    final static String MSG_DEVICE_USER = "mascot-systemtests-device";
    final static String MSG_DEVICE_PW = "nUvgV%8ijZ";
    final static String KEY_MSG_USER = "user";
    final static String KEY_MSG_PW = "password";
    final static String DEVICE_XID = "mascot-testdevices";
    final static String XID = "xId";

    MQTTHelper helper;
    Vertx vertx;
    SmartRESTHelper smartRESTHelper;

    Logger logger = LoggerFactory.getLogger(SystemTest.class);


    @Before
    public void before() {
        helper = MQTTHelper.getInstance();
        vertx = helper.getVertx();
        smartRESTHelper = new SmartRESTHelper();
    }

    @After
    public void after() {
        vertx.undeploy(helper.deploymentID());
    }

    @Test
    public void PublishSystemTest(TestContext context) {
        Properties prop = new Properties();
        prop.put(KEY_BROKER_URI, MQTT_BROKER_HOST);
        prop.put(KEY_BROKER_PORT, MQTT_BROKER_PORT);
        prop.put(KEY_MSG_USER, MSG_DEVICE_USER);
        prop.put(KEY_MSG_PW, MSG_DEVICE_PW);

        String message = smartRESTHelper.getPayloadMeasurement("3884676", DEVICE_XID, "300");

        Async async = context.async();
        helper.publishMessage(MSG_DEVICE, message, prop, back -> {
            logger.info("Back:" + back);
            context.assertTrue((boolean) back);
            async.complete();
        });
        async.awaitSuccess(30000);
    }

    @Test
    public void SubscribeAndPublishSystemTest(TestContext context) throws InterruptedException {
        Properties prop = new Properties();
        prop.put(KEY_BROKER_URI, MQTT_BROKER_HOST);
        prop.put(KEY_BROKER_PORT, MQTT_BROKER_PORT);
        prop.put(KEY_MSG_USER, MSG_DEVICE_USER);
        prop.put(KEY_MSG_PW, MSG_DEVICE_PW);

        Properties configProp = new Properties();
        configProp.setProperty("bootstrapped", BOOTSTRAPPED.name());

        // Add the properties to the config so that the bootstrapped value is set
        JsonObject conf = JsonHelper.from(configProp);
        EventBus eb = helper.getVertx().eventBus();
        eb.publish("setConfig", conf);
        Thread.sleep(2000);
        Async async = context.async();

        Async async2 = context.async();

        String message = smartRESTHelper.getPayloadMeasurement("3884676", DEVICE_XID, "300");

        helper.subscribeToTopic(MSG_DEVICE, prop, back -> {
                helper.publishMessage(MSG_DEVICE, message, prop, back2 -> {
                    logger.info("Back:" + back2);
                    context.assertTrue((boolean) back2);
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

    @Test
    public void BootstrapSystemTest(TestContext context) {
        Properties prop = new Properties();
        prop.put(KEY_BROKER_URI, MQTT_BROKER_HOST);
        prop.put(KEY_BROKER_PORT, MQTT_BROKER_PORT);
        prop.put(KEY_BOOTSTRAP_USER, BOOTSTRAP_USER);
        prop.put(KEY_BOOTSTRAP_PW, BOOTSTRAP_PASSWORD);
        prop.put(XID,DEVICE_XID);

        Async async = context.async();
        helper.registerDevice(BOOTSTRAP_DEVICE, prop, back -> {
            logger.info("Back:" + back);
            context.assertNotNull(back);
            async.complete();
        });
        async.awaitSuccess(300000);
    }
}
