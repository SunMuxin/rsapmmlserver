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
import com.neusoft.aclome.alert.ai.lib.filter.HighAlarmFilter;
import com.neusoft.aclome.alert.ai.lib.filter.Filter;
import com.neusoft.aclome.alert.ai.lib.filter.LowAlarmFilter;
import com.neusoft.aclome.alert.ai.lib.filter.MediumAlarmFilter;
import com.neusoft.aclome.alert.ai.lib.filter.MetricFilter;
import com.neusoft.aclome.alert.ai.lib.util.CONFIG;
import com.neusoft.aclome.alert.ai.lib.util.CONSTANT;
import com.neusoft.aclome.alert.ai.lib.util.Util;
import com.neusoft.aclome.alert.ai.model.BMWReportModel;
import com.neusoft.aclome.westworld.tsp.lib.solr.SolrReader;

public class BMWReportApplication implements Job{
	private static ConcurrentMap<String, Scheduler> BMWRS = new ConcurrentHashMap<String, Scheduler>();
	
	public JobDataMap getJobDataMap(JsonObject info) {
		JobDataMap jobDataMap = new JobDataMap();

		jobDataMap.put(CONSTANT.Job_Data_Map_Info, info);

		List<Filter> filters = new ArrayList<Filter>();
		
		filters.add(new HighAlarmFilter());
		filters.add(new MediumAlarmFilter());
		filters.add(new LowAlarmFilter());
		filters.add(new MetricFilter("res_id", "hostHealth", "serverHealth"));
		filters.add(new MetricFilter("res_id", "JAVAEE_Health", "applicationHealth"));
		filters.add(new MetricFilter("res_id", "dbHealth", "dbHealth"));
		filters.add(new MetricFilter("res_id", "LINUX_CpuUsage", "CpuUsage"));
		filters.add(new MetricFilter("res_id", "LINUX_DiskUsePercent", "DiskUsePercent"));
		filters.add(new MetricFilter("res_id", "LINUX_MemoryUsage", "MemoryUsage"));
		filters.add(new MetricFilter("res_id", "WIN_CpuUsage", "CpuUsage"));
		filters.add(new MetricFilter("res_id", "WIN_DiskUsePercent", "DiskUsePercent"));
		filters.add(new MetricFilter("res_id", "WIN_MemoryUsage", "MemoryUsage"));
		filters.add(new MetricFilter("app_id", "responseElapsed", "responseElapsed"));

		jobDataMap.put(CONSTANT.Job_Data_Map_Filters, filters);

		return jobDataMap;
	}
	
	public void updateModels() throws SchedulerException {
		List<String> filters = new ArrayList<String>();
		filters.add("option_s:bmw_report");
		SolrReader sr = new SolrReader(CONFIG.OPTION_SOLR_URL, filters);
		ConcurrentMap<String, Scheduler> TBMWRS = new ConcurrentHashMap<String, Scheduler>();
		while(sr.hasNextResponse()) {
			JsonObject info = new JsonParser().parse(sr.nextResponse()).getAsJsonObject();
			String id = info.get("id").getAsString();
			if (BMWRS.containsKey(id)) {
				TBMWRS.put(id, BMWRS.get(id));
				continue;
			}
			Scheduler scheduler = Util.creatJob("group"+id, "job-"+id, "trigger-"+id, BMWReportModel.class, getJobDataMap(info));
			if (!scheduler.isShutdown()) {
				scheduler.start();
			}
			TBMWRS.put(id, scheduler);
		}
		try {
			sr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Util.error("updateModels", e.getMessage());
		}
		for (Map.Entry<String, Scheduler> entry : BMWRS.entrySet()) {
			if (!TBMWRS.containsKey(entry.getKey())) {
				if (entry.getValue().isStarted()) {
					entry.getValue().shutdown();
				}
			}
		}
		BMWRS = TBMWRS;
	}
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		// TODO Auto-generated method stub
		try {
			updateModels();
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			Util.error("BMWReportApplication", e.getMessage());
		}
	}
	
}
