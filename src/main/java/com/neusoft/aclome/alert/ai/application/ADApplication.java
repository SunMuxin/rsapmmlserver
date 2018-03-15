package com.neusoft.aclome.alert.ai.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.neusoft.aclome.alert.ai.lib.util.Util;
import com.neusoft.aclome.alert.ai.model.ADModel;
import com.neusoft.aclome.westworld.tsp.lib.solr.SolrReader;

import ch.qos.logback.classic.Logger;

public class ADApplication extends Thread{
	private String OPTION_SOLR_URL = null;
	private Map<String, ADModel> ADMS = null;
	private boolean stopflag = false;
	private String time_field = null;
	private static Logger logger = (Logger) LoggerFactory.getLogger(ADApplication.class);
	private static Thread thread = null;
	
	public ADApplication(String OPTION_SOLR_URL, String time_field) {
		this.OPTION_SOLR_URL = OPTION_SOLR_URL;
		this.ADMS = new HashMap<String, ADModel>();
		this.time_field = time_field;
	}
	
	public void updateModels() {
		List<String> filters = new ArrayList<String>();
		filters.add("option_s:ad");
		SolrReader sr = new SolrReader(this.OPTION_SOLR_URL, filters);
		Map<String, ADModel> TADMS = new HashMap<String, ADModel>();
		while(sr.hasNextResponse()) {
			JsonObject modelJSON = new JsonParser().parse(sr.nextResponse()).getAsJsonObject();
			String id = modelJSON.get("id").getAsString();
			if (ADMS.containsKey(id)) {
				ADMS.get(id).status(false);
				TADMS.put(id, ADMS.get(id));
				continue;
			}
			Util.info("ADApplication - updateModels()", modelJSON.toString());
			ADModel adm = new ADModel(time_field, modelJSON);
			adm.status(false);
			TADMS.put(id, adm);
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
				entry.getValue().status(false);
			} else {
				entry.getValue().status(true);
			}
		}
		this.ADMS = TADMS;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(!stopflag) {
			try {
				updateModels();
				Thread.sleep(1000L * 60);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		for (Entry<String, ADModel> entry : ADMS.entrySet()) {
			entry.getValue().status(true);
		}
	}
	
	public synchronized void status(boolean stopflag) {
		this.stopflag = stopflag;
		for (Entry<String, ADModel> entry : ADMS.entrySet()) {
			entry.getValue().status(stopflag);
		}
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
