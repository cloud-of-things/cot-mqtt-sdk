package de.tarent.telekom.cot.mqtt;


import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Class for managing our configuration file so that we can save information that needs to be persisted.
 */
public class Configuration extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    private final JsonObject conf = new JsonObject();
    private final JsonObject sysConf = new JsonObject();
    private final String configPathKey = "configPath";


    @Override
    public void start() {
        final EventBus eb = vertx.eventBus();

        eb.consumer("config", msg -> {
            final JsonObject o = (JsonObject) msg.body();
            msg.reply(getConfig(o));
        });

        eb.consumer("setConfig", h -> {
            logger.info("in setConfig");
            final JsonObject msg = (JsonObject) h.body();
            setConfig(msg);
            h.reply(msg.put("saved", true));
        });

        eb.consumer("resetConfig", rh -> {
            resetConfig();
            rh.reply(new JsonObject().put("status", "Config cleared"));
        });

        final ConfigRetriever retriever = ConfigRetriever.create(vertx);
        retriever.getConfig(ch -> {
            if (ch.succeeded()) {
                deployConfig(ch);
            }
        });
    }

    private void deployConfig(final AsyncResult<JsonObject> ch) {
        sysConf.mergeIn(ch.result());
        final FileSystem fs = vertx.fileSystem();
        final String dir = sysConf.getString("user.home") + "/.nbiot";
        final String path = dir + "/config.json";
        sysConf.put(configPathKey, path);
        fs.exists(path, fh -> {
            if (fh.succeeded() && fh.result()) {
                readFile(fs, path);
            } else {
                logger.info("Directory has to be created.");
                createDirectory(fs, dir);
            }
        });

        logger.info("Configuration deployed");
    }

    private void readFile(final FileSystem fs, final String path) {
        fs.readFile(path, rf -> {
            if (rf.succeeded()) {
                final JsonObject o = new JsonObject(rf.result());
                conf.mergeIn(o);
            }
        });
    }

    private void createDirectory(final FileSystem fs, final String dir) {
        fs.mkdir(dir, mkdh -> {
            if (mkdh.succeeded()) {
                logger.info("config_dir directory was created.");
            } else {
                logger.error("Creation of config_dir failed: ", mkdh.cause());
            }
        });
    }

    private void setConfig(final JsonObject obj) {
        conf.mergeIn(obj);
        final FileSystem fs = vertx.fileSystem();
        fs.writeFile(sysConf.getString(configPathKey), Buffer.buffer(conf.encodePrettily()), fh -> {
            if (fh.succeeded()) {
                logger.info("configuration saved");
            } else {
                logger.error("saving config failed", fh.cause());
            }
        });
    }

    private JsonObject getConfig(final JsonObject in) {
        final JsonObject out = new JsonObject();

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

    private void resetConfig() {
        conf.clear();
        final FileSystem fs = vertx.fileSystem();
        fs.writeFile(sysConf.getString(configPathKey),
            Buffer.buffer(conf.encode()),
            fh -> logger.info("configuration cleared"));
    }
}
