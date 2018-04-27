package com.neusoft.aclome.alert.ai.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.neusoft.aclome.alert.ai.lib.filter.Filter;
import com.neusoft.aclome.alert.ai.lib.util.AtomicSet;
import com.neusoft.aclome.alert.ai.lib.util.CONFIG;
import com.neusoft.aclome.alert.ai.lib.util.CONSTANT;
import com.neusoft.aclome.alert.ai.lib.util.Util;
import com.neusoft.aclome.westworld.tsp.lib.solr.SolrWriter;
import com.neusoft.aclome.westworld.tsp.lib.util.TimeUtil;

public class BMWReportModel implements Job {
	
	private static final int STATUS_MONTH = 1;

	private static final AtomicSet<String> calculated = new AtomicSet<String>();

	private JsonObject info;
	private List<Filter> filters = new ArrayList<Filter>();
	
	public String getProperty(String key) {
		if (!info.has(key)) return null;
		return info.get(key).getAsString();
	}
	
	private void pushSolr(JsonArray json_array) throws Exception {
		String solr_url = getProperty("solr_writer_url_s");
		SolrWriter sw = new SolrWriter(solr_url);
		JsonObject doc = new JsonObject();
		for (int i = 0; i < json_array.size(); i++) {
			JsonObject json_object = json_array.get(i).getAsJsonObject();
			for (Map.Entry<String, JsonElement> entry : json_object.entrySet()) {
				doc.add(entry.getKey(), entry.getValue());
			}
			doc.addProperty("rs_timestamp", TimeUtil.formatUnixtime2(System.currentTimeMillis()));
			Util.info("push solr", doc.toString());
			sw.write(doc);
		}

		sw.flush();
		sw.close();
	}
	
	private JsonObject status_nifi(long prev_timestamp, long curr_timestamp) {
		JsonObject out = new JsonObject();
		List<String> fqs = new ArrayList<String>();
		fqs.add(CONFIG.TIME_FIELD + ":[" + 
				TimeUtil.formatUnixtime2(prev_timestamp) + " TO " + 
				TimeUtil.formatUnixtime2(curr_timestamp) + "]");
		fqs.add("hostHealth:* OR WIN_CpuUsage:* OR LINUX_CpuUsage:* OR JAVAEE_Health:* OR dbHealth:* OR priority_name:*");
		fqs.add("-res_type:HOST_WINPROCESS");
		fqs.add("-res_type:HOST_LINUXPROCESS");
		fqs.add("-res_type:HOST_WINFS");
		
		String nifi_collection_url = getProperty("nifi_collection_url_s");
		Util.info("BMWReportModel", "nifi fqs = " + fqs);
		for (Filter filter : filters) {
			filter.filter(out, nifi_collection_url, fqs);
		}
		return out;
	}
	
