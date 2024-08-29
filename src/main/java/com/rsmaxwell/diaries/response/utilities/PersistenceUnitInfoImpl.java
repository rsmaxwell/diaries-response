package com.rsmaxwell.diaries.response.utilities;

import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;

public class PersistenceUnitInfoImpl extends ParsedPersistenceXmlDescriptor {

	public PersistenceUnitInfoImpl() {
		super(null);

		setName("com.rsmaxwell.diaries");
		setExcludeUnlistedClasses(false);
		setProviderClassName("org.hibernate.jpa.HibernatePersistenceProvider");
	}

}
