/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.rsmaxwell.mqtt.rpc.example.response;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.rsmaxwell.diaries.response.Responder;

class RequestHandlerTest {
	@Test
	void appHasAGreeting() {
		Responder classUnderTest = new Responder() {
		};
		assertNotNull(classUnderTest, "instance should not be null");
	}
}
