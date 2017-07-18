package com.realsight.westworld.server.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import com.realsight.westworld.server.model.BNModel;
import com.realsight.westworld.tsp.lib.solr.SolrReader;

import ch.qos.logback.classic.Logger;

public class BNApplication extends Thread{
	private String OPTION_SOLR_URL = null;
	private Map<String, BNModel> BNMS = null;
	private boolean stopflag = false;
	private static Logger logger = (Logger) LoggerFactory.getLogger(BNApplication.class);
	
	public BNApplication(String OPTION_SOLR_URL) {
		this.OPTION_SOLR_URL = OPTION_SOLR_URL;
		this.BNMS = new HashMap<String, BNModel>();
	}
	
	public void updateModels() {
		List<String> filters = new ArrayList<String>();
		filters.add("option_s:bn");
		SolrReader sr = new SolrReader(this.OPTION_SOLR_URL, filters);
		sr.setSort("timestamp_l", true);
		Map<String, BNModel> TBNMS = new HashMap<String, BNModel>();
		while(sr.hasNextResponse()) {
			try {
				JSONObject modelJSON = new JSONObject(sr.nextResponse());
				System.err.println(modelJSON);
				String bn_name = modelJSON.optString("bn_name_s");
				String bn_id = modelJSON.optString("id");
				String show_name = modelJSON.optString("show_name_s");
				String solr_reader_url = modelJSON.optString("solr_reader_url_s");
				String solr_writer_url = modelJSON.optString("solr_writer_url_s");
				String fq = modelJSON.optString("fq_s");
				Long interval = modelJSON.optLong("interval_l");
				String stats_field = modelJSON.optString("stats_field_s");
				String stats_type = modelJSON.optString("stats_type_s");
				String stats_facet = modelJSON.optString("stats_facet_s");
				Long timestamp = modelJSON.optLong("timestamp_l");
				Long train_num = modelJSON.optLong("train_num_l");
				Long start_timestamp = modelJSON.optLong("start_timestamp_l");
				Long update_frequency = modelJSON.optLong("update_frequency_l");
				BNModel bnm = TBNMS.getOrDefault(bn_name, new BNModel(bn_name,
						train_num,
						start_timestamp,
						update_frequency,
						interval,
						show_name));
				bnm.addContextIterator(OPTION_SOLR_URL, 
						timestamp, 
						solr_reader_url, 
						solr_writer_url, 
						fq, 
						stats_field, 
						stats_type, 
						stats_facet, 
						bn_id);
				
				TBNMS.put(bn_name, bnm);
				break;
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
		for (Map.Entry<String, BNModel> entry: TBNMS.entrySet()) {
			entry.getValue().status(false);
		}
		for (Map.Entry<String, BNModel> entry: BNMS.entrySet()) {
			if (TBNMS.containsKey(entry.getKey())) {
				continue;
			}
			entry.getValue().status(true);
		}
		this.BNMS = TBNMS;
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
