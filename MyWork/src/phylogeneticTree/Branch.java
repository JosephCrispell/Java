package phylogeneticTree;

public class Branch {

	public int branchIndex;
	public Node source;
	public Node sink;
		
	public Branch(int index, Node from, Node to) {
		
		this.branchIndex = index;
		this.source = from;
		this.sink = to;

	}
	
	// Getting methods
	public int getBranchIndex(){
		return this.branchIndex;
	}
	public Node getSource(){
		return this.source;
	}
	public Node getSink(){
		return this.sink;
	}

}
