package de.tarent.telekom.cot.mqtt;


import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.*;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.unit.report.ReportOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(VertxUnitRunner.class)
public class MQTTHelperIT {

    static Logger logger = LoggerFactory.getLogger(MQTTHelperIT.class);
    MQTTHelper helper;
    Vertx vertx;

    @Before
    public void before(TestContext context){
        helper = MQTTHelper.getInstance();
        vertx = helper.getVertx();
    }

    @After
    public void after(TestContext context){
        vertx.deploymentIDs().forEach(id ->{
            vertx.undeploy(id);
        });
    }

    @Test
    public void testHelperIsDeployed(TestContext context){

        context.assertNotNull (helper.deploymentID());
    }

    @Test
    public void testDeviceRegister(TestContext context){
        String devId = "TestDevice";
        Async async = context.async();
        helper.registerDevice(devId, back ->{
            logger.info("Back:"+back);
            context.assertTrue(((String)back).contains("status"));
            async.complete();
        });
        async.awaitSuccess(3000);
    }

    @Test
    public void testConfiguration(TestContext context){
        System.setProperty("testProperty", "ok");
        EventBus eb = vertx.eventBus();
        JsonObject question = new JsonObject().put("key", "testProperty");
        Async async = context.async();
        eb.send("config", question, r ->{
            if (r.succeeded()){
                JsonObject prop = (JsonObject)r.result().body();
                logger.info(prop.encodePrettily());
                context.assertEquals("ok",prop.getString("testProperty"));

            }else{
                logger.info("Error");
                context.fail(r.cause());
            }
            async.complete();
        });
        async.awaitSuccess(3000);
    }
}
