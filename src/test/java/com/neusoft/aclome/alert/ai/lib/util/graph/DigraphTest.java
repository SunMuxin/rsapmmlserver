package com.neusoft.aclome.alert.ai.lib.util.graph;

import java.util.UUID;

import org.junit.Test;

import com.neusoft.aclome.alert.ai.lib.graph.Digraph;
import com.neusoft.aclome.alert.ai.lib.graph.NodeOfDigraphInterface;
import com.neusoft.aclome.alert.ai.lib.util.CONSTANT;

public class DigraphTest {

	@SuppressWarnings("unused")
	private class Node implements NodeOfDigraphInterface{
		private String id = UUID.randomUUID().toString();
		private String name;
		
		public Node(String name) {
			this.name = name;
			this.id = name;
		}

		@Override
		public String getId() {
			// TODO Auto-generated method stub
			return id;
		}
		
		@Override 
		public int hashCode() {
			return getId().hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			return ((Node) o).getId().equals(getId());
		}
	}
	
	@Test
	public void test() {
		Digraph<Node> dg = new Digraph<Node>();
		dg.add_link(new Node("1"), new Node("2"), 1.0);
		dg.add_link(new Node("2"), new Node("3"), 1.0);
		dg.add_link(new Node("3"), new Node("4"), 1.0);
		dg.add_link(new Node("4"), new Node("1"), 1.0);
		dg.add_link(new Node("3"), new Node("5"), 1.0);
		dg.add_link(new Node("6"), new Node("7"), 1.0);
		dg.add_link(new Node("8"), new Node("9"), 1.0);
		dg.add_link(new Node("3"), new Node("10"), 1.0);
		dg.add_link(new Node("11"), new Node("15"), 1.0);
		dg.add_link(new Node("13"), new Node("11"), 1.0);
		System.out.println(dg.toString());
		System.out.println(dg.keyChain(CONSTANT.int_three).toString());
	}

}
