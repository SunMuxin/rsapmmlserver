package com.realsight.westworld.server.application;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.realsight.westworld.tsp.lib.solr.SolrWriter;
import com.realsight.westworld.tsp.lib.util.Entry;

import ch.qos.logback.classic.Logger;

public class NETOptionApplication extends Thread{
	private String RESULT_SOLR_URL = null;
	private String NAPM_SOLR_URL = null;
	private String OPTION_SOLR_URL = null;
	private Long net_timestamp = null;
	private static Logger logger = (Logger) LoggerFactory.getLogger(NETOptionApplication.class);
	
	public NETOptionApplication(
			String OPTION_SOLR_URL, 
			String NAPM_SOLR_URL,
			String RESULT_SOLR_URL,
			Long timestamp) {
		this.OPTION_SOLR_URL = OPTION_SOLR_URL;
		this.NAPM_SOLR_URL = NAPM_SOLR_URL;
		this.RESULT_SOLR_URL = RESULT_SOLR_URL;
		this.net_timestamp = timestamp;
		this.initHTTP();
		this.initPGSQL();
		this.initDNS();
		this.initICMP();
		this.initMYSQL();
	}

	public void initHTTP() {
		if (NAPM_SOLR_URL == null) return ;
		if (RESULT_SOLR_URL == null) return ;
		SolrWriter sw = new SolrWriter(OPTION_SOLR_URL);
		List<Entry<String, ?>> entrys = new ArrayList<Entry<String,?>>();
		entrys.add(new Entry<String, String>("option_s", "ad"));
		entrys.add(new Entry<String, String>("ad_name_s", "net"));
		entrys.add(new Entry<String, String>("show_name_s", "http"));
		entrys.add(new Entry<String, String>("solr_reader_url_s", NAPM_SOLR_URL));
		entrys.add(new Entry<String, String>("solr_writer_url_s", RESULT_SOLR_URL));
		entrys.add(new Entry<String, String>("fq_s", "type_s:http"));
		entrys.add(new Entry<String, Long>("start_timestamp_l", this.net_timestamp));
		entrys.add(new Entry<String, Long>("interval_l", 300000L));
		entrys.add(new Entry<String, String>("stats_field_s", "responsetime_f"));
		entrys.add(new Entry<String, String>("stats_type_s", "count"));
		entrys.add(new Entry<String, String>("stats_facet_s", "query_s"));
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
	
	public void initPGSQL() {
		if (NAPM_SOLR_URL == null) return ;
		if (RESULT_SOLR_URL == null) return ;
		SolrWriter sw = new SolrWriter(OPTION_SOLR_URL);
		List<Entry<String, ?>> entrys = new ArrayList<Entry<String,?>>();
		entrys.add(new Entry<String, String>("option_s", "ad"));
		entrys.add(new Entry<String, String>("ad_name_s", "net"));
		entrys.add(new Entry<String, String>("show_name_s", "pgsql"));
		entrys.add(new Entry<String, String>("solr_reader_url_s", NAPM_SOLR_URL));
		entrys.add(new Entry<String, String>("solr_writer_url_s", RESULT_SOLR_URL));
		entrys.add(new Entry<String, String>("fq_s", "type_s:pgsql"));
		entrys.add(new Entry<String, Long>("start_timestamp_l", this.net_timestamp));
		entrys.add(new Entry<String, Long>("interval_l", 300000L));
		entrys.add(new Entry<String, String>("stats_field_s", "responsetime_f"));
		entrys.add(new Entry<String, String>("stats_type_s", "count"));
		entrys.add(new Entry<String, String>("stats_facet_s", "query_s"));
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
	
	public void initICMP() {
		if (NAPM_SOLR_URL == null) return ;
		if (RESULT_SOLR_URL == null) return ;
		SolrWriter sw = new SolrWriter(OPTION_SOLR_URL);
		List<Entry<String, ?>> entrys = new ArrayList<Entry<String,?>>();
		entrys.add(new Entry<String, String>("option_s", "ad"));
		entrys.add(new Entry<String, String>("ad_name_s", "net"));
		entrys.add(new Entry<String, String>("show_name_s", "icmp"));
		entrys.add(new Entry<String, String>("solr_reader_url_s", NAPM_SOLR_URL));
		entrys.add(new Entry<String, String>("solr_writer_url_s", RESULT_SOLR_URL));
		entrys.add(new Entry<String, String>("fq_s", "type_s:icmp"));
		entrys.add(new Entry<String, Long>("start_timestamp_l", this.net_timestamp));
		entrys.add(new Entry<String, Long>("interval_l", 300000L));
		entrys.add(new Entry<String, String>("stats_field_s", "responsetime_f"));
		entrys.add(new Entry<String, String>("stats_type_s", "count"));
		entrys.add(new Entry<String, String>("stats_facet_s", "query_s"));
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
	
	public void initDNS() {
		if (NAPM_SOLR_URL == null) return ;
		if (RESULT_SOLR_URL == null) return ;
		SolrWriter sw = new SolrWriter(OPTION_SOLR_URL);
		List<Entry<String, ?>> entrys = new ArrayList<Entry<String,?>>();
		entrys.add(new Entry<String, String>("option_s", "ad"));
		entrys.add(new Entry<String, String>("ad_name_s", "net"));
		entrys.add(new Entry<String, String>("show_name_s", "dns"));
		entrys.add(new Entry<String, String>("solr_reader_url_s", NAPM_SOLR_URL));
		entrys.add(new Entry<String, String>("solr_writer_url_s", RESULT_SOLR_URL));
		entrys.add(new Entry<String, String>("fq_s", "type_s:dns"));
		entrys.add(new Entry<String, Long>("start_timestamp_l", this.net_timestamp));
		entrys.add(new Entry<String, Long>("interval_l", 300000L));
		entrys.add(new Entry<String, String>("stats_field_s", "responsetime_f"));
		entrys.add(new Entry<String, String>("stats_type_s", "count"));
		entrys.add(new Entry<String, String>("stats_facet_s", "query_s"));
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
	
	public void initMYSQL() {
		if (NAPM_SOLR_URL == null) return ;
		if (RESULT_SOLR_URL == null) return ;
		SolrWriter sw = new SolrWriter(OPTION_SOLR_URL);
		List<Entry<String, ?>> entrys = new ArrayList<Entry<String,?>>();
		entrys.add(new Entry<String, String>("option_s", "ad"));
		entrys.add(new Entry<String, String>("ad_name_s", "net"));
		entrys.add(new Entry<String, String>("show_name_s", "mysql"));
		entrys.add(new Entry<String, String>("solr_reader_url_s", NAPM_SOLR_URL));
		entrys.add(new Entry<String, String>("solr_writer_url_s", RESULT_SOLR_URL));
		entrys.add(new Entry<String, String>("fq_s", "type_s:mysql"));
		entrys.add(new Entry<String, Long>("start_timestamp_l", this.net_timestamp));
		entrys.add(new Entry<String, Long>("interval_l", 300000L));
		entrys.add(new Entry<String, String>("stats_field_s", "responsetime_f"));
		entrys.add(new Entry<String, String>("stats_type_s", "count"));
		entrys.add(new Entry<String, String>("stats_facet_s", "query_s"));
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
