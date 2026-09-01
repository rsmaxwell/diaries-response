package com.rsmaxwell.diaries.responder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.rsmaxwell.diaries.responder.utilities.Field;
import com.rsmaxwell.mqtt.rpc.exceptions.RpcStatusException;

class PhoneNumberFieldTest {

	@ParameterizedTest(name = "accepts valid phone number: {0}")
	@ValueSource(strings = {
			"+44 (123) 4567890",
			"+44 (1722) 842753",
			"(01694) 603845",
			"01700  592357",
			"01256 456789",
			"01228 789015",
			"+44 141 222-3344",
			"+44 (7854) 604311",
			"01261 550125",
			"(0123) 4567890",
			"01303 678905",
			"01872824672",
			"+44 785 604-3111",
			"+44 785 4604-311",
			"+44 (1364) 604331",
			"+44 (1346) 604311",
			"+44 (785) 4604311",
			"01427 550125",
			"(01651) 550125",
			"01697 550125",
			"1470 670125",
			"(1503) 555125",
			"+33644444444",
			"+33 6 44 44 44 44",
			"+33 6 36 85 67 89",
			"07-23456-7892",
			"1234567890",
			"123)4567890"
	})
	void acceptsValidPhoneNumber(String phoneNumber) throws Exception {
		Field field = new Field("phone", Map.of("phone", phoneNumber)).phone();

		assertEquals(phoneNumber, field.toString());
	}

	@ParameterizedTest(name = "rejects invalid phone number: {0}")
	@ValueSource(strings = {
			"(1)234567890666",
			"1",
			"123-4567",
			"Hello world"
	})
	void rejectsInvalidPhoneNumber(String phoneNumber) {
		Map<String, Object> args = Map.of("phone", phoneNumber);

		assertThrows(RpcStatusException.class,
				() -> new Field("phone", args).phone());
	}
}
