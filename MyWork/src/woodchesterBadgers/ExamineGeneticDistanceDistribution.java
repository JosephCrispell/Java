package woodchesterBadgers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.HashtableMethods;
import methods.MatrixMethods;
import methods.WriteToFile;

public class ExamineGeneticDistanceDistribution {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		// All Isolates
		
		// Read the Inter-Isolate comparison Distribution
		//String geneticVsEpiTableFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/ExamineIdShuffling/geneticVsEpiTable_08-04-15_ALL_predicted.txt";
		//String geneticVsEpiTableFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/ExamineIdShuffling/geneticVsEpiTable_03-02-15_ALL_predicted_Shuffled.txt";		
		String geneticVsEpiTableFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/TimeDependence_19-05-15/geneticVsEpiTable_IgnoreTimePeriod_predicted.txt";
		Hashtable<String, double[]> interIsolateDistribution = readGeneticVsEpiTable(geneticVsEpiTableFile);
		
		// Get the Isolate Actual and Predicted Genetic Distances
		Hashtable<String, double[][]> isolateDistances = getIsolateActualAndPredictedGeneticDistances(interIsolateDistribution);

		// Summarise the Genetic Distance Distributions for each Isolate - and print it out
		String outFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/TimeDependence_19-05-15/isolateDistributionSummaries_IgnoreTimePeriod.txt";
		Hashtable<String, double[]> isolateDistributionSummary = printSummaryOfIsolateGeneticDistanceDistribution(isolateDistances, 1, outFile);	
	}
	
	
	
	public static Hashtable<String, double[]> printSummaryOfIsolateGeneticDistanceDistribution(Hashtable<String, double[][]> isolateDistances, int print, String fileName) throws IOException{
	
		/**
		 *  Examine the Isolate Actual and Predicted Genetic Distances:
		 *  WBID	ActualMean	ActualMin	ActualMax 	PredictedMean	PredictedMin	PredictedMax	SumDiff	MeanDiff
		 *  		0			1			2			3				4				5				6		7
		 */

		Hashtable<String, double[]> isolateDistributionSummary = new Hashtable<String, double[]>();
		
		// Initialise output file to print summaries to
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		if(print == 1){
			WriteToFile.writeLn(bWriter, "WBID\tActualMean\tActualMin\tActualMax\tPredictedMean\tPredictedMin\tPredictedMax\tSumDiff\tMeanDiff");
		}
		
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
			
			// Print out the Isolate's Summary if wanted
			if(print == 1){
				
				results4Output = ArrayMethods.toString(results, "\t");
				WriteToFile.writeLn(bWriter, key + "\t" + results4Output);				
			}
		}
		
		// Close the Output File
		bWriter.close();
		return isolateDistributionSummary;
	}
	
	public static Hashtable<String, double[][]> getIsolateActualAndPredictedGeneticDistances(Hashtable<String, double[]> interIsolateDistribution){
		
		// Get the actual and predicted Genetic Distance distribution for each isolate
		Hashtable<String, double[][]> isolateActualAndPredictedGeneticDistances = new Hashtable<String, double[][]>();
		String[] keys = HashtableMethods.getKeysString(interIsolateDistribution);
		String[] wbIds;
		double[] distribution;
		double[][] actualAndPredicted;
		double[] values = new double[2];
				
		// Examine each Inter-isolate comparison
		for(String key : keys){
				
			wbIds = key.split(":");
				
			// Get the actual and predicted genetic distances for the current inter-isolate comparison
			distribution = interIsolateDistribution.get(key);
			values[0] = distribution[0]; // Actual Genetic Distance
			values[1] = distribution[15]; // Predicted Genetic Distance
					
			// Store the actual and predicted values for each isolate
			if(isolateActualAndPredictedGeneticDistances.get(wbIds[0]) == null){// FROM
				
				// Initialise a new matrix to store actual and predicted values
				actualAndPredicted = new double[2][1];
				actualAndPredicted[0][0] = values[0];
				actualAndPredicted[1][0] = values[1];
				
				// Store the matrix for the current isolate
				isolateActualAndPredictedGeneticDistances.put(wbIds[0], actualAndPredicted);
			}else{
				
				// Get the actual and predicted values for the current isolate
				actualAndPredicted = isolateActualAndPredictedGeneticDistances.get(wbIds[0]);
				
				// Add in the new values
				actualAndPredicted = MatrixMethods.addCol(actualAndPredicted,values);
				
				// Store the matrix for the current isolate
				isolateActualAndPredictedGeneticDistances.put(wbIds[0], actualAndPredicted);
			}
			
			if(isolateActualAndPredictedGeneticDistances.get(wbIds[1]) == null){// TO
				
				// Initialise a new matrix to store actual and predicted values
				actualAndPredicted = new double[2][1];
				actualAndPredicted[0][0] = values[0];
				actualAndPredicted[1][0] = values[1];
				
				// Store the matrix for the current isolate				
				isolateActualAndPredictedGeneticDistances.put(wbIds[1], actualAndPredicted);
			}else{
				
				// Get the actual and predicted values for the current isolate
				actualAndPredicted = isolateActualAndPredictedGeneticDistances.get(wbIds[1]);
				
				// Add in the new values
				actualAndPredicted = MatrixMethods.addCol(actualAndPredicted,values);
				
				// Store the matrix for the current isolate
				isolateActualAndPredictedGeneticDistances.put(wbIds[1], actualAndPredicted);
			}
		}
		
		return isolateActualAndPredictedGeneticDistances;
	}
	
	public static Hashtable<String, double[]> readGeneticVsEpiTable(String filename) throws IOException{
		
		/**
		 * Genetic	SameMainGroup	SameSampledGroup	SameGroupWhenInfected	PeriodAliveTogether	PeriodInfectedTogether	PeriodInSameGroup
		 * 0		1				2					3						4					5						6
		 * 
		 * TimeBetweenDetection	TimeBetweenSamplin	DistMainGroups	DistSampledGroup	DistGroupWhenInfected	noMovementsMainGroups
		 * 7					8					9				10					11						12
		 * 
		 * noMovementsSampledGroup	noMovementsGroupsWhenInfected	predicted	FROM	TO
		 * 13						14								15			16		17
		 * 
		 */
		
		// Open the Table file
		InputStream input = new FileInputStream(filename);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
								
		// Initialise Variables for processing the file lines
		String line = "";
		String[] parts;
		double[] values;
				
		// The table is stored in Hashtable with the FROM:TO as a key
		Hashtable<String, double[]> interIsolateDistribution = new Hashtable<String, double[]>();
		String key;
				
		// Reader the file
		while(( line = reader.readLine()) != null){
					
			// Skip the Header
			if(line.matches("Genetic(.*)")){
				continue;
			}
					
			// Parse the table row
			parts = line.split("\t");
				
			// Get the FROM:TO key
			key = parts[16] + ":" + parts[17];
			
			// Convert the row into double[]
			values = ArrayMethods.convert2Double(ArrayMethods.subset(parts, 0, 15));
			
			// Store the inter-isolate comparison values
			interIsolateDistribution.put(key, values);
		}
				
		reader.close();
		input.close();
		
		return interIsolateDistribution;
	}

}
