package com.neusoft.aclome.alert.ai.lib.data;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.neusoft.aclome.westworld.tsp.lib.util.JmThreadInfo;
import com.neusoft.aclome.westworld.tsp.lib.util.data.DataType;

public class JmThreadData {
	
	private String info = null;
	private Iterator<JsonObject> thread_infos = null;
	
	public JmThreadData(Path path) throws IOException {
		this(path, "GBK");
	}
	
	public JmThreadData(String str, DataType type) throws IOException {
		if (type.equals(DataType.INFO)) {
			this.info = str;
			return ;
		} else if (type.equals(DataType.URL)){
			CloseableHttpClient client =  HttpClientBuilder.create().build();
			HttpGet get = new HttpGet(str);
			CloseableHttpResponse resp = client.execute(get);
			if (resp.getStatusLine().getStatusCode() != 200)
				throw new IOException(resp.getStatusLine().getReasonPhrase());
			HttpEntity entity = resp.getEntity();
			Scanner sin = new Scanner(EntityUtils.toString(entity));
			entity.getContent();
			StringBuffer info_buffer = new StringBuffer();
			while(sin.hasNext()) {
				info_buffer.append(sin.nextLine());
			}
			this.info = info_buffer.toString();
			sin.close();
			resp.close();
			client.close();
		}
	}
	
	public JmThreadData(Path path, String encoding) throws IOException{
		InputStreamReader read = new InputStreamReader(
				new FileInputStream(path.toFile()), encoding);
        BufferedReader bufferedReader = new BufferedReader(read);
        StringBuffer info_buffer = new StringBuffer();
        String temp = null;
        do {
        	temp = bufferedReader.readLine();
        	info_buffer.append(temp);
        } while(temp != null);
        this.info = info_buffer.toString();
        bufferedReader.close();
	}
	
	public boolean hasNextThreadInfo() {
		if (this.thread_infos == null) {
			if (this.info == null)
				return false;
			try {
				JSONObject info_json = new JSONObject(this.info);
				Type type = new TypeToken<ArrayList<JsonObject>>(){}.getType();
		        ArrayList<JsonObject> jsonObjects = new Gson().fromJson(
		        		info_json.optString("list"), 
		        		type);
				this.thread_infos = jsonObjects.iterator();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return this.thread_infos.hasNext();
	}
	
	public JmThreadInfo next() {
		if (! hasNextThreadInfo())
			return null;
		JmThreadInfo info = new Gson().fromJson(this.thread_infos.next(),
				JmThreadInfo.class);
		return new JmThreadInfo(info.getName(), 
				info.getId(), 
				info.getState(),
				info.getCpuTimeMillis(),
				info.getUserTimeMillis(),
				info.getDeadlocked(),
				info.getLockInfo());
	}

	public void reset() {
		// TODO Auto-generated method stub
		try {
			JSONObject info_json = new JSONObject(this.info);
			Type type = new TypeToken<ArrayList<JsonObject>>(){}.getType();
	        ArrayList<JsonObject> jsonObjects = new Gson().fromJson(
	        		info_json.optString("list"), 
	        		type);
			this.thread_infos = jsonObjects.iterator();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
