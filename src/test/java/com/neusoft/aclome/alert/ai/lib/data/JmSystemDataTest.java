package com.neusoft.aclome.alert.ai.lib.data;

import java.io.IOException;

import org.junit.Test;

public class JmSystemDataTest {

	private static final String URL = "http://10.0.67.21:8080/apm/monitoring?part=systeminformation&format=json";
	
	@Test
	public void test() throws IOException {
		JmSystemData data = new JmSystemData(URL);
		System.out.println(data.getProperty("hostname"));
		System.out.println(data.getInfo());
	}

}
