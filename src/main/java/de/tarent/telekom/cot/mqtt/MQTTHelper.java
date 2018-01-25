package de.tarent.telekom.cot.mqtt;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Properties;
import java.util.function.Consumer;

public class MQTTHelper extends AbstractVerticle {

    static Logger logger = LoggerFactory.getLogger(MQTTHelper.class);
    static MQTTHelper helper;

    private MQTTHelper(){

        init();
    }

    /**
        Starts the included verticles
     */
    private static void init(){
        Vertx v = Vertx.vertx();
        Configuration config = new Configuration();
        v.deployVerticle(config);
        helper = new MQTTHelper();
        v.deployVerticle(helper);
        BootstrapVerticle btvert = new BootstrapVerticle();
        v.deployVerticle(btvert);
        logger.info("Verticles started");
    }

    /**
     * Method returns an MQTTHelper instance and starts the vertx instance if not done before
     * @param prop
     * @return MQTTHelpder instance
     */
    public static MQTTHelper getInstance(Properties prop){
        if (helper == null){
            init();

        }
        return helper;
    }


    public void registerDevice(String deviceId, Consumer callback){
        EventBus eb = vertx.eventBus();
        JsonObject msg = new JsonObject().put("deviceId", deviceId);
        eb.send("register", msg, result ->{
            if (result.succeeded()){
                JsonObject regresult = (JsonObject)result.result().body();
                //ToDo:prepare ReturnMSG
                callback.accept(regresult);
            }else{
                logger.error("Registration failed - ", result.cause());
            }
        });
    }
}
