package com.neusoft.aclome.alert.ai.lib.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.JsonObject;
import com.neusoft.aclome.alert.ai.lib.util.CONFIG;
import com.neusoft.aclome.alert.ai.lib.util.CONSTANT;
import com.neusoft.aclome.westworld.tsp.lib.solr.SolrReader;
import com.neusoft.aclome.westworld.tsp.lib.util.Entry;
import com.neusoft.aclome.westworld.tsp.lib.util.TimeUtil;

public class SolrContextData {
	
	private JsonObject info = null;
	private String fqs = null;
	private long timestamp;

	public SolrContextData(JsonObject info, String fqs) {
		this.fqs = fqs;
		this.info = info;
		this.timestamp = TimeUtil.CSTTimeConversion2(getProperty(CONFIG.TIME_FIELD));
	}
	
	public String getProperty(String key) {
		if (!info.has(key)) return null;
		return info.get(key).getAsString();
	}
	
	public void addProperty(String key, int value) {
		info.addProperty(key, value);
	}
	
	public void addProperty(String key, double value) {
		info.addProperty(key, value);
	}
	
	public void addProperty(String key, String value) {
		info.addProperty(key, value);
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
		
		String stats_fields = getProperty(CONSTANT.OPTION_STATS_FIELD_KEY);
		String stats_type = getProperty(CONSTANT.OPTION_STATS_TYPE_KEY);
		String stats_facet = getProperty(CONSTANT.OPTION_STATS_FACET_KEY);

		List<Entry<String, Double>> res = new ArrayList<Entry<String, Double>>();
		
		try {
			for (String stats_field : stats_fields.split(String.valueOf(CONSTANT.and))){
				double value = getValueFromInfo(minfo.get(stats_field), stats_type);
				res.add(new Entry<String, Double> (stats_field, value));
			}
		} catch(ClassCastException e) {
			e.printStackTrace();
		}

		if (stats_facet!=null && !stats_facet.equals(CONSTANT.STRING_NULL)){
			for (String stats_field : stats_fields.split(String.valueOf(CONSTANT.and))){
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
		
		long interval = Long.parseLong(getProperty(CONSTANT.OPTION_INTERVAL_KEY).trim());
		String stats_fields = getProperty(CONSTANT.OPTION_STATS_FIELD_KEY);
		String solr_reader_url = getProperty(CONSTANT.OPTION_SOLR_READER_URL_KEY);
		String stats_facet = getProperty(CONSTANT.OPTION_STATS_FACET_KEY);
		
		List<String> filters = new ArrayList<String>();
		filters.add(String.format(CONSTANT.STRING_FORMAT_SOLR_SECTION_FQ, 
				CONFIG.TIME_FIELD,
				TimeUtil.formatUnixtime2(timestamp),
				TimeUtil.formatUnixtime2(timestamp+interval)));
		for (int i = 0; i < fqs.split(String.valueOf(CONSTANT.and)).length; i++) {
			filters.add(fqs.split(String.valueOf(CONSTANT.and))[i]);
		}
		List<String> stats_field_list = new ArrayList<String>();
		for (int i = 0; i < stats_fields.split(String.valueOf(CONSTANT.and)).length; i++) {
			stats_field_list.add(stats_fields.split(String.valueOf(CONSTANT.and))[i]);
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
		
		long interval = Long.parseLong(getProperty(CONSTANT.OPTION_INTERVAL_KEY).trim());
		String solr_reader_url = getProperty(CONSTANT.OPTION_SOLR_READER_URL_KEY);
		
		List<String> filters = new ArrayList<String>();
		filters.add(String.format(CONSTANT.STRING_FORMAT_SOLR_SECTION_FQ, 
				CONFIG.TIME_FIELD,
				TimeUtil.formatUnixtime2(timestamp),
				CONSTANT.asterisk));
		for (int i = 0; i < fqs.split(String.valueOf(CONSTANT.and)).length; i++) {
			filters.add(this.fqs.split(String.valueOf(CONSTANT.and))[i]);
		}
		SolrReader sr = new SolrReader(solr_reader_url, filters);
		sr.setSort(CONFIG.TIME_FIELD, false);
		boolean ret = false;
		if (sr.hasNextResponse()) {
			try {
				long curr_timestamp = TimeUtil.timeConversion2(new JSONObject(sr.nextResponse()).optString(CONFIG.TIME_FIELD));
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
