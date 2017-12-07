package workForMarian;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.WriteToFile;
import filterSensitivity.DistanceMatrix;
import filterSensitivity.DistanceMatrixMethods;
import filterSensitivity.NJTreeMethods;
import filterSensitivity.Node;
import filterSensitivity.Sequence;

public class CreateReaDistanceMatrix {

	public static void main(String[] args) throws IOException {
		
		
		// Get the Sequences from the Fasta File
		String fastaFile = "/Users/josephcrisp1/Desktop/NZ_Marian_11-03-15.fasta";
		Sequence[] sequences = getSequencesFromFasta(fastaFile);
		
		// Create the genetic distance matrix
		DistanceMatrix geneticDistances = DistanceMatrixMethods.buildDistanceMatrix(sequences, "pDistance");
		
		// Calculate the mean distances between REA types
		DistanceMatrix reaDistances = buildReaDistanceMatrix2(geneticDistances, 1);
		
		// Convert the REA distances from p-distances to SNP distances
		reaDistances.setDistanceMatrix(convertPDistancesToSnpDistances(reaDistances.getDistanceMatrix(), sequences[0].getSequence().length));
		
		// Build the Neighbour Joining Tree
		Node tree = NJTreeMethods.buildNJTree(reaDistances);

		// Print the NJ tree out to file
		String outputTreeFile = "/Users/josephcrisp1/Desktop/test.tree";
		BufferedWriter out = WriteToFile.openFile(outputTreeFile, false);
		NJTreeMethods.printNode(tree, out);
		WriteToFile.close(out);
		
		// Print out the REA distance matrix
		String outputMatrixFile = "/Users/josephcrisp1/Desktop/distanceMatrix.txt";
		printDistanceMatrix(reaDistances, outputMatrixFile);
		
	}
	
	public static void printDistanceMatrix(DistanceMatrix info, String outputFile) throws IOException{
		
		BufferedWriter bWriter = WriteToFile.openFile(outputFile, false);
		
		String[] names = info.getSampleNames();
		double[][] distances = info.getDistanceMatrix();
		
		// Print out the column names
		String colnames = "";
		for(int i = 0; i < names.length; i++){
			colnames = colnames + "\tREA" + names[i];
		}
		
		WriteToFile.writeLn(bWriter, colnames);
		
		for(int i = 0; i < names.length; i++){
			
			WriteToFile.write(bWriter, "REA" + names[i]);
			
			for(int j = 0; j < names.length; j++){
				
				WriteToFile.write(bWriter, "\t" + distances[i][j]);
				
			}
			
			WriteToFile.writeLn(bWriter, "");
		}
		
		WriteToFile.close(bWriter);
	}
	
	public static double[][] convertPDistancesToSnpDistances(double[][] distances, int sequenceLength){
		
		double[][] snpDistances = new double[distances.length][distances[0].length];
		
		double value;
		
		for(int i = 0; i < distances.length; i++){
			for(int j = 0; j < distances[0].length; j++){
				
				if(i >= j){
					continue;
				}
				
				value = distances[i][j] * (double) sequenceLength;
				
				snpDistances[i][j] = value;
				snpDistances[j][i] = value;
				
			}
		}
		
		
		return snpDistances;
	}
	
	public static String[] getUniqueReaTypes(String[] sampleNames, int index){
		
		/**
		 * Extract all the unique REA types found for the isolates looking at
		 * 
		 * SampleName Structure: N27_REA20_2008_B_Julian_Opunaki_CNI
		 */
		
		// Get all the REA types observed
		String[] types = new String[sampleNames.length];
		for(int i = 0; i < sampleNames.length; i++){
			types[i] = sampleNames[i].split("_")[index].substring(3);
		}
		
		return ArrayMethods.unique(types);
	}
	
	public static Hashtable<String, Integer> indexArray(String[] array){
		
		Hashtable<String, Integer> indexed = new Hashtable<String, Integer>();
		
		for(int i = 0; i < array.length; i++){
			indexed.put(array[i], i);
		}
		
		return indexed;		
		
	}
	
	public static Hashtable<String, double[]> createArraysToStoreDistancesFromComparisons(String[] reaTypes){
		
		// Initialize a Hashtable to store the comparisons
		Hashtable<String, double[]> arrays = new Hashtable<String, double[]>();
		String key;
		
		for(int i = 0; i < reaTypes.length; i++){
			for(int j = 0; j < reaTypes.length; j++){
				
				// Only going to make the comparison once
				if(i >= j){
					continue;
				}
				
				// Create a key REA:REA and initialise array
				key = reaTypes[i] + ":" + reaTypes[j];
				arrays.put(key, new double[0]);		
			}
		}
		
		return arrays;
		
	}	
	
