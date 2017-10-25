package com.realsight.westworld.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.LoggerFactory;

import com.realsight.westworld.server.application.JmMemoryADApplication;
import com.realsight.westworld.server.application.JmSystemApplication;
import com.realsight.westworld.server.application.JmThreadADApplication;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class RSAPMMLServers {
	private String OPTION_SOLR_URL = null;
	private String time_field = "rs_timestamp";
	private static Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
	static {
		logger.setLevel(Level.WARN);
	}
	
	public RSAPMMLServers() {
		initialize();
		JmThreadADApplication jtada = new JmThreadADApplication(OPTION_SOLR_URL, time_field);
		JmMemoryADApplication jmada = new JmMemoryADApplication(OPTION_SOLR_URL, time_field);
		JmSystemApplication jsa = new JmSystemApplication(OPTION_SOLR_URL);
		while(true){
			jtada.status(false);
			jmada.status(false);
			jsa.status(false);
			try {
				Thread.sleep(1000L * 60);
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
        }
        if (property.containsKey("time_field")){
        	this.time_field = property.getProperty("time_field");
        }
	}
	
	public static void main(String[] args) throws IOException {
		new RSAPMMLServers();
	}
}