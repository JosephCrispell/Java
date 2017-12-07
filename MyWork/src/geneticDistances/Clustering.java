package geneticDistances;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Hashtable;
import java.util.Random;

import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.GeneralMethods;
import methods.HashtableMethods;
import methods.WriteToFile;

import org.uncommons.maths.random.MersenneTwisterRNG;

import phylogeneticTree.TerminalNode;
import testBEASTRateEstimation.RunStateTransitionSimulations;

public class Clustering {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		// Define the path to the working directory
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/NewAnalyses_12-05-16/";
		
		// Get the current date and time
		String date = CalendarMethods.getCurrentDate("dd-MM-yyyy");
		
		// Read the NEXUS file
		//String nexus = path + "Sequences/" + "sequences_14-06-16.nexus";
		String nexus = path + "Sequences/" + "sequences_MATCHED_14-06-16.nexus";
		Sequence[] isolateSequences = GeneticDistances.readNexusFile(nexus);
		
		// Read in the sample information
		String combinedSamplingInfo = path + "SamplingInformation/" + "combinedSamplingInformation_23-05-16.csv";
		Hashtable<String, String[]> isolateData = readCombinedSampleInformation(combinedSamplingInfo);
		
		// Calculate the Genetic Distances
		int[][] geneticDistances = GeneticDistances.createGeneticDistanceMatrix(isolateSequences);
		
		/**
		 * Combined sampling information file structure:
		 * 	FileID	AgRID	Year	Host	Latitude	Longitude	Location	Area	REA	Source	Removed
		 *	AgR288	NA		2011	BOVINE	-42.73245	171.15634	HOKITIKA	WEST	1	Farmed	0
		 *	0		1		2		3		4			5			6			7		8	9		10 
		 */
		
		// Initialise random seeds
		int[] seeds = RunStateTransitionSimulations.generateSeeds(4);		
		
		// Separate the isolate names and sequences
		String[] names = getSequenceNames(isolateSequences);

		int threshold = 0;
		int times = 10000;
		
		// ***** SPECIES *****
		System.out.println("Running Clustering Analysis for Species...");
		// Initialise a Random Number Generator
		int seed = seeds[0];
		System.out.println("Seed = " + seed);		
		Random random = GeneralMethods.startRandomNumberGenerator(seed);
		
		// Calculate the observed difference between mean intra- and inter-group genetic distances
		int labelIndex = 3; // HOST
		int accountFor = 6; // Account for spatial structure - Location
		System.out.println("Checking Label Index: Species example = " + checkLabelIndex(isolateData, labelIndex));
		System.out.println("Checking Label Index: Accounting for = " + checkLabelIndex(isolateData, accountFor));
		double observedDifference = calculateDifferenceBetweenMeanIntraAndInterGroupDistances(names, isolateData, geneticDistances, labelIndex, threshold, accountFor);
		
		// Generate a null distribution of observed differences
		String file = path + "GeneticDistanceClustering/" + "clustering_" + date + "_Species.txt";
		double[] nullDistribution = generateRandomMeanIntraInterGroupDifferenceDistribution(names, geneticDistances, isolateData, geneticDistances, labelIndex, threshold, times, file, observedDifference, random, accountFor);
		
		// ***** SOURCE *****
		System.out.println("Running Clustering Analysis for Source...");
		// Initialise a Random Number Generator
		seed = seeds[1];
		System.out.println("Seed = " + seed);		
		random = GeneralMethods.startRandomNumberGenerator(seed);
		
		// Calculate the observed difference between mean intra- and inter-group genetic distances
		labelIndex = 9;
		System.out.println("Checking Label Index: Source example = " + checkLabelIndex(isolateData, labelIndex));
		observedDifference = calculateDifferenceBetweenMeanIntraAndInterGroupDistances(names, isolateData, geneticDistances, labelIndex, threshold);
		
