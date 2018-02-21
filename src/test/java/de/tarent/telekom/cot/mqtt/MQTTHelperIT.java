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
import java.util.Set;


@RunWith(VertxUnitRunner.class)
public class MQTTHelperIT {

    static Logger logger = LoggerFactory.getLogger(MQTTHelperIT.class);
    MQTTHelper helper;
    Vertx vertx;

    @Before
    public void before(){
        Properties prop = new Properties();
        prop.setProperty("bootstrap.initialuser","devicebootstrap");
        prop.setProperty("bootstrap.initialpassword","Fhdt1bb1f" );
        prop.setProperty("bootstrap.brokerURI","localhost" );
        prop.setProperty("bootstrap.brokerPort","11883" );
        prop.setProperty("secret","1234567890abcdef");
        JsonObject conf = JsonHelper.from(prop);
        helper = MQTTHelper.getInstance();
        vertx = helper.getVertx();
        EventBus eb = vertx.eventBus();
        eb.publish("setConfig", conf);
    }

    @After
    public void after(){
        Set<String> list = vertx.deploymentIDs();
        if (list!= null && list.size()>0) {
            list.forEach(id -> {
                logger.info(id);
                vertx.undeploy(id);
            });
        }
    }

    @Test
    public void testHelperIsDeployed(TestContext context){
        context.assertNotNull (helper.deploymentID());
    }

    @Test
    public void testConfiguration(TestContext context){
        EventBus eb = vertx.eventBus();
        JsonObject question = new JsonObject().put("key", "bootstrap.brokerPort");
        Async async = context.async();
        eb.send("config", question, r ->{
            if (r.succeeded()){
                JsonObject prop = (JsonObject)r.result().body();
                logger.info("prop"+prop.encodePrettily());
                context.assertEquals("11883",prop.getString("bootstrap.brokerPort"));
            }else{
                logger.info("Error");
                context.fail(r.cause());
            }
            async.complete();
        });
        async.awaitSuccess(3000);
    }

    @Test
    public void testSetConfig(TestContext context){
        EventBus eb = vertx.eventBus();
        JsonObject toSet = new JsonObject().put("testKey", "testVal");
        Async async = context.async();
        eb.publish("setConfig", toSet);
        try {
            Thread.currentThread().wait(1000);
        }catch(Exception e){
            logger.error(e.getMessage(),e);
        }
        JsonObject question = new JsonObject().put("key", "testKey");
        eb.send("config", question, r ->{
            if (r.succeeded()){
                JsonObject prop = (JsonObject)r.result().body();
                context.assertEquals("testVal",prop.getString("testKey"));

            }else{
                logger.info("Error");
                context.fail(r.cause());
            }
            async.complete();
        });

        async.awaitSuccess(5000);
    }

    @Test
    public void testConfigContainsSecret(TestContext context){
        EventBus eb = vertx.eventBus();
        JsonObject question = new JsonObject().put("key", "secret");
        Async async = context.async();
        eb.send("config", question, r ->{
            if (r.succeeded()){
                JsonObject prop = (JsonObject)r.result().body();
                logger.info("prop"+prop.encodePrettily());
                context.assertEquals("1234567890abcdef",prop.getString("secret"));
            }else{
                logger.info("Error");
                context.fail(r.cause());
            }
            async.complete();
        });
        async.awaitSuccess(3000);
    }

}
