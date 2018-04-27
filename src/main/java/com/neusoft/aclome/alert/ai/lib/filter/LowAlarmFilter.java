package com.neusoft.aclome.alert.ai.lib.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.impl.HttpSolrClient.RemoteSolrException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.neusoft.aclome.westworld.tsp.lib.solr.SolrReader;
import com.neusoft.aclome.westworld.tsp.lib.util.Util;

public class LowAlarmFilter implements Filter{
	
	@Override
	public void filter(Object out, String solr_url, List<String> tfqs) {
		// TODO Auto-generated method stub
		
		
		try {
			List<String> fqs = new ArrayList<String>();
			
			for (String fq : tfqs) {
				fqs.add(fq);
			}
			fqs.add("priority_name:low");
			
			SolrReader solr_data = new SolrReader(solr_url, fqs, "res_id");

			while(solr_data.hasNextFacet()) {
				JsonObject info = new JsonParser().parse(solr_data.nextFacet()).getAsJsonObject();
				for (Map.Entry<String, JsonElement> entry : info.entrySet()) {

					if (entry.getValue().getAsDouble() < 1) continue;

					String res_id = entry.getKey();
					
					JsonObject res_object = new JsonObject();
					if (((JsonObject) out).has(res_id)) {
						res_object = ((JsonObject) out).get(res_id).getAsJsonObject();
					}
					
					JsonObject alarm_object = new JsonObject();
					if (res_object.has("priority_name")) {
						alarm_object = res_object.get("priority_name").getAsJsonObject();
					}
					alarm_object.addProperty("lowAlarm", entry.getValue().getAsDouble());
					
					res_object.add("priority_name", alarm_object);
					
					((JsonObject) out).add(res_id, res_object);
				}
			}
			
			
			solr_data.close();
		} catch (IOException | RemoteSolrException e) {
			// TODO Auto-generated catch block
			Util.error("LowAlarmFilter", e.getMessage());
		}
	}
}
