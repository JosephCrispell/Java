package newickTree;

import java.io.IOException;

public class methods {
	
	public static void main(String[] args) throws IOException {
		
		// Set the path
		String path = "/home/josephcrispell/Desktop/Research/Homoplasy/";
		
		// Read the NEWICK tree and store as a traversable node set
		Tree tree = new Tree(path + "example-TRUE_25-07-18.tree");

		tree.print();
	}
	
}
