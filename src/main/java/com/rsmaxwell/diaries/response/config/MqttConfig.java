package com.rsmaxwell.diaries.response.config;

import lombok.Data;

@Data
public class MqttConfig {

	private String host;
	private int port;
	private String Username;
	private String Password;

	public String getServer() {
		return String.format("tcp://%s:%d", host, port);
	}

}
