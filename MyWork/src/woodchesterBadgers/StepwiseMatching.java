package woodchesterBadgers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream.GetField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Hashtable;

import org.apache.commons.math3.random.MersenneTwister;


import filterSensitivity.DistanceMatrixMethods;
import filterSensitivity.Sequence;

import methods.ArrayMethods;
import methods.HashtableMethods;
import methods.RunCommand;
import methods.WriteToFile;


public class StepwiseMatching {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
	
		// Read in the Badger Group Location Information
		//String territoryCentroidsFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/TerritoryCentroids.csv";
		String territoryCentroidsFile = args[0];
		Hashtable<String, double[]> territoryCentroids = getTerritoryCentroids(territoryCentroidsFile, false);
		
		// Read in the Sample Epidemiological Information
		//String sampleInfoFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/sampleInformation.csv";
		String sampleInfoFile = args[1];
		Hashtable<String, SampleInfo> sampleInfo = CreateDescriptiveEpidemiologicalStats.getSampleInformation(sampleInfoFile);
		
		// Read the Sample Fasta Sequences
		//String fastaFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/vcfFiles/samplesFastaJava.txt";
		//String fastaFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/vcfFiles/samplesFastaJavaPoorlyMappedRemoved.txt";
		String fastaFile = args[2];
		Sequence[] sequences = DistanceMatrixMethods.readFastaFile(fastaFile);
		
		// Keep track of the Order of the Sequences
		String[] sampleIds = getSampleIds(sequences);
		
		/**
		 * Here we are trying to find the best re-shuffling of the sample IDs such that the genetic information is in best agreement with 
		 * the available epidemiological information - as measured by the variation explained in a fitted Random Forest model.
		 * 
		 * 	A list of Genetic Sequences
		 * 	Epidemiological Information associated with Sample Ids
		 * 
		 * The Epidemiological information will remain unchanged - but this aims to do is to reshuffle the sequences associated with the sampleIds.
		 * To start the original order is maintained:
		 * 		The sample Ids are taken from the sequences and it the order of this list that is used to access the sequences - therefore the order
		 * 		matches the original associations.
		 * 
		 * With each iteration:
		 * 		A random sequence is chosen
		 * 		A second sequence is chosen - random but close to the previous sequence
		 * 		These sequences are switched
		 * 
		 * 		The genetic vs. epidemiological distances table is created and random forest model fitted
		 * 
		 * 		The variation explained value is compared to the previous - If it is higher then this new sequence order is kept and previous discarded
		 * 
		 */
		
		// Set the Random Forest Model Settings
		int mtry = Integer.parseInt(args[6]);
		int ntree = Integer.parseInt(args[7]);
		
		// Initialise the necessary Variables
		//String geneticVsEpiTableFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/StepwiseMatching/geneticVsEpiTable.txt";
		String geneticVsEpiTableFile = args[3];
		String rFile = "\"C:\\Users\\Joseph Crisp\\Desktop\\UbuntuSharedFolder\\Woodchester\\Tools\\FitRFModel.R\"";
		String table = "\"C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/StepwiseMatching/geneticVsEpiTable.txt\"";
		String rOutput = "";
		double varExplained = 0;
		//String stateFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/StepwiseMatching/sampleIdOrder.txt";
		String stateFile = args[4];
		//String stateMaxFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/StepwiseMatching/sampleIdOrderMax.txt";
		String stateMaxFile = args[5];
		