		// Generate a null distribution of observed differences
		file = path + "GeneticDistanceClustering/" + "clustering_" + date + "_Source.txt";
		nullDistribution = generateRandomMeanIntraInterGroupDifferenceDistribution(names, geneticDistances, isolateData, geneticDistances, labelIndex, threshold, times, file, observedDifference, random);
		
		
		// ***** LOCATION *****
		System.out.println("Running Clustering Analysis for Location...");
		// Initialise a Random Number Generator
		seed = seeds[2];
		System.out.println("Seed = " + seed);		
		random = GeneralMethods.startRandomNumberGenerator(seed);
		
		// Calculate the observed difference between mean intra- and inter-group genetic distances
		labelIndex = 6;
		System.out.println("Checking Label Index: Location example = " + checkLabelIndex(isolateData, labelIndex));
		observedDifference = calculateDifferenceBetweenMeanIntraAndInterGroupDistances(names, isolateData, geneticDistances, labelIndex, threshold);
				
		// Generate a null distribution of observed differences
		file = path + "GeneticDistanceClustering/" + "clustering_" + date + "_Location.txt";
		nullDistribution = generateRandomMeanIntraInterGroupDifferenceDistribution(names, geneticDistances, isolateData, geneticDistances, labelIndex, threshold, times, file, observedDifference, random);
		
		
		// ***** REA *****
		System.out.println("Running Clustering Analysis for REA...");
		// Initialise a Random Number Generator
		seed = seeds[3];
		System.out.println("Seed = " + seed);		
		random = GeneralMethods.startRandomNumberGenerator(seed);
		
		// Calculate the observed difference between mean intra- and inter-group genetic distances
		labelIndex = 8;
		System.out.println("Checking Label Index: REA example = " + checkLabelIndex(isolateData, labelIndex));
		observedDifference = calculateDifferenceBetweenMeanIntraAndInterGroupDistances(names, isolateData, geneticDistances, labelIndex, threshold);
		
		// Print the genetic distance distribution noting within or between REA distances
		file = path + "GeneticDistanceClustering/" + "geneticDistances_" + date + "_REA.txt";
		printGeneticDistancesNoteWithinBetween(names, file, isolateData, geneticDistances, labelIndex, threshold);
		
