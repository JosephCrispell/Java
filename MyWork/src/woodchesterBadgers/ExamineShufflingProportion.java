package woodchesterBadgers;

import java.io.IOException;
import java.util.Random;

import geneticDistances.GeneticDistances;
import geneticDistances.Sequence;
import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.GeneralMethods;
import methods.GeneticMethods;
import testBEASTRateEstimation.RunStateTransitionSimulations;
import woodchesterGeneticVsEpi.CompareIsolates;

public class ExamineShufflingProportion {

	
	public static void main(String[] args) throws IOException{
		
		// Get the date
		String date = CalendarMethods.getCurrentDate("dd-MM-yyyy");
					
		// Read in the Isolate data
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/NewAnalyses_22-03-18/";
						
		// Read in the Badger data
		String sampledIsolateInfo = path + "IsolateData/BadgerInfo_08-04-15_LatLongs_XY_Centroids.csv";
		String consolidatedCaptureData = path + "BadgerCaptureData/WP_CaptureData_Consolidated_31-07-2017.txt";
		String badgerTerritoryCentroidsFile = path + "BadgerCaptureData/BadgerTerritoryMarkingData/" + 
				"SocialGroupsCentroidsPerYear_16-05-17.txt";
		String relatednessMatrixFile = path + "BadgerRelatedness/GenotypedBadgerRelatedness_ImputationOnly_12-07-17.csv";
		CapturedBadgerLifeHistoryData badgerIsolateLifeHistoryData = 
				CreateDescriptiveEpidemiologicalStats.collateCaptureBadgerInformation(sampledIsolateInfo,
						consolidatedCaptureData, badgerTerritoryCentroidsFile, false, relatednessMatrixFile);
						badgerIsolateLifeHistoryData.setShortestPathsBetweenGroups(CompareIsolates.findShortestPathsBetweenAllNodes(badgerIsolateLifeHistoryData.getGroupAdjacencyMatrix()));
				
		// Read in the sequences
		String fastaFile = path + "vcfFiles/sequences_Prox-10_24-03-2018.fasta";
		Sequence[] sequences = GeneticMethods.readFastaFile(fastaFile);
				
		// Remove cattle
		sequences = CreateDescriptiveEpidemiologicalStats.removeCattleIsolateSequences(sequences);
				
		// Generate genetic vs. epi tables for shuffled isolates
		double[] shufflingProps = ArrayMethods.seq(0, 1, 0.05);
		int nReps = 10;
		Sequence[] shuffled;
		String outputFile;
		
		// Initialise a random number generator
		int seed = RunStateTransitionSimulations.generateSeeds(1)[0];
		Random random = GeneralMethods.startRandomNumberGenerator(seed);
		
		for(int i = 0; i < shufflingProps.length; i++){
			
			for(int repeat = 0; repeat < nReps; repeat++){
				
				shuffled = shuffleIsolates(sequences, shufflingProps[i], random);
				
				outputFile = path + "Mislabelling/Badger-RF-BR/ShufflingProportion/" + 
				"geneticVsEpiTable_SHUFFLED_Prop-" + 
				GeneralMethods.round(shufflingProps[i], 2) + "_" + repeat + "_" + date + ".txt";
				EpidemiologicalMetricsForFilterSensitivity.makeComparisonsBetweenIsolates(shuffled,
						badgerIsolateLifeHistoryData, outputFile, false, true);				
			}
		}
	}
	
	public static Sequence[] shuffleIsolates(Sequence[] isolateSequences, double proportion,
			Random random){
		
		// Create an array of the isolate IDs and sequences
		String[] names = new String[isolateSequences.length];
		char[][] sequences = new char[isolateSequences.length][0];
		
		for(int i = 0; i < isolateSequences.length; i++){
			names[i] = isolateSequences[i].getName();
			sequences[i] = isolateSequences[i].getSequence();
		}
		
		// Shuffle a proportion of the sequences
		int[] shuffled = shuffleProportionOfIndices(isolateSequences.length, proportion, random);
		
		// Re-allocate the sequences
		Sequence[] shuffledSequences = new Sequence[isolateSequences.length];
		for(int i = 0; i < shuffled.length; i++){
			shuffledSequences[i] = new Sequence(names[shuffled[i]], sequences[i]);
		}
		
		return shuffledSequences;
	}
	
	public static int[] shuffleProportionOfIndices(int length, double proportion, Random random){
		int[] indices = ArrayMethods.seq(0, length - 1, 1);
		
		// Select isolates to shuffle
		int[] toShuffle = ArrayMethods.randomChoices(indices, (int) (proportion * (double) length),
				random, false);
		
		int[] shuffled = ArrayMethods.shuffle(toShuffle, random);
		
		// Apply the above shuffling
		int[] indicesWithShuffled = ArrayMethods.seq(0, length - 1, 1);
		for(int i = 0; i < toShuffle.length; i++){
			
			indicesWithShuffled[toShuffle[i]] = indices[shuffled[i]];
		}
		
		return indicesWithShuffled;
	}
}
