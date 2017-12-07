package phylogeneticTree;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Hashtable;

import methods.CalendarMethods;
import methods.HashtableMethods;
import methods.WriteToFile;

public class CalculateTipDistancesToRoot {

	public static void main(String[] args) throws IOException{
		
		// Note the current date
		String date = CalendarMethods.getCurrentDate("dd-MM-yyyy");
		
		// Set the directory path
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/NewAnalyses_02-06-16/";
		
		// Read in the Newick tree
		String newickFile = path + "allVCFs-IncludingPoor/vcfFiles/" + 
		"mlTree_Prox-10_plusRef_rmResequenced_SNPCov-0.1_28-10-16.tree";
		String newickTree = CalculateDistancesToMRCAs.readNewickFile(newickFile);
		
		// Convert the Newick Tree into a Java traversable Node
		Node tree = BeastNewickTreeMethods.readNewickNode(newickTree, new Node(null, null, null));
				
		// For each node in the phylogenetic tree note the path to the root
		CalculateDistancesToMRCAs.notePathToRootForAllNodes(tree, new Node[0]);
		Node[] terminalNodes = Global.terminalNodes;
		
		// Calculate the patristic length of the path to root for each tip
		Hashtable<String, Double> tipDistancesToRoot = calculateDistancesOfTipsToRoot(terminalNodes);
		
		// Print out an output table
		String outputFile = path + "allVCFs-IncludingPoor/vcfFiles/" + 
		"DistancesToRoot_mlTree_28-10-16.txt";
		printDistancesToRoot(outputFile, tipDistancesToRoot);
		
	}
	
	// Functions
	public static void printDistancesToRoot(String fileName, Hashtable<String, Double> distancesToRoot) throws IOException{
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Add a file header
		WriteToFile.writeLn(bWriter, "IsolateID\tPatristicDistanceToRoot");
		
		// Print each of the distances
		for(String tipName : HashtableMethods.getKeysString(distancesToRoot)){
			WriteToFile.writeLn(bWriter, tipName + "\t" + distancesToRoot.get(tipName));
		}
		
		// Close the output file
		WriteToFile.close(bWriter);
	}
	
	public static Hashtable<String, Double> calculateDistancesOfTipsToRoot(Node[] terminalNodes){
		
		// Initialise a Hashtable to record each distance to root
		Hashtable<String, Double> tipDistancesToRoot = new Hashtable<String, Double>();
		
		// Initialise variables to store each tips information
		Node[] pathToRoot;
		double pathLength;
		
		// Examine each tip on the phylogenetic tree
		for(Node terminalNode : terminalNodes){
			
			// Get a path to root for the current node
			pathToRoot = terminalNode.getPathToRoot();
			
			// Begin summing the branch lengths from the tip to root - branch lengths reported back to bodes from tips
			pathLength = terminalNode.getNodeInfo().getBranchLength();
			
			// Calculate its length
			for(Node nodeOnPath : pathToRoot){
				
				// Skip the root - branch length = -99 by default. Has no branch length as they are noted backwards
				if(nodeOnPath.getParentNode().getNodeInfo() == null){
					continue;
				}
				
				pathLength += nodeOnPath.getNodeInfo().getBranchLength();
			}
			
			// Store the path length
			tipDistancesToRoot.put(terminalNode.getNodeInfo().getNodeId(), pathLength);			
		}
		
		return tipDistancesToRoot;
	}
}