		// Generate a null distribution of observed differences
		file = path + "GeneticDistanceClustering/" + "clustering_" + date + "_REA.txt";
		nullDistribution = generateRandomMeanIntraInterGroupDifferenceDistribution(names, geneticDistances, isolateData, geneticDistances, labelIndex, threshold, times, file, observedDifference, random);
			
	}

	public static String checkLabelIndex(Hashtable<String, String[]> isolateData, int index){
		
		return isolateData.get(HashtableMethods.getKeysString(isolateData)[0])[index];
	}
	
	public static Hashtable<String, String[]> readCombinedSampleInformation(String fileName) throws IOException{
		
		/**
		 * Combined sample information file structure:
		 * 	FileID	AgRID	Year	Host	Latitude	Longitude	Location	Area	REA	Source	Removed
		 *	AgR288	NA		2011	BOVINE	-42.73245	171.15634	HOKITIKA	WEST	1	Farmed	0
		 *	0		1		2		3		4			5			6			7		8	9		10 
		 */
		
		// Initialise a Hashtable to store the isolate data
		Hashtable<String, String[]> isolateData = new Hashtable<String, String[]>();
		
		// Open the sample information file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
								
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
						
		// Begin reading the file
		while(( line = reader.readLine()) != null){
					
			// Ignore the header
			if(line.matches("FileID(.*)") == true){
				continue;
			}
				
			// Split the line into its columns
			cols = line.split(",");
				
			// Store the sampling information for the current isolate - key = ID_Year
			isolateData.put(cols[0] + "_" + cols[2], cols);
		}
								
		// Close the input file
		input.close();
		reader.close();
			
		return isolateData;		
	}
	
	public static void printGeneticDistancesNoteWithinBetween(String[] names, String outputFile,
			Hashtable<String, String[]> isolateData, int[][] distances, int labelIndex, int threshold) throws IOException{
		
		// Open an output file and print header
		BufferedWriter bWriter = WriteToFile.openFile(outputFile, false);
		WriteToFile.writeLn(bWriter, "Distance\tComparison");
		
		// Initialise variables to store the group of individuals in comparison
		String groupI = "";
		String groupJ = "";
			
		// Compare each Isolate to all others
		for(int i = 0; i < names.length; i++){
					
			// Get the group for individual I
			groupI = isolateData.get(names[i])[labelIndex];
			
			// Check that group exists
			if(groupI.matches("") == true || groupI.matches(" ") == true || groupI.matches("NA") == true){
				System.out.println("Group not available for Individual: " + names[i] + "\t" + groupI);
				continue;
			}
					
			for(int j =0; j < names.length; j++){
				
				// Make comparison once and ignore diagonal
				if(i >= j){
					continue;
				}
				
				// Ignore genetic distance if it is above a threshold value?
				if(threshold != 0 && distances[i][j] > threshold){
					continue;
				}
				
				// Get the group for individual J
				groupJ = isolateData.get(names[j])[labelIndex]; 
				
				// Check that group exists
				if(groupJ.matches("") == true || groupJ.matches(" ") == true || groupJ.matches("NA") == true){
					System.out.println("Group not available for Individual: " + names[j] + "\t" + groupJ);
					continue;
				}
				
				// Is the comparison inter or intra group?
				if(groupI.matches(groupJ) == true){ // INTRA group
					
					WriteToFile.writeLn(bWriter, distances[i][j] + "\tWITHIN");
					
				}else{ // INTER GROUP
					
					WriteToFile.writeLn(bWriter, distances[i][j] + "\tBETWEEN");
				}
			}
		}
		
		// Close the output file
		WriteToFile.close(bWriter);

	}
	
	public static Hashtable<String, String[]> readOtherSampleInformation(String sampleInfo, String linkFile, 
			Hashtable<String, String[]> isolateData) throws IOException{
		
		// Create a hashtable linking isolateID in sampleInformation (SupplierName) to those in NEXUS file (Lane)
		Hashtable<String, String> link = readIsolateLinkFile(linkFile);
		String name = "";
		
		/**
		 * Remaining isolates sample information file structure:
		 * 	AgR#	[]ug/ml	yrIsolatd	HOST	Latitude	Longitude	LOCATION	AREA
		 *	AgR135	50		1992		BOVINE	-43.156		172.730		AMBERLEY	NORTH CANT.
		 *	0		1		2			3		4			5			6			7
		 * 	
		 * 	BST	PVU	BCL	RESTTYPE	Source	Comments
		 * 	A21	A8	A6	62			Farmed	4 bovine and 2 ferrets from Amberley
		 * 	8	9	10	11			12		13
		 */
		
		// Open the sample information file
		InputStream input = new FileInputStream(sampleInfo);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
						
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
				
		// Initialise an array to store the information of interest
		String[] info = new String[8]; // Year, Species, Latitude, Longitude, Location, Area, REA, Source
						
		// Begin reading the file
		while(( line = reader.readLine()) != null){
					
			// Ignore the header
			if(line.matches("Sample(.*)") == false){
						
				cols = line.split(",");
				
				// Store the information of interest
				info[0] = cols[2]; // YEAR
				info[1] = cols[3]; // SPECIES
				info[2] = cols[4]; // LATITUDE
				info[3] = cols[5]; // LONGITUDE
				info[4] = removeSpaces(cols[6]); // LOCATION
				info[5] = removeSpaces(cols[7]); // AREA
				info[6] = cols[11]; // REA
				info[7] = cols[12]; // SOURCE
				
				// Get the laneID for the current SupplierName
				name = link.get(cols[0]) + "_" + cols[2];
				
				// Store the species of the current isolate
				isolateData.put(name, info);
						
				// Wipe the current isolate's data
				info = new String[8];
			}
		}
							
		// Close the input file
		input.close();
		reader.close();
		
		return isolateData;
	}
	
	public static String removeSpaces(String input){
		
		String output = "";
		String[] parts = input.split(" ");
		
		for(int i = 0; i < parts.length; i++){
			output = output + "" + parts[i];
		}
		
		return output;
	}
	
 	public static Hashtable<String, String> readIsolateLinkFile(String fileName) throws IOException{
		
		/**
		 * 	Lane		Sample			SupplierName	PublicName	Strain	ERS			ERR
		 *	12754_7#1	2982STDY5763550	AgR1			AgR1		NA		ERS421950	ERR564260
		 *	0			1				2				3			4		5			6
		 */

		// Initialise a Hashtable to link the SupplierName to the LaneID
		Hashtable<String, String> link = new Hashtable<String, String>();
		
		// Open the sample information file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
								
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
		
		// Begin reading the file
		while(( line = reader.readLine()) != null){
				
			// Ignore the header
			if(line.matches("Lane(.*)") == false){
				
				cols = line.split(",");
				link.put(cols[2], cols[0]);
			}
		}
							
		// Close the input file
		input.close();
		reader.close();
		
		return link;
	}

	public static Hashtable<String, String[]> readWestCoastSampleInformation(String fileName, 
			Hashtable<String, String[]> isolateData) throws IOException{
		
		/**
		 * 	SampleName	year	HOST	Latitude	Longitude	LOCATION	AREA
		 *	AgR288		2011	BOVINE	-42.724		170.978		HOKITIKA	WESTCOAST
		 *	0			1		2		3			4			5			6
		 *	
		 *	REA	LatitudeNew	LongitudeNew	Source	,	,	,	,	,	,
		 *	1	-42.732		171.156			Farmed	,	,	,	,	,	,
		 *	7	8			9				10		11	12	13	14	15	16
		 */
		
		// Open the sample information file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
						
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
		
		// Initialise an array to store the information of interest
		String[] info = new String[8]; // Year, Species, Latitude, Longitude, Location, Area, REA, Source
						
		// Begin reading the file
		while(( line = reader.readLine()) != null){
					
			// Ignore the header
			if(line.matches("Sample(.*)") == false){
						
				cols = line.split(",");
				
				// Store the information of interest
				info[0] = cols[1]; // YEAR
				info[1] = cols[2]; // SPECIES
				info[2] = cols[8]; // LATITUDE
				info[3] = cols[9]; // LONGITUDE
				info[4] = cols[5]; // LOCATION
				info[5] = cols[6]; // AREA
				info[6] = cols[7]; // REA
				info[7] = cols[10]; // SOURCE
								
				// Store the species of the current isolate
				isolateData.put(cols[0] + "_" + cols[1], info);
				
				// Wipe the current isolate's data
				info = new String[8];
			}
		}
						
		// Close the input file
		input.close();
		reader.close();
		
		return isolateData;
		
	}
	
	public static String[] getSequenceNames(Sequence[] sequences){
		String[] names = new String[sequences.length];
		
		for(int i = 0; i < sequences.length; i++){
			
			names[i] = sequences[i].getName();
		}
		
		return names;
	}
	
	public static char[][] getSequences(Sequence[] isolateSequences){
		char[][] sequences = new char[isolateSequences.length][0];
		
		for(int i = 0; i < sequences.length; i++){
			
			sequences[i] = isolateSequences[i].getSequence();
		}
		
		return sequences;
	}
	
	public static double[] generateRandomMeanIntraInterGroupDifferenceDistribution(String[] names,
			int[][] geneticDistances, Hashtable<String, String[]> isolateData, int[][] distances, int labelIndex,
			int threshold, int times, String outFile, double actualDifference, Random random) throws IOException{
		
		// Copy the sequence names
		String[] copyNames = ArrayMethods.copy(names);
		
		// Open an output file and print the actual calculated Mean inter- vs intra-group difference
		BufferedWriter bWriter = WriteToFile.openFile(outFile, false);
		WriteToFile.writeLn(bWriter, "ActualDiff(" + actualDifference + ")");
		
		// Initialise an array to store the null distribution
		double[] result = new double[times];
		double difference;
		
		// Randomly shuffle isolate names and re-calculate the mean inter vs intra-group distance
		for(int i = 0; i < times; i++){
			
			// Randomly shuffle the sequence names
			copyNames = ArrayMethods.shuffle(copyNames, random);
			
			// Re-calculate the mean inter vs intra-group distance
			difference = calculateDifferenceBetweenMeanIntraAndInterGroupDistances(copyNames, isolateData, distances, labelIndex, threshold);
			result[i] = difference;
			
			// Write the result to file
			WriteToFile.writeLn(bWriter, result[i]);
			
			if( (i + 1) % 1000 == 0){
				System.out.println("Finished " + (i + 1) + " iterations.");
			}
		}
		
		// Close the output file
		WriteToFile.close(bWriter);
		
		return result;
	}
	
	public static double[] generateRandomMeanIntraInterGroupDifferenceDistribution(String[] names,
			int[][] geneticDistances, Hashtable<String, String[]> isolateData, int[][] distances, int labelIndex,
			int threshold, int times, String outFile, double actualDifference, Random random,
			int accountForLabelIndex) throws IOException{
		
		// Copy the sequence names
		String[] copyNames = ArrayMethods.copy(names);
		
		// Open an output file and print the actual calculated Mean inter- vs intra-group difference
		BufferedWriter bWriter = WriteToFile.openFile(outFile, false);
		WriteToFile.writeLn(bWriter, "ActualDiff(" + actualDifference + ")");
		
		// Initialise an array to store the null distribution
		double[] result = new double[times];
		double difference;
		
		// Randomly shuffle isolate names and re-calculate the mean inter vs intra-group distance
		for(int i = 0; i < times; i++){
			
			// Randomly shuffle the sequence names
			copyNames = ArrayMethods.shuffle(copyNames, random);
			
			// Re-calculate the mean inter vs intra-group distance
			difference = calculateDifferenceBetweenMeanIntraAndInterGroupDistances(copyNames, isolateData, distances, labelIndex, threshold, accountForLabelIndex);
			result[i] = difference;
			
			// Write the result to file
			WriteToFile.writeLn(bWriter, result[i]);
			
			if( (i + 1) % 1000 == 0){
				System.out.println("Finished " + (i + 1) + " iterations.");
			}
		}
		
		// Close the output file
		WriteToFile.close(bWriter);
		
		return result;
	}
	
	public static double calculateDifferenceBetweenMeanIntraAndInterGroupDistances(String[] names,
			Hashtable<String, String[]> isolateData, int[][] distances, int labelIndex, int threshold){
		
		// Initialise an array to store the mean Intra and Inter Group Distances
		double[] result = new double[2];
		
		// Initialise variables to note the number of within and between distances examined
		int noWithin = 0;
		int noBetween = 0;
		
		// Initialise variables to store the group of individuals in comparison
		String groupI = "";
		String groupJ = "";
		
		// Compare each Isolate to all others
		for(int i = 0; i < names.length; i++){
			
			// Get the group for individual I
			groupI = isolateData.get(names[i])[labelIndex];
			
			// Check that group exists
			if(groupI.matches("") == true || groupI.matches(" ") == true || groupI.matches("NA") == true){
				System.out.println("Group not available for Individual: " + names[i] + "\t" + groupI);
				continue;
			}
			
			for(int j =0; j < names.length; j++){
				
				// Make comparison once and ignore diagonal
				if(i >= j){
					continue;
				}
				
				// Ignore genetic distance if it is above a threshold value?
				if(threshold != 0 && distances[i][j] > threshold){
					continue;
				}
				
				// Get the group for individual J
				groupJ = isolateData.get(names[j])[labelIndex]; 
				
				// Check that group exists
				if(groupJ.matches("") == true || groupJ.matches(" ") == true || groupJ.matches("NA") == true){
					System.out.println("Group not available for Individual: " + names[j] + "\t" + groupJ);
					continue;
				}
				
				// Is the comparison inter or intra group?
				if(groupI.matches(groupJ) == true){ // INTRA group
					
					result[0] += (double) distances[i][j];
					noWithin++;
					
					//System.out.println("INTRA Group Comparison: " + groupI + " - " + groupJ);
					
				}else{ // INTER GROUP
					
					result[1] += (double) distances[i][j];
					noBetween++;
					
					//System.out.println("INTER Group Comparison: " + groupI + " - " + groupJ);
				}
			}
		}
		
		// Convert the sums to means
		result[0] = result[0] / (double) noWithin;
		result[1] = result[1] / (double) noBetween;
		
		// Difference is the mean Inter-group distance minus the mean Intra-group distance
		return result[1] - result[0];
	}
	
	public static double calculateDifferenceBetweenMeanIntraAndInterGroupDistances(String[] names,
			Hashtable<String, String[]> isolateData, int[][] distances, int labelIndex, int threshold,
			int accountForLabelIndex){
		
		// Initialise an array to store the mean Intra and Inter Group Distances
		double[] result = new double[2];
		
		// Initialise variables to note the number of within and between distances examined
		int noWithin = 0;
		int noBetween = 0;
		
		// Initialise variables to store the group of individuals in comparison
		String groupI = "";
		String groupJ = "";
		String otherGroupI = "";
		String otherGroupJ = "";
		
		// Compare each Isolate to all others
		for(int i = 0; i < names.length; i++){
			
			// Get the group for individual I
			groupI = isolateData.get(names[i])[labelIndex];
			
			// Get the upper level group for individual I
			otherGroupI = isolateData.get(names[i])[accountForLabelIndex];
			
			// Check that group exists
			if(groupI.matches("") == true || groupI.matches(" ") == true || groupI.matches("NA") == true){
				System.out.println("Group not available for Individual: " + names[i] + "\t" + groupI);
				continue;
			}
			if(otherGroupI.matches("") == true || otherGroupI.matches(" ") == true || otherGroupI.matches("NA") == true){
				System.out.println("Group (to account for) not available for Individual: " + names[i] + "\t" + otherGroupI);
				continue;
			}
			
			for(int j =0; j < names.length; j++){
				
				// Make comparison once and ignore diagonal
				if(i >= j){
					continue;
				}
				
				// Ignore genetic distance if it is above a threshold value?
				if(threshold != 0 && distances[i][j] > threshold){
					continue;
				}
				
				// Get the group for individual J
				groupJ = isolateData.get(names[j])[labelIndex]; 
				
				// Get the upper level group for individual I
				otherGroupJ = isolateData.get(names[j])[accountForLabelIndex];
								
				// Check that group exists
				if(groupJ.matches("") == true || groupJ.matches(" ") == true || groupJ.matches("NA") == true){
					System.out.println("Group not available for Individual: " + names[j] + "\t" + groupJ);
					continue;
				}
				if(otherGroupJ.matches("") == true || otherGroupJ.matches(" ") == true || otherGroupJ.matches("NA") == true){
					System.out.println("Group (to account for) not available for Individual: " + names[j] + "\t" + otherGroupJ);
					continue;
				}
				
				// Do both isolates have the same upper level group? The grouping that we are accounting for
				if(otherGroupI.matches(otherGroupJ) == false){
					
					continue;
				}
				
				// Is the comparison inter or intra group?
				/**
				 *  Note that here, if necessary, we can account for the influence of different level of grouping
				 *  	Select only the within and between group distances that are made within the group (of the
				 *  	different level)
				 */
				if(groupI.matches(groupJ) == true){ // INTRA group
					
					result[0] += (double) distances[i][j];
					noWithin++;
					
				}else{ // INTER GROUP
					
					result[1] += (double) distances[i][j];
					noBetween++;
				}
			}
		}
		
		// Convert the sums to means
		result[0] = result[0] / (double) noWithin;
		result[1] = result[1] / (double) noBetween;
		
		// Difference is the mean Inter-group distance minus the mean Intra-group distance
		return result[1] - result[0];
	}
}
