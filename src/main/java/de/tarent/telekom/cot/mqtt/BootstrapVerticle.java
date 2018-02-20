package de.tarent.telekom.cot.mqtt;

import de.tarent.telekom.cot.mqtt.util.EncryptionHelper;
import de.tarent.telekom.cot.mqtt.util.JsonHelper;
import de.tarent.telekom.cot.mqtt.util.Secret;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;

import java.util.Arrays;


public class BootstrapVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapVerticle.class);

    private MqttClient client;

    private EventBus eb;

    @Override
    public void start() throws Exception {
        eb = vertx.eventBus();

        eb.consumer("register", msg -> {
            registerDevice((JsonObject) msg.body(), msg);
        });
    }

    void registerDevice(JsonObject msg, Message handle) {

        Future <String> secret = getConfig("secret");

        LOGGER.info(msg.encodePrettily());
        MqttClientOptions options = new MqttClientOptions().setPassword(msg.getString("initialPassword")).setUsername(msg.getString("initialUser")).setAutoKeepAlive(true);
        client = MqttClient.create(vertx, options);

        secret.setHandler(s -> {

            if(s.succeeded()) {
                setPublishHandler(msg, handle, s.result());

                connectAndPublish(msg, s.result());
            }
        });

    }

    private Future <String> getConfig(String key){

        Future <String> future = Future.future();

        JsonObject configKeys = new JsonObject();

        eb.send("config", new JsonObject().put("key", key) , result -> {
            if(result.succeeded()) {
                JsonObject response = (JsonObject)result.result().body();
                if(response.getValue(key)!=null){
                    future.complete(response.getString(key));
                    LOGGER.info("using existing secret from config: "+future.result());
                }else{
                    future.complete(EncryptionHelper.generatePassword());
                    LOGGER.info("no secret in config found, generating new secret: "+future.result());
                }
            }else{
                future.fail(result.cause());
            }
        });

        return future;

    }

    private void setPublishHandler(JsonObject msg, Message handle, String secret){
        client.publishHandler(s -> {
            if (s.topicName().equals(msg.getString("subscribeTopic"))) {
                LOGGER.info(String.format("Receive message with content: \"%s\" from topic \"%s\"", s.payload().toString("utf-8"), s.topicName()));
                EncryptionHelper ech = new EncryptionHelper();
                byte[] pass = ech.decrypt(new Secret(secret), s.payload().getBytes());
                client.disconnect();

                JsonObject replyObject = new JsonObject();
                replyObject.put("status", "registered");
                replyObject.put("credentials", new String(pass));

                handle.reply(replyObject);

                //write to config that bootstrap process is done
                JsonObject bootStrapDoneMessage = new JsonObject();
                bootStrapDoneMessage.put("bootstrapped", true);
                eb.publish("setConfig", bootStrapDoneMessage);
            }
        });
    }

    private void connectAndPublish(JsonObject msg, String secret) {
        //connect and publish on /iccid
        int port = Integer.parseInt(msg.getString("brokerPort"));
        client.connect(port, msg.getString("brokerURI"), ch -> {
            if (ch.succeeded()) {
                System.out.println("Connected to a server");
                client.subscribe(msg.getString("subscribeTopic"), MqttQoS.AT_LEAST_ONCE.value());

                client.publish(
                    msg.getValue("publishTopic").toString(),
                    Buffer.buffer(secret),
                    MqttQoS.AT_LEAST_ONCE,
                    false,
                    false,
                    s -> LOGGER.info("Publish sent to a server"));

                //write to config that bootstrap process has started
                JsonObject configParams = new JsonObject();
                configParams.put("bootstrapped", false);
                configParams.put("secret", secret);
                eb.publish("setConfig", configParams);


            } else {
                LOGGER.error("Failed to connect to a server", ch.cause());

            }

        });
    }


}
