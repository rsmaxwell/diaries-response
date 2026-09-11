package com.rsmaxwell.diaries.responder.model;

public enum FragmentType {
	MARQUEE,
	IMAGE;

	public static FragmentType fromDatabaseValue(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return FragmentType.valueOf(value);
	}
}
