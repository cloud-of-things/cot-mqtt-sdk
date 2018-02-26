package de.tarent.telekom.cot.mqtt;


import de.tarent.telekom.cot.mqtt.util.MQTTTestClient;
import de.tarent.telekom.cot.mqtt.util.MQTTTestServer;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
public class MessageIT {

    static Logger logger = LoggerFactory.getLogger(MessageIT.class);
    static MQTTHelper helper;
    static MQTTTestServer server;
    static MQTTTestClient client;


    @BeforeClass
    public static void beforeClass() {
        Vertx vc = Vertx.vertx();
        MQTTTestServer server = new MQTTTestServer();
        vc.deployVerticle(server, h-> {
            if (h.succeeded()){
                MQTTTestClient client = new MQTTTestClient(false);
                vc.deployVerticle(client);
            }
        });
        helper = MQTTHelper.getInstance();
    }

    @Test
    public void testPublishMessage(final TestContext context) {
        Properties prop = new Properties();
        prop.setProperty("user", "publishUser");
        prop.setProperty("password", "somePassword");
        prop.setProperty("brokerURI", "localhost");
        prop.setProperty("brokerPort", "11883");
        final String deviceId = "testDevice";
        // Test string taken form device-simulator: SmartRestMessageBuilderTest.testMeasurementPayload();
        final String message = "15,sim770\n" + "300,name,T,89,unit,time,source,type";
        final Async async = context.async();
        helper.publishMessage(deviceId, message, prop, back -> {
            logger.info("Back:" + back);
            context.assertTrue((boolean) back);
            async.complete();
        });

        async.awaitSuccess(4000);
    }

    @Test
    public void testSubscribeToTopic(final TestContext context) {
        Properties prop = new Properties();
        prop.setProperty("user", "subscribeUser");
        prop.setProperty("password", "somePassword");
        prop.setProperty("brokerURI", "localhost");
        prop.setProperty("brokerPort", "11883");
        final String deviceId = "testDevice";
        final Async async = context.async();
        helper.subscribeToTopic(deviceId, prop, back -> {
            logger.info("Back:" + back);
            context.assertTrue((boolean) back);
            async.complete();
        }, callback -> {
            logger.info("message received");//receive message not yet realized in Helper classes, so not tested yet
            assertEquals("15,sim770\\n410,OPID1,SUCCESSFUL,result of the successful command,ln -s",
                callback.toString());
            async.complete();
        });

        async.awaitSuccess(3000);
    }

    @Test
    public void testUnsubscribeFromTopic(final TestContext context) {
        Properties prop = new Properties();
        prop.setProperty("user", "unsubscribeUser");
        prop.setProperty("password", "somePassword");
        prop.setProperty("brokerURI", "localhost");
        prop.setProperty("brokerPort", "11883");
        final String deviceId = "testDevice";
        final Async async = context.async();
        helper.unsubscribeFromTopic(deviceId, prop, back -> {
            logger.info("Back:" + back);
            context.assertTrue((boolean) back);
            async.complete();
        });

        async.awaitSuccess(3000);
    }

}
