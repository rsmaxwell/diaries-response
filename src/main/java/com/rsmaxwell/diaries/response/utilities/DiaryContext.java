package com.rsmaxwell.diaries.response.utilities;

import jakarta.persistence.EntityManagerFactory;
import lombok.Data;

@Data
public class DiaryContext {

	private EntityManagerFactory entityManagerFactory;
	private String secret;
}
