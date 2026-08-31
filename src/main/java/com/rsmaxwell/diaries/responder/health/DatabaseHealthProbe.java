package com.rsmaxwell.diaries.responder.health;

@FunctionalInterface
public interface DatabaseHealthProbe {

	void check() throws Exception;
}
