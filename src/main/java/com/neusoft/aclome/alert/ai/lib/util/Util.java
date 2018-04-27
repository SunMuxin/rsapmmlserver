package com.neusoft.aclome.alert.ai.lib.util;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import com.google.gson.Gson;

public class Util {
	
	private static final String charset = "utf-8"; 
	
	private static final boolean screen = true;
	private static final Path root = Paths.get(System.getProperty("user.dir")).getParent();
	private static final Path log_path = Paths.get(root.toString(), "logs", "alertai.out");
	private static final SimpleDateFormat date_formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private static final int intervalInSeconds = 1;
	
	public static void info(String name, String message) {
		String content = String.format("%s [info] %s - %s", 
				date_formatter.format(new Date()), 
				name, 
				message);
		if (screen) {
			System.out.println(content);
		} else {
			try {
				log_path.getParent().toFile().mkdirs();
				FileWriter fw = new FileWriter(log_path.toFile(), true);
				PrintWriter pw = new PrintWriter(fw);
				pw.println(content);
				pw.flush();
				pw.close();
				fw.close();
	        } catch (IOException e) {
	            throw new RuntimeException(e);
	        }
		}
	}
	
	public static void error(String name, String message) {
		// TODO Auto-generated method stub
		String content = String.format("%s [error] %s - %s", 
				date_formatter.format(new Date()), 
				name, 
				message);
		if (screen) {
			System.err.println(content);
		} else {
			try {
				log_path.getParent().toFile().mkdirs();
				FileWriter fw = new FileWriter(log_path.toFile(), true);
				PrintWriter pw = new PrintWriter(fw);
				pw.println(content);
				pw.flush();
				pw.close();
				fw.close();
	        } catch (IOException e) {
	            throw new RuntimeException(e);
	        }
		}
	}
	
	public static Scheduler creatJob(String group, String jobName, String triggerName, 
			Class<? extends Job> jobClass, JobDataMap newJobDataMap) throws SchedulerException {
		
		Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
		
		Trigger trigger = newTrigger()
				.withIdentity(triggerName, group)
				.startNow()
				.withSchedule(
						simpleSchedule().withIntervalInSeconds(
								intervalInSeconds)
						.repeatForever())
				.build();
		
		JobDetail job = newJob(jobClass)
				.withIdentity(jobName, group)
				.setJobData(newJobDataMap)
				.build();
		
		scheduler.scheduleJob(job, trigger);
		
		return scheduler;
	}
	
    public static void writeStringToTempFile(String content) {
        try {
            writeString(content, Paths.get(File.createTempFile("out-", ".csv").getAbsolutePath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void writeString(String content, Path path) {
    	writeString(content, path, false);
    }
    
    public static void writeString(String content, Path path, boolean append) {
        try {
        	path.getParent().toFile().mkdirs();
			FileWriter fw = new FileWriter(path.toFile(), append);
			PrintWriter pw = new PrintWriter(fw);
			pw.println(content);
			pw.flush();
			pw.close();
			fw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void check(boolean condition) {
        if (!condition) {
            throw new RuntimeException();
        }
    }

    public static void check(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException(message);
        }
    }
	
	public static double Jaccard(List<Integer> seta, List<Integer> setb) {
		if (seta.isEmpty() && setb.isEmpty())
			return 1.0;
		Collections.sort(seta);
		Collections.sort(setb);
		Iterator<Integer> ita = seta.iterator();
		Iterator<Integer> itb = setb.iterator();
		double cnt = 0;
		while(ita.hasNext() && itb.hasNext()) {
			Integer a = ita.next();
			Integer b = itb.next();
			while(a.equals(b)){
				if (a.compareTo(b)<0 && ita.hasNext()){
					a = ita.next();
				} else break;
				if (b.compareTo(a)<0 && itb.hasNext()){
					b = itb.next();
				} else break;
			}
			cnt += (a.equals(b))?1.0:0.0;
		}
		double asize = seta.size();
		double bsize = setb.size();
		return cnt/(asize+bsize-cnt);
    }
	
	public static String HttpGET(String url) throws ClientProtocolException, IOException {
		CloseableHttpClient client =  HttpClientBuilder.create().build();
		HttpGet get = new HttpGet(url);
		CloseableHttpResponse resp = client.execute(get);
		
		if (resp.getStatusLine().getStatusCode() != 200)
			return null;
		
		HttpEntity entity = resp.getEntity();
		Scanner sin = new Scanner(EntityUtils.toString(entity));
		entity.getContent();
		
		StringBuffer res = new StringBuffer();
		while(sin.hasNext()) {
			res.append(sin.nextLine());
		}
		
		sin.close();
		resp.close();
		client.close();
		
		return res.toString();
	}
	
	public static String HttpPOST(String url, Map<String, Object> params) throws ClientProtocolException, IOException {
		return HttpPOST(url, params, charset);
	}
	
	public static String HttpPOST(String url, Map<String, Object> params, String charset) throws ClientProtocolException, IOException {
		CloseableHttpClient client =  HttpClientBuilder.create().build();
		HttpPost post = new HttpPost(url);
        StringEntity se = new StringEntity(new Gson().toJson(params), charset);
        se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        post.setEntity(se);
        CloseableHttpResponse resp = client.execute(post);	
		if (resp.getStatusLine().getStatusCode() != 200)
			return null;
		
		HttpEntity entity = resp.getEntity();
		Scanner sin = new Scanner(EntityUtils.toString(entity));
		entity.getContent();
		
		StringBuffer res = new StringBuffer();
		while(sin.hasNext()) {
			res.append(sin.nextLine());
		}
		
		sin.close();
		resp.close();
		client.close();
		
		return res.toString();
	}
}

