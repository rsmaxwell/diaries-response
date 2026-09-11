package com.rsmaxwell.diaries.responder.migration.migration0022;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

/** Tolerant HTML parser used only to inventory legacy embedded images. */
public final class LegacyImageDetector {

	private LegacyImageDetector() {
	}

	public static List<String> findImageSources(String html) throws IOException {
		List<String> sources = new ArrayList<>();
		if (html == null || html.isBlank()) {
			return sources;
		}

		HTMLEditorKit.ParserCallback callback = new HTMLEditorKit.ParserCallback() {
			private void capture(HTML.Tag tag, MutableAttributeSet attributes) {
				if (tag != HTML.Tag.IMG) {
					return;
				}
				Object source = attributes.getAttribute(HTML.Attribute.SRC);
				sources.add(source == null ? "" : source.toString());
			}

			@Override
			public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
				capture(tag, attributes);
			}

			@Override
			public void handleStartTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
				capture(tag, attributes);
			}
		};

		new ParserDelegator().parse(new StringReader(html), callback, true);
		return sources;
	}
}
