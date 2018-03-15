package com.neusoft.aclome.alert.ai.lib.filter;

import com.google.gson.JsonObject;

public class AlarmFilter implements Filter{

	@Override
	public void filter(Object in, Object out) {
		// TODO Auto-generated method stub
		if (!((JsonObject) in).has("priority_name")) return ;
		if (!((JsonObject) in).has("res_id")) return ;
		
		String res_id = ((JsonObject) in).get("res_id").getAsString();
		String priority_name = ((JsonObject) in).get("priority_name").getAsString();
		
		JsonObject res_object = new JsonObject();
		if (((JsonObject) out).has(res_id)) {
			res_object = ((JsonObject) out).get(res_id).getAsJsonObject();
		} 
		JsonObject alarm_object = new JsonObject();
		if (res_object.has("priority_name")) {
			alarm_object = res_object.get("priority_name").getAsJsonObject();
		}
		if (!alarm_object.has("highAlarm")) {
			alarm_object.addProperty("highAlarm", 0);
		}
		if (!alarm_object.has("mediumAlarm")) {
			alarm_object.addProperty("mediumAlarm", 0);
		}
		if (!alarm_object.has("lowAlarm")) {
			alarm_object.addProperty("lowAlarm", 0);
		}
		if (priority_name.equals("high")) {
			int highAlarm = alarm_object.get("highAlarm").getAsInt()+1;
			alarm_object.addProperty("highAlarm", highAlarm);
		}
		if (priority_name.equals("medium")) {
			int mediumAlarm = alarm_object.get("mediumAlarm").getAsInt()+1;
			alarm_object.addProperty("mediumAlarm", mediumAlarm);
		}
		if (priority_name.equals("low")) {
			int lowAlarm = alarm_object.get("lowAlarm").getAsInt()+1;
			alarm_object.addProperty("lowAlarm", lowAlarm);
		}
		res_object.add("priority_name", alarm_object);
		if (((JsonObject) in).has("res_name")) {
			String res_name = ((JsonObject) in).get("res_name").getAsString();
			res_object.addProperty("res_name", res_name);
		}
		((JsonObject) out).add(res_id, res_object);
	}
}
