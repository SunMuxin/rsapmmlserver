package com.neusoft.aclome.alert.ai.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.neusoft.aclome.alert.ai.lib.context.SolrContext;
import com.neusoft.aclome.alert.ai.lib.tosolr.ADInfoToSolr;
import com.neusoft.aclome.westworld.tsp.api.OnlineAnomalyDetectionAPI;
import com.neusoft.aclome.westworld.tsp.lib.series.TimeSeries;
import com.neusoft.aclome.westworld.tsp.lib.util.Entry;
import com.neusoft.aclome.westworld.tsp.lib.util.TimeUtil;

public class ADModel extends Thread{
	private Map<String, OnlineAnomalyDetectionAPI> oads = null;
	private volatile boolean stopflag = true;
	private SolrContext context = null;
	private Thread thread = null;
	private static final Double th = 0.75;
	
	public ADModel(String time_field, JsonObject info) {
		StringBuffer fq = new StringBuffer();
		fq.append("res_id:"+info.get("res_id_s").getAsString());
		fq.append("&"+info.get("stats_field_s").getAsString()+":*");
		fq.append("&one_level_type:basic_info");
		
		info.addProperty("stats_type_s", "mean");
		this.context = new SolrContext(info, fq.toString(), time_field);
		this.oads = new HashMap<String, OnlineAnomalyDetectionAPI>();
	}

	private void update() {
		if (context.hasNext()==false) return ;
		
		double min = Double.parseDouble(context.getProperty("min_f").trim());
		double max = Double.parseDouble(context.getProperty("max_f").trim());

		Entry<List<Entry<String, Double>>, Long> entrys = context.nextFieldStats();
		if (entrys!=null){
			for (Entry<String, Double> entry : entrys.getFirst()) {
				System.out.println(entry + " " + TimeUtil.formatUnixtime2(entrys.getSecond()));
				try {
					double facet_value = entry.getSecond();
					String facet_name = entry.getFirst();
					OnlineAnomalyDetectionAPI facet_oad = null;
					facet_oad = this.oads.getOrDefault(facet_name, new OnlineAnomalyDetectionAPI(min, max));
					TimeSeries.Entry<Double> res = facet_oad.detection(facet_value, entrys.getSecond());
					if (res == null) continue;
					new Thread(new ADInfoToSolr(context, facet_value, res.getInstant(), res.getItem()>th), "to solr").start();
					this.oads.put(facet_name, facet_oad);
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

		while(!stopflag) {
			if (context.hasNext()) {
				update();
			} else {
				try {
					Thread.sleep(interval/5);
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
