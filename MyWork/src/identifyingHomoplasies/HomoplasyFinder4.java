package identifyingHomoplasies;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

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

public class HomoplasyFinder4 {
	
	public static void main(String[] args) throws IOException{
		
//		if(args[0].equals("-help") || args[0].equals("") || args[0].equals("-h") || args[0].equals("help")){
//			System.out.println("HomoplasyFinder: a tool to identify homoplasies within a phylogenetic tree and alignment");
//			System.out.println("\nCommand Line Structure:");
//			System.out.println("\tjava -jar homoplasyFinder_DATE.jar verbose path sequences.fasta newick.tree\n");
//			System.out.println("\t\tverbose\tDetailed output [0] or none [1]");
//			System.out.println("\t\tsequences.fasta\tFASTA file containing alignment");
//			System.out.println("\t\tnewick.tree\tNewick formatted tree file");
//			System.out.println("\nNotes:");
//			System.out.println("First line of input FASTA file contains the number of isolates and sites in the file");
//
//			System.exit(0);
//		}
//
//		// Get the command line arguments
//		boolean verbose = args[0].matches("1");
//		String fasta = args[1];
//		String treeFile = args[2];
//		String path = "";
//		String date = fasta.split("_")[1].split("\\.")[0];
		
		// Set the path
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Homoplasy/";
								
		// Get the current date
		String date = CalendarMethods.getCurrentDate("dd-MM-yy");
				
		// Set verbose
		boolean verbose = true;
		
		/**
		 * Read in the phylogeny
		 */

		String treeFile = path + "mlTree_27-03-18.tree";
		//String treeFile = path + "example-AFTER_09-04-18.tree";
		Node tree = readNewickTree(treeFile, verbose);
		
		/**
		 * Read in the FASTA file
		 */
		
		// Read in the FASTA file
		String fasta = path + "sequences_Prox-10_24-03-2018.fasta";
		//String fasta = path + "example_09-04-18.fasta";
		Sequence[] sequences = GeneticMethods.readFastaFile(fasta, verbose);
		
		// Get the alleles in the population and the isolates they are associated with
		Hashtable<String, ArrayList<String>> alleles = noteAllelesInPopulation(sequences, verbose);
		ArrayList<String> positions = getAllelePositions(alleles);
		
		/**
		 * Assign allele to node in phylogeny if:
		 * - Found in all isolates above and not in any below node OR vice versa
		 */
		
		// Assign alleles
		Hashtable<String, Integer> assigned = new Hashtable<String, Integer>();
		assignAllelesToCurrentNode(tree, alleles, positions, assigned,  getSequenceIDs(sequences), verbose);
		
		/**
		 * Examine the un-assigned alleles - these are potential homoplasies
		 */
		int[] homoplasyPositions = examineUnAssignedAlleles(assigned, alleles, verbose, path, date);

		/**
		 * Return a FASTA file without the homoplasy sites
		 */
		printFASTAWithoutHomoplasies(homoplasyPositions, path, date, sequences, verbose);
	}
	
	public static void printFASTAWithoutHomoplasies(int[] positions, String path, String date, Sequence[] sequences, boolean verbose) throws IOException{
		
		if(verbose == true){
			System.out.println("Writing sequences (without homoplasy sites) to file...");
		}
		
		// Open an output file
		BufferedWriter bWriter = WriteToFile.openFile(path + "sequences_withoutHomoplasies_" + date + ".fasta", false);
		
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
	
	public static BufferedWriter buildOutputFile(String date, String path) throws IOException{
		
		String outputFile = path + "homoplasyReport_" + date + ".txt";
		BufferedWriter bWriter = WriteToFile.openFile(outputFile, false);
		WriteToFile.writeLn(bWriter, "Position\tAlleles\tIsolatesForAlleles");
				
		return bWriter;
	}
	
	public static int[] examineUnAssignedAlleles(Hashtable<String, Integer> assigned, Hashtable<String, ArrayList<String>> alleles, boolean verbose,
			String path, String date) throws IOException{
		
		// Print progress information
		if(verbose == true){
			System.out.println("Identifying potential homoplasies...");
		}
		
		// Build an output file
		BufferedWriter bWriter = buildOutputFile(date, path);
		
		// Note the unassigned alleles
		ArrayList<String> homoplasies = new ArrayList<String>();
		for(String allele : HashtableMethods.getKeysString(alleles)){
			
			// Ignore allele if assigned to node or if allele is an "N"
			if(assigned.get(allele) != null || allele.matches("(.*):N") == true){
				continue;
			}
			
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
			}
			
			WriteToFile.write(bWriter, (allelePosition + 1) + "\t" + ArrayMethods.toString(homoplasyPositions.get(allelePosition), ",") + "\t");
			
			char[] allelesForHomoplasy = homoplasyPositions.get(allelePosition);
			for(int i = 0; i < allelesForHomoplasy.length; i++){
				if(verbose == true){
					System.out.println("Isolates with allele " + allelesForHomoplasy[i] + ": " + 
							ArrayListMethods.toString(alleles.get(allelePosition + ":" + allelesForHomoplasy[i]), ", "));
				}
				if(i == 0){
					WriteToFile.write(bWriter, ArrayListMethods.toString(alleles.get(allelePosition + ":" + allelesForHomoplasy[i]), "-"));
				}else{
					WriteToFile.write(bWriter, "," + ArrayListMethods.toString(alleles.get(allelePosition + ":" + allelesForHomoplasy[i]), "-"));
				}				
			}
			WriteToFile.write(bWriter, "\n");
		}
		
		// Close the output file
		bWriter.close();
		
		// Return an array of the homoplasy positions
		return(HashtableMethods.getKeysInt(homoplasyPositions));
	}
	
