package de.tarent.telekom.cot.mqtt;


import de.tarent.telekom.cot.mqtt.util.MQTTTestClient;
import de.tarent.telekom.cot.mqtt.util.MQTTTestServer;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Properties;
import java.util.Set;

@RunWith(VertxUnitRunner.class)
public class BootstrapIT {

    static Logger logger = LoggerFactory.getLogger(BootstrapIT.class);
    MQTTHelper helper;
    Vertx vertx;

    @Before
    public void before(final TestContext context) {
        helper = MQTTHelper.getInstance();
        vertx = helper.getVertx();
        MQTTTestServer server = new MQTTTestServer();
        vertx.deployVerticle(server, context.asyncAssertSuccess(s -> System.out.println("Server deployed: " + s)));

        MQTTTestClient client = new MQTTTestClient();
        vertx.deployVerticle(client, context.asyncAssertSuccess(s -> System.out.println("Client deployed: " + s)));
    }

    @After
    public void after(final TestContext context) {
        Set<String> list = vertx.deploymentIDs();
        if (list != null && list.size() > 0) {
            list.forEach(id -> {
                logger.info("to undeploy:" + id);
                vertx.undeploy(id, context.asyncAssertSuccess(s -> System.out.println("Verticle undeployed: " + s)));
            });
        }
    }

    @Test
    public void testDeviceRegister(TestContext context) {
        Properties prop = new Properties();
        prop.setProperty("initialUser", "devicebootstrap");
        prop.setProperty("initialPassword", "Fhdt1bb1f");
        prop.setProperty("brokerURI", "localhost");
        prop.setProperty("brokerPort", "11883");
        prop.setProperty("message", "test1234567890ab");
        String devId = "testDevice";
        Async async = context.async();
        helper.registerDevice(devId, prop,back ->{
            logger.info("Back:"+back);
            context.assertTrue(((String)back).contains("status"));
            async.complete();
        });
        async.awaitSuccess(3000);
    }

    //having a secret is equal to an ongoing bootstrapping process
    public void testDeviceWithExistingSecret(TestContext context){
        Properties prop = new Properties();
        prop.setProperty("initialUser", "devicebootstrap");
        prop.setProperty("initialPassword", "Fhdt1bb1f");
        prop.setProperty("brokerURI", "localhost");
        prop.setProperty("brokerPort", "11883");
        prop.setProperty("secret", "1234567890abcdef");
        prop.setProperty("bootstrapped", "ongoing");
        String devId = "testDevice";
        Async async = context.async();
        helper.registerDevice(devId, prop, back -> {
            logger.info("Back:" + back);
            context.assertTrue(((String) back).contains("status"));
            async.complete();
        });
        async.awaitSuccess(3000);
    }

}
