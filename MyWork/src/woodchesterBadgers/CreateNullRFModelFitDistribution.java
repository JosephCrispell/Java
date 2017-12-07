package woodchesterBadgers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;

import methods.RunCommand;
import methods.WriteToFile;

import filterSensitivity.Sequence;

public class CreateNullRFModelFitDistribution {

	public static void main(String[] args) throws IOException {
		
		// Read in the Sample Information file
		String sampleInfoFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/sampleInformation_08-04-15.csv";
		Hashtable<String, SampleInfo> sampleInfo = CreateDescriptiveEpidemiologicalStats.getSampleInformation(sampleInfoFile);
		
		// Read in the Badger Group Location Information
		String territoryCentroidsFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/TerritoryCentroids.csv";
		Hashtable<String, double[]> territoryCentroids = StepwiseMatching.getTerritoryCentroids(territoryCentroidsFile, false);

		// Read the Sample Fasta Sequences
		String fastaFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/ExamineIdShuffling/sequences.fasta";
		Sequence[] sequences = ExamineIDShuffling.readFastaFile(fastaFile);
				
		// Read in the Badger Trapping Data
		String consolidatedCaptureData = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/TrappingData/consolidatedWPData.txt";
		Hashtable<String, CaptureData> badgerCaptureHistories = CreateDescriptiveEpidemiologicalStats.readConsolidatedBadgerCaptureInfo(consolidatedCaptureData);
				
		// Get a list of all the badger groups
		Hashtable<String, Integer> badgerGroupIndices = CreateDescriptiveEpidemiologicalStats.findAllGroups(badgerCaptureHistories);
		
		// Create a Weighted Adjacency Matrix for Inhabited groups
		int[][] groupAdjacencyMatrix = CreateDescriptiveEpidemiologicalStats.createGroupAdjacencyMatrix(badgerGroupIndices, badgerCaptureHistories);
		
		// Create a distance matrix
		double[][] groupDistanceMatrix = CreateDescriptiveEpidemiologicalStats.createSpatialDistanceMatrix(badgerGroupIndices, territoryCentroids);
		
		// Get the WBIDs for the badgers of interest
		String[] wbIds = ExamineIDShuffling.getSampleIds(sequences);
		
		// Find WBIDs we wish to ignore
		String poorCoverageFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/ExamineIdShuffling/poorCoverage.txt";
		Hashtable<String, Integer> ignore = CreateDescriptiveEpidemiologicalStats.findWBidsToIgnore(poorCoverageFile);
		ignore = ExamineIDShuffling.selectBasedOnSpoligotype(ignore, sampleInfo, "263");
		ignore = ExamineIDShuffling.selectIsolatesThatWereCapturedMultipleTimes(ignore, badgerCaptureHistories, sequences, sampleInfo, 2);
		
		// Shuffle the Sequences and record the observed difference in the Model Fit in R
		String geneticVsEpiTable = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/ExamineIdShuffling/geneticVsEpiTable_01-05-15_Shuffled.txt";
		String rTable = "\"C:\\Users\\Joseph Crisp\\Desktop\\UbuntuSharedFolder\\Woodchester\\ExamineIdShuffling\\geneticVsEpiTable_01-05-15_Shuffled.txt\"";
		String rFile = "\"C:\\Users\\Joseph Crisp\\Desktop\\UbuntuSharedFolder\\Woodchester\\ExamineIdShuffling\\FitRFModelToShuffled.R\"";
		
		// Open an Output File
		String outputFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/ExamineIdShuffling/shufflingRFModelOutput_01-05-15.txt";
		BufferedWriter bWriter = WriteToFile.openFile(outputFile, false);
		WriteToFile.writeLn(bWriter, "PseudoRSquared\tOOBError");
		
		// Begin the Shuffling
		Sequence[] shuffled = new Sequence[sequences.length];
		String output = "";
		Date date = new Date();
		int n = 1000;
		for(int i = 0; i < n; i++){
			
			// Track Progress
			date = new Date();
	       	System.out.println("Starting Shuffling event " + (i + 1) + " of " + n + "\t\t" + date.toString().split(" ")[3]);
			
			// Shuffle the Sequences
			shuffled = ExamineIDShuffling.shuffle(sequences);
			
			// Build a Genetic Vs. Epidemiological Distances Matrix
			CreateDescriptiveEpidemiologicalStats.buildGeneticVsEpiDistanceTable(wbIds, shuffled, sampleInfo, badgerCaptureHistories, badgerGroupIndices, groupAdjacencyMatrix, groupDistanceMatrix, geneticVsEpiTable, ignore);
			
			// Fit the Random Forest Model
			output = fitRandomForestModel(rFile, rTable);
			
			// Store the Model Output
			storeRFOutput(output, bWriter);
		}
		
		// Close the output File
		WriteToFile.close(bWriter);
	}
	
	public static void storeRFOutput(String output, BufferedWriter bWriter) throws IOException{
		
		// The tuneRF automatically produces some command line output and this needs to be ignored
		String[] parts = output.split("\n");
		String[] values = parts[parts.length - 1].split("\t");
		
		WriteToFile.writeLn(bWriter, values[0] + "\t" + values[1]);
		
	}
	
	public static String fitRandomForestModel(String rFile, String table) throws IOException{
		RunCommand result = new RunCommand("Rscript " + rFile + " " + table);
		
		return result.getOutput();
	}

}
