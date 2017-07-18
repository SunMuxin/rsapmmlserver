package com.realsight.westworld.server.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import com.realsight.westworld.server.model.ADModel;
import com.realsight.westworld.tsp.lib.solr.SolrReader;

import ch.qos.logback.classic.Logger;

public class ADApplication extends Thread{
	private String OPTION_SOLR_URL = null;
	private Map<String, ADModel> ADMS = null;
	private boolean stopflag = false;
	private String time_field = null;
	private static Logger logger = (Logger) LoggerFactory.getLogger(ADApplication.class);
	
	public ADApplication(String OPTION_SOLR_URL, String time_field) {
		this.OPTION_SOLR_URL = OPTION_SOLR_URL;
		this.ADMS = new HashMap<String, ADModel>();
		this.time_field = time_field;
	}
	
	public void updateModels() {
		List<String> filters = new ArrayList<String>();
		filters.add("option_s:ad");
		SolrReader sr = new SolrReader(this.OPTION_SOLR_URL, filters);
		sr.setSort("timestamp_l", true);
		Map<String, ADModel> TADMS = new HashMap<String, ADModel>();
		while(sr.hasNextResponse()) {
			try {
				JSONObject modelJSON = new JSONObject(sr.nextResponse());
				String ad_name = modelJSON.optString("ad_name_s");
				String ad_id = modelJSON.optString("id");
				String show_name = modelJSON.optString("show_name_s");
				if (ADMS.containsKey(ad_id)) {
					ADMS.get(ad_id).status(false);
					TADMS.put(ad_id, ADMS.get(ad_id));
					continue;
				}
				String solr_reader_url = modelJSON.optString("solr_reader_url_s");
				String solr_writer_url = modelJSON.optString("solr_writer_url_s");
				String fq = modelJSON.optString("fq_s");
				Long start_timestamp = modelJSON.optLong("start_timestamp_l");
				Long interval = modelJSON.optLong("interval_l");
				String stats_field = modelJSON.optString("stats_field_s");
				String stats_type = modelJSON.optString("stats_type_s");
				String stats_facet = modelJSON.optString("stats_facet_s");
				ADModel adm = new ADModel(OPTION_SOLR_URL,
						0L,
						ad_name,
						solr_reader_url,
						solr_writer_url,
						fq,
						start_timestamp,
						interval,
						stats_field,
						stats_type,
						stats_facet,
						ad_id,
						show_name,
						time_field);
				adm.status(false);
				TADMS.put(ad_id, adm);
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
		for (Map.Entry<String, ADModel> entry:ADMS.entrySet()) {
			if (TADMS.containsKey(entry.getKey())) {
				continue;
			}
			entry.getValue().status(true);
		}
		this.ADMS = TADMS;
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
			if (!this.isAlive()){
				this.start();
			}
		}
	}
	
}
