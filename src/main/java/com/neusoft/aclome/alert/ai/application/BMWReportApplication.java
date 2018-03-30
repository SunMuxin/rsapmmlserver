package com.neusoft.aclome.alert.ai.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.neusoft.aclome.alert.ai.lib.util.Util;
import com.neusoft.aclome.alert.ai.model.BMWReportModel;
import com.neusoft.aclome.westworld.tsp.lib.solr.SolrReader;

public class BMWReportApplication extends Thread{
	private String OPTION_SOLR_URL = null;
	private Map<String, BMWReportModel> BMWRS = null;
	private boolean stopflag = false;
	private String time_field = null;
	private static Thread thread = null;
	
	public BMWReportApplication(String OPTION_SOLR_URL, String time_field) {
		this.OPTION_SOLR_URL = OPTION_SOLR_URL;
		this.BMWRS = new HashMap<String, BMWReportModel>();
		this.time_field = time_field;
	}
	
	public void updateModels() {
		List<String> filters = new ArrayList<String>();
		filters.add("option_s:bmw_report");
		SolrReader sr = new SolrReader(this.OPTION_SOLR_URL, filters);
		Map<String, BMWReportModel> TBMWRS = new HashMap<String, BMWReportModel>();
		while(sr.hasNextResponse()) {
			JsonObject modelJSON = new JsonParser().parse(sr.nextResponse()).getAsJsonObject();
			String id = modelJSON.get("id").getAsString();
			if (BMWRS.containsKey(id)) {
				BMWRS.get(id).status(false);
				TBMWRS.put(id, BMWRS.get(id));
				continue;
			}
			BMWReportModel BMWR = new BMWReportModel(time_field, modelJSON);
			BMWR.status(false);
			TBMWRS.put(id, BMWR);
		}
		try {
			sr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Util.error("updateModels", e.getMessage());
		}
		for (Map.Entry<String, BMWReportModel> entry:BMWRS.entrySet()) {
			if (TBMWRS.containsKey(entry.getKey())) {
				entry.getValue().status(false);
			} else {
				entry.getValue().status(true);
			}
		}
		this.BMWRS = TBMWRS;
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
				Util.error("BMWReportApplication - run", e.getMessage());
			}
		}
		for (Entry<String, BMWReportModel> entry : BMWRS.entrySet()) {
			entry.getValue().status(true);
		}
	}
	
	public synchronized void status(boolean stopflag) {
		this.stopflag = stopflag;
		for (Entry<String, BMWReportModel> entry : BMWRS.entrySet()) {
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
