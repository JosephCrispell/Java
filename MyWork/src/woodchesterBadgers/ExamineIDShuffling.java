package woodchesterBadgers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Hashtable;

import org.apache.commons.math3.random.MersenneTwister;

import methods.ArrayMethods;
import methods.HashtableMethods;

import filterSensitivity.DistanceMatrixMethods;
import filterSensitivity.Sequence;

public class ExamineIDShuffling {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		// Read in the Sample Information file
		String sampleInfoFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/sampleInformation_08-04-15.csv";
		Hashtable<String, SampleInfo> sampleInfo = CreateDescriptiveEpidemiologicalStats.getSampleInformation(sampleInfoFile);

		// Read in the Badger Group Location Information
		String territoryCentroidsFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/TerritoryCentroids.csv";
		Hashtable<String, double[]> territoryCentroids = StepwiseMatching.getTerritoryCentroids(territoryCentroidsFile);

		// Read the Sample Fasta Sequences
		String fastaFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/ExamineIdShuffling/sequences.fasta";
		Sequence[] sequences = readFastaFile(fastaFile);
		
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
		String[] wbIds = getSampleIds(sequences);
		
		// Find WBIDs we wish to ignore
		String poorCoverageFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/ExamineIdShuffling/poorCoverage.txt";
		Hashtable<String, Integer> ignore = CreateDescriptiveEpidemiologicalStats.findWBidsToIgnore(poorCoverageFile);
		ignore = selectBasedOnSpoligotype(ignore, sampleInfo, "263");
		ignore = selectIsolatesThatWereCapturedMultipleTimes(ignore, badgerCaptureHistories, sequences, sampleInfo, 2);
		
		// Shuffle the isolate sequences
		//sequences = shuffle(sequences);
		
		// Generate the epidemiological metrics describing each badger comparison
		String outputFileName = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/ExamineIdShuffling/geneticVsEpiTable_01-05-15.txt";
		CreateDescriptiveEpidemiologicalStats.buildGeneticVsEpiDistanceTable(wbIds, sequences, sampleInfo, badgerCaptureHistories, badgerGroupIndices, groupAdjacencyMatrix, groupDistanceMatrix, outputFileName, ignore);
		
	}
	
	public static Hashtable<String, Integer> selectIsolatesThatWereCapturedMultipleTimes(Hashtable<String, Integer> ignore,
			Hashtable<String, CaptureData> badgerCaptureHistories, Sequence[] sequences,
			Hashtable<String, SampleInfo> info, int times){
		
		/**
		 *  Examine each of the isolates and determine whether the badger they relate to was captured enough times
		 *  to be considered in further analysis.
		 */
		
		String wbId;
		String tattoo;
		CaptureData captureHistory;
		
		for(Sequence sequence : sequences){
			
			// Get the WBID, sequence identified by file name: WB98_S53_93.vcf
			wbId = sequence.getSampleName().split("_")[0];
			
			// Get the tattoo for the badger associated with the current WBID
			tattoo = info.get(wbId).getTattoo();
			
			// Get the Capture history for the current badger
			captureHistory = badgerCaptureHistories.get(tattoo);
			
			// Check if the current badger was captured enough times
			if(captureHistory.getGroupsInhabited().length < times){
				ignore.put(wbId, 1);
			}
		}		
		
		return ignore;
	}
	
	public static Hashtable<String, Integer> selectBasedOnSpoligotype(Hashtable<String, Integer> ignore, Hashtable<String, SampleInfo> isolateInfo, String spoligotype){
		
		String[] wbIds = HashtableMethods.getKeysString(isolateInfo);
		SampleInfo info;
		
		// Examine each of the WBIDs and their information and check whether they have the spoligotype of interest
		for(String wbId : wbIds){
			
			// Get the sample information for the current WBID
			info = isolateInfo.get(wbId);
			
			if(info.getSpoligotype().equals(spoligotype) == false){
				ignore.put(wbId, 1);
			}
			
		}
		
		return ignore;
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
	
	public static String[] getSampleIds(Sequence[] sequences){
		String[] sampleIds = new String[sequences.length];
		
		for(int i = 0; i < sequences.length; i++){
			sampleIds[i] = sequences[i].getSampleName().split("_")[0];
		}
		
		return sampleIds;
	}
	
	public static Sequence[] shuffle(Sequence[] array) {
		
		// Copy the Array of Sequences
		Sequence[] arrayCopy = copy(array);
		
		// Initialise a Random Number Generator
		MersenneTwister random = new MersenneTwister();
		
		// Shuffle the Sequences
		for (int i = 0; i < array.length; i++) {
		    
			// Get a random index of the array past i.
		    int randomIndex = i + (int) (random.nextDouble() * (array.length - i));
		    
		    // Swap the random element with the present element.
		    char[] tempSequence = arrayCopy[randomIndex].getSequence();
		    
		    arrayCopy[randomIndex].setSequence(arrayCopy[i].getSequence());
		    arrayCopy[i].setSequence(tempSequence);
		}
		
		return array;
	}
		
	public static Sequence[] copy(Sequence[] array){
		Sequence[] copy = new Sequence[array.length];
		
		for(int index = 0; index < array.length; index++){
			copy[index] = array[index];
		}
		
		return copy;
	}
}
