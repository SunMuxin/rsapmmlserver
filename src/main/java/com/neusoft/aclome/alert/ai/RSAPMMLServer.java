package com.neusoft.aclome.alert.ai;

import org.slf4j.LoggerFactory;

import com.neusoft.aclome.alert.ai.application.ADApplication;
import com.neusoft.aclome.alert.ai.application.BMWReportApplication;
import com.neusoft.aclome.alert.ai.application.ThreadDiagnoseApplication;
import com.neusoft.aclome.alert.ai.application.UpdateCONFIGApplication;
import com.neusoft.aclome.alert.ai.lib.util.CONFIG;
import com.neusoft.aclome.alert.ai.lib.util.Util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class RSAPMMLServer {

	private static Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
	static {
		logger.setLevel(Level.WARN);
	}
	
	private final ADApplication ada;
	private final ThreadDiagnoseApplication tda;
	private final BMWReportApplication bmwra;
	
	private RSAPMMLServer() {
		UpdateCONFIGApplication.update();
		new UpdateCONFIGApplication().status(false);
		this.ada = new ADApplication(CONFIG.OPTION_SOLR_URL, CONFIG.TIME_FIELD);
		this.tda = new ThreadDiagnoseApplication(CONFIG.OPTION_SOLR_URL, CONFIG.TIME_FIELD);
		this.bmwra = new BMWReportApplication(CONFIG.OPTION_SOLR_URL, CONFIG.TIME_FIELD);
	}
	
	public static RSAPMMLServer getInstance() {
		return Singleton.INSTANVE.getInstance();
	}
	
	private static enum Singleton {
		INSTANVE;
		
		private RSAPMMLServer singleton;
		
		private Singleton() {
			this.singleton = new RSAPMMLServer();
		}
		
		public RSAPMMLServer getInstance() {
			return this.singleton;
		}
	}
	
	public void run() {
		if (CONFIG.AD_APP) {
			ada.status(false);
			Util.info("RSAPMMLServer.run()", "start up ADApplication.");
		} else {
			ada.status(true);
			Util.info("RSAPMMLServer.run()", "stop ADApplication.");
		}
		
		if (CONFIG.THREAD_DIAGNOSE_APP) {
			tda.status(false);
			Util.info("RSAPMMLServer.run()", "start up JmThreadADApplication.");
		} else {
			tda.status(true);
			Util.info("RSAPMMLServer.run()", "stop JmThreadADApplication.");
		}
		
		if (CONFIG.BMW_REPORT_APP) {
			bmwra.status(false);
			Util.info("RSAPMMLServer.run()", "start up BMWReportApplication.");
		} else {
			bmwra.status(true);
			Util.info("RSAPMMLServer.run()", "stop BMWReportApplication.");
		}
	}
	
	public static void main(String[] args) throws Exception {
		RSAPMMLServer.getInstance().run();
	}
}