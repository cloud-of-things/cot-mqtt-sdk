package de.tarent.telekom.cot.mqtt;


import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


public class Configuration extends AbstractVerticle {

    Logger logger = LoggerFactory.getLogger(Configuration.class);

    JsonObject conf = new JsonObject();
    final JsonObject sysConf = new JsonObject();


    @Override
    public void start() {

        ConfigRetriever retriever = ConfigRetriever.create(vertx);
        retriever.getConfig(ch -> {
            if (ch.succeeded()) {
                sysConf.mergeIn(ch.result());
                FileSystem fs = vertx.fileSystem();
                String dir = sysConf.getString("user.home") + "/.nbiot";
                String path = dir + "/config.json";
                sysConf.put("configPath", path);
                fs.exists(path, fh -> {
                    if (fh.succeeded() && fh.result()) {
                        fs.readFile(path, rf -> {
                            if (rf.succeeded()) {
                                JsonObject o = new JsonObject(rf.result());
                                conf.mergeIn(o);
                            }
                        });
                    } else {
                        logger.info("dir has to be created");
                        fs.mkdir(dir, mkdh -> {
                            if (mkdh.succeeded()) {
                                logger.info("config_dir created");
                            } else {
                                logger.error("creation of config dir failed", mkdh.cause());
                            }
                        });
                    }
                });

                EventBus eb = vertx.eventBus();

                eb.consumer("config", msg -> {
                    JsonObject o = (JsonObject) msg.body();
                    msg.reply(getConfig(o));
                });
                eb.consumer("setConfig", h -> {
                    JsonObject msg = (JsonObject) h.body();
                    setConfig(msg);
                    h.reply(msg.put("saved", true));
                });
                eb.consumer("resetConfig", rh -> {
                    resetConfig();
                    rh.reply(new JsonObject().put("status", "Config cleared"));
                });

                logger.info("Configuration deployed");

            }
        });
    }

    public JsonObject getConfig(JsonObject in) {
        JsonObject out = new JsonObject();

        if (in.containsKey("keys")) {
            in.getJsonArray("keys").forEach(o -> {
                JsonObject jso = (JsonObject) o;
                out.put(jso.getString("key"), conf.getString(jso.getString("key")));
            });
        } else if (in.containsKey("key")) {
            out.put(in.getString("key"), conf.getString(in.getString("key")));
        }
        return out;
    }

    public void setConfig(JsonObject obj) {
        conf.mergeIn(obj);
        FileSystem fs = vertx.fileSystem();
        fs.writeFile(sysConf.getString("configPath"), Buffer.buffer(conf.encode()), fh -> {
            logger.info("configuration saved");
        });
    }


    public void resetConfig() {
        conf.clear();
        FileSystem fs = vertx.fileSystem();
        fs.writeFile(sysConf.getString("configPath"), Buffer.buffer(conf.encode()), fh -> {
            logger.info("configuration saved");
        });
    }
}
