package com.neusoft.aclome.alert.ai.application;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import com.google.gson.JsonObject;
import com.neusoft.aclome.alert.ai.lib.context.SolrContext;
import com.neusoft.aclome.alert.ai.lib.util.CONFIG;
import com.neusoft.aclome.alert.ai.lib.util.Util;


@SuppressWarnings("unused")
public class UpdateCONFIGApplication implements Runnable {
	
	private static Thread thread = null;
	
	private boolean stopflag = false;
	
	private static final String SOLR_URL_KEY = "apm.solr.url";
	private static final String SOLR_CORE_SELECTOR_KEY = "apm.solr.core.selector";
	private static final char WEB_SEPARATOR = '/';
	
	private static final long sleep_time_mill = 1000 * 60 * 10;
	
	public static void update() {
		Path root = Paths.get(System.getProperty("user.dir")).getParent();
		Path propertyPath = Paths.get(root.toString(), 
				"config", 
				"rsapmml.properties");
        Properties property = new Properties();
        try {
			property.load(new FileInputStream(propertyPath.toFile()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        if (property.containsKey("rca_solr_url")){
        	CONFIG.RCA_SOLR_URL = property.getProperty("rca_solr_url").trim();
        }
        Util.info("initialize", "RCA_SOLR_URL = " + CONFIG.RCA_SOLR_URL);
        if (property.containsKey("option_solr_url")){
        	CONFIG.OPTION_SOLR_URL = property.getProperty("option_solr_url").trim();
        }
        Util.info("initialize", "OPTION_SOLR_URL = " + CONFIG.OPTION_SOLR_URL);
        CONFIG.DATA_SOLR_URL = null;
        Util.info("initialize", "DATA_SOLR_URL = " + CONFIG.DATA_SOLR_URL);
        if (property.containsKey("TIME_FIELD")){
        	CONFIG.TIME_FIELD = property.getProperty("TIME_FIELD").trim();
        }
        Util.info("initialize", "time_field = " + CONFIG.TIME_FIELD);
        if (property.containsKey("alert_ai_app")){
        	CONFIG.ALERT_AI_APP = Boolean.valueOf(property.getProperty("alert_ai_app").trim());
        }
        Util.info("initialize", "ALERT_AI = " + CONFIG.ALERT_AI_APP);
        if (property.containsKey("update_option_app")){
        	CONFIG.UPDATE_OPTION_APP = Boolean.valueOf(property.getProperty("update_option_app").trim());
        }
        Util.info("initialize", "UPDATE_OPTION_APP = " + CONFIG.UPDATE_OPTION_APP);
        if (property.containsKey("interval")){
        	CONFIG.INTERVAL = Long.valueOf(property.getProperty("interval").trim());
        }
        Util.info("initialize", "INTERVAL = " + CONFIG.INTERVAL);
        if (property.containsKey("ad_app")) {
        	CONFIG.AD_APP = Boolean.valueOf(property.getProperty("ad_app").trim());
        }
        Util.info("initialize", "AD_APP = " + CONFIG.AD_APP);
        if (property.containsKey("thread_diagnose_app")) {
        	CONFIG.THREAD_DIAGNOSE_APP = Boolean.valueOf(property.getProperty("thread_diagnose_app").trim());
        }
        Util.info("initialize", "THREAD_DIAGNOSE_APP = " + CONFIG.THREAD_DIAGNOSE_APP);
        if (property.containsKey("bmw_report_app")) {
        	CONFIG.BMW_REPORT_APP = Boolean.valueOf(property.getProperty("bmw_report_app").trim());
        }
        Util.info("initialize", "BMW_REPORT_APP = " + CONFIG.BMW_REPORT_APP);
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(!stopflag) {
			try {
				update();
				Thread.sleep(sleep_time_mill);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				Util.error("AlertApplication.run()", e.getMessage());
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
