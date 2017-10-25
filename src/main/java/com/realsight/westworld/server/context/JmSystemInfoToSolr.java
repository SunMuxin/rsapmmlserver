package com.realsight.westworld.server.context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.realsight.westworld.tsp.lib.model.analysis.JmSystem;
import com.realsight.westworld.tsp.lib.solr.SolrWriter;
import com.realsight.westworld.tsp.lib.util.Entry;
import com.realsight.westworld.tsp.lib.util.TimeUtil;

public class JmSystemInfoToSolr implements Runnable{
	
	private final String JMSystem_URL;
	private final String jm_name;
	private final String show_name;
	private final String solr_writer_url;
	
	public JmSystemInfoToSolr(String JMSystem_URL,
			String jm_name,
			String show_name,
			String solr_writer_url) {
		this.JMSystem_URL = JMSystem_URL;
		this.jm_name = jm_name;
		this.show_name = show_name;
		this.solr_writer_url = solr_writer_url;
	}
		
	@Override
	public void run() {
		List<Entry<String, ?>> entrys = new ArrayList<Entry<String,?>>();
		SolrWriter sw = new SolrWriter(solr_writer_url);
		entrys.add(new Entry<String, String>("result_s", "jm_info"));
		entrys.add(new Entry<String, String>("jm_name_s", jm_name));
		entrys.add(new Entry<String, String>("show_name_s", show_name));
		entrys.add(new Entry<String, String>("rs_timestamp", TimeUtil.formatUnixtime2(System.currentTimeMillis())));
		try {
			JmSystem js = new JmSystem(JMSystem_URL);
			entrys.add(new Entry<String, String>("system_info", js.getInfo()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
