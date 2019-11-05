package homoplasyFinder;

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
	private ArrayList<Node> internalNodes;
	private int nInternalNodes;
	private ArrayList<Node> terminalNodes;
	private int nTerminalNodes;
	
	// Sequences
	private ArrayList<Sequence> sequences;
	private int nSites;
	private int nStatesPerSite;
	private int[] terminalNodeIndexForEachSequence;
	
	// Consistency index
	private int[][] stateCountsPerPosition;
	private int[] minNumberChangesOnTreeAtEachPosition;
	private int[][] internalNodeIndicesOfChanges;
	private ArrayList<Integer> inconsistentPositions;
	private double[] consistencyIndices;
	
	public ConsistencyIndex(Tree tree, ArrayList<Sequence> sequences, boolean verbose, boolean multithread,
			int nStates) {
		
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
		
		// Store the number of states per site
		this.nStatesPerSite = nStates;
		
		// Store the tree and sequences
		storeTree(tree);
		storeSequences(sequences);
		
		// Initialise all the necessary variables for storing the information associated with calculating the consistency indices
		this.stateCountsPerPosition = new int[this.nSites][this.nStatesPerSite]; // Counts for each possible state at each position
		this.minNumberChangesOnTreeAtEachPosition = new int[this.nSites]; // The minimum number of changes for each position
		this.internalNodeIndicesOfChanges = new int[this.nSites][this.nInternalNodes]; // The internal node indices where those changes occur
		this.inconsistentPositions = new ArrayList<Integer>(); // Array store the inconsistent sites (consistency < 1)
		this.consistencyIndices = new double[this.nSites]; // Consistency index of each site
		
		// Check if we want to multithread
		if(multithread) {
						
			// Calculate the consistency index of each position
			calculateConsistencyIndicesUsingMultipleThreads(this.stateCountsPerPosition, this.minNumberChangesOnTreeAtEachPosition,
					this.internalNodeIndicesOfChanges, this.inconsistentPositions, this.consistencyIndices, this.nSites, 
					this.sequences, this.terminalNodeIndexForEachSequence, this.internalNodes, this.nStatesPerSite, this.nTerminalNodes);			
			
		}else {
			
			// Calculate the consistency index of each position
			calculateConsistencyIndexForEachSiteOnPhylogeny(this.nSites, this.nTerminalNodes, this.nStatesPerSite,
					this.sequences, this.terminalNodeIndexForEachSequence, this.stateCountsPerPosition,
					this.internalNodes, this.internalNodeIndicesOfChanges, this.minNumberChangesOnTreeAtEachPosition,
					this.inconsistentPositions, this.consistencyIndices, 0, this.nSites - 1);
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
	public void printSequencesWithoutInConsistentSites(String fileName) throws IOException {
		
		// Create a hashtable with the inconsistent positions
		Hashtable<Integer, Integer> positionsToIgnore = Methods.indexArrayListInteger(this.inconsistentPositions);
		
		// Open an output file
		if(fileName == null){
			fileName = "sequences_withoutInconsistentSites_" + this.date + ".fasta";
		}
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Print out the number of isolates and sites in FASTA
		int newSequenceLength = this.nSites - this.inconsistentPositions.size();
		WriteToFile.writeLn(bWriter, sequences.size() + " " + newSequenceLength);
		
		// Initialise an array to each isolate sequence data
		char[] sequence = new char[newSequenceLength];
		
		// Write out each of the sequences
		for(int i = 0; i < sequences.size(); i++){
			
			// Print sequence ID
			WriteToFile.writeLn(bWriter, ">" + sequences.get(i).getName());
			
			// Print sequence
			sequence = Methods.deletePositions(sequences.get(i).getSequence(), positionsToIgnore);
			WriteToFile.writeLn(bWriter, Methods.toString(sequence));
		}
		WriteToFile.close(bWriter);
	}
	
	public void printAnnotatedTree(String fileName) throws IOException {
		
		// Examine each of the inconsistent positions
		for(int position : this.inconsistentPositions) {
			
			// Examine each of the internal nodes where a change associated with the current position occurred
			for(int i = 0; i < this.minNumberChangesOnTreeAtEachPosition[position]; i++) {
				
				// Annotate the current internal node - by providing it with an node label (name)
				Node internalNode = this.internalNodes.get(this.internalNodeIndicesOfChanges[position][i]);
				if(internalNode.getName() != null) {
					internalNode.setName(internalNode.getName() + "-" + (position + 1));
				}else {
					internalNode.setName(Integer.toString(position + 1));
				}
			}
		}
		
		// Print the tree to a newick string
		this.tree.print(fileName);
	}
	
	private void printSummary() {
		System.out.println("Identified " + this.inconsistentPositions.size() + " positions with consistency index < 1: ");
		for(int position : this.inconsistentPositions) {
			System.out.println(position + 1);
		}
	}
	
	public void printSummary(String fileName, boolean includeConsistentSites) throws IOException {
		
		// Open the file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Print a summary of the inconsistent positions identified
		WriteToFile.writeLn(bWriter, "Position\tConsistencyIndex\tCountsACGT\tMinimumNumberChangesOnTree");
		
		// Print each position
		String output = "";
		if(includeConsistentSites) {
			for(int position = 0; position < this.nSites; position++) {
				if(areMultipleStatesPresent(this.stateCountsPerPosition[position])) {
					output += (position + 1) + "\t" + this.consistencyIndices[position] + "\t" + 
							Methods.toString(this.stateCountsPerPosition[position], ":") + "\t" + this.minNumberChangesOnTreeAtEachPosition[position] + "\n";
				}else {
					output += (position + 1) + "\t" + 1 + "\t" + Methods.toString(this.stateCountsPerPosition[position], ":") + "\t" + "-" + "\n";
				}				
			}	
		}else {
			for(int position : this.inconsistentPositions) {
				output += (position + 1) + "\t" + this.consistencyIndices[position] + "\t" + 
						Methods.toString(this.stateCountsPerPosition[position], ":") + "\t" + this.minNumberChangesOnTreeAtEachPosition[position] + "\n";
			}			
		}
		WriteToFile.write(bWriter, output);		
		
		// Close the file
		WriteToFile.close(bWriter);
	
	}
	
	public void printSummary(String fileName, ArrayList<int[]> regionCoords, boolean includeConsistentSites) throws IOException {
		
		// Open the file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Print a summary of the inconsistent positions identified
		WriteToFile.writeLn(bWriter, "Start\tEnd\tConsistencyIndex\tCounts\tMinimumNumberChangesOnTree");
		
		// Print each position
		String output = "";
		if(includeConsistentSites) {
			for(int position = 0; position < this.nSites; position++) {
				if(areMultipleStatesPresent(this.stateCountsPerPosition[position])) {
					output += regionCoords.get(position)[0] + "\t" + regionCoords.get(position)[1] + "\t" + 
				              this.consistencyIndices[position] + "\t" + 
							  Methods.toString(this.stateCountsPerPosition[position], ":") + "\t" + 
				              this.minNumberChangesOnTreeAtEachPosition[position] + "\n";
				}else {
					output += regionCoords.get(position)[0] + "\t" + regionCoords.get(position)[1] + "\t" + 1 + "\t" + 
				              Methods.toString(this.stateCountsPerPosition[position], ":") + "\t" + "-" + "\n";
				}				
			}	
		}else {
			for(int position : this.inconsistentPositions) {
				output += regionCoords.get(position)[0] + "\t" + regionCoords.get(position)[1] + "\t" + 
			              this.consistencyIndices[position] + "\t" + Methods.toString(this.stateCountsPerPosition[position], ":") + 
			              "\t" + this.minNumberChangesOnTreeAtEachPosition[position] + "\n";
			}			
		}
		WriteToFile.write(bWriter, output);		
		
		// Close the file
		WriteToFile.close(bWriter);
	
	}
	
	public void printSummary(JTextArea guiTextArea) throws IOException {
				
		// Print a summary of the inconsistent positions identified
		guiTextArea.append("Identified " + this.inconsistentPositions.size() + " positions with consistency index < 1: \n");
		guiTextArea.append("Position\tConsistencyIndex\tCountsACGT\tMinimumNumberChangesOnTree\n");
		
		// Print each position
		String output = "";
		for(int position : this.inconsistentPositions) {
			output += (position + 1) + "\t" + this.consistencyIndices[position] + "\t" + 
					Methods.toString(this.stateCountsPerPosition[position], ":") + "\t" + this.minNumberChangesOnTreeAtEachPosition[position] + "\n";
		}
		guiTextArea.append(output + "\n");
	}
	
	public void printSummary(JTextArea guiTextArea, ArrayList<int[]> regionCoords) throws IOException {
		
		// Print a summary of the inconsistent positions identified
		guiTextArea.append("Identified " + this.inconsistentPositions.size() + " positions with consistency index < 1: \n");
		guiTextArea.append("Start\tEnd\tConsistencyIndex\tCountsACGT\tMinimumNumberChangesOnTree\n");
		
		// Print each position
		String output = "";
		for(int position : this.inconsistentPositions) {
			output += regionCoords.get(position)[0] + "\t" + regionCoords.get(position)[1] + "\t" + 
					"\t" + this.consistencyIndices[position] + "\t" + 
					Methods.toString(this.stateCountsPerPosition[position], ":") + "\t" + this.minNumberChangesOnTreeAtEachPosition[position] + "\n";
		}
		guiTextArea.append(output + "\n");
	}
	
	public int[] getPositions() {
		
		int[] output = new int[this.inconsistentPositions.size()];
		for(int i = 0; i < this.inconsistentPositions.size(); i++) {
			output[i] = this.inconsistentPositions.get(i) + 1;
		}
		
		return output;
	}
	
	// Class specific methods
	public static void calculateConsistencyIndexForEachSiteOnPhylogeny(int nSites, int nTerminalNodes, int nStatesPerSite,
			ArrayList<Sequence> sequences, int[] terminalNodeIndexForEachSequence, int[][] stateCountsPerPosition,
			ArrayList<Node> internalNodes, int[][] internalNodeIndicesOfChanges, int[] minNumberChangesOnTreeAtEachPosition,
			ArrayList<Integer> inconsistentPositions, double[] consistencyIndices, int start, int end) {
		
		// Initialise a hashtable storing a boolean vector - corresponding to states
		Hashtable<Character, boolean[]> stateVectorForEachCharacterState = noteStateVectorForEachCharacter();
		
		// Examine each position in the alignment
		for(int position = start; position <= end; position++) {
			
			// If multithreading the index for storing information will be different than position
			int positionIndex = position - start;
			
			// Count the number of times each state is found at the current position and note terminal node states
			boolean[][] terminalNodeStates = new boolean[nTerminalNodes][nStatesPerSite];
			countAllelesAtEachPositionInSequences(position, positionIndex, terminalNodeStates, nTerminalNodes, nStatesPerSite,
					sequences, terminalNodeIndexForEachSequence, stateCountsPerPosition, 
					stateVectorForEachCharacterState);
			
			// Check if multiple alleles present - i.e. not a constant site
			if(areMultipleStatesPresent(stateCountsPerPosition[positionIndex])) {
				
				// Calculate the minimum number of changes of each position on the phylogeny
				calculateMinimumNumberOfChangesOnPhylogeny(positionIndex, nStatesPerSite,
						internalNodes, terminalNodeStates, internalNodeIndicesOfChanges, minNumberChangesOnTreeAtEachPosition,
						start, end);
				
				// Calculate the consistency index
				checkIfInconsistent(position, positionIndex, inconsistentPositions, consistencyIndices, stateCountsPerPosition,
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
	
	public static Hashtable<Character, boolean[]> noteStateVectorForEachCharacter(){
		
		Hashtable<Character, boolean[]> possibleStatesForEachCharacter = new Hashtable<Character, boolean[]>();
		
		// NUCLEOTIDES
		boolean[] possibleForA = {true, false, false, false};
		possibleStatesForEachCharacter.put('A', possibleForA);
		possibleStatesForEachCharacter.put('a', possibleForA);
		boolean[] possibleForC = {false, true, false, false};
		possibleStatesForEachCharacter.put('C', possibleForC);
		possibleStatesForEachCharacter.put('c', possibleForC);
		boolean[] possibleForG = {false, false, true, false};
		possibleStatesForEachCharacter.put('G', possibleForG);
		possibleStatesForEachCharacter.put('g', possibleForG);
		boolean[] possibleForT = {false, false, false, true};
		possibleStatesForEachCharacter.put('T', possibleForT);
		possibleStatesForEachCharacter.put('t', possibleForT);
		boolean[] possibleForN = {true, true, true, true};
		possibleStatesForEachCharacter.put('N', possibleForN);
		possibleStatesForEachCharacter.put('n', possibleForN);
		boolean[] possibleForDash = {true, true, true, true};
		possibleStatesForEachCharacter.put('-', possibleForDash);
		boolean[] possibleForR = {true, false, true, false};
		possibleStatesForEachCharacter.put('R', possibleForR);
		possibleStatesForEachCharacter.put('r', possibleForR);
		boolean[] possibleForY = {false, true, false, true};
		possibleStatesForEachCharacter.put('Y', possibleForY);
		possibleStatesForEachCharacter.put('y', possibleForY);
		
		// PRESENCE/ABSENCE
		boolean[] possibleForZero = {true, false};
		possibleStatesForEachCharacter.put('0', possibleForZero);
		boolean[] possibleForOne = {false, true};
		possibleStatesForEachCharacter.put('1', possibleForOne);
		
		return possibleStatesForEachCharacter;
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
			int[][] internalNodeIndicesOfChanges, int[] minNumberChangesOnTreeAtEachPosition, int start, int end) {
				
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
	
	public static void countAllelesAtEachPositionInSequences(int position, int positionIndex, boolean[][] terminalNodeStates, int nTerminalNodes, int nStatesPerSite,
			ArrayList<Sequence> sequences, int[] terminalNodeIndexForEachSequence, int[][] stateCountsPerPosition, 
			Hashtable<Character, boolean[]> stateVectorForEachState) {
						
		// Build a hashtable to note which count position is for each allele
		Hashtable<Character, Integer> statePositions = new Hashtable<Character, Integer>();
		statePositions.put('A', 0);
		statePositions.put('a', 0);
		statePositions.put('C', 1);
		statePositions.put('c', 1);
		statePositions.put('G', 2);
		statePositions.put('g', 2);
		statePositions.put('T', 3);
		statePositions.put('t', 3);
		
		// Codes for presence/absence
		statePositions.put('0', 0);
		statePositions.put('1', 1);
		
		
		// Examine each sequence
		for(int sequenceIndex = 0; sequenceIndex < sequences.size(); sequenceIndex++) {
			
			// Get the terminal node index of the current sequence
			int terminalNodeIndex = terminalNodeIndexForEachSequence[sequenceIndex];
			
			// Get the current state
			char state = sequences.get(sequenceIndex).getState(position);
				
			// Only examine recognised state characters, skip anything else
			if(statePositions.containsKey(state)) {
					
				// Count the current allele
				stateCountsPerPosition[positionIndex][statePositions.get(state)]++;
			}
			
			// Check if state is one we expect
			if(stateVectorForEachState.containsKey(state)) {
				
				// Note the current state at the current position (for terminal node states) for the terminal node associated with the current sequence
				terminalNodeStates[terminalNodeIndex] = stateVectorForEachState.get(state);
				
			// Throw an error - found unexpected character (not one of: A, a, C, c, G, g, T, t, N, n, -, R, r, Y, y, 0, or 1)
			}else {
				System.err.println((char)27 + "[31mERROR!! Unrecognised character state (" + state + ") at position: " + (sequenceIndex + 1) + (char)27 + "[0m");
				System.exit(0);
			}
		}
	}

	public static void collect(ConsistencyIndexThread[] threads, int[][] stateCountsPerPosition, int[] minNumberChangesOnTreeAtEachPosition,
			int[][] internalNodeIndicesOfChanges, ArrayList<Integer> inconsistentPositions, double[] consistencyIndices, int nSitesPerThread) {
		
		// Input the data collected from each thread
		for(int i = 0; i < threads.length; i++) {
			
			// Retrieve the data for the positions analysed by the current thread
			for(int position = 0; position < threads[i].getNSites(); position++) {
				
				// Calculate the index of the current position from the current thread in the overall data
				int positionIndex = position + (i * nSitesPerThread);
				
				// Store the information calculated for the current position
				stateCountsPerPosition[positionIndex] = threads[i].getStateCountsPerPosition()[position];
				minNumberChangesOnTreeAtEachPosition[positionIndex] = threads[i].getMinNumberChangesOnTreeAtEachPosition()[position];
				internalNodeIndicesOfChanges[positionIndex] = threads[i].getInternalNodeIndicesOfChanges()[position];
				consistencyIndices[positionIndex] = threads[i].getConsistencyIndices()[position]; // Consistency index of each site
			}
			
			// Store the inconsistent positions found by the current thread
			addInconsistentPositionsFromThread(inconsistentPositions, threads[i]);
		}
	}
	
	public static void addInconsistentPositionsFromThread(ArrayList<Integer> inconsistentPositions, ConsistencyIndexThread thread) {
		
		for(int position : thread.getInconsistentPositions()) {
			inconsistentPositions.add(position);
		}
	}
	
	public static void calculateConsistencyIndicesUsingMultipleThreads(int[][] stateCountsPerPosition, int[] minNumberChangesOnTreeAtEachPosition,
			int[][] internalNodeIndicesOfChanges, ArrayList<Integer> inconsistentPositions,	double[] consistencyIndices, int nSites, 
			ArrayList<Sequence> sequences, int[] terminalNodeIndexForEachSequence, ArrayList<Node> internalNodes, int nStatesPerSite,
			int nTerminalNodes) {
		
		// Find out the number of threads available
		int nThreads = Runtime.getRuntime().availableProcessors();

		// Initialise an array to store the thread objects
		ConsistencyIndexThread[] threads = new ConsistencyIndexThread[nThreads];
		
		// Calculate the number of sites to assign to each thread
		int nSitesPerThread = nSites / nThreads;
		
		// Start the threads
		for(int i = 0; i < nThreads; i++) {
			
			// Calculate the start and end of the current subset of positions to assign to the current thread
			int start = (i * nSitesPerThread);
			int end = start + (nSitesPerThread - 1);
			if(end > nSites - 1 || i == nThreads - 1) {
				end = nSites - 1;
			}
			
			// Create the current thread with the necessary data
			threads[i] = new ConsistencyIndexThread("thread-" + i, start, end, nTerminalNodes, sequences, terminalNodeIndexForEachSequence,
					internalNodes, nStatesPerSite);
			
			// Start the current thread
			threads[i].start();
		}
		
		// Check the threads are finished
		ConsistencyIndexThread.waitUntilAllFinished(threads);
		
		// Collect the data calculated on each thread
		collect(threads, stateCountsPerPosition, minNumberChangesOnTreeAtEachPosition, internalNodeIndicesOfChanges,
				inconsistentPositions, consistencyIndices, nSitesPerThread);
	}
	
	private void noteTerminalNodeIndexAssociatedWithEachSequence(int nTerminalNodes, ArrayList<Sequence> sequences,
			ArrayList<Node> terminalNodes){
		
		// Check there are the same number of terminal nodes in the tree file as there are sequences in the FASTA file
		if(nTerminalNodes != sequences.size()) {
			System.err.println((char)27 + "[31mERROR!! The number of tips in the tree file (" + nTerminalNodes + 
					") isn't equal to the number of sequences (" + sequences.size() + ") provided!" + (char)27 + "[0m");
			System.err.println((char)27 + "[31m        Also note the sequence and tip IDs must match exactly." + (char)27 + "[0m");
			System.exit(0);
		}
		
		// Initialise a hashtable to store the indices of each terminal node
		Hashtable<String, Integer> indices = new Hashtable<String, Integer>();
		
		// Store the index of terminal node
		for(int i = 0; i < nTerminalNodes; i++) {
			indices.put(terminalNodes.get(i).getName(), i);
		}
		
		// Initialise an array to store the terminal node index associated with sequence
		this.terminalNodeIndexForEachSequence = new int[sequences.size()];
				
		// Examine each terminal node and notes its sequence index
		for(int i = 0; i < sequences.size(); i++) {

			if(indices.containsKey(sequences.get(i).getName())) {
				this.terminalNodeIndexForEachSequence[i] = indices.get(sequences.get(i).getName());
			}else {
				System.err.println((char)27 + "[31mERROR!! The following sequence name: \"" + sequences.get(i).getName() + "\" isn't present as a tip label in the newick tree file." + (char)27 + "[0m");
				System.err.println((char)27 + "[31m        The sequence and tip IDs must match exactly." + (char)27 + "[0m");
				System.exit(0);
			}			
		}
	}
	
	private void storeTree(Tree tree) {
		
		// Store the tree
		this.tree = tree;
		
		// Get the internal and terminal nodes from the tree
		this.internalNodes = this.tree.getInternalNodes();
		this.nInternalNodes = this.internalNodes.size();
		this.terminalNodes = this.tree.getTerminalNodes();
		this.nTerminalNodes = this.terminalNodes.size();
	}
	
	private void storeSequences(ArrayList<Sequence> sequences) {
		
		// Store the sequences and their length
		this.sequences = sequences;
		this.nSites = this.sequences.get(0).length;
		
		// Note the terminal node index associated with each sequence
		noteTerminalNodeIndexAssociatedWithEachSequence(this.nTerminalNodes, this.sequences,
				this.terminalNodes);
	}
}
