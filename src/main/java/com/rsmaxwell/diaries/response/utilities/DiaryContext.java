package com.rsmaxwell.diaries.response.utilities;

import com.rsmaxwell.diaries.common.config.DiariesConfig;
import com.rsmaxwell.diaries.response.repository.DiaryRepository;
import com.rsmaxwell.diaries.response.repository.PageRepository;

import jakarta.persistence.EntityManagerFactory;
import lombok.Data;

@Data
public class DiaryContext {

	private EntityManagerFactory entityManagerFactory;
	private DiaryRepository diaryRepository;
	private PageRepository pageRepository;
	private String secret;
	private DiariesConfig diaries;

}
