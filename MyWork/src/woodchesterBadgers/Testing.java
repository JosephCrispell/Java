package woodchesterBadgers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Hashtable;

import org.apache.commons.math3.random.MersenneTwister;

import methods.ArrayMethods;
import methods.GeneticMethods;
import methods.HashtableMethods;
import methods.RunCommand;
import methods.WriteToFile;

import filterSensitivity.DistanceMatrixMethods;
import filterSensitivity.Sequence;

public class Testing {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		// TODO Auto-generated method stub

		/**
		 * Get the Epidemiological Information:
		 * 		Group Spatial Data
		 * 		Badger Capture Histories
		 * 
		 * Generate:
		 * 		Group Adjacency Matrix
		 * 		Group Distance Matrix
		 */
		
		// Read in the Badger Group Location Information
		String territoryCentroidsFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/TerritoryCentroids.csv";
		Hashtable<String, double[]> territoryCentroids = StepwiseMatching.getTerritoryCentroids(territoryCentroidsFile, false);

		// Read in the Badger Trapping Data
		String consolidatedCaptureData = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/TrappingData/consolidatedWPData.txt";
		Hashtable<String, CaptureData> badgerCaptureHistories = CreateDescriptiveEpidemiologicalStats.readConsolidatedBadgerCaptureInfo(consolidatedCaptureData);
				
		// Get a list of all the badger groups
		Hashtable<String, Integer> badgerGroupIndices = CreateDescriptiveEpidemiologicalStats.findAllGroups(badgerCaptureHistories);
				
		// Create a Weighted Adjacency Matrix for Inhabited groups
		int[][] groupAdjacencyMatrix = CreateDescriptiveEpidemiologicalStats.createGroupAdjacencyMatrix(badgerGroupIndices, badgerCaptureHistories);
				
		// Create a distance matrix
		double[][] groupDistanceMatrix = CreateDescriptiveEpidemiologicalStats.createSpatialDistanceMatrix(badgerGroupIndices, territoryCentroids);
		
		/**
		 * Get the Isolate Information
		 * 		Sequence
		 * 		Sampling Information
		 * 
		 * Generate:
		 * 		List of WBIDs to link to tattoos
		 * 		List of Fasta Sequences linked to WBIDs	
		 */
		
		// Read the Sample Fasta Sequences
		String fastaFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/ExamineIsolateGeneticDistances/fasta_15-2-20-0.95-1.txt";
		Sequence[] sequences = DistanceMatrixMethods.readFastaFile(fastaFile);
		
		// Compare the Sampled Badgers to one another -> Genetic Distance vs. Epidemiological Distance table produced
		String[] wbIds = StepwiseMatching.getSampleIds(sequences);
				
		// Read in the Sample Information file
		String sampleInfoFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/sampleInformation_09-01-15.csv";
		Hashtable<String, SampleInfo> sampleInfo = CreateDescriptiveEpidemiologicalStats.getSampleInformation(sampleInfoFile);
		
		/**
		 * Note which Isolates we want to ignore:
		 * 		Poor Coverage			poorCoverage.txt
		 * 		Previous Outliers		outliers.txt
		 */
		
		String poorCoverageFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/ExamineIsolateGeneticDistances/poorCoverage.txt";
		String outliersFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/ExamineIsolateGeneticDistances/outliers.txt";
		Hashtable<String, Integer> ignore = noteWbIds2Ignore(poorCoverageFile, outliersFile);

		// Remove the unwanted isolates
		wbIds = removeIsolatesToBeIgnored(wbIds, ignore);
		
		/**
		 * Compare all isolates and create an Epidemiological Distance Table
		 */
		double[][] epiDistTable = buildEpiDistanceTable(wbIds, sampleInfo, badgerCaptureHistories, badgerGroupIndices, groupAdjacencyMatrix, groupDistanceMatrix);
		
		System.out.println("Finished Making Epidemiological Comparisons");
		
		/**
		 * Shuffle some of the Sequences associated with the WBIDs - record what is shuffled
		 * 	Randomly pick Sequence in the Sequence list and switch them - record the WBIDs that got switched
		 */
		
