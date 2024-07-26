package com.rsmaxwell.diaries.response.db;

import java.sql.Connection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rsmaxwell.diaries.response.config.Config;
import com.rsmaxwell.diaries.response.config.DbConfig;
import com.rsmaxwell.diaries.response.config.User;
import com.rsmaxwell.diaries.response.db.model.Diary;

public class DeleteDatabase {

	private static final Logger logger = LogManager.getLogger(DeleteDatabase.class);

	public static void main(String[] args) throws Exception {

		Config config = Config.read();
		DbConfig dbConfig = config.getDb();
		String database = dbConfig.getDatabase();

		try (Connection con = Database.connect(dbConfig, database)) {
			deleteTables(con);
		}

		try (Connection con = Database.connect(dbConfig)) {
			deleteUsers(con, dbConfig);
			deleteDatabase(con, database);
		}

		logger.info("exiting");
	}

	public static void deleteTables(Connection con) throws Exception {
		deleteTable(con, Diary.TABLE_NAME);
	}

	public static void deleteTable(Connection con, String table) throws Exception {

		boolean found = Database.tableExists(con, table);

		if (!found) {
			logger.info(String.format("table '%s' not found", table));
			return;
		}

		Database.deleteTable(con, table);
	}

	public static void deleteUsers(Connection con, DbConfig dbConfig) throws Exception {

		String database = dbConfig.getDatabase();

		for (User user : dbConfig.getUsers()) {
			String username = user.getUsername();
			deleteUser(con, username, database);
		}
	}

	public static void deleteUser(Connection con, String username, String database) throws Exception {

		boolean found = Database.userExists(con, username);

		if (!found) {
			logger.info(String.format("user '%s' not found", username));
			return;
		}

		Database.removePrivilagesFromUser(con, database, username);
		Database.deleteUser(con, username);
	}

	public static void deleteDatabase(Connection con, String database) throws Exception {

		boolean found = Database.databaseExists(con, database);

		if (!found) {
			logger.info(String.format("Database '%s' not found", database));
			return;
		}

		Database.deleteDatabase(con, database);
	}
}
