package phylogeneticTree;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream.GetField;
import java.util.Arrays;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.HashtableMethods;
import methods.LatLongMethods;
import methods.WriteToFile;

public class ExploreBeastTreePosterior {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		// TODO Auto-generated method stub

		// Find the Files
//		String nexusFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/AnalysesForPaper/EastCoast/run_27-07-15_2_HKY_Exponential_Skyline_Cattle/NZ_27-07-15_2_HKY_Exponential_Skyline_Cattle.trees.txt";
//		String outFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/AnalysesForPaper/EastCoast/run_27-07-15_2_HKY_Exponential_Skyline_Cattle/test.txt";
//		
//		double burnIn = 0.1;
//		int nTrees = 10001;
//		String latLongs = "";
		
		String nexusFile = args[0];
		
		if(nexusFile.equals("-help")){
			
			System.out.println("Java tool to examine the posterior distribution of trees resulting\nfrom a phylogenetic analysis in BEAST.");
			System.out.println("Command line structure:");
			System.out.println("\tjava -jar ExaminePosterior.jar nexusFile.trees outputTable.txt burnIn nTrees latLongsLabel clockRateLabel");
			System.out.println("\n\tburnIn \tThe initial proportion of trees to be discarded.");
			System.out.println("\tnTrees \tThe total number of trees in the posterior file. Note that usually 1 more than you think.");
			System.out.println("\tlatLongsLabel\tA label for when latitude and longitude data were included in the BEAST analysis. Set to \"none\" if not applicable.");
			System.out.println("\tclockRateLabel\tThe label that the clock model substitution rate estimations will be stored under.");
		}else{
		
			String outFile = args[1];
		
			double burnIn = Double.parseDouble(args[2]);
			int nTrees = Integer.parseInt(args[3]);
			String latLongs = args[4];
			String clockRateLabel = args[5];
		
			summariseBeastPosterior(nexusFile, outFile, nTrees, 'K', latLongs, clockRateLabel, burnIn);
		}
	}
	
	public static void summariseBeastPosterior(String nexusFile, String outFile, int noTrees, char unit, String latLongsLabel, String clockRateLabel, double burnIn) throws NumberFormatException, IOException{
		
		// Read the Nexus .trees files
		BeastNewick newickTreeInfo = BeastNewickTreeMethods.readBeastFormattedNexus(nexusFile, noTrees);
		
		// Open an Output File and Print Header
		BufferedWriter bWriter = WriteToFile.openFile(outFile, false);
		WriteToFile.writeLn(bWriter, "MeanDiffusionRate\tMeanDiffusionRateCoefficient\tWeightedAverageDiffusionRate\tTotalDiffusionRateCoefficient\tMeanClockRate\tMeanLatLongsRate");
		
		// Initialise variables to store summary statistics
		double[] treeSummary = new double[4];
		String line;
		
		// Extract the Newick Tree Strings
		String[] newickTrees = newickTreeInfo.getNewickTrees();
		String newickTree;
		
		// Take into account the BurnIn
		double start = burnIn * (double) noTrees;
		
		System.out.println("Beginning to Read Posterior Trees...");
		
		// Examine each of the trees in the Posterior (excluding the posterior)
		for(int i = (int) start - 1; i < noTrees; i++){
			
			newickTree = newickTrees[i];
					
			// Read the current Newick Tree
			System.out.println("Reading Tree No. " + i);
			Node tree = BeastNewickTreeMethods.readNewickNode(newickTree, new Node(null, null, null));
			
			// Summarise the Spatial Diffusion Rate Distribution for the Tree
			treeSummary = summarizePosteriorTree(tree, unit, latLongsLabel, clockRateLabel);
			
			// Print the Tree Summary out to File
			line = ArrayMethods.toString(treeSummary, "\t");
			WriteToFile.writeLn(bWriter, line);
			
			// Clear the information for the current tree - prepare for the next
			NodeCalculationResults.restoreDefaults();
		}
		
		WriteToFile.close(bWriter);
	}
	
	public static void examinePosteriorTreeNode(Node node, char unit, String latLongsLabel, String clockRateLabel){
		
		// Get the Location and Time information for the current node
		NodeInfo nodeInfo = node.getNodeInfo();
		double[] parentLatLongs = new double[2];
		
		if(latLongsLabel.equals("LatLongs")){
			BeastNewickTreeMethods.getLocationInformation(nodeInfo, latLongsLabel);
		}
		
		// Initialise Location and Time variables for subNodes
		NodeInfo subNodeInfo;
		double branchLength; // Branch Length is the distance in evolutionary time from subNode back to its parent Node
		double[] subNodeLatLongs = new double[2];
		
		// Examine each of the Sub Nodes for the current node
		for(Node subNode : node.getSubNodes()){
			
			// Get the Location and Time Information for the current sub node
			subNodeInfo = subNode.getNodeInfo();
			branchLength = subNodeInfo.getBranchLength();
			if(latLongsLabel.equals("LatLongs")){
				BeastNewickTreeMethods.getLocationInformation(subNodeInfo, latLongsLabel);
			}
			
			// Add the Diffusion Information into the Diffusion Rate Distribution
			calculateDiffusionMetrics(branchLength, parentLatLongs, subNodeLatLongs, unit);
			
			// Store the additional available information
			getAdditionalBranchMetrics(subNodeInfo.getBranchInfo(), latLongsLabel, clockRateLabel);
				
			// If current Sub Node is an internal then need to examine it's subNodes
			if(subNode.getSubNodes().length != 0){
				examinePosteriorTreeNode(subNode, unit, latLongsLabel, clockRateLabel);
			}
		}
	}
	
	public static void getAdditionalBranchMetrics(Hashtable<String, double[]> branchInfo, String latLongsLabel, String clockRateLabel){
		double clockRate = branchInfo.get(clockRateLabel)[0];
		double latLongRate = 0;
		if(latLongsLabel.equals("LatLongs")){
			latLongRate = branchInfo.get(latLongsLabel + ".rate")[0];
		}
		
		NodeCalculationResults.clockRates = ArrayMethods.append(NodeCalculationResults.clockRates, clockRate);
		NodeCalculationResults.latLongRates = ArrayMethods.append(NodeCalculationResults.latLongRates, latLongRate);
	}
	
	public static void calculateDiffusionMetrics(double branchLength, double[] parentLatLongs, double[] subNodeLatLongs, char unit){
		
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
		
		// Initialise the necessary variables
		double spatialDistance;
		double diffusionCoefficient;
		double spatialDiffusionEstimate;
		
		// Calculate the Spatial Distance between the current Sub Node and its parent Node
		spatialDistance = LatLongMethods.distance(parentLatLongs[0], parentLatLongs[1], subNodeLatLongs[0], subNodeLatLongs[1], unit);
					
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
					
	}
	
	public static double[] summarizePosteriorTree(Node tree, char unit, String latLongsLabel, String clockRateLabel){
		
		/**
		 *  Initialise an array to store the summary statistics
		 *  	Mean Diffusion Rate
		 *  	Mean Diffusion Rate Coefficients
		 *  	Weight Mean Average Diffusion Rate
		 *  	Total Diffusion Rate Coefficients
		 *  	Mean Clock Rate
		 *  	Mean LatLongs Rate
		 */
		
		double[] results = new double[6];
		
		// Get the Spatial Diffusion Rate Distribution
		examinePosteriorTreeNode(tree, unit, latLongsLabel, clockRateLabel);
		
		// Summarise the Spatial Diffusion Distribution for the current Tree
		results[0] = ArrayMethods.mean(NodeCalculationResults.rateEstimates);
		results[1] = ArrayMethods.mean(NodeCalculationResults.diffusionCoefficients);
		results[2] = NodeCalculationResults.totalDistance / NodeCalculationResults.totalTime;
		results[3] = NodeCalculationResults.totalDiffusionCoefficient;
		results[4] = ArrayMethods.mean(NodeCalculationResults.clockRates);
		results[5] = ArrayMethods.mean(NodeCalculationResults.latLongRates);
		
		return results;
	}
}
