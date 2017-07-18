package com.realsight.westworld.server.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import org.apache.solr.client.solrj.SolrServerException;

import com.realsight.westworld.tsp.lib.solr.SolrWriter;
import com.realsight.westworld.tsp.lib.util.Entry;
import com.realsight.westworld.tsp.lib.util.TimeUtil;

public class TEMPOptionApplication extends Thread{
	private static String solr_url = "http://10.0.67.21:8080/solr/nifi/";
	private static Random rng = new Random();
	private static SolrWriter sw = new SolrWriter(solr_url);
	
	public static void insert(String res_id, String show_name, Long timestamp) {
		
		List<Entry<String, ?>> entrys = new ArrayList<Entry<String,?>>();
		entrys.add(new Entry<String, String>("res_id", res_id));
		entrys.add(new Entry<String, String>("res_name", show_name));
		entrys.add(new Entry<String, String>("one_level_type", "basic_info"));
		entrys.add(new Entry<String, Double>("a_f", rng.nextDouble()));
		entrys.add(new Entry<String, Double>("b_f", rng.nextDouble()));
		entrys.add(new Entry<String, Double>("c_f", rng.nextDouble()));
		entrys.add(new Entry<String, Double>("d_f", rng.nextDouble()));
		entrys.add(new Entry<String, Double>("e_f", rng.nextDouble()));
		entrys.add(new Entry<String, Double>("f_f", rng.nextDouble()));
		entrys.add(new Entry<String, Double>("g_f", rng.nextDouble()));
		entrys.add(new Entry<String, Long>("timestamp_l", timestamp));
		entrys.add(new Entry<String, String>("rs_timestamp", TimeUtil.formatUnixtime2(timestamp)));
		try {
			sw.write(entrys);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		for ( int i = 1; i < 10; i++) {
			String name = String.valueOf(rng.nextLong());
			for(Long timestamp = 1498056600017L; timestamp < Calendar.getInstance().getTimeInMillis(); timestamp += 1000L * 60) {
				insert(name, "VAPM" + i, timestamp);
			}
		}
		try {
			sw.close();
		} catch (IOException | SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
