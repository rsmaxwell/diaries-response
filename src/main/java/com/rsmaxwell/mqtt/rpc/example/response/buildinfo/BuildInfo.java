package com.rsmaxwell.mqtt.rpc.example.response.buildinfo;

import com.rsmaxwell.mqtt.rpc.common.buildinfo.AbstractBuildInfo;

public class BuildInfo extends AbstractBuildInfo {

	public BuildInfo() {
		name = "mqtt-rpc-example-response";
		version = "$VERSION";
		buildID = "$BUILD_ID";
		builddate = "$TIMESTAMP";
		gitCommit = "$GIT_COMMIT";
		gitBranch = "$GIT_BRANCH";
		gitURL = "$GIT_URL";
	}
}
