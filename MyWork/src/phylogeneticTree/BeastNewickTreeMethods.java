package phylogeneticTree;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;
import java.util.SortedSet;

import methods.*;


public class BeastNewickTreeMethods {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		//*************** CALCULATE THE SPATIAL DIFFUSION DISTRIBUTION *****************//
		
		// Prepare the Output File
		String outFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/BuildingTree/runSets/runs_03-01-15/run_03-01-15_1_HKY_Exponential_Constant_LatLongs_Cauchy/spatialDiffusionDistribution.txt";
		BufferedWriter bWriter = WriteToFile.openFile(outFile, false);
		//WriteToFile.writeLn(bWriter, "TreeNo\tMeanDiffusionRate\tMeanDiffusionCoeffient\tWADiffusionRate\tTotalDiffusionCoefficient");
		
		// Read the Nexus File
		String nexusFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/BuildingTree/runSets/runs_03-01-15/run_03-01-15_1_HKY_Exponential_Constant_LatLongs_Cauchy/NZ_03-01-15_1_HKY_Exponential_Constant_LatLongs_Cauchy_MCC.tree";
		BeastNewick newickTreeInfo = readBeastFormattedNexus(nexusFile, 1);
		
		// Initialise Variables for summarising Spatial Diffusion Rate Distribution information
		double meanDiffusionRate = 0;
		double meanDiffusionCoefficient = 0;
		double waDiffusionRate = 0;
		double totalDiffusionCoefficient = 0;
		
		// Examine each of the Newick Trees in the File
		int treeCount = 0;
		for(String newickTree : newickTreeInfo.getNewickTrees()){
			treeCount++;
			
			System.out.println("\n\nReading Tree No. " + treeCount);
			
			Node tree = readNewickNode(newickTree, new Node(null, null, null));
			findSpatialDiffusionDistribution(tree, 'K', "LatLongs");
			
			// Summarise the Spatial Diffusion Distribution for the current Tree
			meanDiffusionRate = ArrayMethods.mean(NodeCalculationResults.rateEstimates);
			meanDiffusionCoefficient = ArrayMethods.mean(NodeCalculationResults.diffusionCoefficients);
			waDiffusionRate = NodeCalculationResults.totalDistance / NodeCalculationResults.totalTime;
			totalDiffusionCoefficient = NodeCalculationResults.totalDiffusionCoefficient;
			
			//WriteToFile.writeLn(bWriter, treeCount + "\t" + meanDiffusionRate + "\t" + meanDiffusionCoefficient + "\t" + waDiffusionRate + "\t" + totalDiffusionCoefficient);
			WriteToFile.writeLn(bWriter, "DiffusionRateEstimate\tDiffusionCoefficients");
			for(int i = 0; i < NodeCalculationResults.rateEstimates.length; i++){
				WriteToFile.writeLn(bWriter, NodeCalculationResults.rateEstimates[i] + "\t" + NodeCalculationResults.diffusionCoefficients[i]);
			}
			
			System.out.println("Weighted Average Diffusion Rate: " + waDiffusionRate);
			System.out.println("Total Diffusion Coefficient: " + totalDiffusionCoefficient);
			
			// Clear the information for the current tree - prepare for the next
			NodeCalculationResults.restoreDefaults();
			
		}
		WriteToFile.close(bWriter);
		
