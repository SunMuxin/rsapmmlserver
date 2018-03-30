package com.neusoft.aclome.alert.ai.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.neusoft.aclome.alert.ai.lib.util.CONSTANT;
import com.neusoft.aclome.alert.ai.model.ThreadDiagnoseModel;
import com.neusoft.aclome.westworld.tsp.lib.solr.SolrReader;

public class ThreadDiagnoseApplication extends Thread{
	private String OPTION_SOLR_URL = null;
	private Map<String, ThreadDiagnoseModel> TMS = null;
	private boolean stopflag = false;
	private static Thread thread = null;
	
	public ThreadDiagnoseApplication(String OPTION_SOLR_URL, String time_field) {
		this.OPTION_SOLR_URL = OPTION_SOLR_URL;
		this.TMS = new HashMap<String, ThreadDiagnoseModel>();
	}
	
	public void updateModels() throws RuntimeException {
		List<String> filters = new ArrayList<String>();
		filters.add(String.format(CONSTANT.STRING_FORMAT_SOLR_BASIC_FQ, CONSTANT.OPTION_OPTION_KEY, CONSTANT.OPTION_THREAD_DIAGNOSE_RESULT_VALUE));
		SolrReader sr = new SolrReader(this.OPTION_SOLR_URL, filters);
		Map<String, ThreadDiagnoseModel> TTMS = new HashMap<String, ThreadDiagnoseModel>();
		while(sr.hasNextResponse()) {
			JsonObject modelJSON = new JsonParser().parse(sr.nextResponse()).getAsJsonObject();
			String id = modelJSON.get(CONSTANT.SOLR_ID_KEY).getAsString();
			if (TMS.containsKey(id)) {
				TMS.get(id).status(false);
				TTMS.put(id, TMS.get(id));
				continue;
			}
			ThreadDiagnoseModel jtadm = new ThreadDiagnoseModel(OPTION_SOLR_URL, modelJSON);
			jtadm.status(false);
			TTMS.put(id, jtadm);
		}
		try {
			sr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (Map.Entry<String, ThreadDiagnoseModel> entry : TMS.entrySet()) {
			if (TTMS.containsKey(entry.getKey())) {
				entry.getValue().status(false);
			} else {
				entry.getValue().status(true);
			}
		}
		this.TMS = TTMS;
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
		for(ThreadDiagnoseModel jtadm : this.TMS.values()) {
			jtadm.status(true);
		}
	}
	
	public synchronized void status(boolean stopflag) {
		this.stopflag = stopflag;
		for(ThreadDiagnoseModel jtadm : this.TMS.values()) {
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
