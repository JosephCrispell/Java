package homoplasyFinderV2;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.JTextArea;

public class ConsistencyIndex {

	// Current date
	private String date = Methods.getCurrentDate("dd-MM-yy");
	
	// Verbose print outs?
	private boolean verbose;
	
	// Tree
	private Tree tree;
	
	// States table
	private States statesTable;
	
	// Consistency index
	private int[][] stateCountsAtEachPosition; // Concatenated counts for each state at each position
	private String[][] statesAtEachPosition;
	private int[] minNumberChangesOnTreeAtEachPosition;
	private int[][] internalNodeIndicesOfChanges;
	private ArrayList<Integer> inconsistentPositions;
	private double[] consistencyIndices;
	
	public ConsistencyIndex(Tree tree, States tipStates, boolean verbose, boolean multithread) {
		
		/**
		 * Algorithm to calculate the minimum number of changes of each site in an alignment on a phylogeny
		 * 
		 *  Instructions taken from slide 9 in ftp://statgen.ncsu.edu/pub/thorne/molevoclass/parsimony2015.pdf
		 *  	Not exactly the same, skip tree rooting and last two steps (I think these are stopping conditions)
		 * 
		 * 	Steps:
		 * 	1. Initialise tree length to 0
		 * 	2. Define the nucleotides found at each position at each tip (essentially the FASTA alignment)
		 * 	3. Visit an unvisited interior node whose descendants have already been visited
		 * 		* IF any of the descendants of the current interior node are unvisited, visit them first
		 * 		* Examine each position
		 * 			- Initialise nucleotide set
		 * 			- If the nucleotide sets of each descendant have nucleotides in common - use INTERSECTION at nucleotide set
		 * 			- Otherwise, use UNION as nucleotide set AND increase tree length by 1
		 * 			- Set the current interior node with the nucleotide set and set as visited
		 * 
		 *  Representing nucleotide sets at a given position:
		 *  
		 *  1	3		4			1	3		4		Nucleotide sets:	A	C	G	T	Tree length change		
		 *  a	b	5	d			A	A	5	A						a	T	F	F	F	-
		 *  |	|	c	|			|	|	G	|						b	T	F	F	F	-
		 *   ---	|	|			 ---	|	|						c	F	F	T	F	-
		 *    e		 ---			  A		 ---						d	T	F	F	F	-					N. states for position = 3
		 *    |		  f				  |		 G,A						e	T	F	F	F	0					N. changes for position = 2
		 *    |		  |		2		  |		  |		2					f	T	F	T	F	+1								  3 - 1
		 *     -------		g		   -------		T					g	F	F	F	T	-					Consistency = ----- = 1
		 *     	  h			|			  A			|					h	T	F	F	F	0									2
		 *     	  |			|			  |			|					i	T	F	F	T	+1
		 *     	   ---------			   ---------						  Tree length = 2
		 *     		   i					  A,T
		 *     		   |					   |
		 *  
		 *  Notes:
		 *  - The tree is traversed only once, at each node the nucleotide sets are created for each position
		 *  - Tree doesn't need to be bifurcating, where > 2 sub-nodes exist the sub-nodes are compared from left to right
		 *  - For ambiguous sites (N, -, R (purine: A or G), or Y (pyrimidine: C or T)):
		 *  		A		C		G		T
		 *  	N	true	true 	true	true (Ancestor will be whatever other sub-node has)
		 *  	-	true	true 	true	true
		 *  	R	true	false	true	false
		 *  	Y	false	true	false	true
		 *  
		 *  Currently extending to work with a presence/absence matrix:
		 *  	state sets:	0	1
		 *  	NOT sure how to deal with 'N' or even if that is necessary
		 */
		
		// Note whether verbose outputs wanted
		this.verbose = verbose;
		
		// Store the tree and tip states at each position/trait
		this.tree = tree;
		this.statesTable = tipStates;
		
		// Initialise all the necessary variables for storing the information associated with calculating the consistency indices
		this.stateCountsAtEachPosition = new int[this.statesTable.getNSites()][0]; // Concatenated counts for each possible state at each position
		this.statesAtEachPosition = new String[this.statesTable.getNSites()][0]; // Ordered array of states observed at each position
		this.minNumberChangesOnTreeAtEachPosition = new int[this.statesTable.getNSites()]; // The minimum number of changes for each position
		this.internalNodeIndicesOfChanges = new int[this.statesTable.getNSites()][this.tree.getNInternalNodes()]; // The internal node indices where those changes occur
		this.inconsistentPositions = new ArrayList<Integer>(); // Array store the inconsistent sites (consistency < 1)
		this.consistencyIndices = new double[this.statesTable.getNSites()]; // Consistency index of each site
		
		// Check if we want to multithread
		if(multithread) {
						
			// Calculate the consistency index of each position
			ConsistencyIndexThread.calculateConsistencyIndicesOnMultipleThreads(this.statesTable, this.tree, this.stateCountsAtEachPosition,
					this.statesAtEachPosition, this.internalNodeIndicesOfChanges, this.minNumberChangesOnTreeAtEachPosition,
					this.inconsistentPositions, this.consistencyIndices);
			
		}else {
			
			// Calculate the consistency index of each position
			calculateConsistencyIndices(this.statesTable, this.tree, this.stateCountsAtEachPosition,
					this.statesAtEachPosition, this.internalNodeIndicesOfChanges, this.minNumberChangesOnTreeAtEachPosition,
					this.inconsistentPositions, this.consistencyIndices, 0, this.statesTable.getNSites() - 1);
		}
		
		// Print summary if verbose
		if(this.verbose) {
			printSummary();
		}
	}
	
