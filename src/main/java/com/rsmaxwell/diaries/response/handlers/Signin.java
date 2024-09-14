package com.rsmaxwell.diaries.response.handlers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;

import com.rsmaxwell.diaries.response.model.Person;
import com.rsmaxwell.diaries.response.repository.PersonRepository;
import com.rsmaxwell.diaries.response.utilities.DiaryContext;
import com.rsmaxwell.mqtt.rpc.common.Response;
import com.rsmaxwell.mqtt.rpc.common.Result;
import com.rsmaxwell.mqtt.rpc.common.Utilities;
import com.rsmaxwell.mqtt.rpc.response.RequestHandler;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class Signin extends RequestHandler {

	private static final Logger log = LogManager.getLogger(Signin.class);

	@Override
	public Result handleRequest(Object ctx, Map<String, Object> args) throws Exception {
		log.traceEntry();

		String username = Utilities.getString(args, "username");
		String password = Utilities.getString(args, "password");

		DiaryContext context = (DiaryContext) ctx;
		PersonRepository personRepository = context.getPersonRepository();
		String secret = context.getSecret();

		Optional<Person> optional = personRepository.findFullByUsername(username);

		if (optional.isEmpty()) {
			return Result.badRequest("bad username or password");
		}

		Person person = optional.get();

		boolean ok = BCrypt.checkpw(password, person.getPasswordHash());
		if (ok) {
			Response response = Response.success();
			response.put("accessToken", getToken(secret, "access", 1));
			response.put("refreshToken", getToken(secret, "refresh", 5));
			response.put("refreshDelta", 4 * 60);
			response.put("id", person.getId());
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
