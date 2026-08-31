package com.rsmaxwell.diaries.responder.health;

import java.io.PrintStream;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.rsmaxwell.diaries.responder.config.Config;
import com.rsmaxwell.mqtt.rpc.common.Response;

public final class ResponderHealthCheck {

	static final String HEALTH_USERNAME = "DIARIES_MQTT_HEALTH_USERNAME";
	static final String HEALTH_PASSWORD = "DIARIES_MQTT_HEALTH_PASSWORD";
	static final String REQUEST_TOPIC = "diaries/rpc/request";
	static final String RESPONSE_TOPIC_PREFIX = "diaries/rpc/";
	static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(4);

	private ResponderHealthCheck() {
	}

	public static void main(String[] args) {
		int exitCode = run(args, System.getenv(), MqttRpcHealthCheckClient::new, DEFAULT_TIMEOUT, System.err);
		System.exit(exitCode);
	}

	static int run(String[] args, Map<String, String> environment, HealthCheckClientFactory clientFactory, Duration timeout, PrintStream error) {
		try {
			Options options = new Options();
			options.addOption(Option.builder("c").longOpt("config").argName("Configuration").desc("Configuration").hasArg().required().get());

			CommandLine commandLine = new DefaultParser().parse(options, args);
			Config config = Config.read(commandLine.getOptionValue("config"));
			return execute(config, environment, clientFactory, timeout, error);
		} catch (Exception exception) {
			failure(error, exception);
			return 1;
		}
	}

	static int execute(Config config, Map<String, String> environment, HealthCheckClientFactory clientFactory, Duration timeout, PrintStream error) {
		HealthCheckClient client = null;
		boolean successful = false;

		try {
			if (config == null || config.getMqtt() == null) {
				throw new HealthCheckException("MQTT configuration is missing");
			}

			String username = requiredEnvironment(environment, HEALTH_USERNAME);
			String password = requiredEnvironment(environment, HEALTH_PASSWORD);
			String clientId = createClientId();
			String responseTopic = RESPONSE_TOPIC_PREFIX + clientId + "/response";
			long deadline = System.nanoTime() + timeout.toNanos();

			client = callBeforeDeadline(deadline,
					() -> clientFactory.create(config.getMqtt().getServer(), clientId, responseTopic, username, password));

			HealthCheckClient connectedClient = client;
			callBeforeDeadline(deadline, () -> {
				connectedClient.connect();
				return null;
			});
			callBeforeDeadline(deadline, () -> {
				connectedClient.subscribe();
				return null;
			});

			Response response = callBeforeDeadline(deadline, connectedClient::request);
			validate(response);
			successful = true;
		} catch (Exception exception) {
			failure(error, exception);
		} finally {
			if (client != null) {
				try {
					client.close();
				} catch (Exception exception) {
					if (successful) {
						failure(error, exception);
					}
					successful = false;
				}
			}
		}

		return successful ? 0 : 1;
	}

	private static String requiredEnvironment(Map<String, String> environment, String name) throws HealthCheckException {
		String value = environment.get(name);
		if (value == null || value.isBlank()) {
			throw new HealthCheckException("Required environment variable is missing: " + name);
		}
		return value;
	}

	private static String createClientId() {
		String randomId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		return "diaries-health-" + randomId;
	}

	private static void validate(Response response) throws HealthCheckException {
		if (response == null) {
			throw new HealthCheckException("No MQTT RPC response received");
		}
		if (response.status() == null || !response.status().isOk()) {
			throw new HealthCheckException("MQTT RPC health status was not successful");
		}
		if (!(response.payload() instanceof Map<?, ?> payload) || !"UP".equals(payload.get("status"))) {
			throw new HealthCheckException("MQTT RPC health payload was invalid");
		}
	}

	private static <T> T callBeforeDeadline(long deadline, Callable<T> callable) throws Exception {
		long remainingNanos = deadline - System.nanoTime();
		if (remainingNanos <= 0) {
			throw new TimeoutException("MQTT RPC health check timed out");
		}

		FutureTask<T> task = new FutureTask<>(callable);
		Thread thread = Thread.ofVirtual().name("diaries-health-check").start(task);

		try {
			return task.get(remainingNanos, TimeUnit.NANOSECONDS);
		} catch (ExecutionException exception) {
			Throwable cause = exception.getCause();
			if (cause instanceof Exception checkedException) {
				throw checkedException;
			}
			throw new HealthCheckException("MQTT RPC health check failed", cause);
		} catch (TimeoutException exception) {
			thread.interrupt();
			throw new TimeoutException("MQTT RPC health check timed out");
		}
	}

	private static void failure(PrintStream error, Exception exception) {
		String message = exception.getMessage();
		if (message == null || message.isBlank()) {
			message = exception.getClass().getSimpleName();
		}
		error.println("Responder health check failed: " + message);
	}

	@FunctionalInterface
	interface HealthCheckClientFactory {
		HealthCheckClient create(String server, String clientId, String responseTopic, String username, String password) throws Exception;
	}

	interface HealthCheckClient extends AutoCloseable {
		void connect() throws Exception;

		void subscribe() throws Exception;

		Response request() throws Exception;

		@Override
		void close() throws Exception;
	}

	private static final class HealthCheckException extends Exception {

		private static final long serialVersionUID = 1L;

		HealthCheckException(String message) {
			super(message);
		}

		HealthCheckException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
