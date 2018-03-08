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
    public void setCommonAlleles(String[] snps){
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
    public String[] getCommonAlleles(){
    	return this.commonVariantPositionAlleles;
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