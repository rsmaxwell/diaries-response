package com.rsmaxwell.diaries.response.handlers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;

import com.rsmaxwell.diaries.response.model.Person;
import com.rsmaxwell.diaries.response.utilities.DiaryContext;
import com.rsmaxwell.mqtt.rpc.common.Response;
import com.rsmaxwell.mqtt.rpc.common.Result;
import com.rsmaxwell.mqtt.rpc.common.Utilities;
import com.rsmaxwell.mqtt.rpc.response.RequestHandler;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;

public class Signin extends RequestHandler {

	private static final Logger log = LogManager.getLogger(Signin.class);

	EntityManager entityManager;

	@SuppressWarnings("unchecked")
	@Override
	public Result handleRequest(Object ctx, Map<String, Object> args) throws Exception {
		log.traceEntry();

		String username = Utilities.getString(args, "username");
		String password = Utilities.getString(args, "password");

		DiaryContext context = (DiaryContext) ctx;
		EntityManagerFactory entityManagerFactory = context.getEntityManagerFactory();
		String secret = context.getSecret();

		List<Person> list = null;
		try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {

			Query query = entityManager.createNativeQuery("select * from person where username = :username ", Person.class);
			query.setParameter("username", username);
			list = query.getResultList();
		}

		int size = list.size();
		if (size < 0) {
			return Result.internalError(String.format("unexpected result: %d", size));
		} else if (size == 0) {
			return Result.badRequest("bad username or password");
		} else if (size > 1) {
			return Result.internalError(String.format("unexpected result: %d", size));
		}

		Person person = list.get(0);

		boolean ok = BCrypt.checkpw(password, person.getPasswordHash());
		if (ok) {
			Response response = Response.success();
			response.put("accessToken", getToken(secret, "access", 1));
			response.put("refreshToken", getToken(secret, "refresh", 5));
			return new Result(response, false);
		} else {
			return Result.badRequest("bad username or password");
		}
	}

	private String getToken(String secret, String subject, int expiration) {

		Instant now = Instant.now();
		byte[] secretBytes = Base64.getDecoder().decode(secret);

		String jwt = null;
		try {
		// @formatter:off
		    jwt = Jwts.builder()
				.subject(subject)
		        .claim("id20", new Random().nextInt(20) + 1)
		        .expiration(Date.from(now.plus(expiration, ChronoUnit.MINUTES)))
		        .signWith(Keys.hmacShaKeyFor(secretBytes))
		        .compact(); 
		// @formatter:on
		} catch (Throwable t) {
			log.catching(t);
		}

		return jwt;
	}
}
