package com.rsmaxwell.diaries.response.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rsmaxwell.diaries.response.config.DbConfig;
import com.rsmaxwell.diaries.response.db.model.Diary;

public class Sql {

	private static final Logger logger = LogManager.getLogger(Sql.class);

	public static Connection connect(DbConfig dbConfig) throws SQLException {
		return connect(dbConfig, "");
	}

	public static Connection connect(DbConfig dbConfig, String database) throws SQLException {

		String url = String.format("%s%s", dbConfig.getJdbcUrl(), database);
		String username = dbConfig.getAdmin().getUsername();
		String password = dbConfig.getAdmin().getPassword();

		logger.info(String.format("Connecting to '%s' as '%s'", url, username));

		Properties connectionProps = new Properties();
		connectionProps.put("user", username);
		connectionProps.put("password", password);

		return DriverManager.getConnection(url, connectionProps);
	}

	public static boolean databaseExists(Connection con, String databaseName) throws SQLException {

		String sql = String.format("SELECT COUNT(*) FROM pg_catalog.pg_database WHERE datname = '%s'", databaseName);

		int count = 0;

		try (Statement stmt = con.createStatement()) {
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				count = rs.getInt(1);
			}
		} catch (SQLException e) {
			logger.catching(e);
			throw e;
		}

		return count > 0;
	}

	public static boolean userExists(Connection con, String username) throws SQLException {

		String sql = String.format("SELECT 1 FROM pg_roles WHERE rolname='%s'", username);

		int count = 0;

		try (Statement stmt = con.createStatement()) {
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				count = rs.getInt(1);
			}
		} catch (SQLException e) {
			logger.catching(e);
			throw e;
		}

		return count > 0;
	}

	public static boolean tableExists(Connection con, String table) throws SQLException {

		String sql = String.format("SELECT 1 FROM pg_tables WHERE tablename='%s'", table);

		int count = 0;

		try (Statement stmt = con.createStatement()) {
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				count = rs.getInt(1);
			}
		} catch (SQLException e) {
			logger.catching(e);
			throw e;
		}

		return count > 0;
	}

	public static void removePrivilagesFromUser(Connection con, String database, String username) throws Exception {
		logger.debug(String.format("removePrivilagesFromUser: '%s'", username));

		String sql = String.format("REVOKE ALL PRIVILEGES ON DATABASE %s FROM %s;", database, username);

		try (Statement stmt = con.createStatement()) {
			stmt.executeUpdate(sql);
		} catch (Exception e) {
			logger.info(sql);
			throw e;
		}
	}

	public static void deleteUser(Connection con, String username) throws Exception {
		logger.debug(String.format("deleteUser: '%s'", username));

		String sql = String.format("DROP ROLE %s;", username);

		try (Statement stmt = con.createStatement()) {
			stmt.executeUpdate(sql);
		} catch (Exception e) {
			logger.info(sql);
			throw e;
		}
	}

	public static void deleteDatabase(Connection con, String database) throws Exception {
		logger.debug(String.format("deleteDatabase: '%s'", database));

		String sql = String.format("DROP DATABASE %s", database);

		try (Statement stmt = con.createStatement()) {
			stmt.executeUpdate(sql);
		} catch (Exception e) {
			logger.info(sql);
			throw e;
		}
	}

	public static void deleteTable(Connection con, String table) throws Exception {
		logger.debug(String.format("deleteTable: %s", table));

		String sql = String.format("DROP TABLE %s", table);

		try (Statement stmt = con.createStatement()) {
			stmt.executeUpdate(sql);
		} catch (Exception e) {
			logger.info(sql);
			throw e;
		}
	}

	public static void createDatabase(Connection con, String database) throws Exception {
		logger.debug(String.format("createDatabase: '%s'", database));

		String sql = String.format("CREATE DATABASE %s", database);

		try (Statement stmt = con.createStatement()) {
			stmt.executeUpdate(sql);
		} catch (Exception e) {
			logger.info(sql);
			throw e;
		}
	}

	public static void createUser(Connection con, String username, String password) throws Exception {
		logger.debug(String.format("createUser: '%s'", username));

		String sql = String.format("CREATE USER %s WITH ENCRYPTED PASSWORD '%s';", username, password);

		try (Statement stmt = con.createStatement()) {
			stmt.executeUpdate(sql);
		} catch (Exception e) {
			logger.info(sql);
			throw e;
		}
	}

	public static void grantPrivilagesToUser(Connection con, String database, String username) throws Exception {
		logger.debug(String.format("grantPrivilagesToUser: database: '%s', username: '%s'", database, username));

		String sql = String.format("GRANT ALL PRIVILEGES ON DATABASE %s TO %s;", database, username);

		try (Statement stmt = con.createStatement()) {
			stmt.executeUpdate(sql);
		} catch (Exception e) {
			logger.info(sql);
			throw e;
		}
	}

	public static void createDiaryTable(Connection con) throws Exception {
		logger.debug("createDiaryTable");

		String format = """
				CREATE TABLE %s (
					id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
					path VARCHAR(1024) NOT NULL
				)""";

		String sql = String.format(format, Diary.TABLE_NAME);

		try (Statement stmt = con.createStatement()) {
			stmt.executeUpdate(sql);
		} catch (Exception e) {
			logger.info(sql);
			throw e;
		}
	}
}
