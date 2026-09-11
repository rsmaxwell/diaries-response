package com.rsmaxwell.diaries.responder.migration.migration0022;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.rsmaxwell.diaries.responder.migration.migration0022.Migration0022Inventory.Classification;
import com.rsmaxwell.diaries.responder.migration.migration0022.Migration0022Inventory.InventoryRecord;
import com.rsmaxwell.diaries.responder.migration.migration0022.Migration0022Inventory.MarqueeReference;
import com.rsmaxwell.diaries.responder.migration.migration0022.Migration0022Inventory.MutableRecord;

class Migration0022InventoryTest {

	@Test
	void classifiesOrdinaryFragmentWithOneValidMarqueeAsSafe() throws Exception {
		MutableRecord row = row(1L, "<p>ordinary transcription</p>");
		row.marquees.add(new MarqueeReference(11L, 101L, true));

		InventoryRecord result = row.classify();

		assertEquals(Classification.ORDINARY_MARQUEE_CANDIDATE, result.classification());
		assertEquals(101L, result.pageId());
		assertEquals(11L, result.marqueeId());
	}

	@Test
	void classifiesEmbeddedImageFragmentForReviewRatherThanAsMarquee() throws Exception {
		MutableRecord row = row(2L, "<p><img src='/files/view.jpg'>A view</p>");
		row.marquees.add(new MarqueeReference(12L, 102L, true));

		InventoryRecord result = row.classify();

		assertEquals(Classification.LEGACY_IMAGE_CANDIDATE, result.classification());
		assertEquals(1, result.imageSources().size());
		assertEquals("/files/view.jpg", result.imageSources().get(0));
	}

	@Test
	void classifiesFragmentWithoutMarqueeAsAnomaly() throws Exception {
		InventoryRecord result = row(3L, "orphan").classify();

		assertEquals(Classification.ORPHAN_OR_INCONSISTENT, result.classification());
		assertNull(result.pageId());
		assertNull(result.marqueeId());
		assertEquals("fragment has no marquee", result.reason());
	}

	private static MutableRecord row(long id, String text) {
		return new MutableRecord(id, 4L, 1830, 3, 8, BigDecimal.ONE, text, "expected-md5");
	}
}
