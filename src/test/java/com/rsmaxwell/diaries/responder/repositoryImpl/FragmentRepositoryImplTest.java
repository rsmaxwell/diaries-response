package com.rsmaxwell.diaries.responder.repositoryImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.rsmaxwell.diaries.responder.dto.FragmentDBDTO;
import com.rsmaxwell.diaries.responder.model.Fragment;
import com.rsmaxwell.diaries.responder.model.FragmentType;

class FragmentRepositoryImplTest {

	private final FragmentRepositoryImpl repository = new FragmentRepositoryImpl(null);

	@Test
	void positionalMappingIncludesPageAndTypeBeforeLockColumns() {
		Object[] row = {
				12L, 3L, new BigDecimal("4.0000"), 1830, 3, 8, "Text",
				85L, "MARQUEE", 42L, "alice", "Ali", 123456L, "session"
		};

		FragmentDBDTO dto = repository.newDTO(row);

		assertEquals(85L, dto.getPageId());
		assertEquals(FragmentType.MARQUEE, dto.getType());
		assertEquals(42L, dto.getLock().lockUserId());
		assertEquals("session", dto.getLock().lockSessionId());
	}

	@Test
	void migrationNullsRemainNullAndRoundTripThroughRepositoryValues() {
		Object[] row = {
				13L, 0L, BigDecimal.ONE, 1830, 3, 9, "Candidate",
				null, null, null, null, null, null, null
		};

		FragmentDBDTO dto = repository.newDTO(row);
		Fragment fragment = new Fragment(dto);

		assertNull(dto.getPageId());
		assertNull(dto.getType());
		assertNull(fragment.getPageId());
		assertEquals(repository.getFields().size(), repository.getValues(fragment).size());
	}
}
