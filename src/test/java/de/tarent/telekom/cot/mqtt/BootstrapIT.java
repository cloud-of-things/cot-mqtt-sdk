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
    public void before(){
        helper = MQTTHelper.getInstance();
        vertx = helper.getVertx();
        MQTTTestServer server = new MQTTTestServer();
        vertx.deployVerticle(server);
        MQTTTestClient client = new MQTTTestClient();
        vertx.deployVerticle(client);
    }

    @After
    public void after(){
        Set<String> list = vertx.deploymentIDs();
        if (list!= null && list.size()>0) {
            list.forEach(id -> {
                logger.info("to undeploy:"+id);
                vertx.undeploy(id);
            });
        }
    }

    @Test
    public void testDeviceRegister(TestContext context){
        Properties prop = new Properties();
        prop.setProperty("initialUser","devicebootstrap");
        prop.setProperty("initialPassword","Fhdt1bb1f" );
        prop.setProperty("brokerURI","localhost" );
        prop.setProperty("brokerPort","1883" );
        prop.setProperty("publish_topic", "/ss/testDevice");
        prop.setProperty("subscribe_topic", "/sr/testDevice");
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

}
