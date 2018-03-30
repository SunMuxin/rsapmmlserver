package com.neusoft.aclome.alert.ai.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.neusoft.aclome.alert.ai.lib.data.SolrContextData;
import com.neusoft.aclome.alert.ai.lib.tosolr.ADInfoToSolr;
import com.neusoft.aclome.alert.ai.lib.util.CONSTANT;
import com.neusoft.aclome.westworld.tsp.api.OnlineAnomalyDetectionAPI;
import com.neusoft.aclome.westworld.tsp.lib.series.TimeSeries;
import com.neusoft.aclome.westworld.tsp.lib.util.Entry;
import com.neusoft.aclome.westworld.tsp.lib.util.TimeUtil;

public class ADModel extends Thread{
	private Map<String, OnlineAnomalyDetectionAPI> oads = null;
	private volatile boolean stopflag = true;
	private SolrContextData context = null;
	private Thread thread = null;
	
	public ADModel(JsonObject info) {
		
		StringBuffer fq = new StringBuffer();
		fq.append(String.format("%s%c%s", CONSTANT.DATA_RES_ID_KEY, CONSTANT.colon, info.get(CONSTANT.OPTION_RES_ID_KEY).getAsString()));
		fq.append(CONSTANT.and);
		fq.append(String.format("%s%c%c", info.get(CONSTANT.OPTION_STATS_FIELD_KEY).getAsString(), CONSTANT.colon, CONSTANT.asterisk));
		fq.append(CONSTANT.and);
		fq.append(CONSTANT.DATA_BASIC_FQ);
		
		info.addProperty(CONSTANT.OPTION_STATS_TYPE_KEY, CONSTANT.SOLR_STATS_TYPE_MEAN);
		
		this.context = new SolrContextData(info, fq.toString());
		this.oads = new HashMap<String, OnlineAnomalyDetectionAPI>();
	}

	private void update() {
		if (context.hasNext()==false) return ;
		
		
		double min = Double.parseDouble(context.getProperty(CONSTANT.OPTION_MIN_KEY).trim());
		double max = Double.parseDouble(context.getProperty(CONSTANT.OPTION_MAX_KEY).trim());

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
					new Thread(new ADInfoToSolr(context, facet_value, res.getInstant(), res.getItem()>CONSTANT.anomaly_threshold)).start();
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
		long interval = Long.parseLong(context.getProperty(CONSTANT.OPTION_INTERVAL_KEY).trim());

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
