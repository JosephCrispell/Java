package homoplasyFinderV2;

import java.util.ArrayList;
import java.util.Hashtable;

public class States {

	// Table to store state (as integer) of each position/trait ordered by tips in phylogeny
	private int[][] stateMatrix;
	
	// Hashtable to record index of each state (nucleotide/factor)
	public Hashtable<String, Integer> stateIndices;
	
	// Hashtable to store index of each terminal node ID
	private Hashtable<String, Integer> tipIndices;
	
	// Initialise the states object from sequences
	public States(ArrayList<Node> terminalNodes, String fileName, String fileType) {
		
		// Index the terminal node IDs
		this.tipIndices = Methods.indexArrayListString(terminalNodeIDs);
	}
	
	// Methods
	private void indexTerminalNodeIDs(ArrayList<Node> terminalNodes) {
		
		// Initialise the hashtable to stroe the indices of each terminal node ID
		this.tipIndices = new Hashtable<String, Integer>();
		
		// Examine each of the terminal nodes
		for(int i = 0; i < terminalNodes.size(); i++) {
			
			// Store the ID and index of the current terminal node
			
		}
	}
}
