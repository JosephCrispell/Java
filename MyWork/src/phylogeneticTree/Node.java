package phylogeneticTree;

import java.util.Hashtable;

public class Node {

	public NodeInfo nodeInfo;
	public Node[] subNodes;
	public Node parentNode;
	
	public Node[] pathToRoot;
	public String[] commonVariantPositionAlleles;
	
    public Node(NodeInfo info, Node[] nodes, Node parent){
    	this.nodeInfo = info;
    	this.subNodes = nodes;
	   	this.parentNode = parent;
	}

    
    // Methods for Setting 
    public void setNodeInfo(NodeInfo info){
    	this.nodeInfo = info;
    }
    public void setSubNodes(Node[] nodes){
    	this.subNodes = nodes;
    }
    public void setParentNode(Node parent){
    	this.parentNode = parent;
    }
    public void setPathToRoot(Node[] nodes){
    	this.pathToRoot = nodes;
    }
    public void setCommonVariantPositionAlleles(String[] snps){
    	this.commonVariantPositionAlleles = snps;
    }
    
    // Methods for Getting
    public NodeInfo getNodeInfo(){
    	return this.nodeInfo;
    }
    public Node[] getSubNodes(){
   	 	return this.subNodes;
    }
    public Node getParentNode(){
    	return this.parentNode;
    }
    public Node[] getPathToRoot(){
    	return this.pathToRoot;
    }
    public String[] getCommonVariantPositionAlleles(){
    	return this.commonVariantPositionAlleles;
    }
}