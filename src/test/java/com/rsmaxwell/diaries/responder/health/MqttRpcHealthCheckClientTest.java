package com.rsmaxwell.diaries.responder.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.UserProperty;
import org.junit.jupiter.api.Test;

import com.rsmaxwell.mqtt.rpc.common.Response;
import com.rsmaxwell.mqtt.rpc.common.Status;

class MqttRpcHealthCheckClientTest {

	@Test
	void decodesStatusPropertyAndPayloadUsingResponderWireContract() throws Exception {
		MqttMessage message = responseMessage(
				"{\"code\":200,\"message\":\"ok\"}",
				"{\"status\":\"UP\"}");

		Response response = MqttRpcHealthCheckClient.decodeResponse(message);

		assertEquals(Status.OK, response.status());
		assertEquals(Map.of("status", "UP"), response.payload());
	}

	@Test
	void rejectsResponseWithoutRpcStatusProperty() {
		MqttMessage message = new MqttMessage("{\"status\":\"UP\"}".getBytes(StandardCharsets.UTF_8));
		message.setProperties(new MqttProperties());

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> MqttRpcHealthCheckClient.decodeResponse(message));

		assertEquals("MQTT RPC response status property was missing", exception.getMessage());
	}

	@Test
	void preservesRpcErrorStatusWithEmptyPayload() throws Exception {
		MqttMessage message = responseMessage(
				"{\"code\":500,\"message\":\"database health probe failed\"}",
				"null");

		Response response = MqttRpcHealthCheckClient.decodeResponse(message);

		assertEquals(500, response.status().code());
		assertEquals("database health probe failed", response.status().message());
		assertNull(response.payload());
	}

	private static MqttMessage responseMessage(String status, String payload) {
		MqttProperties properties = new MqttProperties();
		properties.getUserProperties().add(new UserProperty("status", status));

		MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
		message.setProperties(properties);
		return message;
	}
}
