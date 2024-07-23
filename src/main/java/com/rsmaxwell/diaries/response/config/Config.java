package com.rsmaxwell.diaries.response.config;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;

@Data
public class Config {

	static private ObjectMapper mapper = new ObjectMapper();

	private MqttConfig mqtt;
	private DbConfig db;

	public static Config get() throws StreamReadException, DatabindException, IOException {
		String home = System.getProperty("user.home");
		Path filePath = Paths.get(home, ".diaries", "responder.json");
		return mapper.readValue(filePath.toFile(), Config.class);
	}
}
