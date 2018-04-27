package com.neusoft.aclome.alert.ai.lib.filter;

import java.util.List;

public interface Filter {
	public void filter(Object in, String solr_url, List<String> fqs);
}
