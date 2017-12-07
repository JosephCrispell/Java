package filterSensitivity;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Hashtable;
import methods.*;

import badgerPopulation.Badger;

public class HerdModelFitInput {

	public static void main(String[] args) throws NumberFormatException, IOException {
		
		/**
		 * Here the Genetic Sample Distance Matrix is Reduced to a Herd Distance Matrix. Nearest Sample Pairs
		 * are used to define genetic distances between herds.
		 * The epidemiological variables - used during the model fitting process are changed in order to match the
		 * sample pairs being used.
		 */
		
		//String file0="C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/origTest/originalFastaJava.txt";
		//String file1="C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/herdModelFitAll/herdDistanceMatrix.txt";
		//String file2="C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/herdModelFitAll/herdWeightedAdjacencyMatrix.txt";
		//String file3="C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/glmTest/javaGlmTable.txt";
		
		// Read in the Sample Sequence File and Create a Distance Matrix
		Sequence[] sequences = DistanceMatrixMethods.readFastaFile(args[0]);
		DistanceMatrix genetic = DistanceMatrixMethods.buildDistanceMatrix(sequences, "pDistance");
		
		// Read the Epidemiological information - Herd Distance and Weight Adjacency Matrices
		DistanceMatrix spatial = DistanceMatrixMethods.readInDistanceMatrix(args[1]);
		DistanceMatrix network = DistanceMatrixMethods.readInDistanceMatrix(args[2]);
		
		// Build the Herd Genetic Distance Matrix and Epidemiological Matrix
		int vntr10 = 0;
		buildGroupGeneticDistanceOutput(genetic, spatial, network, args[3], 'H', vntr10);		
		
	}
	
	public static void buildGroupGeneticDistanceOutput(DistanceMatrix genetic, DistanceMatrix spatial, DistanceMatrix network,
			String fileName, char group, int vntr10) throws IOException{
		
		/**
		 * Summarising the Sample Genetic Distance Matrix into a Group Genetic Distance Matrix
		 * 
		 * 	Genetic distance between groups is defined as the distance between the closest Sample pair between Group A and 
		 * 	Group B. Group can either be defined by Herd (H) or Episode (E).
		 * 	Epidemiological Distance between Groups is calculated by comparing the Information available for each of the
		 * 	Sample's in the pair.
		 * 
		 */
		
		/**
		 * Sample Name Structure:
		 * 	NSampleNo_AnimalID_HerdID_EpisodeID_Year_Badger_SampleID
		 * 
		 * Herd Distance/Weighted Adjacency Matrix:
		 * 	HerdA	HerdB	HerdC	HerdD
		 * 	HerdA	-	-	-	-
		 * 	HerdB	-	-	-	-
		 * 	HerdC	-	-	-	-
		 * 	HerdD	-	-	-	-
		 */
		String[] herds = spatial.getSampleNames();
		
		// Index the HerdId - to enable their information to be quickly extracted out of the Spatial Distance Matrix
		Hashtable<String, Integer> herdIdxs = new Hashtable<String, Integer>();
		for(int i = 0; i < herds.length; i++){
			herdIdxs.put(herds[i], i);
		}
				
		// Retrieve the Herd Spatial and Adjacency matrices
		double[][] herdD = spatial.getDistanceMatrix();
		double[][] herdAdj = network.getDistanceMatrix();
		
		// Retrieve Sample Names and Genetic distance Matrix
		String[] sampleNames = genetic.getSampleNames();
		double[][] sampleGeneticDist = genetic.getDistanceMatrix();
		
		/**
		 *  Print the Genetic, Spatial, Network, and Temporal Distances out to File
		 *  Print as columns in table for GLM:
		 *  	Genetic	Spatial	Network	Temporal
		 *  	-		-		-		-
		 *  	-		-		-		-
		 *  
		 *  Avoid the Diagonal and Repeated Observations
		 *  
		 *  Build the table as values are calculate by appending each line to growing String
		 */
		
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		String line = "Genetic\tSpatial\tNetwork\tTemporal";
		WriteToFile.writeLn(bWriter, line);
		
		// Keep record of the Herd Comparisons which have been completed - KEY: HerdA:HerdB and HerdB:HerdA
		Hashtable<String, Integer> done = new Hashtable<String, Integer>();
		String key1;
		String key2;
		
		// Are we ignoring non-VNTR types?
	
		// Calculate the Genetic, Spatial, Network and Temporal Distance Between Groups using Nearest pairs
		for(int i = 0; i < herds.length; i++){
			
			for(int j = 0; j < herds.length; j++){
				
				// Build the keys referring to current comparison
				key1 = herds[i] + ":" + herds[j];
				key2 = herds[j] + ":" + herds[i];
				
				// Are we ignoring non-VNTR types?
				if(herds[i].matches("(.*)29895(.*)") || herds[i].matches("(.*)31121(.*)")){
					if(vntr10 == 1){ continue;}
				}
				
				// Make comparison - check hasn't already been done already and avoid comparing same Group
				if(done.get(key1) == null && done.get(key2) == null && i != j){
					
					// Are we ignoring non-VNTR types?
					if(herds[j].matches("(.*)29895(.*)") || herds[j].matches("(.*)31121(.*)")){
						if(vntr10 == 1){ continue;}
					}
					
					// Find closest Genetic Sample pair - if more than one take first
					int[] pair = findNearestNeighbour4GroupPair(herds[i], herds[j], genetic, group);
					
					// Get the Sample Information
					String[] aParts = sampleNames[pair[0]].split("_");
					String[] bParts = sampleNames[pair[1]].split("_");
					
					// Get the Genetic Distance
					double geneticDistance = sampleGeneticDist[pair[0]][pair[1]];
					
					// Get the Spatial Distance - directly from symmetric distance matrix
					double spatialDistance = herdD[herdIdxs.get(aParts[2])][herdIdxs.get(bParts[2])];
					
					// Get the Herd Movements - Adjacency matrix is none symmetric
					double noMovements = herdAdj[herdIdxs.get(aParts[2])][herdIdxs.get(bParts[2])]; // a -> b
					noMovements += herdAdj[herdIdxs.get(bParts[2])][herdIdxs.get(aParts[2])]; // b -> a
					
					// Calculate the Temporal Difference in Years
					int aYear = Integer.parseInt(aParts[4]);
					int bYear = Integer.parseInt(bParts[4]);
					
					int yearDiff = 0;
					if(aYear > bYear){
						yearDiff = aYear - bYear;
					}else if(bYear > aYear){
						yearDiff = bYear - aYear;
					}
					
					// Store this table line for printing
					line = geneticDistance + "\t" + spatialDistance + "\t" + noMovements + "\t" + yearDiff;
					WriteToFile.writeLn(bWriter, line);
					
					// Record the Comparison - save time
					done.put(key1, 1);
					done.put(key2, 1);
				}
			}
			
			WriteToFile.write(bWriter, line);
		}
		
		// Print the table to Output
		
				
	}
	