		// Remove the Sequences associated with the unwanted isolates
		sequences = removeIsolatesToBeIgnored(sequences, ignore);

		String analysisOut = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/ExamineIsolateGeneticDistances/ShufflingResults.txt";
		BufferedWriter bWriter = WriteToFile.openFile(analysisOut, false);
		//WriteToFile.writeLn(bWriter, "Proportion\tPseudoRSquared\tMSE\tBound95\tWBIDsShuffled\tOutliersFound");
		WriteToFile.writeLn(bWriter, "Proportion\tPseudoRSquared\tMSE\tWBIDsShuffled\tOutliersFound");
		
		// Initialise the necessary variables
		int noToShuffle = 10;
		int mtry = 14;
		int ntree = 500;
		String randomOutputFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/ExamineIsolateGeneticDistances/geneticVsEpiTable_Shuffled.txt";
		String rFile = "\"C:\\Users\\Joseph Crisp\\Desktop\\UbuntuSharedFolder\\Woodchester\\Tools\\FitRFModel_shufflingIds.R\"";
		String geneticVsEpiTableFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/ExamineIsolateGeneticDistances/geneticVsEpiTable_Shuffled_predicted.txt";
		int[] randomSequenceIndices;
		String[] rfOutput;
		
		for(int i = 0; i < 100; i++){
		
			System.out.println("Starting New analysis: " + i + "\t###################################################");
		
			randomSequenceIndices = randomIndices(sequences, 0, noToShuffle);
			Sequence[] shuffledSequences = shuffleChosenWbIds(sequences, randomSequenceIndices);
		
			/**
			 * Add the Inter-Isolate genetic distances to the Epi Distance Table to make GeneticVsEpiTable
			 */
			createGeneticVsEpiDistancesTableWithShuffledIds(shuffledSequences, epiDistTable, "\t", randomOutputFile);
			System.out.println("Shuffled Sequences and built Genetic Vs Epi Distance Table");
		
			/**
			 * Fit a Random Forest Model in R...
			 * 
			 */
			String result = fitRandomForestModel(rFile, mtry, ntree);
			System.out.println("Random Forest Fitting Finished." + "\n" + result);
		
			// PROBLEM with NAs!!!
		
			/**
			 * Find the poorly predicted isolates
			 */	
			// Read the Inter-Isolate comparison Distribution
			Hashtable<String, double[]> interIsolateDistribution = ExamineGeneticDistanceDistribution.readGeneticVsEpiTable(geneticVsEpiTableFile);
				
			// Get the Isolate Actual and Predicted Genetic Distances
			Hashtable<String, double[][]> isolateDistances = ExamineGeneticDistanceDistribution.getIsolateActualAndPredictedGeneticDistances(interIsolateDistribution);

			// Summarise the Genetic Distance Distributions for each Isolate
			Hashtable<String, double[]> isolateDistributionSummary = summariseIsolatePredictedVsActual(isolateDistances);
			System.out.println("Summarised Isolate Predicted Vs Actual Genetic Distance Distribution");
		
			// Find the 95% bound
			//double bound = getMeanDiff95PercentBound(isolateDistributionSummary);
		
			// Find outliers
			//String[] outliers = findOutliers(isolateDistributionSummary, bound);
			String[] outliers = sortWbIdsToFindOutliers(isolateDistributionSummary, noToShuffle);

			/**
			 * Compare the Outliers found to the shuffled wbIds - what proportion were recovered?
			 */
			Hashtable<String, Integer> shuffledWbIds = getShuffledWBIds(randomSequenceIndices, wbIds);
			double proportion = findProportionShuffledWbIdsRecovered(shuffledWbIds, outliers);
			System.out.println("Found Outliers. Proportion WBIDs Recovered = " + proportion);
		
			// Store the Results
			rfOutput = result.split("\t");
			WriteToFile.writeLn(bWriter, proportion + "\t" + rfOutput[0].substring(0, 8) + "\t" + rfOutput[1].substring(0, 8) + "\t" + ArrayMethods.toString(HashtableMethods.getKeysString(shuffledWbIds), ":") + "\t" + ArrayMethods.toString(outliers, ":"));
			
		}
		
