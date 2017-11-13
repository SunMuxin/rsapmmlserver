package com.realsight.westworld.server.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import com.realsight.westworld.server.model.JmThreadsMemoryModel;
import com.realsight.westworld.tsp.lib.solr.SolrReader;

import ch.qos.logback.classic.Logger;

public class JmThreadsMemoryApplication extends Thread{
	private String OPTION_SOLR_URL = null;
	private Map<String, JmThreadsMemoryModel> JTMM = null;
	private boolean stopflag = false;
	private static Logger logger = (Logger) LoggerFactory.getLogger(JmThreadsMemoryApplication.class);
	private static Thread thread = null;
	
	public JmThreadsMemoryApplication(String OPTION_SOLR_URL) {
		this.OPTION_SOLR_URL = OPTION_SOLR_URL;
		this.JTMM = new HashMap<String, JmThreadsMemoryModel>();
	}
	
	public void updateModels() {
		List<String> filters = new ArrayList<String>();
		filters.add("option_s:jm_threadsmemory");
		SolrReader sr = new SolrReader(this.OPTION_SOLR_URL, filters);
		sr.setSort("timestamp_l", true);
		Map<String, JmThreadsMemoryModel> TJTMM = new HashMap<String, JmThreadsMemoryModel>();
		while(sr.hasNextResponse()) {
			try {
				JSONObject modelJSON = new JSONObject(sr.nextResponse());
				String jm_name = modelJSON.optString("jm_name_s");
				String id = modelJSON.optString("id");
				String show_name = modelJSON.optString("show_name_s");
				if (JTMM.containsKey(id)) {
					JTMM.get(id).status(false);
					TJTMM.put(id, JTMM.get(id));
					continue;
				}
				String solr_writer_url = modelJSON.optString("solr_writer_url_s");
				String jm_threadsmemory_url = modelJSON.optString("jm_threadsmemory_url_s");
				long interval = modelJSON.optLong("interval_l");
				JmThreadsMemoryModel jtadm = new JmThreadsMemoryModel(jm_threadsmemory_url,
						jm_name,
						show_name,
						solr_writer_url,
						interval);
				jtadm.status(false);
				TJTMM.put(id, jtadm);
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
		for (Map.Entry<String, JmThreadsMemoryModel> entry : TJTMM.entrySet()) {
			if (TJTMM.containsKey(entry.getKey())) {
				continue;
			}
			entry.getValue().status(true);
		}
		this.JTMM = TJTMM;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(!stopflag) {
			updateModels();
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
