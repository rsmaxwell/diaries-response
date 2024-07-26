package com.rsmaxwell.diaries.response.db;

import java.sql.Connection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rsmaxwell.diaries.response.config.Config;
import com.rsmaxwell.diaries.response.config.DbConfig;
import com.rsmaxwell.diaries.response.config.User;
import com.rsmaxwell.diaries.response.db.model.Diary;

public class CreateDatabase {

	private static final Logger logger = LogManager.getLogger(CreateDatabase.class);

	public static void main(String[] args) throws Exception {

		Config config = Config.read();
		DbConfig dbConfig = config.getDb();
		String database = dbConfig.getDatabase();

		try (Connection con = Database.connect(dbConfig)) {
			createDatabase(con, database);
			createUsers(con, dbConfig);
		}

		try (Connection con = Database.connect(dbConfig, database)) {
			createTables(con);
		}

		logger.info("exiting");
	}

	public static void createTables(Connection con) throws Exception {
		createTable(con, Diary.TABLE_NAME);
	}

	public static void createTable(Connection con, String table) throws Exception {

		boolean found = Database.tableExists(con, table);

		if (found) {
			logger.info(String.format("table '%s' already exists", table));
			return;
		}

		Database.createDiaryTable(con);
	}

	public static void createUsers(Connection con, DbConfig dbConfig) throws Exception {

		String database = dbConfig.getDatabase();

		for (User user : dbConfig.getUsers()) {
			String username = user.getUsername();
			String password = user.getPassword();
			createUser(con, username, password, database);
		}
	}

	public static void createUser(Connection con, String username, String password, String database) throws Exception {

		boolean found = Database.userExists(con, username);

		if (found) {
			logger.info(String.format("user '%s' already exists", username));
			return;
		}

		Database.createUser(con, username, password);
		Database.grantPrivilagesToUser(con, database, username);
	}

	public static void createDatabase(Connection con, String database) throws Exception {

		boolean found = Database.databaseExists(con, database);

		if (found) {
			logger.info(String.format("Database '%s' already exists", database));
			return;
		}

		Database.createDatabase(con, database);
	}
}
