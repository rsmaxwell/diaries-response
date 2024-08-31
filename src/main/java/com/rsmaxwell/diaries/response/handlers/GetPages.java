package com.rsmaxwell.diaries.response.handlers;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rsmaxwell.diaries.common.config.Diaries;
import com.rsmaxwell.diaries.response.utilities.DiaryContext;
import com.rsmaxwell.mqtt.rpc.common.Result;
import com.rsmaxwell.mqtt.rpc.common.Utilities;
import com.rsmaxwell.mqtt.rpc.response.RequestHandler;

public class GetPages extends RequestHandler {

	private static final Logger logger = LogManager.getLogger(GetPages.class);

	public Result handleRequest(Object ctx, Map<String, Object> args) throws Exception {
		logger.traceEntry();

		String diaryName = Utilities.getString(args, "diary");

		DiaryContext diaryContext = (DiaryContext) ctx;
		Diaries diaries = diaryContext.getDiaries();
		String working = diaries.getWorking();

		File workingDir = Paths.get(working, diaryName).toFile();

		File[] pageDirectories = workingDir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File f, String name) {
				if (!f.isDirectory()) {
					return false;
				}
				if (!name.startsWith("img")) {
					return false;
				}
				return true;
			}
		});

		List<String> pages = new ArrayList<String>();
		for (File pageDir : pageDirectories) {
			String pageName = pageDir.getName();
			File thumbnail = Paths.get(working, diaryName, pageName, "thumb-100.jpg").toFile();

			if (!thumbnail.exists()) {
				throw new Exception(String.format("Thumbnail '%s' not found", thumbnail.getAbsoluteFile()));
			}

			pages.add(pageName);
		}

		return Result.success(pages);
	}
}
