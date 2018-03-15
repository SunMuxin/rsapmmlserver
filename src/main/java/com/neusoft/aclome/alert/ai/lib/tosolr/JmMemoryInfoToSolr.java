package com.neusoft.aclome.alert.ai.lib.tosolr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.neusoft.aclome.alert.ai.lib.context.SolrContext;
import com.neusoft.aclome.westworld.tsp.lib.model.analysis.JmMemory;
import com.neusoft.aclome.westworld.tsp.lib.model.analysis.JmSystem;
import com.neusoft.aclome.westworld.tsp.lib.model.analysis.JmThreadsMemory;
import com.neusoft.aclome.westworld.tsp.lib.solr.SolrWriter;
import com.neusoft.aclome.westworld.tsp.lib.util.Entry;
import com.neusoft.aclome.westworld.tsp.lib.util.TimeUtil;

public class JmMemoryInfoToSolr implements Runnable{
	
	private final String jm_threadsmemory_url;
	private final String jm_system_url;
	private final String jm_memory_url;
	private final long gc_exe_time;
	private final Double value;
	private final Long timestamp;
	private final boolean flag;
	
	public JmMemoryInfoToSolr(SolrContext iterator,
			String jm_threadsmemory_url,
			String jm_system_url,
			String jm_memory_url,
			long gc_exe_time,
			Double value,
			Long timestamp,
			boolean flag) {
		this.jm_threadsmemory_url = jm_threadsmemory_url;
		this.jm_system_url = jm_system_url;
		this.jm_memory_url = jm_memory_url;
		this.gc_exe_time = gc_exe_time;
		this.value = value;
		this.timestamp = timestamp;
		this.flag = flag;
	}
		
	@Override
	public void run() {
		List<Entry<String, ?>> entrys = new ArrayList<Entry<String,?>>();
		SolrWriter sw = new SolrWriter(null);
		entrys.add(new Entry<String, String>("result", "jm_memory_ad"));
		entrys.add(new Entry<String, String>("rs_timestamp", TimeUtil.formatUnixtime2(timestamp)));
		entrys.add(new Entry<String, Double>("value", value));
		entrys.add(new Entry<String, Long>("gc_exe_time", gc_exe_time));
		if (flag) {
			try {
				JmMemory jm = new JmMemory(jm_memory_url);
				JmThreadsMemory jtm = new JmThreadsMemory(jm_threadsmemory_url);
				JmSystem js = new JmSystem(jm_system_url);
				entrys.add(new Entry<String, String>("memory_info", jm.getInfo()));
				entrys.add(new Entry<String, String>("threadsmemory_info", jtm.getInfo()));
				entrys.add(new Entry<String, String>("system_info", js.getInfo()));
				entrys.add(new Entry<String, String>("type", "anomaly"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			entrys.add(new Entry<String, String>("type", "nomaly"));
		}
		try {
			sw.write(entrys);
			sw.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
