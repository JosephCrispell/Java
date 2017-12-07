package woodchesterGeneticVsEpi;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Hashtable;

import geneticDistances.GeneticDistances;
import geneticDistances.Sequence;
import methods.CalendarMethods;
import methods.GeneralMethods;
import methods.GeneticMethods;
import methods.HashtableMethods;
import methods.WriteToFile;
import woodchesterBadgers.CapturedBadgerLifeHistoryData;
import woodchesterBadgers.CreateDescriptiveEpidemiologicalStats;
import woodchesterBadgers.SampleInfo;
import woodchesterCattle.CattleIsolateLifeHistoryData;
import woodchesterCattle.MakeEpidemiologicalComparisons;

public class WithinHostVariation {

	public static void main(String[] args) throws IOException{
		
		// Get the date
		String date = CalendarMethods.getCurrentDate("dd-MM-yyyy");
		//date = "15-03-2016"; // Override date
				
		// Read in the Isolate data
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/NewAnalyses_02-06-16/";
				
		// Read in the Badger data
		String sampledIsolateInfo = path + "IsolateData/BadgerInfo_08-04-15_LatLongs.csv";
		String consolidatedCaptureData = path + "BadgerCaptureData/consolidatedWPData.txt";
		String badgerTerritoryCentroidsFile = path + "BadgerCaptureData/TerritoryCentroids.csv";
		CapturedBadgerLifeHistoryData badgerIsolateLifeHistoryData = CreateDescriptiveEpidemiologicalStats.collateCaptureBadgerInformation(sampledIsolateInfo, consolidatedCaptureData, badgerTerritoryCentroidsFile, false);
		
		// Read in the isolate sequences from a FASTA file
		String fastaFile = path + "InitialTree/sequences_minCov-0.9_20-06-16.fasta";
		Sequence[] sequences = GeneticDistances.readFastaFile(fastaFile);
		
		// Parse the sequence names and convert them to IDs
		CompareIsolates.setIsolateIds(sequences);
		
		// Find the sample IDs for each badger
		Hashtable<String, SampleInfo[]> badgerSamplingInfo = findSampleIdsForBadgers(badgerIsolateLifeHistoryData.getSampledIsolateInfo(), sequences);
		
		// Order the sampling events by their date - earliest to latest
		orderSamplingHistoryForEachBadger(badgerSamplingInfo);
		
		// Examine the sampling history for each badger
		String file = path + "WithinBadgerDiversity/DistancesBetweenSamplesFromSameBadger_" + date + ".txt";
		printDistancesBetweenSamples(badgerSamplingInfo, file);
		
	}
	
	public static void orderSamplingHistoryForEachBadger(Hashtable<String, SampleInfo[]> badgerSamplingInfo){
		
		// Process the sampling history for each badger
		for(String tattoo : HashtableMethods.getKeysString(badgerSamplingInfo)){
			
			// Does the current badger have multiple sampling events?
			if(badgerSamplingInfo.get(tattoo).length > 1){
				
				badgerSamplingInfo.put(tattoo, orderSamplingHistory(badgerSamplingInfo.get(tattoo)));
			}
		}
	}
		
	public static SampleInfo[] orderSamplingHistory(SampleInfo[] infoForEachSamplingEvent){
		
		/**
		 * This Method Uses the Bubble Sort Algorithm
		 * 		Described here: http://en.wikipedia.org/wiki/Bubble_sort
		 * 
		 * 	For each element, compare it to the next element. If it is larger than the next element, swap the elements.
		 * 	Do this for each element of the list (except the last). Continue to iterate through the list elements and
		 *  make swaps until no swaps can be made.
		 */
		
		int swappedHappened = 1;
		while(swappedHappened == 1){ // Continue to compare the List elements until no swaps are made
		
			int swapped = 0;
			for(int index = 0; index < infoForEachSamplingEvent.length - 1; index++){
				
				// Compare Current Element to Next Element. 
				// Is the date for the current sampling event after the date for the next sampling event?
				if(infoForEachSamplingEvent[index].getDate().compareTo(infoForEachSamplingEvent[index + 1].getDate()) > 0){
					
					// Swap Current Element is Larger
					SampleInfo a = infoForEachSamplingEvent[index];
					SampleInfo b = infoForEachSamplingEvent[index + 1];
					
					infoForEachSamplingEvent[index] = b;
					infoForEachSamplingEvent[index + 1] = a;
					
					// Record that a Swap occurred
					swapped++;
				}
			}
			
			// Check if any swaps happened during the last iteration - if none then finished
			if(swapped == 0){
				swappedHappened = 0;
			}
		}
		
		return infoForEachSamplingEvent;
	}
		
