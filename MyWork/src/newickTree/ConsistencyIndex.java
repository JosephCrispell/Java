package newickTree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import geneticDistances.Sequence;
import methods.ArrayMethods;
import methods.GeneticMethods;
import methods.HashtableMethods;

public class ConsistencyIndex {

	
	public static void main(String[] args) throws IOException {
		
		long start = System.nanoTime();
		
		// Set the path
		String path = "/home/josephcrispell/Desktop/Research/Homoplasy/";
		
		// Read the NEWICK tree and store as a traversable node set
		Tree tree = new Tree(path + "example-AFTER_31-07-18.tree");

		// Read in the FASTA sequences
		Sequence[] sequences = GeneticMethods.readFastaFile(path + "example_31-07-18.fasta", false);
		
		// Count the number of times each nucleotide is found at each positions
		int[][] counts = countAlellesAtEachPosition(sequences);
		
		// Calculate the minimum number of changes of each position on the phylogeny
		int[] minNumberChangesForEachPosition = calculateMinimumNumberOfChangesOnPhylogeny(tree, sequences, counts);
		
		// Identify inconsistent sites
		identifyInconsistentPositions(counts, minNumberChangesForEachPosition);
		
		long end = System.nanoTime();
		System.out.println("\nTime taken (seconds): " + (double)(end - start)/1000000000.0);
	}
	
