package com.realsight.westworld.server.model;

import java.io.IOException;
import java.util.List;
import org.slf4j.LoggerFactory;

import com.realsight.westworld.server.context.ContextIterator;
import com.realsight.westworld.server.context.JmMemoryInfoToSolr;
import com.realsight.westworld.tsp.api.OnlineAnomalyDetectionAPI;
import com.realsight.westworld.tsp.lib.series.TimeSeries;
import com.realsight.westworld.tsp.lib.util.Entry;
import com.realsight.westworld.tsp.lib.util.TimeUtil;

import ch.qos.logback.classic.Logger;

public class JmMemoryADModel extends Thread{
	private final OnlineAnomalyDetectionAPI oad;
	private volatile boolean stopflag = true;
	private ContextIterator iterator = null;
	private static Logger logger = (Logger) LoggerFactory.getLogger(JmMemoryADModel.class);
	private final String JMMemory_URL;
	private static final Double th = 0.75;
	private Double pre_facet_value = 0.0;
	
	public JmMemoryADModel(String JMMemory_URL,
			String SOLR_URL, 
			Long timestamp,
			String ad_name,
			String solr_reader_url,
			String solr_writer_url,
			String fq,
			Long start_timestamp,
			Long interval,
			String stats_field,
			String stats_type,
			String ad_id,
			String show_name,
			String time_field,
			Double max_value,
			Double min_value) {
		this.iterator = new ContextIterator(SOLR_URL, 
				timestamp, 
				ad_name, 
				solr_reader_url, 
				solr_writer_url, 
				fq, 
				start_timestamp, 
				interval, 
				stats_field, 
				stats_type, 
				null, 
				ad_id, 
				show_name,
				time_field);
		if (max_value!=null && min_value!=null) {
			this.oad = new OnlineAnomalyDetectionAPI(min_value, max_value);
		} else {
			this.oad = new OnlineAnomalyDetectionAPI();
		}
		this.JMMemory_URL = JMMemory_URL;
	}
	
	
		
	private void update(boolean to_solr_flag) throws IOException {
		if (this.iterator.hasNext()==false) return ;
		Entry<List<Entry<String, Double>>, Long> entrys = iterator.next();
		if (entrys!=null){
			if (entrys.getFirst().size() != 1) 
				throw new IOException("facet_name value number error.");
			for (Entry<String, Double> entry : entrys.getFirst()) {
				try {
					Double facet_value = entry.getSecond();
					String facet_name = entry.getFirst();
					logger.info(facet_name + " " + facet_value);
					TimeSeries.Entry<Double> res = oad.detection(facet_value, entrys.getSecond());
					System.out.println("jm_memory " + res + " " + facet_value + " " + TimeUtil.formatUnixtime2(entrys.getSecond()));
					if (res == null) continue;
					if (!Double.isNaN(facet_value)) {
						pre_facet_value = facet_value;
					}
					new Thread(new JmMemoryInfoToSolr(iterator,
							JMMemory_URL,
							pre_facet_value,
							entrys.getSecond(),
							(res.getItem()>th)&&to_solr_flag), "to solr").start();
				} catch(ClassCastException e) {
					e.printStackTrace();
					logger.error(e.getMessage());
				}
			}
		}
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		boolean to_solr_flag = false;
		while(!this.stopflag) {
			if (this.iterator.hasNext()) {
				try {
					update(to_solr_flag);
					to_solr_flag = false;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					logger.error(e.getMessage());
				}
			} else {
				try {
					Thread.sleep(this.iterator.getInterval()/2);
					to_solr_flag = true;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					logger.error(e.getMessage());
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
