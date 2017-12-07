package identifyingHomoplasies;

import java.io.IOException;
import java.util.Hashtable;

import geneticDistances.Sequence;
import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.GeneticMethods;
import methods.HashtableMethods;

public class HomoplasyFinderFASTA {

	public static void main(String[] args) throws IOException{
		
		// Set the path
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Homoplasmy/";
				
		// Get the current date
		String date = CalendarMethods.getCurrentDate("dd-MM-yy");
		
		/**
		 * Indexing SNPs in FASTA file
		 */
		
		// Read in the FASTA file
		String fasta = path + "example_" + date + ".fasta";
		Sequence[] sequences = GeneticMethods.readFastaFile(fasta);
		
		// Index the isolates
		Hashtable<String, Integer> isolateIndices =  HomoplasyFinder.getIndicesOfIsolatesSequences(sequences);
		
		// Get an array of the isolates associated with the genetic distance matrix
		String[] isolates = getIsolateNames(sequences);
		
		// Build a genetic distance matrix
		int[][] geneticDistances = GeneticMethods.createGeneticDistanceMatrix(sequences);
		
		/**
		 * Define clusters of isolates based upon genetic distance thresholds
		 */
		
		// Get a unique array of the genetic distances that exists within genetic distance matrix
		int[] uniqueDistances = getUniqueDistancesInGeneticDistanceMatrix(geneticDistances);
		
		// Find the clusters associated with each distance
		Hashtable<Integer, Clusters> clustersAssociatedWithDistances = defineClustersBasedUponThresholdDistances(
				uniqueDistances, geneticDistances, isolates);
		
		/*
		 * PROBLEM - Always end up with 1/2 clusters since all isolates can be linked along chains of 
		 * short genetic distances
		 */
		
		//--- Methods development area ->
		
		
		
		//--- Methods development area <-
		
		
//		// Examine Variant Postions in FASTA
//		Hashtable<Integer, VariantPosition> variantPositionInfo = 
//				HomoplasyFinder.countNoIsolatesWithEachAlleleAtEachVariantPositionInFasta(sequences);
//		/**
//		 * Identify the alleles associated with the reference - if available
//		 * 	Assumes no homoplasies back to reference <- impossible to find any?
//		 */
//		 
//		// Identify the reference sequence
//		//String reference = HomoplasyFinder.getReferenceID(); // If not available leave as NONE
//		String reference = "1";
//				
//		// Index the reference allele
//		Hashtable<Integer, Character> refAlleles = HomoplasyFinder.indexReferenceAlleles(reference, sequences,
//				isolateIndices);
	}
	
	public static Hashtable<Integer, Clusters> defineClustersBasedUponThresholdDistances(int[] thresholdDistances,
			int[][] geneticDistances, String[] isolates){
		
		// Initialise a Hashtable to store the clusters associated with each distance
		Hashtable<Integer, Clusters> clustersAssociatedWithDistances = new Hashtable<Integer, Clusters>();
		
		// Define clusters of isolates all within X differences of one another
		System.out.println(ArrayMethods.toString(isolates, ", "));
		for(int distance : thresholdDistances){
			
			System.out.println("------------------------------------------------------------");
			System.out.println("Distance = " + distance);
			clustersAssociatedWithDistances.put(distance, new Clusters(geneticDistances, isolates, distance));
			
			clustersAssociatedWithDistances.get(distance).printClusters();
		}
		
		return clustersAssociatedWithDistances;
	}
	
	public static String[] getIsolateNames(Sequence[] sequences){
		
		String[] isolates = new String[sequences.length];
		
		for(int i = 0; i < sequences.length; i++){
			isolates[i] = sequences[i].getName();
		}
		
		return(isolates);
	}
	
	public static int[] getUniqueDistancesInGeneticDistanceMatrix(int[][] geneticDistances){
		
		// Get a unique array of the genetic distances that exists within genetic distance matrix
		Hashtable<Integer, Integer> uniqueDistances = new Hashtable<Integer, Integer>();
		for(int i = 0; i < geneticDistances.length; i++){
			for(int j = 0; j < geneticDistances[0].length; j++){
				
				// Only examine upper triangle
				if(i >= j){
					continue;
				}
				
				// Store distance if never observed before
				if(uniqueDistances.get(geneticDistances[i][j]) == null){
					uniqueDistances.put(geneticDistances[i][j], 1);
				}else{
					uniqueDistances.put(geneticDistances[i][j], uniqueDistances.get(geneticDistances[i][j]) + 1);
				}
			}
		}
		
		return HashtableMethods.getKeysInt(uniqueDistances);
	}
}
