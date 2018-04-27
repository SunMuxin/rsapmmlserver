package com.neusoft.aclome.alert.ai.lib.sample;

import java.io.IOException;

import com.neusoft.aclome.westworld.tsp.lib.util.data.DataType;
import com.neusoft.aclome.westworld.tsp.lib.util.data.JmMemoryData;

public class JmMemory {
	
	private final String info;
	
	public JmMemory(String url) throws IOException {
		JmMemoryData jmd = new JmMemoryData(url, DataType.URL);
		info = jmd.getInfo();
	}
	
	public String getInfo() {
		return info;
	}
	
}
