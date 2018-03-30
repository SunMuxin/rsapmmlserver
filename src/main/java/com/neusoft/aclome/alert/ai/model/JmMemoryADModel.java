package com.neusoft.aclome.alert.ai.model;

import java.io.IOException;
import java.util.List;
import org.slf4j.LoggerFactory;

import com.neusoft.aclome.alert.ai.lib.data.SolrContextData;
import com.neusoft.aclome.alert.ai.lib.sample.JmGCInfo;
import com.neusoft.aclome.alert.ai.lib.tosolr.JmMemoryInfoToSolr;
import com.neusoft.aclome.westworld.tsp.api.OnlineAnomalyDetectionAPI;
import com.neusoft.aclome.westworld.tsp.lib.series.TimeSeries;
import com.neusoft.aclome.westworld.tsp.lib.util.Entry;
import com.neusoft.aclome.westworld.tsp.lib.util.TimeUtil;

import ch.qos.logback.classic.Logger;

public class JmMemoryADModel extends Thread{
	private final OnlineAnomalyDetectionAPI oad;
	private volatile boolean stopflag = true;
	private SolrContextData context = null;
	private static Logger logger = (Logger) LoggerFactory.getLogger(JmMemoryADModel.class);
	private final String jm_threadsmemory_url;
	private final String jm_system_url;
	private final String jm_memory_url;
	private final String jm_gcinfo_url;
	private JmGCInfo jgci = null;
	private static final Double th = 0.75;
	private Double pre_facet_value = 0.0;
	private Thread thread = null;
	
	public JmMemoryADModel(String jm_threadsmemory_url,
			String jm_system_url,
			String jm_memory_url,
			String jm_gcinfo_url,
			String SOLR_URL, 
			Long timestamp,
			String res_id,
			String solr_reader_url,
			String solr_writer_url,
			String fq,
			Long start_timestamp,
			Long interval,
			String stats_field,
			String stats_type,
			String ad_id,
			String name,
			Double max_value,
			Double min_value) throws Exception {
		this.context = new SolrContextData(null, fq.toString());

		if (max_value!=null && min_value!=null) {
			this.oad = new OnlineAnomalyDetectionAPI(min_value, max_value);
		} else {
			throw new Exception("");
		}
		this.jm_threadsmemory_url = jm_threadsmemory_url;
		this.jm_system_url = jm_system_url;
		this.jm_memory_url = jm_memory_url;
		this.jm_gcinfo_url = jm_gcinfo_url;
	}
	
	
		
	private void update(boolean to_solr_flag) throws IOException {
		if (this.context.hasNext()==false) return ;
		Entry<List<Entry<String, Double>>, Long> entrys = context.nextFieldStats();
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
					new Thread(new JmMemoryInfoToSolr(context,
							jm_threadsmemory_url,
							jm_system_url,
							jm_memory_url,
							jgci.getExeTime(),
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
		jgci = new JmGCInfo(jm_gcinfo_url);
		boolean to_solr_flag = false;
		while(!this.stopflag) {
			if (this.context.hasNext()) {
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
					Thread.sleep(1000);
					to_solr_flag = true;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					logger.error(e.getMessage());
				}
			}
		}
		jgci.stop();
	}
	
	public synchronized void status(boolean stopflag) {
		this.stopflag = stopflag;
		if (stopflag) {
			return ;
		} else {
			if (thread==null || !thread.isAlive()){
				thread = new Thread(this);
				thread.start();
			}
		}
	}
}
