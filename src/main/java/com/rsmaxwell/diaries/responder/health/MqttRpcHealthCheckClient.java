package com.rsmaxwell.diaries.responder.health;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rsmaxwell.mqtt.rpc.common.Request;
import com.rsmaxwell.mqtt.rpc.common.Response;
import com.rsmaxwell.mqtt.rpc.requestor.RemoteProcedureCall;
import com.rsmaxwell.mqtt.rpc.requestor.Token;

final class MqttRpcHealthCheckClient implements ResponderHealthCheck.HealthCheckClient {

	private static final long CONNECT_TIMEOUT_MILLIS = 3500;
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final MqttAsyncClient client;
	private final MqttConnectionOptions connectionOptions;
	private final RemoteProcedureCall remoteProcedureCall;

	MqttRpcHealthCheckClient(String server, String clientId, String responseTopic, String username, String password) throws Exception {
		client = new MqttAsyncClient(server, clientId, new MemoryPersistence());
		connectionOptions = new MqttConnectionOptions();
		connectionOptions.setUserName(username);
		connectionOptions.setPassword(password.getBytes(StandardCharsets.UTF_8));
		connectionOptions.setCleanStart(true);
		connectionOptions.setSessionExpiryInterval(0L);
		connectionOptions.setAutomaticReconnect(false);
		connectionOptions.setConnectionTimeout(3);
		remoteProcedureCall = new RemoteProcedureCall(client, responseTopic);
	}

	@Override
	public void connect() throws Exception {
		client.connect(connectionOptions).waitForCompletion(CONNECT_TIMEOUT_MILLIS);
	}

	@Override
	public void subscribe() throws Exception {
		remoteProcedureCall.subscribeToResponseTopic();
	}

	@Override
	public Response request() throws Exception {
		Request request = new Request("health", Map.of());
		byte[] payload = MAPPER.writeValueAsBytes(request);
		Token token = remoteProcedureCall.request(ResponderHealthCheck.REQUEST_TOPIC, payload);
		return token.waitForResponse();
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
