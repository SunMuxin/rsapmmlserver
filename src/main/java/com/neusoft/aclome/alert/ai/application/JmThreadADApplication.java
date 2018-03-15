package com.neusoft.aclome.alert.ai.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.neusoft.aclome.alert.ai.model.JmThreadADModel;
import com.neusoft.aclome.westworld.tsp.lib.solr.SolrReader;

public class JmThreadADApplication extends Thread{
	private String OPTION_SOLR_URL = null;
	private Map<String, JmThreadADModel> JTADMS = null;
	private boolean stopflag = false;
	private String time_field = null;
	private static Thread thread = null;
	
	public JmThreadADApplication(String OPTION_SOLR_URL, String time_field) {
		this.OPTION_SOLR_URL = OPTION_SOLR_URL;
		this.JTADMS = new HashMap<String, JmThreadADModel>();
		this.time_field = time_field;
	}
	
	public void updateModels() throws RuntimeException {
		List<String> filters = new ArrayList<String>();
		filters.add("option_s:jm_thread_ad");
		SolrReader sr = new SolrReader(this.OPTION_SOLR_URL, filters);
		Map<String, JmThreadADModel> TJTADMS = new HashMap<String, JmThreadADModel>();
		while(sr.hasNextResponse()) {
			JsonObject modelJSON = new JsonParser().parse(sr.nextResponse()).getAsJsonObject();
			String id = modelJSON.get("id").getAsString();
			if (JTADMS.containsKey(id)) {
				JTADMS.get(id).status(false);
				TJTADMS.put(id, JTADMS.get(id));
				continue;
			}
			JmThreadADModel jtadm = new JmThreadADModel(OPTION_SOLR_URL, time_field, modelJSON);
			jtadm.status(false);
			TJTADMS.put(id, jtadm);
		}
		try {
			sr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (Map.Entry<String, JmThreadADModel> entry : JTADMS.entrySet()) {
			if (TJTADMS.containsKey(entry.getKey())) {
				entry.getValue().status(false);
			} else {
				entry.getValue().status(true);
			}
		}
		this.JTADMS = TJTADMS;
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
		for(JmThreadADModel jtadm : this.JTADMS.values()) {
			jtadm.status(true);
		}
	}
	
	public synchronized void status(boolean stopflag) {
		this.stopflag = stopflag;
		for(JmThreadADModel jtadm : this.JTADMS.values()) {
			jtadm.status(stopflag);
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
