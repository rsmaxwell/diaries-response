package com.rsmaxwell.diaries.responder.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rsmaxwell.diaries.responder.utilities.DiaryContext;
import com.rsmaxwell.mqtt.rpc.common.Response;
import com.rsmaxwell.mqtt.rpc.common.Status;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;

@SuppressWarnings({ "unchecked", "rawtypes" })
class HealthTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	void successfulProbeReturnsUpAndClosesShortLivedEntityManager() throws Exception {
		JpaProbeHarness harness = new JpaProbeHarness();
		DiaryContext context = context(harness.entityManagerFactory());

		Response response = new Health().handleRequest(context, Map.of(), List.of());

		assertEquals(Status.OK, response.status());
		assertEquals(Map.of("status", "UP"), response.payload());
		assertEquals("SELECT 1", harness.queryText.get());
		assertTrue(harness.entityManagerClosed.get());
		assertEquals("{\"status\":\"UP\"}", MAPPER.writeValueAsString(response.payload()));
	}

	@Test
	void entityManagerCreationFailureReturnsInternalError() {
		JpaProbeHarness harness = new JpaProbeHarness();
		harness.createFailure = new IllegalStateException("database unavailable");

		Response response = new Health().handleRequest(context(harness.entityManagerFactory()), Map.of(), List.of());

		assertEquals(Status.INTERNAL_ERROR.code(), response.status().code());
		assertEquals("database health probe failed", response.status().message());
		assertNull(response.payload());
		assertFalse(harness.entityManagerClosed.get());
	}

	@Test
	void failedQueryReturnsInternalErrorAndClosesShortLivedEntityManager() {
		JpaProbeHarness harness = new JpaProbeHarness();
		harness.queryFailure = new IllegalStateException("query failed");

		Response response = new Health().handleRequest(context(harness.entityManagerFactory()), Map.of(), List.of());

		assertEquals(Status.INTERNAL_ERROR.code(), response.status().code());
		assertEquals("database health probe failed", response.status().message());
		assertNull(response.payload());
		assertTrue(harness.entityManagerClosed.get());
	}

	@Test
	void closedEntityManagerFactoryReturnsInternalError() {
		JpaProbeHarness harness = new JpaProbeHarness();
		harness.factoryOpen = false;

		Response response = new Health().handleRequest(context(harness.entityManagerFactory()), Map.of(), List.of());

		assertEquals(Status.INTERNAL_ERROR.code(), response.status().code());
		assertFalse(harness.createEntityManagerCalled.get());
	}

	@Test
	void missingEntityManagerFactoryReturnsInternalError() {
		Response response = new Health().handleRequest(new DiaryContext(), Map.of(), List.of());

		assertEquals(Status.INTERNAL_ERROR.code(), response.status().code());
		assertEquals("database health probe failed", response.status().message());
	}

	@Test
	void sharedEntityManagerIsNotUsed() {
		JpaProbeHarness harness = new JpaProbeHarness();
		AtomicBoolean sharedEntityManagerUsed = new AtomicBoolean();
		DiaryContext context = context(harness.entityManagerFactory());
		context.setEntityManager(proxy(EntityManager.class, (proxy, method, args) -> {
			sharedEntityManagerUsed.set(true);
			throw new AssertionError("shared EntityManager must not be used by health probe");
		}));

		Response response = new Health().handleRequest(context, Map.of(), List.of());

		assertEquals(Status.OK, response.status());
		assertFalse(sharedEntityManagerUsed.get());
	}

	private static DiaryContext context(EntityManagerFactory entityManagerFactory) {
		DiaryContext context = new DiaryContext();
		context.setEntityManagerFactory(entityManagerFactory);
		return context;
	}

	@SuppressWarnings("unchecked")
	private static <T> T proxy(Class<T> type, InvocationHandler handler) {
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler);
	}

	private static final class JpaProbeHarness {

		private final AtomicBoolean createEntityManagerCalled = new AtomicBoolean();
		private final AtomicBoolean entityManagerClosed = new AtomicBoolean();
		private final AtomicReference<String> queryText = new AtomicReference<>();
		private boolean factoryOpen = true;
		private RuntimeException createFailure;
		private RuntimeException queryFailure;

		EntityManagerFactory entityManagerFactory() {
			TypedQuery<Integer> query = proxy(TypedQuery.class, (proxy, method, args) -> {
				if ("getSingleResult".equals(method.getName())) {
					if (queryFailure != null) {
						throw queryFailure;
					}
					return 1;
				}
				return defaultValue(method.getReturnType());
			});

			EntityManager entityManager = proxy(EntityManager.class, (proxy, method, args) -> {
				if ("createNativeQuery".equals(method.getName())) {
					queryText.set((String) args[0]);
					return query;
				}
				if ("close".equals(method.getName())) {
					entityManagerClosed.set(true);
					return null;
				}
				return defaultValue(method.getReturnType());
			});

			return proxy(EntityManagerFactory.class, (proxy, method, args) -> {
				if ("isOpen".equals(method.getName())) {
					return factoryOpen;
				}
				if ("createEntityManager".equals(method.getName())) {
					createEntityManagerCalled.set(true);
					if (createFailure != null) {
						throw createFailure;
					}
					return entityManager;
				}
				return defaultValue(method.getReturnType());
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
}
