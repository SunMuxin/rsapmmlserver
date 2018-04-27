package com.neusoft.aclome.alert.ai.lib.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Digraph <T extends NodeOfDigraphInterface> {
	
	private Map<T, List<Link<T>>> node_links = new HashMap<T, List<Link<T>>>();
	private List<Link<T>> total_links = new ArrayList<Link<T>>();
	private List<T> nodes = new ArrayList<T>();
	private Set<String> ids = new HashSet<String>();
	private Map<T, Integer> dp = new HashMap<T, Integer>();
		
	public Digraph() {}
	
	public void add_link(T source, T target, Double value) {
		add_node(source);
		add_node(target);
		
		List<Link<T>> links = node_links.getOrDefault(source, new ArrayList<Link<T>>());
		links.add(new Link<T>(source, target, value));
		node_links.put(source, links);
		total_links.add(new Link<T>(source, target, value));
		dp.clear();
	}
	
	public void add_link(Link<T> link) {
		add_link(link.getSource(), link.getTarget(), link.getValue());
	}
	
	public void add_node(T name) {
		if (!ids.contains(name.getId())) {
			nodes.add(name);
			ids.add(name.getId());
		}
	}
	
	private int dfs(T u) {
		if (dp.containsKey(u)) 
			return dp.get(u);
		dp.put(u, 1);
		int ret = 1;
		for (Link<T> link : node_links.getOrDefault(u, new ArrayList<Link<T>>())) {
			ret = Math.max(dfs(link.getTarget())+1, ret);
		}
		dp.put(u, ret);
		return ret;
	}
	
	/**
	 * @param limit_length 
	 */
	public Digraph<T> keyChain(int limit_length) {
        Queue<T> queue = new LinkedList<T>();
        Set<T> visited = new HashSet<T>();
		for (T u : nodes) {
			if (dfs(u) >= limit_length) {
				queue.add(u);
			}
		}
		Digraph<T> ret = new Digraph<T>();
		while(!queue.isEmpty()) {
			T u = queue.poll();
			visited.add(u);
			for (Link<T> link : node_links.getOrDefault(u, new ArrayList<Link<T>>())) {
				ret.add_link(link);
				if (visited.contains(link.getTarget())) continue;
				queue.add(link.getTarget());
			}
		}
		return ret;
	}
	
	public int getLongestChain() {
		int ret = 0;
		for (T u : nodes) {
			ret = Math.max(ret, dfs(u));
		}
		return ret;
	}
		
	@Override
	public String toString() {
		JsonObject json = new JsonObject();
		
		JsonArray nodes = new JsonArray();
		for (T node : this.nodes) {
			nodes.add(new JsonParser().parse(new Gson().toJson(node)));
		}
		json.add("nodes", nodes);
		
		JsonArray links = new JsonArray();
		for (Link<T> link : this.total_links) {
			JsonObject json_link = new JsonObject();
			json_link.addProperty("source", link.getSource().getId());
			json_link.addProperty("target", link.getTarget().getId());
			json_link.addProperty("id", link.getId());
			links.add(json_link);
		}
		json.add("links", links);
		return json.toString();
	}
	
	public static class Link <T extends NodeOfDigraphInterface> {
		private String id = UUID.randomUUID().toString();
		private T source = null;
		private T target = null;
		private Double value = null;
		
		public Link(T source, T target, Double value) {
			this.source = source;
			this.target = target;
			this.value = value;
		}
		public T getSource() {
			return source;
		}
		public T getTarget() {
			return target;
		}
		public Double getValue() {
			return value;
		}
		public String getId() {
			return id;
		}

		@Override
		public int hashCode() {
			if (source == null || target == null)
				return 0;
			return source.hashCode() ^ (target.hashCode()>>1);
		}
		
		@SuppressWarnings("rawtypes")
		@Override
		public boolean equals(Object o) {
			if(o instanceof Digraph.Link) {
				if (!this.source.equals(((Digraph.Link) o).getSource())){
					return false;
				}
				if (!this.target.equals(((Digraph.Link) o).getTarget())){
					return false;
				}
				if (!this.value.equals(((Digraph.Link) o).getValue())){
					return false;
				}
				return true;
			}
			return false;
		}
	}
}
