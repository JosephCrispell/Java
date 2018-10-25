package homoplasyFinder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;

public class Tree {

	public ArrayList<Node> terminalNodes = new ArrayList<Node>();
	public ArrayList<Node> internalNodes = new ArrayList<Node>();
	
	public Tree(String fileName) throws IOException {
		
		// Get newick string
		ArrayList<Character> newickTree = readNewickFile(fileName);
		
		// Store as traversable nodes
		readNewickNode(newickTree, null);
	}
	
	// Getting methods
	public ArrayList<Node> getTerminalNodes(){
		return this.terminalNodes;
	}
	public ArrayList<Node> getInternalNodes(){
		return this.internalNodes;
	}
	public int getNInternalNodes() {
		return this.internalNodes.size();
	}
	
	// General methods
	public void print(String fileName) throws IOException {

		if(fileName == null) {
			System.out.println(internalNodes.get(0).toNewickString(this.terminalNodes, this.internalNodes) + ";");
		}else {
			
			// Open the file
			BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
			
			// Print the tree to file
			WriteToFile.writeLn(bWriter, internalNodes.get(0).toNewickString(this.terminalNodes, this.internalNodes) + ";");
			
			// Close the file
			WriteToFile.close(bWriter);
		}		
	}
	public String toString() {
		return internalNodes.get(0).toNewickString(this.terminalNodes, this.internalNodes) + ";";
	}
	
	// Class specific methods
	private String getSubsetAsString(ArrayList<Character> array, int start, int end) {
		return Methods.toStringChar(Methods.subsetChar(array, start, end));
	}
	
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
		
		// Open the newick tree file
    	InputStream input = null;
    	try {
    		input = new FileInputStream(fileName);
    	}catch(FileNotFoundException e){
    		System.err.println((char)27 + "[31mERROR!! The input tree file: \"" + fileName + "\" could not be found!" + (char)27 + "[0m");
    		System.exit(0);
    	}
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
											
