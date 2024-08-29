package com.rsmaxwell.diaries.response.utilities;

import com.rsmaxwell.diaries.common.config.Diaries;
import com.rsmaxwell.diaries.response.repository.DiaryRepository;

import jakarta.persistence.EntityManagerFactory;
import lombok.Data;

@Data
public class DiaryContext {

	private EntityManagerFactory entityManagerFactory;
	private DiaryRepository diaryRepository;
	private String secret;
	private Diaries diaries;

}