		metropolisHastingsAlgorithm(sampleIds, sequences, sampleInfo, geneticVsEpiTableFile, mtry, ntree, rFile, table, stateFile, stateMaxFile, Integer.parseInt(args[8]));
	}
	
	public static void metropolisHastingsAlgorithm(String[] sampleIds, Sequence[] sequences, Hashtable<String, SampleInfo> sampleInfo, String geneticVsEpiTableFile, int mtry, int ntree,
			String rFile, String geneticsVsEpiTable4R, String stateFile, String stateMaxFile, int noIterations) throws IOException{
		
		// Initialise Variable to store Max value reached
		double maxVarExplained = 0;
		double varExplained = 0;
		String rOutput = "";
				
		// Create Instance of a Random Number Generator
		MersenneTwister random = new MersenneTwister();
				
		//****** Record the Initial State of the system
		String[] prevSampleIds = ArrayMethods.copy(sampleIds);
				
		// Build the Genetic vs. Epidemiological distances table - stored in file
		compareGenetic2EpiDistances(sampleIds, sequences, sampleInfo, geneticVsEpiTableFile);
				
		// Fit the Random Forest Model
		String output = fitRandomForestModel(rFile, geneticsVsEpiTable4R, mtry, ntree);
				
		// Get the Variation Explained Value
		double prevVarExplained = Double.parseDouble(output.split(" ")[0]);
		maxVarExplained = prevVarExplained;
				
		//****** Begin a Random Stepwise search for a better assignment
		for(int i = 0; i < noIterations; i++){
					
			System.out.print(i +"\t" + prevVarExplained + "\t");
					
			// Randomly shuffle a sequence pair - note that this is done by shuffling the sampleIds
			sampleIds = ArrayMethods.randomPoissonShuffle(prevSampleIds, 1, 1);
					
			// Build the Genetic vs. Epidemiological distances table - stored in file
			compareGenetic2EpiDistances(sampleIds, sequences, sampleInfo, geneticVsEpiTableFile);
				
			// Fit the Random Forest Model
			output = fitRandomForestModel(rFile, geneticsVsEpiTable4R, mtry, ntree);
				
			// Get the Variation Explained Value
			rOutput = output.split(" ")[0];
			if(rOutput.matches("")){ // Skip failed Random Forest runs
				System.out.println();
				continue;
			}
			varExplained = Double.parseDouble(rOutput);
					
			System.out.print(varExplained);
					
			// Is it better? - Metropolis Hastings Algorithm
			if(varExplained >= prevVarExplained){
					
				prevVarExplained = varExplained;
				prevSampleIds = ArrayMethods.copy(sampleIds);
						
				System.out.print("\t***");
						
				// Record change in system state
				recordSystemState(prevSampleIds, stateFile, varExplained);
					
				if(varExplained > maxVarExplained){
							
					maxVarExplained = varExplained;
					System.out.print("\t@@@");
					recordSystemState(prevSampleIds, stateMaxFile, varExplained);
				}
						
			}else if(random.nextDouble() < varExplained/prevVarExplained){
						
				prevVarExplained = varExplained;
				prevSampleIds = ArrayMethods.copy(sampleIds);
						
				System.out.print("\t===");
						
				// Record change in system state
				recordSystemState(prevSampleIds, stateFile, varExplained);				
			}
					
			System.out.println();
		}
	}
	
	public static void recordSystemState(String[] sampleIds, String stateFile, double varExplained) throws IOException{
		// Open the Output file
		BufferedWriter bWriter = WriteToFile.openFile(stateFile, false);
		
		WriteToFile.writeLn(bWriter, "VarExplained: " + varExplained);
		
		for(int i = 0; i < sampleIds.length; i++){
			WriteToFile.writeLn(bWriter, sampleIds[i]);
		}
		
		WriteToFile.close(bWriter);
	}
	
	public static String fitRandomForestModel(String rFile, String table, int mtry, int ntree) throws IOException{
		RunCommand result = new RunCommand("Rscript " + rFile + " " + table + " " + mtry + " " + ntree);
		
		return result.getOutput();
	}
	
	public static void  compareGenetic2EpiDistances(String[] sampleIds, Sequence[] sequences, Hashtable<String, SampleInfo> sampleInfo,
			String geneticVsEpiTableFile) throws IOException{
		
		// Open the Output table file
		BufferedWriter bWriter = WriteToFile.openFile(geneticVsEpiTableFile, false);
		
		// Print a header out into the file
		WriteToFile.writeLn(bWriter, "GeneticDist\tTemporalDist\tSpatialDist\tAreaSampled?\tGroupSampled?");
				
		// Initialise variables for genetic and epi distance
		double geneticDist = 0;
		double temporalDist = 0;
		double spatialDist = 0;
		int areaSampleDiff = 0;
		int groupDiff = 0;
		
		// Initialise variables for storing the sample information during comparisons
		SampleInfo sampleInfoI;
		SampleInfo sampleInfoJ;
		
		for(int i = 0; i < sampleIds.length; i++){
			
			// Get the Sample Information for I
			sampleInfoI = sampleInfo.get(sampleIds[i]);
			
			for(int j = 0; j < sampleIds.length; j++){
				
				// Only look at the upper diagonal - compare each to one another once
				if(i >= j){
					continue;
				}
				
				// Get the Sample Information for J
				sampleInfoJ = sampleInfo.get(sampleIds[j]);
				
				// Calculate the genetic distance between the two sequences
				geneticDist = calculateGeneticPDistance(sequences[i].getSequence(), sequences[j].getSequence());
				
				// Calculate the Temporal Distance between the two samples
				temporalDist = calculateTempDistance(sampleInfoI.getDate(), sampleInfoJ.getDate());
				
				// Calculate the Spatial Distance between the two samples
				spatialDist = 0;
				if(sampleInfoI.getBadgerGroup().matches(sampleInfoJ.getBadgerGroup()) == false){
					groupDiff = 1;
					
					if(sampleInfoI.getGroupCentroid() != null && sampleInfoJ.getGroupCentroid() != null){
						spatialDist = calculateSpatialDistance(sampleInfoI.getGroupCentroid(), sampleInfoJ.getGroupCentroid());
					}
				}
				
				// Were the samples taken from the same area?
				areaSampleDiff = 0;
				if(sampleInfoI.getSampleSite().matches(sampleInfoJ.getSampleSite()) == false){
					areaSampleDiff = 1;
				}
				
				// Print out the Genetic vs. Epidemiological Data table
				WriteToFile.writeLn(bWriter, geneticDist + "\t" + temporalDist + "\t" + spatialDist + "\t" + areaSampleDiff + "\t" + groupDiff);
			}
		}
				
		WriteToFile.close(bWriter);
	}
	
	public static double calculateSpatialDistance(double[] a, double[] b){
		// Calculate the Euclidian Distance between two points		
		return Math.sqrt( Math.pow((a[0] - b[0]), 2.0) + Math.pow((a[1] - b[1]), 2.0) );
		
	}
	
	public static double calculateTempDistance(Calendar a, Calendar b){
			
		long diffInMilliSec = Math.abs(a.getTimeInMillis() - b.getTimeInMillis());
		
		return diffInMilliSec / (24 * 60 * 60 * 1000);
	}
	
	public static String[] getSampleIds(Sequence[] sequences){
		String[] sampleIds = new String[sequences.length];
		
		for(int i = 0; i < sequences.length; i++){
			sampleIds[i] = sequences[i].getSampleName();
		}
		
		return sampleIds;
	}
	
	public static Hashtable<String, double[]> getTerritoryCentroids(String fileName,
			boolean latLongs) throws NumberFormatException, IOException{
		
		// Open the Territory Centroids CSV
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise a Hashtable to store the Territory Centroids
		Hashtable<String, double[]> territoryCentroids = new Hashtable<String, double[]>();
		
		// Initialise Variables for processing the file lines
		String line = "";
		String[] parts;
		double[] coordinates = new double[2];
		
		String[] nameParts;
		String name = "";
		
		// Reader the file
	    while(( line = reader.readLine()) != null){
	    	
	    	if(line.matches("SocialGroup(.*)")){
	    		continue;
	    	}
	    	
	    	// Split the line by ","
	    	parts = line.split(",");
	    	
	    	// Get the Territory coordinates
	    	coordinates[0] = Double.parseDouble(parts[1]);
	    	coordinates[1] = Double.parseDouble(parts[2]);
	    	
	    	if(latLongs == true){
	    		coordinates[0] = Double.parseDouble(parts[3]);
	    		coordinates[1] = Double.parseDouble(parts[4]);
	    	}
	    		
	    	// Remove spaces from group names
	    	nameParts = parts[0].split(" ");
	    	name = "";
	    	for(String part : nameParts){
	    		name = name + part;
	    	}
	    	
	    	// Store the Territory Centroid information in the hashtable
	    	territoryCentroids.put(name, coordinates);
	    	
	    	// Reset the coordinates
	    	coordinates = new double[2];
	    }
		
	    reader.close();
	    
	    // Return the hashtable
	    return territoryCentroids;
	}		
}
