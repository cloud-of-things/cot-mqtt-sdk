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
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Properties;
import java.util.Set;

@RunWith(VertxUnitRunner.class)
public class BootstrapIT {

    static Logger logger = LoggerFactory.getLogger(MQTTHelperIT.class);
    MQTTHelper helper;
    Vertx vertx;

    @Before
    public void before(TestContext context){
        Properties prop = new Properties();
        prop.setProperty("bootstrap.initialuser","devicebootstrap");
        prop.setProperty("bootstrap.initialpassword","Fhdt1bb1f" );
        prop.setProperty("bootstrap.brokerURI","nb-iot.int2-ram.m2m.telekom.com" );
        prop.setProperty("bootstrap.brokerPort","1883" );
        JsonObject conf = JsonHelper.from(prop);
        helper = MQTTHelper.getInstance();
        vertx = helper.getVertx();
        EventBus eb = vertx.eventBus();
        eb.publish("setConfig", conf);
        MQTTTestServer server = new MQTTTestServer();
        vertx.deployVerticle(server);
        MQTTTestClient client = new MQTTTestClient();
        vertx.deployVerticle(client);
    }

    @After
    public void after(TestContext context){
        Set<String> list = vertx.deploymentIDs();
        if (list!= null && list.size()>0) {
            list.forEach(id -> {
                logger.info(id);
                vertx.undeploy(id);
            });
        }
    }

    @Test
    public void testDeviceRegister(TestContext context){
        Properties prop = new Properties();
        prop.setProperty("bootstrap.initialuser","devicebootstrap");
        prop.setProperty("bootstrap.initialpassword","Fhdt1bb1f" );
        prop.setProperty("bootstrap.brokerURI","nb-iot.int2-ram.m2m.telekom.com" );
        prop.setProperty("bootstrap.brokerPort","1883" );
        String devId = "TestDevice";
        Async async = context.async();
        helper.registerDevice(devId, prop,back ->{
            logger.info("Back:"+back);
            context.assertTrue(((String)back).contains("status"));
            async.complete();
        });
        async.awaitSuccess(3000);
    }

}
