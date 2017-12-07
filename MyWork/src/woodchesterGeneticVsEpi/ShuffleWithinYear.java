package woodchesterGeneticVsEpi;

import java.io.IOException;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Random;

import org.uncommons.maths.random.MersenneTwisterRNG;

import geneticDistances.GeneticDistances;
import geneticDistances.Sequence;
import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.GeneralMethods;
import methods.HashtableMethods;
import woodchesterBadgers.CapturedBadgerLifeHistoryData;
import woodchesterBadgers.CreateDescriptiveEpidemiologicalStats;
import woodchesterCattle.CattleIsolateLifeHistoryData;
import woodchesterCattle.MakeEpidemiologicalComparisons;

public class ShuffleWithinYear {

	public static void main(String[] args) throws IOException{
		
		// Get the date
		String date = CalendarMethods.getCurrentDate("dd-MM-yyyy");
		date = "19-04-2016"; // Override date
				
		// Read in the Isolate data
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/";
				
		// Read in the Badger data
		String sampledIsolateInfo = path + "IsolateData/BadgerInfo_08-04-15_LatLongs.csv";
		String consolidatedCaptureData = path + "BadgerCaptureData/consolidatedWPData.txt";
		String badgerTerritoryCentroidsFile = path + "BadgerCaptureData/TerritoryCentroids.csv";
		CapturedBadgerLifeHistoryData badgerIsolateLifeHistoryData = CreateDescriptiveEpidemiologicalStats.collateCaptureBadgerInformation(sampledIsolateInfo, consolidatedCaptureData, badgerTerritoryCentroidsFile, false);
		badgerIsolateLifeHistoryData.setShortestPathsBetweenGroups(CompareIsolates.findShortestPathsBetweenAllNodes(badgerIsolateLifeHistoryData.getGroupAdjacencyMatrix()));
		
		// Read in the Cattle data
		String consolidatedSampledAnimalInfo = path + "IsolateData/ConsolidatedCattleIsolateData_" + date + ".txt";
		String consolidatedLocationsOfInterestFile = path + "IsolateData/CollatedCattleLocationInfo_" + date + ".txt";
		String locationAdjacencyMatrixFile = path + "IsolateData/CattleAdjacencyMatrix_" + date + ".txt";
		String locationSpatialDistanceMatrixFile = path + "IsolateData/CattleSpatialDistanceMatrix_" + date + ".txt";
		String nSharedBetweenLocationsMatrixFile = path + "IsolateData/NumberOfAnimalsSharedBetweenLocations_" + date + ".txt";
		String testHistoryFile = path + "CattleTestData/tblccdAnimal_13-09-16.txt";
		CattleIsolateLifeHistoryData cattleIsolateLifeHistoryData = 
				MakeEpidemiologicalComparisons.collateCattleIsolateData(consolidatedSampledAnimalInfo, 
						consolidatedLocationsOfInterestFile, locationAdjacencyMatrixFile, 
						locationSpatialDistanceMatrixFile, nSharedBetweenLocationsMatrixFile, testHistoryFile);
		cattleIsolateLifeHistoryData.getNetworkInfo().setShortestPathsFull(CompareIsolates.findShortestPathsBetweenAllNodes(cattleIsolateLifeHistoryData.getNetworkInfo().getAdjacencyMatrix()));
		String[] premisesTypesToIgnore = {"SR", "CC"}; // Slaughter Houses and Collection Centres
		cattleIsolateLifeHistoryData.getNetworkInfo().setShortestPathsWithoutSelectedPremises(CompareIsolates.findShortestPathsBetweenAllNodesExcludePremiseTypes(cattleIsolateLifeHistoryData.getNetworkInfo().getAdjacencyMatrix(), cattleIsolateLifeHistoryData.getLocations(), premisesTypesToIgnore));
				
//		// Read in the isolate sequences from a FASTA file
//		String fastaFile = path + "GeneticVsEpidemiologicalDistance/sequences_21-01-16_rmPoorGenomeCov-0.9.fasta";
//		Sequence[] sequences = GeneticDistances.readFastaFile(fastaFile);
//		
//		// Parse the sequence names and convert them to IDs
//		CompareIsolates.setIsolateIds(sequences);
		
		// Initialise a random number generator
		int seed = 243254264;
		Random random = GeneralMethods.startRandomNumberGenerator(seed);
		
//		// Shuffle the badger isolates within year
//		shuffleSequencesWithinYear(sequences, random, badgerIsolateLifeHistoryData);
//		
//		// Make the epidemiological comparisons
//		date = GeneralMethods.getCurrentDate("dd-MM-yyyy");
//		String geneticVsEpiDistances = path + "GeneticVsEpidemiologicalDistance/GeneticVsEpidemiologicalDistances_BadgersShuffledWithinYear_" + date + ".txt";
//		CompareIsolates.makeComparisonsBetweenIsolates(sequences, cattleIsolateLifeHistoryData, badgerIsolateLifeHistoryData, geneticVsEpiDistances, false);
		
		for(double prop = 0; prop <= 1; prop += 0.1){
			
			System.out.println("Making epidemiological comparisons on shuffled isolates. Proportion shuffled = " + GeneralMethods.round(prop, 1));
			
			// Read in the isolate sequences from a FASTA file
			String fastaFile = path + "GeneticVsEpidemiologicalDistance/sequences_21-01-16_rmPoorGenomeCov-0.9.fasta";
			Sequence[] sequences = GeneticDistances.readFastaFile(fastaFile);
			
			// Parse the sequence names and convert them to IDs
			CompareIsolates.setIsolateIds(sequences);
			
			// Shuffle a proportion of the isolates
			shuffleProportionOfBadgerIsolates(sequences, random, badgerIsolateLifeHistoryData, prop);
			
			// Make the epidemiological comparisons
			date = CalendarMethods.getCurrentDate("dd-MM-yyyy");
			String geneticVsEpiDistances = path + "GeneticVsEpidemiologicalDistance/ShuffleIsolates/GeneticVsEpidemiologicalDistances_" + date + "_PropShuffled-" + GeneralMethods.round(prop, 1) + ".txt";
			CompareIsolates.makeComparisonsBetweenIsolates(sequences, cattleIsolateLifeHistoryData, badgerIsolateLifeHistoryData, geneticVsEpiDistances, false);		
		}
	}
	
