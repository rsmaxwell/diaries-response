package com.rsmaxwell.diaries.response.handlers;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;

import com.rsmaxwell.diaries.response.model.Person;
import com.rsmaxwell.diaries.response.utilities.DiaryContext;
import com.rsmaxwell.mqtt.rpc.common.Result;
import com.rsmaxwell.mqtt.rpc.common.Utilities;
import com.rsmaxwell.mqtt.rpc.response.RequestHandler;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

public class Register extends RequestHandler {

	private static final Logger log = LogManager.getLogger(Register.class);

	@Override
	public Result handleRequest(Object ctx, Map<String, Object> args) throws Exception {
		log.traceEntry();

		String username = Utilities.getString(args, "username");
		String password = Utilities.getString(args, "password");
		String firstname = Utilities.getString(args, "firstname");
		String lastname = Utilities.getString(args, "lastname");

		String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());

		DiaryContext context = (DiaryContext) ctx;
		EntityManagerFactory entityManagerFactory = context.getEntityManagerFactory();

		EntityTransaction tx = null;
		try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
			tx = entityManager.getTransaction();
			tx.begin();

			Person person = new Person(username, passwordHash, firstname, lastname);
			entityManager.persist(person);

			tx.commit();

			log.info(String.format("Person Registered: %s", person));

			return Result.success(person.getId());
		} catch (Exception e) {
			log.catching(e);
			if (tx != null) {
				tx.rollback();
			}
			return Result.badRequest(e.getMessage());
		}
	}
}
