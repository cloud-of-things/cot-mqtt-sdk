package de.tarent.telekom.cot.mqtt;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class ConfigurationTest {

    Configuration config;

    @Before
    public void before(TestContext context){
        Vertx vertx = Vertx.vertx();
        config = new Configuration();
        vertx.deployVerticle(config, context.asyncAssertSuccess());
    }

    @Test
    public void testSetConfig(TestContext context){
        EventBus eb = config.getVertx().eventBus();
        Async async = context.async();
        JsonObject msg = new JsonObject().put("configurationTest", "content");
        final JsonObject o = new JsonObject().put("key", "configurationTest");
        eb.send("setConfig", msg, reply ->{
            if (reply.succeeded()){
                eb.send("config", o , crep ->{
                    if (crep.succeeded()){
                        JsonObject conf = (JsonObject)crep.result().body();
                        context.assertEquals("content", conf.getString("configurationTest"));
                        async.complete();
                    }
                });
            }
        });
        async.awaitSuccess();
    }
}