	public static DistanceMatrix buildReaDistanceMatrix(DistanceMatrix geneticDistances, int index){
		
		// Get the unique REA types
		String[] reaTypes = getUniqueReaTypes(geneticDistances.getSampleNames(), index);
		
		// Need to have the array of between REA type distances for each REA comparison stored
		Hashtable<String, double[]> arrays = createArraysToStoreDistancesFromComparisons(reaTypes);
		
		// Compare each isolate to one another and place the genetic distance in correct REA comparison array
		arrays = makeReaComparisons(geneticDistances, arrays);
		
		// Fill the REA type distance Matrix
		Hashtable<String, Integer> indexedReas = indexArray(reaTypes); // Index the REA types
		double[][] reaDistances = new double[reaTypes.length][reaTypes.length]; // Initialize a REA distance matrix
		
		double[] distribution;
		double value;
		
		for(int i = 0; i < reaTypes.length; i++){
			for(int j = 0; j < reaTypes.length; j++){
				
				// Only look at REA comparisons once
				if(i >= j){
					continue;
				}
				
				// Get the distribution of genetic distances for this particular REA comparison
				if(arrays.get(reaTypes[i] + ":" + reaTypes[j]) != null){
					
					distribution = arrays.get(reaTypes[i] + ":" + reaTypes[j]);
					
					value = ArrayMethods.mean(distribution);
					reaDistances[i][j] = value;
					reaDistances[j][i] = value;
					
				}else if(arrays.get(reaTypes[j] + ":" + reaTypes[i]) != null){
					
					distribution = arrays.get(reaTypes[j] + ":" + reaTypes[i]);
					
					value = ArrayMethods.mean(distribution);
					reaDistances[i][j] = value;
					reaDistances[j][i] = value;
				}
			}
		}

		return new DistanceMatrix(reaTypes, reaDistances);
		
	}
	
	public static Hashtable<String, double[]> makeReaComparisons(DistanceMatrix geneticDistances,
			Hashtable<String, double[]> arrays){
		
		/**
		 * A distribution of genetic distances resulting from particular REA type comparisons is stored
		 * to allow summary metrics to be calculated.
		 */
		
		String[] sampleNames = geneticDistances.getSampleNames();
		double[][] matrix = geneticDistances.getDistanceMatrix();
		String reaI;
		String reaJ;
		double[] distribution;
		
		for(int i = 0; i < sampleNames.length; i++){
			
			reaI = sampleNames[i].split("_")[1].substring(3);
			
			for(int j = 0; j < sampleNames.length; j++){
				
				reaJ = sampleNames[j].split("_")[1].substring(3);
				
				// Only make the comparison once
				if(i >= j){
					continue;
				}
				
				// Get the array of genetic distances between these two REA types and append the current distance
				if(arrays.get(reaI + ":" + reaJ) != null){
					
					distribution = arrays.get(reaI + ":" + reaJ);
					distribution = ArrayMethods.append(distribution, matrix[i][j]);
					
					arrays.put(reaI + ":" + reaJ, distribution);
					
				}else if(arrays.get(reaJ + ":" + reaI) != null){
					
					distribution = arrays.get(reaJ + ":" + reaI);
					distribution = ArrayMethods.append(distribution, matrix[i][j]);
					
					arrays.put(reaJ + ":" + reaI, distribution);
				}				
			}
		}
		
		return arrays;
	}
	
