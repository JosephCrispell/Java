package phylogeneticTree;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Hashtable;

import methods.WriteToFile;

public class FindClades {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		// TODO Auto-generated method stub
		
		// Read in an MCC tree
		String mccFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/AnalysesForPaper/DecisionTreeModelSelection/DiscreteTrait/run_1_28-08_2015_HKY_Relaxed-Exponential_Skygrid_Cattle-Wildlife/NZ_1_28-08-15_HKY_Rlxd-Exp_Skygrid_Cat-Wild_MCC.tree";
		BeastNewick newickTreeInfo = BeastNewickTreeMethods.readBeastFormattedNexus(mccFile, 1);
		
		// Convert the Newick Tree into a Java traversable Node
		Node tree = BeastNewickTreeMethods.readNewickNode(newickTreeInfo.getNewickTrees()[0], new Node(null, null, null));
		
		// Open an output file
		String outputFile = "C:/Users/Joseph Crisp/Desktop/CladesForIsolates.txt";
		BufferedWriter bWriter = WriteToFile.openFile(outputFile, false);
		
		// Traverse tree and define left and right clades present at each node
		findIsolatesSplitAtNode(tree, newickTreeInfo.getSampleNames(), bWriter);
		
		// Close the output file
		WriteToFile.close(bWriter);
	}

	public static void findIsolatesSplitAtNode(Node node, Hashtable<Integer, String> sampleNames, BufferedWriter bWriter) throws NumberFormatException, IOException{
		
		/**
		 * In a phylogenetic tree a node will have two sub-nodes. Here we are interest in which isolates are
		 * split by a particular node.
		 */
		
		// Initialise an array to store the IDs of any isolates encountered
		String[][] isolatesEncountered = new String[2][0];
		
		// Write a header into the output file
		WriteToFile.writeLn(bWriter, "IsolateId\tClade");
		
		// Find all isolates at tips from branches connected to each of the two sub-nodes to the current node
		int clade = 0;
		for(Node subNode : node.getSubNodes()){
			clade++;

			noteIsolatesFromNode(subNode, sampleNames, bWriter, clade);
		}		
	}
	
	public static void noteIsolatesFromNode(Node node, Hashtable<Integer, String> sampleNames,
			BufferedWriter bWriter, int clade) throws NumberFormatException, IOException{
		
		Node[] subNodes = node.getSubNodes();
		
		if(subNodes.length == 0){
			
			WriteToFile.writeLn(bWriter, sampleNames.get(Integer.parseInt(node.getNodeInfo().getNodeId())) + "\t" + clade);
		}else{
		
			for(Node subNode : node.getSubNodes()){
				noteIsolatesFromNode(subNode, sampleNames, bWriter, clade);
			}
		}		
	}
	
}
