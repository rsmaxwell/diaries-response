package com.rsmaxwell.diaries.response;

import java.sql.Connection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rsmaxwell.diaries.common.config.Config;
import com.rsmaxwell.diaries.common.config.DbConfig;
import com.rsmaxwell.diaries.common.config.User;

public class DeleteDatabase {

	private static final Logger log = LogManager.getLogger(DeleteDatabase.class);

	public static void main(String[] args) throws Exception {

		Config config = Config.read();
		DbConfig dbConfig = config.getDb();
		String database = dbConfig.getDatabase();

		try (Connection con = Database.connect(dbConfig)) {
			deleteUsers(con, dbConfig);
			deleteDatabase(con, database);
		}

		log.info("Success");
	}

	public static void deleteUsers(Connection con, DbConfig dbConfig) throws Exception {

		String database = dbConfig.getDatabase();

		for (User user : dbConfig.getUsers()) {
			String username = user.getUsername();

			deleteUser(con, username, database);
		}
	}

	public static void deleteUser(Connection con, String username, String database) throws Exception {

		boolean userFound = Database.userExists(con, username);

		if (!userFound) {
			log.info(String.format("user '%s' not found", username));
			return;
		}

		boolean databaseFound = Database.databaseExists(con, database);
		if (databaseFound) {
			Database.removePrivilagesFromUser(con, database, username);
		}

		Database.reAssignUserRoles(con, username, "postgres");
		Database.dropOwnedByUser(con, username);
		Database.deleteUser(con, username);
	}

	public static void deleteDatabase(Connection con, String database) throws Exception {

		boolean found = Database.databaseExists(con, database);

		if (!found) {
			log.info(String.format("Database '%s' not found", database));
			return;
		}

		Database.deleteDatabase(con, database);
	}
}
