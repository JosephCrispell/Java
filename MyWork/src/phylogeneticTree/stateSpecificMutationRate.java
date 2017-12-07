package phylogeneticTree;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.Thread.State;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.HashtableMethods;
import methods.WriteToFile;

public class stateSpecificMutationRate {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		
		// Read in the Posterior Trees
		String mccFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/BuildingTree/runSets/runs_13-02-15/run_13-02-15_2_HKY_Exponential_Skyride_LatLongs_Cauchy_Cattle_Removed/NZ_13-02-15_2_HKY_Exponential_Skyride_LatLongs_Cauchy_Cattle_Removed.trees.txt";
		BeastNewick posteriorTrees = BeastNewickTreeMethods.readBeastFormattedNexus(mccFile, 10001);
		
		// Read in the State Transitions Rate file
		String ratesFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/BuildingTree/runSets/runs_13-02-15/run_13-02-15_2_HKY_Exponential_Skyride_LatLongs_Cauchy_Cattle_Removed/NZ_13-02-15_2_HKY_Exponential_Skyride_LatLongs_Cauchy_Cattle_Removed.Cattle.rates.log";
		Hashtable<Integer, double[]> treeRates = readStateTransitionsRateFile(ratesFile);

		// Open an output File
		String outputFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/BuildingTree/runSets/runs_13-02-15/run_13-02-15_2_HKY_Exponential_Skyride_LatLongs_Cauchy_Cattle_Removed/NZ_13-02-15_2_stateSpecificMutationRateOutput.txt";
		BufferedWriter bWriter = WriteToFile.openFile(outputFile, false);
		
		// Record the number of possible states	
		String[] possibleStates = {"BOVINE", "WILDLIFE"};
		Hashtable<String, Integer> stateIndices = indexStates(possibleStates);
				
		// Print Fields into output file
		WriteToFile.writeLn(bWriter, createFields(possibleStates.length, HashtableMethods.getKeysString(stateIndices)));
		
		// Examine each tree in the Posterior distribution
		int[] treeStateNos = posteriorTrees.getTreeStates();
		String[] nwkTrees = posteriorTrees.getNewickTrees();
		int treeNo;
		Node tree;
		for(int i = 0; i < posteriorTrees.getNoTrees(); i++){
			
			// Get the State No. for current Tree
			treeNo = treeStateNos[i];
			System.out.println("Reading Tree No. " + i + "\t(" + treeNo + ")");
			
			// Create a Java traversible tree
			tree = BeastNewickTreeMethods.readNewickNode(nwkTrees[i], new Node(null, null, null));
			
			// Get the State Transitions rates for the current Tree - convert them to probabilities
			double[] trasitionProbs = convertRatesToProbabilities(treeRates.get(treeNo));
			
			// Investigate State specific mutations rates on each branch
			StateSpecificMutationRateResults.stateRates = new double[possibleStates.length][0];
			StateSpecificMutationRateResults.noStateBranches = new int[possibleStates.length];
			
			// Examine the current Tree
			examinePosteriorTreeNode(tree, "Cattle", possibleStates, "ClockModel", stateIndices);
			
			// Summarise the State specific Mutation Rate distributions
			String output = Integer.toString(treeNo) + "\t";
			output = output + summarizeStateSpecificMutationRateDistributions(StateSpecificMutationRateResults.stateRates);
			WriteToFile.writeLn(bWriter, output);
			
			// Reset the information stored - ready for the next tree
			StateSpecificMutationRateResults.restoreDefaults();			
		}
		
