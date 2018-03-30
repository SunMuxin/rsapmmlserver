package com.neusoft.aclome.alert.ai.model;

import java.util.List;

import com.google.gson.JsonObject;
import com.neusoft.aclome.alert.ai.lib.data.SolrContextData;
import com.neusoft.aclome.alert.ai.lib.tosolr.ThreadDiagnoseInfoToSolr;
import com.neusoft.aclome.alert.ai.lib.util.CONSTANT;
import com.neusoft.aclome.westworld.tsp.api.OnlineAnomalyDetectionAPI;
import com.neusoft.aclome.westworld.tsp.lib.series.TimeSeries;
import com.neusoft.aclome.westworld.tsp.lib.util.Entry;
import com.neusoft.aclome.westworld.tsp.lib.util.TimeUtil;

public class ThreadDiagnoseModel extends Thread{
		
	private final OnlineAnomalyDetectionAPI oad;
	private volatile boolean stopflag = true;
	private SolrContextData context = null;
	private double pre_facet_value = 0.0;
	private Thread thread = null;
	
	public ThreadDiagnoseModel(String SOLR_URL, JsonObject info) throws RuntimeException {
		StringBuffer fq = new StringBuffer();
		fq.append(String.format(CONSTANT.STRING_FORMAT_SOLR_BASIC_FQ, CONSTANT.DATA_RES_ID_KEY, info.get(CONSTANT.OPTION_RES_ID_KEY).getAsString()));
		fq.append(CONSTANT.and);
		fq.append(String.format(CONSTANT.STRING_FORMAT_SOLR_BASIC_FQ, info.get(CONSTANT.OPTION_STATS_FIELD_KEY).getAsString(), String.valueOf(CONSTANT.asterisk)));
		fq.append(CONSTANT.and);
		fq.append(CONSTANT.DATA_BASIC_FQ);
				
		info.addProperty(CONSTANT.OPTION_STATS_TYPE_KEY, CONSTANT.SOLR_STATS_TYPE_MEAN);
		
		if (!info.get(CONSTANT.OPTION_RES_TYPE_KEY).getAsString().equals(CONSTANT.RESOURES_TYPE_JAVAEE)) {
			throw new RuntimeException("res_type_s should be JAVAEE.");
		}
		
		this.context = new SolrContextData(info, fq.toString());
		
		String Thread_Diagnose_URL = String.format("%s%s%c%s%c%s%c%s", 
				CONSTANT.HTTP_HEADER,
				info.get(CONSTANT.OPTION_RES_IP_KEY).getAsString(),
				CONSTANT.between_ip_and_port,
				info.get(CONSTANT.OPTION_RES_PORT_KEY).getAsString(),
				CONSTANT.net_spliter,
				info.get(CONSTANT.OPTION_RES_APP_NAME_KEY).getAsString(),
				CONSTANT.net_spliter,
				CONSTANT.MONITORING_PART_THREAD_LOCK);
		
		String System_Info_URL = String.format("%s%s%c%s%c%s%c%s", 
				CONSTANT.HTTP_HEADER,
				info.get(CONSTANT.OPTION_RES_IP_KEY).getAsString(),
				CONSTANT.between_ip_and_port,
				info.get(CONSTANT.OPTION_RES_PORT_KEY).getAsString(),
				CONSTANT.net_spliter,
				info.get(CONSTANT.OPTION_RES_APP_NAME_KEY).getAsString(),
				CONSTANT.net_spliter,
				CONSTANT.MONITORING_PART_SYSTEM_INFOMATION);
		
		context.addProperty(CONSTANT.THREAD_DIAGNOSE_URL_KEY, Thread_Diagnose_URL);
		context.addProperty(CONSTANT.SYSTEM_INFOMATION_URL_KEY, System_Info_URL);
		
		double min = Double.parseDouble(context.getProperty(CONSTANT.OPTION_MIN_KEY).trim());
		double max = Double.parseDouble(context.getProperty(CONSTANT.OPTION_MAX_KEY).trim());
		this.oad = new OnlineAnomalyDetectionAPI(min, max);
	}
	
	
		
	private void update() throws RuntimeException {
		if (context.hasNext()==false) return ;
		
		Entry<List<Entry<String, Double>>, Long> entrys = context.nextFieldStats();
		if (entrys!=null){
			if (entrys.getFirst().size() != 1) 
				throw new RuntimeException("facet_name value number error.");
			for (Entry<String, Double> entry : entrys.getFirst()) {
				System.out.println(context.getProperty(CONSTANT.OPTION_RES_ID_KEY) + " " + entry + " " + TimeUtil.formatUnixtime2(entrys.getSecond()));
				try {
					Double facet_value = entry.getSecond();
					TimeSeries.Entry<Double> res = oad.detection(facet_value, entrys.getSecond());
					if (res == null) continue;
					if (!Double.isNaN(facet_value)) {
						pre_facet_value = facet_value;
					}
					new Thread(new ThreadDiagnoseInfoToSolr(context,
							pre_facet_value,
							entrys.getSecond(),
							res.getItem()>CONSTANT.anomaly_threshold)).start();
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

		while(!this.stopflag) {
			if (context.hasNext()) {
				try {
					update();
				} catch (RuntimeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
