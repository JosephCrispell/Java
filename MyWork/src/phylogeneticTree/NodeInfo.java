package phylogeneticTree;

import java.util.Hashtable;

public class NodeInfo {

	public String nodeId = "NA";
	public Hashtable<String, double[]> nodeInfo = new Hashtable();
	public Hashtable<String, double[]> branchInfo = new Hashtable();
	public double branchLength;
	
	public NodeInfo(String id, Hashtable<String, double[]> nodeVariables, Hashtable<String, double[]> branchVariables, double length){
		this.nodeId = id;
		this.nodeInfo = nodeVariables;
		this.branchInfo = branchVariables;
		this.branchLength = length;
	}
	
	// Setting Methods
	public void setNodeId(String id){
		this.nodeId = id;
	}
	public void setNodeInfo(Hashtable<String, double[]> nodeVariables){
		this.nodeInfo = nodeVariables;
	}
	public void setBranchInfo(Hashtable<String, double[]> branchVariables){
		this.branchInfo = branchVariables;
	}
	public void setBranchLength(double length){
		this.branchLength = length;
	}
	
	// Getting Methods
	public String getNodeId(){
		return this.nodeId;
	}
	public Hashtable<String, double[]> getNodeInfo(){
		return this.nodeInfo;
	}
	public Hashtable<String, double[]> getBranchInfo(){
		return this.branchInfo;
	}
	public double getBranchLength(){
		return this.branchLength;
	}
	
}
