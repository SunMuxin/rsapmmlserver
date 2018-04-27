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
import com.neusoft.aclome.alert.ai.model.ThreadDiagnoseModel;
import com.neusoft.aclome.westworld.tsp.api.OnlineAnomalyDetectionAPI;
import com.neusoft.aclome.westworld.tsp.lib.solr.SolrReader;

public class ThreadDiagnoseApplication implements Job{
	
	private ConcurrentMap<String, Scheduler> TMS = new ConcurrentHashMap<String, Scheduler>();
	
	public JobDataMap getJobDataMap(JsonObject info) throws RuntimeException {
		StringBuffer fq = new StringBuffer();
		fq.append(String.format(CONSTANT.STRING_FORMAT_SOLR_BASIC_FQ, CONSTANT.DATA_RES_ID_KEY, info.get(CONSTANT.OPTION_RES_ID_KEY).getAsString()));
		fq.append(CONSTANT.and);
		fq.append(String.format(CONSTANT.STRING_FORMAT_SOLR_BASIC_FQ, info.get(CONSTANT.OPTION_STATS_FIELD_KEY).getAsString(), String.valueOf(CONSTANT.asterisk)));
		fq.append(CONSTANT.and);
		fq.append(CONSTANT.DATA_BASIC_FQ);
		
		info.addProperty(CONSTANT.OPTION_STATS_TYPE_KEY, CONSTANT.SOLR_STATS_TYPE_MEAN);
		
		if (!info.get(CONSTANT.OPTION_RES_TYPE_KEY).getAsString().equals(CONSTANT.RESOURES_TYPE_JAVAEE)) {
			throw new RuntimeException("res_type_s should be JAVAEE.");
		}
		
		SolrContextData context = new SolrContextData(info, fq.toString());
		
		String Thread_Diagnose_URL = String.format("%s%s%c%s%c%s%c%s", 
				CONSTANT.HTTP_HEADER,
				info.get(CONSTANT.OPTION_RES_IP_KEY).getAsString(),
				CONSTANT.between_ip_and_port,
				info.get(CONSTANT.OPTION_RES_PORT_KEY).getAsString(),
				CONSTANT.net_spliter,
				info.get(CONSTANT.OPTION_RES_APP_NAME_KEY).getAsString(),
				CONSTANT.net_spliter,
				CONSTANT.MONITORING_PART_THREAD_LOCK);
		
		String System_Info_URL = String.format("%s%s%c%s%c%s%c%s", 
				CONSTANT.HTTP_HEADER,
				info.get(CONSTANT.OPTION_RES_IP_KEY).getAsString(),
				CONSTANT.between_ip_and_port,
				info.get(CONSTANT.OPTION_RES_PORT_KEY).getAsString(),
				CONSTANT.net_spliter,
				info.get(CONSTANT.OPTION_RES_APP_NAME_KEY).getAsString(),
				CONSTANT.net_spliter,
				CONSTANT.MONITORING_PART_SYSTEM_INFOMATION);
		
		context.addProperty(CONSTANT.THREAD_DIAGNOSE_URL_KEY, Thread_Diagnose_URL);
		context.addProperty(CONSTANT.SYSTEM_INFOMATION_URL_KEY, System_Info_URL);
		
		
		double min = Double.parseDouble(context.getProperty(CONSTANT.OPTION_MIN_KEY).trim());
		double max = Double.parseDouble(context.getProperty(CONSTANT.OPTION_MAX_KEY).trim());
		OnlineAnomalyDetectionAPI oad = new OnlineAnomalyDetectionAPI(min, max);
		
		JobDataMap jobDataMap = new JobDataMap();

		jobDataMap.put("solr_context", context);
		jobDataMap.put("anomaly_detection", oad);

		return jobDataMap;
	}
	
	public void updateModels() throws RuntimeException, SchedulerException {
		List<String> filters = new ArrayList<String>();
		filters.add(String.format(CONSTANT.STRING_FORMAT_SOLR_BASIC_FQ, CONSTANT.OPTION_OPTION_KEY, CONSTANT.OPTION_THREAD_DIAGNOSE_RESULT_VALUE));
		SolrReader sr = new SolrReader(CONFIG.OPTION_SOLR_URL, filters);
		ConcurrentMap<String, Scheduler> TTMS = new ConcurrentHashMap<String, Scheduler>();
		while(sr.hasNextResponse()) {
			JsonObject info = new JsonParser().parse(sr.nextResponse()).getAsJsonObject();
			String id = info.get(CONSTANT.SOLR_ID_KEY).getAsString();
			if (TMS.containsKey(id)) {
				if (!TMS.get(id).isShutdown()) {
					TMS.get(id).start();
				}
				TTMS.put(id, TMS.get(id));
				continue;
			}
			Scheduler scheduler = Util.creatJob("group"+id, "job-"+id, "trigger-"+id, ThreadDiagnoseModel.class, getJobDataMap(info));
			if (!scheduler.isShutdown()) {
				scheduler.start();
			}
			TTMS.put(id, scheduler);
		}
		try {
			sr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Util.error("ADApplication", e.getMessage());
		}
		for (Map.Entry<String, Scheduler> entry : TMS.entrySet()) {
			if (!TTMS.containsKey(entry.getKey())) {
				if (!entry.getValue().isStarted()) {
					entry.getValue().shutdown();
				}
			}
		}
		this.TMS = TTMS;
	}
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		// TODO Auto-generated method stub
		try {
			updateModels();
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			Util.error("ThreadDiagnoseApplication", e.getMessage());
		}
	}
	
}