		// Close the Output File
		WriteToFile.close(bWriter);
	}
	
	public static String createFields(int noStates, String[] states){
		
		String output = "State";
		
		for(int i = 0; i < noStates; i++){
			output = output + "\t" + states[i] + "Mean" + i + "\tNoBranches" + i + "\tUpper" + i + "\tLower" + i;
		}
		
		output = output + "\tNoStateSwitches";
		
		return output;
	}
	
	public static String summarizeStateSpecificMutationRateDistributions(double[][] stateRates){
		
		String output = "";
		double[] bounds = new double[2];
		
		for(int i = 0; i < stateRates.length; i++){
			
			output = output + ArrayMethods.mean(stateRates[i]) + "\t" + StateSpecificMutationRateResults.noStateBranches[i] + "\t";
			
			bounds = ArrayMethods.getBounds(stateRates[i], 0.95);
			
			output = output + bounds[0] + "\t" + bounds[1];
			
			if(i < stateRates.length - 1){
				output = output + "\t";
			}
		}
		
		output = output + "\t" + StateSpecificMutationRateResults.noStateSwitches;
		
		return output;
	}
	
	public static Hashtable<String, Integer> indexStates(String[] states){
		
		Hashtable<String, Integer> indices = new Hashtable<String, Integer>();
		
		for(int i = 0; i < states.length; i++){
			indices.put(states[i], i);
		}
		
		return indices;		
	}
	
	public static double[][] examineBranchspecificMutationRate(double[][] stateRates, String parentState,
			String subNodeState, double mutationRate, Hashtable<String, Integer> stateIndices){
		
		/**
		 * Given that each node in a BEAST posterior tree has a state label it should be possible to draw out
		 * the state-specific mutation rates from the branch specific mutation rate.
		 * 			branchLength				Transition Rates
		 * 		 -----------------W					W	C
		 * 		|		M1						W		Twc
		 * 	---- W								C	Tcw
		 * 		|
		 * 		 ------------C					Convert rates to probabilities
		 * 			   M2
		 * 	
		 * 		M1 equates to Mw - the mutation rate of the pathogen in state W
		 * 
		 * 		Tw = time spent in state W
		 * 		Tw = (1 - Pwc) * branchLength
		 * 
		 * 		 M2
		 * 	------------  =  Mw * Tw  +  Mc * Tc 
		 *  branchLength	
		 *  
		 *  
		 *  Note that in the first instance just interested in where the state doesn't change between the parent and subNode.
		 *  This equates to M1 situations.				
		 */
		
		if(parentState.equals(subNodeState)){
			stateRates[stateIndices.get(parentState)] = ArrayMethods.append(stateRates[stateIndices.get(parentState)], mutationRate);
			
			StateSpecificMutationRateResults.noStateBranches[stateIndices.get(parentState)]++;
		}else{
			StateSpecificMutationRateResults.noStateSwitches++;
		}
		
		return stateRates;
	}
	
	
	public static double[] convertRatesToProbabilities(double[] rates){
		
		double scalar = 1 / ArrayMethods.sum(rates);
		
		double[] probabilities = new double[rates.length];
		
		for(int i = 0; i < rates.length; i++){
			probabilities[i] = rates[i] * scalar;
		}
		
		return probabilities;
	}
	
	public static void examinePosteriorTreeNode(Node node, String stateLabel, String[] possibleStates,
			String rateLabel, Hashtable<String, Integer> stateIndices){
		
		// Get the Location and Time information for the current node
		NodeInfo parentNodeInfo = node.getNodeInfo();
		
		// Get the Parent Node state
		String parentState = getNodeState(parentNodeInfo, stateLabel, possibleStates);
		
		// Initialise Location and Time variables for subNodes
		NodeInfo subNodeInfo;
		double branchLength; // Branch Length is the distance in evolutionary time from subNode back to its parent Node
		double[] subNodeLatLongs = new double[2];
		String subNodeState;
		double mutationRate;
		
		// Examine each of the Sub Nodes for the current node
		for(Node subNode : node.getSubNodes()){
			
			// Get the Sub Node Info
			subNodeInfo = subNode.getNodeInfo();
			branchLength = subNodeInfo.getBranchLength();
			mutationRate = getMutationRate(subNodeInfo, rateLabel);
						
			// Get the Sub Node State
			subNodeState = getNodeState(subNodeInfo, stateLabel, possibleStates);
			
			// Examine Branch Specific Mutation Rates
			StateSpecificMutationRateResults.stateRates = examineBranchspecificMutationRate(StateSpecificMutationRateResults.stateRates, parentState, subNodeState, mutationRate, stateIndices);
				
			// If current Sub Node is an internal then need to examine it's subNodes
			if(subNode.getSubNodes().length != 0){
				examinePosteriorTreeNode(subNode, stateLabel, possibleStates, rateLabel, stateIndices);
			}
		}
	}
	
	public static double getMutationRate(NodeInfo info, String rateLabel){
		
		double rate = -99;
		
		// Check Node Info for Mutation Rate
		if(info.getNodeInfo().get(rateLabel + ".rate") != null){
			rate = info.getNodeInfo().get(rateLabel + ".rate")[0];
		
		// Check Branch Info for Mutation Rate
		}else if(info.getBranchInfo().get(rateLabel + ".rate") != null){
			rate = info.getBranchInfo().get(rateLabel + ".rate")[0];
		}
		
		return rate;
	}
	
	public static String getNodeState(NodeInfo info, String stateLabel, String[] possibleStates){
		
		String state = "NOTFOUND";
		
		/**
		 *  String Node variables are recorded in the variable key:
		 *  	Cattle="BOVINE" has a key: Cattle--BOVINE
		 */
		
		
		for(String label : possibleStates){
			
			// Check the node Information for the state label
			if(info.getNodeInfo().get(stateLabel + "--" + label) != null){
				state = label;
				break;
			
			// Check the branch Information for the state label
			}else if(info.getBranchInfo().get(stateLabel + "--" + label) != null){
				state = label;
				break;
			}			
		}
		
		return state;		
	}
	
	public static Hashtable<Integer, double[]> readStateTransitionsRateFile(String fileName) throws NumberFormatException, IOException{
		
		/**
		 * State Transition Rate File Structure:
		 * 
		 * 		# BEAST v1.8.1, r6542
		 *		# Generated Fri Feb 13 16:16:28 GMT 2015 [seed=1423844159076]
		 *		state	Cattle.rates1	Cattle.rates2
		 *		0		1.0		1.0
		 *		100000	0.20107173922031743	1.5163331793582007
		 */
		
		// Open the input File
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
						
		// Initialise Hashtable to record sample names and their associated Integers
		Hashtable<Integer, double[]> treeRates = new Hashtable<Integer, double[]>();
		
		// Begin reading the Input file
		String line = null;
		String[] parts = new String[3];
		double[] values;
		while(( line = reader.readLine()) != null){
			
			// Skip the Header Section
			if(line.matches("#(.*)") == true || line.matches("state(.*)") == true){
				continue;
			}
			
			// Store the State Transitions for the current Posterior Tree
			parts = line.split("\t");
			
			// Convert the String values to double
			values = new double[parts.length - 1];
			for(int i = 1; i < parts.length;i++){
				values[i-1] = Double.parseDouble(parts[i]);
			}
			
			treeRates.put(Integer.parseInt(parts[0]), values);			
		}
		
		return treeRates;
	}

}
