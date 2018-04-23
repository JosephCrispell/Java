package identifyingHomoplasies;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Hashtable;

import geneticDistances.Sequence;
import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.GeneticMethods;
import methods.HashtableMethods;
import methods.WriteToFile;
import phylogeneticTree.BeastNewickTreeMethods;
import phylogeneticTree.CalculateDistancesToMRCAs;
import phylogeneticTree.Node;
import phylogeneticTree.NodeMethods;

public class HomoplasyFinder2 {

	public static void main(String[] args) throws IOException{
		
		// Set the path
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Homoplasmy/";
				
		// Get the current date
		String date = CalendarMethods.getCurrentDate("dd-MM-yy");
		
		/**
		 * Read in the phylogeny
		 */

		String treeFile = path + "example_06-03-18.tree";
		Node tree = readNewickTree(treeFile);
		
		/**
		 * Read in the FASTA file
		 */
		
		String fasta = path + "example_06-03-18.fasta";
		Hashtable<String, char[]> sequences = storeSequencesInHashtable(GeneticMethods.readFastaFile(fasta));
		
		/**
		 * Assign alleles to branches below root:
		 * - It is present in all isolates under the current node
		 * - Not assigned to any of branches on path to root
		 */
		
		// Assign the alleles present amongst the sequences to branches in the phylogeny (assigned to immediate sub-node)
		assignAllelesToSubBranches(tree, sequences, new Hashtable<String, Integer>());
		
		// Retrieve the branches which each allele were assigned to
		Hashtable<String, Node[]> branchesAllelesAssignedTo = Global.branchesAllelesAssignedTo;
		
		/**
		 * Re-assign alleles on multiple branches to single branch if:
		 * - All isolates above branch have the allele
		 * - All isolates below branch don't have allele
		 */
		
		/**
		 * Note consensus alleles for each position - Method assumes consensus allele is ancestral
		 */
		
		char[] consensus = GeneticMethods.consensus(getSequences(sequences));
		
		/**
		 * Identify potential homoplasy events
		 */
		
		identifyPotentialHomoplasies(branchesAllelesAssignedTo, consensus, date, path);
	}
	
	// Methods Section
	public static BufferedWriter buildOutputFile(String date, String path) throws IOException{
		
		String outputFile = path + "homoplasyReport_" + date + ".txt";
		BufferedWriter bWriter = WriteToFile.openFile(outputFile, false);
		WriteToFile.writeLn(bWriter, "Position\tAllele\tIsolates");
				
		return bWriter;
	}
	
	public static void identifyPotentialHomoplasies(Hashtable<String, Node[]> branchesAllelesAssignedTo,
			char[] consensus, String date, String path) throws IOException{
		
		// Build an output file
		BufferedWriter bWriter = buildOutputFile(date, path);
		
		// Examine each of alleles - have any been assigned to multiple branches?
		for(String allele : HashtableMethods.getKeysString(branchesAllelesAssignedTo)){
					
			// Skip alleles only assigned to a single branch, in consensus, or 'N's
			if(branchesAllelesAssignedTo.get(allele).length < 2 || inConsensus(allele, consensus) == true ||
				allele.matches("(.*):N") == true){
				continue;
			}
					
			// Report the isolates associated with the potential homoplasy identified
			reportPotentialHomoplasy(allele, branchesAllelesAssignedTo.get(allele), bWriter);
		}		
		bWriter.close();
	}
		
	public static void reportPotentialHomoplasy(String allele, Node[] branches,
			BufferedWriter bWriter) throws IOException{
		
		System.out.println("------------------------------------------------------------------------------------------");
		
		// Split the allele into its position and allele
		String[] parts = allele.split(":");
		System.out.println("Potential homoplasy identified at position: " + (Integer.parseInt(parts[0]) + 1) + " with allele: " + parts[1]);
		WriteToFile.write(bWriter, parts[0] + "\t" + parts[1] + "\t");
		
		// Report the isolates associated with each branch
		WriteToFile.write(bWriter, ArrayMethods.toString(getIdsBelowNode(branches[0]), ","));
		System.out.println("Allele associated with " + branches.length + " branches");
		System.out.println("Branch 0\t" + ArrayMethods.toString(getIdsBelowNode(branches[0]), ", "));
		for(int i = 1; i < branches.length; i++){
			WriteToFile.write(bWriter, "," + ArrayMethods.toString(getIdsBelowNode(branches[i]), ","));
			System.out.println("Branch " + i + "\t" + ArrayMethods.toString(getIdsBelowNode(branches[i]), ", "));
		}
		WriteToFile.write(bWriter, "\n");
	}
	
	public static boolean inConsensus(String allele, char[] consensus){
		
		// Split the allele into its position and allele
		String[] parts = allele.split(":");
		int pos = Integer.parseInt(parts[0]);
		char nucleotide = parts[1].toCharArray()[0];
		
		// Check if the allele matches the consensus
		boolean matches = false;
		if(consensus[pos] == nucleotide){
			matches = true;
		}
		
		return matches;
	}
	
	public static char[][] getSequences(Hashtable<String, char[]> sequences){
		
		// Get the keys from the hashtable
		String[] keys = HashtableMethods.getKeysString(sequences);
		
		// Initialise a matrix to store the sequences
		char[][] output = new char[keys.length][sequences.get(keys[0]).length];
		
		// Store each sequence
		for(int i = 0; i < keys.length; i++){
			output[i] = sequences.get(keys[i]);
		}
		
		return(output);
	}
	
