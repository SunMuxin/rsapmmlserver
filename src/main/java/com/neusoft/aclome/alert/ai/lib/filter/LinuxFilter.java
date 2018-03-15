package com.neusoft.aclome.alert.ai.lib.filter;

import com.google.gson.JsonObject;

public class LinuxFilter implements Filter{

	@Override
	public void filter(Object in, Object out) {
		// TODO Auto-generated method stub
		if (!((JsonObject) in).has("LINUX_CpuUsage")) return ;
		if (!((JsonObject) in).has("LINUX_DiskUsePercent")) return ;
		if (!((JsonObject) in).has("LINUX_MemoryUsage")) return ;
		if (!((JsonObject) in).has("res_id")) return ;
		
		String res_id = ((JsonObject) in).get("res_id").getAsString();
		double CpuUsage = ((JsonObject) in).get("LINUX_CpuUsage").getAsDouble();
		double DiskUsePercent = ((JsonObject) in).get("LINUX_DiskUsePercent").getAsDouble();
		double MemoryUsage = ((JsonObject) in).get("LINUX_MemoryUsage").getAsDouble();

		JsonObject res_object = new JsonObject();
		if (((JsonObject) out).has(res_id)) {
			res_object = ((JsonObject) out).get(res_id).getAsJsonObject();
		}
		
		JsonObject CpuUsage_object = new JsonObject();
		if (res_object.has("CpuUsage")) {
			CpuUsage_object = res_object.get("CpuUsage").getAsJsonObject();
			double sum = CpuUsage_object.get("sum").getAsDouble() + CpuUsage;
			int total = CpuUsage_object.get("total").getAsInt() + 1;
			CpuUsage_object.addProperty("sum", sum);
			CpuUsage_object.addProperty("total", total);
		} else {
			CpuUsage_object.addProperty("sum", CpuUsage);
			CpuUsage_object.addProperty("total", 1);
		}
		res_object.add("CpuUsage", CpuUsage_object);
		
		JsonObject DiskUsePercent_object = new JsonObject();
		if (res_object.has("DiskUsePercent")) {
			DiskUsePercent_object = res_object.get("DiskUsePercent").getAsJsonObject();
			double sum = DiskUsePercent_object.get("sum").getAsDouble() + DiskUsePercent;
			int total = DiskUsePercent_object.get("total").getAsInt() + 1;
			DiskUsePercent_object.addProperty("sum", sum);
			DiskUsePercent_object.addProperty("total", total);
		} else {
			DiskUsePercent_object.addProperty("sum", DiskUsePercent);
			DiskUsePercent_object.addProperty("total", 1);
		}
		res_object.add("DiskUsePercent", DiskUsePercent_object);
		
		JsonObject MemoryUsage_object = new JsonObject();
		if (res_object.has("MemoryUsage")) {
			MemoryUsage_object = res_object.get("MemoryUsage").getAsJsonObject();
			double sum = MemoryUsage_object.get("sum").getAsDouble() + MemoryUsage;
			int total = MemoryUsage_object.get("total").getAsInt() + 1;
			MemoryUsage_object.addProperty("sum", sum);
			MemoryUsage_object.addProperty("total", total);
		} else {
			MemoryUsage_object.addProperty("sum", MemoryUsage);
			MemoryUsage_object.addProperty("total", 1);
		}
		res_object.add("MemoryUsage", MemoryUsage_object);
		
		if (((JsonObject) in).has("res_name")) {
			String res_name = ((JsonObject) in).get("res_name").getAsString();
			res_object.addProperty("res_name", res_name);
		}
		((JsonObject) out).add(res_id, res_object);
	}
}
