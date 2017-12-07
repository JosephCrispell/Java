package ExamineWPInterspeciesTransmission;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Hashtable;

import geneticDistances.GeneticDistances;
import geneticDistances.Sequence;
import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.GeneralMethods;
import methods.HashtableMethods;
import methods.MatrixMethods;
import methods.WriteToFile;
import woodchesterBadgers.CapturedBadgerLifeHistoryData;
import woodchesterBadgers.CreateDescriptiveEpidemiologicalStats;
import woodchesterCattle.CattleIsolateLifeHistoryData;
import woodchesterCattle.MakeEpidemiologicalComparisons;
import woodchesterGeneticVsEpi.CompareIsolates;

public class IdentifyIsolatesInvolved {
	
	public static void main(String[] args) throws IOException{
		
		String date = CalendarMethods.getCurrentDate("dd-MM-yyyy");
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/NewAnalyses_02-06-16/";
				
		// Read in the isolate sequences from a FASTA file
		String fastaFile = path + "InitialTree/";
		fastaFile += "sequences_plusRef_Prox-10_Cov-0.9_22-08-16.fasta";
		Sequence[] sequences = GeneticDistances.readFastaFile(fastaFile);
		CompareIsolates.setIsolateIds(sequences);
		String[] isolateIds = GeneticDistances.getSequenceNames(sequences);
		
		// Calculate the inter-isolate genetic distances
		int[][] distances = GeneticDistances.createGeneticDistanceMatrix(sequences);
		
//		// Find cattle and badgers that are close genetically
//		int[][] connections = defineConnectionsBasedOnGeneticDistance(5, distances);
//		
//		// Find the record the clusters
//		Cluster[] clusters = findClusters(connections, isolateIds);		
//		String clusterFile = path + "clusters.txt";
//		printClusterInformation(clusters, clusterFile);
//		
		
		//String[] isolates = {"TB1481", "TB1385", "TB1399", "TB1797", "TB1445", "TB1782",
		//					 "TB1810", "TB1801", "TB1753", "TB1447", "TB1805", "TB1480",
		//					 "TB1803", "TB1819"};
		
		String[] isolates = {"TB1481", "TB1782", "TB1753", "TB1805", "TB1819", "TB1473"};
		
		int minDistance = 3;
		
		Cluster[] clusters = findRelatedIsolates(isolates, isolateIds, distances, minDistance);
		
		System.out.println("\n----------------------------\n");
		for(Cluster cluster : clusters){
			System.out.println(cluster.getId() + "\t" + ArrayMethods.toString(cluster.getIsolateIds(), ", "));
		}
		
		String clusterFile = path + "InterSpeciesClusters/clusters_" + minDistance + 
				"-SNP_" + date + ".txt";
		printClusterInformation(clusters, clusterFile);

	}
	
	public static Cluster[] findRelatedIsolates(String[] isolates, String[] isolateIds,
			int[][] distances, int threshold){
		
		// Index the isolate Ids (the index in the genetic distance matrix)
		Hashtable<String, Integer> indexedIds = HashtableMethods.indexArray(isolateIds);
		
		// Initialise a hashtable to record whether an isolate has been added to a cluster
		Hashtable<Integer, Integer> addedToCluster = new Hashtable<Integer, Integer>();
		
		// Initialise an array to store the cluster information
		Cluster[] clusters = new Cluster[999];
		int clusterIndex = -1;
		String[] related;
		
		// For each of the isolates of interest, find their related isolates
		for(String isolate : isolates){
			
			// Skip if already added to cluster
			if(addedToCluster.get(indexedIds.get(isolate)) != null){
				continue;
			}
			
			// Find all the isolates that are less than the threshold number of SNPs to the current isolate
			related = getIsolatesWithinOrEqualThresholdGeneticDistance(
					distances[indexedIds.get(isolate)], threshold, isolateIds, addedToCluster);
			
			// Define a new cluster
			if(related.length > 1){ // Will automatically include original isolate (distance = 0)
				
				clusterIndex++;				
				clusters[clusterIndex] = new Cluster(clusterIndex, related);
			}else{
				System.out.println("Not able to find related individuals for isolate: " + isolate);
			}
		}
		
		return Cluster.subset(clusters, 0, clusterIndex);
	}
	
