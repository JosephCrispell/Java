package phylogeneticTree;

import java.io.BufferedWriter;
import java.io.IOException;

import methods.ArrayMethods;
import methods.LatLongMethods;
import methods.WriteToFile;

public class BranchMethods {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		
		String nexusFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/BuildingTree/runSets/runs_03-11-14/run_03-11-14_7_HKY_LogNormal_Skyride_LatLongs_LogNormal/NZ_03-11-14_7_HKY_LogNormal_Skyride_LatLongs_LogNormal_MCC.tree";
		
		BeastNewick nexusInfo = BeastNewickTreeMethods.readBeastFormattedNexus(nexusFile, 1);
		
		Node tree = BeastNewickTreeMethods.readNewickNode(nexusInfo.getNewickTrees()[0], new Node(null, null, null));
		
		String[] variableNames = {"rate", "LatLongs.rate"};
		
		// Open an Output file
		String outFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/BuildingTree/runSets/runs_03-11-14/run_03-11-14_7_HKY_LogNormal_Skyride_LatLongs_LogNormal/branchInfo.txt";
		BufferedWriter bWriter = WriteToFile.openFile(outFile, false);
		
		WriteToFile.writeLn(bWriter, "BranchIndex\tLength\tSpatialDist\tDiffusionRate\tTime\t" + ArrayMethods.toString(variableNames, "\t"));
		
		getBranchInfo(tree, variableNames, "LatLongs", bWriter);
		
		WriteToFile.close(bWriter);
		
	//	System.out.println("Recorded Information for " + branches.length + " branches.");
		
		
	}
	
	public static Branch[] getBranches(Node tree, Branch[] branches){
		
		Branch branch = new Branch(-99, null, null);
		
		Node[] subNodes = tree.getSubNodes();
		
		for(Node subNode : subNodes){
			
			Global.branchIndex++;
			branch = new Branch(Global.branchIndex, tree, subNode);
			
			branches = append(branches, branch);
			
			if(subNode.getSubNodes().length != 0){
				
				branches = getBranches(subNode, branches);
			}
			
		}
		
		return branches;		
	}
	
	public static void getBranchInfo(Node tree, String[] variableNames, String latLongsTag, BufferedWriter bWriter) throws IOException{
		
		// Get the Information for the current Node
		NodeInfo nodeInfo = tree.getNodeInfo();
		NodeInfo subNodeInfo;
		
		// Get the subNodes
		Node[] subNodes = tree.getSubNodes();
		
		// Get Latitude and Longitude Information for the current node
		double[] nodeLatLongs = new double[2];
		nodeLatLongs[0] = nodeInfo.getNodeInfo().get(latLongsTag + 1)[0];
		nodeLatLongs[1] = nodeInfo.getNodeInfo().get(latLongsTag + 2)[0];
		double[] subNodeLatLongs = new double[2];
		
		// Initialise variables to store calculated Information
		double time;
		double spatialDistance;
		double branchLength;
		double diffusionRate;
		
		// Examine the subNodes of the current node - a branch connects the current node to its subnodes
		for(Node subNode : subNodes){
			
			// Get the branch Info - note that each node carries information for its preceding branch - root to tip
			subNodeInfo = subNode.getNodeInfo();
			branchLength = subNodeInfo.getBranchLength();
			
			// Get the Latitude and Longitude Information for the current subnode
			subNodeLatLongs[0] = subNodeInfo.getNodeInfo().get(latLongsTag + 1)[0];
			subNodeLatLongs[1] = subNodeInfo.getNodeInfo().get(latLongsTag + 2)[0];
			
			// Calulate the spatial distance
			spatialDistance = LatLongMethods.distance(nodeLatLongs[0], nodeLatLongs[1], subNodeLatLongs[0], subNodeLatLongs[1], 'K');
			
			// Calculate the Diffusion Rate
			diffusionRate = spatialDistance / branchLength;
			
			// Calculate the mid-point in time for the branch
			time = findMidPointInTime(nodeInfo.getNodeInfo().get("height")[0], nodeInfo.getNodeInfo().get("height")[0]);
			
			Global.branchIndex++;
			
			WriteToFile.write(bWriter, Global.branchIndex + "\t" + branchLength + "\t" + spatialDistance + "\t" + diffusionRate + "\t" + time);
			
			// Get the information for the variables listed
			for(String string : variableNames){
				if(subNodeInfo.getNodeInfo().get(string) != null){
					WriteToFile.write(bWriter, "\t" + subNodeInfo.getNodeInfo().get(string)[0]);
				}else if(subNodeInfo.getBranchInfo().get(string) != null){
					WriteToFile.write(bWriter, "\t" + subNodeInfo.getNodeInfo().get(string)[0]);
				}else{
					WriteToFile.write(bWriter, "NA");
				}
			}
			WriteToFile.write(bWriter, "\n");			
			
			// Does the current subnode have any subnodes of its own?
			if(subNode.getSubNodes().length != 0){
				getBranchInfo(subNode, variableNames, latLongsTag, bWriter);
			}
			
		}
	}
	
	
	public static double findMidPointInTime(double a, double b){
		
		double diff = Math.abs(a - b);
		double value = 0;
		
		if( a > b){
			value = b + (diff/2);
		}else{
			value = a + (diff/2);
		}
		
		return value;
	}
	
	public static Branch[] append(Branch[] array, Branch branch){
		
		Branch[] newArray = new Branch[array.length + 1];
		
		for(int i = 0; i < array.length; i++){
			newArray[i] = array[i];
		}
		newArray[array.length] = branch;
		
		return newArray;
	}

}
