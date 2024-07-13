package com.rsmaxwell.mqtt.rpc.example.response;

import java.util.ArrayList;
import java.util.List;

import com.rsmaxwell.mqtt.rpc.common.buildinfo.IBuildInfo;

public class BuildInfoTest {

	public static void main(String[] args) throws Exception {

		List<IBuildInfo> infos = new ArrayList<IBuildInfo>();
		infos.add(new com.rsmaxwell.mqtt.rpc.common.buildinfo.BuildInfo());
		infos.add(new com.rsmaxwell.mqtt.rpc.response.buildinfo.BuildInfo());
		infos.add(new com.rsmaxwell.mqtt.rpc.example.response.buildinfo.BuildInfo());

		for (IBuildInfo info : infos) {
			info.printAll();
		}
	}

}
