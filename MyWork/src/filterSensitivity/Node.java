package filterSensitivity;
public class Node {

    public Node[] subNodes;
    public double[] branchLengths;
    public String nodeName;
    
    public Node(Node[] nodes, double[] lengths, String name){
    	this.subNodes = nodes;
	   	this.branchLengths = lengths;
	   	this.nodeName = name;
	}

    
    // Methods for Setting 
    
    public void setSubNodes(Node[] nodes){
    	this.subNodes = nodes;
    }
    public void setBranchLengths(double[] lengths){
    	this.branchLengths = lengths;
    }
    public void setNodeName(String name){
    	this.nodeName = name;
    }
    
    
    // Methods for Getting

    public Node[] getSubNodes(){
   	 	return subNodes;
    }
    public double[] getBranchLengths(){
   	 	return branchLengths;
    }
    public String getNodeName(){
    	return nodeName;
    }
}