	public static ArrayList<String> getAllelesAtPosition(String position, Hashtable<String, ArrayList<String>> alleles, Hashtable<String, Integer> assigned){
		
		ArrayList<String> output = new ArrayList<String>();
		char[] nucleotides = {'A', 'C', 'G', 'T'};
		for(char nucleotide : nucleotides){
			String allele = position + ":" + nucleotide;
			if(assigned.get(allele) == null && alleles.get(allele) != null){
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
	
	public static void assignAllelesToCurrentNode(Node node, Hashtable<String, ArrayList<String>> alleles, ArrayList<String> positions, Hashtable<String, Integer> assigned,
			ArrayList<String> ids, boolean verbose){
		
		// Print progress		
		if(verbose == true){
			Global.nodeNo++;
			System.out.println("Assigning alleles to node " + Global.nodeNo + " of " + ((2 * ids.size()) - 1));
		}
		
		// Get all the isolates below the current node and note their common alleles
		ArrayList<String> idsBelow = getIdsBelowNode(node);
				
		// Get all the isolates above the current node and note their common alleles
		ArrayList<String> idsAbove = ArrayListMethods.getUncommon(ids, idsBelow);
		
		// Check if any of the alleles have a set of isolates that match the isolates below or above the current node
		for(String position : positions){
			
			// Get the alleles associated with the current position
			ArrayList<String> allelesAtPosition = getAllelesAtPosition(position, alleles, assigned);
			if(allelesAtPosition.size() == 0){
				continue;
			}
				
			// Get an array of the isolates with an N at the current position
			ArrayList<String> isolatesWithN = alleles.get(position + ":N");
							
			// Remove the isolates with Ns from those above and below the current node
			ArrayList<String> idsBelowWithoutNs = ArrayListMethods.copy(idsBelow);
			ArrayList<String> idsAboveWithoutNs = ArrayListMethods.copy(idsAbove);
			if(isolatesWithN != null){
				ArrayListMethods.remove(idsBelowWithoutNs, isolatesWithN);
				ArrayListMethods.remove(idsAboveWithoutNs, isolatesWithN);
			}			
			
			// Examine each of the alleles at the current position
			for(String allele : allelesAtPosition){
				
				// Check if either the isolates above or below match those associated with the current allele
				if(ArrayListMethods.compare(alleles.get(allele), idsAboveWithoutNs) == true || ArrayListMethods.compare(alleles.get(allele), idsBelowWithoutNs) == true){
					assigned.put(allele, 1);
				}
			}			
		}
				
		// Examine each of the current node's sub-nodes
		if(node.getSubNodes().length != 0){
			for(Node subNode : node.getSubNodes()){
			
				assignAllelesToCurrentNode(subNode, alleles, positions, assigned, ids, verbose);
			}
		}
	}
	
	public static ArrayList<String> getIdsBelowNode(Node node){
		
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
		
		return(ids);
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
	
	public static Hashtable<String, ArrayList<String>> noteAllelesInPopulation(Sequence[] sequences, boolean verbose){
		
		if(verbose == true){
			System.out.println("Recording alleles present in population...");
		}
		
		// Initialise a hashtable to store the isolates associated with each allele
		Hashtable<String, ArrayList<String>> alleles = new Hashtable<String, ArrayList<String>>();
		
		// Initialise a variable to store each allele
		String allele;
		
		// Examine each isolate
		for(Sequence sequence : sequences){
			
			// Examine each position in the current isolate's sequence
			for(int pos = 0; pos < sequence.getSequence().length; pos++){
				
				// Create a key for the current allele
				allele = pos + ":" + sequence.getSequence()[pos];
				
				// Check if we have encountered the current allele before - note each sequence allele found in
				if(alleles.get(allele) != null){
					ArrayList<String> ids = alleles.get(allele);
					ids.add(sequence.getName());
					alleles.put(allele, ids);
				}else{
					
					ArrayList<String> ids = new ArrayList<String>();
					ids.add(sequence.getName());
					alleles.put(allele, ids);
				}
			}
		}
		
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