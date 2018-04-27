package com.neusoft.aclome.alert.ai.lib.sample;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.neusoft.aclome.alert.ai.lib.data.JmThreadData;
import com.neusoft.aclome.alert.ai.lib.util.CONSTANT;
import com.neusoft.aclome.westworld.tsp.lib.util.Entry;
import com.neusoft.aclome.westworld.tsp.lib.util.JmThreadInfo;
import com.neusoft.aclome.westworld.tsp.lib.util.data.DataType;

public class JmThreadSample {
	
	private JmThreadData start_jtd = null;
	private JmThreadData end_jtd = null;
	private List<JmThreadData> jtds = new ArrayList<JmThreadData>();
	private JsonArray durations = null;
	private List<Entry<Long, JmThreadInfo>> temp = null;
	private int CPUContextSwitchCounter = -1;
	
	public JmThreadData getJDT(int index) {
		if (index<0 || index>=this.jtds.size()) return null;
		return this.jtds.get(index);
	}
	
	public JmThreadSample(String url, Long millis) throws IOException {
		Long startTimeMillis = System.currentTimeMillis();
		int sz = 0;
		do{
			sz += 1;
			this.jtds.add(new JmThreadData(url, DataType.URL));
			try {
				Thread.sleep(CONSTANT.ten_second-(System.currentTimeMillis()-startTimeMillis)%CONSTANT.ten_second);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} while(startTimeMillis + millis > System.currentTimeMillis());
		if (sz <= 0)
			throw new IOException("not exit data.");
		this.start_jtd = this.jtds.get(0);
		this.end_jtd = this.jtds.get(sz-1);
	}
	
	public String getSummary(int longest_thread_lock_chain, double cpu_used) {
		
		StringBuffer content = new StringBuffer();
		
		getThreadDuration();
		
		long total = 0;
		long http = 0;
		long jdbc = 0;
		long gc = 0;

		for (Entry<Long, JmThreadInfo> entry: temp) {
			total += entry.getFirst();
			String lockinfo = entry.getSecond().getLockInfo().toLowerCase();
			if (lockinfo.contains(CONSTANT.HTTP_THREAD_INCLUDE_STRING)) {
				http += entry.getFirst();
			}
			if (lockinfo.contains(CONSTANT.DB_THREAD_INCLUDE_STRING)) {
				jdbc += entry.getFirst();
			}
			if (lockinfo.contains(CONSTANT.GC_THREAD_INCLUDE_STRING)) {
				gc += entry.getFirst();
			}			
		}
		
		int row_num = 1;
		
		content.append(String.valueOf(row_num));
		content.append(CONSTANT.period);
		content.append(CONSTANT.spacing);
		row_num += 1;
		content.append(CONSTANT.STRING_PROCESS_CPU_CONSUMPTION_TREND);
		content.append(CONSTANT.newline);
		
		content.append(String.valueOf(row_num));
		content.append(CONSTANT.period);
		content.append(CONSTANT.spacing);
		row_num += 1;
		content.append(CONSTANT.STRING_CPU_CONSUMPTION_SCATTER);
		content.append(CONSTANT.comma);
		content.append(CONSTANT.STRING_THREAD_CPU_CONSUMPTION);
		content.append(CONSTANT.period);
		content.append(CONSTANT.newline);
	
		content.append(String.valueOf(row_num));
		content.append(CONSTANT.period);
		content.append(CONSTANT.spacing);
		row_num += 1;
		content.append(CONSTANT.STRING_HTTP_THREAD_CPU_CONSUMPTION);
		content.append(String.format(" %.2f%%", 100.0*http/total*cpu_used));
		content.append(CONSTANT.comma);
		content.append(CONSTANT.STRING_HTTP_THREAD_CHECK);
		content.append(CONSTANT.newline);
		
		content.append(String.valueOf(row_num));
		content.append(CONSTANT.period);
		content.append(CONSTANT.spacing);
		row_num += 1;
		content.append(CONSTANT.STRING_JDBC_THREAD_CPU_CONSUMPTION);
		content.append(String.format(" %.2f%%", 100.0*jdbc/total*cpu_used));
		content.append(CONSTANT.comma);
		content.append(CONSTANT.STRING_JDBC_THREAD_CHECK);
		content.append(CONSTANT.newline);
		
		content.append(String.valueOf(row_num));
		content.append(CONSTANT.period);
		content.append(CONSTANT.spacing);
		row_num += 1;
		content.append(CONSTANT.STRING_GC_THREAD_CPU_CONSUMPTION);
		content.append(String.format(" %.2f%%", 100.0*gc/total*cpu_used));
		content.append(CONSTANT.comma);
		content.append(CONSTANT.STRING_GC_THREAD_CHECK);
		content.append(CONSTANT.newline);
	
		content.append(String.valueOf(row_num));
		content.append(CONSTANT.period);
		content.append(CONSTANT.spacing);
		row_num += 1;
		content.append(CONSTANT.STRING_LONGEST_THREAD_LOCK_CHAIN);
		content.append(String.valueOf(longest_thread_lock_chain));
		content.append(CONSTANT.comma);
		content.append(CONSTANT.STRING_CPU_CONTEXT_SWITCH_COUNTER);
		content.append(String.valueOf(getCPUContextSwitch()));
		content.append(CONSTANT.comma);
		content.append(CONSTANT.STRING_THREAD_LOCK_CHECK);
		content.append(CONSTANT.newline);
	
		return content.toString();
	}
	
	public String getSuggestion(int longest_thread_lock_chain, double cpu_usage) {
		
		StringBuffer content = new StringBuffer();
		
		getThreadDuration();
		int index = 0;
		
		long total = 0;
		long http = 0;
		long jdbc = 0;
		long top = 0;
		long gc = 0;

		for (Entry<Long, JmThreadInfo> entry: temp) {
			
			total += entry.getFirst();
			String lockinfo = entry.getSecond().getLockInfo().toLowerCase();
			if (lockinfo.contains(CONSTANT.HTTP_THREAD_INCLUDE_STRING)) {
				http += entry.getFirst();
			}
			if (lockinfo.contains(CONSTANT.DB_THREAD_INCLUDE_STRING)) {
				jdbc += entry.getFirst();
			}
			if (index < CONSTANT.top_thread_count) {
				top += entry.getFirst();
			}
			if (lockinfo.contains(CONSTANT.GC_THREAD_INCLUDE_STRING)) {
				gc += entry.getFirst();
			}
			
			index += 1;
		}
		
		int row_num = 1;
	
		if (top>total*CONSTANT.top_thread_threshold && cpu_usage>CONSTANT.lower_cpu_usage_threshold) {			
			content.append(String.valueOf(row_num));
			content.append(CONSTANT.period);
			content.append(CONSTANT.spacing);
			row_num += 1;
			content.append(CONSTANT.STRING_OPTIMIZE_TOP_THREAD);
			content.append(CONSTANT.newline);
		}
		
		if (http>total*CONSTANT.http_thread_threshold && cpu_usage>CONSTANT.lower_cpu_usage_threshold) {
			content.append(String.valueOf(row_num));
			content.append(CONSTANT.period);
			content.append(CONSTANT.spacing);
			row_num += 1;
			content.append(CONSTANT.STRING_OPTIMIZE_HTTP_THREAD);
			content.append(CONSTANT.newline);
		}
		
		if (jdbc>total*CONSTANT.jdbc_thread_threshold && cpu_usage>CONSTANT.lower_cpu_usage_threshold) {
			content.append(String.valueOf(row_num));
			content.append(CONSTANT.period);
			content.append(CONSTANT.spacing);
			row_num += 1;
			content.append(CONSTANT.STRING_OPTIMIZE_JDBC_THREAD);
			content.append(CONSTANT.newline);
		}
		
		if (gc>total*CONSTANT.gc_thread_threshold && cpu_usage>CONSTANT.lower_cpu_usage_threshold) {
			content.append(String.valueOf(row_num));
			content.append(CONSTANT.period);
			content.append(CONSTANT.spacing);
			row_num += 1;
			content.append(CONSTANT.STRING_OPTIMIZE_GC_THREAD);
			content.append(CONSTANT.newline);
		}
		
		if (getCPUContextSwitch()>CONSTANT.cpu_context_switch_threshold || longest_thread_lock_chain>CONSTANT.thread_lock_chain_threshold) {
			content.append(String.valueOf(row_num));
			content.append(CONSTANT.period);
			content.append(CONSTANT.spacing);
			row_num += 1;
			content.append(CONSTANT.STRING_OPTIMIZE_THREAD_LOCK);
			content.append(CONSTANT.newline);
		}
		
		if (cpu_usage < CONSTANT.lower_cpu_usage_threshold) {
			content.append(String.valueOf(row_num));
			content.append(CONSTANT.period);
			content.append(CONSTANT.spacing);
			row_num += 1;
			content.append(CONSTANT.STRING_OPTIMIZE_LOWER_CPU_USAGE);
			content.append(CONSTANT.newline);
		}
		if (row_num == 1) {
			content.append(CONSTANT.STRING_WU);
		}
		return content.toString();
	}
	
	public JsonArray getThreadStackTraces() {
		Map<String, String> flag = new HashMap<String, String>();
		JsonObject temp_stack_traces = new JsonObject();
		JsonArray stack_traces = new JsonArray();
		for (JmThreadData jtd : jtds) {
			jtd.reset();
			while (jtd.hasNextThreadInfo()) {
				JmThreadInfo jti = jtd.next();
				if (flag.containsKey(jti.getName())) {
					if (flag.get(jti.getName()).equals(CONSTANT.STRING_RUNNABLE)) continue;
					if (!jti.getState().equals(CONSTANT.STRING_RUNNABLE)) continue;
					JsonObject stack_trace = new JsonObject();
					stack_trace.addProperty(CONSTANT.STRING_NAME, jti.getName());
					stack_trace.addProperty(CONSTANT.STRING_STACK_TRACE, jti.getLockInfo());
					temp_stack_traces.add(jti.getName(), stack_trace);
					flag.put(jti.getName(), jti.getState());
				} else {
					JsonObject stack_trace = new JsonObject();
					stack_trace.addProperty(CONSTANT.STRING_NAME, jti.getName());
					stack_trace.addProperty(CONSTANT.STRING_STACK_TRACE, jti.getLockInfo());
					temp_stack_traces.add(jti.getName(), stack_trace);
					flag.put(jti.getName(), jti.getState());
				}
			}
		}
		for (Map.Entry<String, JsonElement> entry : temp_stack_traces.entrySet()) {
			stack_traces.add(entry.getValue().toString());
		}
		return stack_traces;
	}
	
	public JsonArray getThreadDuration() {
		if (durations != null) return durations;
		
		durations = new JsonArray();
		Map<Long, JmThreadInfo> jtis = new HashMap<Long, JmThreadInfo>();
		Map<Long, JmThreadInfo> id2jti = new HashMap<Long, JmThreadInfo>();

		for (JmThreadData jtd : jtds) {
			jtd.reset();
			while (jtd.hasNextThreadInfo()) {
				JmThreadInfo jti = jtd.next();
				if (id2jti.containsKey(jti.getId())) continue;
				if (!jti.getState().equals(CONSTANT.STRING_RUNNABLE)) continue;
				id2jti.put(jti.getId(), jti);
			}
		}
		start_jtd.reset();
		end_jtd.reset();
		while(start_jtd.hasNextThreadInfo()) {
			JmThreadInfo jti = start_jtd.next();
			jtis.put(jti.getId(), jti);
		}
		temp = new ArrayList<Entry<Long, JmThreadInfo>>();
		while(end_jtd.hasNextThreadInfo()) {
			JmThreadInfo jti = end_jtd.next();
			long time = 0L;
			if (jtis.containsKey(jti.getId())) {
				time = jti.getCpuTimeMillis() + jti.getUserTimeMillis();
				time -= jtis.get(jti.getId()).getCpuTimeMillis();
				time -= jtis.get(jti.getId()).getUserTimeMillis();
			} else {
				time = jti.getCpuTimeMillis() + jti.getUserTimeMillis();
			}
			temp.add(new Entry<Long, JmThreadInfo>(time, jti));
		}
		Collections.sort(temp);
		for (Entry<Long, JmThreadInfo> entry: temp) {
			JsonObject duration = new JsonObject();
			duration.addProperty(CONSTANT.STRING_NAME, entry.getSecond().getName());
			duration.addProperty(CONSTANT.STRING_TIME, entry.getFirst());
			durations.add(duration.toString());
		}
		return durations;
	}
	
	public List<String> getStates() {
		List<String> stateList = new ArrayList<String>();
		JsonObject states = new JsonObject();
		int index = 0;
		for (JmThreadData jtd : jtds) {
			jtd.reset();
			while (jtd.hasNextThreadInfo()) {
				JmThreadInfo jti = jtd.next();
				if(!states.has(String.valueOf(jti.getName()))){
					states.add(String.valueOf(jti.getName()), new JsonArray());
				}
				while(states.get(String.valueOf(jti.getName())).getAsJsonArray().size() < index) {
					states.get(String.valueOf(jti.getName())).getAsJsonArray().add(CONSTANT.STRING_ABSENCE);
				}
				states.get(String.valueOf(jti.getName())).getAsJsonArray().add(jti.getState());
			}
			index += 1;
		}
		for (Map.Entry<String, JsonElement> entry : states.entrySet()) {
			while (entry.getValue().getAsJsonArray().size() < index) {
				entry.getValue().getAsJsonArray().add(CONSTANT.STRING_ABSENCE);
			}
			JsonObject json = new JsonObject();
			json.addProperty(CONSTANT.STRING_NAME, entry.getKey());
			json.add(CONSTANT.STRING_STATE, entry.getValue());
			stateList.add(json.toString());
		}
		return stateList;
	}
	
	public int getCPUContextSwitch() {
		JsonObject states = new JsonObject();
		if (CPUContextSwitchCounter != -1)
			return CPUContextSwitchCounter;
		int ret = 0;
		for (JmThreadData jtd : jtds) {
			jtd.reset();
			while (jtd.hasNextThreadInfo()) {
				JmThreadInfo jti = jtd.next();
				if (states.has(jti.getName())) {
					if (!states.equals(jti.getState())) ret += 1;
				} else {
					ret += 1;
				}
				states.addProperty(jti.getName(), jti.getState());
			}
		}
		return CPUContextSwitchCounter = ret;
	}
}