	private void update_nifi() {
		
		if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) != 
				Integer.valueOf(getProperty("day_of_month_i").trim())) return ;
		
		JsonArray status_result = new JsonArray();
		
		Calendar curr_month = Calendar.getInstance();
		Calendar prev_month = Calendar.getInstance();
		prev_month.add(Calendar.MONTH, -STATUS_MONTH);
		
		JsonObject month = status_nifi(prev_month.getTimeInMillis(), curr_month.getTimeInMillis());
		JsonObject[] weeks = new JsonObject[5];
		
		Calendar curr_week = Calendar.getInstance();
		Calendar prev_week = Calendar.getInstance();
		prev_week.add(Calendar.DATE, -STATUS_MONTH*7*1);
		Calendar prev_prev_week = Calendar.getInstance();
		prev_prev_week.add(Calendar.DATE, -STATUS_MONTH*7*2);
		Calendar prev_prev_prev_week = Calendar.getInstance();
		prev_prev_prev_week.add(Calendar.DATE, -STATUS_MONTH*7*3);
		Calendar prev_prev_prev_prev_week = Calendar.getInstance();
		prev_prev_prev_prev_week.add(Calendar.DATE, -STATUS_MONTH*7*4);

		weeks[4] = status_nifi(prev_week.getTimeInMillis(), curr_week.getTimeInMillis());
		weeks[3] = status_nifi(prev_prev_week.getTimeInMillis(), prev_week.getTimeInMillis());
		weeks[2] = status_nifi(prev_prev_prev_week.getTimeInMillis(), prev_prev_week.getTimeInMillis());
		weeks[1] = status_nifi(prev_prev_prev_prev_week.getTimeInMillis(), prev_prev_prev_week.getTimeInMillis());
		weeks[0] = status_nifi(prev_month.getTimeInMillis(), prev_prev_prev_prev_week.getTimeInMillis());

		for (Map.Entry<String, JsonElement> entry : month.entrySet()) {
			JsonObject json = new JsonObject();
			JsonObject res = entry.getValue().getAsJsonObject();
			json.addProperty("res_id", entry.getKey());
			if (entry.getValue().getAsJsonObject().has("res_name")) {
				json.addProperty("res_name", res.get("res_name").getAsString());
			}
			double health_status = 0;
			double serverHealth = -1;
			double applicationHealth = -1;
			double dbHealth = -1;
			int cnt = 0;
			if (res.has("serverHealth")) {
				double sum = res.get("serverHealth").getAsJsonObject().get("sum").getAsDouble();
				double count = res.get("serverHealth").getAsJsonObject().get("count").getAsDouble();
				serverHealth = sum/count;
				health_status += serverHealth;
				cnt += 1;
			}
			if (res.has("applicationHealth")) {
				double sum = res.get("applicationHealth").getAsJsonObject().get("sum").getAsDouble();
				double count = res.get("applicationHealth").getAsJsonObject().get("count").getAsDouble();
				applicationHealth = sum/count;
				health_status += applicationHealth;
				cnt += 1;
			}
			if (res.has("dbHealth")) {
				double sum = res.get("dbHealth").getAsJsonObject().get("sum").getAsDouble();
				double count = res.get("dbHealth").getAsJsonObject().get("count").getAsDouble();
				dbHealth = sum/count;
				health_status += dbHealth;
				cnt += 1;
			}
			if (cnt > 0) {
				health_status /= cnt;
			} else {
				health_status = 100;
			}
			if (health_status>60) {
				json.addProperty("healthStatus", "normal");
			} else if (health_status>30) {
				json.addProperty("healthStatus", "warnning");
			} else {
				json.addProperty("healthStatus", "risk");
			}
			json.addProperty("serverHealth", serverHealth);
			json.addProperty("applicationHealth", applicationHealth);
			json.addProperty("dbHealth", dbHealth);
			
			JsonArray serverHealthList = new JsonArray();
			JsonArray applicationHealthList = new JsonArray();
			JsonArray dbHealthList = new JsonArray();
			
			JsonArray cpuUsageList = new JsonArray();
			JsonArray diskUsePercentList = new JsonArray();
			JsonArray memoryUsageList = new JsonArray();

			for (JsonObject week : weeks) {
				JsonObject week_res = new JsonObject();
				if (week.has(entry.getKey()))
					week_res = week.get(entry.getKey()).getAsJsonObject();
				if (week_res.has("serverHealth")) {
					double sum = week_res.get("serverHealth").getAsJsonObject().get("sum").getAsDouble();
					double count = week_res.get("serverHealth").getAsJsonObject().get("count").getAsDouble();
					serverHealthList.add(sum/count);
				} else {
					serverHealthList.add(-1);
				}
				if (week_res.has("applicationHealth")) {
					double sum = week_res.get("applicationHealth").getAsJsonObject().get("sum").getAsDouble();
					double count = week_res.get("applicationHealth").getAsJsonObject().get("count").getAsDouble();
					applicationHealthList.add(sum/count);
				} else {
					applicationHealthList.add(-1);
				}
				if (week_res.has("dbHealth")) {
					double sum = week_res.get("dbHealth").getAsJsonObject().get("sum").getAsDouble();
					double count = week_res.get("dbHealth").getAsJsonObject().get("count").getAsDouble();
					dbHealthList.add(sum/count);
				} else {
					dbHealthList.add(-1);
				}
				
				if (week_res.has("CpuUsage")) {
					double sum = week_res.get("CpuUsage").getAsJsonObject().get("sum").getAsDouble();
					double count = week_res.get("CpuUsage").getAsJsonObject().get("count").getAsDouble();
					cpuUsageList.add(sum/count);
				} else {
					cpuUsageList.add(-1);
				}
				if (week_res.has("DiskUsePercent")) {
					double sum = week_res.get("DiskUsePercent").getAsJsonObject().get("sum").getAsDouble();
					double count = week_res.get("DiskUsePercent").getAsJsonObject().get("count").getAsDouble();
					diskUsePercentList.add(sum/count);
				} else {
					diskUsePercentList.add(-1);
				}
				if (week_res.has("MemoryUsage")) {
					double sum = week_res.get("MemoryUsage").getAsJsonObject().get("sum").getAsDouble();
					double count = week_res.get("MemoryUsage").getAsJsonObject().get("count").getAsDouble();
					memoryUsageList.add(sum/count);
				} else {
					memoryUsageList.add(-1);
				}
				
			}
			json.add("serverHealthList", serverHealthList);
			json.add("applicationHealthList", applicationHealthList);
			json.add("dbHealthList", dbHealthList);
			
			json.add("cpuUsageList", cpuUsageList);
			json.add("diskUsePercentList", diskUsePercentList);
			json.add("memoryUsageList", memoryUsageList);
			
			if (res.has("priority_name")) {
				if (res.get("priority_name").getAsJsonObject().has("highAlarm")) {
					json.addProperty("highAlarm", res.get("priority_name").getAsJsonObject().get("highAlarm").getAsInt());
				} else {
					json.addProperty("highAlarm", 0);
				} 
				if (res.get("priority_name").getAsJsonObject().has("mediumAlarm")) {
					json.addProperty("mediumAlarm", res.get("priority_name").getAsJsonObject().get("mediumAlarm").getAsInt());
				} else {
					json.addProperty("mediumAlarm", 0);
				}
				if (res.get("priority_name").getAsJsonObject().has("lowAlarm")) {
					json.addProperty("lowAlarm", res.get("priority_name").getAsJsonObject().get("lowAlarm").getAsInt());
				} else {
					json.addProperty("lowAlarm", 0);
				}
			} else {
				json.addProperty("highAlarm", 0);
				json.addProperty("mediumAlarm", 0);
				json.addProperty("lowAlarm", 0);
			}
			
			if (res.has("CpuUsage")) {
				double sum = res.get("CpuUsage").getAsJsonObject().get("sum").getAsDouble();
				double count = res.get("CpuUsage").getAsJsonObject().get("count").getAsDouble();
				json.addProperty("CpuUsage", sum/count);
			}
			if (res.has("DiskUsePercent")) {
				double sum = res.get("DiskUsePercent").getAsJsonObject().get("sum").getAsDouble();
				double count = res.get("DiskUsePercent").getAsJsonObject().get("count").getAsDouble();
				json.addProperty("DiskUsePercent", sum/count);
			}
			if (res.has("MemoryUsage")) {
				double sum = res.get("MemoryUsage").getAsJsonObject().get("sum").getAsDouble();
				double count = res.get("MemoryUsage").getAsJsonObject().get("count").getAsDouble();
				json.addProperty("MemoryUsage", sum/count);
			}
			status_result.add(json);;
		}
