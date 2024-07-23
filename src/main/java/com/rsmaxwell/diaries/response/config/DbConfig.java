package com.rsmaxwell.diaries.response.config;

import lombok.Data;

@Data
public class DbConfig {

	private Go go;
	private Jdbc jdbc;
	private String host;
	private int port;
	private String database;
	private String username;
	private String password;

	public String getJdbcUrl() {
		return String.format("jdbc:%s://%s:%d/", jdbc.getDbms(), host, port);
	}

	public String getJdbcUrlWithDatabase() {
		return String.format("jdbc:%s://%s:%d/%s", jdbc.getDbms(), host, port, database);
	}
}
