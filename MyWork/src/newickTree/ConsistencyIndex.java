package newickTree;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import geneticDistances.Sequence;
import identifyingHomoplasies.MultiThreadPositions;
import methods.ArrayListMethods;
import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.WriteToFile;

public class ConsistencyIndex {

	// Current date
	private String date = CalendarMethods.getCurrentDate("dd-MM-yy");
	
	// Verbose print outs?
	private boolean verbose;
	
	// Tree
	private Tree tree;
	private ArrayList<Node> internalNodes;
	private int nInternalNodes;
	private ArrayList<Node> terminalNodes;
	
	// Sequences
	private Sequence[] sequences;
	private int sequenceLength;
	private Hashtable<String, Integer> sequenceIndices;
	
	// Consistency index
	private int[][] nucleotideCountsPerPosition;
	private int[] minNumberChangesOnTreeAtEachPosition;
	private int[][] internalNodeIndicesOfChanges;
	private ArrayList<Integer> inconsistentPositions;
	
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
		countAllelesAtEachPosition();
		
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
		Hashtable<Integer, Integer> positionsToIgnore = ArrayListMethods.indexArrayListInteger(this.inconsistentPositions);
		
		// Open an output file
		if(fileName == null){
			fileName = "sequences_withoutHomoplasies_" + this.date + ".fasta";
		}
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Print out the number of isolates and sites in FASTA
		int newSequenceLength = this.sequenceLength - this.inconsistentPositions.size();
		WriteToFile.writeLn(bWriter, sequences.length + " " + newSequenceLength);
		
		// Initialise an array to each isolate sequence data
		char[] sequence = new char[newSequenceLength];
		
