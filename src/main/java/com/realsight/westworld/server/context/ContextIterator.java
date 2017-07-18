package com.realsight.westworld.server.context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import com.realsight.westworld.tsp.lib.solr.SolrReader;
import com.realsight.westworld.tsp.lib.util.Entry;
import com.realsight.westworld.tsp.lib.util.TimeUtil;

import ch.qos.logback.classic.Logger;

public class ContextIterator {
	
	private String SOLR_URL = null;
	private String name = null;
	private String solr_reader_url = null;
	private String solr_writer_url = null;
	private String fq = null;
	private Long start_timestamp = null;
	private Long interval = null;
	private Long timestamp = null;
	private String stats_fields = null;
	private String stats_type = null;
	private String stats_facet = null;
	private String id = null;
	private String show_name = null;
	private static Logger logger = (Logger) LoggerFactory.getLogger(ContextIterator.class);
	private static Random rng = new Random();
	private String time_field = null;

	public ContextIterator(String SOLR_URL, 
			Long timestamp,
			String name,
			String solr_reader_url,
			String solr_writer_url,
			String fq,
			Long start_timestamp,
			Long interval,
			String stats_fields,
			String stats_type,
			String stats_facet,
			String id,
			String show_name,
			String time_field) {
		this.SOLR_URL = SOLR_URL;
		this.timestamp = timestamp;
		this.name = name;
		this.solr_reader_url = solr_reader_url;
		this.solr_writer_url = solr_writer_url;
		this.fq = fq;
		this.start_timestamp = start_timestamp;
		this.interval = interval;
		this.stats_fields = stats_fields;
		this.stats_type = stats_type;
		this.stats_facet = stats_facet;
		this.id = id;
		this.show_name = show_name;
		this.time_field = time_field;
	}
	
	public String getStats_fields() {
		return stats_fields;
	}
	public String getSOLR_URL() {
		return SOLR_URL;
	}
	public String getName() {
		return name;
	}
	public String getSolr_reader_url() {
		return solr_reader_url;
	}
	public String getSolr_writer_url() {
		return solr_writer_url;
	}
	public String getFq() {
		return fq;
	}
	public Long getStart_timestamp() {
		return start_timestamp;
	}
	public Long getInterval() {
		return interval;
	}
	public String getStats_facet() {
		return stats_facet;
	}
	public String getStats_type() {
		return stats_type;
	}
	public String getId() {
		return id;
	}
	public String getShow_name() {
		return show_name;
	}
	public Long getTimestamp() {
		return timestamp;
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
		List<Entry<String, Double>> res = new ArrayList<Entry<String, Double>>();
		try {
			for (String stats_field : this.stats_fields.split("&")){
				double value = getValueFromInfo(minfo.get(stats_field), this.stats_type);
				res.add(new Entry<String, Double> (stats_field, value));
			}
		} catch(ClassCastException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
//		System.err.println(this.oad.detection(value, this.start_timestamp));
		if (this.stats_facet!=null && !this.stats_facet.equals("")){
			for (String stats_field : this.stats_fields.split("&")){
				List<FieldStatsInfo> facet_infos = minfo.get(stats_field).getFacets().get(this.stats_facet);
	//			System.err.println(minfo.get(this.stats_field).getFacets().get(this.stats_facet));
				for (FieldStatsInfo facet_info : facet_infos) {
	//				System.out.println(facet_info.getName());
					try {
						double facet_value = getValueFromInfo(facet_info, this.stats_type);
						String facet_name = facet_info.getName();
						res.add(new Entry<String, Double> (facet_name, facet_value));
					} catch(ClassCastException e) {
						logger.error(e.getMessage());
						e.printStackTrace();
					}
				}
			}
		}
		return res;
		
	}
	
	public synchronized Entry<List<Entry<String, Double>>, Long> next() {
		if (hasNext() == false) return null;
		List<String> filters = new ArrayList<String>();
		filters.add(this.time_field + ":[" + 
				TimeUtil.formatUnixtime2(this.start_timestamp) + " TO " + 
				TimeUtil.formatUnixtime2(this.start_timestamp+this.interval) + "]");
		for (int i = 0; i < this.fq.split("&").length; i++) {
			filters.add(this.fq.split("&")[i]);
		}
		List<String> stats_fields = new ArrayList<String>();
		for (int i = 0; i < this.stats_fields.split("&").length; i++) {
			stats_fields.add(this.stats_fields.split("&")[i]);
		}
		SolrReader sr = new SolrReader(this.solr_reader_url,
				1,
				null, 
				stats_fields,
				this.stats_facet,
				filters);
		List<Entry<String, Double>> entrys = update(sr.getFieldStatsInfo());
		this.start_timestamp += this.interval;
		try {
			sr.close();
			Thread.sleep(rng.nextInt(500));
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return new Entry<List<Entry<String, Double>>, Long> (entrys, this.start_timestamp);
	}
	
	public synchronized boolean hasNext() {
		// TODO Auto-generated method stub
		if (this.start_timestamp + this.interval > this.timestamp) {
			List<String> filters = new ArrayList<String>();
			filters.add(this.time_field + ":[" + 
					TimeUtil.formatUnixtime2(this.start_timestamp) + " TO *]");
//			filters.add(this.time_field + ":[" + 
//					TimeUtil.formatUnixtime2(this.start_timestamp) + " TO *]");
			for (int i = 0; i < this.fq.split("&").length; i++) {
				filters.add(this.fq.split("&")[i]);
			}
			SolrReader sr = new SolrReader(this.solr_reader_url, filters);
			sr.setSort(time_field, false);
			boolean ret = false;
			if (sr.hasNextResponse()) {
				try {
//					System.out.println(new JSONObject(sr.nextResponse()).optString(time_field));
					Long timestamp = TimeUtil.timeConversion2(new JSONObject(sr.nextResponse()).optString(time_field));
					this.timestamp = timestamp;
					if (this.start_timestamp + this.interval > this.timestamp) {
						ret = false;
					} else {
						ret = true;
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					logger.error(e.getMessage());
					e.printStackTrace();
				}
			}				
			try {
				sr.close();
				Thread.sleep(rng.nextInt(500));
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				logger.error(e.getMessage());
				e.printStackTrace();
			}
			return ret;
		}
		return true;
	}
}
