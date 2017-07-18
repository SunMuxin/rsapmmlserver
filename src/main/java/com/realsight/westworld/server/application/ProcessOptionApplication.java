package com.realsight.westworld.server.application;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.realsight.westworld.tsp.lib.solr.SolrWriter;
import com.realsight.westworld.tsp.lib.util.Entry;

import ch.qos.logback.classic.Logger;

public class ProcessOptionApplication extends Thread{
	private String RESULT_SOLR_URL = null;
	private String NAPM_SOLR_URL = null;
	private String OPTION_SOLR_URL = null;
	private Long process_timestamp = 0L;
	private String process_names = null;
	private static Logger logger = (Logger) LoggerFactory.getLogger(ProcessOptionApplication.class);
	
	public ProcessOptionApplication(
			String OPTION_SOLR_URL, 
			String NAPM_SOLR_URL,
			String RESULT_SOLR_URL,
			String process_names,
			Long timestamp) {
		this.OPTION_SOLR_URL = OPTION_SOLR_URL;
		this.NAPM_SOLR_URL = NAPM_SOLR_URL;
		this.RESULT_SOLR_URL = RESULT_SOLR_URL;
		new HashSet<String>();
		this.process_timestamp = timestamp;
		this.process_names = process_names;
		this.init();
	}
	
	public void init() {
		if (this.process_names == null) return ;
		for (String process_name : this.process_names.split(",")) {
			add_ad_process_option(process_name, process_name, "system_process_memory_rss_bytes_f");
			add_ad_process_option(process_name, process_name, "system_process_memory_share_f");
			add_ad_process_option(process_name, process_name, "system_process_cpu_total_pct_f");
		}
	}
	
	public void add_ad_process_option(String ad_name, String show_name, String field) {
		if (NAPM_SOLR_URL == null) return ;
		if (RESULT_SOLR_URL == null) return ;
		SolrWriter sw = new SolrWriter(OPTION_SOLR_URL);
		List<Entry<String, ?>> entrys = new ArrayList<Entry<String,?>>();
		entrys.add(new Entry<String, String>("option_s", "ad"));
		entrys.add(new Entry<String, String>("ad_name_s", ad_name));
		entrys.add(new Entry<String, String>("show_name_s", show_name));
		entrys.add(new Entry<String, String>("solr_reader_url_s", NAPM_SOLR_URL));
		entrys.add(new Entry<String, String>("solr_writer_url_s", RESULT_SOLR_URL));
		entrys.add(new Entry<String, String>("fq_s", "system_process_name_s:"+show_name+"&metricset_name_s:process"));
		entrys.add(new Entry<String, Long>("start_timestamp_l", this.process_timestamp));
		entrys.add(new Entry<String, Long>("interval_l", 60000L));
		entrys.add(new Entry<String, String>("stats_field_s", field));
		entrys.add(new Entry<String, String>("stats_type_s", "mean"));
		entrys.add(new Entry<String, Long>("timestamp_l", Calendar.getInstance().getTimeInMillis()));
		try {
			sw.write(entrys);
			sw.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(e.getMessage());
		}
	}
}
