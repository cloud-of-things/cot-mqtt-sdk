package de.tarent.telekom.cot.mqtt;

import de.tarent.telekom.cot.mqtt.util.EncryptionHelper;
import de.tarent.telekom.cot.mqtt.util.Secret;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;


public class BootstrapVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapVerticle.class);

    private MqttClient client;

    private EventBus eb;

    @Override
    public void start() throws Exception {
        eb = vertx.eventBus();

        eb.consumer("register", msg -> {
            registerDevice((JsonObject) msg.body());
        });
    }

    void registerDevice(JsonObject msg) {

        Future <JsonObject> config = getConfig();

        LOGGER.info(msg.encodePrettily());
        MqttClientOptions options = new MqttClientOptions().setPassword(msg.getString("initialPassword")).setUsername(msg.getString("initialUser")).setAutoKeepAlive(true);
        client = MqttClient.create(vertx, options);

        config.setHandler(s -> {

            if (s.succeeded()) {

                String bootstrapped = s.result().getString("bootstrapped");
                String secret = s.result().getString("secret");

                    if (bootstrapped==null) {
                        String newSecret = EncryptionHelper.generatePassword();
                        setPublishHandler(msg, newSecret);
                        LOGGER.info("no bootstrapping request sent. connect and publish.");
                        connectAndPublish(msg, newSecret);
                    } else if(bootstrapped.equals("ongoing")){
                        setPublishHandler(msg, secret);
                        LOGGER.info("bootstrapping request already sent. just connect and resubscribe.");
                        int port = Integer.parseInt(msg.getString("brokerPort"));
                        client.connect(port, msg.getString("brokerURI"), ch -> {
                            if (ch.succeeded()) {
                                System.out.println("Connected to a server");
                                client.subscribe(msg.getString("subscribeTopic"), MqttQoS.AT_LEAST_ONCE.value());
                            }
                        });
                    }
            }
        });

    }

    private Future <JsonObject> getConfig(){

        Future <JsonObject> future = Future.future();

        JsonObject params = new JsonObject().put("keys", new JsonArray().add(new JsonObject().put("key","secret")).add(new JsonObject().put("key","bootstrapped")));

        eb.send("config", params , result -> {
            if(result.succeeded()) {
                future.complete((JsonObject)result.result().body());
            }else{
                future.fail(result.cause());
            }
        });

        return future;

    }

    private void setPublishHandler(JsonObject msg, String secret){
        client.publishHandler(s -> {
            if (s.topicName().equals(msg.getString("subscribeTopic"))) {
                LOGGER.info(String.format("Receive message with content: \"%s\" from topic \"%s\"", s.payload().toString("utf-8"), s.topicName()));
                EncryptionHelper ech = new EncryptionHelper();
                byte[] pass = ech.decrypt(new Secret(secret), s.payload().getBytes());
                client.unsubscribe(msg.getString("subscribeTopic"));
                client.disconnect();

                JsonObject replyObject = new JsonObject();
                replyObject.put("status", "registered");
                replyObject.put("password", new String(pass));

                eb.publish("bootstrapComplete", replyObject);

                //write to config that bootstrap process is done
                JsonObject bootStrapDoneMessage = new JsonObject();
                bootStrapDoneMessage.put("bootstrapped", "bootstrapped");
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

                JsonObject configParams = new JsonObject();

                client.publish(
                    msg.getValue("publishTopic").toString(),
                    Buffer.buffer(secret),
                    MqttQoS.AT_LEAST_ONCE,
                    false,
                    false,
                    s -> {
                        LOGGER.info("Publish sent to a server");
                        //write to config that bootstrap process has started
                        configParams.put("bootstrapped", "ongoing");
                        configParams.put("secret", secret);
                        eb.publish("setConfig", configParams);
                    });

            } else {
                LOGGER.error("Failed to connect to a server", ch.cause());

            }

        });
    }


}
