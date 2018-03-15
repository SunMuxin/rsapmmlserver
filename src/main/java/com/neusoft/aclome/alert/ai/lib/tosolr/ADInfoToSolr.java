package com.neusoft.aclome.alert.ai.lib.tosolr;

import java.util.ArrayList;
import java.util.List;

import com.neusoft.aclome.alert.ai.lib.context.SolrContext;
import com.neusoft.aclome.alert.ai.lib.util.Util;
import com.neusoft.aclome.westworld.tsp.lib.solr.SolrWriter;
import com.neusoft.aclome.westworld.tsp.lib.util.Entry;
import com.neusoft.aclome.westworld.tsp.lib.util.TimeUtil;

public class ADInfoToSolr implements Runnable{
	
	private final SolrContext context;
	private final Double value;
	private final Long timestamp;
	private final boolean flag;
	
	public ADInfoToSolr(SolrContext context,
			Double value,
			Long timestamp,
			boolean flag) {
		this.context = context;
		this.value = value;
		this.timestamp = timestamp;
		this.flag = flag;
	}
		
	@Override
	public void run() {
		if (flag == false) return ;
		
		String solr_writer_url = context.getProperty("solr_writer_url_s");
		String res_id = context.getProperty("res_id_s");
		String name = context.getProperty("name_s");
		String stats_fields = context.getProperty("stats_field_s");
		String alert_policy_id = context.getProperty("alert_policy_id_s");
		String alert_policy_name = context.getProperty("alert_policy_name_s");
		String measurement_id = context.getProperty("measurement_id_s");
		String metric_name = context.getProperty("metricName_s");
		int priority = Integer.parseInt(context.getProperty("priority_i"));
		String res_type = context.getProperty("res_type_s");
		
		List<Entry<String, ?>> entrys = new ArrayList<Entry<String,?>>();
		SolrWriter sw = new SolrWriter(solr_writer_url);
		entrys.add(new Entry<String, String>("result", "ad"));
		entrys.add(new Entry<String, String>("res_id", res_id));
		entrys.add(new Entry<String, String>("name", name));
		entrys.add(new Entry<String, String>("stats_fields", stats_fields));
		entrys.add(new Entry<String, String>("rs_timestamp", TimeUtil.formatUnixtime2(timestamp)));
		entrys.add(new Entry<String, Double>("value", value));
		entrys.add(new Entry<String, Integer>("priority", priority));
		entrys.add(new Entry<String, String>("alert_policy_id", alert_policy_id));
		entrys.add(new Entry<String, String>("alert_policy_name", alert_policy_name));
		entrys.add(new Entry<String, String>("measurement_id", measurement_id));
		entrys.add(new Entry<String, String>("metric_name", metric_name));
		entrys.add(new Entry<String, String>("res_type", res_type));
		
		if (flag) {
			Util.info("ADInfoToSolr", "anomaly");
			entrys.add(new Entry<String, String>("type", "anomaly"));
		} else {
			entrys.add(new Entry<String, String>("type", "nomaly"));
		}
		try {
			sw.write(entrys);
			sw.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
