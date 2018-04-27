
package com.neusoft.aclome.alert.ai.application;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.neusoft.aclome.alert.ai.lib.util.CONFIG;
import com.neusoft.aclome.alert.ai.lib.util.Util;


public class UpdateCONFIGApplication implements Job {
	
	private static boolean show = true;
	
	static {
		Init();
	}
	
	public static void getConfig() {
		Path root = Paths.get(System.getProperty("user.dir")).getParent();
		Path propertyPath = Paths.get(root.toString(), 
				"config", 
				"rsapmai.properties");
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
        if (show) Util.info("initialize", "RCA_SOLR_URL = " + CONFIG.RCA_SOLR_URL);
        if (property.containsKey("option_solr_url")){
        	CONFIG.OPTION_SOLR_URL = property.getProperty("option_solr_url").trim();
        }
        if (show) Util.info("initialize", "OPTION_SOLR_URL = " + CONFIG.OPTION_SOLR_URL);
        CONFIG.DATA_SOLR_URL = null;
        if (show) Util.info("initialize", "DATA_SOLR_URL = " + CONFIG.DATA_SOLR_URL);
        if (property.containsKey("TIME_FIELD")){
        	CONFIG.TIME_FIELD = property.getProperty("TIME_FIELD").trim();
        }
        if (show) Util.info("initialize", "time_field = " + CONFIG.TIME_FIELD);
        if (property.containsKey("alert_ai_app")){
        	CONFIG.ALERT_AI_APP = Boolean.valueOf(property.getProperty("alert_ai_app").trim());
        }
        if (show) Util.info("initialize", "ALERT_AI = " + CONFIG.ALERT_AI_APP);
        if (property.containsKey("update_option_app")){
        	CONFIG.UPDATE_OPTION_APP = Boolean.valueOf(property.getProperty("update_option_app").trim());
        }
        if (show) Util.info("initialize", "UPDATE_OPTION_APP = " + CONFIG.UPDATE_OPTION_APP);
        if (property.containsKey("interval")){
        	CONFIG.INTERVAL = Long.valueOf(property.getProperty("interval").trim());
        }
        if (show) Util.info("initialize", "INTERVAL = " + CONFIG.INTERVAL);
        if (property.containsKey("ad_app")) {
        	CONFIG.AD_APP = Boolean.valueOf(property.getProperty("ad_app").trim());
        }
        if (show) Util.info("initialize", "AD_APP = " + CONFIG.AD_APP);
        if (property.containsKey("thread_diagnose_app")) {
        	CONFIG.THREAD_DIAGNOSE_APP = Boolean.valueOf(property.getProperty("thread_diagnose_app").trim());
        }
        if (show) Util.info("initialize", "THREAD_DIAGNOSE_APP = " + CONFIG.THREAD_DIAGNOSE_APP);
        if (property.containsKey("bmw_report_app")) {
        	CONFIG.BMW_REPORT_APP = Boolean.valueOf(property.getProperty("bmw_report_app").trim());
        }
        if (show) Util.info("initialize", "BMW_REPORT_APP = " + CONFIG.BMW_REPORT_APP);
	}

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		// TODO Auto-generated method stub
		getConfig();
	}

	public static void Init() {
		// TODO Auto-generated method stub
		getConfig();
		show =false;
	}
}
