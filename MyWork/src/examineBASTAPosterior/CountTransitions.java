package examineBASTAPosterior;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.HashtableMethods;
import methods.WriteToFile;
import newickTree.Node;
import newickTree.Tree;

public class CountTransitions {

	public static void main(String[] args) throws IOException{
		
		// Insert a help statement
		if(args.length < 2 || args[0].equals("-help") || args[0].equals("") || args[0].equals("--help") || args[0].equals("-h") || args[0].equals("help")) {
			System.out.println("A simple tool to count the number of transitions between estimated states on a distribution of posterior trees from BASTA.");
			
			System.out.println("\nCommand line structure: ");
			System.out.println("java -jar CountTransitions_DATE.jar BASTA-log-file.trees BASTA-log-file.log states");
			System.out.println("\tBASTA-log-file.trees\tPosterior distribution of trees from BASTA analysis");
			System.out.println("\tBASTA-log-file.log\tPosterior distribution of parameter estimates from BASTA analysis");
			System.out.println("\tstates\t\t\tComma seperated list of possible states for nodes in posterior trees");
			System.out.println("\nNOTE:\nUnsampled estimated states must be included in states list and will be formatted \"Unsampled0\", \"Unsampled1\", etc.");
			System.out.println("These states are named in order of appearance in the migration rate matrix.");
			System.exit(0);
		}
		
		// Get the command line arguments
		String treesFile = args[0];
		String logFile = args[1];
		String analysis = treesFile.substring(0, treesFile.length()-6);
		String outputFile = analysis + "_TransitionCounts.txt";
		String[] states = args[2].split(",");
		
//		String treesFile = "/home/josephcrispell/Desktop/Research/Cumbria/BASTA/Replicate1_10-05-19/4DemeCumbria_equal_relaxed_1_10-05-19/4DemeCumbria_equal_relaxed_1_10-05-19.trees";
//		String logFile = "/home/josephcrispell/Desktop/Research/Cumbria/BASTA/Replicate1_10-05-19/4DemeCumbria_equal_relaxed_1_10-05-19/4DemeCumbria_equal_relaxed_1_10-05-19.log";
//		String analysis = treesFile.substring(0, treesFile.length()-6);
//		String outputFile = analysis + "_TransitionCounts.txt";
//		String[] states = {"badgerCumbria", "badgerTVR", "cowCumbria", "cowTVR"};
		
		// Read in a trees file
		Hashtable<String, Tree> trees = readPosteriorTrees(treesFile);
		
		// Read the log file to get each sample likelihood value
		Hashtable<String, String> likelihoods = readPosteriorLogFile(logFile);
		
		// Count the number of transitions between the node states of posterior trees
		countTransitionsOnPhylogenies(trees, likelihoods, outputFile, states);		
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
	
	public static void countTransitionsOnPhylogenies(Hashtable<String, Tree> trees, Hashtable<String, String> likelihoods, String outputFile,
			String[] states) throws IOException {
		
		// Sort the vector of states - to make sure they were input in the correct order
		Arrays.sort(states);
		
		// Print the states and their indices
		printStates(states);
		
		// Index the states
		Hashtable<String, Integer> indexedStates = HashtableMethods.indexArray(states);
		
		// Print progress
		System.out.println("Counting transitions on " + trees.size() + " trees...");
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(outputFile, false);
		
		// Add header into output file
		bWriter.write("Sample\tPosterior\tTreeLikelihood");
		addStateTransitionColumnHeaders(bWriter, states);
		
		// Get the sample names
		String[] keys = HashtableMethods.getKeysString(trees);
		
		// Examine each of the posterior trees
		for(int i = 0; i < keys.length; i++) {
			
			// Get the current tree
			Tree tree = trees.get(keys[i]);
			
			/**
			 *  Initialise matrices to store the transition counts and branch lengths sums
			 *  	
			 *  	A	B	...
			 *  A	0   0
			 *  B   0   0
			 *  ... 
			 */
			int[][] transitions = new int[states.length][states.length];
			
			// Get the internal and terminal nodes from the tree
			ArrayList<Node> internalNodes = tree.getInternalNodes();
			ArrayList<Node> terminalNodes = tree.getTerminalNodes();
			
			// Find the root node
			Node root = findRoot(tree.getInternalNodes());
			
			// Start traversing the tree from the current node
			examineNodeConservative(root, transitions, internalNodes, terminalNodes, indexedStates);
						
			// Print progress information
			if((i + 1) % 1000 == 0) {
				System.out.println("Finished counting transitions on " + (i + 1) + " trees...");
			}
			
			// Print transition counts for current tree to file
			bWriter.write(keys[i] + "\t" + likelihoods.get(keys[i]));
			addStateTransitionCounts(bWriter, transitions);
		}		
	}
	public static void printStates(String[] states) {
		System.out.println("Ordered states: ");
		for(int i = 0; i < states.length; i++) {
			System.out.println("\t" + states[i] + "\t" + i);
		}
		System.out.println();
	}
	public static void addStateTransitionColumnHeaders(BufferedWriter bWriter, String[] states) throws IOException {
		for(String a : states) {
			for(String b : states) {
				
				// Skip the diagonal
				if(a.matches(b)) {
					continue;
				}
				
				// Create the column label for the current count
				bWriter.write("\tCount_" + a + "-" + b);
			}
		}
		bWriter.write("\n");
	}
		
	public static void addStateTransitionCounts(BufferedWriter bWriter, int[][] transitionCounts) throws IOException {
		for(int i = 0; i < transitionCounts.length; i++) {
			for(int j = 0; j < transitionCounts.length; j++) {
				
				// Skip the diagonal
				if(i == j) {
					continue;
				}
				
				// Create the column label for the current count
				bWriter.write( "\t" + transitionCounts[i][j]);
			}
		}
		bWriter.write("\n");
	}
	
	public static void examineNodeConservative(Node node, int[][] transitions, ArrayList<Node> internalNodes, ArrayList<Node> terminalNodes,
			Hashtable<String, Integer> indexedStates) {
		
		/**
		 * A more conservative transition count method - recommended by Nicola De Maio
		 * 	- Assumes the ancestor represents one of the tips in the past
		 * 
		 * For a two state problem: A & B
		 *  ----A				----A				----A				----B	
		 * |		1 AA	   |		0 AA	   |		1 AA	   |		0 AA
		 * A		0 AB	   A		1 AB	   A----B	1 AB	   A		2 AB
		 * |				   |				   |				   |
		 *  ----A				----B				----A				----B
		 */
		
		// Get the sub node indices and types of the current node
		ArrayList<Integer> subNodeIndices = node.getSubNodeIndices();
		ArrayList<Boolean> subNodeTypes = node.getSubNodeTypes();
		
		// Get the current node's state
		String nodeState = HashtableMethods.getKeysString(node.getNodeInfo())[0].split("--")[1];
		int nodeStateIndex = indexedStates.get(nodeState);
		
		// Initialise a variable to count how many
		int nSameAsNode = 0;
		
		// Examine each of the sub nodes
		for(int i = 0; i < subNodeIndices.size(); i++) {
			
			// Get the current sub node object
			Node subNode = null;
			if(subNodeTypes.get(i)) {
				subNode = internalNodes.get(subNodeIndices.get(i));
					
				// For internal nodes - go and examine them
				examineNodeConservative(subNode, transitions, internalNodes, terminalNodes, indexedStates);
			}else {
				subNode = terminalNodes.get(subNodeIndices.get(i));
			}
				
			// Get the state of the current sub-node
			String subNodeState = HashtableMethods.getKeysString(subNode.getNodeInfo())[0].split("--")[1];
			int subNodeStateIndex = indexedStates.get(subNodeState);
				
			// Check if it is the same as the input node
			if(nodeStateIndex != -1 && nodeStateIndex == subNodeStateIndex) {
				nSameAsNode++;
				
			// If it's different then count the inter-state transition rates
			}else if(nodeStateIndex != -1 && subNodeStateIndex != -1){
					
				// Count the transition
				transitions[nodeStateIndex][subNodeStateIndex]++;
			}
		}
			
		// Count the within state transition rates if they occurred
		if(nSameAsNode > 1) {
			transitions[nodeStateIndex][nodeStateIndex] += nSameAsNode - 1;
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
