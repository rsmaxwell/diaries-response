package com.rsmaxwell.diaries.response.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rsmaxwell.diaries.response.model.Diary;
import com.rsmaxwell.diaries.response.repository.DiaryRepository;
import com.rsmaxwell.diaries.response.utilities.DiaryContext;
import com.rsmaxwell.mqtt.rpc.common.Result;
import com.rsmaxwell.mqtt.rpc.response.RequestHandler;

public class GetDiaries extends RequestHandler {

	private static final Logger log = LogManager.getLogger(GetDiaries.class);

	static private ObjectMapper mapper = new ObjectMapper();

	public Result handleRequest(Object ctx, Map<String, Object> args) throws Exception {

		DiaryContext context = (DiaryContext) ctx;

		DiaryRepository diaryRepository = context.getDiaryRepository();

		List<Diary> diaries = new ArrayList<Diary>();
		Iterable<Diary> all = diaryRepository.findAll();
		for (Diary diary : all) {
			diaries.add(diary);
		}

		String json = mapper.writeValueAsString(diaries);
		log.info(json);

		return Result.success(json);
	}
}