		WriteToFile.close(bWriter);
		
	}
	
	public static double findProportionShuffledWbIdsRecovered(Hashtable<String, Integer> shuffledWbIds, String[] outliers){
		
		double proportion = 0;
		for(String outlier : outliers){
			if(shuffledWbIds.get(outlier) != null){
				proportion++;
			}
		}
		
		return proportion / (double) shuffledWbIds.size();
	}
	
	public static Hashtable<String, Integer> getShuffledWBIds(int[] indices, String[] wbIds){
		
		Hashtable<String, Integer> shuffled = new Hashtable<String, Integer>();
		
		for(int i = 0; i < indices.length; i++){
			shuffled.put(wbIds[indices[i]], 1);
		}
		
		return shuffled;
	}
	
	public static Hashtable<String, double[]> summariseIsolatePredictedVsActual(Hashtable<String, double[][]> isolateDistances) throws IOException{
		
		/**
		 *  Examine the Isolate Actual and Predicted Genetic Distances:
		 *  WBID	ActualMean	ActualMin	ActualMax 	PredictedMean	PredictedMin	PredictedMax	SumDiff	MeanDiff
		 *  		0			1			2			3				4				5				6		7
		 */

		Hashtable<String, double[]> isolateDistributionSummary = new Hashtable<String, double[]>();
		
		// Initialise variables to store summary statistics
		double[] results = new double[8];
		double difference;
		double[][] distribution;
		String results4Output;
		
		// Examine each of the Isolates - identified by their WBID
		String[] keys = HashtableMethods.getKeysString(isolateDistances);
		for(String key : keys){
			
			// Get the Isolates Actual and Predicted Genetic Distributions
			distribution = isolateDistances.get(key);
			
			// Reset vector to store statistics
			results = new double[8];
			results[1] = 10; // For Actual Min
			results[4] = 10; // For Predicted Min
			
			// Summarise Isolate's Actual and Predicted Genetic Distance Distributions
			for(int i = 0; i < distribution[0].length; i++){
				
				// Actual Mean
				results[0] += distribution[0][i];
				
				// Actual Min
				if(distribution[0][i] < results[1]){
					results[1] = distribution[0][i];
				}
				
				// Actual Max
				if(distribution[0][i] > results[2]){
					results[2] = distribution[0][i];
				}
				
				// Predicted Mean
				results[3] += distribution[1][i];
				
				// Predicted Min
				if(distribution[1][i] < results[4]){
					results[4] = distribution[1][i];
				}
				
				// Predicted Max
				if(distribution[1][i] > results[5]){
					results[5] = distribution[1][i];
				}
				
				// Sum of Difference
				difference = Math.abs(distribution[0][i] - distribution[1][i]);
				results[6] += difference;
				
			}
			
			// Actual Mean
			results[0] = results[0] / (double) distribution[0].length;
			
			// Predicted Mean
			results[3] = results[3] / (double) distribution[0].length;
			
			// Mean Difference
			results[7] = results[6] / (double) distribution[0].length;
			
			// Store the Isolates Summary Information
			isolateDistributionSummary.put(key, results);
			
		}
		
		return isolateDistributionSummary;
	}

	public static double[] getMeanDiffDistribution(Hashtable<String, double[]> isolateDistributionSummary){
		// Initialise an array to store the distribution
		double[] distribution = new double[isolateDistributionSummary.size()];
					
		// Get the Wbids
		String[] keys = HashtableMethods.getKeysString(isolateDistributionSummary);
					
		// Get the Mean Diff associated with each WBID
		for(int i = 0; i < keys.length; i++){
			
			distribution[i] = isolateDistributionSummary.get(keys[i])[7];			
		}
		
		return distribution;
	}
	
	public static String[] sortWbIdsToFindOutliers(Hashtable<String, double[]> isolateDistributionSummary, int noShuffled){
		
		// Get the WBIDs and their associated mean diff value
		String[] wbIds = HashtableMethods.getKeysString(isolateDistributionSummary);
		double[] meanDiffs = getMeanDiffDistribution(isolateDistributionSummary);
		
		// Create a copy the WBIDs and their mean diffs to sort
		double[] srtdMeanDiffs = ArrayMethods.copy(meanDiffs);
		String[] srtdWbIds = ArrayMethods.copy(wbIds);
		
		/**
		 * This Method Uses the Bubble Sort Algorithm
		 * 		Described here: http://en.wikipedia.org/wiki/Bubble_sort
		 * 
		 * 	For each element, compare it to the next element. If it is larger than the next element, swap the elements.
		 * 	Do this for each element of the list (except the last). Continue to iterate through the list elements and
		 *  make swaps until no swaps can be made.
		 */
		
		double a;
		double b;
		String aa;
		String bb;
		
		int swappedHappened = 1;
		while(swappedHappened == 1){ // Continue to compare the List elements until no swaps are made
		
			int swapped = 0;
			for(int index = 0; index < wbIds.length - 1; index++){
				
				// Compare Current Element to Next Element
				if(srtdMeanDiffs[index] > srtdMeanDiffs[index + 1]){
					
					// Swap Current Element is Larger - note that we are swapping the WBIDs as well
					a = srtdMeanDiffs[index];
					b = srtdMeanDiffs[index + 1];
					
					aa = srtdWbIds[index];
					bb = srtdWbIds[index + 1];
					
					srtdMeanDiffs[index] = b;
					srtdMeanDiffs[index + 1] = a;
					
					srtdWbIds[index] = bb;
					srtdWbIds[index + 1] = aa;
					
					// Record that a Swap occurred
					swapped++;
				}
			}
			
			// Check if any swaps happened during the last iteration - if none then finished
			if(swapped == 0){
				swappedHappened = 0;
			}
		}
		
		// Select the last x (no shuffled) wbIds from the sorted list - these are the "outliers"
		return ArrayMethods.subset(srtdWbIds, srtdWbIds.length - noShuffled, srtdWbIds.length -1 );
	}
	
	public static String[] findOutliers(Hashtable<String, double[]> isolateDistributionSummary, double bound){
		/**
		 *  Examine the Isolate Actual and Predicted Genetic Distances:
		 *  WBID	ActualMean	ActualMin	ActualMax 	PredictedMean	PredictedMin	PredictedMax	SumDiff	MeanDiff
		 *  		0			1			2			3				4				5				6		7
		 */
		
		// Initialise an array to store the WBIDs of the outliers
		String[] outliers = new String[1000];
		int index = -1;
		
		// Get the Wbids
		String[] keys = HashtableMethods.getKeysString(isolateDistributionSummary);
				
		// Get the Mean Diff associated with each WBID
		for(int i = 0; i < keys.length; i++){
			
			if(isolateDistributionSummary.get(keys[i])[7] > bound){
		
				index++;
				outliers[index] = keys[i];
			}
		}
		
		return ArrayMethods.subset(outliers, 0, index);
	}
	
	public static double getMeanDiff95PercentBound(Hashtable<String, double[]> isolateDistributionSummary){
		
		// Initialise an array to store the distribution
		double[] distribution = new double[isolateDistributionSummary.size()];
		
		// Get the Wbids
		String[] keys = HashtableMethods.getKeysString(isolateDistributionSummary);
		
		// Get the Mean Diff associated with each WBID
		for(int i = 0; i < keys.length; i++){
			
			distribution[i] = isolateDistributionSummary.get(keys[i])[7];			
		}
		
		// Sort the MeanDiff Distribution
		double[] sorted = ArrayMethods.sort(distribution);
		
		// Find the 95% bound index
		double index = 0.95 * (double) sorted.length;
		
		return sorted[(int) index];
	}
	
	public static String fitRandomForestModel(String rFile, int mtry, int ntree) throws IOException{
		RunCommand result = new RunCommand("Rscript " + rFile + " " + mtry + " " + ntree);
		
		return result.getOutput();// + "\n" + result.getError();
	}
	
	public static void createGeneticVsEpiDistancesTableWithShuffledIds(Sequence[] shuffledSequences,
			double[][] epiDistTable, String sep, String outputFile) throws IOException{
		
		// Open the output file for the genetic vs. epi distance table to written to
		BufferedWriter bWriter = WriteToFile.openFile(outputFile, false);
			
		// Initialise Strings to store output information
		String header = "Genetic\tSameMainGroup\tSameSampledGroup\tSameGroupWhenInfected\tPeriodAliveTogether\tPeriodInfectedTogether\tPeriodInSameGroup\t";
		header += "TimeBetweenDetection\tTimeBetweenSamplin\tDistMainGroups\tDistSampledGroup\tDistGroupWhenInfected\tnoMovementsMainGroups\t";
		header += "noMovementsSampledGroup\tnoMovementsGroupsWhenInfected\tFROM\tTO";
				
		WriteToFile.writeLn(bWriter, header);
		
		// The first column of the epiDistTable has been left blank to be filled with the genetic distances
		int row = -1;
		String line;
		for(int i = 0; i < shuffledSequences.length; i++){
			
			for(int j = 0; j < shuffledSequences.length; j++){
				
				// Only make the comparison once and avoid self comparisons
				if(i >= j){
					continue;
				}
				
				row++;
				
				// Find the genetic Distance between the isolates taken from Badger I and J
				epiDistTable[row][0] = GeneticMethods.calculateNumberDifferencesBetweenSequences(shuffledSequences[i].getSequence(), shuffledSequences[j].getSequence());
				
				// Print the Current row in the Table out to File
				line = ArrayMethods.toString(epiDistTable[row], sep);
				line += "\t" + shuffledSequences[i].getSampleName() + "\t" + shuffledSequences[j].getSampleName();
				WriteToFile.writeLn(bWriter, line);
			}
		}
		
		WriteToFile.close(bWriter);
	}
	
	public static Sequence[] copy(Sequence[] array){
		Sequence[] copy = new Sequence[array.length];
		
		for(int index = 0; index < array.length; index++){
			copy[index] = array[index];
		}
		
		return copy;
	}
	
	public static int[] randomIndices(Sequence[] array, int replacement, int n){
		
		// Create an array of indices
		int[] indices = ArrayMethods.range(0, array.length -1, 1);
		
		// Create Instance of a Random Number Generator
		MersenneTwister random = new MersenneTwister();
		
		// Initialise an array to store the randomly chosen indices
		int[] chosen = new int[n];
		
		// Initialise a Hashtable to record indices chosen
		Hashtable<Integer, Integer> removed = new Hashtable<Integer, Integer>();
		
		// Random pick n indices
		int index;
		int i = -1;
		while(i < (n - 1)){
			
			index = random.nextInt(indices.length - 1);
			
			if(removed.get(index) == null){
				i++;
				
				chosen[i] = index;
				
				if(replacement == 0){
					removed.put(index, 1);
				}
			}
		}
		
		return chosen;
	}
	
	public static Sequence[] shuffleChosenWbIds(Sequence[] sequences, int[] indices){
		
		Sequence[] shuffled = copy(sequences);
		
		Sequence a;
		Sequence b;
		
		for(int i = 0; i < indices.length; i = i + 2){
			
			a = shuffled[indices[i]];
			b = shuffled[indices[i + 1]];
			
			shuffled[indices[i]] = b;
			shuffled[indices[i + 1]] = a;						
		}
		
		return shuffled;
	}
	
	public static Sequence[] removeIsolatesToBeIgnored(Sequence[] sequences, Hashtable<String, Integer> ignore){
		
		Sequence[] newSequences = new Sequence[sequences.length - ignore.size()];
		
		int i = -1;
		for(Sequence sequence : sequences){
			
			if(ignore.get(sequence.getSampleName()) != null){
				continue;
			}
			
			i++;
			newSequences[i] = sequence;
		}
		
		return newSequences;
	}
	
	public static String[] removeIsolatesToBeIgnored(String[] wbIds, Hashtable<String, Integer> ignore){
		
		String[] newWbIds = new String[wbIds.length - ignore.size()];
		
		int i = -1;
		for(String wbId : wbIds){
			
			if(ignore.get(wbId) != null){
				continue;
			}
			
			i++;
			newWbIds[i] = wbId;
		}
		
		return newWbIds;
	}
	
	public static Hashtable<String, Integer> noteWbIds2Ignore(String poorCoverageFile, String outliersFile) throws IOException{
		
		// Open the Sample Information File
		InputStream input = new FileInputStream(poorCoverageFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
								
		// Initialise a Hashtable to store the Sample Information
		Hashtable<String, Integer> ignore = new Hashtable<String, Integer>();
								
		// Initialise Variables for processing the file lines
		String line = "";
		String[] parts;
		
		String wbId;
								
		// Reader the file
		while(( line = reader.readLine()) != null){
			
			wbId = line.split(" ")[0];
			if(wbId.matches("WB(.*)")){
				ignore.put(wbId, 1);
			}
		}
				
		input.close();
		reader.close();
		
		// Open the Sample Information File
		input = new FileInputStream(outliersFile);
		reader = new BufferedReader(new InputStreamReader(input));
										
		// Reader the file
		while(( line = reader.readLine()) != null){
					
			wbId = line.split("\t")[0];
					
			if(wbId.matches("WB(.*)")){
				ignore.put(wbId, 1);
			}
		}
						
		input.close();
		reader.close();
		
		return ignore;
	}
	
	public static double[][] buildEpiDistanceTable(String[] wbIds, Hashtable<String, SampleInfo> sampleInfo,
			Hashtable<String, CaptureData> badgerCaptureHistories, Hashtable<String, Integer> badgerGroupIndices,
			int[][] groupAdjacencyMatrix, double[][] groupDistanceMatrix) throws IOException{
		
		// Initialise variables available Badger Information
		SampleInfo sampleInfoI;
		SampleInfo sampleInfoJ;
		CaptureData captureHistoryI;
		CaptureData captureHistoryJ;
		long[] captureDatesMilliI;
		long[] captureDatesMilliJ;
		
		/**
		 * Create a Matrix to store the results of the epidemiological comparison:
		 * 
		 * 	Empty	SameMainGrp	SameSampGrp	SameInfGrp	PerAliveToge	PerInfToge	PerInSameGrp	timeBetInf
		 * 	0		1			2			3			4				5			6				7
		 * 
		 * 	timeBetSamp	DistMainGrp	DistInfGrp	DistSampGrp	MovementsMainGrp	MovementsInfGrp	MovementsSampGrp
		 * 	8			9			10			11			12					13				14
		 */
		
		// Calculate the number of comparisons to be made
		double n = wbIds.length;
		double nComparisons = (n * (n - 1))/2;
	
		double[][] epiDistTable = new double[(int)nComparisons][15];
		
		// Compare the Isolates
		int row = -1;
		for(int i = 0; i < wbIds.length; i++){
			
			// Get the Sample Information for Badger I
			sampleInfoI = sampleInfo.get(wbIds[i]);
			
			// Get the Capture History for Badger I
			captureHistoryI = badgerCaptureHistories.get(sampleInfoI.getTattoo());
			
			captureDatesMilliI = new long[0];
			if(captureHistoryI != null){
				captureDatesMilliI = captureHistoryI.getDatesInMilliSeconds();
			}
			
			for(int j = 0; j < wbIds.length; j++){
				
				// Only compare isolates once and don't do self comparisons
				if(i >= j){
					continue;
				}
				
				// Get the Sample Information for Badger J
				sampleInfoJ = sampleInfo.get(wbIds[j]);
				
				// Get the Capture History for Badger J
				captureHistoryJ = badgerCaptureHistories.get(sampleInfoJ.getTattoo());
				
				captureDatesMilliJ = new long[0];
				if(captureHistoryJ != null){
					captureDatesMilliJ = captureHistoryJ.getDatesInMilliSeconds();
				}
				
				row++;
								
				// Same Group? Main, Sampled, Infected
				epiDistTable[row][1] = CreateDescriptiveEpidemiologicalStats.checkIfInSameGroup(captureHistoryI.getMainGroup(), captureHistoryJ.getMainGroup());
				epiDistTable[row][2] = CreateDescriptiveEpidemiologicalStats.checkIfInSameGroup(sampleInfoI.getBadgerGroup(), sampleInfoJ.getBadgerGroup());
				epiDistTable[row][3] = CreateDescriptiveEpidemiologicalStats.checkIfInSameGroup(captureHistoryI.getGroupWhenFirstInfected(), captureHistoryJ.getGroupWhenFirstInfected());
				
				// Time Spent Together in Groups: Main, Sampled, Infected
				epiDistTable[row][4] = CreateDescriptiveEpidemiologicalStats.findTimeTogether(captureDatesMilliI[0], captureDatesMilliI[captureDatesMilliI.length - 1], captureDatesMilliJ[0], captureDatesMilliJ[captureDatesMilliJ.length - 1]);
				epiDistTable[row][5] = CreateDescriptiveEpidemiologicalStats.findTimeTogether(captureDatesMilliI[captureHistoryI.getWhenInfectionDetected()], captureDatesMilliI[captureDatesMilliI.length - 1], captureDatesMilliJ[captureHistoryJ.getWhenInfectionDetected()], captureDatesMilliJ[captureDatesMilliJ.length - 1]);
				epiDistTable[row][6] = CreateDescriptiveEpidemiologicalStats.findPeriodSpentInSameGroup(captureHistoryI.getPeriodsInEachGroup(), captureHistoryJ.getPeriodsInEachGroup());
				
				// Temporal Distances between Events: Sampled, Infected
				epiDistTable[row][7] = StepwiseMatching.calculateTempDistance(sampleInfoI.getDate(), sampleInfoJ.getDate());
				epiDistTable[row][8] = Math.abs(captureDatesMilliI[captureHistoryI.getWhenInfectionDetected()] - captureDatesMilliJ[captureHistoryJ.getWhenInfectionDetected()]) / (24 * 60 * 60 * 1000);
				
				// Network Distance between Groups: Main, Sampled, Infected
				epiDistTable[row][9] = groupDistanceMatrix[badgerGroupIndices.get(captureHistoryI.getMainGroup())][badgerGroupIndices.get(captureHistoryJ.getMainGroup())];
				epiDistTable[row][10] = groupDistanceMatrix[badgerGroupIndices.get(captureHistoryI.getGroupWhenFirstInfected())][badgerGroupIndices.get(captureHistoryJ.getGroupWhenFirstInfected())];
				epiDistTable[row][11] = groupDistanceMatrix[badgerGroupIndices.get(sampleInfoI.getBadgerGroup())][badgerGroupIndices.get(sampleInfoJ.getBadgerGroup())];
				
				// Spatial Distance between Groups: Main, Sampled, Infected
				epiDistTable[row][12] = CreateDescriptiveEpidemiologicalStats.getNoMovementsBetweenGroups(captureHistoryI.getMainGroup(), captureHistoryJ.getMainGroup(), badgerGroupIndices, groupAdjacencyMatrix);
				epiDistTable[row][13] = CreateDescriptiveEpidemiologicalStats.getNoMovementsBetweenGroups(captureHistoryI.getGroupWhenFirstInfected(), captureHistoryJ.getGroupWhenFirstInfected(), badgerGroupIndices, groupAdjacencyMatrix);
				epiDistTable[row][14] = CreateDescriptiveEpidemiologicalStats.getNoMovementsBetweenGroups(sampleInfoI.getBadgerGroup(), sampleInfoJ.getBadgerGroup(), badgerGroupIndices, groupAdjacencyMatrix);
			}
		}

		return epiDistTable;
	}

}
