package com.neusoft.aclome.alert.ai.lib.tosolr;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.neusoft.aclome.alert.ai.lib.data.JmSystemData;
import com.neusoft.aclome.alert.ai.lib.data.JmThreadData;
import com.neusoft.aclome.alert.ai.lib.data.SolrContextData;
import com.neusoft.aclome.alert.ai.lib.graph.Digraph;
import com.neusoft.aclome.alert.ai.lib.graph.NodeOfDigraphInterface;
import com.neusoft.aclome.alert.ai.lib.sample.JmThreadSample;
import com.neusoft.aclome.alert.ai.lib.util.CONFIG;
import com.neusoft.aclome.alert.ai.lib.util.CONSTANT;
import com.neusoft.aclome.alert.ai.lib.util.Util;
import com.neusoft.aclome.westworld.tsp.lib.solr.SolrWriter;
import com.neusoft.aclome.westworld.tsp.lib.util.JmThreadInfo;
import com.neusoft.aclome.westworld.tsp.lib.util.TimeUtil;

public class ThreadDiagnoseInfoToSolr implements Runnable{
	
	private final SolrContextData context;
	private final Double value;
	private final Long timestamp;
	private final boolean anomaly;
	
	public ThreadDiagnoseInfoToSolr(SolrContextData context,
			Double value,
			Long timestamp,
			boolean anomaly) {
		this.context = context;
		this.value = value;
		this.timestamp = timestamp;
		this.anomaly = anomaly;
	}
	
	@SuppressWarnings("unused")
	private class Node implements NodeOfDigraphInterface{
		private String id = UUID.randomUUID().toString();
		private String name;
		private String category;
		
		public Node(String name, String category) {
			this.name = name;
			this.category = category;
			this.id = name;
		}

		@Override
		public String getId() {
			// TODO Auto-generated method stub
			return id;
		}
		
		@Override 
		public int hashCode() {
			return getId().hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			return ((Node) o).getId().equals(getId());
		}
	}
	
	@Override
	public void run() {
		String solr_writer_url = context.getProperty(CONSTANT.OPTION_SOLR_WRITER_URL_KEY);
		String res_id = context.getProperty(CONSTANT.OPTION_RES_ID_KEY);
		String name = context.getProperty(CONSTANT.OPTION_RES_NAME_KEY);
		SolrWriter sw = new SolrWriter(solr_writer_url);
		JsonObject doc = new JsonObject();
		doc.addProperty(CONSTANT.RCA_RESULT_KEY, CONSTANT.RCA_THREAD_DIAGNOSE_RESULT_VALUE);
		doc.addProperty(CONSTANT.RCA_RES_ID_KEY, res_id);
		doc.addProperty(CONSTANT.RCA_RES_NAME_KEY, name);
		doc.addProperty(CONFIG.TIME_FIELD, TimeUtil.formatUnixtime2(timestamp));
		doc.addProperty(CONSTANT.RCA_VALUE_KEY, value);
				
		try {
			if (anomaly && this.value<CONSTANT.get_thread_lock_threshold) {
				JmThreadSample jt = new JmThreadSample(context.getProperty(CONSTANT.THREAD_DIAGNOSE_URL_KEY), CONSTANT.two_minite);
				Digraph<Node> dg = new Digraph<Node>();
				JmThreadData jtd = jt.getJDT(0);
				int thread_count = 0;
				Set<String> locks = new HashSet<String>();
				boolean deadlock = false;
				while(jtd.hasNextThreadInfo()){
					JmThreadInfo jti = jtd.next();
					deadlock = jti.getDeadlocked();
					for(String res : jti.getLockedRes()) {
						Node source_node = new Node(String.format("%s%c%s", CONSTANT.LOCK_CATEGORY_NAME, CONSTANT.at, res), CONSTANT.LOCK_CATEGORY_NAME);
						Node target_node = new Node(jti.getName(), CONSTANT.THREAD_CATEGORY_NAME);
						dg.add_link(source_node, target_node, CONSTANT.double_one);
						locks.add(res);
					}
					for(String res : jti.getWaitRes()) {
						Node source_node = new Node(jti.getName(), CONSTANT.THREAD_CATEGORY_NAME);
						Node target_node = new Node(String.format("%s%c%s", CONSTANT.LOCK_CATEGORY_NAME, CONSTANT.at, res), CONSTANT.LOCK_CATEGORY_NAME);
						dg.add_link(source_node, target_node, CONSTANT.double_one);
						locks.add(res);
					}
					for(String res : jti.getBlockRes()) {
						Node source_node = new Node(jti.getName(), CONSTANT.THREAD_CATEGORY_NAME);
						Node target_node = new Node(String.format("%s%c%s", CONSTANT.LOCK_CATEGORY_NAME, CONSTANT.at, res), CONSTANT.LOCK_CATEGORY_NAME);
						dg.add_link(source_node, target_node, CONSTANT.double_one);
						locks.add(res);
					}
					thread_count += 1;
				}
				
				JsonObject summary = new JsonObject();
				summary.addProperty(CONSTANT.SUMMARY_CPU_CONTEXT_SWITCH, jt.getCPUContextSwitch());
				summary.addProperty(CONSTANT.SUMMARY_THREAD_COUNT, thread_count);
				summary.addProperty(CONSTANT.SUMMARY_LOCK_COUNT, locks.size());
				summary.addProperty(CONSTANT.SUMMARY_DEAD_LOCK, deadlock);
				summary.addProperty(CONSTANT.SUMMARY_GC_TIME, CONSTANT.int_zero);
				summary.addProperty(CONSTANT.SUMMARY_LONGEST_LOCK_CHAIN, dg.getLongestChain());
				summary.addProperty(CONSTANT.SUMMARY_THREAD_SUGGESTION, jt.getSuggestion(dg.getLongestChain(), value/100.0));
				summary.addProperty(CONSTANT.SUMMARY_THREAD_SUMMARY, jt.getSummary(dg.getLongestChain(), value/100.0));

				doc.add(CONSTANT.RCA_THREAD_STACK_TRACE_KEY, jt.getThreadStackTraces());
				doc.add(CONSTANT.RCA_THREAD_DURATION_KEY, jt.getThreadDuration());
				doc.addProperty(CONSTANT.RCA_THREAD_LOCK_KEY, dg.keyChain(CONSTANT.key_chain_limit_length).toString());
				doc.addProperty(CONSTANT.RCA_THREAD_SUMMARY_KEY, summary.toString());
				doc.addProperty(CONSTANT.RCA_TYPE_KEY, CONSTANT.MESSAGE_ANOMALY);
				Util.info(ThreadDiagnoseInfoToSolr.class.getName(), CONSTANT.MESSAGE_ANOMALY);
			} else {
				doc.addProperty(CONSTANT.RCA_TYPE_KEY, CONSTANT.MESSAGE_NOMALY);
			}
			
			JmSystemData jsd = new JmSystemData(context.getProperty(CONSTANT.SYSTEM_INFOMATION_URL_KEY));
			doc.addProperty(CONSTANT.RCA_SYSTEM_INFOMATION_KEY, jsd.getInfo().toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Util.error("ThreadDiagnoseInfoToSolr", e.getMessage());
		}
		
		try {
			sw.write(doc);
			sw.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Util.error("ThreadDiagnoseInfoToSolr", e.getMessage());
		}
	}

}
