package newickTree;

import java.util.ArrayList;
import java.util.Hashtable;

public class Node {

	public String name; // Only available for the tips
	public int index;
	public boolean internal;
	public int parentIndex;
	public int[] subNodeIndices;
	public double branchLength = -1;
	
	public Hashtable<String, ArrayList<Double>> nodeInfo;
	public Hashtable<String, ArrayList<Double>> branchInfo;
	
	public Node(int id, boolean isInternal) {
		
		this.index = id;
		this.internal = isInternal;
	}
	
	// Setting methods
	public void setName(String id) {
		this.name = id;
	}
	public void setBranchLength(double value) {
		this.branchLength = value;
	}
	public void setNodeInfo(Hashtable<String, ArrayList<Double>> info) {
		this.nodeInfo = info;
	}
	public void setBranchInfo(Hashtable<String, ArrayList<Double>> info) {
		this.branchInfo = info;
	}
	
	// Getting methods
	public double getBranchLength() {
		return this.branchLength;
	}
}
