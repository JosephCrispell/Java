package phylogeneticTree;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.WriteToFile;

public class CalculateDistancesToMRCAs {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {

		// Get the path to the current directory
		String isBeast = args[0];
		
		// Print some help information
		if(isBeast.equals("-help") || isBeast.equals("help") || isBeast.equals("")){
			
			System.out.println("JAVA Tool to Calculate the distance to the MRCAs of terminal node pairs.");
			System.out.println("Author: Joseph Crisp");
			System.out.println("Created: 23-01-2016");
			System.out.println("Can be used on a BEAST MCC Tree or a newick string.");
			
			System.out.println("\nCommand line structure:");
			System.out.println("java -jar CalculateDistancesToMRCAs.jar isBeast treeFile outputFile");
			System.out.println("isBeast\t\tIs the input tree a BEAST MCC tree? [0,1]");
			
		}else{
			
			// Note the tree file
			String treeFile = args[1];
			
			// Note the output file
			String outputFile = args[2];
			
			// What type of tree is the input?
			if(isBeast.matches("1")){
				createDistanceToMRCAMatrixForMCCTree(treeFile, outputFile, "\t");
			}else{
				createDistanceToMRCAMatrixForNewickTree(treeFile, outputFile, "\t");
			}			
		}
	}
	
	public static String readNewickFile(String fileName) throws IOException{
		
		// Open the animals table file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
											
		// Initialise a variable to store the newick string
		String line = null;
		String tree = "";
												
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			
			tree = tree + "" + line;			
		}
		
		// Close the input file
		input.close();
		reader.close();
		
