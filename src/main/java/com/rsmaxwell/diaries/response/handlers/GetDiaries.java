package com.rsmaxwell.diaries.response.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rsmaxwell.diaries.response.model.Diary;
import com.rsmaxwell.mqtt.rpc.common.Result;
import com.rsmaxwell.mqtt.rpc.response.RequestHandler;

public class GetDiaries extends RequestHandler {

	private static final Logger log = LogManager.getLogger(GetDiaries.class);

	static private ObjectMapper mapper = new ObjectMapper();

	public Result handleRequest(Object ctx, Map<String, Object> args) throws Exception {

		List<Diary> list = new ArrayList<Diary>();
		list.add(new Diary(15L, "diary-1828-and-1829-and-jan-1830"));
		list.add(new Diary(16L, "diary-1830"));
		list.add(new Diary(17L, "diary-1831"));
		list.add(new Diary(18L, "diary-1832"));
		list.add(new Diary(19L, "diary-1834"));
		list.add(new Diary(20L, "diary-1835"));
		list.add(new Diary(21L, "diary-1836"));
		list.add(new Diary(22L, "diary-1837"));
		list.add(new Diary(23L, "diary-1838"));
		list.add(new Diary(24L, "diary-1839"));

		String json = mapper.writeValueAsString(list);
		log.info(json);

		return Result.success(list);
	}
}
