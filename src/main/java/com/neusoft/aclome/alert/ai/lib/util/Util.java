package com.neusoft.aclome.alert.ai.lib.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

import com.google.gson.Gson;
import com.neusoft.aclome.westworld.tsp.lib.solr.SolrWriter;
import com.neusoft.aclome.westworld.tsp.lib.util.Entry;

public class Util {
	
	private static final String charset = "utf-8"; 
	
	private static final String root = System.getProperty("user.dir");
	private static final Path log_path = Paths.get(root, "logs", "alertai.out");
	private static final SimpleDateFormat date_formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static void addJMThreadADOption(
			String option_url,
			String res_id,
			String name,
			String solr_reader_url,
			String solr_writer_url,
			String jm_thread_url,
			long interval ) {
		
		SolrWriter sw = new SolrWriter(option_url);
		List<Entry<String, ?>> entrys = new ArrayList<Entry<String, ?>>();
		entrys.add(new Entry<String, String>("option", "jm_thread_ad"));
		entrys.add(new Entry<String, String>("res_id", res_id));
		entrys.add(new Entry<String, String>("name", name));
		entrys.add(new Entry<String, String>("solr_reader_url", solr_reader_url));
		entrys.add(new Entry<String, String>("solr_writer_url", solr_writer_url));
		entrys.add(new Entry<String, String>("jm_thread_url", jm_thread_url));
		entrys.add(new Entry<String, String>("fq", String.format("res_id:%s&res_ip:*&one_level_type:basic_info&JAVAEE_CPU_used:*", res_id)));
		entrys.add(new Entry<String, Long>("start_timestamp", System.currentTimeMillis()));
		entrys.add(new Entry<String, Long>("interval", interval));
		entrys.add(new Entry<String, String>("stats_field", "JAVAEE_CPU_used"));
		entrys.add(new Entry<String, String>("stats_type", "mean"));
		entrys.add(new Entry<String, String>("rs_timestamp", TimeUtil.formatUnixtime2(System.currentTimeMillis())));
		entrys.add(new Entry<String, Double>("max", 100.0));
		entrys.add(new Entry<String, Double>("min", 0.0));
		Util.info("addJMThreadADOption", entrys.toString());
		try {
			sw.write(entrys);
			sw.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void info(String name, String message) {
		String content = String.format("%s [info] %s - %s", 
				date_formatter.format(new Date()), 
				name, 
				message);
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
	
	public static void error(String name, String message) {
		// TODO Auto-generated method stub
		String content = String.format("%s [error] %s - %s", 
				date_formatter.format(new Date()), 
				name, 
				message);
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

