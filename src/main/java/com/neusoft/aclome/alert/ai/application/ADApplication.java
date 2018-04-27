package com.neusoft.aclome.alert.ai.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.neusoft.aclome.alert.ai.lib.data.SolrContextData;
import com.neusoft.aclome.alert.ai.lib.util.CONFIG;
import com.neusoft.aclome.alert.ai.lib.util.CONSTANT;
import com.neusoft.aclome.alert.ai.lib.util.Util;
import com.neusoft.aclome.alert.ai.model.ADModel;
import com.neusoft.aclome.westworld.tsp.lib.solr.SolrReader;

public class ADApplication implements Job{
		
	private ConcurrentMap<String, Scheduler> ADMS = new ConcurrentHashMap<String, Scheduler>();
	
	public JobDataMap getJobDataMap(JsonObject info) {
		StringBuffer fq = new StringBuffer();
		fq.append(String.format("%s:%s", CONSTANT.DATA_RES_ID_KEY, info.get(CONSTANT.OPTION_RES_ID_KEY).getAsString()));
		fq.append(CONSTANT.and);
		fq.append(String.format("%s:%c", info.get(CONSTANT.OPTION_STATS_FIELD_KEY).getAsString(), CONSTANT.asterisk));
		fq.append(CONSTANT.and);
		fq.append(CONSTANT.DATA_BASIC_FQ);
		
		info.addProperty(CONSTANT.OPTION_STATS_TYPE_KEY, CONSTANT.SOLR_STATS_TYPE_MEAN);
		
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put("solr_context", new SolrContextData(info, fq.toString()));
		
		return jobDataMap;
	}
	
	public void updateModels() throws SchedulerException {
		List<String> filters = new ArrayList<String>();
		filters.add(String.format("%s:%s", CONSTANT.OPTION_OPTION_KEY, CONSTANT.OPTION_ANOMALY_DETECTION_RESULT_VALUE));
		SolrReader sr = new SolrReader(CONFIG.OPTION_SOLR_URL, filters);
		ConcurrentMap<String, Scheduler> TADMS = new ConcurrentHashMap<String, Scheduler>();
		while(sr.hasNextResponse()) {
			JsonObject info = new JsonParser().parse(sr.nextResponse()).getAsJsonObject();
			String id = info.get(CONSTANT.SOLR_ID_KEY).getAsString();
			if (ADMS.containsKey(id)) {
				if (!ADMS.get(id).isShutdown()) {
					ADMS.get(id).start();
				}
				TADMS.put(id, ADMS.get(id));
				continue;
			}
			Scheduler scheduler = Util.creatJob("group"+id, "job-"+id, "trigger-"+id, ADModel.class, getJobDataMap(info));
			if (!scheduler.isShutdown()) {
				scheduler.start();
			}
			TADMS.put(id, scheduler);
		}
		try {
			sr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Util.error("ADApplication", e.getMessage());
		}
		for (Map.Entry<String, Scheduler> entry : ADMS.entrySet()) {
			if (!TADMS.containsKey(entry.getKey())) {
				if (!entry.getValue().isStarted()) {
					entry.getValue().shutdown();
				}
			}
		}
		this.ADMS = TADMS;
	}
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		// TODO Auto-generated method stub
		try {
			updateModels();
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			Util.error("ADApplication", e.getMessage());
		}
	}
}