		return tree;
	}
	
	public static void createDistanceToMRCAMatrixForNewickTree(String newickFile, String outputFile, String sep) throws IOException{
		
		// Read the Newick String into a variable
		String newickTree = readNewickFile(newickFile);
		
		// Convert the Newick Tree into a Java traversable Node
		Node tree = BeastNewickTreeMethods.readNewickNode(newickTree, new Node(null, null, null));
		
		// For each node in the phylogenetic tree note the path to the root
		notePathToRootForAllNodes(tree, new Node[0]);
				
		// Calculate the distance to MRCA for all terminal node pairs
		double[][] distancesToMRCAs = calculateDistancesToMRCAs(Global.terminalNodes);
				
		// Print the distances out to file
		printDistancesToMRCAs(Global.terminalNodes, distancesToMRCAs, outputFile, sep);				
		
	}
	
	public static void createDistanceToMRCAMatrixForMCCTree(String mccFile, String outputFile, String sep) throws NumberFormatException, IOException{
		
		// Read in the MCC newick tree
		BeastNewick newickTreeInfo = BeastNewickTreeMethods.readBeastFormattedNexus(mccFile, 1);
				
		// Convert the Newick Tree into a Java traversable Node
		Node tree = BeastNewickTreeMethods.readNewickNode(newickTreeInfo.getNewickTrees()[0], new Node(null, null, null));
		
		// For each node in the phylogenetic tree note the path to the root
		notePathToRootForAllNodes(tree, new Node[0]);
		
		// Calculate the distance to MRCA for all terminal node pairs
		double[][] distancesToMRCAs = calculateDistancesToMRCAs(Global.terminalNodes);
		
		// Print the distances out to file
		printDistancesToMRCAs(Global.terminalNodes, distancesToMRCAs, outputFile, sep, newickTreeInfo.getSampleNames());
		
	}
	
	public static void printDistancesToMRCAs(Node[] terminalNodes, double[][] distancesToMRCAs, String fileName,
			String sep, Hashtable<Integer, String> isolateNames) throws IOException{
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Initialise a string to store the output
		String colNames = isolateNames.get(Integer.parseInt(terminalNodes[0].getNodeInfo().getNodeId()));
		String output = "";
		
		// Build the output
		for(int i = 0; i < distancesToMRCAs.length; i++){
			
			// Build the first line
			if(i > 0){
				colNames = colNames + sep + isolateNames.get(Integer.parseInt(terminalNodes[i].getNodeInfo().getNodeId()));
			}
			
			// Add the node Id into the first column
			output = output + isolateNames.get(Integer.parseInt(terminalNodes[i].getNodeInfo().getNodeId()));
			
			for(int j = 0; j < distancesToMRCAs[0].length; j++){
				
				output = output + sep + distancesToMRCAs[i][j];				
			}
			
			// Finish the line
			output = output + "\n";
		}
		
		// Print the output to file
		WriteToFile.write(bWriter, colNames + "\n" + output);
		
		// Close the output file
		WriteToFile.close(bWriter);
	}
	
	public static void printDistancesToMRCAs(Node[] terminalNodes, double[][] distancesToMRCAs, String fileName,
			String sep) throws IOException{
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Initialise a string to store the output
		String colNames = terminalNodes[0].getNodeInfo().getNodeId();
		String output = "";
		
		// Build the output
		for(int i = 0; i < distancesToMRCAs.length; i++){
			
			// Build the first line
			if(i > 0){
				colNames = colNames + sep + terminalNodes[i].getNodeInfo().getNodeId();
			}
			
			// Add the node Id into the first column
			output = output + terminalNodes[i].getNodeInfo().getNodeId();
			
			for(int j = 0; j < distancesToMRCAs[0].length; j++){
				
				output = output + sep + distancesToMRCAs[i][j];				
			}
			
			// Finish the line
			output = output + "\n";
		}
		
		// Print the output to file
		WriteToFile.write(bWriter, colNames + "\n" + output);
		
		// Close the output file
		WriteToFile.close(bWriter);
	}
	
	public static double[][] calculateDistancesToMRCAs(Node[] terminalNodes){
		
		// Initialise a matrix to store the distances to MRCAs of pairs
		int nTerminalNodes = terminalNodes.length;
		double[][] distancesToMRCAs = new double[nTerminalNodes][nTerminalNodes];
		
		// Initialise a variable to store the MRCA and the calculate distance
		Node MRCA;
		double distance;
		
		// Compare the terminal nodes to one another
		for(int i = 0; i < nTerminalNodes; i++){
					
			for(int j = 0; j < nTerminalNodes; j++){
						
				// Don't make self comparisons, or the same comparison twice
				if(i >= j){
					continue;
				}
						
				// Find the MRCA between the terminal nodes
				MRCA = findMRCA(terminalNodes[i].getPathToRoot(), terminalNodes[j].getPathToRoot());
				
				// Calculate the distances from Node i to the MRCA
				distance = calculateDistanceToMRCA(terminalNodes[i], MRCA);
				distancesToMRCAs[i][j] = distance;
				
				// Calculate the distance from Node j to the MRCA
				distance = calculateDistanceToMRCA(terminalNodes[j], MRCA);
				distancesToMRCAs[j][i] = distance;
			}			
		}
		
		return distancesToMRCAs;
	}
	
	public static double calculateDistanceToMRCA(Node node, Node MRCA){
		
		// Work backwards from the leaf towards the root (and MRCA), summing branch lengths along the way
		
		// Initialise a variable to store the summed branch lengths
		double distance = node.getNodeInfo().getBranchLength();
		
		// Work backwards from the leaf towards the root
		for(int i = node.getPathToRoot().length - 1; i >= 0; i--){
			
			if(node.getPathToRoot()[i] != MRCA){
				
				distance += node.getPathToRoot()[i].getNodeInfo().getBranchLength();
				
			}else{
				break;
			}			
		}
		
		return distance;
	}
	
	public static void notePathToRootForAllNodes(Node node, Node[] pathToRoot){
		
		// Set the Path to the root for the current node
		node.setPathToRoot(pathToRoot);
		
		// Add the current node to the pathToRoot
		pathToRoot = NodeMethods.append(pathToRoot, node);
		
		// Check if we have reached a terminal node
		if(node.getSubNodes().length == 0){
			
			Global.terminalNodes = NodeMethods.append(Global.terminalNodes, node);
			
		}else{
		
			// Examine the subnodes of the current node
			for(Node subNode : node.getSubNodes()){
			
				notePathToRootForAllNodes(subNode, pathToRoot);			
			}
		}
		
	}

	public static Node findMRCA(Node[] pathA, Node[] pathB){
		
		// Create a variable to store the MRCA
		Node MRCA = null;
		
		// Find the minimum length
		int[] lengths = {pathA.length, pathB.length};
		int min = ArrayMethods.min(lengths);
		
		// Find the last common node in the paths from the root to the leaves
		for(int i = 0; i < min; i++){
			
			if(pathA[i] != pathB[i]){
				MRCA = pathA[i - 1];
				break;
			}
		}
		
		// If didn't find an MRCA then it will be at the last position of the smallest list (in either list)
		if(MRCA == null){
			MRCA = pathA[min - 1];
		}
		
		return MRCA;		
	}
	
	public static Node findMRCA(Node[][] paths){
		
		// Create a variable to store the MRCA
		Node MRCA = null;
		
		// Find the minimum length
		int min = findMinLength(paths);
		
		// Find the last common node in the paths from the root to the leaves
		for(int i = 0; i < min; i++){
			
			if(checkIfAllNodesSame(i, paths) == false){
				MRCA = paths[0][i - 1];
				break;
			}
		}
		
		// If didn't find an MRCA then it will be at the last position of the smallest list (in either list)
		if(MRCA == null){
			MRCA = paths[0][min - 1];
		}
		
		return MRCA;		
	}
	
	public static boolean checkIfAllNodesSame(int index, Node[][] paths){
		
		boolean result = true;
		for(int i = 0; i < paths.length; i++){
			
			for(int j = 0; j < paths.length; j++){
				
				if(i >= j){
					continue;
				}
				
				if(paths[i][index] != paths[j][index]){
					result = false;
				}
			}
		}
		
		return result;
	}
	
	public static int findMinLength(Node[][] paths){
		
		int minLength = 99999;
		for(int i = 0; i < paths.length; i++){
			
			if(paths[i].length < minLength){
				minLength = paths[i].length;
			}
		}
		
		return minLength;
	}
	
}
