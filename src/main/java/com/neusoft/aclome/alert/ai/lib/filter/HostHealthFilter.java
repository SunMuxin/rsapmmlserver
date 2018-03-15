package com.neusoft.aclome.alert.ai.lib.filter;

import com.google.gson.JsonObject;

public class HostHealthFilter implements Filter{

	@Override
	public void filter(Object in, Object out) {
		// TODO Auto-generated method stub
		if (!((JsonObject) in).has("hostHealth")) return ;
		if (!((JsonObject) in).has("res_id")) return ;

		String res_id = ((JsonObject) in).get("res_id").getAsString();
		double health_value = ((JsonObject) in).get("hostHealth").getAsDouble();
		
		JsonObject res_object = new JsonObject();
		if (((JsonObject) out).has(res_id)) {
			res_object = ((JsonObject) out).get(res_id).getAsJsonObject();
		} 
		JsonObject health_object = new JsonObject();
		if (res_object.has("serverHealth")) {
			health_object = res_object.get("serverHealth").getAsJsonObject();
			double sum = health_object.get("sum").getAsDouble() + health_value;
			int total = health_object.get("total").getAsInt() + 1;
			health_object.addProperty("sum", sum);
			health_object.addProperty("total", total);
		} else {
			health_object.addProperty("sum", health_value);
			health_object.addProperty("total", 1);
		}
		res_object.add("serverHealth", health_object);
		if (((JsonObject) in).has("res_name")) {
			String res_name = ((JsonObject) in).get("res_name").getAsString();
			res_object.addProperty("res_name", res_name);
		}
		((JsonObject) out).add(res_id, res_object);
	}

}
