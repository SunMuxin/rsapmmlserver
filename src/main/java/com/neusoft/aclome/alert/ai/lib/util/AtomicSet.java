package com.neusoft.aclome.alert.ai.lib.util;

import java.util.HashSet;
import java.util.Set;

public class AtomicSet<T> {
	private Set<T> set = new HashSet<T>();
	
	public synchronized boolean containsAndAdd(T e) {
		if (set.contains(e)) return true;
		set.add(e);
		return false;
	}
}