	public static int[] findNearestNeighbour4GroupPair(String groupA, String groupB, DistanceMatrix genetic, char group){
		
		/**
		 * Find the Nearest Sample Pair between two different herds
		 */
		
		// Initialise Array to store the Sample Names of the pair
		int[] samplePair = new int[2];
		
		/**
		 * Find all samples in each of the two Groups
		 * 				0	1 	2	3	4	...
		 * 	Group	0	-	-	-	-	- 	...
		 * 	Group	1	-	-	-	-	-	...
		 */
		
		int[][] herdSampleIdxs = findAllSamplesInGroups(groupA, groupB, genetic.getSampleNames(), group);
		
		// Retrieve the Genetic Distance matrix
		double[][] d = genetic.getDistanceMatrix();
				
		// Compare these samples and Find the Closest Pair - in terms of Genetic Distance
		double min = 999999999;
		for(int i : herdSampleIdxs[0]){
			
			for(int j : herdSampleIdxs[1]){
				
				if(d[i][j] < min){
					
					// Store the New Min Genetic Distance
					min = d[i][j];
					
					// Store the Sample Names
					samplePair[0] = i;
					samplePair[1] = j;
					
				}
				
			}
		}
		
		return samplePair;
	}
	
	public static int[][] findAllSamplesInGroups(String groupA, String groupB, String[] sampleNames, char group){
		/**
		 * Find all Samples in two different Groups:
		 * 		Herd		H
		 * 		Episode		E
		 * 
		 * Sample Name Structure:
		 * 		NSampleNo_AnimalID_HerdID_EpisodeID_Year_Badger_SampleID
		 */
		
		// Store the Samples found in each of the two Groups
		int[][] sampleIdxs = new int[2][sampleNames.length];
		
		// Initialise indexes to record the next position to add a sample's Index
		int posA = -1;
		int posB = -1;
		
		// Choose the correct index dependent on what group investigating
		int part = -1;
		if(group == 'H'){
			part = 2;
		}else if(group == 'E'){
			part = 3;
		}
				
		// Examine each of the Samples
		for(int i = 0; i < sampleNames.length; i++){
			
			// Check if Sample's Group ID matches either of the two Groups
			String samplesHerd = (sampleNames[i].split("_"))[part];
			if(samplesHerd.equals(groupA)){
				posA++;
				sampleIdxs[0][posA] = i;
				
			}else if(samplesHerd.equals(groupB)){
				posB++;
				sampleIdxs[1][posB] = i;
			}
		}
		
		// Select the used indices of the two lists
		sampleIdxs[0] = ArrayMethods.subset(sampleIdxs[0], 0, posA);
		sampleIdxs[1] = ArrayMethods.subset(sampleIdxs[1], 0, posB);
		
		return sampleIdxs;
	}

}
