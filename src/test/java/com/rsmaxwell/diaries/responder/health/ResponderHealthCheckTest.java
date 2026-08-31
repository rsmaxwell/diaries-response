package com.rsmaxwell.diaries.responder.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.rsmaxwell.diaries.responder.config.Config;
import com.rsmaxwell.diaries.responder.config.MqttConfig;
import com.rsmaxwell.mqtt.rpc.common.Response;
import com.rsmaxwell.mqtt.rpc.common.Status;

class ResponderHealthCheckTest {

	private static final Map<String, String> ENVIRONMENT = Map.of(
			ResponderHealthCheck.HEALTH_USERNAME, "diaries-health",
			ResponderHealthCheck.HEALTH_PASSWORD, "test-password");

	@Test
	void succeedsOnlyForValidUpResponseAndUsesClientScopedResponseTopic() {
		FakeClient client = new FakeClient();
		client.response = Response.success(Map.of("status", "UP"));
		AtomicReference<String> clientId = new AtomicReference<>();
		AtomicReference<String> responseTopic = new AtomicReference<>();

		Result result = execute(client, (server, id, topic, username, password) -> {
			clientId.set(id);
			responseTopic.set(topic);
			return client;
		});

		assertEquals(0, result.exitCode);
		assertTrue(client.connected);
		assertTrue(client.subscribed);
		assertTrue(client.requested);
		assertTrue(client.closed);
		assertTrue(clientId.get().startsWith("diaries-health-"));
		assertEquals("diaries/rpc/" + clientId.get() + "/response", responseTopic.get());
		assertEquals("", result.error);
	}

	@Test
	void usesAUniqueClientIdForEachInvocation() {
		AtomicReference<String> firstClientId = new AtomicReference<>();
		AtomicReference<String> secondClientId = new AtomicReference<>();

		FakeClient first = validClient();
		Result firstResult = execute(first, (server, id, topic, username, password) -> {
			firstClientId.set(id);
			return first;
		});

		FakeClient second = validClient();
		Result secondResult = execute(second, (server, id, topic, username, password) -> {
			secondClientId.set(id);
			return second;
		});

		assertEquals(0, firstResult.exitCode);
		assertEquals(0, secondResult.exitCode);
		assertNotEquals(firstClientId.get(), secondClientId.get());
	}

	@Test
	void connectionFailureReturnsFailure() {
		FakeClient client = validClient();
		client.connectFailure = new IllegalStateException("connection failed");

		Result result = execute(client);

		assertEquals(1, result.exitCode);
		assertFalse(client.subscribed);
		assertTrue(client.closed);
	}

	@Test
	void subscriptionFailureReturnsFailure() {
		FakeClient client = validClient();
		client.subscribeFailure = new IllegalStateException("subscription failed");

		Result result = execute(client);

		assertEquals(1, result.exitCode);
		assertFalse(client.requested);
		assertTrue(client.closed);
	}

	@Test
	void requestPublishFailureReturnsFailure() {
		FakeClient client = validClient();
		client.requestFailure = new IllegalStateException("request publish failed");

		Result result = execute(client);

		assertEquals(1, result.exitCode);
		assertTrue(client.requested);
		assertTrue(client.closed);
	}

	@Test
	void missingResponseReturnsFailure() {
		FakeClient client = validClient();
		client.response = null;

		Result result = execute(client);

		assertEquals(1, result.exitCode);
		assertTrue(result.error.contains("No MQTT RPC response"));
	}

	@Test
	void responseWaitIsBounded() {
		FakeClient client = validClient();
		client.blockRequest = true;

		Result result = execute(client, (server, id, topic, username, password) -> client, Duration.ofMillis(100));

		assertEquals(1, result.exitCode);
		assertTrue(result.error.contains("timed out"));
	}

	@Test
	void nonSuccessRpcStatusReturnsFailure() {
		FakeClient client = validClient();
		client.response = Response.error(Status.INTERNAL_ERROR, "database health probe failed");

		Result result = execute(client);

		assertEquals(1, result.exitCode);
		assertTrue(result.error.contains("status was not successful"));
	}

	@Test
	void malformedHealthPayloadReturnsFailure() {
		FakeClient client = validClient();
		client.response = Response.success(Map.of("state", "UP"));

		Result result = execute(client);

		assertEquals(1, result.exitCode);
		assertTrue(result.error.contains("payload was invalid"));
	}

	@Test
	void missingCredentialsFailWithoutPrintingPassword() {
		FakeClient client = validClient();
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		int exitCode = ResponderHealthCheck.execute(config(), Map.of(),
				(server, id, topic, username, password) -> client,
				Duration.ofSeconds(1), new PrintStream(output));

		String error = output.toString(StandardCharsets.UTF_8);
		assertEquals(1, exitCode);
		assertTrue(error.contains(ResponderHealthCheck.HEALTH_USERNAME));
		assertFalse(error.contains("test-password"));
	}

	private static FakeClient validClient() {
		FakeClient client = new FakeClient();
		client.response = Response.success(Map.of("status", "UP"));
		return client;
	}

	private static Result execute(FakeClient client) {
		return execute(client, (server, id, topic, username, password) -> client);
	}

	private static Result execute(FakeClient client, ResponderHealthCheck.HealthCheckClientFactory factory) {
		return execute(client, factory, Duration.ofSeconds(1));
	}

	private static Result execute(FakeClient client, ResponderHealthCheck.HealthCheckClientFactory factory, Duration timeout) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		int exitCode = ResponderHealthCheck.execute(config(), ENVIRONMENT, factory, timeout, new PrintStream(output));
		return new Result(exitCode, output.toString(StandardCharsets.UTF_8));
	}

	private static Config config() {
		MqttConfig mqttConfig = new MqttConfig();
		mqttConfig.setHost("mqtt.example");
		mqttConfig.setPort(1883);

		Config config = new Config();
		config.setMqtt(mqttConfig);
		return config;
	}

	private record Result(int exitCode, String error) {
	}

	private static final class FakeClient implements ResponderHealthCheck.HealthCheckClient {

		private boolean connected;
		private boolean subscribed;
		private boolean requested;
		private boolean closed;
		private boolean blockRequest;
		private RuntimeException connectFailure;
		private RuntimeException subscribeFailure;
		private RuntimeException requestFailure;
		private Response response;

		@Override
		public void connect() {
			connected = true;
			if (connectFailure != null) {
				throw connectFailure;
			}
		}

		@Override
		public void subscribe() {
			subscribed = true;
			if (subscribeFailure != null) {
				throw subscribeFailure;
			}
		}

		@Override
		public Response request() throws Exception {
			requested = true;
			if (requestFailure != null) {
				throw requestFailure;
			}
			if (blockRequest) {
				Thread.sleep(Duration.ofSeconds(10));
			}
			return response;
		}

		@Override
		public void close() {
			closed = true;
		}
	}
}
