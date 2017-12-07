package phylogeneticTree;

public class TerminalNode {

	public Node node;
	public String[] label;
	public Node[] internalNodes = new Node[0];
	public double[] distance2InternalNodes = new double[0];
	
	public TerminalNode(Node leaf, String[] info){
		this.node = leaf;
		this.label = info;
	}
	
	// Setting Methods
	public void setNode(Node leaf){
		this.node = leaf;
	}
	public void setInternalNodes(Node[] parents){
		this.internalNodes = parents;
	}
	public void setDistancesToInternalNodes(double[] distances){
		this.distance2InternalNodes = distances;
	}
	public void setLabel(String[] info){
		this.label = info;
	}
	
	// Getting Methods
	public Node getNode(){
		return this.node;
	}
	public Node[] getInternalNodes(){
		return this.internalNodes;
	}
	public double[] getDistances2ToInternalNodes(){
		return this.distance2InternalNodes;
	}
	public String[] getLabel(){
		return this.label;
	}
}