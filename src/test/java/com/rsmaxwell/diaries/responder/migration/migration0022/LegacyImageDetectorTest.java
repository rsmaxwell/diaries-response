package com.rsmaxwell.diaries.responder.migration.migration0022;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class LegacyImageDetectorTest {

	@Test
	void findsImagesInTolerantHtmlAndPreservesDocumentOrder() throws Exception {
		String html = "<p>Before<img src='/files/one.jpg'><IMG alt='missing source'>"
				+ "<img src=\"/files/two.png\" /></p>";

		assertEquals(
				List.of("/files/one.jpg", "", "/files/two.png"),
				LegacyImageDetector.findImageSources(html));
	}

	@Test
	void doesNotTreatEscapedOrPlainTextAsAnImageElement() throws Exception {
		assertEquals(List.of(), LegacyImageDetector.findImageSources(
				"<p>&lt;img src='/not-an-element.jpg'&gt; ordinary text</p>"));
	}

	@Test
	void handlesNullAndBlankInput() throws Exception {
		assertEquals(List.of(), LegacyImageDetector.findImageSources(null));
		assertEquals(List.of(), LegacyImageDetector.findImageSources("   "));
	}
}
