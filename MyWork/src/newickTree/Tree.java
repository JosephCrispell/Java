package newickTree;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import methods.ArrayListMethods;
import methods.ArrayMethods;
import phylogeneticTree.Node;
import phylogeneticTree.NodeInfo;

public class Tree {

	ArrayList<Node> tips;
	ArrayList<Node> internalNodes;
	
	public Tree(String fileName) throws IOException {
		
		// Get newick string
		StringBuffer newickTree = readNewickFile(fileName);
		
		// Store as traversable nodes
		readNewickNode(newickTree, null);
	}
	
	// Class specific methods
	private StringBuffer readNewickFile(String fileName) throws IOException{
		
		// Open the animals table file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
											
		// Initialise a variable to store the newick string
		String line = null;
		StringBuffer tree = new StringBuffer();
												
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			
			tree = tree.append(line);			
		}
		
		// Close the input file
		input.close();
		reader.close();
		
		return tree;
	}

	private ArrayList<ArrayList<Character>> findNewickSubNodes(ArrayList<Character> newickNode){
		
		// Initialise a variable to store each character
		char current;
		
		// Create Variable to track open Brackets
		int openBracket = 0;		// ( [ {	} ] )
		
		// Create Variable to node where a Newick Sub Node begins
		int nodeStartIndex = 1;
		
		// Initialise Array to Store the Newick Sub Node strings
		ArrayList<ArrayList<Character>> newickNodes = new ArrayList<ArrayList<Character>>();
		
		// Examine each of the characters in the Newick Node String
		for(int i = 0; i < newickNode.size(); i++){
			
			// Get the current character
			current = newickNode.get(i);
			
			// If the current character is a opening bracket record it
			if(current == '(' || current == '[' || current == '{'){
				openBracket++;
				
			// If the current character is a closing bracket record it
			}else if(current == ')' || current == ']' || current == '}'){
				openBracket--;
				
				// Are we finished with sub Node Information?
				if(openBracket == 0){
					
					// Store the latest Newick Node
					newickNodes.add(new ArrayList<Character>(newickNode.subList(nodeStartIndex, i)));
					
					// Finish
					break;
				}
				
			// If there are no open brackets (except the one enclosing the subNodes) and the current character is a comma
			// - reached the end a sub nodes info
			}else if(current == ',' && openBracket == 1){
				
				// Store the newick sub node
				newickNodes.add(new ArrayList<Character>(newickNode.subList(nodeStartIndex, i)));
				
				// Move the nodeStartIndex on
				nodeStartIndex = i + 1;
			}			
		}
		
		return newickNodes;
	}

	private String removeSubNodeInfo(String newickNode){
		
		/**
		 * Internal Node:
		 * 		(subNodes)[&NodeInfo]:[&BranchInfo]BranchLength
		 * 
		 * This method removes the (subNodes) part
		 */
		String[] parts = newickNode.split("\\)");
		
		// Return the last part - this will be at the correct level
		return parts[parts.length - 1];
	}
	
	private NodeInfo extractNodeInformation(String newickNode, boolean internal){
		/**
		 * Newick Node Information is stored in a variable format:
		 *  	Terminal Node: 
		 *  		NodeID[&NodeInfo]:[&BranchInfo]BranchLength
		 *  	Internal Node:
		 *  		(SubkNodes)[&NodeInfo]:[&BranchInfo]BranchLength
		 *  	
		 *  
		 *  It is only necessary for the NodeID and BranchLength to be present the rest is optional
		 *  	NOTE - BranchLength and BranchInfo are never present for the root	
		 *  
		 *  3[&rate_range={0.002,6.93},LatLongs1=-44.303,LatLongs2=170.304]:[&rate_range={0.002},length_range={4.000,26.978}]4.200
		 *  
		 *  Variable information can have multiple values, in addition the values could be strings e.g. species="BOVINE", species.set={"BOVINE", "POSSUM"}
		 */
		
		// If this is an internal node - need to remove the subNode Info
		if(internal){
			newickNode = removeSubNodeInfo(newickNode);
		}
		
		// Convert the Newick Node into a character array
		char[] chars = newickNode.toCharArray();
		char current = 'Z';
		
		// Initialise variables to store the Information for the current Newick Node
		String nodeId = "NA";
		double branchLength = -99;
		Hashtable<String, double[]> nodeInfo = new Hashtable();
		Hashtable<String, double[]> branchInfo = new Hashtable();
		
		// Initialise the kays and values for Hashtable
		String variableName = ""; 
		double[] values = new double[10000]; // Note by default large size - use subset to select used positions
		int posUsed = -1;
		int multipleStrings = 0;
		
		// Initialise Variables to trace progress
		int openCurlyBracket = 0;
		int readingNodeInfo = 0;
		int readingBranchInfo = 0;
		int variableStartIndex = -99;
		int branchLengthPresent = 0;
		
		// Start examining each character of the Newick Node String
		for(int i = 0; i < chars.length; i++){
			
			// Note current Character
			current = chars[i];
				
			if(current == '['){
						
				// Get the Node ID
				if(nodeId.equals("NA") && internal == 0){
					nodeId = newickNode.substring(0, i);
				}
						
				// Which Information are we currently reading?
				if(readingNodeInfo == 0){
					readingNodeInfo = 1;
				}else if(branchLengthPresent == 1 && variableStartIndex != i){
					
					// Means either no Node Information available or it has already been read - Store the Branch Length if Present
					branchLength = Double.parseDouble(newickNode.substring(variableStartIndex, i));
					
					// Begin reading the Branch Information if present
					readingBranchInfo = 1;
				}else{
					readingBranchInfo = 1;
				}
						
				// Move into the Information Area - skip the "[&" bit
				variableStartIndex = i + 2;
				continue;
						
			}else if(current == '='){
						
				// Record the Variable Name
				variableName = newickNode.substring(variableStartIndex, i);
				variableStartIndex = i + 1;
				continue;
						
			}else if(current == '{'){ // Means more than one value for the variable
				openCurlyBracket++;
				variableStartIndex = i + 1;
				
				// Check if the values are a list of Strings e.g. species.set={"BOVINE", "POSSUM"}
				if(Character.isDigit(chars[i + 1]) == false){
					
					// For Variables with multiple Strings: species.set={"BOVINE", "POSSUM"} -> species.set--BOVINE-POSSUM
					variableName = variableName + "--";
					multipleStrings = 1;
					
					// Check if variables are within quotes
					if(chars[i + 1] != '"'){
						multipleStrings = 2;
					}
				}
				
				continue;
						
			}else if(current == '}'){ // Finished finding all values for the variable
				openCurlyBracket--;
						
				// Store the Current Value - check that not dealing with multiple Strings
				if(multipleStrings == 0){
					posUsed++;
					values[posUsed] = Double.parseDouble(newickNode.substring(variableStartIndex, i));
				}else if(multipleStrings == 1){
					
					// For Variables with multiple Strings: species.set={"BOVINE", "POSSUM"} -> species.set--BOVINE-POSSUM
					variableName = variableName + newickNode.substring(variableStartIndex + 1, i - 1);				
				}else if(multipleStrings == 2){
					
					// For Variables with multiple Strings: species.set={"BOVINE", "POSSUM"} -> species.set--BOVINE-POSSUM
					variableName = variableName + newickNode.substring(variableStartIndex, i);
				}
				continue;
						
			}else if(openCurlyBracket == 1 && current == ','){ // Still inside curly brackets - add value into array for the current variable
				
				// Store the Current Value - check that not dealing with multiple Strings
				if(multipleStrings == 0){
					posUsed++;
					values[posUsed] = Double.parseDouble(newickNode.substring(variableStartIndex, i));
				}else if(multipleStrings == 1){
					
					// For Variables with multiple Strings: species.set={"BOVINE", "POSSUM"} -> species.set--BOVINE-POSSUM
					variableName = variableName + newickNode.substring(variableStartIndex + 1, i - 1) + "-";
				
				}else if(multipleStrings == 2){
					
					// For Variables with multiple Strings: species.set={"BOVINE", "POSSUM"} -> species.set--BOVINE-POSSUM
					variableName = variableName + newickNode.substring(variableStartIndex, i);
				}
						
				// Move Value index on
				variableStartIndex = i + 1;
				continue;
						
			}else if(openCurlyBracket == 0 && current == ','){
					
				// Extract the Variable Information
				if(posUsed == -1 && multipleStrings == 0){
					
					// Check that value isn't in a String format e.g. species = "BOVINE"
					if(Character.isDigit(chars[variableStartIndex]) == true){
						posUsed++;
						values[posUsed] = Double.parseDouble(newickNode.substring(variableStartIndex, i));
					
					// Value is a string - check if there are quotes present
					}else if(chars[variableStartIndex] == '"'){
						// Combine the value to the Variable Name: key: species--BOVINE
						variableName = variableName + "--" + newickNode.substring(variableStartIndex + 1, i - 1);
						
					}else{
						// Combine the value to the Variable Name: key: species--BOVINE
						variableName = variableName + "--" + newickNode.substring(variableStartIndex, i);
					}
					
				}else if(multipleStrings != 0){
					
					multipleStrings = 0; // Finished dealing with variable
				}
				
				// Store the Variable Information
				if(readingNodeInfo == 1){
					nodeInfo.put(variableName, ArrayMethods.subset(values, 0, posUsed));
				}else if(readingBranchInfo == 1){
					branchInfo.put(variableName, ArrayMethods.subset(values, 0, posUsed));
				}
						
				// Reset the Variable Information
				variableStartIndex = i + 1;
				posUsed = -1;
				values = new double[10000];
				continue;
						
			}else if(current == ']'){
				
				// Extract the Variable Information
				if(posUsed == -1 && multipleStrings == 0){
					
					// Check that value isn't in a String format e.g. species = "BOVINE"
					if(chars[variableStartIndex] != '"'){
						posUsed++;
						values[posUsed] = Double.parseDouble(newickNode.substring(variableStartIndex, i));
						
					}else if(chars[variableStartIndex] == '"'){
						// Combine the value to the Variable Name: key: species--BOVINE
						variableName = variableName + "--" + newickNode.substring(variableStartIndex + 1, i - 1);
					}
					
				}else if(multipleStrings == 1){
					multipleStrings = 0; // Finished dealing with variable
				}
				
				// Store the Variable Information
				if(readingNodeInfo == 1){
					nodeInfo.put(variableName, ArrayMethods.subset(values, 0, posUsed));
				}else if(readingBranchInfo == 1){
					branchInfo.put(variableName, ArrayMethods.subset(values, 0, posUsed));
				}
						
				// Reset the Variable Information
				variableStartIndex = i + 1;
				posUsed = -1;
				values = new double[100];
					
				// Note that finished Reading Variable Information
				if(readingNodeInfo == 1){
					readingNodeInfo = 2;
				}else if(readingBranchInfo == 1){
					readingBranchInfo = 2;
				}
						
			}else if(current == ':'){
					
				// Get the Node ID
				if(nodeId.equals("NA") && internal == 0){
					nodeId = newickNode.substring(0, i);
				}
				
				// For Branch length increase index by 1
				branchLengthPresent = 1;
				variableStartIndex = i + 1;
				
				// Note that finished reading nodeInfo if it was present
				readingNodeInfo = 2;
						
			}else if(i == chars.length - 1 && branchLength == -99 && branchLengthPresent == 1){ // If reached end and no BranchLength yet stored
				
				// Make sure Branch Length is stored
				branchLength = Double.parseDouble(newickNode.substring(variableStartIndex, i + 1));
			}
		}
		
		// Store the Information for the Current Node
		return new NodeInfo(nodeId, nodeInfo, branchInfo, branchLength);
	}
	
	private Node readNewickNode(StringBuffer newickNode, Node parentNode){
		
		/**
		 * This method is to read a Newick Node. This node is an internal node with associated sub nodes that can be either internal
		 * or terminal nodes. In order to traverse the entire Newick tree this method should be used within itself. For each internal
		 * node all subnodes are explored and their information stored as a tree of nodes within Java.
		 */
		
		// Convert the newickNode to a character array
		ArrayList<Character> newickNodeAsCharacters = ArrayListMethods.toArrayList(newickNode.toString().toCharArray());
		
		// Find the Sub Nodes for the Current Node
		ArrayList<ArrayList<Character>> newickSubNodes = findNewickSubNodes(newickNodeAsCharacters);
		String current = "";
		
		// Initialise array to store the Sub Nodes for the current Node
		Node[] subNodes = new Node[newickSubNodes.length];
		int posUsed = -1;
		
		// Create the Current Node
		NodeInfo nodeInfo = extractNodeInformation(newickNodeAsCharacters, true);
		Node node = new Node(nodeInfo, subNodes, parentNode);
				
		// Initialise variable to store the information associated with any terminal sub nodes
		NodeInfo terminalNodeInfo;
		
		// Examine each of the SubNodes
		for(int i = 0; i < newickSubNodes.length; i++){
			current = newickSubNodes[i];
			
			// Is the Current newick Sub node an Internal or Terminal Node?
			if(current.matches("\\((.*)")){ // I.e. does it start with a bracket "("? - If it does then it is an internal node
				
				// Create and Store this Internal Node
				posUsed++;
				subNodes[posUsed] = readNewickNode(current, node);
				
			}else{
				
				// Create and Store this Terminal Node
				terminalNodeInfo = extractNodeInformation(current, 0);
				posUsed++;
				subNodes[posUsed] = new Node(terminalNodeInfo, new Node[0], node);
			}
		}
		
		// Update the SubNode Information
		node.setSubNodes(subNodes);
		
		// Return the constructed tree Node
		return node;		
	}

}
