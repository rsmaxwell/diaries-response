package com.rsmaxwell.diaries.response;

import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttClientPersistence;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MqttDefaultFilePersistence;
import org.eclipse.paho.mqttv5.common.MqttSubscription;

import com.rsmaxwell.diaries.common.config.Config;
import com.rsmaxwell.diaries.common.config.DbConfig;
import com.rsmaxwell.diaries.common.config.Jdbc;
import com.rsmaxwell.diaries.common.config.MqttConfig;
import com.rsmaxwell.diaries.common.config.User;
import com.rsmaxwell.diaries.response.handlers.Calculator;
import com.rsmaxwell.diaries.response.handlers.GetPages;
import com.rsmaxwell.diaries.response.handlers.Quit;
import com.rsmaxwell.diaries.response.handlers.Register;
import com.rsmaxwell.diaries.response.handlers.Signin;
import com.rsmaxwell.diaries.response.utilities.DiaryContext;
import com.rsmaxwell.mqtt.rpc.response.MessageHandler;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class Responder {

	private static final Logger log = LogManager.getLogger(Responder.class);

	static final String clientID_responder = "responder";
	static final String clientID_subscriber = "listener";
	static final String requestTopic = "request";
	static final int qos = 0;

	static MessageHandler messageHandler = new MessageHandler();

	static {
		messageHandler.putHandler("calculator", new Calculator());
		messageHandler.putHandler("getPages", new GetPages());
		messageHandler.putHandler("register", new Register());
		messageHandler.putHandler("signin", new Signin());
		messageHandler.putHandler("quit", new Quit());
	}

	public static void main(String[] args) throws Exception {

		Config config = Config.read();
		DbConfig dbConfig = config.getDb();
		MqttConfig mqtt = config.getMqtt();
		String server = mqtt.getServer();
		User user = mqtt.getUser();

		try (EntityManagerFactory entityManagerFactory = getEntityManagerFactory(dbConfig)) {

			DiaryContext context = new DiaryContext();
			context.setEntityManagerFactory(entityManagerFactory);
			context.setSecret(config.getSecret());

			MqttClientPersistence persistence = new MqttDefaultFilePersistence();
			MqttAsyncClient client_responder = new MqttAsyncClient(server, clientID_responder, persistence);
			MqttAsyncClient client_subscriber = new MqttAsyncClient(server, clientID_subscriber, persistence);

			messageHandler.setContext(context);
			messageHandler.setClient(client_responder);
			client_subscriber.setCallback(messageHandler);

			log.info(String.format("Connecting to broker '%s' as '%s'", server, clientID_responder));
			MqttConnectionOptions connOpts_responder = new MqttConnectionOptions();
			connOpts_responder.setUserName(user.getUsername());
			connOpts_responder.setPassword(user.getPassword().getBytes());
			connOpts_responder.setCleanStart(true);
			client_responder.connect(connOpts_responder).waitForCompletion();

			log.info(String.format("Connecting to broker '%s' as '%s'", server, clientID_subscriber));
			MqttConnectionOptions connOpts_subscriber = new MqttConnectionOptions();
			connOpts_subscriber.setUserName(user.getUsername());
			connOpts_subscriber.setPassword(user.getPassword().getBytes());
			connOpts_subscriber.setCleanStart(true);
			client_subscriber.connect(connOpts_subscriber).waitForCompletion();

			log.info(String.format("subscribing to: %s", requestTopic));
			MqttSubscription subscription = new MqttSubscription(requestTopic);
			client_subscriber.subscribe(subscription).waitForCompletion();

			// Wait till quit request received
			messageHandler.waitForCompletion();

			log.info("disconnect");
			client_responder.disconnect().waitForCompletion();
			client_subscriber.disconnect().waitForCompletion();

			log.info("Success");

		} catch (Exception e) {
			log.catching(e);
			return;
		}
	}

	public static EntityManagerFactory getEntityManagerFactory(DbConfig dbConfig) {

		EntityManagerFactory entityManagerFactory = null;

		try {
			Jdbc jdbc = dbConfig.getJdbc();
			List<User> users = dbConfig.getUsers();
			if (users.size() <= 0) {
				throw new Exception("No users defined in configuration");
			}
			User user = users.get(0);

			Properties props = new Properties();
			props.put("jakarta.persistence.jdbc.url", dbConfig.getJdbcUrlWithDatabase());
			props.put("jakarta.persistence.jdbc.driver", jdbc.getDriver());
			props.put("jakarta.persistence.jdbc.user", user.getUsername());
			props.put("jakarta.persistence.jdbc.password", user.getPassword());

			log.debug("JDBC properties:");
			for (String key : props.stringPropertyNames()) {
				log.debug(String.format("    %s : %s", key, props.getProperty(key)));
			}

			entityManagerFactory = Persistence.createEntityManagerFactory("com.rsmaxwell.diaries", props);

		} catch (Throwable ex) {
			// Make sure you log the exception, as it might be swallowed
			System.err.println("Initial SessionFactory creation failed." + ex);
			throw new ExceptionInInitializerError(ex);
		}

		return entityManagerFactory;
	}
}
