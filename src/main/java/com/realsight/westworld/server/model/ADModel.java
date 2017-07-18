package com.realsight.westworld.server.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import com.realsight.westworld.server.context.ContextIterator;
import com.realsight.westworld.tsp.api.OnlineAnomalyDetectionAPI;
import com.realsight.westworld.tsp.lib.series.TimeSeries;
import com.realsight.westworld.tsp.lib.solr.SolrWriter;
import com.realsight.westworld.tsp.lib.util.Entry;
import com.realsight.westworld.tsp.lib.util.TimeUtil;
import com.realsight.westworld.tsp.lib.util.Triple;

import ch.qos.logback.classic.Logger;

public class ADModel extends Thread{
	private Map<String, OnlineAnomalyDetectionAPI> oads = null;
	private volatile boolean stopflag = true;
	private ContextIterator iterator = null;
	private static Random rng = new Random();
	private static Logger logger = (Logger) LoggerFactory.getLogger(ADModel.class);
	
	public ADModel(String SOLR_URL, 
			Long timestamp,
			String ad_name,
			String solr_reader_url,
			String solr_writer_url,
			String fq,
			Long start_timestamp,
			Long interval,
			String stats_field,
			String stats_type,
			String stats_facet,
			String ad_id,
			String show_name,
			String time_field) {
		this.iterator = new ContextIterator(SOLR_URL, 
				timestamp, 
				ad_name, 
				solr_reader_url, 
				solr_writer_url, 
				fq, 
				start_timestamp, 
				interval, 
				stats_field, 
				stats_type, 
				stats_facet, 
				ad_id, 
				show_name,
				time_field);
		this.oads = new HashMap<String, OnlineAnomalyDetectionAPI>();
	}
	
	private void addADResult(TimeSeries.Entry<Double> anomaly_result, 
			List<Triple<Double, Double, Double>> prediction_result,
			Double value, 
			String facet_name) {
		if (anomaly_result == null) return ;
//		if (prediction_result == null) return ;
//		if (prediction_result.size() == 0) return ;
		SolrWriter sw = new SolrWriter(this.iterator.getSolr_writer_url(), 1000);
		logger.info("anomaly_result = " + anomaly_result + ", name = " + facet_name);
		logger.info("prediction_result = " + prediction_result + ", name = " + facet_name);
		System.out.println("anomaly_result = " + anomaly_result + ", name = " + facet_name);
		System.out.println("prediction_result = " + prediction_result + ", name = " + facet_name);
		List<Entry<String, ?>> entrys = new ArrayList<Entry<String,?>>();
		entrys.add(new Entry<String, String>("result_s", "ad"));
		entrys.add(new Entry<String, String>("ad_name_s", this.iterator.getName()));
		entrys.add(new Entry<String, String>("solr_reader_url_s", this.iterator.getSolr_reader_url()));
		entrys.add(new Entry<String, String>("solr_writer_url_s", this.iterator.getSolr_writer_url()));
		entrys.add(new Entry<String, String>("fq_s", this.iterator.getFq()));
		entrys.add(new Entry<String, Long>("start_timestamp_l", anomaly_result.getInstant()));
		entrys.add(new Entry<String, String>("rs_timestamp", TimeUtil.formatUnixtime2(anomaly_result.getInstant())));
		entrys.add(new Entry<String, Long>("interval_l", this.iterator.getInterval()));
		entrys.add(new Entry<String, String>("stats_field_s", this.iterator.getStats_fields()));
		entrys.add(new Entry<String, String>("stats_type_s", this.iterator.getStats_type()));
		entrys.add(new Entry<String, String>("stats_facet_s", this.iterator.getStats_facet()));
		entrys.add(new Entry<String, Double>("anomaly_f", anomaly_result.getItem()));
		entrys.add(new Entry<String, Double>("value_f", value));
		entrys.add(new Entry<String, String>("ad_id", this.iterator.getId()));
		entrys.add(new Entry<String, String>("show_name_s", this.iterator.getShow_name()));
		JSONArray predictions = new JSONArray();
		double up_margin = Double.NEGATIVE_INFINITY, down_margin = Double.POSITIVE_INFINITY;
		for (int i = 0; i < prediction_result.size(); i++) {
			double from = prediction_result.get(i).getFirst();
			double to = prediction_result.get(i).getSecond();
			double probility = prediction_result.get(i).getThird();
			
			if (probility > 0.1) {
				up_margin = Math.max(up_margin, to);
				down_margin = Math.min(down_margin, from);
			}
			String arange = String.valueOf(from) + " TO " + String.valueOf(to);
			JSONObject prediction = new JSONObject();
			try {
				prediction.put(arange, probility);
				predictions.put(prediction);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				logger.error(e.getMessage());
			}
		}
		if (value < up_margin && value > down_margin) {
			up_margin = (0.2+rng.nextDouble()*0.2)*(up_margin-value) + value;
			down_margin = (rng.nextDouble()*0.2+0.6)*(value-down_margin) + down_margin;
		} else if (value > up_margin) {
			down_margin = (rng.nextDouble()*0.2+0.6)*(up_margin-down_margin) + down_margin;
		} else if (value < down_margin) {
			up_margin = (0.2+rng.nextDouble()*0.2)*(up_margin-down_margin) + down_margin;
		}
		String factor = null;
		if (down_margin > value) {
			factor = "Small";
		} else if (up_margin < value){
			factor = "Large";
		} else {
			factor = "Strange Fluctuation";
		}
		entrys.add(new Entry<String, Double>("up_margin_f", up_margin));
		entrys.add(new Entry<String, Double>("down_margin_f", down_margin));
		entrys.add(new Entry<String, String>("anomaly_factor_s", factor));
		entrys.add(new Entry<String, String>("predictions_s", predictions.toString()));
		entrys.add(new Entry<String, String>("facet_name_s", facet_name));
		entrys.add(new Entry<String, Long>("timestamp_l", this.iterator.getTimestamp()));
		try {
			sw.write(entrys);
			sw.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}
		
	private void update() {
		if (this.iterator.hasNext()==false) return ;
		Entry<List<Entry<String, Double>>, Long> entrys = iterator.next();
		if (entrys!=null){
			for (Entry<String, Double> entry : entrys.getFirst()) {
//				System.out.println(facet_info.getName());
				try {
					double facet_value = entry.getSecond();
					String facet_name = entry.getFirst();
					logger.info(facet_name + " " + facet_value);
					OnlineAnomalyDetectionAPI facet_oad = this.oads.getOrDefault(facet_name, new OnlineAnomalyDetectionAPI());
					List<Triple<Double, Double, Double>> prediction_result = facet_oad.predict();
					TimeSeries.Entry<Double> res = facet_oad.detection(facet_value, entrys.getSecond());
					addADResult(res, prediction_result, facet_value, facet_name);
					this.oads.put(facet_name, facet_oad);
				} catch(ClassCastException e) {
					e.printStackTrace();
					logger.error(e.getMessage());
				}
			}
		}
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(!this.stopflag) {
			if (this.iterator.hasNext()) {
				update();
			} else {
				try {
					Thread.sleep(this.iterator.getInterval());
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					logger.error(e.getMessage());
				}
			}
			
		}
	}
	
	public synchronized void status(boolean stopflag) {
		this.stopflag = stopflag;
		if (stopflag) {
			return ;
		}
		if (this.isAlive())
			return ;
		this.start();
	}
}
