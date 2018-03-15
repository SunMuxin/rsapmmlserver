package com.neusoft.aclome.alert.ai.lib.util.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.neusoft.aclome.alert.ai.lib.util.Util;

public class Digraph <T extends NodeInterface> {
	
	private Map<T, List<Link<T>>> node_links = new HashMap<T, List<Link<T>>>();
	private List<Link<T>> total_links = new ArrayList<Link<T>>();
	private Set<T> nodes = new HashSet<T>();
	
	public Digraph() {}
	
	public void add_link(T source, T target, Double value) {
		if (!nodes.contains(source)) {
			nodes.add(source);
		}
		if (!nodes.contains(target)) {
			nodes.add(target);
		}
		List<Link<T>> links = node_links.getOrDefault(source, new ArrayList<Link<T>>());
		links.add(new Link<T>(source, target, value));
		node_links.put(source, links);
		total_links.add(new Link<T>(source, target, value));
	}
	
	public void add_node(T name) {
		if (!nodes.contains(name)) {
			nodes.add(name);
		}
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
			links.add(new JsonParser().parse(new Gson().toJson(link)));
		}
		json.add("links", links);
		Util.info("Digraph", json.toString());
		return json.toString();
	}
	
	public static class Link <T extends NodeInterface> {
		private String id = UUID.randomUUID().toString();
		private String source = null;
		private String target = null;
		private Double value = null;
		
		public Link(T source, T target, Double value) {
			this.source = source.getId();
			this.target = target.getId();
			this.value = value;
		}
		public String getSource() {
			return source;
		}
		public String getTarget() {
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
