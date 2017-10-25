package com.realsight.westworld.server.model;

import org.slf4j.LoggerFactory;

import com.realsight.westworld.server.context.JmSystemInfoToSolr;

import ch.qos.logback.classic.Logger;

public class JmSystemModel extends Thread{
	private volatile boolean stopflag = true;
	private static Logger logger = (Logger) LoggerFactory.getLogger(JmSystemModel.class);
	private final String JMSystem_URL;
	private final long interval;
	private final String jm_name;
	private final String show_name;
	private final String solr_writer_url;
	
	public JmSystemModel(String JMSystem_URL,
			String jm_name,
			String show_name,
			String solr_writer_url,
			long interval) {
		this.JMSystem_URL = JMSystem_URL;
		this.interval = interval;
		this.jm_name = jm_name;
		this.show_name = show_name;
		this.solr_writer_url = solr_writer_url;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(!this.stopflag) {
			new Thread(new JmSystemInfoToSolr(JMSystem_URL,
					this.jm_name,
					this.show_name,
					this.solr_writer_url)).start();
			try {
				Thread.sleep(this.interval);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				logger.error(e.getMessage());
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