		//************************ GET THE NODE INFORMATION ********************************//
		
//		String nexusFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/BuildingTree/runSets/runs_03-01-15/run_03-01-15_1_HKY_Exponential_Constant_LatLongs_Cauchy/NZ_03-01-15_1_HKY_Exponential_Constant_LatLongs_Cauchy_MCC.tree";
//		
//		BeastNewick nexusInfo = readBeastFormattedNexus(nexusFile, 1, "AgR(.*)");
//		
//		Node tree = readNewickNode(nexusInfo.getNewickTrees()[0], new Node(null, null, null));
//		
//		String outFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/BuildingTree/runSets/runs_03-01-15/run_03-01-15_1_HKY_Exponential_Constant_LatLongs_Cauchy/nodeInformation.txt";
//		
//		BufferedWriter bWriter = WriteToFile.openFile(outFile, false);
//		
//		printNodeInformation(tree, bWriter, nexusInfo.getSampleNames());
//		
//		printNodeInfoTableHeader(Global.variables);
//		
//		WriteToFile.close(bWriter);
		
	}
	
	public static void printNodeInfoTableHeader(Hashtable<String, Integer> variables){
		
		String[] varNames = new String[variables.size()];
		
		Set<String> keys = variables.keySet();
		for(String key : keys){
			varNames[variables.get(key)] = key;
		}
		
		System.out.println("NodeNo\tNodeID\t" + ArrayMethods.toString(varNames, "\t"));
	}
	
	public static String[] returnNodeVariableInfo(NodeInfo info){
		
		// Initialise String Array to store Variable Info - note default large size
		String[] varInfo = new String[10000];
				
		// Extract the Node Information
		Hashtable<String, double[]> nodeInfo = info.getNodeInfo();
		Hashtable<String, double[]> branchInfo = info.getBranchInfo();
		double branchLength = info.getBranchLength();
		
		// Store the BranchLength
		varInfo[0] = Double.toString(branchLength);
		Global.variables.put("branchLength", 0);
		
		// Examine the Hashtables
		varInfo = returnVarInfoFromHash(nodeInfo, varInfo);
		varInfo = returnVarInfoFromHash(branchInfo, varInfo);
		
		// Return the Array of Variable Values with only the used elements
		return ArrayMethods.subset(varInfo, 0, Global.lastIndexUsed);
	}
	
	public static String[] returnVarInfoFromHash(Hashtable<String, double[]> info, String[] varValues){
		
		// Extract the Variable Names form the Hash table
		Set<String> keys = info.keySet();
		
		// Initialise emtpy variables
		double[] values;
		String[] parts;
		String[] stringValues;
		String variableName;
		
		// Examine each of the variables and their associated information
		for(String key : keys){
					
			values = info.get(key);
				
			if(values.length > 1){
				
				for(int i = 0; i < values.length; i++){
					
					variableName = key + "_" + i;
					
					if(Global.variables.get(variableName) != null){
						varValues[Global.variables.get(variableName)] = Double.toString(values[i]);
					}else{
						Global.lastIndexUsed++;
						Global.variables.put(variableName, Global.lastIndexUsed);
						varValues[Global.variables.get(variableName)] = Double.toString(values[i]);
					}
				}
						
			}else if(values.length == 1){
					
				if(Global.variables.get(key) != null){
					varValues[Global.variables.get(key)] = Double.toString(values[0]);
				}else{
					Global.lastIndexUsed++;
					Global.variables.put(key, Global.lastIndexUsed);
					varValues[Global.variables.get(key)] = Double.toString(values[0]);
				}
						
			}else{ // No values information available - means value(s) were in a string format
						
				/**
				 * species="BOVINE" stored as: species--BOVINE
				 * species.set{"BOVINE","POSSUM"} stored as: species--BOVINE-POSSUM
				 */
						
				parts = key.split("--");
				stringValues = parts[1].split("-");
						
				if(stringValues.length == 1){
					
					if(Global.variables.get(key) != null){
						varValues[Global.variables.get(key)] = stringValues[0];
					}else{
						Global.lastIndexUsed++;
						Global.variables.put(key, Global.lastIndexUsed);
						varValues[Global.variables.get(key)] = stringValues[0];
					}
					
				}else{
					
					for(int i = 0; i < stringValues.length; i++){
						
						variableName = key + "_" + i;
						
						if(Global.variables.get(variableName) != null){
							varValues[Global.variables.get(variableName)] = stringValues[i];
						}else{
							Global.lastIndexUsed++;
							Global.variables.put(variableName, Global.lastIndexUsed);
							varValues[Global.variables.get(variableName)] = stringValues[i];
						}
					}		
				}
			}
		}
		
		return varValues;
	}
	
	public static void printNodeInformation(Node node, BufferedWriter bWriter, Hashtable<Integer, String> sampleNames) throws IOException{
		// Extract the Information for the Current Node
		NodeInfo info = node.getNodeInfo();
		
		// Get Node Information into a table like format
		String[] varInfo = returnNodeVariableInfo(info);
		
		// Convert the Variable String values into a Table Line
		String varInfoLine = ArrayMethods.toString(varInfo, "\t");
		
		// Assign the current Internal Node a Node No
		Global.nodeNo++;
						
		// Initialise a variable to store the Node Label for any terminal nodes
		String nodeLabel = "";
				
		// Print out the Information associated with the current Node
		WriteToFile.writeLn(bWriter, Global.nodeNo + "\t" + "NA" + "\t" + varInfoLine);
						
		// Explore the SubNodes for the current Node
		for(Node subNode : node.getSubNodes()){
					
			// Is the current Node and Internal Node?
			if(subNode.getSubNodes().length != 0){
								
				// Examine this internal node
				printNodeInformation(subNode, bWriter, sampleNames);
									
			}else{
									
				// Get the Node Information for the current terminal node
				info = subNode.getNodeInfo();
				
				System.out.println(Integer.parseInt(info.getNodeId()) + "\t" + sampleNames.get(Integer.parseInt(info.getNodeId())));
				
				// Get the Node Label or the current terminal node
				nodeLabel = sampleNames.get(Integer.parseInt(info.getNodeId()));
				
				// Get Node Information into a table like format
				varInfo = returnNodeVariableInfo(info);
				
				// Convert the Variable String values into a Table Line
				varInfoLine = ArrayMethods.toString(varInfo, "\t");
									
				// Assign the current Terminal Node a Node No
				Global.nodeNo++;
				
				// Print out the Information associated with the current Node
				WriteToFile.writeLn(bWriter, Global.nodeNo + "\t" + nodeLabel + "\t" + varInfoLine);						
			}
		}
	}
	
	public static void findSpatialDiffusionDistribution(Node node, char unit, String label){
		
		/**
		 * Different Spatial Diffusion Estimate - Summarising Branch Specific Diffusion Rates
		 * 	
		 * 	Each branch is examined independently. The latitude and longitude points at either end are used to to 
		 * 	calculate the spatial distance and this is compared to the temporal distance that separates the dated
		 *  ends.
		 *  
		 *  				  Spatial Distance
		 *  Diffusion Rate = ------------------	= X km/Year
		 *  				   Branch Length
		 *  
		 *  Branch Length is the distance in evolutionary time between the nodes
		 * 
		 */
		
		// Get the Location and Time information for the current node
		NodeInfo nodeInfo = node.getNodeInfo();
		double[] latLongs = getLocationInformation(nodeInfo, label);
		
		// Initialise Location and Time variables for subNodes
		NodeInfo subNodeInfo;
		double branchLength; // Branch Length is the distance in evolutionary time from subNode back to its parent Node
		double[] subNodeLatLongs = new double[2];
		
		// Initialise variable to store the calculated spatial distance
		double spatialDistance;
		double diffusionCoefficient;
		double spatialDiffusionEstimate;
		
		// Examine each of the Sub Nodes for the current node
		for(Node subNode : node.getSubNodes()){
			
			// Get the Location and Time Information for the current sub node
			subNodeInfo = subNode.getNodeInfo();
			branchLength = subNodeInfo.getBranchLength();
			subNodeLatLongs = getLocationInformation(subNodeInfo, label);
			
			// Calculate the Spatial Distance between the current Sub Node and its parent Node
			spatialDistance = LatLongMethods.distance(latLongs[0], latLongs[1], subNodeLatLongs[0], subNodeLatLongs[1], unit);
			
			// **** Make the different spatial distribution calculations ****
			
			// Weighted Average Diffusion Rate
			NodeCalculationResults.totalTime += branchLength;
			NodeCalculationResults.totalDistance += spatialDistance;
			
			// Coefficient of Variation for Diffusion Rate
			diffusionCoefficient = Math.pow(spatialDistance, 2) / (4 * branchLength);
			NodeCalculationResults.diffusionCoefficients = ArrayMethods.append(NodeCalculationResults.diffusionCoefficients, diffusionCoefficient);
			NodeCalculationResults.totalDiffusionCoefficient += diffusionCoefficient * branchLength;
			
			// Branch Specific Spatial Diffusion Estimate
			spatialDiffusionEstimate = spatialDistance / branchLength;
			NodeCalculationResults.rateEstimates = ArrayMethods.append(NodeCalculationResults.rateEstimates, spatialDiffusionEstimate);
			
			// If current Sub Node is an internal then need to examine it's subNodes
			if(subNode.getSubNodes().length != 0){
				findSpatialDiffusionDistribution(subNode, unit, "LatLongs");
			}
			
		}
	}
	
	public static double[] getLocationInformation(NodeInfo info, String label){
		
		/**
		 * Latitude and Longitude information can be stored in a variety of ways:
		 * 		LatLongs={latitude, longitude}
		 * 		LatLongs1=latitude	LatLongs2=longitude
		 * In addition this information can be stored in either the Branch or Node Information
		 */
		
		// Initialise variable to store Latitude and Longitude Information
		double[] latLongs = new double[2];
		int done = 0;
		
		// Check the Node Information for the LatLong information
		Hashtable<String, double[]> nodeInfo = info.getNodeInfo();
		if(nodeInfo.get(label) != null){
			latLongs = nodeInfo.get(label);
			done = 1;
		}else if(nodeInfo.get(label + "1") != null && nodeInfo.get(label + "2") != null){
			latLongs[0] = nodeInfo.get(label + "1")[0];
			latLongs[1] = nodeInfo.get(label + "2")[0];
			done = 1;
		}
		
		// Check the Branch Information for the LatLong Information if didn't find it in the Node Information
		if(done == 0){
			Hashtable<String, double[]> branchInfo = info.getBranchInfo();
			if(branchInfo.get(label) != null){
				latLongs = branchInfo.get(label);
				done = 1;
			}else if(branchInfo.get(label + "1") != null && branchInfo.get(label + "2") != null){
				latLongs[0] = branchInfo.get(label + "1")[0];
				latLongs[1] = branchInfo.get(label + "2")[1];
				done = 1;
			}
		}
		
		// Chuck error if couldn't find Latitude and Longitude Information
		if(done == 0){
			System.out.println("ERROR: BeastNewickTreeMethods:getLocationInformation: Couldn't find Location Information in the Information Provided");
		}
		
		return latLongs;
	}
	
	public static void printNode(Node node){
		
		/**
		 * Internal Node:
		 * 		(SubNodes)[&NodeInfo]:[&BranchInfo]BranchLength
		 * 
		 * Terminal Node:
		 * 		NodeId[&NodeInfo]:[&BranchInfo]BranchLength
		 */
		
		System.out.print("(");
		
		Node[] subNodes = node.getSubNodes();
		Node current;
		NodeInfo terminalNodeInfo;
		
		for(int i = 0; i < subNodes.length; i++){
			
			current = subNodes[i];
			
			// Is the current Node and Internal Node?
			if(current.getSubNodes().length != 0){
				
				// Examine this internal node
				printNode(current);
				
				if(i < subNodes.length - 1){
					System.out.print(",");
				}
				
			}else{
				
				// Get the Node Information for the current terminal node
				terminalNodeInfo = current.getNodeInfo();
				
				// Make Node Info into informative String
				System.out.print(terminalNodeInfo.getNodeId() + buildNodeInformation(terminalNodeInfo));
				
				if(i < subNodes.length - 1){
					System.out.print(",");
				}
			}
		}
		
		System.out.print(")" + buildNodeInformation(node.getNodeInfo()));
		
	}

	public static String buildNodeInformation(NodeInfo info){
		
		// Get the Node Information
		Hashtable<String, double[]> nodeInfo = info.getNodeInfo();
		
		// Get the Branch Information
		Hashtable<String, double[]> branchInfo = info.getBranchInfo();
		
		// Get the Branch Length
		double branchLength = info.getBranchLength();
		
		// Initialise an Output string
		String output = "";
		
		// Is there Node Information present?
		if(nodeInfo.size() != 0){
			output = output + returnVariableInformationNewickFormat(nodeInfo);
		}
		
		// Is there Branch Information present?
		if(branchInfo.size() != 0){
			output = output + ":" + returnVariableInformationNewickFormat(branchInfo);
		}
		
		// Is there a Branch Length present?
		if(branchLength != -99 && branchInfo.size() != 0){
			output = output + branchLength;
		}else if(branchLength != -99){ // Allow for Branch Info not being present but there still being a Branch length
			output = output + ":" + branchLength;
		}
		
		return output;
		
	}
	
	public static String returnVariableInformationNewickFormat(Hashtable<String, double[]> info){
		
		String out = "[&";
		
		double[] values;
		String[] parts;
		String[] stringValues;
		Set<String> keys = info.keySet();
		
		int noKeys = info.size();
		int y = 0;
		for(String key : keys){
			y++;
			
			values = info.get(key);
			
			if(values.length > 1){	
				
				out = out + key + "=" + "{";
								
				for(int i = 0; i < values.length; i++){
					out = out + values[i];
				
					if(i < values.length - 1){
						out = out + ",";
					}
				}
				
				out = out + "}";
				
			}else if(values.length == 1){
			
				out = out + key + "=" + values[0];
				
			}else{ // No values information available - means value(s) were in a string format
				
				/**
				 * species="BOVINE" stored as: species--BOVINE
				 * species.set{"BOVINE","POSSUM"} stored as: species--BOVINE-POSSUM
				 */
				
				parts = key.split("--");
				stringValues = parts[1].split("-");
				
				if(stringValues.length == 1){
					out = out + parts[0] + "=" + "\"" + parts[1] + "\"";
				}else{
					out = out + parts[0] + "=" + "{";
					for(int i = 0; i < stringValues.length; i++){
						out = out + "\"" + stringValues[i] + "\"";
						
						if(i < stringValues.length - 1){
							out = out + ",";
						}
					}
					out = out + "}";
				}
			}
			
			if(y != noKeys){
				out = out + ",";
			}			
		}
		
		return out + "]";
		
	}
	
	public static Node readNewickNode(String newickNode, Node parentNode){
		
		/**
		 * This method is to read a Newick Node. This node is an internal node with associated sub nodes that can be either internal or terminal nodes.
		 * In order to traverse the entire Newick tree this method should be used within itself. For each Internal Node all subnodes are explored and
		 * and there information stored as a tree of nodes within Java.
		 */
		
		// Find the Sub Nodes for the Current Node
		String[] newickSubNodes = findNewickSubNodes(newickNode);
		String current = "";
		
		// Initialise array to store the Sub Nodes for the current Node
		Node[] subNodes = new Node[newickSubNodes.length];
		int posUsed = -1;
		
		// Create the Current Node
		NodeInfo nodeInfo = extractNodeInformation(newickNode, 1);
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
	
	public static String[] findNewickSubNodes(String newickNode){
		
		// Convert the Newick Node String into a character array
		char[] chars = newickNode.toCharArray();
		char current = 'Z';
		
		// Create Variable to track open Brackets
		int openBracket = 0;		// ( [ {	} ] )
		
		// Create Variable to node where a Newick Sub Node begins
		int nodeStartIndex = 1;
		
		// Initialise Array to Store the Newick Sub Node strings
		String[] newickNodes = new String[0];
		
		// Examine each of the characters in the Newick Node String
		for(int i = 0; i < chars.length; i++){
			current = chars[i];
			
			// If the current character is a opening bracket record it
			if(current == '(' || current == '[' || current == '{'){
				openBracket++;
				continue;
				
			// If the current character is a closing bracket record it
			}else if(current == ')' || current == ']' || current == '}'){
				openBracket--;
				
				// Are we finished with sub Node Information?
				if(openBracket == 0){
					
					// Store the latest Newick Node
					newickNodes = ArrayMethods.append(newickNodes, newickNode.substring(nodeStartIndex, i));
					
					// Finish
					break;
				}
				
				continue;
				
			// If there are no open brackets (except the one enclosing the subNodes) and the current character is a comma - reached the end a sub nodes info
			}else if(current == ',' && openBracket == 1){
				
				// Store the newick sub node
				newickNodes = ArrayMethods.append(newickNodes, newickNode.substring(nodeStartIndex, i));
				
				// Move the nodeStartIndex on
				nodeStartIndex = i + 1;
				continue;
				
			}			
		}
		
		return newickNodes;
	}
	
	public static String removeSubNodeInfo(String newickNode){
		
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
	
	public static Hashtable<String, double[]> extractTreeInformation(String treeVariableInformation){
	
		/**
		 * Tree information can be stored in the following format
		 * [&lnP="SOMETHING",posterior=-212.0536341712189,precision={5.0000e-02,2.0000e-03,5.0000e-02},lnP=-19.993426049692662,LatLongs.traitLikelihood=-19.993426049692662]
		 * 
		 * 	Variables can have multiple values, and can be Strings
		 */
		
		// Split the Tree Information into a character array
		char[] chars = treeVariableInformation.toCharArray();
		char current;
		
		// Initialise the variable information
		String variableName = "";
		int variableStartIndex = -99;
		double[] values = new double[100];
		int posUsed = -1;
		
		// Initialise variables to trace progress
		int openCurlyBracket = 0;
		
		// Initialise Hashtable for store tree Information
		Hashtable<String, double[]> treeInfo = new Hashtable();
		
		// Begin examining each of the characters
		for(int i = 0; i < chars.length; i++){
			current = chars[i];
			
			if(current == '['){
				
				// Move into the Information Area - skip the "[&" bit
				variableStartIndex = i + 2;
				continue;
			}else if(current == '='){
				
				// Record the Variable Name
				variableName = treeVariableInformation.substring(variableStartIndex, i);
				variableStartIndex = i + 1;
				
			}else if(current == '{'){ // Means more than one value for the variable
				openCurlyBracket++;
				variableStartIndex = i + 1;
				continue;
						
			}else if(current == '}'){ // Finished finding all values for the variable
				openCurlyBracket--;
						
				// Store the Current Value
				posUsed++;
				values[posUsed] = Double.parseDouble(treeVariableInformation.substring(variableStartIndex, i));
				continue;
						
			}else if(openCurlyBracket == 1 && current == ','){ // Still inside curly brackets - add value into array for the current variable
					
				// Store the Current Value
				posUsed++;
				values[posUsed] = Double.parseDouble(treeVariableInformation.substring(variableStartIndex, i));
						
				// Move Value index on
				variableStartIndex = i + 1;
				continue;
			}else if(openCurlyBracket == 0 && current == ','){
				
				// Extract the Variable Information
				if(posUsed == -1){
				
					// Check that value isn't in a String format e.g. species = "BOVINE"
					if(chars[variableStartIndex] != '"'){
						posUsed++;
						values[posUsed] = Double.parseDouble(treeVariableInformation.substring(variableStartIndex, i));
					
					}else{
						// Combine the value to the Variable Name and assign an empty values array: key: species--BOVINE
						variableName = variableName + "--" + treeVariableInformation.substring(variableStartIndex + 1, i - 1);
						posUsed++;
						values[posUsed] = -1;
					}
				}
			
				// Store the Variable Information
				treeInfo.put(variableName, ArrayMethods.subset(values, 0, posUsed));
					
				// Reset the Variable Information
				variableStartIndex = i + 1;
				posUsed = -1;
				values = new double[100];
				continue;
					
			}else if(current == ']'){
				
				// Extract the Variable Information
				if(posUsed == -1){
			
					// Check that value isn't in a String format e.g. species = "BOVINE"
					if(chars[variableStartIndex] != '"'){
						posUsed++;
						values[posUsed] = Double.parseDouble(treeVariableInformation.substring(variableStartIndex, i));
				
					}else{
						// Combine the value to the Variable Name and assign an empty values array: key: species--BOVINE
						variableName = variableName + "--" + treeVariableInformation.substring(variableStartIndex + 1, i - 1);
						posUsed++;
						values[posUsed] = -1;
					}
				}
				
				// Store the Variable Information
				treeInfo.put(variableName, ArrayMethods.subset(values, 0, posUsed));
				break;
			}
		}
		
		return treeInfo;
	}
	
	public static NodeInfo extractNodeInformation(String newickNode, int internal){
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
		if(internal == 1){
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

	public static BeastNewick readBeastFormattedNexus(String fileName, int noTrees) throws NumberFormatException, IOException{
		/**
		 * Read a newick tree string from a BEAST tree formatted NEXUS file - Note can contain multiple trees
		 * 
		 * Structure:
		 * 		#NEXUS
		 *
		 *		Begin taxa;
		 *			Dimensions ntax=3;
		 *			Taxlabels
		 *				IsolateA
		 *				IsolateB
		 *				IsolateC
		 *				;
		 *		end;
		 *
		 *		Begin trees;
		 *			Translate
		 *				1 IsolateA,
		 *				2 IsolateB,
		 *				3 IsolateC
		 *				;
		 *		tree STATE_0 [&lnP= ,...] = [&R] NEWICKSTRING
		 *		tree STATE_50000 [&lnP= ,...] = [&R] NEWICKSTRING
		 *		tree STATE_100000 [&lnP= ,...] = [&R] NEWICKSTRING 
		 *		End;
		 *
		 *	(Note sample names in NEWICK string replaced with Integers)
		 *
		 *	When multiple trees present STATE and tree Information ([&lnP...]) present
		 */
		
		// Open the input File
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
				
		// Initialise Hashtable to record sample names and their associated Integers
		Hashtable<Integer, String> sampleNames = new Hashtable<Integer, String>();
		String[] parts;
		String[] subParts;
		
		// Initialise array to store all the newick trees
		String[] trees = new String[noTrees];
		int treePos = -1;
		
		// Initialise array to store all the tree states
		int[] treeStates = new int[noTrees];
		
		// Initialise array of Hashtables to store information relevant to each of the trees
		Hashtable<String, double[]>[] treeInfo = new Hashtable[noTrees];
				
		// Begin reading the Input file
		int nodeLabelBlockStarted = 0;
		String line = null;
	    while(( line = reader.readLine()) != null){
			    	
	    	// Build the Sample Names Hashtable
	    	if(line.matches("(^\tTranslate.*)")){
	    		
	    		nodeLabelBlockStarted = 1;
	    	}else if(nodeLabelBlockStarted == 1 && line.matches("(.*[0-9].*)")){
	    	
	    		// Split the File line by white space
	    		parts = line.split("( +)");
		    	subParts = parts[parts.length - 2].split("\t");
	    		
	    		// Extract the Integer that indexes the Sample in the newick tree
	    		int number = Integer.parseInt(subParts[subParts.length - 1]);
			    		
	    		// Extract the sample name - Remove last character
	    		String sampleName = "";
	    		if(parts[parts.length - 1].substring(parts[parts.length - 1].length() - 1).equals(",")){
	    			sampleName = parts[parts.length - 1].substring(0, parts[parts.length - 1].length() - 1);
	    		}else{
	    			sampleName = parts[parts.length - 1];
	    		}
			    		
	    		// Store the Indexed Sample Name
	    		sampleNames.put(number, sampleName);
	    		
	    	}else if(nodeLabelBlockStarted == 1 && line.matches("(.*;.*)")){
	    		
	    		nodeLabelBlockStarted = 0;

	    	}else if(line.matches("tree(.*)") == true){
	    		
	    		// Split the File line by white space
	    		parts = line.split("( +)");
	    		
	    		if(noTrees > 1){
	    		
	    			// Store the Newick formatted tree String
	    			treePos++;
	    			trees[treePos] = parts[5];
	    		    		
	    			// Extract the Tree State
	    			treeStates[treePos] = Integer.parseInt(parts[1].split("_")[1]);
	    		
	    			// Extract the Tree Information
	    			treeInfo[treePos] = extractTreeInformation(parts[2]);
	    		}else{
	    			// Store the Newick formatted tree String
	    			treePos++;
	    			trees[treePos] = parts[parts.length - 1].substring(0, parts[parts.length - 1].length() - 1);
	    		}
	    	}
	    }
		
	    input.close();
	    reader.close();	
	    
	    return new BeastNewick(sampleNames, treeStates, trees, treeInfo);
	}
	
}
