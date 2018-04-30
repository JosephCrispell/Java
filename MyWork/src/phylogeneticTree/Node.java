package phylogeneticTree;

import java.util.ArrayList;
import java.util.Hashtable;

public class Node {

	public NodeInfo nodeInfo;
	public Node[] subNodes;
	public Node parentNode;
	public ArrayList<String> tips;
	
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
    public void setTips(ArrayList<String> nodes){
    	this.tips = nodes;
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
    public ArrayList<String> getTips(){
    	return this.tips;
    }
    
    // General Methods
    public static Node[] append(Node[] array, Node node){
		
		Node[] newArray = new Node[array.length + 1];
		
		for(int i = 0; i < array.length; i++){
			newArray[i] = array[i];
		}
		
		newArray[array.length] = node;
		
		return newArray;		
	}
	public static boolean in(Node[] array, Node element){
		
		boolean found = false;
		
		for(Node x : array){
			if(x == element){
				found = true;
				break;
			}
		}
		
		return found;
	}

}