		// Initialise a variable to store the newick string
		String line = null;
		StringBuilder tree = new StringBuilder();
												
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			
			tree = tree.append(line);			
		}
		
		// Close the input file
		input.close();
		reader.close();
		
		// Convert the newickNode to a character array
		ArrayList<Character> treeAsCharacters = Methods.toArrayList(tree.toString().toCharArray());
		
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
					newickNodes.add(Methods.subsetChar(newickNode, nodeStartIndex, i));
					
					// Extract the node information from the input node (found after all the subNodes)
					extractNodeInformation(newickNode, node, i, true);
					
					// Finish
					break;
				}
				
			// If there are no open brackets (except the one enclosing the subNodes) and the current character is a comma
			// - reached the end a sub nodes info
			}else if(current == ',' && openBracket == 1){
				
				// Store the newick sub node
				newickNodes.add(Methods.subsetChar(newickNode, nodeStartIndex, i));
				
				// Move the nodeStartIndex on
				nodeStartIndex = i + 1;
			}			
		}
		
		return newickNodes;
	}

	private void extractNodeInformation(ArrayList<Character> newickNode, Node node, int lastBracketIndex, boolean internal){
		/**
		 * Newick Node Information is stored in a variable format:
		 *  	Terminal Node: 
		 *  		NodeID[&NodeInfo]:[&BranchInfo]BranchLength
		 *  	Internal Node:
		 *  		(SubNodes)[&NodeInfo]:[&BranchInfo]BranchLength
		 *  	
		 *  
		 *  It is only necessary for the NodeID to be present the rest is optional
		 *  	NOTE - BranchLength and BranchInfo are never present for the root	
		 *  
		 *  3[&rate_range={0.002,6.93},LatLongs1=-44.303,LatLongs2=170.304]:[&rate_range={0.002},length_range={4.000,26.978}]4.200
		 *  
		 *  Variable information can have multiple values, in addition the values could be strings e.g. species="BOVINE", species.set={"BOVINE", "POSSUM"}
		 */
		
		// If this is a terminal node, no last bracket index will be available - need to identify when info begins
		if(internal == false){
			lastBracketIndex = findNodeInfoStart(newickNode) - 1;
		}
		
		// Check if any information present - possible that branch length isn't present. If not present last Bracket index will be -2
		if(lastBracketIndex != -2) {
			
			// Subset the characters associated with the node information
			ArrayList<Character> infoCharacters = Methods.subsetChar(newickNode, lastBracketIndex + 1, newickNode.size());
			
			// Initialise variables to store the Information for the current Newick Node
			Hashtable<String, ArrayList<Double>> nodeInfo = new Hashtable<String, ArrayList<Double>>();
			Hashtable<String, ArrayList<Double>> branchInfo = new Hashtable<String, ArrayList<Double>>();
			
			// Initialise the kays and values for Hashtable
			String variableName = ""; 
			ArrayList<Double> values = new ArrayList<Double>();
			int multipleStrings = 0;
			
			// Initialise Variables to trace progress
			int openCurlyBracket = 0;
			int readingNodeInfo = 0; // 0 = haven't found, 1 = found, 2 = finished
			int readingBranchInfo = 0; // 0 = haven't found, 1 = found, 2 = finished
			int variableStartIndex = -1;
			boolean branchLengthPresent = false;
			
			// Initialise a variable to record the current character
			char current;
			
			// Start examining each character of the Newick Node String
			for(int i = 0; i < infoCharacters.size(); i++){
				
				// Note current Character
				current = infoCharacters.get(i);
					
				if(current == '['){
							
					// Get the Node ID
					if(internal == false){
						node.setName(getSubsetAsString(infoCharacters, 0, i));
					}
							
					// Which Information are we currently reading?
					if(readingNodeInfo == 0){
						readingNodeInfo = 1;
					}else if(branchLengthPresent == true && variableStartIndex != i){
						
						// Means either no Node Information available or it has already been read - Store the Branch Length if Present
						node.setBranchLength(Double.parseDouble(getSubsetAsString(infoCharacters, variableStartIndex, i)));
											
						// Begin reading the Branch Information if present
						readingBranchInfo = 1;
					}else{
						readingBranchInfo = 1;
					}
							
					// Move into the Information Area - skip the "[&" bit
					variableStartIndex = i + 2;
							
				}else if(current == '='){
							
					// Record the Variable Name
					variableName = getSubsetAsString(infoCharacters, variableStartIndex, i);
					variableStartIndex = i + 1;
							
				}else if(current == '{'){ // Means more than one value for the variable
					openCurlyBracket++;
					variableStartIndex = i + 1;
					
					// Check if the values are a list of Strings e.g. species.set={"BOVINE", "POSSUM"}
					if(Character.isDigit(infoCharacters.get(i + 1)) == false){
						
						// For Variables with multiple Strings: species.set={"BOVINE", "POSSUM"} -> species.set--BOVINE-POSSUM
						variableName = variableName + "--";
						multipleStrings = 1;
						
						// Check if variables are within quotes
						if(infoCharacters.get(i + 1) != '"'){
							multipleStrings = 2;
						}
					}
							
				}else if(current == '}'){ // Finished finding all values for the variable
					openCurlyBracket--;
							
					// Store the Current Value - check that not dealing with multiple Strings
					if(multipleStrings == 0){
						values.add(Double.parseDouble(getSubsetAsString(infoCharacters, variableStartIndex, i)));
					}else if(multipleStrings == 1){
						
						// For Variables with multiple Strings: species.set={"BOVINE", "POSSUM"} -> species.set--BOVINE-POSSUM
						variableName = variableName + getSubsetAsString(infoCharacters, variableStartIndex, i);				
					}else if(multipleStrings == 2){
						
						// For Variables with multiple Strings: species.set={"BOVINE", "POSSUM"} -> species.set--BOVINE-POSSUM
						variableName = variableName + getSubsetAsString(infoCharacters, variableStartIndex, i);
					}
							
				}else if(openCurlyBracket == 1 && current == ','){ // Still inside curly brackets - add value into array for the current variable
					
					// Store the Current Value - check that not dealing with multiple Strings
					if(multipleStrings == 0){
						values.add(Double.parseDouble(getSubsetAsString(infoCharacters, variableStartIndex, i)));
					}else if(multipleStrings == 1){
						
						// For Variables with multiple Strings: species.set={"BOVINE", "POSSUM"} -> species.set--BOVINE-POSSUM
						variableName = variableName + getSubsetAsString(infoCharacters, variableStartIndex + 1, i - 1) + "-";
					
					}else if(multipleStrings == 2){
						
						// For Variables with multiple Strings: species.set={"BOVINE", "POSSUM"} -> species.set--BOVINE-POSSUM
						variableName = variableName + getSubsetAsString(infoCharacters, variableStartIndex, i);
					}
							
					// Move Value index on
					variableStartIndex = i + 1;
							
				}else if(openCurlyBracket == 0 && current == ','){
						
					// Extract the Variable Information
					if(values.isEmpty() && multipleStrings == 0){
						
						// Check that value isn't in a String format e.g. species = "BOVINE"
						if(Character.isDigit(infoCharacters.get(variableStartIndex)) == true){
							values.add(Double.parseDouble(getSubsetAsString(infoCharacters, variableStartIndex, i)));
						
						// Value is a string - check if there are quotes present
						}else if(infoCharacters.get(variableStartIndex) == '"'){
							// Combine the value to the Variable Name: key: species--BOVINE
							variableName = variableName + "--" + getSubsetAsString(infoCharacters, variableStartIndex + 1, i - 1);
							
						}else{
							// Combine the value to the Variable Name: key: species--BOVINE
							variableName = variableName + "--" + getSubsetAsString(infoCharacters, variableStartIndex, i);
						}
						
					}else if(multipleStrings != 0){
						
						multipleStrings = 0; // Finished dealing with variable
					}
					
					// Store the Variable Information
					if(readingNodeInfo == 1){
						nodeInfo.put(variableName, Methods.copyDouble(values));
					}else if(readingBranchInfo == 1){
						branchInfo.put(variableName, Methods.copyDouble(values));
					}
							
					// Reset the Variable Information
					variableStartIndex = i + 1;
					values = new ArrayList<Double>();
							
				}else if(current == ']'){
					
					// Extract the Variable Information
					if(values.isEmpty() && multipleStrings == 0){
						
						// Check that value isn't in a String format e.g. species = "BOVINE"
						if(infoCharacters.get(variableStartIndex) != '"'){
							values.add(Double.parseDouble(getSubsetAsString(infoCharacters, variableStartIndex, i)));
							
						}else if(infoCharacters.get(variableStartIndex) == '"'){
							// Combine the value to the Variable Name: key: species--BOVINE
							variableName = variableName + "--" + getSubsetAsString(infoCharacters, variableStartIndex + 1, i - 1);
						}
						
					}else if(multipleStrings == 1){
						multipleStrings = 0; // Finished dealing with variable
					}
					
					// Store the Variable Information
					if(readingNodeInfo == 1){
						nodeInfo.put(variableName, Methods.copyDouble(values));
					}else if(readingBranchInfo == 1){
						branchInfo.put(variableName, Methods.copyDouble(values));
					}
							
					// Reset the Variable Information
					variableStartIndex = i + 1;
					values = new ArrayList<Double>();
						
					// Note that finished Reading Variable Information
					if(readingNodeInfo == 1){
						readingNodeInfo = 2;
					}else if(readingBranchInfo == 1){
						readingBranchInfo = 2;
					}
							
				}else if(current == ':'){
						
					// Get the Node ID
					if(internal == false){
						node.setName(getSubsetAsString(newickNode, 0, lastBracketIndex + 1));
					}
					
					// For Branch length increase index by 1
					branchLengthPresent = true;
					variableStartIndex = i + 1;
					
					// Note that finished reading nodeInfo if it was present
					readingNodeInfo = 2;
							
				}else if(i == infoCharacters.size() - 1 && node.getBranchLength() == -1 && branchLengthPresent == true){ // If reached end and no BranchLength yet stored
					
					// Make sure Branch Length is stored
					node.setBranchLength(Double.parseDouble(getSubsetAsString(infoCharacters, variableStartIndex, i + 1)));
				}
			}
			
			// Store the node and branch information found
			node.setNodeInfo(nodeInfo);
			node.setBranchInfo(branchInfo);
			
		// If no branch information and this is a terminal node - set ID to the the newick node as ID should be the only thing present
		}else if(internal == false){
			node.setName(Methods.toStringChar(newickNode));
		}
	}
	
	private void readNewickNode(ArrayList<Character> newickNode, Node parentNode){
		
		/**
		 * This method is to read a Newick Node. This node is an internal node with associated sub nodes that can be either internal
		 * or terminal nodes. In order to traverse the entire Newick tree this method should be used within itself. For each internal
		 * node all subnodes are explored and their information stored as a tree of nodes within Java.
		 */
		
		// Remove last character if it is a semi-colon
		if(newickNode.get(newickNode.size() - 1) == ';') {
			newickNode.remove(newickNode.size() - 1);
		}
		
		// Initialise the current node as an internal node
		Node node = new Node(this.internalNodes.size(), true);
		this.internalNodes.add(node);
		
		// Set the parent node index of the current node if it exists
		if(parentNode != null) {
			node.setParentIndex(parentNode.getIndex());
		}
		
		// Find the Sub Nodes for the Current Node
		ArrayList<ArrayList<Character>> newickSubNodes = findNewickSubNodes(newickNode, node);
		ArrayList<Character> currentNewickSubNode;
		
		// Examine each of the SubNodes
		for(int i = 0; i < newickSubNodes.size(); i++){
			currentNewickSubNode = newickSubNodes.get(i);
			
			// Is the Current newick Sub node an Internal or Terminal Node?
			if(currentNewickSubNode.get(0) == '('){ // I.e. does it start with a bracket "("? - If it does then it is an internal node
				
				// Assign the current sub node to its parent
				node.addSubNodeReference(this.internalNodes.size(), true);
				
				// Create and Store this Internal Node
				readNewickNode(currentNewickSubNode, node);				
				
			}else{
				
				// Create and Store this Terminal Node
				Node terminalNode = new Node(this.terminalNodes.size(), false);
				this.terminalNodes.add(terminalNode);
				
				// Get the terminal node information
				extractNodeInformation(currentNewickSubNode, terminalNode, -1, false);
				
				// Assign the current sub node to its parent
				node.addSubNodeReference(this.terminalNodes.size() - 1, false);
			}
		}
	}

}
