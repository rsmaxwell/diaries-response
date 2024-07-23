package com.rsmaxwell.diaries.response;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttClientPersistence;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MqttDefaultFilePersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttSubscription;

import com.rsmaxwell.diaries.response.config.Config;
import com.rsmaxwell.diaries.response.config.DbConfig;
import com.rsmaxwell.diaries.response.config.MqttConfig;
import com.rsmaxwell.diaries.response.handlers.Calculator;
import com.rsmaxwell.diaries.response.handlers.GetPages;
import com.rsmaxwell.diaries.response.handlers.Quit;
import com.rsmaxwell.diaries.response.handlers.Register;
import com.rsmaxwell.diaries.response.handlers.Signin;
import com.rsmaxwell.mqtt.rpc.response.MessageHandler;

public class Responder {

	private static final Logger logger = LogManager.getLogger(Responder.class);

	static String clientID_responder = "responder";
	static String clientID_subscriber = "listener";
	static String requestTopic = "request";

	static int qos = 0;

	static MessageHandler messageHandler;

	static {
		messageHandler = new MessageHandler();
		messageHandler.putHandler("calculator", new Calculator());
		messageHandler.putHandler("getPages", new GetPages());
		messageHandler.putHandler("register", new Register());
		messageHandler.putHandler("signin", new Signin());
		messageHandler.putHandler("quit", new Quit());
	}

	public static void main(String[] args) throws Exception {

		logger.info("diaries Responder");

		Config config = Config.get();

		try (Connection conn = getConnection(config.getDb())) {

			MqttConfig mqtt = config.getMqtt();
			String server = mqtt.getServer();
			String username = mqtt.getUsername();
			String password = mqtt.getPassword();

			MqttClientPersistence persistence = new MqttDefaultFilePersistence();

			MqttAsyncClient client_responder = new MqttAsyncClient(server, clientID_responder, persistence);
			MqttAsyncClient client_subscriber = new MqttAsyncClient(server, clientID_subscriber, persistence);

			messageHandler.setClient(client_responder);
			client_subscriber.setCallback(messageHandler);

			logger.info(String.format("Connecting to broker '%s' as '%s'", server, clientID_responder));
			MqttConnectionOptions connOpts_responder = new MqttConnectionOptions();
			connOpts_responder.setUserName(username);
			connOpts_responder.setPassword(password.getBytes());
			connOpts_responder.setCleanStart(true);
			try {
				client_responder.connect(connOpts_responder).waitForCompletion();
			} catch (MqttException e) {
				logger.info(String.format("Could not connect to the MQTT Broker at: %s", server));
				return;
			} catch (Exception e) {
				logger.error("%s: %s", e.getClass().getSimpleName(), e.getMessage());
				return;
			}

			logger.info(String.format("Connecting to broker '%s' as '%s'", server, clientID_subscriber));
			MqttConnectionOptions connOpts_subscriber = new MqttConnectionOptions();
			connOpts_subscriber.setUserName(username);
			connOpts_subscriber.setPassword(password.getBytes());
			connOpts_subscriber.setCleanStart(true);
			client_subscriber.connect(connOpts_subscriber).waitForCompletion();

			logger.info(String.format("subscribing to: %s", requestTopic));
			MqttSubscription subscription = new MqttSubscription(requestTopic);
			client_subscriber.subscribe(subscription).waitForCompletion();

			// Wait till quit request received
			messageHandler.waitForCompletion();

			logger.info("disconnect");
			client_responder.disconnect().waitForCompletion();
			client_subscriber.disconnect().waitForCompletion();
		}

		logger.info("exiting");
	}

	public static Connection getConnection(DbConfig dbConfig) throws SQLException {

		logger.info(String.format("Connecting to database '%s' as '%s'", dbConfig.getJdbcUrl(), dbConfig.getUsername()));

		Properties connectionProps = new Properties();
		connectionProps.put("user", dbConfig.getUsername());
		connectionProps.put("password", dbConfig.getPassword());

		return DriverManager.getConnection(dbConfig.getJdbcUrl(), connectionProps);
	}
}
