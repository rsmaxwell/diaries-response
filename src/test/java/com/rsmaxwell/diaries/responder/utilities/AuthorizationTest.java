package com.rsmaxwell.diaries.responder.utilities;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.paho.mqttv5.common.packet.UserProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.rsmaxwell.diaries.responder.model.Role;
import com.rsmaxwell.mqtt.rpc.common.Status;
import com.rsmaxwell.mqtt.rpc.exceptions.RpcStatusException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

class AuthorizationTest {

	private static final String SECRET = secret("01234567890123456789012345678901");
	private static final String OTHER_SECRET = secret("abcdefghijklmnopqrstuvwxyzABCDEF");

	@Test
	void accessTokenIsReadFromMqttUserProperties() {
		List<UserProperty> properties = List.of(
				new UserProperty("correlationId", "123"),
				new UserProperty("accessToken", "signed-token"));

		assertEquals("signed-token", Authorization.getAccessToken(properties));
	}

	@Test
	void missingAccessTokenReturnsNull() {
		assertNull(Authorization.getAccessToken(
				List.of(new UserProperty("correlationId", "123"))));
	}

	@Test
	void refreshTokenIsReadOnlyWhenItIsAString() {
		assertEquals("refresh-token",
				Authorization.getRefreshToken(Map.of("refreshToken", "refresh-token")));
		assertNull(Authorization.getRefreshToken(Map.of()));
		assertNull(Authorization.getRefreshToken(Map.of("refreshToken", 123)));
	}

	@Test
	void generatedTokenContainsSubjectAndClaims() {
		String token = Authorization.getTokenWithClaims(
				SECRET,
				"access",
				60,
				ChronoUnit.SECONDS,
				Map.of("userId", 42L, "sessionId", "session-1"));

		Claims claims = Authorization.parseToken(SECRET, token);

		assertEquals("access", claims.getSubject());
		assertEquals(42L, ((Number) claims.get("userId")).longValue());
		assertEquals("session-1", claims.get("sessionId"));
		assertNotNull(claims.getExpiration());
	}

	@Test
	void tokenSignedWithAnotherSecretIsRejected() {
		String token = Authorization.getToken(OTHER_SECRET, "access", 60, ChronoUnit.SECONDS);

		assertThrows(JwtException.class, () -> Authorization.parseToken(SECRET, token));
	}

	@Test
	void checkTokenRejectsMissingExpiredAndUnexpectedSubjectTokens() {
		DiaryContext context = context();

		RpcStatusException missing = assertThrows(RpcStatusException.class,
				() -> Authorization.checkToken(context, "access", null));
		assertEquals(Status.UNAUTHORIZED.code(), missing.getStatus().code());

		String expired = Authorization.getToken(SECRET, "access", -1, ChronoUnit.SECONDS);
		RpcStatusException expiredError = assertThrows(RpcStatusException.class,
				() -> Authorization.checkToken(context, "access", expired));
		assertEquals(Status.UNAUTHORIZED.code(), expiredError.getStatus().code());

		String refresh = Authorization.getToken(SECRET, "refresh", 60, ChronoUnit.SECONDS);
		RpcStatusException wrongSubject = assertThrows(RpcStatusException.class,
				() -> Authorization.checkToken(context, "access", refresh));
		assertEquals(Status.UNAUTHORIZED.code(), wrongSubject.getStatus().code());
	}

	@Test
	void activeClaimIsRequired() {
		assertDoesNotThrow(() -> Authorization.checkActive(claims(Map.of("status", "ACTIVE"))));

		for (Map<String, Object> claimValues : List.<Map<String, Object>>of(
				Map.of(),
				Map.of("status", "PENDING"),
				Map.of("status", "DISABLED"))) {
			RpcStatusException error = assertThrows(RpcStatusException.class,
					() -> Authorization.checkActive(claims(claimValues)));
			assertEquals(Status.UNAUTHORIZED.code(), error.getStatus().code());
		}
	}

	@ParameterizedTest(name = "role {0} satisfies requirement {1}: {2}")
	@MethodSource("roleChecks")
	void roleHierarchyIsEnforced(Role actual, Role required, boolean allowed) throws Exception {
		Claims claims = claims(Map.of("role", actual.name()));

		if (allowed) {
			assertDoesNotThrow(() -> Authorization.checkRoleAtLeast(claims, required));
		} else {
			RpcStatusException error = assertThrows(RpcStatusException.class,
					() -> Authorization.checkRoleAtLeast(claims, required));
			assertEquals(Status.UNAUTHORIZED.code(), error.getStatus().code());
		}
	}

	@Test
	void missingAndInvalidRoleClaimsAreRejected() {
		assertThrows(RpcStatusException.class,
				() -> Authorization.checkRoleAtLeast(claims(Map.of()), Role.READER));
		assertThrows(RpcStatusException.class,
				() -> Authorization.checkRoleAtLeast(
						claims(Map.of("role", "SUPERUSER")), Role.READER));
	}

	private static Stream<Arguments> roleChecks() {
		return Stream.of(
				Arguments.of(Role.READER, Role.READER, true),
				Arguments.of(Role.READER, Role.EDITOR, false),
				Arguments.of(Role.READER, Role.ADMIN, false),
				Arguments.of(Role.EDITOR, Role.READER, true),
				Arguments.of(Role.EDITOR, Role.EDITOR, true),
				Arguments.of(Role.EDITOR, Role.ADMIN, false),
				Arguments.of(Role.ADMIN, Role.READER, true),
				Arguments.of(Role.ADMIN, Role.EDITOR, true),
				Arguments.of(Role.ADMIN, Role.ADMIN, true));
	}

	private static Claims claims(Map<String, Object> values) {
		String token = Authorization.getTokenWithClaims(
				SECRET, "access", 60, ChronoUnit.SECONDS, values);
		return Authorization.parseToken(SECRET, token);
	}

	private static DiaryContext context() {
		DiaryContext context = new DiaryContext();
		context.setSecret(SECRET);
		return context;
	}

	private static String secret(String value) {
		return Base64.getEncoder().encodeToString(
				value.getBytes(StandardCharsets.UTF_8));
	}
}
