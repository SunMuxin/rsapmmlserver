package com.realsight.westworld.server.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.realsight.westworld.server.context.ContextIterator;
import com.realsight.westworld.tsp.lib.series.DoubleSeries;
import com.realsight.westworld.tsp.lib.series.MultipleDoubleSeries;
import com.realsight.westworld.tsp.lib.series.TimeSeries;
import com.realsight.westworld.tsp.lib.util.Entry;
import com.realsight.westworld.tsp.lib.util.Util;

public class BNModel extends Thread{
	
	private Map<String, DoubleSeries> data = null;
	private Long train_num = null;
	private Long start_timestamp = null;
	private Long update_frequency = null;
	private Long interval = null;
	private String show_name = null;
	private String bn_name = null;
	private Map<String, ContextIterator> CIM = new HashMap<String, ContextIterator>();
	private boolean stopflag = true;
	
	public BNModel(String bn_name,
			Long train_num,
			Long start_timestamp,
			Long update_frequency,
			Long interval,
			String show_name) {
		this.bn_name = bn_name;
		this.train_num = train_num;
		this.start_timestamp = start_timestamp;
		this.update_frequency = update_frequency;
		this.interval = interval;
		this.show_name = show_name;
		this.data = new HashMap<String, DoubleSeries>();
	}
	
	public synchronized void addContextIterator(String SOLR_URL, 
			Long timestamp,
			String solr_reader_url,
			String solr_writer_url,
			String fq,
			String stats_field,
			String stats_type,
			String stats_facet,
			String bn_id) {
		if (this.CIM.containsKey(bn_id)) return ;
		CIM.put(bn_id, new ContextIterator (SOLR_URL, 
				timestamp, 
				bn_name, 
				solr_reader_url, 
				solr_writer_url, 
				fq, 
				start_timestamp, 
				interval, 
				stats_field, 
				stats_type, 
				stats_facet, 
				bn_id, 
				show_name,
				""));
	}
	
	private synchronized boolean runOneStep() {
		for (Map.Entry<String, ContextIterator> entry : this.CIM.entrySet()) {
			ContextIterator value = entry.getValue();
			while(value.hasNext() && value.getStart_timestamp()<this.start_timestamp+this.update_frequency) {
				Entry<List<Entry<String, Double>>, Long> nEntrys = value.next();
				for (Entry<String, Double> nEntry : nEntrys.getFirst()) {
					DoubleSeries ds = data.getOrDefault(nEntry.getFirst(), new DoubleSeries(nEntry.getFirst()));
					ds.add(new TimeSeries.Entry<Double>(nEntry.getSecond(), nEntrys.getSecond()));
					if (ds.size() > 2 * this.train_num) {
						ds.pop(this.train_num.intValue());
					}
					data.put(nEntry.getFirst(), ds);
					System.err.println(data.size() + " " + ds.size());
				}
			}
			if (!value.hasNext())
				return false;
		}
		return true;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(!this.stopflag) {
			if (this.runOneStep()) {
				List<DoubleSeries> series = new ArrayList<DoubleSeries>();
				for (Map.Entry<String, DoubleSeries> entry : data.entrySet()) {
					if (entry.getValue().size() < this.train_num) continue;
					series.add(entry.getValue());
				}
				Path root = Paths.get(System.getProperty("user.dir"));
				MultipleDoubleSeries mSeries = new MultipleDoubleSeries(this.bn_name, series);
				Util.writeCsv(mSeries, root);
				this.start_timestamp += this.update_frequency;
			} else {
				try {
					Thread.sleep(this.interval/2);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	public synchronized void status(boolean stopflag) {
		this.stopflag = stopflag;
		if (stopflag) {
			return ;
		}
		if (this.isAlive())
			return ;
		this.start();
	}
}
