package de.tarent.telekom.cot.mqtt;


import de.tarent.telekom.cot.mqtt.util.MQTTTestClient;
import de.tarent.telekom.cot.mqtt.util.MQTTTestServer;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
public class MessageIT {

    static Logger logger = LoggerFactory.getLogger(MessageIT.class);
    static MQTTHelper helper;
    static MQTTTestServer server;
    static MQTTTestClient client;
    static Vertx vertx;

    @BeforeClass
    public static void beforeClass() {
        helper = MQTTHelper.getInstance();
        vertx = helper.getVertx();
        server = new MQTTTestServer();
        vertx.deployVerticle(server);
        client = new MQTTTestClient();
        vertx.deployVerticle(client);
    }

    @After
    public void after(final TestContext context) {
        final Set<String> list = vertx.deploymentIDs();
        if (list != null && list.size() > 0) {
            list.forEach(id -> {
                logger.info("to undeploy:" + id);
                vertx.undeploy(id, context.asyncAssertSuccess(s -> System.out.println("Verticle undeployed: " + s)));
            });
        }
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
            assertTrue(back.toString().contains("published"));
            async.complete();
        });

        async.awaitSuccess(3000);
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
            assertTrue(back.toString().contains("subscribed"));
            async.complete();
        }, callback -> {
            logger.info("message received");//receive message not yet realized in Helper classes, so not tested yet
            assertEquals("15,sim770\\n410,OPID1,SUCCESSFUL,result of the successful command,ln -s",
                callback.toString());
            async.complete();
        });

        async.awaitSuccess(3000);
    }

}