	public static void shuffleProportionOfBadgerIsolates(Sequence[] sequences, Random random,
			CapturedBadgerLifeHistoryData badgerIsolateLifeHistoryData, double prop){
		
		// Get a list of the badger indices
		int[] badgerIndices = getIndicesOfBadgers(sequences, badgerIsolateLifeHistoryData);
		
		// Randomly shuffle indices - so that there isn't an effect of order
		badgerIndices = ArrayMethods.shuffle(badgerIndices, random);
		
		// Calculate the number of isolates to shuffle
		int nIsolates = (int) (prop * badgerIndices.length);
		
		// Shuffle that number of isolates
		shuffle(badgerIndices, sequences, random, nIsolates);		
	}
	
	public static int[] getIndicesOfBadgers(Sequence[] sequences, CapturedBadgerLifeHistoryData badgerIsolateLifeHistoryData){
		
		// Initialise an array to store the indices of the badger isolates
		int[] indices = new int[sequences.length];
		int pos = -1;
		
		for(int i = 0; i < sequences.length; i++){
			
			// Skip cattle isolates
			if(sequences[i].getSpecies() == 'C'){
				continue;
			}
			
			// Add the index of the current badger sequence to the array
			pos++;
			indices[pos] = i;
		}
		
		return ArrayMethods.subset(indices, 0, pos);
	}
	
	public static void shuffleSequencesWithinYear(Sequence[] sequences, Random random,
			CapturedBadgerLifeHistoryData badgerIsolateLifeHistoryData){
		// Shuffle the isolates within year
		Hashtable<Integer, int[]> isolatesInYears = new Hashtable<Integer, int[]>(); // Year: seqIndexArray
		int year;
		int[] indices;
				
		for(int i = 0; i < sequences.length; i++){
			
			// Skip cattle isolates
			if(sequences[i].getSpecies() == 'C'){
				continue;
			}
					
			// Get the sampling year for the current isolate
			year = badgerIsolateLifeHistoryData.getSampledIsolateInfo().get(sequences[i].getName()).getDate().get(Calendar.YEAR);
					
			// Have we encountered this year before?
			if(isolatesInYears.get(year) != null){
						
				// Add the sequence index to the current list of indices
				isolatesInYears.put(year, ArrayMethods.append(isolatesInYears.get(year), i));
					
			// Create a new list of indices for the current year
			}else{
				indices = new int[1];
				indices[0] = i;
				isolatesInYears.put(year, indices);
			}
		}
		
		for(int key : HashtableMethods.getKeysInt(isolatesInYears)){
			
			indices = isolatesInYears.get(key);
			
			shuffle(indices, sequences, random, indices.length);
		}
	}
	
	public static void shuffle(int[] indices, Sequence[] sequences, Random random, int n) {
		
		for (int i = 0; i < n; i++) {
		    
			// Choose a random element
		    int randomIndex = ArrayMethods.randomIndex(indices.length, random);
		    
		    // Swap the sequences' names
		    String id = sequences[indices[i]].getName();
		    sequences[indices[i]].setName(sequences[indices[randomIndex]].getName());
		    sequences[indices[randomIndex]].setName(id);
		    
		    // Swap the random element with the present element.
		    int randomElement = indices[randomIndex];
		    indices[randomIndex] = indices[i];
		    indices[i] = randomElement;
		}
	}
}
