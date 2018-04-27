package com.neusoft.aclome.alert.ai.lib.sample;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.neusoft.aclome.westworld.tsp.lib.util.JmThreadMemoryInfo;
import com.neusoft.aclome.westworld.tsp.lib.util.data.DataType;
import com.neusoft.aclome.westworld.tsp.lib.util.data.JmThreadsMemoryData;

public class JmThreadsMemory {
	
	private static final long interval = 1000l;
	private static final int top = 100;
	
	private final List<JmThreadMemoryInfo> infos = new ArrayList<JmThreadMemoryInfo>();
		
	public JmThreadsMemory(String url) throws IOException, InterruptedException {
		long timestamp = System.currentTimeMillis();
		JmThreadsMemoryData jtmd = new JmThreadsMemoryData(url, DataType.URL);
		Map<String, Long> tmm = new HashMap<String, Long>();
		while(jtmd.hasNextThreadMemoryInfo()) {
			JmThreadMemoryInfo jtmi = jtmd.next();
			tmm.put(jtmi.getThreadname(), jtmi.getMemory());
		}
		Thread.sleep(interval - System.currentTimeMillis() + timestamp);
		jtmd = new JmThreadsMemoryData(url, DataType.URL);
		while(jtmd.hasNextThreadMemoryInfo()) {
			JmThreadMemoryInfo jtmi = jtmd.next();
			infos.add(new JmThreadMemoryInfo(
					jtmi.getThreadname(), 
					jtmi.getMemory()-tmm.getOrDefault(jtmi.getThreadname(), 0L))
					);
		}
		Collections.sort(infos);
		Collections.reverse(infos);
	}
	
	public String getInfo() {
		if (infos.size() > top) {
			return new Gson().toJson(infos.subList(0, top));
		}
		return new Gson().toJson(infos);
	}
}
