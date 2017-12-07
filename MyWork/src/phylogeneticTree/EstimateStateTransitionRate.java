package phylogeneticTree;

import java.io.IOException;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.HashtableMethods;
import methods.MatrixMethods;

public class EstimateStateTransitionRate {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	
	public static void main(String[] args) throws NumberFormatException, IOException {
		// TODO Auto-generated method stub

		// Read in the MCC newick tree
		//String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/AnalysesForPaper/CombinedEastAndWest/Clade1_SouthIsland/DiscreteTraitAnalysis/Matched_17-02-16_Asymmetric_1/";
		//String mccFile = path + "NZ_1_14-12-15_HKY_Relaxed-Exp_Skygrid_Cattle-Asym_MCC.tree";
		//String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/AnalysesForPaper/CombinedEastAndWest/Clade1_SouthIsland/DiscreteTraitAnalysis/Matched_17-02-16_Symmetric_1/";
		//String mccFile = path + "NZ_1_14-12-15_HKY_Relaxed-Exp_Skygrid_Cattle-Sym_MCC.tree";
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/AnalysesForPaper/CombinedEastAndWest/Clade1_SouthIsland/AllIsolates/run_1_15-12-15_HKY_Relaxed-Exp_Skygrid_Cattle_1/";
		String mccFile = path + "NZ_1_15-12-15_HKY_Relaxed-Exp_Skygrid_Cattle_MCC.tree";
		
		// Note the states
		String[] states = {"BOVINE", "NONBOVINE"};
		
		// Count the number of state transitions
		int[] yearRange = {1980, 2013};
		double posteriorThreshold = 0.6;
		
		// Examine the MCC tree
		examineTransitionsOnMCCTree(mccFile, states, yearRange, posteriorThreshold);
		
	}

	
	public static void examineTransitionsOnMCCTree(String mccFile, String[] states, int[] yearRange,
			double posteriorThreshold) throws NumberFormatException, IOException{
		
		// Read in the MCC newick tree
		BeastNewick newickTreeInfo = BeastNewickTreeMethods.readBeastFormattedNexus(mccFile, 1);
		
		// Convert the Newick Tree into a Java traversable Node
		Node tree = BeastNewickTreeMethods.readNewickNode(newickTreeInfo.getNewickTrees()[0], new Node(null, null, null));
		
		// Index the possible states
		Hashtable<String, Integer> indexedStates = HashtableMethods.indexArray(states);
		Global.counts = new int[states.length][states.length];
		Global.time = new double[states.length][states.length];
		
		// Count the number of state transitions
		examineStateTransitions(tree, states, indexedStates, "CATTLE", yearRange, "height", yearRange[1], "CATTLE.prob", posteriorThreshold);
		MatrixMethods.print(Global.counts);
		
		System.out.println("------------------------------------------\n\n");
		MatrixMethods.print(Global.time, 2);
		
		System.out.println("------------------------------------------\n\n");
		MatrixMethods.print(calculateTransitionRates(Global.counts, Global.time), 2);
	}
	
	public static double[][] calculateTransitionRates(int[][] counts, double[][] sumBranchLengths){
		
		double[][] rates = new double[counts.length][counts[0].length];
		
		for(int i = 0; i < counts.length; i++){
			
			for(int j = 0; j < counts[0].length; j++){
				rates[i][j] = (double) counts[i][j] / sumBranchLengths[i][j];
			}
		}
		
		return rates;
	}
	
	public static void examineStateTransitions(Node node, String[] states, Hashtable<String, Integer> indexedStates,
			String stateLabel, int[] yearRange, String heightLabel, int lastYearSampled,
			String statePosteriorLabel, double posteriorThreshold){
		
		// Get the sub-nodes of the node
		Node[] subNodes = node.getSubNodes();
		
		// Get the index of the current nodes state
		int nodeStateIndex = getNodeStateIndex(node, states, indexedStates, stateLabel);
		int subNodeStateIndex;
		
		// Get the height of the current node
		double nodeHeight = (double) lastYearSampled - getNodeValue(node, heightLabel);
		double subNodeHeight;
		
		// Get the posterior probability of the state associated with the current node
		double nodeStatePosterior = getNodeValue(node, statePosteriorLabel);
		double subNodeStatePosterior;
		
		// Examine the branches to all of the subnodes
		for(Node subNode : subNodes){
			
			// Get the State Index of the current subnode
			subNodeStateIndex = getNodeStateIndex(subNode, states, indexedStates, stateLabel);
			
			// Get the posterior probability of the state associated with the current node
			subNodeStatePosterior = getNodeValue(subNode, statePosteriorLabel);

			// Get the height of the subNode
			subNodeHeight = (double) lastYearSampled - getNodeValue(subNode, heightLabel);
			
			// Determine whether the current branch falls within our window of interest?
			// Is the state of both the node and its subnode well supported in the posterior?
			if(nodeHeight >= yearRange[0] && nodeHeight <= yearRange[1] && subNodeHeight >= yearRange[0] && subNodeHeight <= yearRange[1]
					&& nodeStatePosterior >= posteriorThreshold && subNodeStatePosterior >= posteriorThreshold){
				
				// Record the transition
				Global.counts[nodeStateIndex][subNodeStateIndex]++;
				Global.time[nodeStateIndex][subNodeStateIndex] += subNode.getNodeInfo().getBranchLength();
			}
			
			// Does the current subnode have any subnodes?
			if(subNode.getSubNodes().length > 0){
				examineStateTransitions(subNode, states, indexedStates, stateLabel, yearRange, heightLabel, lastYearSampled, statePosteriorLabel, posteriorThreshold);
			}
			
		}
	}
	
	public static double getNodeValue(Node node, String label){
		
		return node.getNodeInfo().getNodeInfo().get(label)[0];
	}
	
	public static int getNodeStateIndex(Node node, String[] states, Hashtable<String, Integer> indexedStates,
			String label){
		
		// Get the Node Information for the current node
		Hashtable<String, double[]> nodeInfo = node.getNodeInfo().getNodeInfo();
		
		// Determine which state the current node was assigned
		int stateIndex = -1;
		for(String state : states){
			
			// Check whether the node has the current state
			if(nodeInfo.get(label + "--" + state) != null){
				stateIndex = indexedStates.get(state);
				break;
			}			
		}
		
		return stateIndex;		
	}
	
	public static void recordTransitionOnBranch(Node source, Node sink, String label){
		
	}
}
