package com.rsmaxwell.diaries.response.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PersonDTO {

	private long id;
	private String username;
	private String firstName;
	private String lastName;
}
