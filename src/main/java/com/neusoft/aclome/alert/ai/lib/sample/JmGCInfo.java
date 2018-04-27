package com.neusoft.aclome.alert.ai.lib.sample;

import com.neusoft.aclome.westworld.tsp.lib.util.data.JmGCInfoData;

public class JmGCInfo {
	
	private final JmGCInfoData jgcid;
	private long time = 0;
	
	public JmGCInfo(String url) {
		jgcid = new JmGCInfoData(url);
		jgcid.start();
	}
	
	public long getExeTime() {
		long res = jgcid.getExeTime()-time;
		time = jgcid.getExeTime();
		return res;
	}
	
	public void stop() {
		jgcid.stop(true);
	}
}
