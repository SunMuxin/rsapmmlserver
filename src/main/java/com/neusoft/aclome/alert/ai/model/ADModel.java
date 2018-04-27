package com.neusoft.aclome.alert.ai.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.neusoft.aclome.alert.ai.lib.data.SolrContextData;
import com.neusoft.aclome.alert.ai.lib.tosolr.ADInfoToSolr;
import com.neusoft.aclome.alert.ai.lib.util.CONSTANT;
import com.neusoft.aclome.westworld.tsp.api.OnlineAnomalyDetectionAPI;
import com.neusoft.aclome.westworld.tsp.lib.series.TimeSeries;
import com.neusoft.aclome.westworld.tsp.lib.util.Entry;
import com.neusoft.aclome.westworld.tsp.lib.util.TimeUtil;

public class ADModel implements Job{
	private Map<String, OnlineAnomalyDetectionAPI> oads = new HashMap<String, OnlineAnomalyDetectionAPI>();

	private void update(SolrContextData context) {
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
	public void execute(JobExecutionContext context) throws JobExecutionException {
		// TODO Auto-generated method stub
		SolrContextData solr_context = (SolrContextData) context.getJobDetail().getJobDataMap().get("solr_context");
		if (solr_context.hasNext()) {
			update(solr_context);
		}
	}
}
