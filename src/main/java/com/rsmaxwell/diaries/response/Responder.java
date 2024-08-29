package com.rsmaxwell.diaries.response;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttClientPersistence;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MqttDefaultFilePersistence;
import org.eclipse.paho.mqttv5.common.MqttSubscription;

import com.rsmaxwell.diaries.common.config.Config;
import com.rsmaxwell.diaries.common.config.DbConfig;
import com.rsmaxwell.diaries.common.config.MqttConfig;
import com.rsmaxwell.diaries.common.config.User;
import com.rsmaxwell.diaries.response.handlers.Calculator;
import com.rsmaxwell.diaries.response.handlers.GetDiaries;
import com.rsmaxwell.diaries.response.handlers.GetPages;
import com.rsmaxwell.diaries.response.handlers.Quit;
import com.rsmaxwell.diaries.response.handlers.Register;
import com.rsmaxwell.diaries.response.handlers.Signin;
import com.rsmaxwell.diaries.response.repository.DiaryRepository;
import com.rsmaxwell.diaries.response.repositoryImpl.DiaryRepositoryImpl;
import com.rsmaxwell.diaries.response.utilities.DiaryContext;
import com.rsmaxwell.diaries.response.utilities.GetEntityManager;
import com.rsmaxwell.mqtt.rpc.response.MessageHandler;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

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
		messageHandler.putHandler("getDiaries", new GetDiaries());
		messageHandler.putHandler("register", new Register());
		messageHandler.putHandler("signin", new Signin());
		messageHandler.putHandler("quit", new Quit());
	}

	static Option createOption(String shortName, String longName, String argName, String description, boolean required) {
		return Option.builder(shortName).longOpt(longName).argName(argName).desc(description).hasArg().required(required).build();
	}

	public static void main(String[] args) throws Exception {

		Option configOption = createOption("c", "config", "Configuration", "Configuration", true);

		// @formatter:off
		Options options = new Options();
		options.addOption(configOption);
		// @formatter:on

		CommandLineParser commandLineParser = new DefaultParser();
		CommandLine commandLine = commandLineParser.parse(options, args);

		String filename = commandLine.getOptionValue("config");
		Config config = Config.read(filename);
		DbConfig dbConfig = config.getDb();
		MqttConfig mqtt = config.getMqtt();
		String server = mqtt.getServer();
		User user = mqtt.getUser();

		// @formatter:off
		try (EntityManagerFactory entityManagerFactory = GetEntityManager.adminFactory(dbConfig); 
			 EntityManager entityManager = entityManagerFactory.createEntityManager()) {
			// @formatter:on

			DiaryRepository diaryRepository = new DiaryRepositoryImpl(entityManager);

			DiaryContext context = new DiaryContext();
			context.setDiaryRepository(diaryRepository);
			context.setSecret(config.getSecret());
			context.setDiaries(config.getDiaries());

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
}