	public static void printDistancesBetweenSamples(Hashtable<String, SampleInfo[]> badgerSamplingInfo,
			String fileName) throws IOException{
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Initialise an array to store the sampling information
		SampleInfo[] infoForEachSamplingEvent;
		
		// Initialise variables to calculate the genetic and temporal distance
		double temporalDist;
		int geneticDist;
		
		WriteToFile.writeLn(bWriter, "Tattoo\tTemporalDist\tGeneticDist\tiSampleType\tjSampleType\ti\tj\tWBIDi\tWBIDj");
		
		// Examine the sampling history for each badger
		for(String tattoo : HashtableMethods.getKeysString(badgerSamplingInfo)){
			
			// Skip badgers that only have one sampling event
			if(badgerSamplingInfo.get(tattoo).length == 1){
				continue;
			}
			
			// Get the Sampling information
			infoForEachSamplingEvent = badgerSamplingInfo.get(tattoo);
			
			// Calculate the genetic and temporal distances between sampling events
			for(int i = 0; i < infoForEachSamplingEvent.length; i++){
				
				for(int j = 0; j < infoForEachSamplingEvent.length; j++){
					
					if(i >= j){
						continue;
					}
					
					temporalDist = CalendarMethods.calculateNDaysBetweenDates(infoForEachSamplingEvent[i].getDate(), infoForEachSamplingEvent[j].getDate());
					geneticDist = GeneticMethods.calculateNumberDifferencesBetweenSequences(infoForEachSamplingEvent[i].getSequence().getSequence(), infoForEachSamplingEvent[j].getSequence().getSequence());
					WriteToFile.writeLn(bWriter, tattoo + "\t" + temporalDist + "\t" + geneticDist + "\t" + 
							infoForEachSamplingEvent[i].getSampleType() + "\t" + infoForEachSamplingEvent[j].getSampleType() +
							"\t" + i + "\t" + j +
							"\t" + infoForEachSamplingEvent[i].getWbId() + "\t" + infoForEachSamplingEvent[j].getWbId());
				}
			}
		}
		
		// Close the output file
		WriteToFile.close(bWriter);
	}
	
	public static Hashtable<String, SampleInfo[]> findSampleIdsForBadgers(Hashtable<String, SampleInfo> samplingInfo,
			Sequence[] sequences){
		
		// Initialise a hashtable to store the sampling info found for each badger - key = tattoo
		Hashtable<String, SampleInfo[]> badgerSamplingInfo = new Hashtable<String, SampleInfo[]>();
		
		// Initialise an array to store the information for each sampling event
		SampleInfo[] infoForSamplingEvents;
		SampleInfo info;
		
		// Look through all the sampling information and assign it to the correct badger
		for(Sequence sequence : sequences){
			
			// Skip the isolate if it is from a cow
			if(sequence.getName().matches("(TB.*)")){
				continue;
			}
			
			// Get the sampling information
			info = samplingInfo.get(sequence.getName());
			info.setSequence(sequence);
			
			// Have we encountered the sampled badger before?
			if(badgerSamplingInfo.get(info.getTattoo()) != null){
				
				badgerSamplingInfo.put(info.getTattoo(), SampleInfo.append(badgerSamplingInfo.get(info.getTattoo()), info));
			}else{
				
				infoForSamplingEvents = new SampleInfo[1];
				infoForSamplingEvents[0] = info;
				badgerSamplingInfo.put(info.getTattoo(), infoForSamplingEvents);
			}
		}
		
		return badgerSamplingInfo;
	}
}
