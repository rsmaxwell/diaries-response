package com.rsmaxwell.diaries.response.handlers;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rsmaxwell.diaries.response.model.Diary;
import com.rsmaxwell.diaries.response.utilities.DiaryContext;
import com.rsmaxwell.mqtt.rpc.common.Result;
import com.rsmaxwell.mqtt.rpc.response.RequestHandler;

public class GetDiaries extends RequestHandler {

	private static final Logger log = LogManager.getLogger(GetDiaries.class);

	static private ObjectMapper mapper = new ObjectMapper();

	public Result handleRequest(Object ctx, Map<String, Object> args) throws Exception {

		DiaryContext context = (DiaryContext) ctx;

		String root = context.getDiaries().getPath();
		File rootDir = new File(root);

		File[] directories = rootDir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File f, String name) {
				if (!f.isDirectory()) {
					return false;
				}
				if (!name.startsWith("diary")) {
					return false;
				}
				return true;
			}
		});

		List<Diary> list = new ArrayList<Diary>();
		for (File dir : directories) {
			list.add(new Diary(dir.getName()));
		}

		String json = mapper.writeValueAsString(list);
		log.info(json);

		return Result.success(list);
	}
}
