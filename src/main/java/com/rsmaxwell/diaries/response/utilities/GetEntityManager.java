package com.rsmaxwell.diaries.response.utilities;

import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rsmaxwell.diaries.common.config.DbConfig;
import com.rsmaxwell.diaries.common.config.Jdbc;
import com.rsmaxwell.diaries.common.config.User;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class GetEntityManager {

	private static final Logger log = LogManager.getLogger(GetEntityManager.class);

	public static EntityManagerFactory factory(DbConfig dbConfig) {

		EntityManagerFactory entityManagerFactory = null;

		try {
			Jdbc jdbc = dbConfig.getJdbc();
			List<User> users = dbConfig.getUsers();
			if (users.size() <= 0) {
				throw new Exception("No users defined in configuration");
			}
			User user = users.get(0);

			Properties props = new Properties();
			props.put("jakarta.persistence.jdbc.url", dbConfig.getJdbcUrlWithDatabase());
			props.put("jakarta.persistence.jdbc.driver", jdbc.getDriver());
			props.put("jakarta.persistence.jdbc.user", user.getUsername());
			props.put("jakarta.persistence.jdbc.password", user.getPassword());

			log.debug("JDBC properties:");
			for (String key : props.stringPropertyNames()) {
				log.debug(String.format("    %s : %s", key, props.getProperty(key)));
			}

			entityManagerFactory = Persistence.createEntityManagerFactory("com.rsmaxwell.diaries", props);

		} catch (Throwable ex) {
			System.err.println("Initial SessionFactory creation failed." + ex);
			throw new ExceptionInInitializerError(ex);
		}

		return entityManagerFactory;
	}
}
