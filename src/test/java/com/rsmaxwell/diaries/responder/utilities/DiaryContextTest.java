package com.rsmaxwell.diaries.responder.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rsmaxwell.diaries.responder.dto.FragmentDBDTO;
import com.rsmaxwell.diaries.responder.model.FragmentType;
import com.rsmaxwell.diaries.responder.repository.DiaryRepository;
import com.rsmaxwell.diaries.responder.repository.FragmentRepository;
import com.rsmaxwell.diaries.responder.repository.MarqueeRepository;

class DiaryContextTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	void databaseReplayPublishesAFragmentWithoutDependingOnAMarquee() throws Exception {
		FragmentDBDTO fragment = FragmentDBDTO.builder()
				.id(33L)
				.version(2L)
				.pageId(null)
				.type(null)
				.year(1830)
				.month(3)
				.day(8)
				.sequence(BigDecimal.ONE)
				.text("Legacy candidate")
				.build();

		DiaryContext context = new DiaryContext();
		context.setDiaryRepository(proxy(DiaryRepository.class, Map.of("findAll", List.of())));
		context.setFragmentRepository(proxy(FragmentRepository.class, Map.of(
				"findAll", List.of(fragment),
				"findById", Optional.of(fragment))));
		context.setMarqueeRepository(proxy(MarqueeRepository.class, Map.of(
				"findByFragment", Optional.empty())));

		Map<String, String> retained = context.loadFromDatabase();

		assertEquals(2, retained.size());
		assertTrue(retained.containsKey("diaries/fragments/33"));
		assertTrue(retained.containsKey("diaries/dates/1830/3/8/33"));
		JsonNode payload = MAPPER.readTree(retained.get("diaries/fragments/33"));
		assertTrue(payload.get("pageId").isNull());
		assertTrue(payload.get("type").isNull());
	}

	@SuppressWarnings("unchecked")
	private static <T> T proxy(Class<T> type, Map<String, Object> results) {
		return (T) Proxy.newProxyInstance(
				type.getClassLoader(),
				new Class<?>[] { type },
				(instance, method, args) -> {
					if (results.containsKey(method.getName())) {
						return results.get(method.getName());
					}
					if (method.getName().equals("toString")) {
						return type.getSimpleName() + " test proxy";
					}
					throw new UnsupportedOperationException(method.getName());
				});
	}
}