		// Write out each of the sequences
		for(int i = 0; i < sequences.length; i++){
			
			// Print sequence ID
			WriteToFile.writeLn(bWriter, ">" + sequences[i].getName());
			
			// Print sequence
			sequence = ArrayMethods.deletePositions(sequences[i].getSequence(), positionsToIgnore);
			WriteToFile.writeLn(bWriter, ArrayMethods.toString(sequence));
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
					internalNode.setName(internalNode.getName() + "-" + position);
				}else {
					internalNode.setName(Integer.toString(position));
				}
			}
		}
		
		// Print the tree to a newick string
		this.tree.print(fileName);
	}
	
	public void printSummary(String fileName) throws IOException {
		
		// Check if file name provided
		if(fileName == null) {
			System.out.println("Identified " + this.inconsistentPositions.size() + " positions with consistency index < 1: ");
			System.out.println(ArrayListMethods.toStringInt(this.inconsistentPositions, "\n"));
		}else {
			
			// Open the file
			BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
			
			// Print a summary of the inconsistent positions identified
			WriteToFile.writeLn(bWriter, "Identified " + this.inconsistentPositions.size() + " positions with consistency index < 1: ");
			WriteToFile.writeLn(bWriter, ArrayListMethods.toStringInt(this.inconsistentPositions, "\n"));
			
			// Close the file
			WriteToFile.close(bWriter);
		}	
	}
	
	// Class specific methods
	private Hashtable<Character, boolean[]> notePossibleNucleotidesForNucleotide(){
		
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
		
		// Examine each position
		for(int pos = 0; pos < this.sequenceLength; pos++) {
			
			// Calculate the consistency index
			double consistency = calculateConsistencyIndexForPosition(this.nucleotideCountsPerPosition[pos], 
					this.minNumberChangesOnTreeAtEachPosition[pos]);
			
			// Report the position if, the consistency index is less than 1
			if(consistency < 1) {
				
				this.inconsistentPositions.add(pos);
			}			
		}
		
		if(this.verbose) {
			System.out.println("Identified " + this.inconsistentPositions.size() + " positions with consistency index < 1: ");
			System.out.println(ArrayListMethods.toStringInt(this.inconsistentPositions, "\n"));
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
				
		// Initialise a hashtable storing a boolean vector - corresponding to nucleotides
		Hashtable<Character, boolean[]> possibleNucleotidesForEachNucleotide = notePossibleNucleotidesForNucleotide();
		
		// Initialise an array to store the minimum number of changes for each position
		this.minNumberChangesOnTreeAtEachPosition = new int[this.sequenceLength];
		
		// Initialise an array to store the internal node indices where those changes occur
		this.internalNodeIndicesOfChanges = new int[this.sequenceLength][this.tree.getNInternalNodes()];
		
		// Initialise an array to record which of the internal nodes have been visited
		boolean[] visited = new boolean[this.nInternalNodes]; // Defaults to falses
		
		// Initialise an array to store the possible nucleotides for each position assigned to each internal node
		boolean[][][] possibleNucleotides = new boolean[this.nInternalNodes][this.sequenceLength][4];
		
		// Starting at the first terminal node's ancestor, visit all the internal nodes
		identifyPossibleNucleotides(0, visited, possibleNucleotides, possibleNucleotidesForEachNucleotide);
	}

	private void identifyPossibleNucleotides(int internalNodeIndex, boolean[] visited, boolean[][][] possibleNucleotides,
			Hashtable<Character, boolean[]> possibleNucleotidesForEachNucleotide) {
		
		// Get the sub-node information for the current internal node
		ArrayList<Integer> subNodeIndices = this.internalNodes.get(internalNodeIndex).getSubNodeIndices();
		ArrayList<Boolean> subNodeTypes = this.internalNodes.get(internalNodeIndex).getSubNodeTypes();
		
		// Initialise an array to store the possible nucleotides assigned for each position, for each sub-node
		boolean[][][] possibleNucleotidesForSubNodes = new boolean[subNodeIndices.size()][this.sequenceLength][4];
		
		// Examine each sub-node
		for(int i = 0; i < subNodeIndices.size(); i++) {
			
			// Check whether the current node is internal
			if(subNodeTypes.get(i)) {
			
				// Check if this internal sub-node has been visited
				if(visited[subNodeIndices.get(i)] == false) {
					identifyPossibleNucleotides(subNodeIndices.get(i), visited, possibleNucleotides, possibleNucleotidesForEachNucleotide);
				}
				
				// Get the possible nucleotides assigned to this internal sub-node
				possibleNucleotidesForSubNodes[i] = possibleNucleotides[subNodeIndices.get(i)];
				
			// If it is terminal, get the nucleotides assigned at each position
			}else {
				possibleNucleotidesForSubNodes[i] = getNucleotidesAssigned(this.terminalNodes.get(subNodeIndices.get(i)).getName(),
						possibleNucleotidesForEachNucleotide);
			}
		}
		
		// Determine the possible nucleotides for the current node, based on those of the sub-nodes
		possibleNucleotides[internalNodeIndex] = identifyPossibleNucleotidesAcrossAllSubNodesForInternalNode(possibleNucleotidesForSubNodes,
				internalNodeIndex);
		
		// Note that the current internal node has been visited
		visited[internalNodeIndex] = true;
	}
	
	private boolean[][] identifyPossibleNucleotidesAcrossAllSubNodesForInternalNode(boolean[][][] possibleNucleotidesForSubNodes,
			int internalNodeIndex){
		
		// Get the possible nucleotides for the first subNode
		boolean[][] possibleNucleotides = possibleNucleotidesForSubNodes[0];
		
		// Examine the other sub-nodes
		for(int i = 1; i < possibleNucleotidesForSubNodes.length; i++) {
			
			// Examine each of the positions
			for(int pos = 0; pos < this.sequenceLength; pos++) {
				
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
	
	private boolean[][] getNucleotidesAssigned(String sequenceName, Hashtable<Character, boolean[]> possibleNucleotidesForEachNucleotide){
		
		// Get the corresponding sequence
		char[] sequence = this.sequences[this.sequenceIndices.get(sequenceName)].getSequence();
		
		// Initialise an array recording which nucleotides are present at each position
		boolean[][] nucleotidesPresent = new boolean[this.sequenceLength][4];
		
		// Examine each position
		for(int pos = 0; pos < this.sequenceLength; pos++) {
			
			nucleotidesPresent[pos] = possibleNucleotidesForEachNucleotide.get(sequence[pos]);
		}
		
		return nucleotidesPresent;
	}
	
	private Hashtable<String, Integer> indexSequences(Sequence[] sequences){
		
		// Initialise a hashtable to store the indices
		Hashtable<String, Integer> indices = new Hashtable<String, Integer>();
		
		// Store the index of sequence sequence
		for(int i = 0; i < sequences.length; i++) {
			indices.put(sequences[i].getName(), i);
		}
		
		return indices;
	}
	
	private void countAllelesAtEachPosition() {
		
		// Initialise an array to store the allele counts
		this.nucleotideCountsPerPosition = new int[this.sequenceLength][4]; // Counts for each possible (A, C, G, T) at each position
		
		// Build a hashtable to note which count position is for each allele
		Hashtable<Character, Integer> nucleotidePositions = new Hashtable<Character, Integer>();
		nucleotidePositions.put('A', 0);
		nucleotidePositions.put('C', 1);
		nucleotidePositions.put('G', 2);
		nucleotidePositions.put('T', 3);
		
		// Examine each position
		for(int pos = 0; pos < this.sequences[0].getLength(); pos++) {
			
			// Examine each isolates allele at the current position
			for(int sequenceIndex = 0; sequenceIndex < this.sequences.length; sequenceIndex++) {
				
				// Count the current allele
				this.nucleotideCountsPerPosition[pos][nucleotidePositions.get(this.sequences[sequenceIndex].getNucleotide(pos))]++;
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
	}
	
	private void storeSequences(Sequence[] sequences) {
		
		// Store the sequences and their length
		this.sequences = sequences;
		this.sequenceLength = this.sequences[0].length;
		
		// Index the sequences
		this.sequenceIndices = indexSequences(sequences);
	}
}
