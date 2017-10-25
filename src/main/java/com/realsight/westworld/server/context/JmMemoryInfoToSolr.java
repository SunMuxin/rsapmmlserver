package com.realsight.westworld.server.context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.realsight.westworld.tsp.lib.model.analysis.JmMemory;
import com.realsight.westworld.tsp.lib.solr.SolrWriter;
import com.realsight.westworld.tsp.lib.util.Entry;
import com.realsight.westworld.tsp.lib.util.TimeUtil;

public class JmMemoryInfoToSolr implements Runnable{
	
	private final ContextIterator iterator;
	private final String JMMemory_URL;
	private final Double value;
	private final Long timestamp;
	private final boolean flag;
	
	public JmMemoryInfoToSolr(ContextIterator iterator,
			String JMMemory_URL,
			Double value,
			Long timestamp,
			boolean flag) {
		this.iterator = iterator;
		this.JMMemory_URL = JMMemory_URL;
		this.value = value;
		this.timestamp = timestamp;
		this.flag = flag;
	}
		
	@Override
	public void run() {
		List<Entry<String, ?>> entrys = new ArrayList<Entry<String,?>>();
		SolrWriter sw = new SolrWriter(this.iterator.getSolr_writer_url());
		entrys.add(new Entry<String, String>("result_s", "jm_memory_ad"));
		entrys.add(new Entry<String, String>("jm_name_s", this.iterator.getName()));
		entrys.add(new Entry<String, String>("show_name_s", this.iterator.getShow_name()));
		entrys.add(new Entry<String, Long>("timestamp_l", timestamp));
		entrys.add(new Entry<String, String>("rs_timestamp", TimeUtil.formatUnixtime2(timestamp)));
		entrys.add(new Entry<String, Double>("value_f", value));
		if (flag) {
			try {
				JmMemory jm = new JmMemory(JMMemory_URL);
				entrys.add(new Entry<String, String>("memory_info", jm.getInfo()));
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
