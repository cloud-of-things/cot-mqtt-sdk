package de.tarent.telekom.cot.mqtt;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class SystemTest {
	
	final static String MQTT_BROKER_HOST = "nb-iot.int2-ram.m2m.telekom.com";
	final static String MQTT_BROKER_PORT = "1883";
	final static String BOOTSTRAP_USER = "devicebootstrap";
	final static String BOOTSTRAP_PASSWORD = "Fhdt1bb1f";
	final static String BOOTSTRAP_KEY = "bootstrapkey1234";
	final static String BOOTSTRAP_DEVICE = "apitester";
	
	final static String KEY_BOOTSTRAP_USER = "initialUser";
	final static String KEY_BOOTSTRAP_PW = "initialPasword";
	final static String KEY_BROKER_PORT = "brokerPort";
	final static String KEY_BROKER_URI = "brokerURI";
	final static String KEY_BOOTSTRAP_PUBLISH_TOPIV = "publish_topic";
	final static String KEY_BOOTSTRAP_SUBSCRIBE_TOPIC = "subscribe_topic";
	final static String KEY_BOOTSTRAP_ECNCRYPTION = "message";
	final static String KEY_BOOTSTRAP_PUBLISH_PREFIX = "/ss/";
	final static String KEY_BOOTSTRAP_SUBSCRIBE_PREFIX = "/sr/";
	
	
	MQTTHelper helper;
	Vertx vertx;
	
	@Before
	public void before() {
		helper = MQTTHelper.getInstance();
		vertx = helper.getVertx();
	}

	@Test
	public void BootstrapSystemTest() {
		Properties prop = new Properties();
	}
}
