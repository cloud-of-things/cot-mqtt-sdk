package de.tarent.telekom.cot.mqtt;


import de.tarent.telekom.cot.mqtt.util.MQTTTestServer;
import io.vertx.core.Vertx;
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
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
public class MessageIT {

    static Logger logger = LoggerFactory.getLogger(MessageIT.class);
    MQTTHelper helper;
    MQTTTestServer server;
    Vertx vertx;

    @Before
    public void before() {
        helper = MQTTHelper.getInstance();
        vertx = helper.getVertx();
        server = new MQTTTestServer();
        vertx.deployVerticle(server);
    }

    @After
    public void after() {
        Set<String> list = vertx.deploymentIDs();
        if (list != null && list.size() > 0) {
            list.forEach(id -> {
                logger.info("to undeploy:" + id);
                vertx.undeploy(id);
            });
        }
    }

    @Test
    public void testPublishMessage(final TestContext context) {
        Properties prop = new Properties();
        prop.setProperty("user", "publishUser");
        prop.setProperty("password", "somePassword");
        prop.setProperty("brokerURI", "localhost");
        prop.setProperty("brokerPort", "1883");
        final String topic = "/mr/testDevice";
        final String message = "test1234567890ab";
        final Async async = context.async();
        helper.publishMessage(topic, message, prop, back -> {
            logger.info("Back:" + back);
            assertTrue(back.toString().contains("published"));
            assertFalse(back.toString().contains("subscribed"));
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
        prop.setProperty("brokerPort", "1883");
        final String topic = "/ms/testDevice";
        final Async async = context.async();
        helper.subscribeToTopic(topic, prop, back -> {
            logger.info("Back:" + back);
            assertTrue(back.toString().contains("subscribed"));
            async.complete();
        }, callback ->{
        		logger.info("message received");//receive message not yet realized in Helper classes, so not tested yet
        });

        async.awaitSuccess(3000);
    }

}
