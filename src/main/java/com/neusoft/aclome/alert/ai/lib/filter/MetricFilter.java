package com.neusoft.aclome.alert.ai.lib.filter;

import java.io.IOException;
import java.util.List;

import org.apache.solr.client.solrj.impl.HttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.response.FieldStatsInfo;

import com.google.gson.JsonObject;
import com.neusoft.aclome.westworld.tsp.lib.solr.SolrReader;
import com.neusoft.aclome.westworld.tsp.lib.util.Util;

public class MetricFilter implements Filter{

	private final String metric_name;
	private final String map_name;
	private final String res_id;
	
	public MetricFilter(String res_id, String metric_name, String map_name) {
		this.res_id = res_id;
		this.metric_name = metric_name;
		this.map_name = map_name;
	}
	@Override
	public void filter(Object out, String solr_url, List<String> fqs) {
		// TODO Auto-generated method stub
		
		
		try {
			SolrReader solr_data = new SolrReader(solr_url, fqs, metric_name, res_id, 1);

			if (!solr_data.hasNextResponse()) return ;
			for (FieldStatsInfo info : solr_data.getFieldStatsInfo().get(metric_name).getFacets().get(res_id)) {
				
				if (info.getCount() <= 0) continue;
				
				String res_id = info.getName();
				
				JsonObject metric_object = new JsonObject();
				metric_object.addProperty("sum", (double) info.getSum());
				metric_object.addProperty("count", info.getCount());
				
				JsonObject res_object = new JsonObject();
				if (((JsonObject) out).has(res_id)) {
					res_object = ((JsonObject) out).get(res_id).getAsJsonObject();
				}
				
				res_object.add(map_name, metric_object);
				
				((JsonObject) out).add(res_id, res_object);
			}
			solr_data.close();
		} catch (IOException | RemoteSolrException e) {
			// TODO Auto-generated catch block
			Util.error("MetricFilter", e.getMessage());
		}
	}
}
