package com.neusoft.aclome.alert.ai.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import com.neusoft.aclome.alert.ai.model.JmMemoryADModel;
import com.neusoft.aclome.westworld.tsp.lib.solr.SolrReader;

import ch.qos.logback.classic.Logger;

public class JmMemoryADApplication extends Thread{
	private String OPTION_SOLR_URL = null;
	private Map<String, JmMemoryADModel> JMADMS = null;
	private boolean stopflag = false;
	private String time_field = null;
	private static Logger logger = (Logger) LoggerFactory.getLogger(JmMemoryADApplication.class);
	private static Thread thread = null;
	
	public JmMemoryADApplication(String OPTION_SOLR_URL, String time_field) {
		this.OPTION_SOLR_URL = OPTION_SOLR_URL;
		this.JMADMS = new HashMap<String, JmMemoryADModel>();
		this.time_field = time_field;
	}
	
	public void updateModels() throws Exception {
		List<String> filters = new ArrayList<String>();
		filters.add("option_s:jm_memory_ad");
		SolrReader sr = new SolrReader(this.OPTION_SOLR_URL, filters);
		sr.setSort("timestamp_l", true);
		Map<String, JmMemoryADModel> TJMADMS = new HashMap<String, JmMemoryADModel>();
		while(sr.hasNextResponse()) {
			try {
				JSONObject modelJSON = new JSONObject(sr.nextResponse());
				String jm_name = modelJSON.optString("jm_name_s");
				String id = modelJSON.optString("id");
				String show_name = modelJSON.optString("show_name_s");
				if (JMADMS.containsKey(id)) {
					JMADMS.get(id).status(false);
					TJMADMS.put(id, JMADMS.get(id));
					continue;
				}
				String solr_reader_url = modelJSON.optString("solr_reader_url_s");
				String solr_writer_url = modelJSON.optString("solr_writer_url_s");
				
				String jm_threadsmemory_url = modelJSON.optString("jm_threadsmemory_url_s");
				String jm_system_url = modelJSON.optString("jm_system_url_s");
				String jm_memory_url = modelJSON.optString("jm_memory_url_s");
				String jm_gcinfo_url = modelJSON.optString("jm_gcinfo_url_s");
				String fq = modelJSON.optString("fq_s");
				Long start_timestamp = modelJSON.optLong("start_timestamp_l");
				Long interval = modelJSON.optLong("interval_l");
				String stats_field = modelJSON.optString("stats_field_s");
				String stats_type = modelJSON.optString("stats_type_s");
				Double max_value = modelJSON.optDouble("max_f");
				Double min_value = modelJSON.optDouble("min_f");
				JmMemoryADModel jtadm = new JmMemoryADModel(jm_threadsmemory_url,
						jm_system_url,
						jm_memory_url,
						jm_gcinfo_url,
						OPTION_SOLR_URL,
						0L,
						jm_name,
						solr_reader_url,
						solr_writer_url,
						fq,
						start_timestamp,
						interval,
						stats_field,
						stats_type,
						id,
						show_name,
						time_field,
						max_value,
						min_value);
				jtadm.status(false);
				TJMADMS.put(id, jtadm);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				logger.error(e.getMessage());
			}
		}
		try {
			sr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		for (Map.Entry<String, JmMemoryADModel> entry : JMADMS.entrySet()) {
			if (TJMADMS.containsKey(entry.getKey())) {
				continue;
			}
			entry.getValue().status(true);
		}
		this.JMADMS = TJMADMS;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(!stopflag) {
			try {
				updateModels();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				Thread.sleep(1000L * 60);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				logger.error(e.getMessage());
			}
		}
	}
	
	public synchronized void status(boolean stopflag) {
		this.stopflag = stopflag;
		if (stopflag) {
			return ;
		} else {
			if (thread==null || !thread.isAlive()){
				thread = new Thread(this);
				thread.start();
			}
		}
	}
	
}
