package com.rsmaxwell.diaries.response.handlers;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rsmaxwell.mqtt.rpc.common.Result;
import com.rsmaxwell.mqtt.rpc.common.Utilities;
import com.rsmaxwell.mqtt.rpc.response.RequestHandler;

public class Calculator extends RequestHandler {

	private static final Logger logger = LogManager.getLogger(Calculator.class);

	@Override
	public Result handleRequest(Map<String, Object> args) throws Exception {
		logger.traceEntry();

		try {
			String operation = Utilities.getString(args, "operation");
			int param1 = Utilities.getInteger(args, "param1");
			int param2 = Utilities.getInteger(args, "param2");

			int value = 0;

			switch (operation) {
			case "add":
				value = param1 + param2;
				break;
			case "mul":
				value = param1 * param2;
				break;
			case "div":
				value = param1 / param2;
				break;
			case "sub":
				value = param1 - param2;
				break;
			default:
				throw new Exception(String.format("Unexpected operation: %s", operation));
			}

			return success(value);
		} catch (Exception e) {
			return badRequest(e.getMessage());
		}
	}
}
