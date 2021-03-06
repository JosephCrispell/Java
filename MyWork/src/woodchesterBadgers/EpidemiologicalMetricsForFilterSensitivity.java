package woodchesterBadgers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Random;

import geneticDistances.GeneticDistances;
import geneticDistances.Sequence;
import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.GeneralMethods;
import methods.GeneticMethods;
import methods.WriteToFile;
import woodchesterCattle.MakeEpidemiologicalComparisons;
import woodchesterGeneticVsEpi.CompareIsolates;

public class EpidemiologicalMetricsForFilterSensitivity {

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
		String fastaFile = path + "vcfFiles/sequences_Prox-10_23-03-2018.fasta";
		Sequence[] sequences = GeneticMethods.readFastaFile(fastaFile);
		
		// Remove cattle
		sequences = CreateDescriptiveEpidemiologicalStats.removeCattleIsolateSequences(sequences);
		
		// Compare the badgers
		String outputFile = path + "Mislabelling/Badger-RF-BR/" + "geneticVsEpiTable_" + date + ".txt";
		makeComparisonsBetweenIsolates(sequences, badgerIsolateLifeHistoryData, outputFile, false, true);

	}
	
	public static void parseSequenceNames(Sequence[] sequences){
		
		for(int i = 0; i < sequences.length; i++){
			sequences[i].setName(sequences[i].getName().split("_")[0]);
		}
	}
	
	public static void makeComparisonsBetweenIsolates(Sequence[] sequences,
			CapturedBadgerLifeHistoryData badgerIsolateLifeHistoryData,
			String outputFile, boolean addNAs, boolean addIDs) throws IOException{
		
		/**
		 * 					SAME											PERIOD
		 * 	_________________________________________	_______________________________________________
		 * 	MainGroup	SampledGroup	InfectedGroup	AliveTogether	InfectedTogether	InSameGroup
		 * 	0			1				2				3				4					5
		 * 
		 * 				  TIME BETWEEN							SPATIAL DISTANCE BETWEEN
		 * 	____________________________		_________________________________________
		 * 	InfectionDetection	Sampling		MainGroup	SampledGroup	InfectedGroup
		 * 	6				  	7				8			9				10				
		 * 
		 * 				MOVEMENTS BETWEEN				SameAnimal	SHORTEST PATH LENGTH / MEAN N MOVEMENTS ON EDGES
		 * 	_________________________________________		__		_________________________________________
		 * 	MainGroup	SampledGroup	InfectedGroup				MainGroup	SampledGroup	InfectedGroup
		 * 	11			12				13					14		15	16		17		18		19		20
		 * 
		 * 		NUMBER SHARED ANIMALS BETWEEN			 Host relatedness
		 * 	_________________________________________		___________
		 * 	MainGroup	SampledGroup	InfectedGroup		Relatedness
		 * 	21			22				23					24
		 */
		
		// Build a Genetic Distance Matrix
		int[][] geneticDistances = GeneticDistances.createGeneticDistanceMatrix(sequences);
		
		// Open the output file and print header
		BufferedWriter bWriter = WriteToFile.openFile(outputFile, false);
		String header = "GeneticDistance\tSameMainGroup\tSameSampledGroup\tSameInfectedGroup\t"
				+ "PeriodSpentAliveTogether\tPeriodSpentInfectedTogether\tPeriodSpentInSameGroup\t"
				+ "TimeBetweenInfectionDetection\tTimeBetweenSampling\t"
				+ "DistanceBetweenMainGroups\tDistanceBetweenSampledGroups\tDistanceBetweenInfectedGroups\t"
				+ "NMovementsBetweenMainGroups\tNMovementsBetweenSampledGroups\tNMovementsBetweenInfectedGroups\t"
				+ "SameAnimal\t"
				+ "ShortestPathLengthMain\tMeanNMovementsOnEdgesOfShortestPathMain\t"
				+ "ShortestPathLengthSampled\tMeanNMovementsOnEdgesOfShortestPathSampled\t"
				+ "ShortestPathLengthInfected\tMeanNMovementsOnEdgesOfShortestPathInfected\t"
				+ "NSharedAnimalsBetweenMainGroups\tNSharedAnimalsBetweenSampledGroups\tNSharedAnimalsBetweenInfectedGroups\t"
				+ "Relatedness";

		// Add two additional ID columns if necessary
		if(addIDs == true){
			header = header + "\tIsolateI\tIsolateJ";
		}
		
		WriteToFile.writeLn(bWriter, header);
		String output;
		
		// Initialise an array to store the epidemiological metrics from each copmarison
		double[] epiMetrics = new double[33];
		
		// Parse the sequence names and convert them to IDs
		parseSequenceNames(sequences);
		
		// Initialise variables to store the isolate data
		SampleInfo badgerI = null;
		SampleInfo badgerJ = null;
		
		for(int i = 0; i < sequences.length; i++){
			
			// Get the isolate information for isolate i
			badgerI = badgerIsolateLifeHistoryData.getSampledIsolateInfo().get(sequences[i].getName());
			
			for(int j = 0; j < sequences.length; j++){
				
				// Only make comparisons once and skip self comparisons
				if(i >= j){
					continue;
				}
				
				// Get the isolate information for isolate j
				badgerJ = badgerIsolateLifeHistoryData.getSampledIsolateInfo().get(sequences[j].getName());
								
				// Compare the two badgers
				epiMetrics = makeBadgerBadgerComparison(badgerI, badgerJ, badgerIsolateLifeHistoryData);
				
				// Create the output file - genetic distance and associated epidemiological metrics
				output = geneticDistances[i][j] + "\t" + CompareIsolates.convertEpiMetricsToString(epiMetrics, addNAs);
				
				// Add the badger IDs if wanted
				if(addIDs == true){
					output = output + "\t" + badgerI.getWbId() + "\t" + badgerJ.getWbId();
				}
				
				// Write the output out to file
				WriteToFile.writeLn(bWriter, output);				
			}			
		}
		
		// Close the output file
		WriteToFile.close(bWriter);		
	}
		
	public static double[] makeBadgerBadgerComparison(SampleInfo a, SampleInfo b,
			CapturedBadgerLifeHistoryData badgerIsolateLifeHistoryData){
		
		// Initialise the array to store the results of the epidemiological comparison
		double[] epiMetrics = new double[25];
		
		// Get the life history data
		CaptureData aCaptureData = badgerIsolateLifeHistoryData.getBadgerCaptureHistories().get(a.getTattoo());
		CaptureData bCaptureData = badgerIsolateLifeHistoryData.getBadgerCaptureHistories().get(b.getTattoo());
		
		// Get the badger group information
		Hashtable<String, Integer> badgerGroupIndices = badgerIsolateLifeHistoryData.getBadgerGroupIndices();
		int[][] groupAdjacencyMatrix = badgerIsolateLifeHistoryData.getGroupAdjacencyMatrix();
		Hashtable<String, TerritoryCentroids> territoryCentroids = badgerIsolateLifeHistoryData.getTerritoryCentroids();
		Hashtable<Integer, int[][]> shortestPaths = badgerIsolateLifeHistoryData.getShortestPathsBetweenGroups();
		int[][] nShared = badgerIsolateLifeHistoryData.getNSharedBadgersBetweenGroups();
		double[][] relatedness = badgerIsolateLifeHistoryData.getGeneticRelatedness();
		
		//**** Make the comparison ****
		
		// Did these two badgers share the same main/sampled/infected group?
		epiMetrics[0] = CreateDescriptiveEpidemiologicalStats.checkIfInSameGroup(aCaptureData.getMainGroup(), bCaptureData.getMainGroup());
		epiMetrics[1] = CreateDescriptiveEpidemiologicalStats.checkIfInSameGroup(a.getBadgerGroup(), b.getBadgerGroup());
		epiMetrics[2] = CreateDescriptiveEpidemiologicalStats.checkIfInSameGroup(aCaptureData.getGroupWhenFirstInfected(), bCaptureData.getGroupWhenFirstInfected());
		
		// How much time were these badgers alive/infected/in the same group together?
		epiMetrics[3] = MakeEpidemiologicalComparisons.calculateNDaysOverlapped(aCaptureData.getStart(), aCaptureData.getEnd(), bCaptureData.getStart(), bCaptureData.getEnd());
		epiMetrics[4] = MakeEpidemiologicalComparisons.calculateNDaysOverlapped(aCaptureData.getDatesInMilliSeconds()[aCaptureData.getWhenInfectionDetected()], aCaptureData.getEnd(), bCaptureData.getDatesInMilliSeconds()[bCaptureData.getWhenInfectionDetected()], bCaptureData.getEnd());
		epiMetrics[5] = CreateDescriptiveEpidemiologicalStats.findPeriodSpentInSameGroup(aCaptureData.getPeriodsInEachGroup(), bCaptureData.getPeriodsInEachGroup());
		
		// Calculate the time between infection detection/sampling/breakdown
		epiMetrics[6] = CalendarMethods.calculateNDaysBetweenDates(aCaptureData.getDatesInMilliSeconds()[aCaptureData.getWhenInfectionDetected()], bCaptureData.getDatesInMilliSeconds()[bCaptureData.getWhenInfectionDetected()]);
		epiMetrics[7] = CalendarMethods.calculateNDaysBetweenDates(a.getDate(), b.getDate());
		
		// Calculate the spatial distance between the Main/Sampled/Infected Groups
		epiMetrics[8] = CreateDescriptiveEpidemiologicalStats.calculateSpatialDistanceBetweenGroups(
				aCaptureData.getMainGroup(), bCaptureData.getMainGroup(), 
				-1, -1, territoryCentroids);
		epiMetrics[9] = CreateDescriptiveEpidemiologicalStats.calculateSpatialDistanceBetweenGroups(
				a.getBadgerGroup(), b.getBadgerGroup(), 
				a.getDate().get(Calendar.YEAR),
				b.getDate().get(Calendar.YEAR),
				territoryCentroids);
		epiMetrics[10] = CreateDescriptiveEpidemiologicalStats.calculateSpatialDistanceBetweenGroups(
				aCaptureData.getGroupWhenFirstInfected(), bCaptureData.getGroupWhenFirstInfected(), 
				aCaptureData.getCaptureDates()[aCaptureData.getWhenInfectionDetected()].get(Calendar.YEAR),
				bCaptureData.getCaptureDates()[bCaptureData.getWhenInfectionDetected()].get(Calendar.YEAR),
				territoryCentroids);
		
		// Note the number of movements linking the Main/Sampled/Infected Groups
		epiMetrics[11] = groupAdjacencyMatrix[badgerGroupIndices.get(aCaptureData.getMainGroup())][badgerGroupIndices.get(bCaptureData.getMainGroup())];
		epiMetrics[12] = groupAdjacencyMatrix[badgerGroupIndices.get(a.getBadgerGroup())][badgerGroupIndices.get(b.getBadgerGroup())];
		epiMetrics[13] = groupAdjacencyMatrix[badgerGroupIndices.get(aCaptureData.getGroupWhenFirstInfected())][badgerGroupIndices.get(bCaptureData.getGroupWhenFirstInfected())];
		
		// Are these isolates from the same animal?
		epiMetrics[14] = 0;
		if(a.getTattoo().matches(b.getTattoo())){
			epiMetrics[14] = 1;
		}
		
		// Look at the shortest path between the sampled animals Main/Sampled/Infected herds/groups
		int[] indices = shortestPaths.get(badgerGroupIndices.get(aCaptureData.getMainGroup()))[badgerGroupIndices.get(bCaptureData.getMainGroup())];
		epiMetrics[15] = -1;
		epiMetrics[16] = -1;
		if(indices.length != 0){
			epiMetrics[15] = indices.length;
			epiMetrics[16] = CompareIsolates.calculateMeanNMovementsOnEdgesOfShortestPath(indices, badgerGroupIndices.get(bCaptureData.getMainGroup()), groupAdjacencyMatrix);
		}
		
		indices = shortestPaths.get(badgerGroupIndices.get(a.getBadgerGroup()))[badgerGroupIndices.get(b.getBadgerGroup())];
		epiMetrics[17] = -1;
		epiMetrics[18] = -1;
		if(indices.length != 0){
			epiMetrics[17] = indices.length;
			epiMetrics[18] = CompareIsolates.calculateMeanNMovementsOnEdgesOfShortestPath(indices, badgerGroupIndices.get(b.getBadgerGroup()), groupAdjacencyMatrix);
		}
		
		indices = shortestPaths.get(badgerGroupIndices.get(aCaptureData.getGroupWhenFirstInfected()))[badgerGroupIndices.get(bCaptureData.getGroupWhenFirstInfected())];
		epiMetrics[19] = -1;
		epiMetrics[20] = -1;
		if(indices.length != 0){
			epiMetrics[19] = indices.length;
			epiMetrics[20] = CompareIsolates.calculateMeanNMovementsOnEdgesOfShortestPath(indices, badgerGroupIndices.get(bCaptureData.getGroupWhenFirstInfected()), groupAdjacencyMatrix);
		}
		
		// Note the number of animals shared between the Main/Sampled/Infected Groups
		epiMetrics[21] = nShared[badgerGroupIndices.get(aCaptureData.getMainGroup())][badgerGroupIndices.get(bCaptureData.getMainGroup())];
		epiMetrics[22] = nShared[badgerGroupIndices.get(a.getBadgerGroup())][badgerGroupIndices.get(b.getBadgerGroup())];
		epiMetrics[23] = nShared[badgerGroupIndices.get(aCaptureData.getGroupWhenFirstInfected())][badgerGroupIndices.get(bCaptureData.getGroupWhenFirstInfected())];
		
		// Note the relatedness of the badgers
		epiMetrics[24] = CreateDescriptiveEpidemiologicalStats.getRelatedness(aCaptureData, bCaptureData, relatedness);
		
		return epiMetrics;
	}
}
