package phylogeneticTree;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Random;

import org.uncommons.maths.random.MersenneTwisterRNG;

import methods.ArrayMethods;
import methods.WriteToFile;

public class Clustering {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		/**
		 *  Is there clustering in the leaves of the tree according to their epidemiological characteristics?
		 *  
		 *  	Metrics to describe clustering:
		 *  		Mean within group distance
		 *  		Mean between group distance
		 *  
		 *  		Mean Neighbourhood size (needs some neighbourhood threshold)
		 *  		Mean distance to same type
		 *  
		 *  Distance could just distance in array of leaf labels OR distance through phylogenetic tree
		 *  
		 */

		// Read in labelled BEAST MCC tree
		String mccFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/AnalysesForPaper/DecisionTreeModelSelection/PopulationModel_SteppingStone/run_7_23-09-15_HKY_Relaxed-Exponential_Skygrid/NZ_7_23-09-15_HKY_Relaxed-Exponential_Skygrid_MCC-labeled.tree";
		BeastNewick newickTreeInfo = BeastNewickTreeMethods.readBeastFormattedNexus(mccFile, 1);
				
		// Convert the Newick Tree into a Java traversable Node
		Node tree = BeastNewickTreeMethods.readNewickNode(newickTreeInfo.getNewickTrees()[0], new Node(null, null, null));
		
		// Get the terminal nodes of the Phylogenetic tree
		TerminalNode[] terminalNodes = getTerminalNodes(tree, new TerminalNode[0], newickTreeInfo.getSampleNames(), "_");
		
		// Calculate the Distances (branch length) back to the Internal nodes of each Terminal Node
		calculateDistances2InternalNodes(terminalNodes);
		
		// Generate a Terminal Node Distance Matrix
		double[][] distances = generateTerminalNodeDistanceMatrix(terminalNodes);
		
		/**
		 * Isolate name structure:
		 * 	SampleId_Year_Location_Species_REA_Area
		 * 	0		 1	  2		   3       4   5
		 */
		
		// Initialise a Random Number Generator
		Random random = new MersenneTwisterRNG();
		
		// Null distribution size
		int times = 10000;
		
		// Output file path
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/AnalysesForPaper/DecisionTreeModelSelection/PopulationModel_SteppingStone/run_7_23-09-15_HKY_Relaxed-Exponential_Skygrid/Clustering/";
				
		// ##### AREA #####
		System.out.println("Examining Clustering for AREA...");
		int index = 5;
		String outputFile = path + "NZ_7_23-09-15_Clustering-AREA.txt";
		double actualDifference = calculateDifferenceBetweenMeanIntraAndInterGroupDistances(terminalNodes, distances, index);
		double[] nulldistribution = generateRandomMeanIntraInterGroupDifferenceDistribution(terminalNodes, times, distances, index, outputFile, actualDifference, random);
		
		// ##### LOCATION #####
		System.out.println("Examining Clustering for LOCATION...");
		index = 2;
		outputFile = path + "NZ_7_23-09-15_Clustering-LOCATION.txt";
		actualDifference = calculateDifferenceBetweenMeanIntraAndInterGroupDistances(terminalNodes, distances, index);
		nulldistribution = generateRandomMeanIntraInterGroupDifferenceDistribution(terminalNodes, times, distances, index, outputFile, actualDifference, random);
		
		// ##### SPECIES #####
		System.out.println("Examining Clustering for SPECIES...");
		index = 3;
		outputFile = path + "NZ_7_23-09-15_Clustering-SPECIES.txt";
		actualDifference = calculateDifferenceBetweenMeanIntraAndInterGroupDistances(terminalNodes, distances, index);
		nulldistribution = generateRandomMeanIntraInterGroupDifferenceDistribution(terminalNodes, times, distances, index, outputFile, actualDifference, random);
		
