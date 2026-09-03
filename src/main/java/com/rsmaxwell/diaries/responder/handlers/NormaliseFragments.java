package com.rsmaxwell.diaries.responder.handlers;

import java.util.List;
import java.util.Map;

import org.eclipse.paho.mqttv5.common.packet.UserProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rsmaxwell.diaries.responder.model.Fragment;
import com.rsmaxwell.diaries.responder.model.Role;
import com.rsmaxwell.diaries.responder.repository.FragmentRepository;
import com.rsmaxwell.diaries.responder.utilities.Authorization;
import com.rsmaxwell.diaries.responder.utilities.DiaryContext;
import com.rsmaxwell.diaries.responder.utilities.FragmentSequenceNormaliser;
import com.rsmaxwell.mqtt.rpc.common.Response;
import com.rsmaxwell.mqtt.rpc.common.Utilities;
import com.rsmaxwell.mqtt.rpc.exceptions.RpcStatusException;
import com.rsmaxwell.mqtt.rpc.responder.RequestHandler;

import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

public class NormaliseFragments extends RequestHandler {

	private static final Logger log = LoggerFactory.getLogger(NormaliseFragments.class);
	static private ObjectMapper mapper = new ObjectMapper();

	@Override
	public Response handleRequest(Object ctx, Map<String, Object> args, List<UserProperty> userProperties) throws Exception {

		log.info("NormaliseFragments.handleRequest");

		String accessToken = Authorization.getAccessToken(userProperties);
		DiaryContext context = (DiaryContext) ctx;
		Claims claims = Authorization.checkToken(context, "access", accessToken);
		Authorization.checkActive(claims);
		Authorization.checkRoleAtLeast(claims, Role.EDITOR);
		log.info("NormaliseFragments.handleRequest: Authorization.check: OK!");

		FragmentRepository fragmentRepository = context.getFragmentRepository();

		log.info("NormaliseFragments.handleRequest: get the date arguments");

		Integer year;
		Integer month;
		Integer day;
		try {
			year = Utilities.getInteger(args, "year");
			month = Utilities.getInteger(args, "month");
			day = Utilities.getInteger(args, "day");

		} catch (Exception e) {
			log.info("NormaliseFragments.handleRequest: args: " + mapper.writeValueAsString(args));
			throw RpcStatusException.badRequest(e.getMessage());
		}

		EntityManager em = context.getEntityManager();
		EntityTransaction tx = em.getTransaction();

		List<Fragment> updates;

		tx.begin();
		try {
			updates = FragmentSequenceNormaliser.normaliseDate(fragmentRepository, year, month, day);
			tx.commit();

		} catch (Exception ex) {
			if (tx.isActive()) {
				tx.rollback();
			}
			throw ex;
		}

		FragmentSequenceNormaliser.publish(context, updates);

		return Response.success(updates.size());
	}
}
