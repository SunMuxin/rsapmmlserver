package com.realsight.westworld.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import org.slf4j.LoggerFactory;

import com.realsight.westworld.server.application.ADApplication;
import com.realsight.westworld.server.application.ExampleApplication;
import com.realsight.westworld.server.application.NETOptionApplication;
import com.realsight.westworld.server.application.MetricOptionApplication;
import com.realsight.westworld.server.application.ProcessOptionApplication;
import com.realsight.westworld.tsp.lib.series.DoubleSeries;
import com.realsight.westworld.tsp.lib.solr.SolrDelete;
import com.realsight.westworld.tsp.lib.util.data.TimeseriesDataWithoutTimestamp;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class RSAPMMLServers {
	private String OPTION_SOLR_URL = null;
	private String METRIC_SOLR_URL = null;
	private String NAPM_SOLR_URL = null;
	private String RESULT_SOLR_URL = null;
	private String time_field = "rs_timestamp";
	private String process_names = null;
	private List<DoubleSeries> lseries = null;
	private static Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
	static {
		logger.setLevel(Level.WARN);
	}
	
	public RSAPMMLServers() {
		initialize();
		new ExampleApplication(lseries, RESULT_SOLR_URL);
		ADApplication ada = new ADApplication(OPTION_SOLR_URL, time_field);
		MetricOptionApplication moa = new MetricOptionApplication(
				OPTION_SOLR_URL,
				METRIC_SOLR_URL,
				RESULT_SOLR_URL,
				time_field,
				Calendar.getInstance().getTimeInMillis());
		new ProcessOptionApplication(
				OPTION_SOLR_URL,
				NAPM_SOLR_URL,
				RESULT_SOLR_URL,
				process_names,
				Calendar.getInstance().getTimeInMillis());
		new NETOptionApplication(
				OPTION_SOLR_URL,
				NAPM_SOLR_URL,
				RESULT_SOLR_URL,
				Calendar.getInstance().getTimeInMillis());
		while(true) {
			moa.status(false);
			ada.status(false);
			try {
				Thread.sleep(1000L * 6);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void initialize() {
		Path root = Paths.get(System.getProperty("user.dir")).getParent();
		Path propertyPath = Paths.get(root.toString(), 
				"config", 
				"rsapmml.properties");
        Properties property = new Properties();
        try {
			property.load(new FileInputStream(propertyPath.toFile()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        if (property.containsKey("option_solr_url")){
        	this.OPTION_SOLR_URL = property.getProperty("option_solr_url");
            SolrDelete sd = new SolrDelete(this.OPTION_SOLR_URL);
            sd.delete_all();
            try {
				sd.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				logger.error(e.getMessage());
			}
        }
        if (property.containsKey("mertic_solr_url")){
        	this.METRIC_SOLR_URL = property.getProperty("mertic_solr_url");
        }
        if (property.containsKey("napm_solr_url")){
        	this.NAPM_SOLR_URL = property.getProperty("napm_solr_url");
        }
        if (property.containsKey("result_solr_url")){
        	this.RESULT_SOLR_URL = property.getProperty("result_solr_url");
        }
        if (property.containsKey("time_field")){
        	this.time_field = property.getProperty("time_field");
        }
        if (property.containsKey("process_names")) {
        	this.process_names = property.getProperty("process_names");
        }
        Path examplePath = Paths.get(root.toString(), 
				"config", 
				"example_metric");
        this.lseries = new ArrayList<DoubleSeries>();
        for (File metricfile : examplePath.toFile().listFiles()) {
        	if (metricfile.isDirectory()) continue;
        	TimeseriesDataWithoutTimestamp tdwt = new TimeseriesDataWithoutTimestamp(metricfile.getAbsolutePath());
        	lseries.add(tdwt.getPropertyDoubleSeries("value"));
        }
	}
	
	public static void main(String[] args) throws IOException {
//		System.out.println(System.getProperty("user.dir"));
		new RSAPMMLServers();
	}
}
