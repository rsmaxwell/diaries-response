package com.rsmaxwell.diaries.responder.health;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

public class JpaDatabaseHealthProbe implements DatabaseHealthProbe {

	private final EntityManagerFactory entityManagerFactory;

	public JpaDatabaseHealthProbe(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	@Override
	public void check() {
		if (entityManagerFactory == null || !entityManagerFactory.isOpen()) {
			throw new IllegalStateException("EntityManagerFactory is unavailable");
		}

		EntityManager entityManager = null;

		try {
			entityManager = entityManagerFactory.createEntityManager();
			if (entityManager == null) {
				throw new IllegalStateException("Unable to create EntityManager");
			}

			Object result = entityManager.createNativeQuery("SELECT 1", Integer.class).getSingleResult();
			if (!(result instanceof Number number) || number.intValue() != 1) {
				throw new IllegalStateException("Unexpected database health probe result");
			}
		} finally {
			if (entityManager != null) {
				entityManager.close();
			}
		}
	}
}