	// Setting methods
	public void setMinNumberChangesOnTreeAtEachPosition(int[] minNumberChangesOnTreeAtEachPosition) {
		this.minNumberChangesOnTreeAtEachPosition = minNumberChangesOnTreeAtEachPosition;
	}
	
	public void setInternalNodeIndicesOfChanges(int[][] internalNodeIndicesOfChanges) {
		this.internalNodeIndicesOfChanges = internalNodeIndicesOfChanges;
	}
	
	// Output methods
	public void printFASTAWithoutInConsistentSites(String fileName) throws IOException {
		
		// Create a hashtable with the inconsistent positions
		Hashtable<Integer, Integer> positionsToIgnore = Methods.indexArrayListInteger(this.inconsistentPositions);
		
		// Open an output file
		if(fileName == null){
			fileName = "sequences_withoutInconsistentSites_" + this.date + ".fasta";
		}
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Print out the number of isolates and sites in FASTA
		int newSequenceLength = this.statesTable.getNSites() - this.inconsistentPositions.size();
		WriteToFile.writeLn(bWriter, this.tree.getNTerminalNodes() + " " + newSequenceLength);
		
		// Write out each of the sequences
		for(int i = 0; i < this.tree.getNTerminalNodes(); i++){
			
			// Print sequence ID
			WriteToFile.writeLn(bWriter, ">" + this.tree.getTerminalNodes().get(i).getName());
			
			// Build the nucleotide sequence for printing
			WriteToFile.writeLn(bWriter, this.statesTable.getSequence(i, positionsToIgnore));
		}
		WriteToFile.close(bWriter);
	}
	
	public void printAnnotatedTree(String fileName) throws IOException {
		
		// Examine each of the inconsistent positions
		for(int position : this.inconsistentPositions) {
			
			// Examine each of the internal nodes where a change associated with the current position occurred
			for(int i = 0; i < this.minNumberChangesOnTreeAtEachPosition[position]; i++) {
				
				// Annotate the current internal node - by providing it with an node label (name)
				Node internalNode = this.tree.getInternalNodes().get(this.internalNodeIndicesOfChanges[position][i]);
				
				// Check if node already has name
				if(internalNode.getName() != null) {
					
					// Check if dealing with FASTA positions
					if(this.statesTable.getFileType().matches("fasta")) {
						internalNode.setName(internalNode.getName() + "-" + (position + 1));
					
					// Otherwise print trait name
					}else {
						internalNode.setName(internalNode.getName() + "-" + this.statesTable.getSites()[position]);
					}					
				
				// If not, create one
				}else {
					
					// Check if dealing with FASTA positions
					if(this.statesTable.getFileType().matches("fasta")) {
						internalNode.setName(Integer.toString(position + 1));
					
					// Otherwise print trait name
					}else {
						internalNode.setName(this.statesTable.getSites()[position]);
					}
				}
			}
		}
		
		// Print the tree to a newick string
		this.tree.print(fileName);
	}
	
	private void printSummary() {
		
		// Check if fasta file
		if(this.statesTable.getFileType().matches("fasta")) {
			
			// Print brief summary of inconsistent sites
			System.out.println("Identified " + this.inconsistentPositions.size() + " positions with consistency index < 1: ");
			for(int position : this.inconsistentPositions) {
				System.out.println(position + 1);
			}
			
		// Otherwise print summary of trait consistencies
		}else {
			
			// Print brief summary of inconsistent traits
			System.out.println("Identified " + this.inconsistentPositions.size() + " traits with consistency index < 1: ");
			for(int position : this.inconsistentPositions) {
				System.out.println(this.statesTable.getSites()[position]);
			}
		}
		
	}
	
