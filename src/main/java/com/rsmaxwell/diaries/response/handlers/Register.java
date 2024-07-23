package com.rsmaxwell.diaries.response.handlers;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rsmaxwell.mqtt.rpc.common.Result;
import com.rsmaxwell.mqtt.rpc.common.Utilities;
import com.rsmaxwell.mqtt.rpc.response.RequestHandler;

public class Register extends RequestHandler {

	private static final Logger logger = LogManager.getLogger(Register.class);

	@Override
	public Result handleRequest(Map<String, Object> args) throws Exception {
		logger.traceEntry();

		try {
			String username = Utilities.getString(args, "username");
			String password = Utilities.getString(args, "password");

			return Result.success();
		} catch (Exception e) {
			return Result.badRequest(e.getMessage());
		}
	}
}