		// ##### REA #####
		System.out.println("Examining Clustering for REA...");
		index = 4;
		outputFile = path + "NZ_7_23-09-15_Clustering-REA.txt";
		actualDifference = calculateDifferenceBetweenMeanIntraAndInterGroupDistances(terminalNodes, distances, index);
		nulldistribution = generateRandomMeanIntraInterGroupDifferenceDistribution(terminalNodes, times, distances, index, outputFile, actualDifference, random);
		
	}
	
	public static void examineTemporalVsPhylogeneticDistance(TerminalNode[] terminalNodes, String outFile, int yearIndex) throws IOException{
		
		// Open the output file and pritn a header
		BufferedWriter bWriter = WriteToFile.openFile(outFile, false);
		WriteToFile.writeLn(bWriter, "Actual\tPhylogenetic");
		
		// Initialise
		int temporalDistance;
		double phylogeneticDistance;
		
		
		int yearI;
		int yearJ;
				
		for(int i = 0; i < terminalNodes.length; i++){
			
			yearI = Integer.parseInt(terminalNodes[i].getLabel()[yearIndex]);
			for(int j = 0; j < terminalNodes.length; j++){
				
				if(i >= j){
					continue;
				}
				
				yearJ = Integer.parseInt(terminalNodes[j].getLabel()[yearIndex]);
				
				// Calculate temporal distance
				temporalDistance = Math.abs(yearI - yearJ);
				
				// Calculate the phylogenetic distance
				phylogeneticDistance = findDistance2MRCA(terminalNodes[i], terminalNodes[j]);
				
				// Print to File
				WriteToFile.writeLn(bWriter, temporalDistance + "\t" + phylogeneticDistance);
			}
		}
		
		WriteToFile.close(bWriter);
	}
	
	public static Hashtable<String, Integer> indexGroups(String[] groups){
		
		Hashtable<String, Integer> groupIndices = new Hashtable<String, Integer>();
		
		for(int i = 0; i < groups.length; i++){
			groupIndices.put(groups[i], i);
		}
		
		return groupIndices;
	}
	
	public static String[][] getGroups4EachLabelInSampleName(TerminalNode[] terminalNodes, String sep){
		
		/**
		 * Method to get a list of the groupings defined by each isolate's epidemiological information
		 * 	isolateA_SpeciesA_LocationA_1991
		 *  isolateB_SpeciesA_LocationB_1991
		 *  isolateC_SpeciesB_LocationB_1991
		 *  isolateD_SpeciesB_LocationC_1993
		 *  
		 *  	groups:
		 *  		Species: A, B
		 *  		Location: A, B, C
		 *  		Year: 1991, 1993
		 */
		
		// Get the number of different labels and Initialise a matrix to store the group info for each
		int noLabels = terminalNodes[0].getLabel().length;
		String[][] isolateLabelGroups = new String[noLabels][terminalNodes.length];
		
		// Examine each of the Terminal nodes and their epidemiological information
		String[] label; 
		for(int i = 0; i < terminalNodes.length; i++){
			
			label = terminalNodes[i].getLabel();
			
			for(int labelIndex = 0; labelIndex < noLabels; labelIndex++){
				
				isolateLabelGroups[labelIndex][i] = label[labelIndex];
			}	
		}
		
		// Find the unique groups for each label
		for(int labelIndex = 0; labelIndex < noLabels; labelIndex++){
			
			isolateLabelGroups[labelIndex] = ArrayMethods.unique(isolateLabelGroups[labelIndex]);
		}
		
		return isolateLabelGroups;
	}
	
	public static double[] generateRandomMeanIntraInterGroupDifferenceDistribution(TerminalNode[] terminalNodes,
			int times, double[][] distances, int index, String outFile, double actualDifference,
			Random random) throws IOException{
		
		// Open an output file and print the actual calculated Mean inter- vs intra-group difference
		BufferedWriter bWriter = WriteToFile.openFile(outFile, false);
		WriteToFile.writeLn(bWriter, "ActualDiff(" + actualDifference + ")");
		
		// Initialise an array to store the mean inter- vs intra-group distance based upon randomly shuffled tips
		double[] result = new double[times];
		
		// Initialise an array to store the shuffled terminal nodes
		TerminalNode[] shuffled = new TerminalNode[terminalNodes.length];
		
		for(int i = 0; i < times; i++){
			
			// Randomly shuffle the Terminal Nodes
			shuffled = TerminalNodeMethods.randomShuffleAll(terminalNodes, random);
			
			// Calculate the Difference between the Mean Intra and Inter group distances
			result[i] = calculateDifferenceBetweenMeanIntraAndInterGroupDistances(shuffled, distances, index);
			WriteToFile.writeLn(bWriter, result[i]);
			
			if( (i + 1) % 1000 == 0){
				System.out.println("Finished " + (i + 1) + " iterations.");
			}
		}
		
		WriteToFile.close(bWriter);
		
		return result;		
	}
	
	public static double calculateDifferenceBetweenMeanIntraAndInterGroupDistances(TerminalNode[] terminalNodes,
			double[][] distances, int labelIndex){
		
		// Initialise an array to store the mean Intra and Inter Group Distances
		double[] result = new double[2];
		
		// Initialise variables to note the number of within and between distances examined
		int noWithin = 0;
		int noBetween = 0;
		
		// Compare each Terminal Node to all others
		for(int i = 0; i < terminalNodes.length; i++){
			for(int j =0; j < terminalNodes.length; j++){
				
				// Make comparison once and ignore diagonal
				if(i >= j){
					continue;
				}
				
				// Is the comparison inter or intra group?
				if(terminalNodes[i].getLabel()[labelIndex].matches(terminalNodes[j].getLabel()[labelIndex]) == true){ // INTRA group
					
					result[0] += distances[i][j];
					noWithin++;
					
				}else{ // INTER GROUP
					
					result[1] += distances[i][j];
					noBetween++;
				}
			}
		}
		
		// Convert the sums to means
		result[0] = result[0] / (double) noWithin;
		result[1] = result[1] / (double) noBetween;
		
		// Difference is the mean Inter-group distance minus the mean Intra-group distance
		return result[1] - result[0];
	}
	
	public static double[][] generateTerminalNodeDistanceMatrix(TerminalNode[] terminalNodes){
		double[][] distances = new double[terminalNodes.length][terminalNodes.length];
		
		// Initialise a variable to store the calculated patristic distance between two terminal nodes
		double patristicDistance;
		
		// Compare each terminal node to one another
		for(int i = 0; i < terminalNodes.length; i++){
			for(int j =0; j < terminalNodes.length; j++){
				
				// Symmetric matrix, only make comparison once and skip diagonal
				if(i >= j){
					continue;
				}
				
				// Find the distance between the leaf i and leaf j - through the phylogenetic tree (branch length)
				patristicDistance = findDistance2MRCA(terminalNodes[i], terminalNodes[j]);
				
				// Record the distance calculated
				distances[i][j] = patristicDistance;
				distances[j][i] = patristicDistance;				
			}
		}
		
		return distances;
	}
	
	public static double findDistance2MRCA(TerminalNode a, TerminalNode b){
		
		// Get the Internal nodes for both Leaves
		Node[] internalNodesA = a.getInternalNodes();
		Node[] internalNodesB = b.getInternalNodes();
		
		// Get the distances to the Internal nodes for both Leaves
		double[] distancesA = a.getDistances2ToInternalNodes();
		double[] distancesB = b.getDistances2ToInternalNodes();
		
		// Initialise double to store the distance from A to B through the tree
		double distance = -9999999;
		
		// Working from the root forward, find the first different Internal node
		int length = getMinNumberOfInternalNodes(internalNodesA, internalNodesB);
		
		for(int x = 1; x < length + 1; x++){
			
			if(internalNodesA[internalNodesA.length - x] != internalNodesB[internalNodesB.length - x]){
				
				// The distance to the MRCA for the pair is the sum of the distance to the MRCA for each
				distance = distancesA[distancesA.length - (x - 1)];
				distance += distancesB[distancesB.length - (x - 1)];
								
				break;
			}else if(x == length){
				
				// Have reached the end without finding a common ancestor - MRCA is the last entry in the lists
				distance = distancesA[distancesA.length - x];
				distance += distancesB[distancesB.length - x];
			}
		}
		
		return distance;		
	}
	
	public static int getMinNumberOfInternalNodes(Node[] a, Node[] b){
		int length = a.length;
		
		if(a.length > b.length){
			length = b.length;
		}
		
		return length;
	}
	
	public static void calculateDistances2InternalNodes(TerminalNode[] terminalNodes){
		
		for(TerminalNode leaf : terminalNodes){
			
			calculateDistance2InternalNode(leaf.getNode(), leaf);
			
		}
	}
	
	public static void calculateDistance2InternalNode(Node node, TerminalNode leaf){
		
		// Get the Branch Length from the current node to its parent
		double branchLength = node.getNodeInfo().getBranchLength();
		
		// The root has a negative branch length (-99) by default - if found root then finished with terminal node
		if(branchLength > 0){
			
			// Calculate the distance from the parent to the leaf
			double distance2Leaf = branchLength + getLastDistance2InternalNode(leaf.getDistances2ToInternalNodes());
			
			// Append the current parent to leaf's list of internal node with its distance
			leaf.setInternalNodes(NodeMethods.append(leaf.getInternalNodes(), node.getParentNode()));
			leaf.setDistancesToInternalNodes(ArrayMethods.append(leaf.getDistances2ToInternalNodes(), distance2Leaf));
			
			// Look at the next Parent node of the parent if present
			calculateDistance2InternalNode(node.getParentNode(), leaf);
			
		}
		
		
		
	}
	
	public static double getLastDistance2InternalNode(double[] distances2InternalNodes){
		double distance = 0;
		
		if(distances2InternalNodes.length != 0){
			distance = distances2InternalNodes[distances2InternalNodes.length - 1];
		}
		
		return distance;
	}
	
	public static TerminalNode[] getTerminalNodes(Node node, TerminalNode[] leaves,
			Hashtable<Integer, String> sampleNames, String labelSep){
		
		// Get the subNodes for the current Node
		Node[] subNodes = node.getSubNodes();
		
		// Initialise the necessary variables
		int nodeId;
		String[] label;
		
		// Does the current Node have any sub nodes?
		if(subNodes.length == 0){
			
			// Get the Label for the current Terminal Node
			nodeId = Integer.parseInt(node.getNodeInfo().getNodeId());
			label = sampleNames.get(nodeId).split(labelSep);
			
			leaves = TerminalNodeMethods.append(leaves, new TerminalNode(node, label));
		}else{
			
			for(Node subNode : subNodes){
				leaves = getTerminalNodes(subNode, leaves, sampleNames, labelSep);
			}
		}
		
		return leaves;		
	}
	
	public static String[][] getTipLabelsFromFigTreeOutput(String fileName, String samplePrefix, int nCols) throws IOException{
		
		/**
		 * Read a FigTree output
		 * 
		 * Structure:
		 * 		#NEXUS
		 *
		 *		Begin taxa;
		 *			Dimensions ntax=6;
		 *			Taxlabels
		 *				AgR227_WESTCOAST_KARAMERA_CERVINE_21
		 *				AgR247_WESTCOAST_KARAMEA_BOVINE_21
		 *				AgR262_WESTCOAST_KARAMEA_BOVINE_20
		 *				AgR250_WESTCOAST_KARAMEA_BOVINE_20
		 *				AgR270_WESTCOAST_KARAMEA_BOVINE_20
		 *				AgR253_WESTCOAST_KARAMEA_BOVINE_20
		 *				;
		 *		end;
		 *
		 *		begin trees;
		 *			tree TREE1 = [&R] NEWICKSTRING
		 *		end;
		 *
		 *	begin figtree;
		 *		set ...
		 *		set ...
		 *		...
		 *	end;
		 *
		 * Taxa label structure:
		 * 		AgR253		WESTCOAST	KARAMEA		BOVINE	20
		 * 		SampleID	Region		Location	Species	REA
		 * 		0			1			2			3		4
		 */
		
		// Open the input File
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise an Array to store the tip labels
		String[][] labels = new String[0][nCols];
		String[] parts;
		int noTaxa;
		int sampleIndex = -1;
						
		// Begin reading the Input file
		String line = null;
		while(( line = reader.readLine()) != null){
			
			// Only interested in the Tree tip labels - skip all other lines
			if(line.matches("\t" + samplePrefix + "(.*)") == false){
				
				// Get the number of taxa on the tree
				if(line.matches("\tdimensions ntax=(.*)")){
					
					parts = line.split("=");
					noTaxa = Integer.parseInt(parts[1].substring(0, parts[1].length() - 1));
					
					// Create an array of the correct size to store the labels
					labels = new String[noTaxa][nCols];
				}
				
				continue;
			}
			
			// Get the Sample labels
			sampleIndex++;
			parts = line.split("\t");
			labels[sampleIndex] = parts[1].split("_");		
		}
		
		input.close();
		reader.close();
		
		return labels;
				
	}

}