	public void printSummary(String fileName, boolean includeConsistentSites) throws IOException {
		
		// Open the file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Create an output string
		String output = "";
		
		// Check if fasta file
		if(this.statesTable.getFileType().matches("fasta")) {
			
			// Print a summary of the inconsistent positions identified
			output += "Position\tConsistencyIndex\tCountsACGT\tMinimumNumberChangesOnTree\n";
			
			// Print each position
			if(includeConsistentSites) {
				for(int position = 0; position < this.statesTable.getNSites(); position++) {
					if(areMultipleStatesPresent(this.stateCountsAtEachPosition[position])) {
						output += (position + 1) + "\t" + this.consistencyIndices[position] + "\t" + 
								Methods.toString(this.stateCountsAtEachPosition[position], ":") + "\t" + this.minNumberChangesOnTreeAtEachPosition[position] + "\n";
					}else {
						output += (position + 1) + "\t" + 1 + "\t" + Methods.toString(this.stateCountsAtEachPosition[position], ":") + "\t" + "-" + "\n";
					}				
				}	
			}else {
				for(int position : this.inconsistentPositions) {
					output += (position + 1) + "\t" + this.consistencyIndices[position] + "\t" + 
							Methods.toString(this.stateCountsAtEachPosition[position], ":") + "\t" + this.minNumberChangesOnTreeAtEachPosition[position] + "\n";
				}			
			}
			
		// Otherwise print trait information
		}else {
			
			// Print a summary of the inconsistent positions identified
			output += "Trait\tConsistencyIndex\tTraits\tCounts\tMinimumNumberChangesOnTree\n";
			
			// Print each position
			if(includeConsistentSites) {
				for(int position = 0; position < this.statesTable.getNSites(); position++) {
					if(areMultipleStatesPresent(this.stateCountsAtEachPosition[position])) {
						output += this.statesTable.getSites()[position] + "\t" + this.consistencyIndices[position] + "\t" + 
								Methods.toString(this.statesAtEachPosition[position], ":") + "\t" + 
								Methods.toString(this.stateCountsAtEachPosition[position], ":") + "\t" + 
								this.minNumberChangesOnTreeAtEachPosition[position] + "\n";
					}else {
						output += this.statesTable.getSites()[position] + "\t" + 1 + "\t" + 
								Methods.toString(this.statesAtEachPosition[position], ":") + "\t" + 
								Methods.toString(this.stateCountsAtEachPosition[position], ":") + "\t" + "-" + "\n";
					}				
				}	
			}else {
				for(int position : this.inconsistentPositions) {
					output += this.statesTable.getSites()[position] + "\t" + this.consistencyIndices[position] + "\t" + 
							Methods.toString(this.statesAtEachPosition[position], ":") + "\t" + 
							Methods.toString(this.stateCountsAtEachPosition[position], ":") + "\t" + 
							this.minNumberChangesOnTreeAtEachPosition[position] + "\n";
				}			
			}
		}
		
		// Write the output to file
		WriteToFile.write(bWriter, output);		
		
		// Close the file
		WriteToFile.close(bWriter);
	
	}
	
	public void printSummary(JTextArea guiTextArea) throws IOException {
		
		// Check if fasta file
		if(this.statesTable.getFileType().matches("fasta")) {
					
			// Print a summary of the inconsistent positions identified
			guiTextArea.append("Identified " + this.inconsistentPositions.size() + " positions with consistency index < 1: \n");
			guiTextArea.append("Position\tConsistencyIndex\tCounts_A:C:G:T\tMinimumNumberChangesOnTree\n");
			
			// Print information for each position
			String output = "";
			for(int position : this.inconsistentPositions) {
				output += (position + 1) + "\t" + this.consistencyIndices[position] + "\t" + 
						Methods.toString(this.stateCountsAtEachPosition[position], ":") + "\t" + this.minNumberChangesOnTreeAtEachPosition[position] + "\n";
			}
			guiTextArea.append(output + "\n");
					
		// Otherwise print summary of trait consistencies
		}else {
			
			// Print brief summary of inconsistent traits
			guiTextArea.append("Identified " + this.inconsistentPositions.size() + " positions with consistency index < 1: \n");
			guiTextArea.append("Trait\tConsistencyIndex\tTraits\tCounts\tMinimumNumberChangesOnTree\n");
			
			// Print information for each position
			String output = "";
			for(int position : this.inconsistentPositions) {
				output += this.statesTable.getSites()[position] + "\t" + this.consistencyIndices[position] + "\t" + 
						Methods.toString(this.statesAtEachPosition[position], ":") + "\t" + 
						Methods.toString(this.stateCountsAtEachPosition[position], ":") + "\t" + 
						this.minNumberChangesOnTreeAtEachPosition[position] + "\n";
			}
			guiTextArea.append(output + "\n");
		}
	}
	
