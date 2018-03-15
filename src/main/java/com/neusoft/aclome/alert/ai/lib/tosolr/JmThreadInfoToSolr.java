package com.neusoft.aclome.alert.ai.lib.tosolr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.neusoft.aclome.alert.ai.lib.context.SolrContext;
import com.neusoft.aclome.alert.ai.lib.util.Util;
import com.neusoft.aclome.alert.ai.lib.util.graph.Digraph;
import com.neusoft.aclome.alert.ai.lib.util.graph.NodeInterface;
import com.neusoft.aclome.westworld.tsp.lib.model.analysis.JmThread;
import com.neusoft.aclome.westworld.tsp.lib.solr.SolrWriter;
import com.neusoft.aclome.westworld.tsp.lib.util.Entry;
import com.neusoft.aclome.westworld.tsp.lib.util.JmThreadInfo;
import com.neusoft.aclome.westworld.tsp.lib.util.TimeUtil;
import com.neusoft.aclome.westworld.tsp.lib.util.data.JmThreadData;

public class JmThreadInfoToSolr implements Runnable{
	
	private final SolrContext context;
	private final String JMThread_URL;
	private final Double value;
	private final Long timestamp;
	private final boolean flag;
	private final String res_type;
	
	public JmThreadInfoToSolr(SolrContext context,
			String JMThread_URL,
			Double value,
			Long timestamp,
			boolean flag,
			String res_type) {
		this.context = context;
		this.JMThread_URL = JMThread_URL;
		this.value = value;
		this.timestamp = timestamp;
		this.flag = flag;
		this.res_type = res_type;
	}
	
	@SuppressWarnings("unused")
	private class Node implements NodeInterface{
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
	}
	
	@Override
	public void run() {
		long interval = Long.parseLong(context.getProperty("interval_l").trim())/2;
		String solr_writer_url = context.getProperty("solr_writer_url_s");
		String res_id = context.getProperty("res_id_s");
		String name = context.getProperty("name_s");
		
		List<Entry<String, ?>> entrys = new ArrayList<Entry<String,?>>();
		SolrWriter sw = new SolrWriter(solr_writer_url);
		entrys.add(new Entry<String, String>("result", "jm_thread_ad"));
		entrys.add(new Entry<String, String>("res_id", res_id));
		entrys.add(new Entry<String, String>("res_type", res_type));
		entrys.add(new Entry<String, String>("name", name));
		entrys.add(new Entry<String, String>("rs_timestamp", TimeUtil.formatUnixtime2(timestamp)));
		entrys.add(new Entry<String, Double>("value", value));
		if (flag) {
			try {
				JmThread jt = new JmThread(JMThread_URL, interval);
				Digraph<Node> dg = new Digraph<Node>();
				JmThreadData jtd = jt.getJDT(0);
				while(jtd.hasNextThreadInfo()){
					JmThreadInfo jti = jtd.next();
					for(String res : jti.getLockedRes()) {
						dg.add_link(new Node("locked@"+res, "LOCKED"), new Node(jti.getName(), "THREAD"), 1.0);
					}
					for(String res : jti.getWaitRes()) {
						dg.add_link(new Node(jti.getName(), "THREAD"), new Node("locked@"+res, "LOCKED"), 1.0);
					}
					for(String res : jti.getBlockRes()) {
						dg.add_link(new Node(jti.getName(), "THREAD"), new Node("locked@"+res, "LOCKED"), 1.0);
					}
				}
				entrys.add(new Entry<String, List<String>>("thread_stack_trace", jt.getThreadStackTraces()));
				entrys.add(new Entry<String, List<String>>("thread_duration", jt.getThreadDuration()));
				entrys.add(new Entry<String, List<String>>("thread_state", jt.getStates()));
				entrys.add(new Entry<String, String>("lock_thread", dg.toString()));
				entrys.add(new Entry<String, String>("type", "anomaly"));
				
				Util.info("JmThreadInfoToSolr", "anomaly");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
