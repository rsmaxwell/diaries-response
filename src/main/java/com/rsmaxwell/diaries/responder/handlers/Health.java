package com.rsmaxwell.diaries.responder.handlers;

import java.util.List;
import java.util.Map;

import org.eclipse.paho.mqttv5.common.packet.UserProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rsmaxwell.diaries.responder.health.DatabaseHealthProbe;
import com.rsmaxwell.diaries.responder.health.JpaDatabaseHealthProbe;
import com.rsmaxwell.diaries.responder.utilities.DiaryContext;
import com.rsmaxwell.mqtt.rpc.common.Response;
import com.rsmaxwell.mqtt.rpc.common.Status;
import com.rsmaxwell.mqtt.rpc.responder.RequestHandler;

/** Performs a live database readiness probe without requiring a Diaries access token. */
public class Health extends RequestHandler {

	private static final Logger log = LoggerFactory.getLogger(Health.class);
	private static final Map<String, String> UP_PAYLOAD = Map.of("status", "UP");

	private final DatabaseHealthProbe healthProbe;

	public Health() {
		this(null);
	}

	public Health(DatabaseHealthProbe healthProbe) {
		this.healthProbe = healthProbe;
	}

	@Override
	public Response handleRequest(Object ctx, Map<String, Object> args, List<UserProperty> userProperties) {
		try {
			DatabaseHealthProbe probe = healthProbe;
			if (probe == null) {
				DiaryContext context = (DiaryContext) ctx;
				probe = new JpaDatabaseHealthProbe(context.getEntityManagerFactory());
			}

			probe.check();
			return Response.success(UP_PAYLOAD);
		} catch (Exception exception) {
			log.debug("Database health probe failed: {}", exception.toString());
			return Response.error(Status.INTERNAL_ERROR, "database health probe failed");
		}
	}
}
