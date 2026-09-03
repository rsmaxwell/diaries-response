package com.rsmaxwell.diaries.responder.health;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.UserProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rsmaxwell.mqtt.rpc.common.Adapter;
import com.rsmaxwell.mqtt.rpc.common.Request;
import com.rsmaxwell.mqtt.rpc.common.Response;
import com.rsmaxwell.mqtt.rpc.common.Status;

final class MqttRpcHealthCheckClient implements ResponderHealthCheck.HealthCheckClient {

	private static final long CONNECT_TIMEOUT_MILLIS = 3500;
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final MqttAsyncClient client;
	private final MqttConnectionOptions connectionOptions;
	private final String responseTopic;
	private final CompletableFuture<MqttMessage> responseMessage = new CompletableFuture<>();
	private volatile byte[] expectedCorrelationData;

	MqttRpcHealthCheckClient(String server, String clientId, String responseTopic, String username, String password) throws Exception {
		client = new MqttAsyncClient(server, clientId, new MemoryPersistence());
		this.responseTopic = responseTopic;
		connectionOptions = new MqttConnectionOptions();
		connectionOptions.setUserName(username);
		connectionOptions.setPassword(password.getBytes(StandardCharsets.UTF_8));
		connectionOptions.setCleanStart(true);
		connectionOptions.setSessionExpiryInterval(0L);
		connectionOptions.setAutomaticReconnect(false);
		connectionOptions.setConnectionTimeout(3);
		client.setCallback(new Adapter() {

			@Override
			public void messageArrived(String topic, MqttMessage message) {
				MqttProperties properties = message.getProperties();
				byte[] actualCorrelationData = properties == null ? null : properties.getCorrelationData();
				byte[] expected = expectedCorrelationData;

				if (expected != null && Arrays.equals(expected, actualCorrelationData)) {
					responseMessage.complete(message);
				}
			}
		});
	}

	@Override
	public void connect() throws Exception {
		client.connect(connectionOptions).waitForCompletion(CONNECT_TIMEOUT_MILLIS);
	}

	@Override
	public void subscribe() throws Exception {
		client.subscribe(new MqttSubscription(responseTopic)).waitForCompletion(CONNECT_TIMEOUT_MILLIS);
	}

	@Override
	public Response request() throws Exception {
		Request request = new Request("health", Map.of());
		MqttMessage message = new MqttMessage(MAPPER.writeValueAsBytes(request));
		MqttProperties properties = new MqttProperties();
		expectedCorrelationData = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
		properties.setResponseTopic(responseTopic);
		properties.setCorrelationData(expectedCorrelationData);
		message.setProperties(properties);

		client.publish(ResponderHealthCheck.REQUEST_TOPIC, message).waitForCompletion(CONNECT_TIMEOUT_MILLIS);
		return decodeResponse(responseMessage.get());
	}

	static Response decodeResponse(MqttMessage message) throws Exception {
		if (message == null || message.getProperties() == null) {
			throw new IllegalArgumentException("MQTT RPC response properties were missing");
		}

		String statusJson = null;
		for (UserProperty property : message.getProperties().getUserProperties()) {
			if ("status".equals(property.getKey())) {
				statusJson = property.getValue();
				break;
			}
		}

		if (statusJson == null || statusJson.isBlank()) {
			throw new IllegalArgumentException("MQTT RPC response status property was missing");
		}

		Status status = MAPPER.readValue(statusJson, Status.class);
		byte[] payloadBytes = message.getPayload();
		Object payload = payloadBytes == null || payloadBytes.length == 0
				? null
				: MAPPER.readValue(payloadBytes, Object.class);

		return new Response(status, payload, false);
	}

	@Override
	public void close() throws Exception {
		try {
			if (client.isConnected()) {
				client.disconnectForcibly(100, 100, false);
			}
		} finally {
			client.close(true);
		}
	}
}
