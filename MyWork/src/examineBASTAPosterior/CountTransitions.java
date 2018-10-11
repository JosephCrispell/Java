package examineBASTAPosterior;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;

import methods.CalendarMethods;
import methods.HashtableMethods;
import methods.WriteToFile;
import newickTree.Node;
import newickTree.Tree;

public class CountTransitions {

	public static void main(String[] args) throws IOException{
		
		// Set the path
		String path = "/home/josephcrispell/Desktop/Research/Woodchester_CattleAndBadgers/NewAnalyses_22-03-18/BASTA/";
		
		// Note the date when analyses were created
		String date = "10-04-18";
		
		// Create an output file
		String outputFile = path + "TransitionCounts_" + date + ".txt";
		BufferedWriter bWriter = WriteToFile.openFile(outputFile, false);
		bWriter.write("Analysis\tReplicate\tSample\tPosterior\tTreeLikelihood\tCount_BB\tCount_BC\tCount_CB\tCount_CC\tSumBranchLengths_BB\tSumBranchLengths_BC\tSumBranchLengths_CB\tSumBranchLengths_CC\n");
		
		// Note each of the BASTA analyses
		String[] analyses = {
				"2Deme_equal_relaxed", "3Deme-outerIsCattle_varying_relaxed", "8Deme-EastWest_equal_relaxed",
				"2Deme_varying_relaxed", "4Deme_equal_relaxed", "8Deme-EastWest_varying_relaxed",
				"3Deme-outerIsBadger_equal_relaxed", "4Deme_varying_relaxed", "8Deme-NorthSouth_equal_relaxed",
				"3Deme-outerIsBadger_varying_relaxed", "6Deme-EastWest_equal_relaxed", "8Deme-NorthSouth_varying_relaxed",
				"3Deme-outerIsBoth_equal_relaxed", "6Deme-EastWest_varying_relaxed", "3Deme-outerIsBoth_varying_relaxed", 
				"6Deme-NorthSouth_equal_relaxed", "3Deme-outerIsCattle_equal_relaxed", "6Deme-NorthSouth_varying_relaxed"};
					
		// Examine each of the different BASTA analyses
		for(String analysis : analyses) {
				
			// Loop through each of the three replicates - for the BASTA analyses
			for(int rep = 1; rep <= 3; rep++) {
				
				System.out.println("Counting transitions for: " + analysis + ". Replicate: " + rep);
				
				// Read in a trees file
				String treesFile = path + "Replicate" + rep + "_" + date + "/" + analysis + "_" + date + "/" + analysis + "_" + date + ".trees";
				Hashtable<String, Tree> trees = readPosteriorTrees(treesFile);
				
				// Read the log file to get each sample likelihood value
				String logFile = path + "Replicate" + rep + "_" + date + "/" + analysis + "_" + date + "/" + analysis + "_" + date + ".log";
				Hashtable<String, String> likelihoods = readPosteriorLogFile(logFile);
				
				// Count the number of badger-to-cattle and cattle-to-badger transitions
				countTransitionsOnPhylogenies(trees, likelihoods, bWriter, analysis, rep);
				
				System.out.println("\n\n");
			}
		}		
		
		// Close the output file
		bWriter.close();
		
	}
	
	public static Hashtable<String, String> readPosteriorLogFile(String fileName) throws IOException{
		
		System.out.println("Reading posterior log table...");
		
		// Open the animals table file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
													
		// Initialise necessary variables for parsing the file
		String line = null;
		String[] cols;
				
		// Initialise a hashtable to store the sample likelihood values
		Hashtable<String, String> likelihoods = new Hashtable<String, String>();
								
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			
			// Skip the header lines
			if(line.matches("#(.*)") || line.matches("Sample(.*)")) {
				continue;
			}
			
			// Split the current line into its columns
			cols = line.split("\t");
			
			// Store the sample's likelihood
			likelihoods.put(cols[0], cols[1] + "\t" + cols[2]);
		}
		
		// Close the input files
		input.close();
		reader.close();
		
		System.out.println("Finished reading posterior log table. Found " + likelihoods.size() + " records.\n");
		
