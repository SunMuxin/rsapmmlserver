package com.neusoft.aclome.alert.ai;

import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import org.slf4j.LoggerFactory;

import com.neusoft.aclome.alert.ai.application.ADApplication;
import com.neusoft.aclome.alert.ai.application.BMWReportApplication;
import com.neusoft.aclome.alert.ai.application.ThreadDiagnoseApplication;
import com.neusoft.aclome.alert.ai.application.UpdateCONFIGApplication;
import com.neusoft.aclome.alert.ai.lib.util.CONFIG;
import com.neusoft.aclome.alert.ai.lib.util.Util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class RSAPMAIServer {

	private static Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
	static {
		logger.setLevel(Level.WARN);
	}

	private static final String update_config_group = "update_config_group";
	private static final String update_config_trigger_name = "update_config_trigger";
	private static final String update_config_name = "update_config";

	private static final String ad_group = "ad_group";
	private static final String ad_trigger_name = "ad_trigger";
	private static final String ad_name = "ad";

	private static final String bmw_group = "bmw_group";
	private static final String bmw_trigger_name = "bmw_trigger";
	private static final String bmw_name = "bmw";

	private static final String thread_diagnose_group = "thread_diagnose_group";
	private static final String thread_diagnose_trigger_name = "thread_diagnose_trigger";
	private static final String thread_diagnose_name = "thread_diagnose";

	private Scheduler update_config;
	private Scheduler ad;
	private Scheduler bmw;
	private Scheduler thread_diagnose;
	
	private RSAPMAIServer() {
		try {
			UpdateCONFIGApplication.Init();
			update_config = Util.creatJob(update_config_group, update_config_name, update_config_trigger_name,
					UpdateCONFIGApplication.class, new JobDataMap());

			if (!update_config.isShutdown()) {
				update_config.start();
			}
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			Util.error("RSAPMAIServer", e.getMessage());
		}
	}

	public static RSAPMAIServer getInstance() {
		return Singleton.INSTANVE.getInstance();
	}

	private static enum Singleton {
		INSTANVE;

		private RSAPMAIServer singleton;

		private Singleton() {
			this.singleton = new RSAPMAIServer();
		}

		public RSAPMAIServer getInstance() {
			return this.singleton;
		}
	}

	public void run() {
		try {
			if (CONFIG.AD_APP) {
				ad = Util.creatJob(ad_group, ad_name, ad_trigger_name, ADApplication.class, new JobDataMap());
				if (!ad.isShutdown()) {
					ad.start();
				}
				Util.info("RSAPMAIServer", "start up ADApplication.");
			}

			if (CONFIG.THREAD_DIAGNOSE_APP) {
				thread_diagnose = Util.creatJob(thread_diagnose_group, thread_diagnose_name,
						thread_diagnose_trigger_name, ThreadDiagnoseApplication.class, new JobDataMap());
				if (!thread_diagnose.isShutdown()) {
					thread_diagnose.start();
				}
				Util.info("RSAPMAIServer", "start up JmThreadADApplication.");
			}

			if (CONFIG.BMW_REPORT_APP) {
				bmw = Util.creatJob(bmw_group, bmw_name, bmw_trigger_name, BMWReportApplication.class, new JobDataMap());
				if (!bmw.isShutdown()) {
					bmw.start();
				}
				Util.info("RSAPMAIServer", "start up BMWReportApplication.");
			}
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			Util.error("RSAPMAIServer", e.getMessage());

		}
	}

	public static void main(String[] args) throws Exception {
		RSAPMAIServer.getInstance().run();
	}
}