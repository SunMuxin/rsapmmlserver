package com.realsight.westworld.server.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import com.realsight.westworld.tsp.lib.solr.SolrReader;
import com.realsight.westworld.tsp.lib.solr.SolrWriter;
import com.realsight.westworld.tsp.lib.util.Entry;
import com.realsight.westworld.tsp.lib.util.TimeUtil;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class MetricOptionApplication extends Thread{
	private String METRIC_SOLR_URL = null;
	private String RESULT_SOLR_URL = null;
	private String OPTION_SOLR_URL = null;
	private Set<String> res_ids = null;
	private boolean stopflag = false;
	private Long res_timestamp = 0L;
	private String time_field = null;
	private static String[] blacklist = new String[]{
			"id", 
			"_version_", 
			"timestamp_l",
			"JAVAEE_Http_1xx",
			"JAVAEE_Http_2xx",
			"JAVAEE_Http_3xx",
			"JAVAEE_Http_4xx",
			"JAVAEE_Http_5xx",
			"PHP_Http_1xx",
			"PHP_Http_2xx",
			"PHP_Http_3xx",
			"PHP_Http_4xx",
			"PHP_Http_5xx",
			"JAVAEE_Http_error",
			"JAVAEE_Durations_sum",
			"JAVAEE_Http_global"};
	private static Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
	static {
		logger.setLevel(Level.WARN);
	}
	
	public MetricOptionApplication(
			String OPTION_SOLR_URL, 
			String METRIC_SOLR_URL,
			String RESULT_SOLR_URL,
			String time_field,
			Long timestamp) {
		this.OPTION_SOLR_URL = OPTION_SOLR_URL;
		this.METRIC_SOLR_URL = METRIC_SOLR_URL;
		this.RESULT_SOLR_URL = RESULT_SOLR_URL;
		this.res_ids = new HashSet<String>();
		this.res_timestamp = timestamp;
		this.time_field = time_field;
	}
	
	public void add_bn_res_option(String bn_name, String res_id, String indexList) {
		if (METRIC_SOLR_URL == null) return ;
		if (RESULT_SOLR_URL == null) return ;
		SolrWriter sw = new SolrWriter(OPTION_SOLR_URL);
		List<Entry<String, ?>> entrys = new ArrayList<Entry<String,?>>();
		entrys.add(new Entry<String, String>("option_s", "bn"));
		entrys.add(new Entry<String, String>("bn_name_s", bn_name));
		entrys.add(new Entry<String, String>("solr_reader_url_s", METRIC_SOLR_URL));
		entrys.add(new Entry<String, String>("solr_writer_url_s", RESULT_SOLR_URL));
		entrys.add(new Entry<String, String>("fq_s", "res_id:"+res_id+"&one_level_type:basic_info"));
//		entrys.add(new Entry<String, Long>("starttime_l", Calendar.getInstance().getTimeInMillis()));
		entrys.add(new Entry<String, Long>("starttime_l", this.res_timestamp));
		entrys.add(new Entry<String, Long>("interval_l", 300000L));
		entrys.add(new Entry<String, Long>("gap_l", 86400000L));
		entrys.add(new Entry<String, String>("res_list_s", res_id));
		entrys.add(new Entry<String, String>("indexList_s", indexList));
		entrys.add(new Entry<String, Long>("timestamp_l", Calendar.getInstance().getTimeInMillis()));
		try {
			sw.write(entrys);
			sw.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void add_ad_res_option(String ad_name, String res_id, String stats_field, String show_name) {
		if (METRIC_SOLR_URL == null) return ;
		if (RESULT_SOLR_URL == null) return ;
		SolrWriter sw = new SolrWriter(OPTION_SOLR_URL);
		List<Entry<String, ?>> entrys = new ArrayList<Entry<String,?>>();
		entrys.add(new Entry<String, String>("option_s", "ad"));
		entrys.add(new Entry<String, String>("ad_name_s", ad_name));
		entrys.add(new Entry<String, String>("show_name_s", show_name));
		entrys.add(new Entry<String, String>("solr_reader_url_s", METRIC_SOLR_URL));
		entrys.add(new Entry<String, String>("solr_writer_url_s", RESULT_SOLR_URL));
		entrys.add(new Entry<String, String>("fq_s", "res_id:"+res_id+"&one_level_type:basic_info"));
//		entrys.add(new Entry<String, Long>("start_timestamp_l", Calendar.getInstance().getTimeInMillis()));
		entrys.add(new Entry<String, Long>("start_timestamp_l", this.res_timestamp));
		entrys.add(new Entry<String, Long>("interval_l", 300000L));
		entrys.add(new Entry<String, String>("stats_field_s", stats_field));
		entrys.add(new Entry<String, String>("stats_type_s", "mean"));
		entrys.add(new Entry<String, Long>("timestamp_l", Calendar.getInstance().getTimeInMillis()));
		try {
			sw.write(entrys);
			sw.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void add_res_id(String res_id) {
		logger.info("start add res_id:" + res_id);
		List<String> filters = new ArrayList<String>();
		filters.add("one_level_type:basic_info");
		filters.add(this.time_field+":*");
		filters.add("res_id:"+res_id);
		SolrReader sr = new SolrReader(
				this.METRIC_SOLR_URL, 
				filters);
		StringBuffer stats_fields = null;
		StringBuffer indexList = null;
		String show_name = "";
		if(sr.hasNextResponse()) {
			try {
				JSONObject json = new JSONObject(sr.nextResponse());
				show_name = json.optString("res_name");
				Iterator<String> iter = json.keys();
				while(iter.hasNext()) {
					String key = iter.next();
					boolean flag = false;
					for (String black : blacklist) {
						if (key.equals(black)) {
							flag = true;
							continue;
						}
					}
					if (flag) continue;
					Double value = json.optDouble(key);
					if (value.isNaN()) continue;
					if (stats_fields == null) {
						stats_fields = new StringBuffer();
					} else {
						stats_fields.append("&");
					}
					if (indexList == null) {
						indexList = new StringBuffer();
					} else {
						indexList.append(",");
					}
					stats_fields.append(key);
					indexList.append(res_id + ":" + key);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				logger.error(e.getMessage());
				e.printStackTrace();
			}
			add_ad_res_option(res_id, res_id, stats_fields.toString(), show_name);
			add_bn_res_option(res_id, res_id, indexList.toString());
		}
		try {
			sr.close();
			this.res_ids.add(res_id);
			logger.info("end add res_id:" + res_id);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void updateResIDOption() {
		if (this.METRIC_SOLR_URL == null) return ;
		Long timestamp = Calendar.getInstance().getTimeInMillis();
		List<String> filters = new ArrayList<String>();
		filters.add("one_level_type:basic_info");
		filters.add(this.time_field+":[" + TimeUtil.formatUnixtime2(this.res_timestamp) + 
				" TO " + TimeUtil.formatUnixtime2(timestamp) + "]");
		SolrReader sr = new SolrReader(
				this.METRIC_SOLR_URL, 
				"res_id", 
				filters);
		while(sr.hasNextFacet()) {
			try {
				JSONObject json = new JSONObject(sr.nextFacet());
//				System.out.println(json);
				Iterator<String> iter = json.keys();
				while(iter.hasNext()) {
					String key = iter.next();
					int value = json.getInt(key);
					if (value < 0) continue;
					if (res_ids.contains(key)) {
						logger.info("option contains " + key);
						continue;
					}
					add_res_id(key);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				logger.error(e.getMessage());
				e.printStackTrace();
			}
		}
		try {
			sr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		this.res_timestamp = timestamp;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(!stopflag) {
			try {
				updateResIDOption();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				logger.error(e.getMessage());
				e.printStackTrace();
			}
			try {
				Thread.sleep(1000L * 60);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				logger.error(e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	public synchronized void status(boolean stopflag) {
		this.stopflag = stopflag;
		if (stopflag) {
			return ;
		} else {
			if (!this.isAlive()){
				this.start();
			}
		}
	}
}
