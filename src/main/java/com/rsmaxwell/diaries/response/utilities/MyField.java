package com.rsmaxwell.diaries.response.utilities;

import lombok.Data;

@Data
public class MyField {

	private String name;
	private String column;

	public MyField(String name, String column) {
		this.name = name;
		this.column = column;
	}
}
