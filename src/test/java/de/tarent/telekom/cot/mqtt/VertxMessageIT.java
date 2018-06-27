package de.tarent.telekom.cot.mqtt;


import de.tarent.telekom.cot.mqtt.util.JsonHelper;
import de.tarent.telekom.cot.mqtt.util.MQTTTestClient;
import de.tarent.telekom.cot.mqtt.util.MQTTTestServer;
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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Properties;

import static de.tarent.telekom.cot.mqtt.util.Bootstrapped.BOOTSTRAPPED;
import static org.junit.Assert.assertEquals;

@RunWith(VertxUnitRunner.class)
public class VertxMessageIT {

    static Logger logger = LoggerFactory.getLogger(VertxMessageIT.class);
    static MQTTHelper helper;

    String serverId = "";
    String clientId = "";

    Vertx vc = Vertx.vertx();

    @Before
    public void before(TestContext ctx) {
        Async async = ctx.async(2);
        MQTTTestServer server = new MQTTTestServer();
        vc.deployVerticle(server, h-> {
            if (h.succeeded()){
                serverId = h.result();
                MQTTTestClient client = new MQTTTestClient(false);
                async.countDown();
                vc.deployVerticle(client, cl ->{
                    if (cl.succeeded()){
                        clientId = cl.result();
                        async.countDown();
                    }
                });
            }
        });
        async.awaitSuccess();
        helper = MQTTHelper.getInstance(vc);
    }

    @After
    public void after(TestContext ctx){
        Async async = ctx.async(2);
        vc.undeploy(clientId, complete ->{
            async.countDown();
        });
        vc.undeploy(serverId, complete ->{
            async.countDown();
        });
        async.awaitSuccess();
    }

    @Test
    public void testPublishMessage(final TestContext context) {
        Properties prop = new Properties();
        prop.setProperty("user", "publishUser");
        prop.setProperty("password", "somePassword");
        prop.setProperty("brokerURI", "localhost");
        prop.setProperty("brokerPort", "11883");
        prop.setProperty("ssl", "false");
        final String deviceId = "testDevice";
        // Test string taken form device-simulator: SmartRestMessageBuilderTest.testMeasurementPayload();
        final String message = "15,sim770\n" + "300,name,T,89,unit,time,source,type";
        final Async async = context.async();
        helper.publishMessage(deviceId, message, prop, back -> {
            logger.info("Back:" + back);
            context.assertTrue((boolean) back);
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
        prop.setProperty("bootstrapped", BOOTSTRAPPED.name());
        prop.setProperty("ssl", "false");

        // Add the properties to the config so that the bootstrapped value is set
        final JsonObject conf = JsonHelper.from(prop);
        final EventBus eb = helper.getVertx().eventBus();
        eb.publish("setConfig", conf);

        final String deviceId = "testDevice";
        final Async async = context.async();
        helper.subscribeToTopic(deviceId, prop, back -> {
            logger.info("Back:" + back);
            System.out.println(back);
            context.assertTrue((boolean) back);
            async.complete();
        }, callback -> {
            logger.info("message received");//receive message not yet realized in Helper classes, so not tested yet
            assertEquals("15,sim770\\n410,OPID1,SUCCESSFUL,result of the successful command,ln -s",
                callback.toString());
            async.complete();
        });

        async.awaitSuccess(5000);
    }

    @Test
    public void testUnsubscribeFromTopic(final TestContext context) {
        Properties prop = new Properties();
        prop.setProperty("user", "unsubscribeUser");
        prop.setProperty("password", "somePassword");
        prop.setProperty("brokerURI", "localhost");
        prop.setProperty("brokerPort", "11883");
        prop.setProperty("ssl", "true");
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