	public static String[] getIsolatesWithinOrEqualThresholdGeneticDistance(int[] row, int threshold,
			String[] isolateIds, Hashtable<Integer, Integer> addedToCluster){
		String[] isolates = new String[row.length];
		int index = -1;
		
		for(int i = 0; i < row.length; i++){
			
			if(row[i] <= threshold){
				index++;
				isolates[index] = isolateIds[i];
				addedToCluster.put(i, 1);
			}
		}
		
		return ArrayMethods.subset(isolates, 0, index);
	}
	
	public static void printClusterInformation(Cluster[] clusters, String fileName) throws IOException{
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		WriteToFile.writeLn(bWriter, "ID\tCluster");
		
		for(Cluster cluster : clusters){
			
			for(String id : cluster.getIsolateIds()){
				WriteToFile.writeLn(bWriter, id + "\t" + cluster.getId());
			}
		}
		
		// Close the output file
		WriteToFile.close(bWriter);		
	}
	
	public static Cluster[] findClusters(int[][] connections, String[] isolateIds){
		
		// Initialise an array to store the cluster information
		Cluster[] clusters = new Cluster[999];
		int cluster = -1;
		
		// Initialise a hashtable to record the ids of the connected individuals found in recursive search
		Hashtable<String, Integer> connectedIds;

		// Initialise a hashtable to record the individual whose connections we have already examined
		Hashtable<Integer, Integer> examined = new Hashtable<Integer, Integer>();
		
		// Examine every individual in the adjacency matrix
		for(int row = 0; row < connections.length; row++){
			
			System.out.println("Examining individual: " + isolateIds[row] + " (" + (row + 1) + "/" + connections.length + ")");
			
			// Skip individuals that we have already examined
			if(examined.get(row) != null){
				continue;
			}
			
			// Initialise an empty hashtable to record the individuals we find oin recursive search
			connectedIds = new Hashtable<String, Integer>();
			
			// Search for connected individuals to the current individual recursively
			findConnections(connections, row, isolateIds, connectedIds, examined);
			
			// Create a cluster if one or more connected individuals was found
			if(connectedIds.isEmpty() == false){
				
				cluster++;
				
				connectedIds.put(isolateIds[row], 1);
				clusters[cluster] = new Cluster(cluster, HashtableMethods.getKeysString(connectedIds));
			}			
		}
		
		return Cluster.subset(clusters, 0, cluster);
	}
	
	public static void findConnections(int[][] connections, int index, String[] isolateIds,
			Hashtable<String, Integer> connectedIds, Hashtable<Integer, Integer> examined){
		
		examined.put(index, 1);
		
		for(int i = 0; i < connections[index].length; i++){
			
			if(connections[index][i] == 1){
				
				connectedIds.put(isolateIds[i], 1);
			
				if(examined.get(i) == null){
					findConnections(connections, i, isolateIds, connectedIds, examined);
				}
			}
		}
	}
	
	public static Cluster[] defineClusters(int[][] connections, String[] isolateIds){
		
		// Initialise an array to define the clusters that each isolate has been added to
		int[] isolateClusters = ArrayMethods.repeat(-1, isolateIds.length);
		
		// Initialise an array to store the clusters that are found
		Cluster[] clusters = new Cluster[999];
		int clusterIndex = -1;
		
		// Examine the adjacencyMatrix and define the clusters
		for(int row = 0; row < connections.length; row++){
			
		}
		
		return clusters;
	}
	
	public static int[][] defineConnectionsBasedOnGeneticDistance(int threshold, int[][] distances){
		
		// Initialise an empty adjacency matrix
		int[][] connections = new int[distances.length][distances[0].length];
		
		// Fill the matrix, 1 if <= threshold
		for(int i = 0; i < distances.length; i++){
			for(int j = 0; j < distances.length; j++){
				
				// Skip self comparisons and the same comparison
				if(i >= j){
					continue;
				}
				
				if(distances[i][j] <= threshold){
					connections[i][j] = 1;
					connections[j][i] = 1;
				}
			}
		}
		
		return connections;
	}
	
	public static String getSpecies(String name){
		String species = "NA";
		
		if(name.matches("TB(.*)") == true){
			species = "COW";
		}else if(name.matches("WB(.*)") == true){
			species = "BADGER";
		}
		
		return species;
	}
}