	public static void identifyInconsistentPositions(int[][] alleleCountsForEachPosition, int[] minNumberChangesForEachPosition) {
		
		// Examine each position
		for(int pos = 0; pos < alleleCountsForEachPosition.length; pos++) {
			
			// Calculate the consistency index
			double consistency = calculateConsistencyIndexForPosition(alleleCountsForEachPosition[pos], minNumberChangesForEachPosition[pos]);
			
			// Report the position if, the consistency index is less than 1
			if(consistency < 1) {
				System.out.println("############################################################################");
				System.out.println("Position: " + (pos + 1));
				System.out.println("Allele counts: ");
				System.out.println("\tA:" + alleleCountsForEachPosition[pos][0]);
				System.out.println("\tC:" + alleleCountsForEachPosition[pos][1]);
				System.out.println("\tG:" + alleleCountsForEachPosition[pos][2]);
				System.out.println("\tT:" + alleleCountsForEachPosition[pos][3]);
				System.out.println("Minimum number of changes on tree: " + minNumberChangesForEachPosition[pos]);
				System.out.println("Consistency index: " + consistency);
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
	
	public static int[] calculateMinimumNumberOfChangesOnPhylogeny(Tree tree, Sequence[] sequences, int[][] counts) {
		
		// Initialise a nucleotide index
		Hashtable<Character, Integer> nucleotideIndices = new Hashtable<Character, Integer>();
		nucleotideIndices.put('A', 0);
		nucleotideIndices.put('C', 1);
		nucleotideIndices.put('G', 2);
		nucleotideIndices.put('T', 3);
		
		// Get the internal and terminal nodes from the tree
		ArrayList<Node> internalNodes = tree.getInternalNodes();
		ArrayList<Node> terminalNodes = tree.getTerminalNodes();
		
		// Initialise an array to record the minimum number of changes for each position
		int seqLength = sequences[0].getLength();
		int[] minNumberChanges = new int[seqLength];
		
		// Initialise an array to record which of the internal nodes have been visited
		boolean[] visited = new boolean[internalNodes.size()]; // Defaults to falses
		
		// Initialise an array to store the possible nucleotides for each position assigned to each internal node
		boolean[][][] possibleNucleotides = new boolean[internalNodes.size()][seqLength][4];
		
		// Index the array of sequences by their name
		Hashtable<String, Integer> sequenceIndices = indexSequences(sequences);
		
		// Starting at the first terminal node's ancestor, visit all the internal nodes
		identifyPossibleNucleotides(0, internalNodes, terminalNodes, minNumberChanges, visited, possibleNucleotides,
				nucleotideIndices, sequenceIndices, seqLength, sequences);
		
		return minNumberChanges;
	}
	
	public static void identifyPossibleNucleotides(int internalNodeIndex, ArrayList<Node> internalNodes, ArrayList<Node> terminalNodes,
			int[] minNumberChanges, boolean[] visited,	boolean[][][] possibleNucleotides, Hashtable<Character, Integer> nucleotideIndices,
			Hashtable<String, Integer> sequenceIndices, int seqLength, Sequence[] sequences) {
		
		// Get the sub-node information for the current internal node
		ArrayList<Integer> subNodeIndices = internalNodes.get(internalNodeIndex).getSubNodeIndices();
		ArrayList<Boolean> subNodeTypes = internalNodes.get(internalNodeIndex).getSubNodeTypes();
		
		// Initialise an array to store the possible nucleotides assigned for each position, for each sub-node
		boolean[][][] possibleNucleotidesForSubNodes = new boolean[subNodeIndices.size()][seqLength][4];
		
		// Examine each sub-node
		for(int i = 0; i < subNodeIndices.size(); i++) {
			
			// Check whether the current node is internal
			if(subNodeTypes.get(i)) {
			
				// Check if this internal sub-node has been visited
				if(visited[subNodeIndices.get(i)] == false) {
					identifyPossibleNucleotides(subNodeIndices.get(i), internalNodes, terminalNodes, minNumberChanges, visited,
							possibleNucleotides, nucleotideIndices, sequenceIndices, seqLength, sequences);
				}
				
				// Get the possible nucleotides assigned to this internal sub-node
				possibleNucleotidesForSubNodes[i] = possibleNucleotides[subNodeIndices.get(i)];
				
			// If it is terminal, get the nucleotides assigned at each position
			}else {
				possibleNucleotidesForSubNodes[i] = getNucleotidesAssigned(
						sequences[sequenceIndices.get(terminalNodes.get(subNodeIndices.get(i)).getName())].getSequence(),
						nucleotideIndices);
			}
		}
		
		// Determine the possible nucleotides for the current node, based on those of the sub-nodes
		possibleNucleotides[internalNodeIndex] = identifyPossibleNucleotidesForInternalNode(possibleNucleotidesForSubNodes, minNumberChanges);
		
		// Note that the current internal node has been visited
		visited[internalNodeIndex] = true;
	}
	
	public static boolean[][] identifyPossibleNucleotidesForInternalNode(boolean[][][] possibleNucleotidesForSubNodes, int[] minNumberChanges){
		
		// Get the possible nucleotides for the first subNode
		boolean[][] possibleNucleotides = possibleNucleotidesForSubNodes[0];
		
		// Examine the other sub-nodes
		for(int i = 1; i < possibleNucleotidesForSubNodes.length; i++) {
			
			// Examine each of the positions
			for(int pos = 0; pos < possibleNucleotidesForSubNodes[i].length; pos++) {
				
				// Compare the current sub-nodes possible nucleotides to those stored
				possibleNucleotides[pos] =  comparePossibleNucleotides(possibleNucleotides[pos], possibleNucleotidesForSubNodes[i][pos], pos, 
						minNumberChanges);
			}
		}
		
		return possibleNucleotides;		
	}
	
	public static boolean[] comparePossibleNucleotides(boolean[] a, boolean[] b, int position, int[] minNumberChanges) {
		
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
			minNumberChanges[position]++;
			return union;
		}
	}
	
	public static boolean[][] getNucleotidesAssigned(char[] sequence, Hashtable<Character, Integer> nucleotideIndex){
		
		// Initialise an array recording which nucleotides are present at each position
		boolean[][] nucleotidesPresent = new boolean[sequence.length][4];
		
		// Examine each position
		for(int pos = 0; pos < sequence.length; pos++) {
			nucleotidesPresent[pos][nucleotideIndex.get(sequence[pos])] = true;
		}
		
		return nucleotidesPresent;
	}
	
	public static Hashtable<String, Integer> indexSequences(Sequence[] sequences){
		
		// Initialise a hashtable to store the indices
		Hashtable<String, Integer> indices = new Hashtable<String, Integer>();
		
		// Store the index of sequence sequence
		for(int i = 0; i < sequences.length; i++) {
			indices.put(sequences[i].getName(), i);
		}
		
		return indices;
	}
	
	public static int[][] countAlellesAtEachPosition(Sequence[] sequences) {
		
		// Initialise an array to store the allele counts
		int[][] counts = new int[sequences[0].getLength()][4]; // Counts for each possible (A, C, G, T) at each position
		
		// Build a hashtable to note which count position is for each allele
		Hashtable<Character, Integer> nucleotidePositions = new Hashtable<Character, Integer>();
		nucleotidePositions.put('A', 0);
		nucleotidePositions.put('C', 1);
		nucleotidePositions.put('G', 2);
		nucleotidePositions.put('T', 3);
		
		// Examine each position
		for(int pos = 0; pos < sequences[0].getLength(); pos++) {
			
			// Examine each isolates allele at the current position
			for(int sequenceIndex = 0; sequenceIndex < sequences.length; sequenceIndex++) {
				
				// Count the current allele
				counts[pos][nucleotidePositions.get(sequences[sequenceIndex].getNucleotide(pos))]++;
			}
		}
		
		return counts;
	}

}
