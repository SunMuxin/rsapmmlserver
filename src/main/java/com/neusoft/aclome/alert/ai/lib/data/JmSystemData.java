package com.neusoft.aclome.alert.ai.lib.data;

import java.io.IOException;
import java.util.Scanner;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JmSystemData {
	
	private final JsonObject info;
	
	public JmSystemData(String url) throws IOException {
		CloseableHttpClient client =  HttpClientBuilder.create().build();
		HttpGet get = new HttpGet(url);
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
		JsonObject json = new JsonParser().parse(info_buffer.toString()).getAsJsonObject();
		this.info = json.get("list").getAsJsonArray().get(0).getAsJsonObject();
		sin.close();
		resp.close();
		client.close();
	}
	
	public JsonObject getInfo() {
		return info;
	}
	
	public String getProperty(String key) {
		if (!info.has(key)) return null;
		return info.get(key).getAsString();
	}
	
	public void addProperty(String key, int value) {
		info.addProperty(key, value);
	}
	
}
