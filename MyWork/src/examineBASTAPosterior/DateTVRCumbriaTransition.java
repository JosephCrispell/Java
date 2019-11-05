package examineBASTAPosterior;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import methods.HashtableMethods;
import methods.WriteToFile;
import newickTree.Node;
import newickTree.Tree;

public class DateTVRCumbriaTransition {

	public static void main(String[] args) throws IOException{
		
		// Insert a help statement
		if(args.length != 1 || args[0].equals("-help") || args[0].equals("") || args[0].equals("--help") || args[0].equals("-h") || args[0].equals("help")) {
			System.out.println("A simple tool to count the number of transitions between estimated states on a distribution of posterior trees from BASTA.");
			
			System.out.println("\nCommand line structure: ");
			System.out.println("java -jar DateTransitionFromTVRToCumbria_DATE.jar BASTA-log-file.trees");
			System.out.println("\tBASTA-log-file.trees\tPosterior distribution of trees from BASTA analysis");
			System.exit(0);
		}
		
		// Get the command line arguments
		String treesFile = args[0];
		String analysis = treesFile.substring(0, treesFile.length()-6);
		String outputFile = analysis + "_TVRCumbriaTransitionTiming.txt";
		
//		String treesFile = "/home/josephcrispell/Desktop/Research/Cumbria/BASTA/Run_16-08-19/Replicate1_16-08-19/4DemeCumbria_equal_relaxed_1_16-08-19/4DemeCumbria_equal_relaxed_1_16-08-19_TEST.trees";
//		String analysis = treesFile.substring(0, treesFile.length()-6);
//		String outputFile = analysis + "_TVRCumbriaTransitionTiming.txt";
		
		// Read in a trees file
		Hashtable<String, Tree> trees = CountTransitions.readPosteriorTrees(treesFile);
		
		// Report the estimated height(s) of each transition from TVR to Cumbria on each tree in the posterior
		reportHeightsOfTransitionFromTVRToCumbriaOnEachTree(trees, outputFile);
	}
	
	public static void reportHeightsOfTransitionFromTVRToCumbriaOnEachTree(Hashtable<String, Tree> trees, String outputFile) throws IOException {
		
		System.out.println("\nReporting height estimations on posterior trees...");
		
		// Open an output file
		BufferedWriter bWriter = WriteToFile.openFile(outputFile, false);
						
		// Add header into output file
		bWriter.write("Sample\tTransitionHeights\tMaxHeightOnTree\n");
				
		// Examine each tree
		String[] states = HashtableMethods.getKeysString(trees);
		for(int stateIndex = 0; stateIndex < states.length; stateIndex++) {
			
			// Note current state
			String state = states[stateIndex];
			
			// Calculate the height of every node in every tree
			Tree tree = trees.get(state);
			Node root = CountTransitions.findRoot(tree.getInternalNodes());
			noteDistanceToRoot(root, 0, tree);
			
			// Note maximum height on tree
			double maxDistanceToRoot = noteMaxHeight(tree.getInternalNodes());
					
			// Record the height of each cowTVR -> cowCumbria
			ArrayList<Double> heights = new ArrayList<Double>();
			noteHeightsOfTransitionFromTVRToCumbria(root, tree, heights);
			
			bWriter.write(state + "\t" + methods.ArrayListMethods.toStringDouble(heights, ",") + "\t" + maxDistanceToRoot + "\n");
			
			// Print progress information
			if((stateIndex + 1) % 1000 == 0) {
				System.out.println("Finished examining " + (stateIndex + 1) + " trees...");
			}
		}
		System.out.println("Finished :-)");
				
		// Close the output file
		bWriter.close();
	}
	
	public static double noteMaxHeight(ArrayList<Node> internalNodes) {
		
		// Initialise a variable to store the maximum height
		double maxDistanceToRoot = 0;
		
		// Examine each of the internal nodes
		for(Node internalNode : internalNodes) {
			
			// Check if height of current internal node is higher than seen before
			if(internalNode.getHeight() > maxDistanceToRoot) {
				maxDistanceToRoot = internalNode.getHeight();
			}
		}
		
		return(maxDistanceToRoot);
	}
	
	public static void noteHeightsOfTransitionFromTVRToCumbria(Node node, Tree tree, ArrayList<Double> heights) {
		
		// Get the state of the parent of the current node
		String parentState = "NONE";
		if(node.getParentIndex() != -1) {
			
			// Get the parent of the current node
			Node parentNode = tree.getInternalNodes().get(node.getParentIndex());
			
			// Note its state
			parentState = HashtableMethods.getKeysString(parentNode.getNodeInfo())[0].split("--")[1];
		}
		
		// Get the state of the current node
		String nodeState = HashtableMethods.getKeysString(node.getNodeInfo())[0].split("--")[1];
		
		// Check if transition occurred at current node
		if(parentState.matches("cowTVR") && nodeState.matches("cowCumbria")) {
			heights.add(node.getHeight());
		}
		
		// Get the sub node indices and types of the current node
		ArrayList<Integer> subNodeIndices = node.getSubNodeIndices();
		ArrayList<Boolean> subNodeTypes = node.getSubNodeTypes();
		
		// Examine each of the sub nodes
		for(int i = 0; i < subNodeIndices.size(); i++) {
			
			// Check if current node is internal
			if(subNodeTypes.get(i)) {
				
				Node subNode = tree.getInternalNodes().get(subNodeIndices.get(i));
				noteHeightsOfTransitionFromTVRToCumbria(subNode, tree, heights);
			}else {
				
				Node subNode = tree.getTerminalNodes().get(subNodeIndices.get(i));
				noteHeightsOfTransitionFromTVRToCumbria(subNode, tree, heights);
			}
		}
	}
	
	public static void noteDistanceToRoot(Node node, double parentDistanceToRoot, Tree tree) {
		
		// Check if the current node has a branch length
		if(node.getBranchLength() != -1) {
			node.setHeight(parentDistanceToRoot + node.getBranchLength());
		
		// Else print error
		}else {
			System.err.println("No branch length associated with current node!");
			System.exit(0);
		}
		
		// Get the sub node indices and types of the current node
		ArrayList<Integer> subNodeIndices = node.getSubNodeIndices();
		ArrayList<Boolean> subNodeTypes = node.getSubNodeTypes();
		
		// Examine each of the sub nodes
		for(int i = 0; i < subNodeIndices.size(); i++) {
			
			// Check if current node is internal
			if(subNodeTypes.get(i)) {
				
				Node subNode = tree.getInternalNodes().get(subNodeIndices.get(i));
				noteDistanceToRoot(subNode, node.getHeight(), tree);
			}else {
				
				Node subNode = tree.getTerminalNodes().get(subNodeIndices.get(i));
				noteDistanceToRoot(subNode, node.getHeight(), tree);
			}
		}
	}
}
