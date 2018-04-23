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

public class HomoplasyFinder3 {

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

		//String treeFile = path + "example_06-03-18.tree";
		String treeFile = path + "mlTree_27-03-18.tree";
		Node tree = readNewickTree(treeFile, verbose);
		
		/**
		 * Read in the FASTA file
		 */
		
		// Read in the FASTA file
		String fasta = path + "sequences_Prox-10_24-03-2018.fasta";
		Sequence[] sequences = GeneticMethods.readFastaFile(fasta, verbose);
		//Hashtable<String, char[]> isolateSequencesPriorToParsingIDs = storeSequencesInHashtable(sequences);
		Hashtable<String, char[]> isolateSequences = storeSequencesInHashtable(sequences);
		
		// Parse New Zealand sequence IDs - don't match those used in tree
/*		Hashtable<String, char[]> isolateSequences = new Hashtable<String, char[]>();
		for(String key : HashtableMethods.getKeysString(isolateSequencesPriorToParsingIDs)){
			
			String[] parts = key.split("_");
			String newKey = parts[0];
			if(key.matches("(.*)#(.*)") == true){
				newKey = parts[0] + "_" + parts[1];
			}
			isolateSequences.put(newKey, isolateSequencesPriorToParsingIDs.get(key));
		}
			*/	
		// Get the alleles in the population and the isolates they are associated with
		Hashtable<String, String[]> alleles = noteAllelesInPopulation(isolateSequences, verbose);
		
		/**
		 * Assign allele to node in phylogeny if:
		 * - Found in all isolates above and not in any below node OR vice versa
		 */
		
		// Assign alleles (CHECK R VERSION - COULD THIS BE WRITTEN FASTA USING ALLELES HASHTABLE ABOVE??)
		assignAllelesToCurrentNode(tree, isolateSequences, HashtableMethods.getKeysString(isolateSequences), verbose);
		
		// Get the assigned alleles
		Hashtable<String, Node[]> assignedAlleles = Global.nodeForEachAllele;
				
		/**
		 * Examine the un-assigned alleles - these are potential homoplasies
		 */
		int[] positions = examineUnAssignedAlleles(assignedAlleles, alleles, verbose, path, date);
		
