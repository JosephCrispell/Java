package identifyingHomoplasies;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.JTextArea;

import geneticDistances.Sequence;
import methods.ArrayListMethods;
import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.GeneticMethods;
import methods.HashtableMethods;
import methods.WriteToFile;
import phylogeneticTree.BeastNewickTreeMethods;
import phylogeneticTree.CalculateDistancesToMRCAs;
import phylogeneticTree.Node;
import phylogeneticTree.NodeMethods;

public class HomoplasyFinder6 {
	
	public static void main(String[] args) throws IOException, InterruptedException{
		
		if(args[0].equals("-help") || args[0].equals("") || args[0].equals("-h") || args[0].equals("help")){
			System.out.println("HomoplasyFinder: a tool to identify homoplasies within a phylogenetic tree and alignment");
			System.out.println("\nCommand Line Structure:");
			System.out.println("\tjava -jar homoplasyFinder_DATE.jar verbose path sequences.fasta newick.tree\n");
			System.out.println("\t\tverbose\tDetailed output [0] or none [1]");
			System.out.println("\t\tsequences.fasta\tFASTA file containing alignment");
			System.out.println("\t\tnewick.tree\tNewick formatted tree file");
			System.out.println("\nNotes:");
			System.out.println("First line of input FASTA file contains the number of isolates and sites in the file");

			System.exit(0);
		}

		// Get the command line arguments
		boolean verbose = args[0].matches("1");
		String fasta = args[1];
		String treeFile = args[2];
		String path = "";

		// Set the path
//		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Homoplasy/DataForTesting/";
								
		// Get the current date
		String date = CalendarMethods.getCurrentDate("dd-MM-yy");
				
		// Set verbose
//		boolean verbose = true;
		
		/**
		 * Read in the phylogeny
		 */
		
		//String treeFile = path + "mlTree_withRef_14-06-16_NZ.tree"; // NZ
//		String treeFile = path + "mlTree_27-03-18_WP.tree"; // WP
		//String treeFile = path + "example-AFTER_09-04-18.tree"; // EXAMPLE
		Node tree = readNewickTree(treeFile, verbose);
		
		/**
		 * Read in the FASTA file
		 */
		
		// Read in the FASTA file
		//String fasta = path + "sequences_withRef_Prox-10_14-06-16_NZ.fasta"; // NZ
//		String fasta = path + "sequences_Prox-10_24-03-2018_WP.fasta"; // WP
		//String fasta = path + "example_09-04-18.fasta"; // EXAMPLE
		Sequence[] sequences = GeneticMethods.readFastaFile(fasta, verbose);
		
		// Get the alleles in the population and the isolates they are associated with
		Hashtable<String, ArrayList<String>> alleles = noteAllelesInPopulation(sequences, verbose);
		ArrayList<String> positions = getAllelePositions(alleles);
		
		/**
		 * Assign allele to node in phylogeny if:
		 * - Found in all isolates above and not in any below node OR vice versa
		 */
		
		// Assign alleles
		ArrayList<String> unassigned = assignAllelesToNodes(alleles, positions, tree, getSequenceIDs(sequences));
		
		/**
		 * Examine the un-assigned alleles - these are potential homoplasies
		 */
		int[] homoplasyPositions = examineUnAssignedAlleles(unassigned, alleles, verbose, path, null, date, null);

		/**
		 * Return a FASTA file without the homoplasy sites
		 */
		printFASTAWithoutHomoplasies(homoplasyPositions, path, null, date, sequences, verbose);
	}
	
	public static void getNodes(Node node, ArrayList<Node> nodes, boolean noteTips){
		
		// Note the tips for the current node
		if(noteTips == true){
			getIdsBelowNode(node);
		}
		
		// Add the current node
		nodes.add(node);
		
		// Examine each of the current node's sub-nodes
		if(node.getSubNodes().length != 0){
			for(Node subNode : node.getSubNodes()){
			
				getNodes(subNode, nodes, noteTips);
			}
		}		
	}
	
