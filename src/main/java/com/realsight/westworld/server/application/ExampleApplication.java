package com.realsight.westworld.server.application;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import com.realsight.westworld.tsp.api.OnlineAnomalyDetectionAPI;
import com.realsight.westworld.tsp.lib.series.DoubleSeries;
import com.realsight.westworld.tsp.lib.series.TimeSeries;
import com.realsight.westworld.tsp.lib.solr.SolrWriter;
import com.realsight.westworld.tsp.lib.util.Entry;
import com.realsight.westworld.tsp.lib.util.TimeUtil;
import com.realsight.westworld.tsp.lib.util.Triple;

import ch.qos.logback.classic.Logger;

public class ExampleApplication extends Thread{
	private String RESULT_SOLR_URL = null;
	private List<DoubleSeries> metrics = null;
	private static Random rng = new Random();
	private static Logger logger = (Logger) LoggerFactory.getLogger(ExampleApplication.class);
	
	public ExampleApplication(
			List<DoubleSeries> metrics,
			String RESULT_SOLR_URL) {
		this.RESULT_SOLR_URL = RESULT_SOLR_URL;
		this.metrics = metrics;
	}
	
	private void addADResult(Double anomaly,
			Long start_timestamp,
			List<Triple<Double, Double, Double>> prediction_result,
			Double value, 
			String facet_name,
			String show_name) throws Exception {
//		if (prediction_result == null) return ;
//		if (prediction_result.size() == 0) return ;
		SolrWriter sw = new SolrWriter(RESULT_SOLR_URL, 1000);
		logger.info("anomaly_result = " + anomaly + ", name = " + facet_name);
		logger.info("prediction_result = " + prediction_result + ", name = " + facet_name);
		System.out.println("anomaly_result = " + anomaly + ", name = " + facet_name);
		System.out.println("prediction_result = " + prediction_result + ", name = " + facet_name);
		List<Entry<String, ?>> entrys = new ArrayList<Entry<String,?>>();
		entrys.add(new Entry<String, String>("result_s", "ad"));
		entrys.add(new Entry<String, String>("ad_name_s", "example"));
		entrys.add(new Entry<String, String>("solr_reader_url_s", "example_url"));
		entrys.add(new Entry<String, String>("solr_writer_url_s", "example_url"));
		entrys.add(new Entry<String, String>("fq_s", "null"));
		entrys.add(new Entry<String, Long>("start_timestamp_l", start_timestamp));
		entrys.add(new Entry<String, String>("rs_timestamp", TimeUtil.formatUnixtime2(start_timestamp)));
		entrys.add(new Entry<String, String>("stats_field_s", "value"));
		entrys.add(new Entry<String, String>("stats_type_s", "mean"));
		entrys.add(new Entry<String, String>("stats_facet_s", "name"));
		entrys.add(new Entry<String, Double>("anomaly_f", anomaly));
		entrys.add(new Entry<String, Double>("value_f", value));
		entrys.add(new Entry<String, String>("show_name_s", show_name));
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
		sw.write(entrys);
		sw.close();
	}

	public void init(DoubleSeries metric) {
		if (RESULT_SOLR_URL == null) return ;
		double scope = metric.max() - metric.min();
		OnlineAnomalyDetectionAPI oada = new OnlineAnomalyDetectionAPI(
				metric.min() - scope*0.1, 
				metric.max() + scope*0.1);
		int n = (int) (metric.size()*0.5);
		Long invertal = 1000L * 60 * 60 * 48 / metric.size();
		DoubleSeries anomalys = new DoubleSeries(metric.getName());
		for (int i = 0; i < metric.size(); i++) {
			Long start_timestamp = Calendar.getInstance().getTimeInMillis() - (metric.size() - i) * invertal;
			double value = metric.get(i).getItem();
			long timestamp = metric.get(i).getInstant();
			List<Triple<Double, Double, Double>> prediction_result = oada.predict();
			Double anomaly = oada.detection(value, timestamp).getItem();
			if (i < n) continue;
			if (anomaly > 1.0) anomaly = 1.0;
			anomalys.add(new TimeSeries.Entry<Double>(anomaly, timestamp));
			do {
				try {
					addADResult(anomaly,
							start_timestamp,
							prediction_result,
							value,
							metric.getName(),
							"example");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					logger.error(e.getMessage());
					e.printStackTrace();
				}
				try {
					Thread.sleep(rng.nextInt(100));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			} while(true);
		}
	}
	
	public void run() {
		if (metrics == null) return ;
		while(true) {
			for (int i = 0; i < metrics.size(); i++) {
				this.init(metrics.get(i));
			}
			try {
				Thread.sleep(1000 * 60 * 60 * 24);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}
		}
	}
}
