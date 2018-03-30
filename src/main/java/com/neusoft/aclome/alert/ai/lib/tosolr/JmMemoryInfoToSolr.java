package com.neusoft.aclome.alert.ai.lib.tosolr;

import java.io.IOException;

import com.google.gson.JsonObject;
import com.neusoft.aclome.alert.ai.lib.data.JmSystemData;
import com.neusoft.aclome.alert.ai.lib.data.SolrContextData;
import com.neusoft.aclome.alert.ai.lib.sample.JmMemory;
import com.neusoft.aclome.alert.ai.lib.sample.JmThreadsMemory;
import com.neusoft.aclome.westworld.tsp.lib.solr.SolrWriter;
import com.neusoft.aclome.westworld.tsp.lib.util.TimeUtil;

public class JmMemoryInfoToSolr implements Runnable{
	
	private final String jm_threadsmemory_url;
	private final String jm_system_url;
	private final String jm_memory_url;
	private final long gc_time;
	private final Double value;
	private final Long timestamp;
	private final boolean flag;
	
	public JmMemoryInfoToSolr(SolrContextData iterator,
			String jm_threadsmemory_url,
			String jm_system_url,
			String jm_memory_url,
			long gc_time,
			Double value,
			Long timestamp,
			boolean flag) {
		this.jm_threadsmemory_url = jm_threadsmemory_url;
		this.jm_system_url = jm_system_url;
		this.jm_memory_url = jm_memory_url;
		this.gc_time = gc_time;
		this.value = value;
		this.timestamp = timestamp;
		this.flag = flag;
	}
		
	@Override
	public void run() {
		SolrWriter sw = new SolrWriter(null);
		JsonObject doc = new JsonObject();
		doc.addProperty("result", "jm_memory_ad");
		doc.addProperty("rs_timestamp", TimeUtil.formatUnixtime2(timestamp));
		doc.addProperty("value", value);
		doc.addProperty("gc_time", gc_time);
		if (flag) {
			try {
				JmMemory jm = new JmMemory(jm_memory_url);
				JmThreadsMemory jtm = new JmThreadsMemory(jm_threadsmemory_url);
				JmSystemData jsd = new JmSystemData(jm_system_url);
				doc.addProperty("memory_info", jm.getInfo());
				doc.addProperty("threadsmemory_info", jtm.getInfo());
				doc.addProperty("system_info", jsd.getProperty("hostname"));
				doc.addProperty("type", "anomaly");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			doc.addProperty("type", "nomaly");
		}
		try {
			sw.write(doc);
			sw.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
