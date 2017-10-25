package com.realsight.westworld.server.context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.realsight.westworld.tsp.lib.model.analysis.JmThread;
import com.realsight.westworld.tsp.lib.solr.SolrWriter;
import com.realsight.westworld.tsp.lib.util.Entry;
import com.realsight.westworld.tsp.lib.util.JmThreadInfo;
import com.realsight.westworld.tsp.lib.util.TimeUtil;
import com.realsight.westworld.tsp.lib.util.data.JmThreadData;
import com.realsight.westworld.tsp.lib.util.graph.Digraph;

public class JmThreadInfoToSolr implements Runnable{
	
	private final ContextIterator iterator;
	private final String JMThread_URL;
	private final Double value;
	private final Long timestamp;
	private final boolean flag;
	
	public JmThreadInfoToSolr(ContextIterator iterator,
			String JMThread_URL,
			Double value,
			Long timestamp,
			boolean flag) {
		this.iterator = iterator;
		this.JMThread_URL = JMThread_URL;
		this.value = value;
		this.timestamp = timestamp;
		this.flag = flag;
	}
		
	@Override
	public void run() {
		List<Entry<String, ?>> entrys = new ArrayList<Entry<String,?>>();
		SolrWriter sw = new SolrWriter(this.iterator.getSolr_writer_url());
		entrys.add(new Entry<String, String>("result_s", "jm_thread_ad"));
		entrys.add(new Entry<String, String>("jm_name_s", this.iterator.getName()));
		entrys.add(new Entry<String, String>("show_name_s", this.iterator.getShow_name()));
		entrys.add(new Entry<String, Long>("timestamp_l", timestamp));
		entrys.add(new Entry<String, String>("rs_timestamp", TimeUtil.formatUnixtime2(timestamp)));
		entrys.add(new Entry<String, Double>("value_f", value));
		if (flag) {
			try {
				JmThread jt = new JmThread(JMThread_URL, iterator.getInterval());
				Digraph<JmThreadInfo> dg = new Digraph<JmThreadInfo>();
				JmThreadData jtd = jt.getJDT(0);
				while(jtd.hasNextThreadInfo()){
					JmThreadInfo jti = jtd.next();
					for(String res : jti.getLockedRes()) {
						JmThreadInfo tmp = new JmThreadInfo("locked@" + res, Long.parseLong(res, 16), "LOCKED", "");
						dg.add_link(tmp, jti, 1.0);
					}
					for(String res : jti.getWaitRes()) {
						JmThreadInfo tmp = new JmThreadInfo("locked@" + res, Long.parseLong(res, 16), "LOCKED", "");
						dg.add_link(jti, tmp, 1.0);
					}
					for(String res : jti.getBlockRes()) {
						JmThreadInfo tmp = new JmThreadInfo("locked@" + res, Long.parseLong(res, 16), "LOCKED", "");
						dg.add_link(jti, tmp, 1.0);
					}
				}
				entrys.add(new Entry<String, String>("cpu_duration", jt.getCPUDuration()));
				entrys.add(new Entry<String, String>("states_list", jt.getStatesLists()));
				entrys.add(new Entry<String, String>("graph", dg.toString()));
				entrys.add(new Entry<String, String>("sub_graph", dg.getDiameter().toString()));
				entrys.add(new Entry<String, String>("type_s", "anomaly"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			entrys.add(new Entry<String, String>("type_s", "nomaly"));
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