	public static ArrayList<String> assignAllelesToNodes(Hashtable<String, ArrayList<String>> alleles, ArrayList<String> positions, Node tree, ArrayList<String> ids) throws InterruptedException{
		
		// Create an array of nodes and note their tips at the same time
		ArrayList<Node> nodes = new ArrayList<Node>();
		getNodes(tree, nodes, true);
		
		// Initialise an array to store the thread objects
		MultiThreadAssignment[] threads = new MultiThreadAssignment[positions.size()];
		
		// Examine each isolate
		for(int i = 0; i < positions.size(); i++){
			
			// Create the thread and start it
			threads[i] = new MultiThreadAssignment(positions.get(i), alleles, nodes, ids);
			threads[i].start();
		}
		
		// Wait until all threads finished
		MultiThreadAssignment.waitUntilAllFinished(threads);

		// Collect the results from each thread
		ArrayList<String> unassigned = MultiThreadAssignment.collect(threads);
		
		return unassigned;
	}
	
	public static boolean checkIfAllAssigned(boolean[] assigned){
		boolean output = true;
		for(boolean value : assigned){
			if(value == false){
				output = false;
				break;
			}
		}
		
		return output;
	}
	
	public static void printFASTAWithoutHomoplasies(int[] positions, String path, String fileName, String date, Sequence[] sequences, boolean verbose) throws IOException{
		
		if(verbose == true){
			System.out.println("Writing sequences (without homoplasy sites) to file...");
		}
		
		// Open an output file
		if(fileName == null){
			fileName = "sequences_withoutHomoplasies_" + date + ".fasta";
		}
		BufferedWriter bWriter = WriteToFile.openFile(path + fileName, false);
		
		// Print out the number of isolates and sites in FASTA
		WriteToFile.writeLn(bWriter, sequences.length + " " + (sequences[0].getSequence().length - positions.length));
		
		// Initialise an array to each isolate sequence data
		char[] sequence = new char[sequences[0].getSequence().length];
		
		// Write out each of the sequences
		for(int i = 0; i < sequences.length; i++){
			
			// Print sequence ID
			WriteToFile.writeLn(bWriter, ">" + sequences[i].getName());
			
			// Print sequence
			sequence = ArrayMethods.deletePositions(sequences[i].getSequence(), positions);
			WriteToFile.writeLn(bWriter, ArrayMethods.toString(sequence));
		}
		WriteToFile.close(bWriter);
	}
	
	public static BufferedWriter buildOutputFile(String date, String reportFile, String path) throws IOException{
		
		String outputFile = path + "homoplasyReport_" + date + ".txt";
		if(reportFile != null){
			outputFile = path + reportFile;
		}
		BufferedWriter bWriter = WriteToFile.openFile(outputFile, false);
		WriteToFile.writeLn(bWriter, "Position\tAlleles\tIsolatesForAlleles");
				
		return bWriter;
	}
	
	public static int[] examineUnAssignedAlleles(ArrayList<String> unassigned, Hashtable<String, ArrayList<String>> alleles, boolean verbose,
			String path, String reportFile, String date, JTextArea guiTextArea) throws IOException{
		
		// Print progress information
		if(verbose == true){
			System.out.println("Identifying potential homoplasies...");
		}else if(guiTextArea != null){
			guiTextArea.setText("Found homoplasies at: \n");
		}
		
		// Build an output file
		BufferedWriter bWriter = buildOutputFile(date, reportFile, path);
		
		// Note the unassigned alleles
		ArrayList<String> homoplasies = new ArrayList<String>();
		for(String allele : unassigned){
			
			// Store the current allele
			homoplasies.add(allele);
		}
		
		// Note the alleles of each position a homoplasy was found at
		String[] parts;
		int position;
		char nucleotide;
		Hashtable<Integer, char[]> homoplasyPositions = new Hashtable<Integer, char[]>();
		for(String allele : homoplasies){
			
			// Split the allele into its position and nucleotide
			parts = allele.split(":");
			position = Integer.parseInt(parts[0]);
			nucleotide = parts[1].toCharArray()[0];
			
			// Check if already encountered this position
			if(homoplasyPositions.get(position) != null){
				homoplasyPositions.put(position, ArrayMethods.append(homoplasyPositions.get(position), nucleotide));
			}else{
				char[] nucleotides = {nucleotide};
				homoplasyPositions.put(position, nucleotides);
			}
		}
		
		// Report the information about each homoplasy
		for(int allelePosition : HashtableMethods.getKeysInt(homoplasyPositions)){
			
			if(verbose == true){
				System.out.println("---------------------------------------------------------------------------");
				System.out.println("Potential homoplasy identified at position: " + (allelePosition + 1) + " with alleles " + 
						ArrayMethods.toString(homoplasyPositions.get(allelePosition), ", "));
			}else if(guiTextArea != null){
				guiTextArea.append("Position: " + (allelePosition + 1) + " with alleles " + 
						ArrayMethods.toString(homoplasyPositions.get(allelePosition), ", ") + "\n");
			}
			
			WriteToFile.write(bWriter, (allelePosition + 1) + "\t" + ArrayMethods.toString(homoplasyPositions.get(allelePosition), ",") + "\t");
			
			char[] allelesForHomoplasy = homoplasyPositions.get(allelePosition);
			for(int i = 0; i < allelesForHomoplasy.length; i++){
				if(verbose == true){
					System.out.println("Isolates with allele " + allelesForHomoplasy[i] + ": " + 
							ArrayListMethods.toString(alleles.get(allelePosition + ":" + allelesForHomoplasy[i]), ", "));
				}
				if(i == 0){
					WriteToFile.write(bWriter, ArrayListMethods.toString(alleles.get(allelePosition + ":" + allelesForHomoplasy[i]), ":"));
				}else{
					WriteToFile.write(bWriter, "," + ArrayListMethods.toString(alleles.get(allelePosition + ":" + allelesForHomoplasy[i]), ":"));
				}				
			}
			WriteToFile.write(bWriter, "\n");
		}
		
		// Close the output file
		bWriter.close();
		
		// Return an array of the homoplasy positions
		return(HashtableMethods.getKeysInt(homoplasyPositions));
	}
	