		return likelihoods;
	}
	
	public static void countTransitionsOnPhylogenies(Hashtable<String, Tree> trees, Hashtable<String, String> likelihoods, BufferedWriter bWriter,
			String analysis, int replicate) throws IOException {
		
		System.out.println("Counting transitions on " + trees.size() + " trees...");
		
		// Get the sample names
		String[] keys = HashtableMethods.getKeysString(trees);
		
		// Examine each of the posterior trees
		for(int i = 0; i < keys.length; i++) {
			
			// Get the current tree
			Tree tree = trees.get(keys[i]);
			
			/**
			 *  Initialise matrices to store the transition counts and branch lengths sums
			 *  	
			 *  		Badger	Cattle
			 *  Badger	  0       0
			 *  Cattle    0       0
			 */
			int[][] transitions = new int[2][2];
			double[][] branchLengthSums = new double[2][2];
			
			// Get the internal and terminal nodes from the tree
			ArrayList<Node> internalNodes = tree.getInternalNodes();
			ArrayList<Node> terminalNodes = tree.getTerminalNodes();
			
			// Find the root node
			Node root = findRoot(tree.getInternalNodes());
			
			// Start traversing the tree from the current node
			examineNode(root, null, transitions, branchLengthSums, internalNodes, terminalNodes);
			
			// Print progress information
			if((i + 1) % 1000 == 0) {
				System.out.println("Finished counting transitions on " + (i + 1) + " trees...");
			}
			
			// Print transition counts for current tree to file
			bWriter.write(analysis + "\t" + replicate + "\t" + keys[i] + "\t" + likelihoods.get(keys[i]) + 
					"\t" + transitions[0][0] + "\t" + transitions[0][1] + "\t" + transitions[1][0] + "\t" + transitions[1][1] +
					"\t" + branchLengthSums[0][0] + "\t" + branchLengthSums[0][1] + "\t" + branchLengthSums[1][0] + "\t" + branchLengthSums[1][1] + "\n");
		}		
	}
	
	public static void examineNode(Node node, Node parent, int[][] transitions, double[][] branchLengthSums, ArrayList<Node> internalNodes,
			ArrayList<Node> terminalNodes) {
		
		// Check if NOT at root, root has no parent consider the state transition from parent to the current node
		if(parent != null) {
			
			// Get the state indices for the current node and its parent
			int parentStateIndex = returnStateIndex(HashtableMethods.getKeysString(parent.getNodeInfo())[0]);
			int nodeStateIndex = returnStateIndex(HashtableMethods.getKeysString(node.getNodeInfo())[0]);
			
			// If state indices are available record the transition (only considers cattle/badger demes, not unsampled)
			if(parentStateIndex != -1 && nodeStateIndex != -1) {
				
				// Count the transition
				transitions[parentStateIndex][nodeStateIndex]++;
				
				// Increment appropriate branch length
				branchLengthSums[parentStateIndex][nodeStateIndex] += node.getBranchLength();
			}
		}
		
		// Get the sub node indices and types of the current node
		ArrayList<Integer> subNodeIndices = node.getSubNodeIndices();
		ArrayList<Boolean> subNodeTypes = node.getSubNodeTypes();
		
		// If there are sub nodes available examine them
		for(int i = 0; i < subNodeIndices.size(); i++) {
			
			// Check if internal node
			if(subNodeTypes.get(i)) {
				
				examineNode(internalNodes.get(subNodeIndices.get(i)), node, transitions, branchLengthSums, internalNodes, terminalNodes);
			}else {
				examineNode(terminalNodes.get(subNodeIndices.get(i)), node, transitions, branchLengthSums, internalNodes, terminalNodes);
			}
		}		
	}
	
	public static int returnStateIndex(String state) {
		
		/**
		 * Any states with "badger" or "cow" in them are recognised
		 * 	*badger* = 0
		 * 	*cow* = 1
		 * 
		 * Note: not considering unsampled states 
		 */
		
		// Initialise a variable to store the state index
		int index = -1;
		
		// Examine the string
		if(state.matches("(.*)badger(.*)")) {
			index = 0;
		}else if(state.matches("(.*)cow(.*)")) {
			index = 1;
		}
		
		return index;
	}
	
	public static Node findRoot(ArrayList<Node> internalNodes) {
		
		// Initialise a variable to store the root node
		Node root = null;
		
		// Examine each internal node
		for(Node internalNode : internalNodes) {
			if(internalNode.getParentIndex() == -1) {
				root = internalNode;
				break;
			}
		}
		
		return root;
	}
	
	public static Hashtable<String, Tree> readPosteriorTrees(String fileName) throws IOException {
		
		System.out.println("Reading posterior trees file...");
		
		// Open the animals table file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
											
		// Initialise necessary variables for parsing the file
		String line = null;
		String[] parts;
		
		// Initialise a hashtable to store the phylogenetic trees
		Hashtable<String, Tree> trees = new Hashtable<String, Tree>();
						
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			
			// Skip all the lines except those represent the posterior trees (at the end)
			if(line.matches("tree STATE_(.*)") == false) {
				continue;
			}
			
			// Split the current line into parts
			parts = line.split(" = ");
			
			// Get the sample number for the current tree
			String sample = parts[0].split("_")[1];
			
			// Get the newick formatted tree from the current line
			String newickTree = parts[1];
			
			// Convert the newick tree into a traversable set of nodes
			trees.put(sample, new Tree(null, newickTree));
			
			// Print progress information
			if(trees.size() % 1000 == 0) {
				System.out.println("Finished reading " + trees.size() + " trees...");
			}
		}
		System.out.println("Found " + trees.size() + " trees.\n");
		
		// Close the input files
		input.close();
		reader.close();
		
		return trees;
	}
}