	public int[] getPositions() {
		
		int[] output = new int[this.inconsistentPositions.size()];
		for(int i = 0; i < this.inconsistentPositions.size(); i++) {
			output[i] = this.inconsistentPositions.get(i) + 1;
		}
		
		return output;
	}
	
	// Class specific methods
	public static void calculateConsistencyIndices(States states, Tree tree, int[][] stateCountsAtEachPosition,
			String[][] statesAtEachPosition, int[][] internalNodeIndicesOfChanges, int[] minNumberChangesOnTreeAtEachPosition, 
			ArrayList<Integer> inconsistentPositions, double[] consistencyIndices, int start, int end) {
		
		// Examine each position in the alignment
		for(int position = start; position <= end; position++) {
			
			// If multithreading the index for storing information will be different than position
			int positionIndex = position - start;
			
			// Count the number of times each state is found at the current position and note terminal node states
			boolean[][] terminalNodeStates = states.getTipStates(position, positionIndex, stateCountsAtEachPosition, statesAtEachPosition);
			
			// Check if multiple alleles present - i.e. not a constant site
			if(areMultipleStatesPresent(stateCountsAtEachPosition[positionIndex])) {
				
				// Calculate the minimum number of changes of each position on the phylogeny
				calculateMinimumNumberOfChangesOnPhylogeny(positionIndex, states.getNStates(),
						tree.getInternalNodes(), terminalNodeStates, internalNodeIndicesOfChanges, minNumberChangesOnTreeAtEachPosition);
				
				// Calculate the consistency index
				checkIfInconsistent(position, positionIndex, inconsistentPositions, consistencyIndices, stateCountsAtEachPosition,
						minNumberChangesOnTreeAtEachPosition);
			}
		}
	}
	
	public static boolean areMultipleStatesPresent(int[] stateCounts) {
		
		// Initialise a variable to return
		boolean result = false;
		
		// Initialise a count variable
		int count = 0;
		
		// Examine the counts
		for(int i = 0; i < stateCounts.length; i++) {
			
			if(stateCounts[i] > 1) {
				count++;
				if(count > 1) {
					result = true;
					break;
				}
			}
		}
		
		return result;
	}
	
	public static void checkIfInconsistent(int position, int positionIndex, ArrayList<Integer> inconsistentPositions, double[] consistencyIndices,
			int[][] stateCountsPerPosition, int[] minNumberChangesOnTreeAtEachPosition) {
					
		// Calculate the consistency index
		consistencyIndices[positionIndex] = calculateConsistencyIndexForPosition(stateCountsPerPosition[positionIndex], 
				minNumberChangesOnTreeAtEachPosition[positionIndex]);
		
		// Report the position if, the consistency index is less than 1
		if(consistencyIndices[positionIndex] < 1) {
		
			inconsistentPositions.add(position);
		}
	}
	
	public static double calculateConsistencyIndexForPosition(int[] alleleCounts, int minNumberChangesOnPhylogeny) {
		
		// Initialise a variable to record how many alleles are present
		int count = 0;
		
		// Examine each alleles count, how many are more than 0?
		for(int i = 0; i < alleleCounts.length; i++) {
			if(alleleCounts[i] > 0) {
				count++;
			}
		}
		
		// Calculate the consistency index: minimum number of changes (number states - 1) / number required on tree
		double consistency = 1;
		if(minNumberChangesOnPhylogeny > 0 && count > 1) {
			consistency = ((double) count - 1) / (double) minNumberChangesOnPhylogeny;
		}
		
		return consistency;
	}
	
