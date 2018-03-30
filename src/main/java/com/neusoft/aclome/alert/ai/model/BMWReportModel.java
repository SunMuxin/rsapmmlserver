package com.neusoft.aclome.alert.ai.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.neusoft.aclome.alert.ai.lib.filter.AlarmFilter;
import com.neusoft.aclome.alert.ai.lib.filter.DBHealthFilter;
import com.neusoft.aclome.alert.ai.lib.filter.Filter;
import com.neusoft.aclome.alert.ai.lib.filter.HostHealthFilter;
import com.neusoft.aclome.alert.ai.lib.filter.JAVAEEHealthFilter;
import com.neusoft.aclome.alert.ai.lib.filter.LinuxFilter;
import com.neusoft.aclome.alert.ai.lib.filter.ResponseElapsedFilter;
import com.neusoft.aclome.alert.ai.lib.filter.WindowsFilter;
import com.neusoft.aclome.alert.ai.lib.util.Util;
import com.neusoft.aclome.westworld.tsp.lib.solr.SolrReader;
import com.neusoft.aclome.westworld.tsp.lib.solr.SolrWriter;
import com.neusoft.aclome.westworld.tsp.lib.util.TimeUtil;

public class BMWReportModel extends Thread{
	private volatile boolean stopflag = true;
	private Thread thread = null;
	
	private final JsonObject info;
	private final String time_field;
	private static final long sleeptime = 1000 * 60 * 60 * 24;
	private static final int STATUS_MONTH = 1;
	
	private final List<Filter> filters = new ArrayList<Filter>();
	
	public BMWReportModel(String time_field, JsonObject info) {
		Util.info("BMWReportModel", info.toString());
		this.time_field = time_field;
		this.info = info;

		this.filters.add(new HostHealthFilter());
		this.filters.add(new JAVAEEHealthFilter());
		this.filters.add(new DBHealthFilter());
		this.filters.add(new AlarmFilter());
		this.filters.add(new WindowsFilter());
		this.filters.add(new LinuxFilter());
		this.filters.add(new ResponseElapsedFilter());
	}
	
	public void addFilter(Filter filter) {
		filters.add(filter);
	}
	
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
		fqs.add(time_field + ":[" + 
				TimeUtil.formatUnixtime2(prev_timestamp) + " TO " + 
				TimeUtil.formatUnixtime2(curr_timestamp) + "]");
		fqs.add("hostHealth:* OR WIN_CpuUsage:* OR LINUX_CpuUsage:* OR JAVAEE_Health:* OR dbHealth:* OR priority_name:*");
		fqs.add("-res_type:HOST_WINPROCESS");
		fqs.add("-res_type:HOST_LINUXPROCESS");
		fqs.add("-res_type:HOST_WINFS");
		