		/**
		 * Return a FASTA file without the homoplasy sites
		 */
		printFASTAWithoutHomoplasies(positions, path, date, sequences, verbose);
	}
	
	// Methods section
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
			WriteToFile.writeLn(bWriter, ArrayMethods.toString(sequence, ""));
		}
		WriteToFile.close(bWriter);
	}
	
	public static BufferedWriter buildOutputFile(String date, String path) throws IOException{
		
		String outputFile = path + "homoplasyReport_" + date + ".txt";
		BufferedWriter bWriter = WriteToFile.openFile(outputFile, false);
		WriteToFile.writeLn(bWriter, "Position\tAlleles\tIsolatesForAlleles");
				
		return bWriter;
	}
	
	public static Hashtable<String, String[]> noteAllelesInPopulation(Hashtable<String, char[]> sequences, boolean verbose){
		
		if(verbose == true){
			System.out.println("Recording alleles present in population...");
		}
		
		// Initialise a hashtable to store the isolates associated with each allele
		Hashtable<String, String[]> alleles = new Hashtable<String, String[]>();
		
		// Initialise a variable to store each allele
		String allele;
		
		// Examine each isolate
		for(String id : HashtableMethods.getKeysString(sequences)){
			
			// Examine each position in the current isolate's sequence
			for(int pos = 0; pos < sequences.get(id).length; pos++){
				
				// Create a key for the current allele
				allele = pos + ":" + sequences.get(id)[pos];
				
				// Check if we have encountered the current allele before - note each sequence allele found in
				if(alleles.get(allele) != null){
					
					alleles.put(allele, ArrayMethods.append(alleles.get(allele), id));
				}else{
					
					String[] ids = {id};
					alleles.put(allele, ids);
				}
			}
		}
		
		return alleles;
	}
	
	public static String[] findAllelesCommonToAAndNotInB(Hashtable<String, Boolean> a, Hashtable<String, Boolean> b){
		
		// Initialise an array to store the alleles common to A and absent from B
		String[] alleles = new String[a.size()];
		int pos = -1;
		
		// Examine each of the alleles in A
		for(String allele : HashtableMethods.getKeysString(a)){
			
			// Check if current allele common to A and not in b
			if(a.get(allele) == true && b.get(allele) == null){
				pos++;
				alleles[pos] = allele;
			}
		}
		
		return ArrayMethods.subset(alleles, 0, pos);
	}

	public static int[] examineUnAssignedAlleles(Hashtable<String, Node[]> assignedAlleles, Hashtable<String, String[]> alleles, boolean verbose,
			String path, String date) throws IOException{
		
		// Print progress information
		if(verbose == true){
			System.out.println("Identifying potential homoplasies...");
		}
		
		// Build an output file
		BufferedWriter bWriter = buildOutputFile(date, path);
		
		// Note the unassigned alleles
		String[] homoplasies = new String[alleles.size() - assignedAlleles.size()];
		int pos = -1;
		for(String allele : HashtableMethods.getKeysString(alleles)){
			
			// Ignore allele if assigned to node or if allele is an "N"
			if(assignedAlleles.get(allele) != null || allele.matches("(.*):N") == true){
				continue;
			}
			
			// Store the current allele
			pos++;
			homoplasies[pos] = allele;
		}
		homoplasies = ArrayMethods.subset(homoplasies, 0, pos);
		
		// Note the alleles of teach position a homoplasy was found at
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
							ArrayMethods.toString(alleles.get(allelePosition + ":" + allelesForHomoplasy[i]), ", "));
				}
				if(i == 0){
					WriteToFile.write(bWriter, ArrayMethods.toString(alleles.get(allelePosition + ":" + allelesForHomoplasy[i]), "-"));
				}else{
					WriteToFile.write(bWriter, "," + ArrayMethods.toString(alleles.get(allelePosition + ":" + allelesForHomoplasy[i]), "-"));
				}				
			}
			WriteToFile.write(bWriter, "\n");
		}
		
		// Close the output file
		bWriter.close();
		
		// Return an array of the homoplasy positions
		return(HashtableMethods.getKeysInt(homoplasyPositions));
	}
	
	public static void assignAllelesToCurrentNode(Node node, Hashtable<String, char[]> sequences, String[] ids, boolean verbose){
		
		// Print progress		
		if(verbose == true){
			Global.nodeNo++;
			System.out.println("Assigning alleles to node " + Global.nodeNo + " of " + ((2 * ids.length) - 1));
		}
		
		// Get all the isolates below the current node and note their common alleles
		String[] idsBelow = getIdsBelowNode(node);
		Hashtable<String, Boolean> allelesBelow = findCommonAlleles(idsBelow, sequences);
		
		// Get all the isolates above the current node and note their common alleles
		String[] idsAbove = ArrayMethods.returnNotCommonElements(idsBelow, ids);
		Hashtable<String, Boolean> allelesAbove = findCommonAlleles(idsAbove, sequences);
		
		// Identify alleles common to isolates above node and NOT in isolates below OR vice versa
		String[] allelesCommonToBelowAndNotInAbove = findAllelesCommonToAAndNotInB(allelesBelow, allelesAbove);
		String[] allelesCommonToAboveAndNotInBelow = findAllelesCommonToAAndNotInB(allelesAbove, allelesBelow);
		String[] allelesToAssignToCurrentNode = ArrayMethods.combine(allelesCommonToBelowAndNotInAbove, allelesCommonToAboveAndNotInBelow);
		
		// Assign each of the alleles found to the current node
		for(String allele : allelesToAssignToCurrentNode){
			if(Global.nodeForEachAllele.get(allele) == null){
				Node[] nodes = {node};
				Global.nodeForEachAllele.put(allele, nodes);
			}else{
				Global.nodeForEachAllele.put(allele, Node.append(Global.nodeForEachAllele.get(allele), node));
				if(verbose == true){
					System.out.println("Current allele (" + allele + ") already assigned to multiple nodes");
				}				
			}			
		}
		
		// Examine each of the current node's sub-nodes
		if(node.getSubNodes().length != 0){
			for(Node subNode : node.getSubNodes()){
			
				assignAllelesToCurrentNode(subNode, sequences, ids, verbose);
			}
		}
	}
	
	public static Hashtable<String, Boolean> findCommonAlleles(String[] ids, Hashtable<String, char[]> sequences){
		
		// Initialise a hashtable to store the common alleles
		Hashtable<String, Integer> alleleCounts = new Hashtable<String, Integer>();
		
		// Initialise an array to store each isolates sequence
		char[] sequence;
		
		// Initialise a variable to act as an allele key
		String alleleKey;
		
		// Count the alleles present across all the sequences
		for(int idIndex = 0; idIndex < ids.length; idIndex++){
			
			// Get the current isolate's sequence
			sequence = sequences.get(ids[idIndex]);
			
			// Examine each site in the current isolates sequence
			for(int pos = 0; pos < sequence.length; pos++){
				
				// Check whether current position is an 'N' - insert a counter with no allele
				if(sequence[pos] == 'N'){
					if(alleleCounts.get(Integer.toString(pos)) != null){
						alleleCounts.put(Integer.toString(pos), alleleCounts.get(Integer.toString(pos)) + 1);
					}else{
						alleleCounts.put(Integer.toString(pos), 1);
					}
					continue;
				}
				
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
		
		// Note which alleles are common - found in all isolates
		Hashtable<String, Boolean> alleles = new Hashtable<String, Boolean>();
		int nNs;
		for(String allele : HashtableMethods.getKeysString(alleleCounts)){
			
			// Skip the 'N' records with position but no allele
			if(allele.matches("(.*):(.*)") == false){
				continue;
			}
			
			// Find the number of Ns at the current site
			nNs = 0;
			if(alleleCounts.get(allele.split(":")[0]) != null){
				nNs = alleleCounts.get(allele.split(":")[0]);
			}
			
			// Check if allele is common to all isolates without 'N's
			if(alleleCounts.get(allele) == (ids.length - nNs)){
				alleles.put(allele, true);
			}else{
				alleles.put(allele, false);
			}
		}
		
		return alleles;
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