	public static void calculateMinimumNumberOfChangesOnPhylogeny(int positionIndex, int nStatesPerSite,
			ArrayList<Node> internalNodes, boolean[][] terminalNodeStates,
			int[][] internalNodeIndicesOfChanges, int[] minNumberChangesOnTreeAtEachPosition) {
				
		// Initialise an array to store the possible states for each position assigned to each internal node
		boolean[][] possibleStatesForEachInternalNode = new boolean[internalNodes.size()][nStatesPerSite];
		
		// Starting at the first terminal node's ancestor, visit all the internal nodes
		identifyPossibleStatesForInternalNode(0, possibleStatesForEachInternalNode, positionIndex,
				internalNodes, nStatesPerSite, terminalNodeStates, internalNodeIndicesOfChanges, minNumberChangesOnTreeAtEachPosition);
	}

	
	public static void identifyPossibleStatesForInternalNode(int internalNodeIndex, boolean[][] possibleStatesForEachInternalNode, int positionIndex,
			ArrayList<Node> internalNodes, int nStatesPerSite, boolean[][] terminalNodeStates, int[][] internalNodeIndicesOfChanges,
			int[] minNumberChangesOnTreeAtEachPosition) {
		
		// Get the sub-node information for the current internal node
		ArrayList<Integer> subNodeIndices = internalNodes.get(internalNodeIndex).getSubNodeIndices();
		ArrayList<Boolean> subNodeTypes = internalNodes.get(internalNodeIndex).getSubNodeTypes();
		
		// Initialise an array to store the possible states assigned for each position, for each sub-node
		boolean[][] possibleStatesForSubNodes = new boolean[subNodeIndices.size()][nStatesPerSite];
		
		// Examine each sub-node
		for(int i = 0; i < subNodeIndices.size(); i++) {
			
			// Check whether the current node is internal
			if(subNodeTypes.get(i)) {
			
				// Identify the possible states for the current internal sub-node
				identifyPossibleStatesForInternalNode(subNodeIndices.get(i), possibleStatesForEachInternalNode, positionIndex, internalNodes, nStatesPerSite,
						terminalNodeStates, internalNodeIndicesOfChanges, minNumberChangesOnTreeAtEachPosition);
				
				// Get the possible states assigned to this internal sub-node
				possibleStatesForSubNodes[i] = possibleStatesForEachInternalNode[subNodeIndices.get(i)];
				
			// If it is terminal, get the states assigned at each position
			}else {
				possibleStatesForSubNodes[i] = terminalNodeStates[subNodeIndices.get(i)];
			}
		}
		
		// Determine the possible states for the current node, based on those of the sub-nodes
		possibleStatesForEachInternalNode[internalNodeIndex] = getPossibleStateForInternalNodeFromPossibleStatesOfSubNodes(possibleStatesForSubNodes,
				internalNodeIndex, positionIndex, internalNodeIndicesOfChanges, minNumberChangesOnTreeAtEachPosition, nStatesPerSite);
	}
	
	public static boolean[] getPossibleStateForInternalNodeFromPossibleStatesOfSubNodes(boolean[][] possibleStatesForSubNodes,
			int internalNodeIndex, int positionIndex, int[][] internalNodeIndicesOfChanges, 
			int[] minNumberChangesOnTreeAtEachPosition, int nStatesPerSite){
		
		// Get the possible states for the first subNode
		boolean[] possibleStates = possibleStatesForSubNodes[0];
		
		// Examine the other sub-nodes
		for(int i = 1; i < possibleStatesForSubNodes.length; i++) {
		
			// Compare the current sub-nodes possible states to those stored
			possibleStates =  comparePossibleStatesOfTwoSubNodes(possibleStates, possibleStatesForSubNodes[i], positionIndex, internalNodeIndex,
					internalNodeIndicesOfChanges, minNumberChangesOnTreeAtEachPosition, nStatesPerSite);
		}
		
		return possibleStates;		
	}
	
	public static boolean[] comparePossibleStatesOfTwoSubNodes(boolean[] a, boolean[] b, int positionIndex, int internalNodeIndex,
			int[][] internalNodeIndicesOfChanges, int[] minNumberChangesOnTreeAtEachPosition, int nStatesPerSite) {
		
		// Initialise a vector to store the possible states found in both a and b (union)
		boolean[] union = new boolean[4];
		
		// Initialise a vector to store the possible states found only in a or b (intersect)
		boolean[] intersect = new boolean[4];
		
		// Initialise a variable to record whether common possible states found
		boolean common = false;
		
		// Examine the possible states
		for(int i = 0; i < nStatesPerSite; i++) {
			
			// Check for a common state
			if(a[i] == true && b[i] == true) {
				intersect[i] = true;
				common = true;
				
			// Check for a non-common state
			}else if(a[i] != b[i]){
				union[i] = true;
			}
		}
		
		// If common possible states found, return the intersect (only those common states)
		if(common) {
			return intersect;
			
		// Else return the union (all possible states) and increment the tree length for the current position
		}else {
			internalNodeIndicesOfChanges[positionIndex][minNumberChangesOnTreeAtEachPosition[positionIndex]] = internalNodeIndex;
			minNumberChangesOnTreeAtEachPosition[positionIndex]++;
			return union;
		}
	}
	
}