		String nifi_collection_url = getProperty("nifi_collection_url_s");
		Util.info("BMWReportModel", "nifi fqs = " + fqs);
		SolrReader nifi_data = new SolrReader(nifi_collection_url, fqs);
		nifi_data.setSort(time_field, true);
		while(nifi_data.hasNextResponse()) {
			JsonObject in = new JsonParser().parse(nifi_data.nextResponse()).getAsJsonObject();
			for (Filter filter : filters) {
				filter.filter(in, out);
			}
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
				double total = res.get("serverHealth").getAsJsonObject().get("total").getAsDouble();
				serverHealth = sum/total;
				health_status += serverHealth;
				cnt += 1;
			}
			if (res.has("applicationHealth")) {
				double sum = res.get("applicationHealth").getAsJsonObject().get("sum").getAsDouble();
				double total = res.get("applicationHealth").getAsJsonObject().get("total").getAsDouble();
				applicationHealth = sum/total;
				health_status += applicationHealth;
				cnt += 1;
			}
			if (res.has("dbHealth")) {
				double sum = res.get("dbHealth").getAsJsonObject().get("sum").getAsDouble();
				double total = res.get("dbHealth").getAsJsonObject().get("total").getAsDouble();
				dbHealth = sum/total;
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

			for (JsonObject week : weeks) {
				JsonObject week_res = new JsonObject();
				if (week.has(entry.getKey()))
					week_res = week.get(entry.getKey()).getAsJsonObject();
				if (week_res.has("serverHealth")) {
					double sum = week_res.get("serverHealth").getAsJsonObject().get("sum").getAsDouble();
					double total = week_res.get("serverHealth").getAsJsonObject().get("total").getAsDouble();
					serverHealthList.add(sum/total);
				} else {
					serverHealthList.add(-1);
				}
				if (week_res.has("applicationHealth")) {
					double sum = week_res.get("applicationHealth").getAsJsonObject().get("sum").getAsDouble();
					double total = week_res.get("applicationHealth").getAsJsonObject().get("total").getAsDouble();
					applicationHealthList.add(sum/total);
				} else {
					applicationHealthList.add(-1);
				}
				if (week_res.has("dbHealth")) {
					double sum = week_res.get("dbHealth").getAsJsonObject().get("sum").getAsDouble();
					double total = week_res.get("dbHealth").getAsJsonObject().get("total").getAsDouble();
					dbHealthList.add(sum/total);
				} else {
					dbHealthList.add(-1);
				}
			}
			json.add("serverHealthList", serverHealthList);
			json.add("applicationHealthList", applicationHealthList);
			json.add("dbHealthList", dbHealthList);
			
			if (res.has("priority_name")) {
				json.addProperty("highAlarm", res.get("priority_name").getAsJsonObject().get("highAlarm").getAsInt());
				json.addProperty("mediumAlarm", res.get("priority_name").getAsJsonObject().get("mediumAlarm").getAsInt());
				json.addProperty("lowAlarm", res.get("priority_name").getAsJsonObject().get("lowAlarm").getAsInt());
			} else {
				json.addProperty("highAlarm", 0);
				json.addProperty("mediumAlarm", 0);
				json.addProperty("lowAlarm", 0);
			}
			
			if (res.has("CpuUsage")) {
				double sum = res.get("CpuUsage").getAsJsonObject().get("sum").getAsDouble();
				double total = res.get("CpuUsage").getAsJsonObject().get("total").getAsDouble();
				json.addProperty("CpuUsage", sum/total);
			}
			if (res.has("DiskUsePercent")) {
				double sum = res.get("DiskUsePercent").getAsJsonObject().get("sum").getAsDouble();
				double total = res.get("DiskUsePercent").getAsJsonObject().get("total").getAsDouble();
				json.addProperty("DiskUsePercent", sum/total);
			}
			if (res.has("MemoryUsage")) {
				double sum = res.get("MemoryUsage").getAsJsonObject().get("sum").getAsDouble();
				double total = res.get("MemoryUsage").getAsJsonObject().get("total").getAsDouble();
				json.addProperty("MemoryUsage", sum/total);
			}
			status_result.add(json);;
		}
		Util.info("update_nifi", status_result.toString());
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
		fqs.add(time_field + ":[" + 
				TimeUtil.formatUnixtime2(prev_timestamp) + " TO " + 
				TimeUtil.formatUnixtime2(curr_timestamp) + "]");
		fqs.add("responseElapsed:*");
		fqs.add("app_id:*");
		
		String apm3_collection_url = getProperty("apm3_collection_url_s");
		Util.info("BMWReportModel", "apm3 fqs = " + fqs);
		SolrReader apm3_data = new SolrReader(apm3_collection_url, fqs);
		apm3_data.setSort(time_field, true);
		while(apm3_data.hasNextResponse()) {
			JsonObject in = new JsonParser().parse(apm3_data.nextResponse()).getAsJsonObject();
			for (Filter filter : filters) {
				filter.filter(in, out);
			}
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
				double total = app.get("responseElapsed").getAsJsonObject().get("total").getAsDouble();
				json.addProperty("responseElapsed", sum/total);
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
					double total = week_app.get("responseElapsed").getAsJsonObject().get("total").getAsDouble();
					responseElapsedList.add(sum/total);
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
	@Override
	public void run() {
		// TODO Auto-generated method stub
		Util.info("BMWReportModel", "start update");

		while(!stopflag) {
			update_nifi();
			update_apm3();
			try {
				Thread.sleep(sleeptime);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				Util.error("BMWReportModel - update_apm3", e.getMessage());
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
