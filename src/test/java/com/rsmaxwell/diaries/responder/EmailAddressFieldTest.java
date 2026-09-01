package com.rsmaxwell.diaries.responder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.rsmaxwell.diaries.responder.utilities.Field;
import com.rsmaxwell.mqtt.rpc.exceptions.RpcStatusException;

class EmailAddressFieldTest {

	@ParameterizedTest(name = "accepts valid email: {0}")
	@ValueSource(strings = {
			"email@example.com",
			"firstname.lastname@example.com",
			"email@subdomain.example.com",
			"firstname+lastname@example.com",
			"“email”@example.com",
			"あいうえお@example.com",
			"1234567890@example.com",
			"email@example-one.com",
			"_______@example.com",
			"email@example.name",
			"email@example.museum",
			"email@example.co.jp"
	})
	void acceptsValidEmailAddress(String emailAddress) throws Exception {
		Field field = new Field("email", Map.of("email", emailAddress)).email();

		assertEquals(emailAddress, field.toString());
	}

	@ParameterizedTest(name = "rejects invalid email: {0}")
	@ValueSource(strings = {
			"firstname-lastname@example.com\t",
			"email@123.123.123.123",
			"plainaddress",
			"#@%^%#$@#$@#.com",
			"@example.com",
			"Joe Smith <email@example.com>",
			"email.example.com",
			"email@example@example.com",
			".email@example.com",
			"email.@example.com",
			"email..email@example.com",
			"email@example.com (Joe Smith)",
			"email@example",
			"email@-example.com",
			"email@example.web",
			"email@111.222.333.44444",
			"email@example..com",
			"Abc..123@example.com"
	})
	void rejectsInvalidEmailAddress(String emailAddress) {
		Map<String, Object> args = Map.of("email", emailAddress);

		assertThrows(RpcStatusException.class,
				() -> new Field("email", args).email());
	}
}
