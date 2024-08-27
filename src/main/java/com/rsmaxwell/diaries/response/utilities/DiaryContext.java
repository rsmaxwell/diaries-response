package com.rsmaxwell.diaries.response.utilities;

import com.rsmaxwell.diaries.common.config.Diaries;

import jakarta.persistence.EntityManagerFactory;
import lombok.Data;

@Data
public class DiaryContext {

	private EntityManagerFactory entityManagerFactory;
	private String secret;
	private Diaries diaries;
}
