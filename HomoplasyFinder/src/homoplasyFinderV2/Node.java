package homoplasyFinderV2;

import java.util.ArrayList;
import java.util.Hashtable;

public class Node {

	public String name; // Only available for the tips (unless added, consistency index labels inconsistent positions at some internal nodes)
	public int index;
	public boolean internal;
	
	public int parentIndex = -1; // -1 if root
	public ArrayList<Integer> subNodeIndices = new ArrayList<Integer>();
	public ArrayList<Boolean> subNodeTypes = new ArrayList<Boolean>();
	
	public double branchLength = -1;
	
	public Hashtable<String, ArrayList<Double>> nodeInfo;
	public Hashtable<String, ArrayList<Double>> branchInfo;
	
	public Node(int id, boolean isInternal) {
		
		this.index = id;
		this.internal = isInternal;
	}
	
	// Setting methods
	public void setName(String id) {
		this.name = id;
	}
	public void setBranchLength(double value) {
		this.branchLength = value;
	}
	public void setNodeInfo(Hashtable<String, ArrayList<Double>> info) {
		this.nodeInfo = info;
	}
	public void setBranchInfo(Hashtable<String, ArrayList<Double>> info) {
		this.branchInfo = info;
	}
	public void setParentIndex(int index) {
		this.parentIndex = index;
	}
	
	// Getting methods
	public double getBranchLength() {
		return this.branchLength;
	}
	public int getIndex() {
		return this.index;
	}
	public String getName() {
		return this.name;
	}
	public ArrayList<Integer> getSubNodeIndices() {
		return this.subNodeIndices;
	}
	public ArrayList<Boolean> getSubNodeTypes() {
		return this.subNodeTypes;
	}
	
	// General methods
	public void addSubNodeReference(int index, boolean isInternal) {
		this.subNodeIndices.add(index);
		this.subNodeTypes.add(isInternal);
	}
	
	public static String returnVariableInformationNewickFormat(Hashtable<String, ArrayList<Double>> info){
		
		// Start building the output string
		String output = "[&";
		
		// Initialise a variable to store the values associated with a given variable
		ArrayList<Double> values;
		
		// Initialise variables for parsing the variable names and values
		String[] parts;
		String[] stringValues;
		
		// Get the variables from the hashtable
		String[] keys = Methods.getKeysString(info);
		
		// Examine each variable
		for(int keyIndex = 0; keyIndex < keys.length; keyIndex++){
			
			// Get the values associated with the current variable
			values = info.get(keys[keyIndex]);
			
			// Check if multiple values are present
			if(values.size() > 1){	
				
				// Add the variable name into the output and the values
				output = output + keys[keyIndex] + "={" + Methods.toStringDouble(values, ",") + "}";
			
			// Check if only one value given
			}else if(values.size() == 1){
			
				// Add variable name and value into the output
				output = output + keys[keyIndex] + "=" + values.get(0);
			
			// If no values present, values were strings and combined into key
			}else{
				
				/**
				 * species="BOVINE" stored as: species--BOVINE
				 * species.set{"BOVINE","POSSUM"} stored as: species--BOVINE-POSSUM
				 */
				
				// Split the key into its parts
				parts = keys[keyIndex].split("--");
				
				// Split the values
				stringValues = parts[1].split("-");
				
				// Check if only one value present
				if(stringValues.length == 1){
					
					// Add variable name and value into the output
					output = output + parts[0] + "=" + "\"" + parts[1] + "\"";
					
				// If multiple values present
				}else{
					
					// Add the variable nameinto the output
					output = output + parts[0] + "=" + "{";
					
					// Add each of the values (in quotes) into the output
					for(int i = 0; i < stringValues.length; i++){
						output = output + "\"" + stringValues[i] + "\"";
						
						if(i < stringValues.length - 1){
							output = output + ",";
						}
					}
					
					// Finish with a bracket
					output = output + "}";
				}
			}
			
			// Add comma to separate variables, as long as we haven't reached the last one
			if(keyIndex < keys.length - 1){
				output = output + ",";
			}			
		}
		
		return output + "]";
		
	}
	
	public String toNewickString(ArrayList<Node> terminalNodes, ArrayList<Node> internalNodes) {
		
		/**
		 * Internal Node:
		 * 		(SubNodes)[&NodeInfo]:[&BranchInfo]BranchLength
		 * 
		 * Terminal Node:
		 * 		NodeId[&NodeInfo]:[&BranchInfo]BranchLength
		 */
		
		// Start building the output string
		String output = "(";
		
		// Examine each of the sub nodes
		for(int i = 0; i < this.subNodeIndices.size(); i++) {
			
			// Get the current sub node's information
			int index = this.subNodeIndices.get(i);
			boolean isInternal = this.subNodeTypes.get(i);
			
			// Is the current sub-node internal?
			if(isInternal) {
							
				// Convert this current sub-node to a string and add it to the output
				output += internalNodes.get(index).toNewickString(terminalNodes, internalNodes);
				
				// Add node name if provided
				if(internalNodes.get(index).getName() != null) {
					output += internalNodes.get(index).getName();
				}
				
				// Add node information if available
				if(this.nodeInfo.size() != 0) {
					output += returnVariableInformationNewickFormat(this.nodeInfo);
				}
				
				// Add branch information if available
				output += ":";
				if(this.branchInfo.size() != 0) {
					output += returnVariableInformationNewickFormat(this.branchInfo);
				}
				
				// Add the branch length
				output += internalNodes.get(index).getBranchLength();
			}else {
				
				// Print the current tip's information
				output += terminalNodes.get(index).getName();
				
				// Add node information if available
				if(this.nodeInfo.size() != 0) {
					output += returnVariableInformationNewickFormat(this.nodeInfo);
				}
				
				// Add branch information if available
				output += ":";
				if(this.branchInfo.size() != 0) {
					output += returnVariableInformationNewickFormat(this.branchInfo);
				}
				
				// Add the branch length
				output += terminalNodes.get(index).getBranchLength();
			}			
			
			// Add comma to separate current sub-node from the next - if haven't reached last sub-node
			if(i < this.subNodeIndices.size() - 1) {
				output += ",";
			}
		}
		
		return output + ")";
	}	
}
