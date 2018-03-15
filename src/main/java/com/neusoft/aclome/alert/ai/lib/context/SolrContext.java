package com.neusoft.aclome.alert.ai.lib.context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.JsonObject;
import com.neusoft.aclome.westworld.tsp.lib.solr.SolrReader;
import com.neusoft.aclome.westworld.tsp.lib.util.Entry;
import com.neusoft.aclome.westworld.tsp.lib.util.TimeUtil;

public class SolrContext {
	
	private JsonObject info = null;
	private String fqs = null;
	private String time_field = null;
	private long timestamp;

	public SolrContext(JsonObject info, String fqs, String time_field) {
		this.time_field = time_field;
		this.fqs = fqs;
		this.info = info;
		this.timestamp = TimeUtil.CSTTimeConversion2(getProperty(time_field))*1000;
	}
	
	public String getProperty(String key) {
		if (!info.has(key)) return null;
		return info.get(key).getAsString();
	}
	
	private double getValueFromInfo(FieldStatsInfo info, String type) {
		double value = Double.NaN;
		if (info == null) return value;
		if (!type.equals("count") && 
				!(info.getSum() instanceof Double)) {
			return value;
		}
		if (type.equals("count")) {
			value = info.getCount();
		} else if (type.equals("min")) {
			if (info.getMin() == null)
				return value;
			value = (double) info.getMin();
		} else if (type.equals("max")) {
			if (info.getMax() == null){
				return value;
			}
			value = (double) info.getMax();
		} else if (type.equals("stddev")) {
			if (info.getStddev() == null)
				return value;
			value = (double) info.getStddev();
		} else if (type.equals("mean")) {
			if (info.getMean() == null)
				return value;
			value = (double) info.getMean();
		} else if (type.equals("sum")) {
			if (info.getSum() == null)
				return value;
			value = (double) info.getSum();
		} else if (type.equals("missing")) {
			if (info.getMissing() == null)
				return value;
			value = (double) info.getMissing();
		} else {
			return value;
		}
		return value;
	}
	
	private List<Entry<String, Double>> update(Map<String, FieldStatsInfo> minfo) {
		if (minfo == null) return null;
		
		String stats_fields = getProperty("stats_field_s");
		String stats_type = getProperty("stats_type_s");
		String stats_facet = getProperty("stats_facet_s");

		List<Entry<String, Double>> res = new ArrayList<Entry<String, Double>>();
		
		try {
			for (String stats_field : stats_fields.split("&")){
				double value = getValueFromInfo(minfo.get(stats_field), stats_type);
				res.add(new Entry<String, Double> (stats_field, value));
			}
		} catch(ClassCastException e) {
			e.printStackTrace();
		}

		if (stats_facet!=null && !stats_facet.equals("")){
			for (String stats_field : stats_fields.split("&")){
				List<FieldStatsInfo> facet_infos = minfo.get(stats_field).getFacets().get(stats_facet);
				for (FieldStatsInfo facet_info : facet_infos) {
					try {
						double facet_value = getValueFromInfo(facet_info, stats_type);
						String facet_name = facet_info.getName();
						res.add(new Entry<String, Double> (facet_name, facet_value));
					} catch(ClassCastException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return res;
	}
	
	public synchronized Entry<List<Entry<String, Double>>, Long> nextFieldStats() {
		if (hasNext() == false) return null;
		
		long interval = Long.parseLong(getProperty("interval_l").trim());
		String stats_fields = getProperty("stats_field_s");
		String solr_reader_url = getProperty("solr_reader_url_s");
		String stats_facet = getProperty("stats_facet_s");
		
		List<String> filters = new ArrayList<String>();
		filters.add(this.time_field + ":[" + 
				TimeUtil.formatUnixtime2(timestamp) + " TO " + 
				TimeUtil.formatUnixtime2(timestamp+interval) + "]");
		for (int i = 0; i < fqs.split("&").length; i++) {
			filters.add(fqs.split("&")[i]);
		}
		List<String> stats_field_list = new ArrayList<String>();
		for (int i = 0; i < stats_fields.split("&").length; i++) {
			stats_field_list.add(stats_fields.split("&")[i]);
		}
		SolrReader sr = new SolrReader(solr_reader_url,
				1,
				null,
				stats_fields,
				stats_facet,
				filters);
		List<Entry<String, Double>> entrys = update(sr.getFieldStatsInfo());
		timestamp += interval;
		try {
			sr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new Entry<List<Entry<String, Double>>, Long> (entrys, this.timestamp);
	}
	
	public synchronized boolean hasNext() {
		// TODO Auto-generated method stub
		
		long interval = Long.parseLong(getProperty("interval_l").trim());
		String solr_reader_url = getProperty("solr_reader_url_s");
		
		List<String> filters = new ArrayList<String>();
		filters.add(this.time_field + ":[" + 
				TimeUtil.formatUnixtime2(this.timestamp) + " TO *]");
		for (int i = 0; i < fqs.split("&").length; i++) {
			filters.add(this.fqs.split("&")[i]);
		}
		SolrReader sr = new SolrReader(solr_reader_url, filters);
		sr.setSort(time_field, false);
		boolean ret = false;
		if (sr.hasNextResponse()) {
			try {
				long curr_timestamp = TimeUtil.timeConversion2(new JSONObject(sr.nextResponse()).optString(time_field));
				if (timestamp + interval > curr_timestamp) {
					ret = false;
				} else {
					ret = true;
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			sr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}
}
