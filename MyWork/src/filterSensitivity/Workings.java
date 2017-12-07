package filterSensitivity;

import java.io.IOException;

import methods.*;


public class Workings {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		/**
		 *  Read in Original Distance Matrix
		 *  
		 *  Command Line Structure:
		 *  java -jar jarFile originalDistanceMatrix.txt currentFasta.txt treeDirectory Depth HQDepth MQ HZGTY  
		 *  
		 *  Filters:
		 *  Depth 20; HQDepth 2; MQ 20; Heterozygosity 0.95
		 */
		
		DistanceMatrix origDistanceMatrixInfo = DistanceMatrixMethods.readInDistanceMatrix(args[0]);
		
		/**
		 * Read in Current Sequence Fasta File and Create a Distance Matrix
		 * How consistent is the Current tree with the Original Tree?
		 */
		
		Sequence[] sequences = DistanceMatrixMethods.readFastaFile(args[1]);
		
		DistanceMatrix currDistanceMatrixInfo = DistanceMatrixMethods.buildDistanceMatrix(sequences, "pDistance");
		
		/**
		 * Comparing the Current tree to the 'Original' tree:
		 * 		- How consistent are the Nearest Neighbour Distributions?
		 * 				At the Sample Level
		 * 				At the Herd Level - need to be able to define the herd within the phylogenetic tree
		 * 				In Time - using the Year of Sampling
		 * 
		 * Sample Name Structure:
		 * 		NSampleNo_AnimalID_HerdID_Year_Badger_SampleID
		 */
		
		// Sample Level Nearest Neighbour Distribution Consistency
		double[] proportionsNeighboursConsistent = DistanceMatrixMethods.compareNearestNeighbours(origDistanceMatrixInfo, currDistanceMatrixInfo);
		double meanNeighbourDistributionConsistency = ArrayMethods.mean(proportionsNeighboursConsistent);
		
		
		// Herd Level Nearest Neighbour Distribution Consistency
		/**
		 * Plan 1:
		 * 	For each Sample have a Array storing the proportion of the Sample's nearest neighbours are from a given herd
		 * 		A -> (B,1), (C,2), (D,1), (E,1)
		 * 		Groups -> 1,2,3,4
		 * 		A -> (0.75, 0.25, 0, 0)
		 * 	How do you compare these distributions?
		 * 
		 * Plan 2:
		 * 	Generate a Herd Distance Matrix
		 *  	Mij = sum of the distance from each of the Samples from Herd i to each of the Samples from Herd j
		 */
		
		// Plan 1
		double[] proportionHerdsConsistent = DistanceMatrixMethods.compareNearestNeighbourGroupProportions(origDistanceMatrixInfo, currDistanceMatrixInfo, 'H');
		double meanNeighbourHerdConsistency = ArrayMethods.mean(proportionHerdsConsistent);
		
		// Plan 2
		double[] proportionsHerdNeighboursConsistent = DistanceMatrixMethods.compareGroupNearestNeighbourDistributions(origDistanceMatrixInfo, currDistanceMatrixInfo, 'H');
		double meanNearestHerdConsistency = ArrayMethods.mean(proportionsHerdNeighboursConsistent);
		
		
		// Herd Level Nearest Neighbour Distribution Consistency
		/**
		 * Plan 1:
		 * 	For each Sample have a Array storing the proportion of the Sample's nearest neighbours are from a given herd
		 * 		A -> (B,1), (C,2), (D,1), (E,1)
		 * 		Groups -> 1,2,3,4
		 * 		A -> (0.75, 0.25, 0, 0)
		 * 	How do you compare these distributions?
		 * 
		 * Plan 2:
		 * 	Generate a Herd Distance Matrix
		 *  	Mij = sum of the distance from each of the Samples from Herd i to each of the Samples from Herd j
		 */
		
		// Plan 1
		double[] proportionEpisodesConsistent = DistanceMatrixMethods.compareNearestNeighbourGroupProportions(origDistanceMatrixInfo, currDistanceMatrixInfo, 'E');
		double meanNeighbourEpisodesConsistency = ArrayMethods.mean(proportionEpisodesConsistent);
		
		// Plan 2
		double[] proportionsEpisodeNeighboursConsistent = DistanceMatrixMethods.compareGroupNearestNeighbourDistributions(origDistanceMatrixInfo, currDistanceMatrixInfo, 'E');
		double meanNearestEpisodeConsistency = ArrayMethods.mean(proportionsEpisodeNeighboursConsistent);
		
		
		// Store the Consistency Measure Outputs
		System.out.println(meanNeighbourDistributionConsistency + "\t" + meanNeighbourHerdConsistency + "\t" + meanNearestHerdConsistency + "\t" + meanNeighbourEpisodesConsistency + "\t" + meanNearestEpisodeConsistency);
		
		// Store the Distance Matrix from the Current Filter Combination - Depth HQDepth MQ HZGTY
		//String fileName = args[2] + "distanceMatrix_" + args[3] + "_" + args[4] + "_" + args[5] + "_" + args[6] + "_.txt";
		//DistanceMatrixMethods.print(currDistanceMatrixInfo, fileName);
	}
}