	public static Sequence[] getSequencesFromFasta(String fastaFile) throws IOException{
		
		/**
		 * Extracting Nucleotide Sequences from standard FASTA file
		 * 
		 * 	FASTA file format:
		 * 		>N27_REA20_2008_B_Julian_Opunaki_CNI
		 *		ATGGGAGGCTCTAGGGCCGCTCCAGCGAACGACTCCCCG
		 *		>AgR182_REA62_2003_B_Culverden_NC
		 *		ATGGGAGGCTCTAGGGCCGCTCCAGCGAACGACGCCCCG
		 */
		
		// Initialize an Array to store the Sequences
		Sequence[] sequences = new Sequence[99999];
		int posUsed = -1;
		
		// Open the FASTA file for reading
		InputStream input = new FileInputStream(fastaFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialize variables to keep track of when we are reading sequences
		int reading = 0;
		String sampleName = "";
		String sequence = "";
		char[] nucleotides;
		
		// Begin reading the Fasta File
		String line = null;
		int lineNo = -1;
	    while(( line = reader.readLine()) != null){
	    	lineNo++;
	    	
	    	if(line.matches("(^>.*)")){
	    		
	    		// Found a new Isolate!
	    		sampleName = line.substring(1);
	    		
	    		// Store the previous Sequence unless this is the first
	    		if(lineNo != 0){
	    			
	    			// Split the sequence into its nucleotides
	    			nucleotides = sequence.toCharArray();
	    			
	    			// Store the sequence and associated sample name
	    			posUsed++;
	    			sequences[posUsed] = new Sequence(sampleName, nucleotides);    			
	    		}
	    		
	    		// Reset the Sequence
	    		sequence = "";
	    		
	    	}else if(line.matches("(^[A-Z])(.*)")){

	    		sequence = sequence + "" + line;
	    	}
	    }
	    
	    return subset(sequences, 0, posUsed);
	}
	
	public static Sequence[] subset(Sequence[] array, int start, int end){
		Sequence[] part = new Sequence[end - start + 1];
		
		int pos = -1;
		for(int index = 0; index < array.length; index++){
			
			if(index >= start && index <= end){
				pos++;
				part[pos] = array[index];
			}
		}
		
		return part;
	}
	
	// -------- Testing Different Method
	
	public static double[][][] getGeneticDistancesFromReaComparisons(DistanceMatrix geneticDistances, Hashtable<String, Integer> reaTypes, int index){
		
		// Initialize a Matrix to store the genetic distance distribution resulting from each inter-REA comparison
		double[][][] comparisonDistributions = new double[reaTypes.size()][reaTypes.size()][0];
		
		// Get the sample names and genetic distance matrix
		String[] sampleNames = geneticDistances.getSampleNames();
		double[][] distances = geneticDistances.getDistanceMatrix();
		
		// Compare each of the isolates to one another
		String reaI;
		int indexReaI;
		String reaJ;
		int indexReaJ;
		double[] distribution;
		
		for(int i = 0; i < sampleNames.length; i++){
			
			reaI = sampleNames[i].split("_")[index].substring(3);
			indexReaI = reaTypes.get(reaI);
			
			for(int j = 0; j < sampleNames.length; j++){
				
				if(i >= j){
					continue;
				}
				
				reaJ = sampleNames[j].split("_")[index].substring(3);
				indexReaJ = reaTypes.get(reaJ);
				
				// Get the genetic distance distribution for the current comparison
				distribution = comparisonDistributions[indexReaI][indexReaJ];
				
				// Append the current genetic distance
				distribution = ArrayMethods.append(distribution, distances[i][j]);
				comparisonDistributions[indexReaI][indexReaJ] = distribution;				
			}
		}
		
		return comparisonDistributions;		
	}
	
	public static DistanceMatrix buildReaDistanceMatrix2(DistanceMatrix geneticDistances, int index){
		
		// Get the unique REA types
		String[] reaTypes = getUniqueReaTypes(geneticDistances.getSampleNames(), index);
		
		// Index the REA types
		Hashtable<String, Integer> indexedReas = indexArray(reaTypes); // Index the REA types
		
		// Get the Genetic Distance Distributions specific to each REA type comparison
		double[][][] comparisonDistributions = getGeneticDistancesFromReaComparisons(geneticDistances, indexedReas, index);
		
		// Build the REA type distance Matrix - summarises the genetic distance distributions for each REA type comparison
		int indexReaI;
		int indexReaJ;
		double[] distribution;
		double[][] reaDistances = new double[reaTypes.length][reaTypes.length];
		double value;
		
		for(int i = 0; i < reaTypes.length; i++){
			
			indexReaI = indexedReas.get(reaTypes[i]);
			
			for(int j = 0; j < reaTypes.length; j++){
				
				// Only compare REA types once and avoid self comparisons
				if(i >= j){
					continue;
				}
				
				indexReaJ = indexedReas.get(reaTypes[j]);
				
				// Get the genetic distance distribution for the current REA type comparison
				distribution = combine(comparisonDistributions[i][j], comparisonDistributions[j][i]);
				
				// Summarize Distribution
				value = ArrayMethods.mean(distribution);
				
				// Store the result in the REA distance Matrix
				reaDistances[indexReaI][indexReaJ] = value;
				reaDistances[indexReaI][indexReaJ] = value;				
			}
		}
		
		return new DistanceMatrix(reaTypes, reaDistances);		
	}
	
	public static double[] combine(double[] array1, double[] array2){
		double[] array = new double[array1.length + array2.length];
		for(int i = 0; i < array1.length; i++){
			array[i] = array1[i];
		}
		for(int i = 0; i < array2.length; i++){
			array[i + array1.length] = array2[i];
		}
		
		return array;
	}
	
}

