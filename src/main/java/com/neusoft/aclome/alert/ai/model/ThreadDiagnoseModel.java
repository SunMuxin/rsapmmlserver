package com.neusoft.aclome.alert.ai.model;

import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.neusoft.aclome.alert.ai.lib.data.SolrContextData;
import com.neusoft.aclome.alert.ai.lib.tosolr.ThreadDiagnoseInfoToSolr;
import com.neusoft.aclome.alert.ai.lib.util.CONSTANT;
import com.neusoft.aclome.westworld.tsp.api.OnlineAnomalyDetectionAPI;
import com.neusoft.aclome.westworld.tsp.lib.series.TimeSeries;
import com.neusoft.aclome.westworld.tsp.lib.util.Entry;
import com.neusoft.aclome.westworld.tsp.lib.util.TimeUtil;

public class ThreadDiagnoseModel implements Job{
	
	private double pre_facet_value = 0.0;
	
	private void update(SolrContextData context, OnlineAnomalyDetectionAPI oad) throws RuntimeException {
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
	public void execute(JobExecutionContext context) throws JobExecutionException {
		// TODO Auto-generated method stub
		SolrContextData solr_context = (SolrContextData) context.getJobDetail().getJobDataMap().get("solr_context");
		OnlineAnomalyDetectionAPI anomaly_detection = (OnlineAnomalyDetectionAPI) context.getJobDetail().getJobDataMap().get("anomaly_detection");
		update(solr_context, anomaly_detection);
	}
}
