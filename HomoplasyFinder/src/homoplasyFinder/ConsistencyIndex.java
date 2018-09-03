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
	private Sequence[] sequences;
	private int nSites;
	private int nStatesPerSite;
	private int[] terminalNodeIndexForEachSequence;
	
	// Consistency index
	private int[][] stateCountsPerPosition;
	private int[] minNumberChangesOnTreeAtEachPosition;
	private int[][] internalNodeIndicesOfChanges;
	private ArrayList<Integer> inconsistentPositions;
	private double[] consistencyIndices;
	private boolean[][][] terminalNodeStates;
	
	public ConsistencyIndex(Tree tree, Sequence[] sequences, boolean verbose) {
		
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
		 */
		
		// Note whether verbose outputs wanter
		this.verbose = verbose;
		
		// Store the tree and sequences
		storeTree(tree);
		storeSequences(sequences);
		
		// Count the number of times each nucleotide is found at each positions
		countAllelesAtEachPositionInSequences();
		
		// Calculate the minimum number of changes of each position on the phylogeny
		calculateMinimumNumberOfChangesOnPhylogeny();
		//calculateMinimumNumberOfChangesOnPhylogenyMultiThread();
		
		// Identify the inconsistent positions
		identifyInconsistentPositions();
		
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
			fileName = "sequences_withoutHomoplasies_" + this.date + ".fasta";
		}
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Print out the number of isolates and sites in FASTA
		int newSequenceLength = this.nSites - this.inconsistentPositions.size();
		WriteToFile.writeLn(bWriter, sequences.length + " " + newSequenceLength);
		
		// Initialise an array to each isolate sequence data
		char[] sequence = new char[newSequenceLength];
		
		// Write out each of the sequences
		for(int i = 0; i < sequences.length; i++){
			
			// Print sequence ID
			WriteToFile.writeLn(bWriter, ">" + sequences[i].getName());
			
			// Print sequence
			sequence = Methods.deletePositions(sequences[i].getSequence(), positionsToIgnore);
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
	
	public void printSummary(String fileName) throws IOException {
		
		// Open the file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Print a summary of the inconsistent positions identified
		WriteToFile.writeLn(bWriter, "Position\tConsistencyIndex\tCountsACGT\tMinimumNumberChangesOnTree");
		
		// Print each position
		String output = "";
		for(int position : this.inconsistentPositions) {
			output += (position + 1) + "\t" + this.consistencyIndices[position] + "\t" + 
					Methods.toString(this.stateCountsPerPosition[position], ":") + "\t" + this.minNumberChangesOnTreeAtEachPosition[position] + "\n";
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
	
	public int[] getPositions() {
		
		int[] output = new int[this.inconsistentPositions.size()];
		for(int i = 0; i < this.inconsistentPositions.size(); i++) {
			output[i] = this.inconsistentPositions.get(i) + 1;
		}
		
		return output;
	}
	
	// Class specific methods
	private Hashtable<Character, boolean[]> noteStateVectorForEachNucleotide(){
		
		Hashtable<Character, boolean[]> possibleNucleotidesForEachNucleotide = new Hashtable<Character, boolean[]>();
		boolean[] possibleForA = {true, false, false, false};
		possibleNucleotidesForEachNucleotide.put('A', possibleForA);
		boolean[] possibleForC = {false, true, false, false};
		possibleNucleotidesForEachNucleotide.put('C', possibleForC);
		boolean[] possibleForG = {false, false, true, false};
		possibleNucleotidesForEachNucleotide.put('G', possibleForG);
		boolean[] possibleForT = {false, false, false, true};
		possibleNucleotidesForEachNucleotide.put('T', possibleForT);
		boolean[] possibleForN = {true, true, true, true};
		possibleNucleotidesForEachNucleotide.put('N', possibleForN);
		boolean[] possibleForDash = {true, true, true, true};
		possibleNucleotidesForEachNucleotide.put('-', possibleForDash);
		boolean[] possibleForR = {true, false, true, false};
		possibleNucleotidesForEachNucleotide.put('R', possibleForR);
		boolean[] possibleForY = {false, true, false, true};
		possibleNucleotidesForEachNucleotide.put('Y', possibleForY);
		
		return possibleNucleotidesForEachNucleotide;
	}
	
 	private void identifyInconsistentPositions() {
		
		// Initialise an array to store the inconsistent sites (consistency < 1)
		this.inconsistentPositions = new ArrayList<Integer>();
		
		// Initialise an array to store the consistency index of each site
		this.consistencyIndices = new double[this.nSites];
		
		// Examine each position
		for(int pos = 0; pos < this.nSites; pos++) {
			
			// Calculate the consistency index
			this.consistencyIndices[pos] = calculateConsistencyIndexForPosition(this.stateCountsPerPosition[pos], 
					this.minNumberChangesOnTreeAtEachPosition[pos]);
			
			// Report the position if, the consistency index is less than 1
			if(this.consistencyIndices[pos] < 1) {
				
				this.inconsistentPositions.add(pos);
			}			
		}
		
		if(this.verbose) {
			System.out.println("Identified " + this.inconsistentPositions.size() + " positions with consistency index < 1: ");
			for(int position : this.inconsistentPositions) {
				System.out.println(position + 1);
			}
		}
	}
	
	public static double calculateConsistencyIndexForPosition(int[] alleleCounts, int minNumberChangesOnPhylogeny) {
		
		// Initialise a variable to record how many alleles are present
		int count = 0;
		
		// Examine each alleles count, how many are more than 0?
		for(int i = 0; i < 4; i++) {
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
	
	private void calculateMinimumNumberOfChangesOnPhylogeny() {
				
		// Initialise an array to store the minimum number of changes for each position
		this.minNumberChangesOnTreeAtEachPosition = new int[this.nSites];
		
		// Initialise an array to store the internal node indices where those changes occur
		this.internalNodeIndicesOfChanges = new int[this.nSites][this.tree.getNInternalNodes()];
		
		// Initialise an array to store the possible nucleotides for each position assigned to each internal node
		boolean[][][] possibleStatesForEachInternalNode = new boolean[this.nInternalNodes][this.nSites][this.nStatesPerSite];
		
		// Starting at the first terminal node's ancestor, visit all the internal nodes
		identifyPossibleStatesForInternalNode(0, possibleStatesForEachInternalNode);
	}

	private void identifyPossibleStatesForInternalNode(int internalNodeIndex, boolean[][][] possibleStatesForEachInternalNode) {
		
		// Get the sub-node information for the current internal node
		ArrayList<Integer> subNodeIndices = this.internalNodes.get(internalNodeIndex).getSubNodeIndices();
		ArrayList<Boolean> subNodeTypes = this.internalNodes.get(internalNodeIndex).getSubNodeTypes();
		
		// Initialise an array to store the possible states assigned for each position, for each sub-node
		boolean[][][] possibleStatesForSubNodes = new boolean[subNodeIndices.size()][this.nSites][this.nStatesPerSite];
		
		// Examine each sub-node
		for(int i = 0; i < subNodeIndices.size(); i++) {
			
			// Check whether the current node is internal
			if(subNodeTypes.get(i)) {
			
				// Identify the possible states for the current internal sub-node
				identifyPossibleStatesForInternalNode(subNodeIndices.get(i), possibleStatesForEachInternalNode);
				
				// Get the possible nucleotides assigned to this internal sub-node
				possibleStatesForSubNodes[i] = possibleStatesForEachInternalNode[subNodeIndices.get(i)];
				
			// If it is terminal, get the nucleotides assigned at each position
			}else {
				possibleStatesForSubNodes[i] = this.terminalNodeStates[subNodeIndices.get(i)];
			}
		}
		
		// Determine the possible nucleotides for the current node, based on those of the sub-nodes
		possibleStatesForEachInternalNode[internalNodeIndex] = getPossibleStateForInternalNodeFromPossibleStatesOfSubNodes(possibleStatesForSubNodes,
				internalNodeIndex);
	}
	
	private boolean[][] getPossibleStateForInternalNodeFromPossibleStatesOfSubNodes(boolean[][][] possibleNucleotidesForSubNodes,
			int internalNodeIndex){
		
		// Get the possible nucleotides for the first subNode
		boolean[][] possibleNucleotides = possibleNucleotidesForSubNodes[0];
		
		// Examine the other sub-nodes
		for(int i = 1; i < possibleNucleotidesForSubNodes.length; i++) {
			
			// Examine each of the positions
			for(int pos = 0; pos < this.nSites; pos++) {
				
				// Compare the current sub-nodes possible nucleotides to those stored
				possibleNucleotides[pos] =  comparePossibleNucleotidesOfTwoSubNodes(possibleNucleotides[pos], possibleNucleotidesForSubNodes[i][pos],
						pos, internalNodeIndex);
			}
		}
		
		return possibleNucleotides;		
	}
	
	private boolean[] comparePossibleNucleotidesOfTwoSubNodes(boolean[] a, boolean[] b, int position, int internalNodeIndex) {
		
		// Initialise a vector to store the possible nucleotides found in both a and b (union)
		boolean[] union = new boolean[4];
		
		// Initialise a vector to store the possible nucleotides found only in a or b (intersect)
		boolean[] intersect = new boolean[4];
		
		// Initialise a variable to record whether common possible nucleotides found
		boolean common = false;
		
		// Examine the possible nucleotides
		for(int i = 0; i < 4; i++) {
			
			// Check for a common nucleotide
			if(a[i] == true && b[i] == true) {
				intersect[i] = true;
				common = true;
				
			// Check for a non-common nucleotide
			}else if(a[i] != b[i]){
				union[i] = true;
			}
		}
		
		// If common possible nucleotides found, return the intersect (only those common nucleotides)
		if(common) {
			return intersect;
			
		// Else return the union (all possible nucleotides) and increment the tree length for the current position
		}else {
			this.internalNodeIndicesOfChanges[position][this.minNumberChangesOnTreeAtEachPosition[position]] = internalNodeIndex;
			this.minNumberChangesOnTreeAtEachPosition[position]++;
			return union;
		}
	}
	
	private void noteTerminalNodeIndexAssociatedWithEachSequence(){
		
		// Initialise a hashtable to store the indices of each terminal node
		Hashtable<String, Integer> indices = new Hashtable<String, Integer>();
		
		// Store the index of terminal node
		for(int i = 0; i < this.nTerminalNodes; i++) {
			indices.put(this.terminalNodes.get(i).getName(), i);
		}
		
		// Initialise an array to store the terminal node index associated with sequence
		this.terminalNodeIndexForEachSequence = new int[this.sequences.length];
		
		// Examine each terminal node and notes its sequence index
		for(int i = 0; i < this.sequences.length; i++) {

			this.terminalNodeIndexForEachSequence[i] = indices.get(this.sequences[i].getName());
		}
	}
	
	private void countAllelesAtEachPositionInSequences() {
		
		// Initialise a hashtable storing a boolean vector - corresponding to nucleotides
		Hashtable<Character, boolean[]> stateVectorForEachNucleotide = noteStateVectorForEachNucleotide();
		
		// Set the number of states per site to 4
		this.nStatesPerSite = 4;
		
		// Initialise an array to store the allele counts
		this.stateCountsPerPosition = new int[this.nSites][this.nStatesPerSite]; // Counts for each possible (A, C, G, T) at each position
		
		// Initialise an array to store the possible state at each site at each terminal node
		this.terminalNodeStates = new boolean[this.nTerminalNodes][this.nSites][this.nStatesPerSite];
		
		// Build a hashtable to note which count position is for each allele
		Hashtable<Character, Integer> nucleotidePositions = new Hashtable<Character, Integer>();
		nucleotidePositions.put('A', 0);
		nucleotidePositions.put('C', 1);
		nucleotidePositions.put('G', 2);
		nucleotidePositions.put('T', 3);
				
		// Examine each sequence
		for(int sequenceIndex = 0; sequenceIndex < this.sequences.length; sequenceIndex++) {
			
			// Get the terminal node index of the current sequence
			int terminalNodeIndex = this.terminalNodeIndexForEachSequence[sequenceIndex];
			
			// Examine each position in the current sequence
			for(int pos = 0; pos < this.sequences[0].getLength(); pos++) {
			
				// Get the current nucleotide
				char nucleotide = this.sequences[sequenceIndex].getNucleotide(pos);
				
				// Only examine nucleotide characters, skip anything else
				if(nucleotidePositions.containsKey(nucleotide)) {
					
					// Count the current allele
					this.stateCountsPerPosition[pos][nucleotidePositions.get(nucleotide)]++;
				}
				
				// Note the current state at the current position (for terminal node states) for the terminal node associated with the current sequence
				this.terminalNodeStates[terminalNodeIndex][pos] = stateVectorForEachNucleotide.get(nucleotide);
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
	
	private void storeSequences(Sequence[] sequences) {
		
		// Store the sequences and their length
		this.sequences = sequences;
		this.nSites = this.sequences[0].length;
		
		// Note the terminal node index associated with each sequence
		noteTerminalNodeIndexAssociatedWithEachSequence();
	}
}