	public static ArrayList<String> getAllelesAtPosition(String position, Hashtable<String, ArrayList<String>> alleles){
		
		ArrayList<String> output = new ArrayList<String>();
		char[] nucleotides = {'A', 'C', 'G', 'T'};
		for(char nucleotide : nucleotides){
			String allele = position + ":" + nucleotide;
			if(alleles.get(allele) != null){
				output.add(position + ":" + nucleotide);
			}
		}
		
		return output;
	}
	
	public static ArrayList<String> getAllelePositions(Hashtable<String, ArrayList<String>> alleles){
		ArrayList<String> positions = new ArrayList<String>();
		
		for(String key : HashtableMethods.getKeysString(alleles)){
			String position = key.split(":")[0];
			if(positions.contains(position) == false){
				positions.add(position);
			}			
		}
		
		return positions;
	}
	
	public static ArrayList<String> getSequenceIDs(Sequence[] sequences){
		
		ArrayList<String> ids = new ArrayList<String>();
		for(int i = 0; i < sequences.length; i++){
			ids.add(sequences[i].getName());
		}
		
		return ids;
	}
	
	public static void getIdsBelowNode(Node node){
		
		// Reset the terminal nodes array
		Global.terminalNodes = new Node[0];
					
		// Note the terminal nodes associated with the current node
		noteTerminalNodes(node);
		
		// Note the isolates associated with the terminal nodes found
		ArrayList<String> ids = new ArrayList<String>();
		for(int i = 0; i < Global.terminalNodes.length; i++){
			
			ids.add(Global.terminalNodes[i].getNodeInfo().getNodeId());
		}
		
		// Reset the terminal nodes array
		Global.terminalNodes = new Node[0];
		
		node.setTips(ids);
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
	
	public static Hashtable<String, ArrayList<String>> noteAllelesInPopulation(Sequence[] sequences, boolean verbose) throws InterruptedException{
		
		if(verbose == true){
			System.out.println("Recording alleles present in population...");
		}
		
		// Initialise an array to store the thread objects
		int seqLength = sequences[0].getSequence().length;
		MultiThreadPositions[] threads = new MultiThreadPositions[seqLength];
		
		// Examine each isolate
		for(int position = 0; position < seqLength; position++){
			
			// Create the thread and start it
			threads[position] = new MultiThreadPositions(position, sequences);
			threads[position].start();
		}
		
		// Wait until all threads finished
		MultiThreadPositions.waitUntilAllFinished(threads);		
		
		// Combine the outputs from each thread
		Hashtable<String, ArrayList<String>> alleles = MultiThreadPositions.collect(threads);
		
		return alleles;
	}
	
	public static Node readNewickTree(String pathToFile, boolean verbose) throws IOException{
		
		if(verbose == true){
			System.out.println("Reading newick tree file: " + pathToFile + ")...");
		}
		
		// Get the Newick tree string from file
		String newickTree = CalculateDistancesToMRCAs.readNewickFile(pathToFile); 
		
		// Store the tree as a series of traversable nodes
		Node tree = BeastNewickTreeMethods.readNewickNode(newickTree, new Node(null, null, null));
		
		return tree;
	}
}
