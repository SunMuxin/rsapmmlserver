package com.neusoft.aclome.alert.ai.model;

import java.util.List;

import com.google.gson.JsonObject;
import com.neusoft.aclome.alert.ai.lib.context.SolrContext;
import com.neusoft.aclome.alert.ai.lib.tosolr.JmThreadInfoToSolr;
import com.neusoft.aclome.westworld.tsp.api.OnlineAnomalyDetectionAPI;
import com.neusoft.aclome.westworld.tsp.lib.series.TimeSeries;
import com.neusoft.aclome.westworld.tsp.lib.util.Entry;
import com.neusoft.aclome.westworld.tsp.lib.util.TimeUtil;

public class JmThreadADModel extends Thread{
	
	private static final String JAVAEE_RESOURCE_TYPE = "JAVAEE_JAVAEE";
	
	private final OnlineAnomalyDetectionAPI oad;
	private volatile boolean stopflag = true;
	private SolrContext context = null;
	private final String JMThread_URL;
	private static final Double th = 0.75;
	private Double pre_facet_value = 0.0;
	private Thread thread = null;
	
	public JmThreadADModel(String SOLR_URL, String time_field, JsonObject info) throws RuntimeException {
		StringBuffer fq = new StringBuffer();
		fq.append("res_id:"+info.get("res_id_s").getAsString());
		fq.append("&"+info.get("stats_field_s").getAsString()+":*");
		fq.append("&one_level_type:basic_info");
		info.addProperty("stats_type_s", "mean");
		
		if (!info.get("res_type_s").getAsString().equals(JAVAEE_RESOURCE_TYPE)) {
			throw new RuntimeException("res_type_s should be JAVAEE.");
		}
		info.addProperty("stats_type_s", "mean");
		this.context = new SolrContext(info, fq.toString(), time_field);
		double min = Double.parseDouble(context.getProperty("min_f").trim());
		double max = Double.parseDouble(context.getProperty("max_f").trim());
		this.oad = new OnlineAnomalyDetectionAPI(min, max);
		this.JMThread_URL = String.format("http://%s:%s/%s/monitoring?part=threadlock&format=json", 
				info.get("ip_s").getAsString(),
				info.get("port_s").getAsString(),
				info.get("appName_s").getAsString());
	}
	
	
		
	private void update(boolean to_solr_flag) throws RuntimeException {
		if (context.hasNext()==false) return ;
		
		Entry<List<Entry<String, Double>>, Long> entrys = context.nextFieldStats();
		if (entrys!=null){
			if (entrys.getFirst().size() != 1) 
				throw new RuntimeException("facet_name value number error.");
			for (Entry<String, Double> entry : entrys.getFirst()) {
				System.out.println(entry + " " + TimeUtil.formatUnixtime2(entrys.getSecond()));
				try {
					Double facet_value = entry.getSecond();
					TimeSeries.Entry<Double> res = oad.detection(facet_value, entrys.getSecond());
					if (res == null) continue;
					if (!Double.isNaN(facet_value)) {
						pre_facet_value = facet_value;
					}
					new Thread(new JmThreadInfoToSolr(context,
							JMThread_URL,
							pre_facet_value,
							entrys.getSecond(),
							(res.getItem()>th)&&to_solr_flag,
							JAVAEE_RESOURCE_TYPE), "to solr").start();
				} catch(ClassCastException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		long interval = Long.parseLong(context.getProperty("interval_l").trim());
		
		boolean to_solr_flag = true;
		while(!this.stopflag) {
			if (context.hasNext()) {
				try {
					update(to_solr_flag);
					to_solr_flag = true;
				} catch (RuntimeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				try {
					Thread.sleep(interval/5);
					to_solr_flag = true;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
