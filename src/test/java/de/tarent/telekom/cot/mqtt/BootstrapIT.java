package de.tarent.telekom.cot.mqtt;


import de.tarent.telekom.cot.mqtt.util.JsonHelper;
import de.tarent.telekom.cot.mqtt.util.MQTTTestClient;
import de.tarent.telekom.cot.mqtt.util.MQTTTestServer;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import org.junit.*;
import org.junit.runner.RunWith;

import java.util.Properties;

import static de.tarent.telekom.cot.mqtt.util.Bootstrapped.ONGOING;

@RunWith(VertxUnitRunner.class)
public class BootstrapIT {

    static Logger logger = LoggerFactory.getLogger(BootstrapIT.class);
    MQTTHelper helper;


    @BeforeClass
    public static void beforeClass() {
        Vertx vc = Vertx.vertx();
        MQTTTestServer server = new MQTTTestServer();
        vc.deployVerticle(server, h-> {
            if (h.succeeded()){
                MQTTTestClient client = new MQTTTestClient(false);
                vc.deployVerticle(client);
            }
        });
    }

    @Before
    public void before(TestContext context) throws InterruptedException{
        helper = MQTTHelper.getInstance();
        Thread.sleep(500); //NOSONAR - This is in a test, it's OK.
        EventBus eb = helper.getVertx().eventBus();
        eb.publish("resetConfig", new JsonObject());
        Thread.sleep(500); //NOSONAR - This is in a test, it's OK.
    }

    @After
    public void after(TestContext context) throws InterruptedException{
        Vertx v = helper.getVertx();
        v.undeploy(helper.deploymentID(),h ->{
            if (h.succeeded()){
                logger.info("helper undeployed");
            }else{
                logger.error("error during undeployment of helper", h.cause());
            }
        });
        Thread.sleep(1000); //NOSONAR - This is in a test, it's OK.
    }

    @Test
    public void testConfiguration(TestContext context){
        Properties prop = new Properties();
        prop.setProperty("initialUser", "devicebootstrap");
        prop.setProperty("initialPassword", "Fhdt1bb1f");
        prop.setProperty("brokerURI", "localhost");
        prop.setProperty("brokerPort", "11883");
        prop.setProperty("message", "test1234567890ab");
        prop.setProperty("ssl", "false");
        JsonObject o = JsonHelper.from(prop);
        EventBus eb = helper.getVertx().eventBus();
        Async async = context.async();
        eb.send("setConfig", o, r -> {
            async.complete();
        });
        async.awaitSuccess(2000);
    }

    @Test
    public void testDeviceRegister(TestContext context) {
        Properties prop = new Properties();
        prop.setProperty("initialUser", "devicebootstrap");
        prop.setProperty("initialPassword", "Fhdt1bb1f");
        prop.setProperty("brokerURI", "localhost");
        prop.setProperty("brokerPort", "11883");
        prop.setProperty("ssl", "false");
        String devId = "testDevice";
        Async async = context.async();
        helper.registerDevice(devId, prop, back -> {
            logger.info("Back:" + back);
            context.assertNotNull(back);
            async.complete();
        });
        async.awaitSuccess(5000);
    }

    @Test
    public void testDeviceWithExistingSecret(TestContext context){
        Properties prop = new Properties();
        prop.setProperty("initialUser", "devicebootstrap");
        prop.setProperty("initialPassword", "Fhdt1bb1f");
        prop.setProperty("brokerURI", "localhost");
        prop.setProperty("brokerPort", "11883");
        prop.setProperty("secret", "1234567890abcdef");
        prop.setProperty("bootstrapped", ONGOING.name());
        prop.setProperty("ssl", "false");
        String devId = "testDevice";
        JsonObject conf = JsonHelper.from(prop);
        EventBus eb = helper.getVertx().eventBus();
        eb.publish("setConfig", conf);
        Async async = context.async();
        helper.registerDevice(devId, prop, back -> {
            logger.info("Back:" + back);
            context.assertNotNull(back);
            async.complete();
        });
        MqttClientOptions option = new MqttClientOptions();
        option.setUsername(conf.getString("initialUser"));
        option.setPassword(conf.getString("initialPassword"));
        option.setClientId("fakeClient");
        MqttClient mqttClient = MqttClient.create(helper.getVertx(), option);
        int port = Integer.parseInt(conf.getString("brokerPort"));
        mqttClient.connect(port, conf.getString("brokerURI"),conHandler->{
            if (conHandler.succeeded()){
                mqttClient.publish("ss/testDevice", Buffer.buffer(conf.getString("secret")),MqttQoS.AT_LEAST_ONCE
                , false, false);
            }
        });

        async.awaitSuccess(5000);
    }

}
