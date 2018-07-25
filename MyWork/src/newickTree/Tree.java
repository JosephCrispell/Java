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

public class Tree {

	public ArrayList<Node> tips = new ArrayList<Node>();
	public ArrayList<Node> internalNodes = new ArrayList<Node>();
	
	public Tree(String fileName) throws IOException {
		
		// Get newick string
		ArrayList<Character> newickTree = readNewickFile(fileName);
		
		// Store as traversable nodes
		readNewickNode(newickTree, null);
	}
	
	// Class specific methods
	private int findNodeInfoStart(ArrayList<Character> array){
		
		int index = -1;
		for(int i = 0; i < array.size(); i++) {

			if(array.get(i) == ':' || array.get(i) == '[') {
				index = i;
				break;
			}
		}
		
		return index;
	}

	private ArrayList<Character> readNewickFile(String fileName) throws IOException{
		
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
		
		// Convert the newickNode to a character array
		ArrayList<Character> treeAsCharacters = ArrayListMethods.toArrayList(tree.toString().toCharArray());
		
		return treeAsCharacters;
	}

	private ArrayList<ArrayList<Character>> findNewickSubNodes(ArrayList<Character> newickNode, Node node){
		
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
					newickNodes.add(ArrayListMethods.subsetChar(newickNode, nodeStartIndex, i));
					
					// Extract the node information from the input node (found after all the subNodes)
					extractNodeInformation(newickNode, node, i);
					
					// Finish
					break;
				}
				
			// If there are no open brackets (except the one enclosing the subNodes) and the current character is a comma
			// - reached the end a sub nodes info
			}else if(current == ',' && openBracket == 1){
				
				// Store the newick sub node
				newickNodes.add(ArrayListMethods.subsetChar(newickNode, nodeStartIndex, i));
				
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
	
	private void extractNodeInformation(ArrayList<Character> newickNode, Node node, int lastBracketIndex){
		/**
		 * Newick Node Information is stored in a variable format:
		 *  	Terminal Node: 
		 *  		NodeID[&NodeInfo]:[&BranchInfo]BranchLength
		 *  	Internal Node:
		 *  		(SubNodes)[&NodeInfo]:[&BranchInfo]BranchLength
		 *  	
		 *  
		 *  It is only necessary for the NodeID and BranchLength to be present the rest is optional
		 *  	NOTE - BranchLength and BranchInfo are never present for the root	
		 *  
		 *  3[&rate_range={0.002,6.93},LatLongs1=-44.303,LatLongs2=170.304]:[&rate_range={0.002},length_range={4.000,26.978}]4.200
		 *  
		 *  Variable information can have multiple values, in addition the values could be strings e.g. species="BOVINE", species.set={"BOVINE", "POSSUM"}
		 */
		
		// If this is a terminal node, no last bracket index will be available - need to identify when info begins
		if(lastBracketIndex == -1){
			lastBracketIndex = findNodeInfoStart(newickNode);
		}
		
		// Subset the characters associated with the node information
		ArrayList<Character> nodeInfoCharacters = ArrayListMethods.subsetChar(newickNode, lastBracketIndex + 1, newickNode.size() - 1);
		
		// Initialise variables to store the Information for the current Newick Node
		Hashtable<String, ArrayList<Double>> nodeInfo = new Hashtable();
		Hashtable<String, ArrayList<Double>> branchInfo = new Hashtable();
		
		// Initialise the kays and values for Hashtable
		String variableName = ""; 
		ArrayList<Double> values = new ArrayList<Double>();
		int multipleStrings = 0;
		
		// Initialise Variables to trace progress
		int openCurlyBracket = 0;
		boolean readingNodeInfo = false;
		boolean readingBranchInfo = false;
		int variableStartIndex = -99;
		int branchLengthPresent = 0;
		
		// Start examining each character of the Newick Node String
		for(int i = 0; i < nodeInfoCharacters.size(); i++){
			
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
	
	private Node readNewickNode(ArrayList<Character> newickNode, Node parentNode){
		
		/**
		 * This method is to read a Newick Node. This node is an internal node with associated sub nodes that can be either internal
		 * or terminal nodes. In order to traverse the entire Newick tree this method should be used within itself. For each internal
		 * node all subnodes are explored and their information stored as a tree of nodes within Java.
		 */
		
		// Initialise the current node as an internal node
		this.internalNodes.add(new Node(this.internalNodes.size() - 1, true));
		
		// Find the Sub Nodes for the Current Node
		ArrayList<ArrayList<Character>> newickSubNodes = findNewickSubNodes(newickNode, 
				this.internalNodes.get(this.internalNodes.size() - 1));
		
		String current = "";
		
		// Initialise the indices for each sub node
		int[] subNodeIndices = ArrayMethods.range(this.)
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
