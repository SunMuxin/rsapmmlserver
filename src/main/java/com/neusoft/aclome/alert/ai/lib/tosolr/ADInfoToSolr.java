package com.neusoft.aclome.alert.ai.lib.tosolr;

import com.google.gson.JsonObject;
import com.neusoft.aclome.alert.ai.lib.data.SolrContextData;
import com.neusoft.aclome.alert.ai.lib.util.CONFIG;
import com.neusoft.aclome.alert.ai.lib.util.CONSTANT;
import com.neusoft.aclome.alert.ai.lib.util.Util;
import com.neusoft.aclome.westworld.tsp.lib.solr.SolrWriter;
import com.neusoft.aclome.westworld.tsp.lib.util.TimeUtil;

public class ADInfoToSolr implements Runnable{
	
	private final SolrContextData context;
	private final Double value;
	private final Long timestamp;
	private final boolean anomaly;
	
	public ADInfoToSolr(SolrContextData context,
			Double value,
			Long timestamp,
			boolean anomaly) {
		this.context = context;
		this.value = value;
		this.timestamp = timestamp;
		this.anomaly = anomaly;
	}
		
	@Override
	public void run() {
		if (anomaly == false) return ;
		
		String solr_writer_url = context.getProperty(CONSTANT.OPTION_SOLR_WRITER_URL_KEY);
		String res_id = context.getProperty(CONSTANT.OPTION_RES_ID_KEY);
		String name = context.getProperty(CONSTANT.OPTION_RES_NAME_KEY);
		String stats_fields = context.getProperty(CONSTANT.OPTION_STATS_FIELD_KEY);
		String alert_policy_id = context.getProperty(CONSTANT.OPTION_ALERT_POLICY_ID_KEY);
		String alert_policy_name = context.getProperty(CONSTANT.OPTION_ALERT_POLICY_NAME_KEY);
		String measurement_id = context.getProperty(CONSTANT.OPTION_ALERT_MEASUREENT_ID_KEY);
		String metric_name = context.getProperty(CONSTANT.OPTION_ALERT_METRIC_NAME_KEY);
		int priority = Integer.parseInt(context.getProperty(CONSTANT.OPTION_ALERT_PRIORITY_KEY));
		String res_type = context.getProperty(CONSTANT.OPTION_RES_TYPE_KEY);
		
		SolrWriter sw = new SolrWriter(solr_writer_url);
		JsonObject doc = new JsonObject();
		
		doc.addProperty(CONSTANT.RCA_RESULT_KEY, CONSTANT.RCA_ANOMALY_DETECTION_RESULT_VALUE);
		doc.addProperty(CONSTANT.RCA_RES_ID_KEY, res_id);
		doc.addProperty(CONSTANT.RCA_RES_NAME_KEY, name);
		doc.addProperty(CONSTANT.RCA_STATS_FILEDS_KEY, stats_fields);
		doc.addProperty(CONFIG.TIME_FIELD, TimeUtil.formatUnixtime2(timestamp));
		doc.addProperty(CONSTANT.RCA_VALUE_KEY, value);
		doc.addProperty(CONSTANT.RCA_PRIORITY_KEY, priority);
		doc.addProperty(CONSTANT.RCA_ALERT_POLICY_ID_KEY, alert_policy_id);
		doc.addProperty(CONSTANT.RCA_ALERT_POLICY_NAME_KEY, alert_policy_name);
		doc.addProperty(CONSTANT.RCA_MEASUREENT_ID_KEY, measurement_id);
		doc.addProperty(CONSTANT.RCA_METRIC_NAME_KEY, metric_name);
		doc.addProperty(CONSTANT.RCA_RES_TYPE_KEY, res_type);
		
		if (anomaly) {
			Util.info(ADInfoToSolr.class.getName(), CONSTANT.MESSAGE_ANOMALY);
			doc.addProperty(CONSTANT.RCA_TYPE_KEY, CONSTANT.MESSAGE_ANOMALY);
		} else {
			doc.addProperty(CONSTANT.RCA_TYPE_KEY, CONSTANT.MESSAGE_NOMALY);
		}
		try {
			sw.write(doc);
			sw.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
