package com.rsmaxwell.diaries.responder.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import com.rsmaxwell.diaries.responder.dto.PersonDTO;
import com.rsmaxwell.diaries.responder.model.Role;
import com.rsmaxwell.diaries.responder.model.UserStatus;
import com.rsmaxwell.diaries.responder.repository.PersonRepository;
import com.rsmaxwell.diaries.responder.response.RefreshTokenReply;
import com.rsmaxwell.diaries.responder.response.SigninReply;
import com.rsmaxwell.diaries.responder.utilities.Authorization;
import com.rsmaxwell.diaries.responder.utilities.DiaryContext;
import com.rsmaxwell.mqtt.rpc.common.Response;
import com.rsmaxwell.mqtt.rpc.common.Status;
import com.rsmaxwell.mqtt.rpc.exceptions.RpcStatusException;

import io.jsonwebtoken.Claims;

class AuthenticationHandlerTest {

	private static final String SECRET = Base64.getEncoder().encodeToString(
			"01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));
	private static final String PASSWORD = "correct horse battery staple";

	@Test
	void successfulSigninReturnsTokensWithTheAuthenticatedIdentityAndSession() throws Exception {
		PersonDTO person = person(UserStatus.ACTIVE, Role.EDITOR);
		DiaryContext context = context(repository(Optional.of(person), Optional.of(person)));

		Response response = new Signin().handleRequest(
				context, signinArgs(PASSWORD), List.of());

		assertEquals(Status.OK, response.status());
		SigninReply reply = (SigninReply) response.payload();
		assertEquals(person.getId(), reply.userId());
		assertEquals(person.getUsername(), reply.username());
		assertEquals(person.getKnownas(), reply.knownAs());
		assertEquals("browser-session", reply.sessionId());
		assertEquals("ACTIVE", reply.status());
		assertEquals("EDITOR", reply.role());
		assertEquals(300, reply.refreshPeriod());
		assertNotNull(reply.accessToken());
		assertNotNull(reply.refreshToken());

		Claims accessClaims = Authorization.parseToken(SECRET, reply.accessToken());
		assertEquals("access", accessClaims.getSubject());
		assertEquals(person.getId(), ((Number) accessClaims.get("userId")).longValue());
		assertEquals("browser-session", accessClaims.get("sessionId"));
		assertEquals("ACTIVE", accessClaims.get("status"));
		assertEquals("EDITOR", accessClaims.get("role"));

		Claims refreshClaims = Authorization.parseToken(SECRET, reply.refreshToken());
		assertEquals("refresh", refreshClaims.getSubject());
		assertEquals(person.getId(), ((Number) refreshClaims.get("userId")).longValue());
		assertEquals("browser-session", refreshClaims.get("sessionId"));
	}

	@Test
	void unknownUsernameAndWrongPasswordReturnTheSameFailure() {
		PersonDTO person = person(UserStatus.ACTIVE, Role.EDITOR);
		DiaryContext unknownUserContext = context(repository(Optional.empty(), Optional.empty()));
		DiaryContext wrongPasswordContext = context(repository(Optional.of(person), Optional.of(person)));

		RpcStatusException unknownUser = assertThrows(RpcStatusException.class,
				() -> new Signin().handleRequest(
						unknownUserContext, signinArgs(PASSWORD), List.of()));
		RpcStatusException wrongPassword = assertThrows(RpcStatusException.class,
				() -> new Signin().handleRequest(
						wrongPasswordContext, signinArgs("wrong password"), List.of()));

		assertEquals(Status.BAD_REQUEST.code(), unknownUser.getStatus().code());
		assertEquals(unknownUser.getStatus(), wrongPassword.getStatus());
	}

	@Test
	void signinRejectsAccountsThatAreNotActiveOrHaveNoRole() {
		for (PersonDTO person : List.of(
				person(null, Role.EDITOR),
				person(UserStatus.PENDING, Role.EDITOR),
				person(UserStatus.DISABLED, Role.EDITOR),
				person(UserStatus.ACTIVE, null))) {
			DiaryContext context = context(repository(Optional.of(person), Optional.of(person)));

			RpcStatusException error = assertThrows(RpcStatusException.class,
					() -> new Signin().handleRequest(
							context, signinArgs(PASSWORD), List.of()));

			assertEquals(Status.FORBIDDEN.code(), error.getStatus().code());
		}
	}

	@Test
	void refreshUsesCurrentAccountDetailsAndPreservesTheSession() throws Exception {
		PersonDTO person = person(UserStatus.ACTIVE, Role.ADMIN);
		DiaryContext context = context(repository(Optional.of(person), Optional.of(person)));
		String refreshToken = refreshToken(person.getId(), "original-session");

		Response response = new RefreshToken().handleRequest(
				context, Map.of("refreshToken", refreshToken), List.of());

		assertEquals(Status.OK, response.status());
		RefreshTokenReply reply = (RefreshTokenReply) response.payload();
		assertEquals(300, reply.refreshPeriod());

		Claims claims = Authorization.parseToken(SECRET, reply.accessToken());
		assertEquals("access", claims.getSubject());
		assertEquals(person.getId(), ((Number) claims.get("userId")).longValue());
		assertEquals(person.getUsername(), claims.get("username"));
		assertEquals(person.getKnownas(), claims.get("knownAs"));
		assertEquals("original-session", claims.get("sessionId"));
		assertEquals("ACTIVE", claims.get("status"));
		assertEquals("ADMIN", claims.get("role"));
	}

	@Test
	void refreshRejectsMissingInactiveAndRolelessAccounts() {
		String refreshToken = refreshToken(7L, "browser-session");

		DiaryContext missingContext = context(repository(Optional.empty(), Optional.empty()));
		RpcStatusException missing = assertThrows(RpcStatusException.class,
				() -> new RefreshToken().handleRequest(
						missingContext, Map.of("refreshToken", refreshToken), List.of()));
		assertEquals(Status.INTERNAL_ERROR.code(), missing.getStatus().code());

		for (PersonDTO person : List.of(
				person(null, Role.EDITOR),
				person(UserStatus.DISABLED, Role.EDITOR),
				person(UserStatus.ACTIVE, null))) {
			DiaryContext context = context(repository(Optional.of(person), Optional.of(person)));
			RpcStatusException error = assertThrows(RpcStatusException.class,
					() -> new RefreshToken().handleRequest(
							context, Map.of("refreshToken", refreshToken), List.of()));
			assertEquals(Status.FORBIDDEN.code(), error.getStatus().code());
		}
	}

	private static Map<String, Object> signinArgs(String password) {
		return Map.of(
				"username", "alice",
				"password", password,
				"sessionId", "browser-session");
	}

	private static PersonDTO person(UserStatus status, Role role) {
		return PersonDTO.builder()
				.id(7L)
				.version(0L)
				.username("alice")
				.passwordHash(BCrypt.hashpw(PASSWORD, BCrypt.gensalt(4)))
				.firstName("Alice")
				.lastName("Example")
				.knownas("Ali")
				.email("alice@example.com")
				.countryCode(44)
				.nationalNumber(1234567890L)
				.status(status)
				.role(role)
				.build();
	}

	private static String refreshToken(Long userId, String sessionId) {
		return Authorization.getTokenWithClaims(
				SECRET,
				"refresh",
				3600,
				ChronoUnit.SECONDS,
				Map.of("userId", userId, "sessionId", sessionId));
	}

	private static DiaryContext context(PersonRepository repository) {
		DiaryContext context = new DiaryContext();
		context.setPersonRepository(repository);
		context.setSecret(SECRET);
		context.setRefreshPeriod(300);
		context.setRefreshExpiration(3600);
		return context;
	}

	private static PersonRepository repository(
			Optional<PersonDTO> byUsername,
			Optional<PersonDTO> byId) {
		return (PersonRepository) Proxy.newProxyInstance(
				PersonRepository.class.getClassLoader(),
				new Class<?>[] { PersonRepository.class },
				(proxy, method, args) -> switch (method.getName()) {
				case "findByUsername" -> byUsername;
				case "findById" -> byId;
				default -> defaultValue(method.getReturnType());
				});
	}

	private static Object defaultValue(Class<?> type) {
		if (!type.isPrimitive()) {
			return null;
		}
		if (type == boolean.class) {
			return false;
		}
		if (type == char.class) {
			return '\0';
		}
		return 0;
	}
}
