package filterSensitivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import methods.*;


public class NJTreeMethods {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// Method Testing Area
		String file = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/AllRawReads/vcfFiles/samplesfasta.txt";
		Sequence[] sequences = DistanceMatrixMethods.readFastaFile(file);
		
		DistanceMatrix distanceMatrix = DistanceMatrixMethods.buildDistanceMatrix(sequences, "pDistance");

		Node tree = buildNJTree(distanceMatrix);
		
		String treeFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/AllRawReads/vcfFiles/treeFile.txt";
		BufferedWriter out = WriteToFile.openFile(treeFile, false);
		printNode(tree, out);
		WriteToFile.close(out);
			
	}
	
	// Methods for examining the Phylogenetic Tree - Stored as Nodes
		
	/**
	 *  Methods for Building Neighbour Joining Tree from Newick Tree file
	 *  String file = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/tree.txt";
	 *	String newickTree = returnNewickString(file);
	 *	
	 *	Node tree = examineNewickNode(newickTree);
	 */
	
	public static Node examineNewickNode(String nodeInfo){
		/**
		 * Newick Format:
		 * A:1,B:2,(C:3,D:4):5
		 */
		
		Node[] subNodes = new Node[0];
		double[] branchLengths = new double[0];
				
		// Find the SubNodes Information for the Current Node
		String[] subNodeInfo = findNewickNodes(nodeInfo);
		
		// Investigate each of the SubNodes
		for(int index = 0; index < subNodeInfo.length; index++){
			
			// Is it a Terminal Node?
			if(!subNodeInfo[index].substring(0,1).equals("(")){ // Note the ! means unless
				
				// Create the Terminal Node
				String[] parts = subNodeInfo[index].split(":");
				Node terminalNode = new Node(new Node[0], new double[0], parts[0]);
				
				// Store the Terminal Nodes Information
				subNodes = NodeMethods.append(subNodes, terminalNode);
				branchLengths = ArrayMethods.append(branchLengths, Double.parseDouble(parts[1]));
							
			// It is an Internal Node
			}else{
				Node internalNode = examineNewickNode(subNodeInfo[index]);
				
				// Store the Internal Node Information
				String[] parts = subNodeInfo[index].split(":");
				subNodes = NodeMethods.append(subNodes, internalNode);
				branchLengths = ArrayMethods.append(branchLengths, Double.parseDouble(parts[parts.length - 1]));
			}
		}
		
		// Store the Information for the Current Node as a Node Object
		String nodeName = "";
		for(int index = 0; index < subNodes.length; index++){
			nodeName = nodeName + subNodes[index].getNodeName();
		}
		Node node = new Node(subNodes, branchLengths, nodeName);
		
		return node;
	}
	
	public static String[] findNewickNodes(String node){
		/**
		 * Here we want to examine the Current Node (level in Newick Tree) and split the String such that
		 * each Node within the current Node is represented as its String:
		 * 
		 * 	(A:1,B:2,(C:3,D:4,(E:2, F:7):2):5); ---> A:1	B:2	(C:0.3,D:0.4,(E:0.2, F:0.7):0.2):0.5
		 * 
		 * Has to be able to dealing the TERMINAL and INTERNAL nodes. In this case in order to stay in the correct
		 * level the opening and closing brackets must be matched - so that for the INTERNAL nodes (which contain 
		 * other nodes) the information is retained.
		 */
		
		// Trim the Node Info String to Remove the containing Brackets and Redundant Distance Information if Present
		node = trimNodeInfo(node);
		char[] chars = node.toCharArray();
		
		// Storing each of the individual node information Strings in Array
		String[] nodes = {};
		
		// Variables to extract the information relevant to the Current Node being investigated
		int previousNodeEnd = 0;
		int terminalNode = 1;
		
		// Variables used to match Brackets for Internal Nodes
		int openingBracket = 0;
		int closingBracket = 0;

		for(int index = 0; index < chars.length; index++ ){
			if(chars[index] == ',' && terminalNode == 1){
				
				// Dealing with TERMINAL nodes
				nodes = ArrayMethods.append(nodes, node.substring(previousNodeEnd, index));
				previousNodeEnd = index + 1;
				
			}else if(chars[index] == '('){
				
				// Found a INTERNAL Node
				if( terminalNode == 1){
					terminalNode = 0;
					openingBracket++;
				// Found a INTERNAL SUBNODE - record the opening Bracket
				}else if(terminalNode == 0){ 
					openingBracket++;
				}
				
			}else if(chars[index] == ')' && terminalNode == 0){
				
				// Found a closing Bracket for an INTERNAL SUBNODE - record the closing Bracket
				closingBracket++;
								
			}else if(terminalNode == 0 && chars[index] == ',' && closingBracket == openingBracket){
				
				// Dealing with an INTERNAL node - which contain nodes
				nodes = ArrayMethods.append(nodes, node.substring(previousNodeEnd, index));
				previousNodeEnd = index + 1;
					
				// Reset the Terminal Node Info
				openingBracket = 0;
				closingBracket = 0;
				terminalNode = 1;
				
			}
			
		}
		
		// Add the Last Node's Information
		nodes = ArrayMethods.append(nodes, node.substring(previousNodeEnd, node.length()));
		
		return nodes;
	}
	
    public static String returnNewickString(String file) throws IOException{
    	InputStream input = new FileInputStream(file);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		/**
		 *  Read in Newick Tree file
		 *  Lines can be on multiple Lines. This returns whole tree as a single string
		 *  (A:1,B:2,(C:3,D:4):5);
		 */
		
		String tree = "";
		String line = null;
	    while(( line = reader.readLine()) != null){
	    	tree = tree + line;
	    }
	    
	    return tree;
    }
	
    public static String trimNodeInfo(String node){
    	
    	int lastBracketIndex = 0;
    	char[] chars = node.toCharArray();
    	for(int index = chars.length - 1; index > 0; index--){
    		
    		// Find the Position of the Last closing Bracket
    		if(chars[index] == ')'){
    			lastBracketIndex = index;
    			break;
    		}
    	}

    	return node.substring(1, lastBracketIndex);
    }

    /**
     *  Methods for Building Neighbour Joining Tree from Fasta - BROKEN!!!!!!!
     *  
     *  String file = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/allRawReads/vcfFiles/perlDistanceMatrix.txt";
	 * 
	 *  DistanceMatrix distanceMatrixInfo = DistanceMatrixMethods.readInDistanceMatrix(file);
	 * 
	 *  Node tree = NJTreeMethods.BuildNJTree(distanceMatrixInfo);
     */

    public static void printNode(Node tree, BufferedWriter treeFile) throws IOException{
    	
    	// Extract the Branch Length Information for the current Node
    	double[] branchLengths = tree.getBranchLengths();
    	
    	// Investigate each of the Sub-Nodes for the current Node
    	WriteToFile.write(treeFile, "( ");
    	int index = -1;
    	for(Node node : tree.getSubNodes()){
    		index++;
    		
    		// If the Sub-Node is a Leaf (has no Sub-Nodes)
    		if(node.getSubNodes().length == 0){
    			WriteToFile.write(treeFile, node.getNodeName() + ":" + branchLengths[index]);    			
    			
    			// Add comma if this is not the last of the Sub-Nodes
    			if(index < tree.getSubNodes().length - 1){
    				WriteToFile.write(treeFile, ", ");
    			}
    		
    		// If the Sub-Node is an Internal Node
    		}else{
    			
    			// Investigate its Sub-Nodes
    			// Note the Loop to the SAME Method - Continues to Search all Branches until all have reached their
    			// Terminal Leaves
    			printNode(node, treeFile);
    			
    			// Add the Branch Length to this Internal Node to the Previous Node
    			WriteToFile.write(treeFile, ":" + branchLengths[index]);
    			
    			// Add a comma if investigating the First of the Sub-Nodes
    			if(index < tree.getSubNodes().length - 1){
    				
    				WriteToFile.write(treeFile, ", ");
    			}
    		}
    		
    	}
    	
    	// Finished with Current Node
    	WriteToFile.write(treeFile, " )");
    }
    
	public static Node buildNJTree(DistanceMatrix distanceMatrixInfo){
    	   
    	/**
    	 * The Neighbour Joining Tree Building Algorithm - Saitou and Nei , 1987
    	 * 
    	 * Method which progressively clusters taxa together until all the taxa form an unrooted tree.
    	 * 
    	 * 1. Convert the Distance Matrix into an S Matrix using:
    	 * 		S(i,j) = (N - 2)d(i,j) - R(i) - R(j)
    	 * 
    	 * 		N = No. of Taxa
    	 * 		R(x) = the sum of the row x in the distance matrix
    	 * 
    	 * 2. Find the smallest entry in the S matrix ---> S(x,y)
    	 * 
    	 * 3. Form a new internal node, z, that is parent to x and y and calculate the edge lengths from z to x and
    	 * 	  z to y.
    	 * 	  	d(x,z) = 1 / [ 2(N - 2) * ( (N - 2)d(x,y) + R(x) - R(y) ) ]
    	 * 		d(y,z) = d(x,y) - d(x,z)
    	 * 
    	 * 4. Update the distance matrix
    	 * 		d(w,z) = 1/2[ d(x,w) + d(y,w) - d(x,y) ]
    	 * 
    	 * Repeat the Above steps until only two remain
    	 * 
    	 */
    	
    	// Extract the Distance Matrix Information
    	double[][] d = distanceMatrixInfo.getDistanceMatrix();
    	String[] sampleNames = distanceMatrixInfo.getSampleNames();
    	
    	// Initialise the Sample Nodes
    	
    	Node[] availableNodes = initialiseNodes(sampleNames);
    	
    	// BUILD THE TREE
    	
    	while(d.length > 2){
    	
    		// Step 1 - Generate the S matrix
    		double[][] s = createSMatrix(d);
    	
    		// Step 2 - Find the minimum distance in the S matrix
    		int[] coordinates = findMinimumDistance(s);
    		int x = coordinates[0];
    		int y = coordinates[1];
    	
    		// Step 3 - Find branch lengths from z to x and y
    		double[] branchLengths = calculateBranchLengths(d, coordinates);
    	
    		// Step 4 - Update the distance matrix
    		d = updateDistanceMatrix(d, x, y);
    	
    		// Create the Internal Node (z)
    		Node[] subNodes = {availableNodes[x], availableNodes[y]};
    		String nodeName = sampleNames[x] + sampleNames[y];
    	    	
    		Node node = new Node(subNodes, branchLengths, nodeName);
    	
    		// Update the Sample Information to match the distance Matrix
    		sampleNames = updateSampleNames(sampleNames, x, y);
    		availableNodes = updateAvailableNodes(availableNodes, x, y, node);
    		
    	}
    	
    	// Join the Last two Nodes - The Distance between these Nodes is d[i][j]
    	Node[] subNodes = {availableNodes[0], availableNodes[1]};
		String nodeName = sampleNames[0] + sampleNames[1];
		double[] branchLengths = {0, d[0][1]}; // Note only noting the Distance Once

	    	
		Node tree = new Node(subNodes, branchLengths, nodeName);
    	
    	return tree;
    }
    
    public static double[][] createSMatrix(double[][] d){
    	double[][] s = new double[d.length][d.length];
    	
    	/**
    	 * Convert the Distance Matrix into an S Matrix using:
    	 * 	S(i,j) = (N - 2)d(i,j) - R(i) - R(j)
    	 * 		N = No. of Taxa
    	 * 		R(x) = the sum of the row x in the distance matrix
    	 */
    	
    	// Create an Array storing all the Row totals
    	double[] rowSums = calculateRowSums(d);
    	
    	// Fill the empty S matrix
    	for(int i = 0; i < d.length; i++){
    		for(int j = 0; j < d.length; j++){
    			
    			// Skip the Diagonal
    			if(i == j){
    				continue;
    			}
    			
    			s[i][j] = (d.length - 2) * d[i][j] - rowSums[i] - rowSums[j];
    		}
    	}
    	
    	return s;
    }

    public static int[] findMinimumDistance(double[][] matrix){
		int[] coordinates = new int[2];
		
		double minimum = 1000;
		for(int i = 0; i < matrix.length; i++){
			for(int j = 0; j < matrix[0].length; j++){
				
				// Avoid the Diagonal
				if(i == j){
					continue;
				}
				
				if(matrix[i][j] < minimum){
					coordinates[0] = i;
					coordinates[1] = j;
					minimum = matrix[i][j];
				}
			}
		}
		
		return coordinates;
	}

    public static double[] calculateBranchLengths(double[][] d, int[] coordinates){
    	
    	/**
    	 * Calculate the branch length from the internal node, z, to x and y.
    	 * 	  	d(x,z) = d(x,y)/2 + (R(x) - R(y))/2(N-2)
    	 * 		d(y,z) = d(x,y) - d(x,z)
    	 */
    	
    	int x = coordinates[0];
    	int y = coordinates[1];
    	double[] rowSums = calculateRowSums(d);
    	double[] lengths = new double[2];
    	
    	// Calculate the branch length from x to z
    	lengths[0] = (d[x][y] / 2) + ( (rowSums[x] - rowSums[y])/(2*(d.length - 2)) );
    	
    	// Calculate the branch length from y to z
    	lengths[1] = d[x][y] - lengths[0];
    	
    	return lengths;
    }
    
    public static double[] calculateRowSums(double[][] matrix){
    	double[] rowSums = new double[matrix.length];
    	
    	for(int row = 0; row < matrix.length; row++){
    		double total = 0;
    		for(int col = 0; col < matrix.length; col++){
    			total += matrix[row][col];
    		}
    		
    		rowSums[row] = total;
    	}
    	
    	return rowSums;
    }

    public static double[][] updateDistanceMatrix(double[][] d, int x, int y){
    	
    	/**
    	 * Update the distance matrix
    	 * 		d(w,z) = 1/2[ d(x,w) + d(y,w) - d(x,y) ]
    	 */
    	
    	double[][] newD = MatrixMethods.copy(d);
    	
    	// Update the information such that y becomes z
    	
    	for(int w = 0; w < d[y].length; w++){
    		
    		// Update the elements but avoid the x and y nodes
    		double element = 0;
    		if( w != x || w != y){
    			element = 0.5 * (d[x][w] + d[y][w] - d[x][y]);
    		}
    		    		
    		newD[y][w] = element;
    		newD[w][y] = element;
    	}
    	
    	// Remove the x column and row
    	newD = removeNode(newD, x);
    	
    	return newD;
    }
    
    public static double[][] removeNode(double[][] d, int node){
    	
    	double[][] newD = new double[d.length - 1][d.length - 1];
    	
    	int row = -1;
    	for(int i = 0; i < d.length; i++){
    		    		
    		// Skip Node's Row
    		if( i == node){
    			continue;
    		}
    		row++;
    		
    		int col = -1;
    		for(int j = 0; j < d[0].length; j++){
    			
    			// Skip Node's Column
    			if(j == node){
    				continue;
    			}
    			col++;
    			
    			newD[row][col] = d[i][j];
    		}
    	}
    	
    	return newD;
    }

    public static String[] updateSampleNames(String[] sampleNames, int x, int y){
    	
    	
    	String[] newNodes = ArrayMethods.copy(sampleNames);
    	newNodes[y] = sampleNames[x] + sampleNames[y];
    	newNodes = ArrayMethods.deletePosition(newNodes, x);
    	
    	return newNodes;
    	
    }
    
    public static Node[] updateAvailableNodes(Node[] availableNodes, int x, int y, Node node){
    	
    	
    	Node[] newAvailableNodes = NodeMethods.copy(availableNodes);
    	newAvailableNodes[y] = node;
    	newAvailableNodes = NodeMethods.deletePosition(newAvailableNodes, x);
    	
    	return newAvailableNodes;
    	
    }

    public static Node[] initialiseNodes(String[] sampleNames){
    	Node[] availableNodes = new Node[sampleNames.length];
    	Node[] subNodes = new Node[0];
    	double[] lengths = new double[0];
    	
    	for(int index = 0; index < sampleNames.length; index++){
    		availableNodes[index] = new Node(subNodes, lengths, sampleNames[index]);
    	}
    	
    	return availableNodes;
    }

    public static void printNode(Node tree){
    	
    	// Extract the Branch Length Information for the current Node
    	double[] branchLengths = tree.getBranchLengths();
    	
    	// Investigate each of the Sub-Nodes for the current Node
	    System.out.print("( ");
    	
    	int index = -1;
    	for(Node node : tree.getSubNodes()){
    		index++;
    		
    		// If the Sub-Node is a Leaf (has no Sub-Nodes)
    		if(node.getSubNodes().length == 0){
    			System.out.print(node.getNodeName() + ":" + branchLengths[index]);    			
    			
    			// Add comma if looking at the first of the Sub-Nodes
    			if(index < tree.getSubNodes().length - 1){
    				System.out.print(", ");
    			}
    		
    		// If the Sub-Node is an Internal Node
    		}else{
    			
    			// Investigate its Sub-Nodes
    			// Note the Loop to the SAME Method - Continues to Search all Branches until all have reached their
    			// Terminal Leaves
    			printNode(node);
    			
    			// Add the Branch Length to this Internal Node to the Previous Node
    			System.out.print(":" + branchLengths[index]);
    			
    			// Add a comma if investigating the First of the Sub-Nodes
    			if(index < tree.getSubNodes().length - 1){
    				
    				System.out.print(", ");
    			}
    		}
    	}
    	
    	//
    	
    	// Finished with Current Node
    	System.out.print(" )");
    }
    
}
