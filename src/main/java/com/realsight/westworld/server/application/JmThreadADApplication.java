package com.realsight.westworld.server.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import com.realsight.westworld.server.model.JmThreadADModel;
import com.realsight.westworld.tsp.lib.solr.SolrReader;

import ch.qos.logback.classic.Logger;

public class JmThreadADApplication extends Thread{
	private String OPTION_SOLR_URL = null;
	private Map<String, JmThreadADModel> JTADMS = null;
	private boolean stopflag = false;
	private String time_field = null;
	private static Logger logger = (Logger) LoggerFactory.getLogger(JmThreadADApplication.class);
	private static Thread thread = null;
	
	public JmThreadADApplication(String OPTION_SOLR_URL, String time_field) {
		this.OPTION_SOLR_URL = OPTION_SOLR_URL;
		this.JTADMS = new HashMap<String, JmThreadADModel>();
		this.time_field = time_field;
	}
	
	public void updateModels() {
		List<String> filters = new ArrayList<String>();
		filters.add("option_s:jm_thread_ad");
		SolrReader sr = new SolrReader(this.OPTION_SOLR_URL, filters);
		sr.setSort("timestamp_l", true);
		Map<String, JmThreadADModel> TJTADMS = new HashMap<String, JmThreadADModel>();
		while(sr.hasNextResponse()) {
			try {
				JSONObject modelJSON = new JSONObject(sr.nextResponse());
				String jm_name = modelJSON.optString("jm_name_s");
				String ad_id = modelJSON.optString("id");
				String show_name = modelJSON.optString("show_name_s");
				if (JTADMS.containsKey(ad_id)) {
					JTADMS.get(ad_id).status(false);
					TJTADMS.put(ad_id, JTADMS.get(ad_id));
					continue;
				}
				String solr_reader_url = modelJSON.optString("solr_reader_url_s");
				String solr_writer_url = modelJSON.optString("solr_writer_url_s");
				String jm_thread_url = modelJSON.optString("jm_thread_url_s");
				String fq = modelJSON.optString("fq_s");
				Long start_timestamp = modelJSON.optLong("start_timestamp_l");
				Long interval = modelJSON.optLong("interval_l");
				String stats_field = modelJSON.optString("stats_field_s");
				String stats_type = modelJSON.optString("stats_type_s");
				Double max_value = modelJSON.optDouble("max_f");
				Double min_value = modelJSON.optDouble("min_f");
				JmThreadADModel jtadm = new JmThreadADModel(jm_thread_url,
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
						ad_id,
						show_name,
						time_field,
						max_value,
						min_value);
				jtadm.status(false);
				TJTADMS.put(ad_id, jtadm);
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
		for (Map.Entry<String, JmThreadADModel> entry : JTADMS.entrySet()) {
			if (TJTADMS.containsKey(entry.getKey())) {
				continue;
			}
			entry.getValue().status(true);
		}
		this.JTADMS = TJTADMS;
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