//		Util.info("update_nifi", status_result.toString());
		try {
			pushSolr(status_result);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Util.error("BMWReportModel - update_nifi", e.getMessage());
		}
	}
	
	private JsonObject status_apm3(long prev_timestamp, long curr_timestamp) {
		JsonObject out = new JsonObject();
		List<String> fqs = new ArrayList<String>();
		fqs.add(CONFIG.TIME_FIELD + ":[" + 
				TimeUtil.formatUnixtime2(prev_timestamp) + " TO " + 
				TimeUtil.formatUnixtime2(curr_timestamp) + "]");
		fqs.add("responseElapsed:*");
		fqs.add("app_id:*");
		
		String apm3_collection_url = getProperty("apm3_collection_url_s");
		Util.info("BMWReportModel", "apm3 fqs = " + fqs);
		for (Filter filter : filters) {
			filter.filter(out, apm3_collection_url, fqs);
		}
		return out;
	}
	
	private void update_apm3() {
		
		if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) != 
				Integer.valueOf(getProperty("day_of_month_i").trim())) return ;
		
		JsonArray status_result = new JsonArray();
		
		Calendar curr_month = Calendar.getInstance();
		Calendar prev_month = Calendar.getInstance();
		prev_month.add(Calendar.MONTH, -STATUS_MONTH);
		
		JsonObject month = status_apm3(prev_month.getTimeInMillis(), curr_month.getTimeInMillis());
		JsonObject[] weeks = new JsonObject[5];
		
		Calendar curr_week = Calendar.getInstance();
		Calendar prev_week = Calendar.getInstance();
		prev_week.add(Calendar.DATE, -STATUS_MONTH*7*1);
		Calendar prev_prev_week = Calendar.getInstance();
		prev_prev_week.add(Calendar.DATE, -STATUS_MONTH*7*2);
		Calendar prev_prev_prev_week = Calendar.getInstance();
		prev_prev_prev_week.add(Calendar.DATE, -STATUS_MONTH*7*3);
		Calendar prev_prev_prev_prev_week = Calendar.getInstance();
		prev_prev_prev_prev_week.add(Calendar.DATE, -STATUS_MONTH*7*4);

		weeks[4] = status_apm3(prev_week.getTimeInMillis(), curr_week.getTimeInMillis());
		weeks[3] = status_apm3(prev_prev_week.getTimeInMillis(), prev_week.getTimeInMillis());
		weeks[2] = status_apm3(prev_prev_prev_week.getTimeInMillis(), prev_prev_week.getTimeInMillis());
		weeks[1] = status_apm3(prev_prev_prev_prev_week.getTimeInMillis(), prev_prev_prev_week.getTimeInMillis());
		weeks[0] = status_apm3(prev_month.getTimeInMillis(), prev_prev_prev_prev_week.getTimeInMillis());

		for (Map.Entry<String, JsonElement> entry : month.entrySet()) {
			JsonObject json = new JsonObject();
			JsonObject app = entry.getValue().getAsJsonObject();
			json.addProperty("app_id", entry.getKey());
			if (app.has("responseElapsed")) {
				double sum = app.get("responseElapsed").getAsJsonObject().get("sum").getAsDouble();
				double count = app.get("responseElapsed").getAsJsonObject().get("count").getAsDouble();
				json.addProperty("responseElapsed", sum/count);
			} else {
				json.addProperty("responseElapsed", -1.0);
			}
			JsonArray responseElapsedList = new JsonArray();
			for (JsonObject week : weeks) {
				JsonObject week_app = new JsonObject();
				if (week.has(entry.getKey()))
					week_app = week.get(entry.getKey()).getAsJsonObject();
				if (week_app.has("responseElapsed")) {
					double sum = week_app.get("responseElapsed").getAsJsonObject().get("sum").getAsDouble();
					double count = week_app.get("responseElapsed").getAsJsonObject().get("count").getAsDouble();
					responseElapsedList.add(sum/count);
				} else {
					responseElapsedList.add(-1);
				}
			}
			json.add("responseElapsedList", responseElapsedList);
			status_result.add(json);;
		}
		Util.info("update_apm3", status_result.toString());
		try {
			pushSolr(status_result);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Util.error("BMWReportModel - run", e.getMessage());
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		// TODO Auto-generated method stub
		
		this.info = (JsonObject) context.getJobDetail().getJobDataMap().get(CONSTANT.Job_Data_Map_Info);
		this.filters = (List<Filter>) context.getJobDetail().getJobDataMap().get(CONSTANT.Job_Data_Map_Filters);
		
		
		Calendar cal = Calendar.getInstance();

		if (calculated.containsAndAdd(String.format("%s-%d%d%d", 
				info.get(CONSTANT.SOLR_ID_KEY).getAsString(), cal.get(Calendar.YEAR), 
				cal.get(Calendar.MONTH), cal.get(Calendar.DATE))))
			return ;

		Util.info("BMWReportModel run", info.toString());

		update_nifi();
		update_apm3();
	}
}