	public static String[] findCommonAlleles(String[] ids, Hashtable<String, char[]> sequences,
			Hashtable<String, Integer> ancestralAlleles){
		
		// Initialise a hashtable to store the common alleles
		Hashtable<String, Integer> alleleCounts = new Hashtable<String, Integer>();
		
		// Initialise an array to store each isolates sequence
		char[] sequence;
		
		// Initialise a variable to act as an allele key
		String alleleKey;
		
		// Examine each isolate
		for(int idIndex = 0; idIndex < ids.length; idIndex++){
			
			// Get the current isolates sequence
			sequence = sequences.get(ids[idIndex]);
			
			// Examine each site in the current isolates sequence
			for(int pos = 0; pos < sequence.length; pos++){
				
				// Define key based on the current allele at the current position
				alleleKey = pos + ":" + sequence[pos];
				
				// Check whether we have seen the current allele at the current site before
				if(alleleCounts.get(alleleKey) != null){
					alleleCounts.put(alleleKey, alleleCounts.get(alleleKey) + 1);
				}else{
					alleleCounts.put(alleleKey, 1);
				}
			}
		}
		
		// Find the alleles that were found in all the sequences
		String[] commonAlleles = new String[alleleCounts.size()];
		int pos = -1;
		for(String key : HashtableMethods.getKeysString(alleleCounts)){
			
			if(alleleCounts.get(key) == ids.length && ancestralAlleles.get(key) == null){
				pos++;
				commonAlleles[pos] = key;
			}
		}
		
		return(ArrayMethods.subset(commonAlleles, 0, pos));
	}
	
	public static void assignAllelesToSubBranches(Node node, Hashtable<String, char[]> sequences, 
			Hashtable<String, Integer> ancestralAlleles){
		
		// Initialise an array to store the isolates below a node
		String[] idsBelowNode;
		
		// Get the subNodes of the current node
		Node[] subNodes = node.getSubNodes();
		
		// Initialise an array to store the common alleles
		String[] commonAlleles;	
		
		// Examine each of the sub nodes associated with the current node
		for(int i = 0; i < subNodes.length; i++){
			
			// Get a list of the isolates associated with the current node
			idsBelowNode = getIdsBelowNode(subNodes[i]);
			
			// Find and store the common alleles for these isolates
			commonAlleles = findCommonAlleles(idsBelowNode, sequences, ancestralAlleles);
			subNodes[i].setCommonAlleles(commonAlleles);
			
			// Record that each of the common alleles were assigned to branch preceding the current sub-node
			for(String allele : commonAlleles){
				
				if(Global.branchesAllelesAssignedTo.get(allele) != null){
					Global.branchesAllelesAssignedTo.put(allele, 
							Node.append(Global.branchesAllelesAssignedTo.get(allele), subNodes[i]));
				}else{
					Node[] nodes = {subNodes[i]};
					Global.branchesAllelesAssignedTo.put(allele, nodes);
				}
			}
			
			// Examine the subnodes of the current node
			assignAllelesToSubBranches(subNodes[i], sequences, 
					assignAllelesToAncestral(ancestralAlleles, commonAlleles));			
		}
	}
	
	public static Hashtable<String, Integer> assignAllelesToAncestral(Hashtable<String, Integer> ancestralAlleles, String[] alleles){
		
		/**
		 * Initialise new hashtable table to store ancestral alleles as well as those to be assigned
		 * 	Using new hashtable, as the ancestral alleles hashtable can't be allowed to change - otherwise it'll affect other sub-nodes
		 */
		Hashtable<String, Integer> output = new Hashtable<String, Integer>();
		
		// Assign each of the ancestral alleles
		for(String allele : HashtableMethods.getKeysString(ancestralAlleles)){
			output.put(allele, 1);
		}
		
		// Assign the new alleles
		for(String allele : alleles){
			output.put(allele, 1);
		}
		
		return(output);
	}
	
	public static String[] getIdsBelowNode(Node node){
		
		
		// Reset the terminal nodes array
		Global.terminalNodes = new Node[0];
					
		// Note the terminal nodes associated with the current node
		noteTerminalNodes(node);
		
		// Note the isolates associated with the terminal nodes found
		String[] ids = new String[Global.terminalNodes.length];
		for(int i = 0; i < Global.terminalNodes.length; i++){
			
			ids[i] = Global.terminalNodes[i].getNodeInfo().getNodeId();
		}
		
		// Reset the terminal nodes array
		Global.terminalNodes = new Node[0];
		
		return(ids);
	}
	
	public static Hashtable<String, char[]> storeSequencesInHashtable(Sequence[] sequences){
		
		Hashtable<String, char[]> isolateSequences = new Hashtable<String, char[]>();
		
		// Examine each isolate sequence
		for(int i = 0; i < sequences.length; i++){
			
			isolateSequences.put(sequences[i].getName(), sequences[i].getSequence());
		}
		
		return(isolateSequences);
	}
	
	public static void noteTerminalNodes(Node node){
		
		// Check if we have reached a terminal node
		if(node.getSubNodes().length != 0){
				
			// Examine the subnodes of the current node
			for(Node subNode : node.getSubNodes()){
			
				noteTerminalNodes(subNode);
			}
		}else{
			Global.terminalNodes = NodeMethods.append(Global.terminalNodes, node);
		}
	}
	
	public static Node readNewickTree(String pathToFile) throws IOException{
		
		// Get the Newick tree string from file
		String newickTree = CalculateDistancesToMRCAs.readNewickFile(pathToFile); 
		
		// Store the tree as a series of traversable nodes
		Node tree = BeastNewickTreeMethods.readNewickNode(newickTree, new Node(null, null, null));
		
		return tree;
	}
}