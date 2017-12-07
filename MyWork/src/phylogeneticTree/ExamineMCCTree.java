package phylogeneticTree;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.HashtableMethods;
import methods.LatLongMethods;
import methods.WriteToFile;

public class ExamineMCCTree {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		// TODO Auto-generated method stub

		String mccFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/AnalysesForPaper/CombinedEastAndWest/run_1_28-10-15_HKY_Relaxed-Exponential_Skygrid/NZ_1_28-10-15_GTR_Relaxed-Exponential_Skygrid_MCC.tree";
		String outFile = "C:/Users/Joseph Crisp/Desktop/test.txt";
		String latLongsLabel = "none";
		//String clockRateLabel = "ClockModel.rate";
		String clockRateLabel = "meanRate";
		//String stateLabel = "CATTLE";
		String stateLabel = "none";
		String[] stateOptions = {"BOVINE", "NONBOVINE"};
		
		//String mccFile = args[0];
		
		if(mccFile.equals("-help")){
			
			System.out.println("Java tool to examine the Maximum Clade Credibility tree produced in phylogenetic analyses in BEAST.");
			System.out.println("Command line structure:");
			System.out.println("\tjava -jar ExamineMCCTree.jar nexusMCC.tree outputTable.txt latLongsLabel clockRateLabel stateLabel stateOptions");
			System.out.println("\n\tlatLongsLabel\tA label for when latitude and longitude data were included in the BEAST analysis. Set to \"none\" if not applicable.");
			System.out.println("\tclockRateLabel\tThe label that the clock model substitution rate estimations will be stored under.");
			System.out.println("\tstateLabel\tThe label that the discrete state estimations will be identified with. Set to \"none\" if not applicable.");
			System.out.println("\tstateOptions\tThe different discrete states included in the discrete state analysis in BEAST.");
			
		}else{
		
			//String outFile = args[1];
			//String latLongsLabel = args[2];
			//String clockRateLabel = args[3];
			//String stateLabel = args[4];
			//String[] stateOptions = args[5].split(",");
			
			// Read in the MCC newick tree
			BeastNewick newickTreeInfo = BeastNewickTreeMethods.readBeastFormattedNexus(mccFile, 1);
			Hashtable<Integer, String> sampleNames = newickTreeInfo.getSampleNames();		
		
			// Convert the Newick Tree into a Java traversable Node
			Node tree = BeastNewickTreeMethods.readNewickNode(newickTreeInfo.getNewickTrees()[0], new Node(null, null, null));
		
			// Open an outputFile to write to
			BufferedWriter bWriter = WriteToFile.openFile(outFile, false);
			WriteToFile.writeLn(bWriter, "NodeNo.\tNodeId\tBranchLength\tNodeHeight\tClockRate\tLatLongsRate\tDiffusionRate\tCattle");
		
			// Traverse the MCC tree and record the Node/Branch Information
			examineMccNode(tree, latLongsLabel, 'K', bWriter, sampleNames, clockRateLabel, stateLabel, stateOptions);
		
			// Calculate the Mean Spatial Diffusion Metrics for tree
			calculateDiffusionMetricsForTree();
		
			WriteToFile.close(bWriter);
		}
	}
	
	public static void calculateDiffusionMetricsForTree(){
					
		/**
		 *  Initialise an array to store the summary statistics
		 *  	Mean Diffusion Rate
		 *  	Mean Diffusion Rate Coefficients
		 *  	Weight Mean Average Diffusion Rate
		 *  	Total Diffusion Rate Coefficients
		 *  	Mean Clock Rate
		 *  	Mean LatLongs Rate
		 */
			
		// Summarise the Spatial Diffusion Distribution for the current Tree
		System.out.println("Mean Spatial Diffusion Rate: " + ArrayMethods.mean(NodeCalculationResults.rateEstimates));
		System.out.println("Mean Spatial Diffusion Coefficient: " + ArrayMethods.mean(NodeCalculationResults.diffusionCoefficients));
		System.out.println("Weighted Average Diffusion Rate: " + NodeCalculationResults.totalDistance / NodeCalculationResults.totalTime);
		System.out.println("Total Spatial Diffusion Coefficient: " + NodeCalculationResults.totalDiffusionCoefficient);
			
	}
	
	public static void examineMccNode(Node node, String latLongsLabel, char unit, BufferedWriter bWriter, 
			Hashtable<Integer, String> sampleNames, String clockRateLabel, String stateLabel,
			String[] stateOptions) throws IOException{
		
		// Get the Location and Time information for the current node
		NodeInfo nodeInfo = node.getNodeInfo();
		double[] parentLatLongs = new double[2];
		if(latLongsLabel.equals("LatLongs")){
			parentLatLongs = BeastNewickTreeMethods.getLocationInformation(nodeInfo, latLongsLabel);
		}
		
		// Initialise Location and Time variables for subNodes
		NodeInfo subNodeInfo;
		double[] subNodeLatLongs = new double[2];
		
		// Initialise variables for the metrics of interest
		Hashtable<String, double[]> subNodeVariables;	
		double[] metrics = new double[5]; // Branch Length, Height, Clock Rate, LatLongs Rate, DiffusionRate
		String line;
						
		// Examine each of the Sub Nodes for the current node
		for(Node subNode : node.getSubNodes()){
			Global.nodeNo++;
			
			// Get the Sub Node Information
			subNodeInfo = subNode.getNodeInfo();
			metrics[0] = subNodeInfo.getBranchLength();
			subNodeVariables = subNodeInfo.getNodeInfo();
						
			// Get the node info metrics
			System.out.println(ArrayMethods.toString(HashtableMethods.getKeysString(subNodeVariables), ", "));
			
			metrics[1] = subNodeVariables.get("height")[0];
			metrics[2] = subNodeVariables.get(clockRateLabel)[0];
			metrics[3] = 0;
			if(latLongsLabel.equals("LatLongs")){
				metrics[3] = subNodeVariables.get(latLongsLabel + ".rate")[0];
			}
								
			// Calculate the Spatial Diffusion Rate Estimate
			if(latLongsLabel.equals("LatLongs")){
				subNodeLatLongs = BeastNewickTreeMethods.getLocationInformation(subNodeInfo, latLongsLabel);
			}			
			metrics[4] = calculateDiffusionRate(parentLatLongs, subNodeLatLongs, metrics[0], unit);
			
			// Get the sample name for the current node if present
			String nodeId = subNodeInfo.getNodeId();
			if(nodeId.matches("NA") == false){
				nodeId = sampleNames.get(Integer.parseInt(nodeId)).split("_")[0];
			}
			
			// Print the metrics collected out to file for the current node
			line = ArrayMethods.toString(metrics, "\t");
			
			// ADD IN WHETHER OR NOT THIS NODE WAS CATTLE
			String result = "NA";
			if(stateLabel.equals("none") == false){
				result = getVariableState4String(stateLabel, stateOptions, subNodeVariables);
			}
			
			line = Global.nodeNo + "\t" + nodeId + "\t" + line + "\t" + result;
			WriteToFile.writeLn(bWriter, line);
			
			// If current Sub Node is an internal then need to examine it's subNodes
			if(subNode.getSubNodes().length != 0){
				examineMccNode(subNode, latLongsLabel, unit, bWriter, sampleNames, clockRateLabel, stateLabel, stateOptions);
			}
		}
	}
	
	public static String getVariableState4String(String variableName, String[] options, Hashtable<String, double[]> nodeVariables){
		
		String result = "";
		
		// Variables with Strings as states are recorded as: species="BOVINE" -> key: species--BOVINE value=1
		for(String option : options){
			if(nodeVariables.get(variableName + "--" + option) != null){
				result = option;
				break;
			}
		}
		
		return result;
	}
	
	public static double calculateDiffusionRate(double[] parentLatLongs, double[] subNodeLatLongs, double branchLength, char unit){
		
		// Calculate the Spatial Distance between the current Sub Node and its parent Node
		double spatialDistance = LatLongMethods.distance(parentLatLongs[0], parentLatLongs[1], subNodeLatLongs[0], subNodeLatLongs[1], unit);
		
		NodeCalculationResults.totalTime += branchLength;
		NodeCalculationResults.totalDistance += spatialDistance;
				
		// Coefficient of Variation for Diffusion Rate
		double diffusionCoefficient = Math.pow(spatialDistance, 2) / (4 * branchLength);
		NodeCalculationResults.diffusionCoefficients = ArrayMethods.append(NodeCalculationResults.diffusionCoefficients, diffusionCoefficient);
		NodeCalculationResults.totalDiffusionCoefficient += diffusionCoefficient * branchLength;
			
		// Branch Specific Spatial Diffusion Estimate
		double spatialDiffusionEstimate = spatialDistance / branchLength;
		NodeCalculationResults.rateEstimates = ArrayMethods.append(NodeCalculationResults.rateEstimates, spatialDiffusionEstimate);
		
		
		return spatialDiffusionEstimate;
	}

}
