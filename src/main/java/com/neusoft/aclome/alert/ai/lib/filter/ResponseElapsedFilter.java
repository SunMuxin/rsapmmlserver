package com.neusoft.aclome.alert.ai.lib.filter;

import com.google.gson.JsonObject;

public class ResponseElapsedFilter implements Filter{

	@Override
	public void filter(Object in, Object out) {
		// TODO Auto-generated method stub
		if (!((JsonObject) in).has("responseElapsed")) return ;
		if (!((JsonObject) in).has("app_id")) return ;

		String res_id = ((JsonObject) in).get("app_id").getAsString();
		double response_elapsed = ((JsonObject) in).get("responseElapsed").getAsDouble();
		
		JsonObject res_object = new JsonObject();
		if (((JsonObject) out).has(res_id)) {
			res_object = ((JsonObject) out).get(res_id).getAsJsonObject();
		}
		JsonObject response_elapsed_object = new JsonObject();
		if (res_object.has("responseElapsed")) {
			response_elapsed_object = res_object.get("responseElapsed").getAsJsonObject();
			double sum = response_elapsed_object.get("sum").getAsDouble() + response_elapsed;
			int total = response_elapsed_object.get("total").getAsInt() + 1;
			response_elapsed_object.addProperty("sum", sum);
			response_elapsed_object.addProperty("total", total);
		} else {
			response_elapsed_object.addProperty("sum", response_elapsed);
			response_elapsed_object.addProperty("total", 1);
		}
		res_object.add("responseElapsed", response_elapsed_object);
		((JsonObject) out).add(res_id, res_object);
	}

}
