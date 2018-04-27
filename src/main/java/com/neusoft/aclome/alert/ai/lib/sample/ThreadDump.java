package com.neusoft.aclome.alert.ai.lib.sample;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.neusoft.aclome.westworld.tsp.lib.util.ThreadDumpInfo;
import com.neusoft.aclome.westworld.tsp.lib.util.data.ThreadDumpData;
import com.neusoft.aclome.westworld.tsp.lib.util.data.ThreadDumpData.DataType;

public class ThreadDump {
	
	private List<ThreadDumpData> tdds = new ArrayList<ThreadDumpData>();
	
	public ThreadDump(String url, Long millis) throws IOException {
		Long startTimeMillis = System.currentTimeMillis();
		while(startTimeMillis + millis > System.currentTimeMillis()) {
			this.tdds.add(new ThreadDumpData(url, DataType.url));
		}
	}
	
	public String getStatesLists() {
		Map<String, List<String>> statsLists = new HashMap<String, List<String>>();
		int index = 0;
		for (ThreadDumpData tdd : tdds) {
			while (tdd.hasNextThreadInfo()) {
				ThreadDumpInfo tdi = tdd.next();
				String tname = tdi.getName() + ", " + tdi.getId();
				List<String> states = statsLists.getOrDefault(tname, new ArrayList<String>());
				while(states.size() < index) {
					states.add("ABSENCE");
				}
				states.add(tdi.getState());
				statsLists.put(tname, states);
			}
			index += 1;
		}
		for (Map.Entry<String, List<String>> entry : statsLists.entrySet()) {
			while(entry.getValue().size() < index) {
				entry.getValue().add("ABSENCE");
			}
		}
		return new Gson().toJson(statsLists);
	}
	
}
