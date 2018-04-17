package woodchesterGeneticVsEpi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Hashtable;

import geneticDistances.GeneticDistances;
import geneticDistances.Sequence;
import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.GeneralMethods;
import methods.HashtableMethods;
import methods.LatLongMethods;
import methods.MatrixMethods;
import methods.WriteToFile;
import woodchesterBadgers.CaptureData;
import woodchesterBadgers.CapturedBadgerLifeHistoryData;
import woodchesterBadgers.CreateDescriptiveEpidemiologicalStats;
import woodchesterBadgers.SampleInfo;
import woodchesterBadgers.TerritoryCentroids;
import woodchesterCattle.CattleIsolateLifeHistoryData;
import woodchesterCattle.IsolateData;
import woodchesterCattle.Location;
import woodchesterCattle.MakeEpidemiologicalComparisons;

public class CompareIsolates {

	public static void main(String[] args) throws IOException{
		
		// Get the date
		String date = CalendarMethods.getCurrentDate("dd-MM-yyyy");
		date = "04-04-2018"; // Override date
				
		// Read in the Isolate data
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/NewAnalyses_22-03-18/";
				
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
		cattleIsolateLifeHistoryData.getNetworkInfo().setShortestPathsFull(
				findShortestPathsBetweenAllNodes(
						cattleIsolateLifeHistoryData.getNetworkInfo().getAdjacencyMatrix()));
		String[] premisesTypesToIgnore = {"SR", "CC", "SW", "EX"}; // Slaughter Houses and Collection Centres
		cattleIsolateLifeHistoryData.getNetworkInfo().setShortestPathsWithoutSelectedPremises(
				findShortestPathsBetweenAllNodesExcludePremiseTypes(
						cattleIsolateLifeHistoryData.getNetworkInfo().getAdjacencyMatrix(),
						cattleIsolateLifeHistoryData.getLocations(), premisesTypesToIgnore));
		
		// Read in the Land Parcel centroid data
		String landParcelCentroidsFile = path + "LandParcelData/RPA_CLAD_ASI_CURRENT_SP_ST-SO_NE-SE/";
		landParcelCentroidsFile += "RPA_CLAD_ASI_CURRENT_SP_ST-SO_NE-SE_Centroids-XY.csv";
		Hashtable<String, String> idsForCPHs = linkCPHsToLocationIds(cattleIsolateLifeHistoryData.getLocations());
		getLocationCentroidInformation(landParcelCentroidsFile, cattleIsolateLifeHistoryData.getLocations());
		
		// Read in the Badger data
		String sampledIsolateInfo = path + "IsolateData/BadgerInfo_08-04-15_LatLongs_XY_Centroids.csv";
		String consolidatedCaptureData = path + "BadgerCaptureData/WP_CaptureData_Consolidated_31-07-2017.txt";
		String territoryCentroidsFile = path + "BadgerCaptureData/BadgerTerritoryMarkingData/" + 
				"SocialGroupsCentroidsPerYear_16-05-17.txt";
		String relatednessMatrixFile = path + "BadgerRelatedness/GenotypedBadgerRelatedness_ImputationOnly_12-07-17.csv";
		CapturedBadgerLifeHistoryData badgerIsolateLifeHistoryData = 
				CreateDescriptiveEpidemiologicalStats.collateCaptureBadgerInformation(sampledIsolateInfo,
						consolidatedCaptureData, territoryCentroidsFile, false, relatednessMatrixFile);
		badgerIsolateLifeHistoryData.setShortestPathsBetweenGroups(
				findShortestPathsBetweenAllNodes(badgerIsolateLifeHistoryData.getGroupAdjacencyMatrix()));
		
		// Read in the isolate sequences from a FASTA file
		String fastaFile = path + "vcfFiles/sequences_withoutHomoplasies_27-03-18.fasta";
		Sequence[] sequences = GeneticDistances.readFastaFile(fastaFile);
		
		// Remove reference
		sequences = removeReferenceFromSequences(sequences);
		
		// Make the epidemiological comparisons
		date = CalendarMethods.getCurrentDate("dd-MM-YY");
		String geneticVsEpiDistancesFile = path + "GeneticVsEpidemiologicalDistances/" +
		"GeneticVsEpidemiologicalDistances_" + date + ".txt";
		makeComparisonsBetweenIsolates(sequences, cattleIsolateLifeHistoryData, badgerIsolateLifeHistoryData, geneticVsEpiDistancesFile, false);
	}
	
	public static Sequence[] removeReferenceFromSequences(Sequence[] sequences){
		
		// Initialise a new array to store the sequences
		Sequence[] output = null;
		
		// Check that reference is present
		if(sequences[sequences.length - 1].getName().matches("Ref(.*)") == true){
			
			output = Sequence.subset(sequences, 0, sequences.length - 2);
		}else{
			System.out.println("ERROR!: Reference not present!");
		}
		
		return output;
	}
	
	public static void makeComparisonsBetweenIsolates(Sequence[] sequences,
			CattleIsolateLifeHistoryData cattleIsolateLifeHistoryData,
			CapturedBadgerLifeHistoryData badgerIsolateLifeHistoryData,
			String outputFile, boolean addNAs) throws IOException{
		
		/**
		 * 					SAME											PERIOD
		 * 	_________________________________________	_______________________________________________
		 * 	MainGroup	SampledGroup	InfectedGroup	AliveTogether	InfectedTogether	InSameGroup
		 * 	0			1				2				3				4					5
		 * 	B-B			B-B				B-B				B-B				B-B					B-B
		 * 	C-C			C-C				NULL			C-C				NULL				C-C	
		 * 	NULL		NULL			NULL			C-B				NULL				NULL
		 * 
		 * 
		 * 				  TIME BETWEEN							SPATIAL DISTANCE BETWEEN
		 * 	_________________________________________	_________________________________________
		 * 	InfectionDetection	Sampling	Breakdown	MainGroup	SampledGroup	InfectedGroup
		 * 	6				  	7			8			9			10				11
		 * 	B-B				  	B-B			NULL		B-B			B-B				B-B
		 * 	NULL				C-C			C-C			C-C			C-C				NULL
		 * 	C-B					NULL		NULL		C-B			C-B				NULL
		 * 
		 * 
		 * 				MOVEMENTS BETWEEN				SameAnimal	SHORTEST PATH LENGTH / MEAN N MOVEMENTS ON EDGES
		 * 	_________________________________________		__		_________________________________________
		 * 	MainGroup	SampledGroup	InfectedGroup				MainGroup	SampledGroup	InfectedGroup
		 * 	12			13				14					15		16	17		18		19		20		21
		 * 	B-B			B-B				B-B					B-B		B-B	B-B		B-B		B-B		B-B		B-B
		 * 	C-C			C-C				NULL				C-C		C-C	C-C		C-C		C-C		NULL	NULL
		 * 	NULL		NULL			NULL				NULL	NULLNULL	NULL	NULL	NULL	NULL
		 * 
		 * 		NUMBER SHARED ANIMALS BETWEEN			SHORTEST PATH EXCLUDING PREMISES / MEAN N MOVEMENTS ON EDGES
		 * 	_________________________________________	_________________________________________
		 * 	MainGroup	SampledGroup	InfectedGroup	MainGroup	SampledGroup	InfectedGroup
		 * 	22			23				24				25	26		27		28		29		30
		 * 	B-B			B-B				B-B				NULLNULL	NULL	NULL	NULL	NULL
		 * 	C-C			C-C				NULL			C-C	C-C		C-C		C-C		NULL	NULL
		 * 	NULL		NULL			NULL			NULLNULL	NULL	NULL	NULL	NULL
		 * 
		 *   DISTANCE BETWEEN GROUPS USING CENTROIDS	HOST RELATEDNESS
		 *  ________________________					__
		 * 	MainGroup	SampledGroup		
		 * 	31			32								33
		 * 	NULL		NULL							B-B
		 * 	C-C			C-C								NULL
		 * 	C-B			C-B					 			NULL
		 */
		
		// Build a Genetic Distance Matrix
		int[][] geneticDistances = GeneticDistances.createGeneticDistanceMatrix(sequences);
		
		// Open the output file and print header
		BufferedWriter bWriter = WriteToFile.openFile(outputFile, false);
		String header = "GeneticDistance\tiSpeciesJSpecies\tSameMainGroup\tSameSampledGroup\tSameInfectedGroup\t"
				+ "PeriodSpentAliveTogether\tPeriodSpentInfectedTogether\tPeriodSpentInSameGroup\t"
				+ "TimeBetweenInfectionDetection\tTimeBetweenSampling\tTimeBetweenBreakdown\t"
				+ "DistanceBetweenMainGroups\tDistanceBetweenSampledGroups\tDistanceBetweenInfectedGroups\t"
				+ "NMovementsBetweenMainGroups\tNMovementsBetweenSampledGroups\tNMovementsBetweenInfectedGroups\t"
				+ "SameAnimal\t"
				+ "ShortestPathLengthMain\tMeanNMovementsOnEdgesOfShortestPathMain\t"
				+ "ShortestPathLengthSampled\tMeanNMovementsOnEdgesOfShortestPathSampled\t"
				+ "ShortestPathLengthInfected\tMeanNMovementsOnEdgesOfShortestPathInfected\t"
				+ "NSharedAnimalsBetweenMainGroups\tNSharedAnimalsBetweenSampledGroups\tNSharedAnimalsBetweenInfectedGroups\t"
				+ "ShortestPathLengthEXCLMain\tMeanNMovementsOnEdgesOfShortestPathEXCLMain\t"
				+ "ShortestPathLengthEXCLSampled\tMeanNMovementsOnEdgesOfShortestPathEXCLSampled\t"
				+ "ShortestPathLengthEXCLInfected\tMeanNMovementsOnEdgesOfShortestPathEXCLInfected\t"
				+ "CentroidDistBetweenMain\tCentroidDistBetweenSamp\tHostRelatedness\t"
				+ "IsolateI\tIsolateJ";

		WriteToFile.writeLn(bWriter, header);
		String output;
		
		// Initialise an array to store the epidemiological metrics from each copmarison
		double[] epiMetrics = new double[33];
		
		// Parse the sequence names and convert them to IDs
		setIsolateIds(sequences);
		
		// Initialise variables to store the isolate data
		IsolateData cattleI = null;
		IsolateData cattleJ = null;
		SampleInfo badgerI = null;
		SampleInfo badgerJ = null;
		IsolateData cow;
		SampleInfo badger;
		
		Hashtable<String, Integer> foundMistake = new Hashtable<String, Integer>();
		
		for(int i = 0; i < sequences.length; i++){
			
			// Get the isolate information for isolate i
			cattleI = null;
			badgerI = null;
			if(sequences[i].getSpecies() == 'C'){
				cattleI = cattleIsolateLifeHistoryData.getIsolates().get(cattleIsolateLifeHistoryData.getEartagsForStrainIds().get(sequences[i].getName()));
			}else if(sequences[i].getSpecies() == 'B'){
				badgerI = badgerIsolateLifeHistoryData.getSampledIsolateInfo().get(sequences[i].getName());
			}else{
				System.out.println("This isolate doesn't have a species!!!" + "\t" + sequences[i].getName());
			}
			
			if(cattleI == null && badgerI == null){
				
				if(foundMistake.get(sequences[i].getName()) == null){
					System.out.println("Weren't able to find Isolate data for: " + sequences[i].getName() + "\t" + sequences[i].getSpecies());
					foundMistake.put(sequences[i].getName(), 1);
				}
				
				continue;
			}
			
			for(int j = 0; j < sequences.length; j++){
				
				// Only make comparisons once and skip self comparisons
				if(i >= j){
					continue;
				}
				
				// Get the isolate information for isolate j
				cattleJ = null;
				badgerJ = null;
				if(sequences[j].getSpecies() == 'C'){
					cattleJ = cattleIsolateLifeHistoryData.getIsolates().get(cattleIsolateLifeHistoryData.getEartagsForStrainIds().get(sequences[j].getName()));
				}else if(sequences[j].getSpecies() == 'B'){
					badgerJ = badgerIsolateLifeHistoryData.getSampledIsolateInfo().get(sequences[j].getName());
				}else{
					System.out.println("ERROR!: This isolate doesn't have a species!!!" + "\t" + sequences[j].getName());
				}
				
				if(cattleJ == null && badgerJ == null){
					
					if(foundMistake.get(sequences[i].getName()) == null){
						System.out.println("Weren't able to find Isolate data for: " + sequences[i].getName() + "\t" + sequences[i].getSpecies());
						foundMistake.put(sequences[i].getName(), 1);
					}					
					continue;
				}

				// Is this a Badger-Badger comparison?
				if(badgerI != null && badgerJ != null){
					
					// Compare the two badgers
					epiMetrics = makeBadgerBadgerComparison(badgerI, badgerJ, badgerIsolateLifeHistoryData);
				
				// Is this a Cattle-Cattle comparison?
				}else if(cattleI != null && cattleJ != null){
					
					// Compare the two cattle
					epiMetrics = makeCattleCattleComparison(cattleI, cattleJ, cattleIsolateLifeHistoryData);
					
				// This is a Cattle <-> Badger comparison
				}else if((cattleI != null || cattleJ != null) && (badgerI != null || badgerJ != null)){
					
					cow = cattleI;
					if(cattleJ != null){
						cow = cattleJ;
					}
					badger = badgerI;
					if(badgerJ != null){
						badger = badgerJ;
					}
					
					// Compare between a cow and badger
					epiMetrics = makeCattleBadgerComparison(cow, badger, badgerIsolateLifeHistoryData, cattleIsolateLifeHistoryData);
				}else{
					System.out.println("ERROR!: Unable to find the Isolate Information for both the isolates being compared.");
				}
				
				// Create the output file - genetic distance and associated epidemiological metrics
				output = geneticDistances[i][j] + "\t" + sequences[i].getSpecies() + "" + sequences[j].getSpecies() + "\t" + convertEpiMetricsToString(epiMetrics, addNAs) + "\t" + sequences[i].getName() + "\t" + sequences[j].getName();
				
				// Write the output out to file
				WriteToFile.writeLn(bWriter, output);				
			}			
		}
		System.out.println();
		
		// Close the output file
		WriteToFile.close(bWriter);		
	}
	
	public static String convertEpiMetricsToString(double[] epiMetrics, boolean addNA){
		
		// Initialise a string to store the output
		String output = "";
		
		// Add every element of the epiMetrics array - replace -1 with NA if addNA == true
		for(int i = 0; i < epiMetrics.length; i++){
			
			// Check if the value is -1
			if(epiMetrics[i] == -1 && addNA == true){
				output += "NA";
			}else{
				output += epiMetrics[i];
			}
			
			// Add separator if haven't reached the end
			if(i < epiMetrics.length - 1){
				output += "\t";
			}
		}
		
		return output;
	}
	
	public static double calculateSpatialDistance(double aX, double aY, double bX, double bY){
		
		double distance = -1;
		
		double value;
		
		// Check that both the locations have x and y coordinates
		if(aX != -1 && aY != -1 && bX != -1 && bY != -1){
			
			value = Math.pow((double)(bX - aX), 2);
			value += Math.pow((double)(bY - aY), 2);
			distance = Math.sqrt(value);
		}
		
		return distance;
	}
	
	public static double[] makeCattleBadgerComparison(IsolateData cow, SampleInfo badger,
			CapturedBadgerLifeHistoryData badgerIsolateLifeHistoryData, CattleIsolateLifeHistoryData cattleIsolateLifeHistoryData){
		
		// Initialise the array to store the results of the epidemiological comparison
		double[] epiMetrics = new double[34];
		
		// Get the badger capture data
		CaptureData badgerCaptureData = badgerIsolateLifeHistoryData.getBadgerCaptureHistories().get(badger.getTattoo());
		Hashtable<String, TerritoryCentroids> badgerTerritoryCentroids = badgerIsolateLifeHistoryData.getTerritoryCentroids();
		
		// Get spatial data for badger main and sampled group
		double[] badgerMainGroupCentroid = null;
		if(badgerTerritoryCentroids.get(badgerCaptureData.getMainGroup()) != null){
			badgerMainGroupCentroid = 
					badgerTerritoryCentroids.get(badgerCaptureData.getMainGroup()).getCoords("-1");
		}
		double[] badgerSampledGroupCentroid = null;
		if(badgerTerritoryCentroids.get(badger.getBadgerGroup()) != null){
			badgerSampledGroupCentroid = 
					badgerTerritoryCentroids.get(badger.getBadgerGroup()).getCoords(Integer.toString(badger.getDate().get(Calendar.YEAR)));
		}		
		
		double[][] badgerGroupCentroid = new double[1][2];
		
		// Get the main and sampled location for the cattle
		Location cattleMainHerd = null;
		if(cow.getMainHerd() != null && cattleIsolateLifeHistoryData.getLocations().get(cow.getMainHerd()) != null){
			cattleMainHerd = cattleIsolateLifeHistoryData.getLocations().get(cow.getMainHerd());
		}
		Location cattleSampledHerd = null;
		if(cow.getLocationId() != null){
			cattleSampledHerd = cattleIsolateLifeHistoryData.getLocations().get(cow.getLocationId());
		}
		
		//**** Make the comparison ****
		
		// Did these two cattle share the same main/sampled/infected herd?
		epiMetrics[0] = 0;
		epiMetrics[1] = 0;
		epiMetrics[2] = 0;
		
		// How much time were these cattle alive/infected/in the same herd together?
		epiMetrics[3] = MakeEpidemiologicalComparisons.calculateNDaysOverlapped(cow.getStart(), cow.getEnd(), badgerCaptureData.getStart(), badgerCaptureData.getEnd());
		epiMetrics[4] = -1;
		epiMetrics[5] = -1;
		
		// Calculate the time between infection detection/sampling/breakdown
		epiMetrics[6] = CalendarMethods.calculateNDaysBetweenDates(cow.getCultureDate().getTimeInMillis(), badgerCaptureData.getDatesInMilliSeconds()[badgerCaptureData.getWhenInfectionDetected()]);
		epiMetrics[7] = -1;
		epiMetrics[8] = -1;
				
		// Calculate the spatial distance between the Main/Sampled/Infected herds
		epiMetrics[9] = -1;
		if(cattleMainHerd != null && cattleMainHerd.getX() != -1 && cattleMainHerd.getY() != -1 && badgerMainGroupCentroid != null){
			
			epiMetrics[9] = calculateSpatialDistance(cattleMainHerd.getX(), cattleMainHerd.getY(), badgerMainGroupCentroid[0], badgerMainGroupCentroid[1]);
		}
		epiMetrics[10] = -1;
		if(cattleSampledHerd != null && cattleSampledHerd.getX() != -1 && cattleSampledHerd.getY() != -1 && badgerSampledGroupCentroid != null){
			
			epiMetrics[10] = calculateSpatialDistance(cattleSampledHerd.getX(), cattleSampledHerd.getY(), badgerSampledGroupCentroid[0], badgerSampledGroupCentroid[1]);
		}
		epiMetrics[11] = -1;
		
		// Note the number of movements linking the Main/Sampled/Infected herds
		epiMetrics[12] = -1;
		epiMetrics[13] = -1;
		epiMetrics[14] = -1;
		
		// Are these isolates from the same animal?
		epiMetrics[15] = 0;
		
		// Look at shortest path between Main/Sampled/Infected herds/groups?
		epiMetrics[16] = -1;
		epiMetrics[17] = -1;
		epiMetrics[18] = -1;
		epiMetrics[19] = -1;
		epiMetrics[20] = -1;
		epiMetrics[21] = -1;
		
		// Look at the number of animals shared between groups
		epiMetrics[22] = -1;
		epiMetrics[23] = -1;
		epiMetrics[24] = -1;
		
		// Look at shortest path between Main/Sampled/Infected herds/groups excluding certain premises types?
		epiMetrics[25] = -1;
		epiMetrics[26] = -1;
		epiMetrics[27] = -1;
		epiMetrics[28] = -1;
		epiMetrics[29] = -1;
		epiMetrics[30] = -1;
		
		// Calculate the Spatial Distance between Main/Sampled herds/groups using land parcel centroids
		epiMetrics[31] = -1;
		if(cattleMainHerd != null && cattleMainHerd.getLandParcelCentroids() != null && badgerMainGroupCentroid != null){
			
			badgerGroupCentroid[0][0] = badgerMainGroupCentroid[0];
			badgerGroupCentroid[0][1] = badgerMainGroupCentroid[1];
			
			epiMetrics[31] = calculateMinDistanceBetweenCentroidArrays(badgerGroupCentroid, cattleMainHerd.getLandParcelCentroids());
		}
		epiMetrics[32] = -1;
		if(cattleSampledHerd != null && cattleSampledHerd.getLandParcelCentroids() != null && badgerSampledGroupCentroid != null){
			badgerGroupCentroid[0][0] = badgerSampledGroupCentroid[0];
			badgerGroupCentroid[0][1] = badgerSampledGroupCentroid[1];
			
			epiMetrics[32] = calculateMinDistanceBetweenCentroidArrays(badgerGroupCentroid, cattleSampledHerd.getLandParcelCentroids());
		}
		
		// Badger relatedness
		epiMetrics[33] = -1;
		
		return epiMetrics;		
	}
	
	public static double[] makeCattleCattleComparison(IsolateData a, IsolateData b, 
			CattleIsolateLifeHistoryData cattleIsolateLifeHistoryData){
		
		// Initialise the array to store the results of the epidemiological comparison
		double[] epiMetrics = new double[34];
		
		// Get the cattle movement data
		Hashtable<String, Location> locationInfo = cattleIsolateLifeHistoryData.getNetworkInfo().getLocations();
		int[][] locationAdjacencyMatrix = cattleIsolateLifeHistoryData.getNetworkInfo().getAdjacencyMatrix();
		double[][] locationDistanceMatrix = cattleIsolateLifeHistoryData.getNetworkInfo().getSpatialDistanceMatrix();
		Hashtable<Integer, int[][]> shortestPaths = cattleIsolateLifeHistoryData.getNetworkInfo().getShortestPathsFull();
		int[][] nShared = cattleIsolateLifeHistoryData.getNetworkInfo().getNSharedBetweenLocationsMatrix();
		Hashtable<Integer, int[][]> shortestPathsExcludingPremisesTypes = cattleIsolateLifeHistoryData.getNetworkInfo().getShortestPathsWithoutSelectedPremises();
		
		//**** Make the comparison ****
		
		// Did these two cattle share the same main/sampled/infected herd?
		epiMetrics[0] = -1;
		if(a.getMainHerd() != null && b.getMainHerd() != null){
			epiMetrics[0] = CreateDescriptiveEpidemiologicalStats.checkIfInSameGroup(a.getMainHerd(), b.getMainHerd());
		}		
		epiMetrics[1] = CreateDescriptiveEpidemiologicalStats.checkIfInSameGroup(a.getCph(), b.getCph());
		epiMetrics[2] = -1;
		
		// How much time were these cattle alive/infected/in the same herd together?
		epiMetrics[3] = MakeEpidemiologicalComparisons.calculateNDaysOverlapped(a.getStart(), a.getEnd(), b.getStart(), b.getEnd());
		epiMetrics[4] = -1;
		epiMetrics[5] = MakeEpidemiologicalComparisons.calculatePeriodSpentInSameHerd(a.getInfoForHerdsInhabited(), b.getInfoForHerdsInhabited());
		
		// Calculate the time between infection detection/sampling/breakdown
		epiMetrics[6] = -1;
		epiMetrics[7] = CalendarMethods.calculateNDaysBetweenDates(a.getCultureDate(), b.getCultureDate());
		epiMetrics[8] = CalendarMethods.calculateNDaysBetweenDates(a.getBreakdownDate(), b.getBreakdownDate());
				
		// Calculate the spatial distance between the Main/Sampled/Infected herds
		epiMetrics[9] = -1;
		if(a.getMainHerd() != null && b.getMainHerd() != null && 
				a.getMainHerd().matches("") == false && b.getMainHerd().matches("") == false){
				epiMetrics[9] = locationDistanceMatrix[locationInfo.get(a.getMainHerd()).getPosInAdjacencyMatrix()][locationInfo.get(b.getMainHerd()).getPosInAdjacencyMatrix()];
		}
		epiMetrics[10] = -1;
		if(a.getLocationId() != null && b.getLocationId() != null){
			epiMetrics[10] = locationDistanceMatrix[locationInfo.get(a.getLocationId()).getPosInAdjacencyMatrix()][locationInfo.get(b.getLocationId()).getPosInAdjacencyMatrix()];
		}		
		epiMetrics[11] = -1;
		
		// Note the number of movements linking the Main/Sampled/Infected herds
		epiMetrics[12] = -1;
		if(a.getMainHerd() != null && b.getMainHerd() != null &&
				a.getMainHerd().matches("") == false && b.getMainHerd().matches("") == false){
			epiMetrics[12] = locationAdjacencyMatrix[locationInfo.get(a.getMainHerd()).getPosInAdjacencyMatrix()][locationInfo.get(b.getMainHerd()).getPosInAdjacencyMatrix()];
		}
		epiMetrics[13] = -1;
		if(a.getLocationId() != null && b.getLocationId() != null){
			epiMetrics[13] = locationAdjacencyMatrix[locationInfo.get(a.getLocationId()).getPosInAdjacencyMatrix()][locationInfo.get(b.getLocationId()).getPosInAdjacencyMatrix()];
		}		
		epiMetrics[14] = -1;
		
		// Are these isolates from the same animal?
		epiMetrics[15] = 0;
		if(a.getEartag().matches(b.getEartag())){
			epiMetrics[15] = 1;
		}
		
		// Look at the shortest path between the sampled animals Main/Sampled/Infected herds/groups
		int[] indices = new int[0];
		if(a.getMainHerd() != null && b.getMainHerd() != null &&
				a.getMainHerd().matches("") == false && b.getMainHerd().matches("") == false){
			indices = shortestPaths.get(locationInfo.get(a.getMainHerd()).getPosInAdjacencyMatrix())[locationInfo.get(b.getMainHerd()).getPosInAdjacencyMatrix()];
		}
		epiMetrics[16] = -1;
		epiMetrics[17] = -1;
		if(indices.length != 0){
			epiMetrics[16] = indices.length;
			epiMetrics[17] = calculateMeanNMovementsOnEdgesOfShortestPath(indices, locationInfo.get(b.getMainHerd()).getPosInAdjacencyMatrix(), locationAdjacencyMatrix);
		}
		
		indices = new int[0];
		if(a.getLocationId() != null && b.getLocationId() != null){
			indices = shortestPaths.get(locationInfo.get(a.getLocationId()).getPosInAdjacencyMatrix())[locationInfo.get(b.getLocationId()).getPosInAdjacencyMatrix()];
		}		
		epiMetrics[18] = -1;
		epiMetrics[19] = -1;
		if(indices.length != 0){
			epiMetrics[18] = indices.length;
			epiMetrics[19] = calculateMeanNMovementsOnEdgesOfShortestPath(indices, locationInfo.get(b.getLocationId()).getPosInAdjacencyMatrix(), locationAdjacencyMatrix);
		}
		
		epiMetrics[20] = -1;
		epiMetrics[21] = -1;
		
		// Look at the number of animals shared between groups
		epiMetrics[22] = -1;
		if(a.getMainHerd() != null && b.getMainHerd() != null &&
				a.getMainHerd().matches("") == false && b.getMainHerd().matches("") == false){
			epiMetrics[22] = nShared[locationInfo.get(a.getMainHerd()).getPosInAdjacencyMatrix()][locationInfo.get(b.getMainHerd()).getPosInAdjacencyMatrix()];
		}
		epiMetrics[23] = -1;
		if(a.getLocationId() != null && b.getLocationId() != null){
			epiMetrics[23] = nShared[locationInfo.get(a.getLocationId()).getPosInAdjacencyMatrix()][locationInfo.get(b.getLocationId()).getPosInAdjacencyMatrix()];
		}		
		epiMetrics[24] = -1;
		
		// Look at shortest path between Main/Sampled/Infected herds/groups excluding certain premises types?
		indices = new int[0];
		if(a.getMainHerd() != null && b.getMainHerd() != null &&
				a.getMainHerd().matches("") == false && b.getMainHerd().matches("") == false){
			indices = shortestPathsExcludingPremisesTypes.get(locationInfo.get(a.getMainHerd()).getPosInAdjacencyMatrix())[locationInfo.get(b.getMainHerd()).getPosInAdjacencyMatrix()];
		}
		epiMetrics[25] = -1;
		epiMetrics[26] = -1;
		if(indices.length != 0){
			epiMetrics[25] = indices.length;
			epiMetrics[26] = calculateMeanNMovementsOnEdgesOfShortestPath(indices, locationInfo.get(b.getMainHerd()).getPosInAdjacencyMatrix(), locationAdjacencyMatrix);
		}
		
		indices = new int[0];
		if(a.getLocationId() != null && b.getLocationId() != null){
			indices = shortestPaths.get(locationInfo.get(a.getLocationId()).getPosInAdjacencyMatrix())[locationInfo.get(b.getLocationId()).getPosInAdjacencyMatrix()];
		}		
		epiMetrics[27] = -1;
		epiMetrics[28] = -1;
		if(indices.length != 0){
			epiMetrics[27] = indices.length;
			epiMetrics[28] = calculateMeanNMovementsOnEdgesOfShortestPath(indices, locationInfo.get(b.getLocationId()).getPosInAdjacencyMatrix(), locationAdjacencyMatrix);
		}
		
		epiMetrics[29] = -1;
		epiMetrics[30] = -1;
		
		// Calculate the Spatial Distance between Main/Sampled herds/groups using land parcel centroids
		epiMetrics[31] = -1;
		if(a.getMainHerd() != null && b.getMainHerd() != null &&
				a.getMainHerd().matches("") == false && b.getMainHerd().matches("") == false &&
				locationInfo.get(a.getMainHerd()).getLandParcelCentroids() != null &&
				locationInfo.get(b.getMainHerd()).getLandParcelCentroids() != null){
			epiMetrics[31] = calculateMinDistanceBetweenCentroidArrays(
					locationInfo.get(a.getMainHerd()).getLandParcelCentroids(), 
					locationInfo.get(b.getMainHerd()).getLandParcelCentroids());
		}
		epiMetrics[32] = -1;
		if(a.getLocationId() != null && b.getLocationId() != null &&
		   locationInfo.get(a.getLocationId()).getLandParcelCentroids() != null &&
		   locationInfo.get(b.getLocationId()).getLandParcelCentroids() != null){
			
			epiMetrics[32] = epiMetrics[31] = calculateMinDistanceBetweenCentroidArrays(
					locationInfo.get(a.getLocationId()).getLandParcelCentroids(), 
					locationInfo.get(b.getLocationId()).getLandParcelCentroids());
		}
		
		// Badger relatedness
		epiMetrics[33] = -1;
		
		return epiMetrics;		
	}
	
	public static double[] makeBadgerBadgerComparison(SampleInfo a, SampleInfo b,
			CapturedBadgerLifeHistoryData badgerIsolateLifeHistoryData){
		
		// Initialise the array to store the results of the epidemiological comparison
		double[] epiMetrics = new double[34];
		
		// Get the social group territory centroids data
		Hashtable<String, TerritoryCentroids> territoryCentroids = badgerIsolateLifeHistoryData.getTerritoryCentroids();
		
		// Get the host relatedness matrix
		double[][] hostRelatenessMatrix = badgerIsolateLifeHistoryData.getGeneticRelatedness();
		
		// Get the life history data
		CaptureData aCaptureData = badgerIsolateLifeHistoryData.getBadgerCaptureHistories().get(a.getTattoo());
		CaptureData bCaptureData = badgerIsolateLifeHistoryData.getBadgerCaptureHistories().get(b.getTattoo());
		
		// Get the badger group information
		Hashtable<String, Integer> badgerGroupIndices = badgerIsolateLifeHistoryData.getBadgerGroupIndices();
		int[][] groupAdjacencyMatrix = badgerIsolateLifeHistoryData.getGroupAdjacencyMatrix();
		Hashtable<Integer, int[][]> shortestPaths = badgerIsolateLifeHistoryData.getShortestPathsBetweenGroups();
		int[][] nShared = badgerIsolateLifeHistoryData.getNSharedBadgersBetweenGroups();
		
		//**** Make the comparison ****
		
		// Did these two badgers share the same main/sampled/infected group?
		epiMetrics[0] = CreateDescriptiveEpidemiologicalStats.checkIfInSameGroup(aCaptureData.getMainGroup(), bCaptureData.getMainGroup());
		epiMetrics[1] = CreateDescriptiveEpidemiologicalStats.checkIfInSameGroup(a.getBadgerGroup(), b.getBadgerGroup());
		epiMetrics[2] = CreateDescriptiveEpidemiologicalStats.checkIfInSameGroup(aCaptureData.getGroupWhenFirstInfected(), bCaptureData.getGroupWhenFirstInfected());
		
		// How much time were these badgers alive/infected/in-the-same-group together?
		epiMetrics[3] = MakeEpidemiologicalComparisons.calculateNDaysOverlapped(aCaptureData.getStart(), aCaptureData.getEnd(), bCaptureData.getStart(), bCaptureData.getEnd());
		epiMetrics[4] = MakeEpidemiologicalComparisons.calculateNDaysOverlapped(aCaptureData.getDatesInMilliSeconds()[aCaptureData.getWhenInfectionDetected()], aCaptureData.getEnd(), bCaptureData.getDatesInMilliSeconds()[bCaptureData.getWhenInfectionDetected()], bCaptureData.getEnd());
		epiMetrics[5] = CreateDescriptiveEpidemiologicalStats.findPeriodSpentInSameGroup(aCaptureData.getPeriodsInEachGroup(), bCaptureData.getPeriodsInEachGroup());
		
		// Calculate the time between infection detection/sampling/breakdown
		epiMetrics[6] = CalendarMethods.calculateNDaysBetweenDates(aCaptureData.getDatesInMilliSeconds()[aCaptureData.getWhenInfectionDetected()], bCaptureData.getDatesInMilliSeconds()[bCaptureData.getWhenInfectionDetected()]);
		epiMetrics[7] = CalendarMethods.calculateNDaysBetweenDates(a.getDate(), b.getDate());
		epiMetrics[8] = -1;
		
		// Calculate the spatial distance between the Main/Sampled/Infected Groups
		epiMetrics[9] = CreateDescriptiveEpidemiologicalStats.calculateSpatialDistanceBetweenGroups(
				aCaptureData.getMainGroup(), bCaptureData.getMainGroup(), 
				-1, -1, territoryCentroids);
		epiMetrics[10] = CreateDescriptiveEpidemiologicalStats.calculateSpatialDistanceBetweenGroups(
				a.getBadgerGroup(), b.getBadgerGroup(), 
				a.getDate().get(Calendar.YEAR),
				b.getDate().get(Calendar.YEAR),
				territoryCentroids);
		epiMetrics[11] = CreateDescriptiveEpidemiologicalStats.calculateSpatialDistanceBetweenGroups(
				aCaptureData.getGroupWhenFirstInfected(), bCaptureData.getGroupWhenFirstInfected(), 
				aCaptureData.getCaptureDates()[aCaptureData.getWhenInfectionDetected()].get(Calendar.YEAR),
				bCaptureData.getCaptureDates()[bCaptureData.getWhenInfectionDetected()].get(Calendar.YEAR),
				territoryCentroids);
		
		// Note the number of movements linking the Main/Sampled/Infected Groups
		epiMetrics[12] = groupAdjacencyMatrix[badgerGroupIndices.get(aCaptureData.getMainGroup())][badgerGroupIndices.get(bCaptureData.getMainGroup())];
		epiMetrics[13] = groupAdjacencyMatrix[badgerGroupIndices.get(a.getBadgerGroup())][badgerGroupIndices.get(b.getBadgerGroup())];
		epiMetrics[14] = groupAdjacencyMatrix[badgerGroupIndices.get(aCaptureData.getGroupWhenFirstInfected())][badgerGroupIndices.get(bCaptureData.getGroupWhenFirstInfected())];
		
		// Are these isolates from the same animal?
		epiMetrics[15] = 0;
		if(a.getTattoo().matches(b.getTattoo())){
			epiMetrics[15] = 1;
		}
		
		// Look at the shortest path between the sampled animals Main/Sampled/Infected herds/groups
		int[] indices = shortestPaths.get(badgerGroupIndices.get(aCaptureData.getMainGroup()))[badgerGroupIndices.get(bCaptureData.getMainGroup())];
		epiMetrics[16] = -1;
		epiMetrics[17] = -1;
		if(indices.length != 0){
			epiMetrics[16] = indices.length;
			epiMetrics[17] = calculateMeanNMovementsOnEdgesOfShortestPath(indices, badgerGroupIndices.get(bCaptureData.getMainGroup()), groupAdjacencyMatrix);
		}
		
		indices = shortestPaths.get(badgerGroupIndices.get(a.getBadgerGroup()))[badgerGroupIndices.get(b.getBadgerGroup())];
		epiMetrics[18] = -1;
		epiMetrics[19] = -1;
		if(indices.length != 0){
			epiMetrics[18] = indices.length;
			epiMetrics[19] = calculateMeanNMovementsOnEdgesOfShortestPath(indices, badgerGroupIndices.get(b.getBadgerGroup()), groupAdjacencyMatrix);
		}
		
		indices = shortestPaths.get(badgerGroupIndices.get(aCaptureData.getGroupWhenFirstInfected()))[badgerGroupIndices.get(bCaptureData.getGroupWhenFirstInfected())];
		epiMetrics[20] = -1;
		epiMetrics[21] = -1;
		if(indices.length != 0){
			epiMetrics[20] = indices.length;
			epiMetrics[21] = calculateMeanNMovementsOnEdgesOfShortestPath(indices, badgerGroupIndices.get(bCaptureData.getGroupWhenFirstInfected()), groupAdjacencyMatrix);
		}
		
		// Note the number of animals shared between the Main/Sampled/Infected Groups
		epiMetrics[22] = nShared[badgerGroupIndices.get(aCaptureData.getMainGroup())][badgerGroupIndices.get(bCaptureData.getMainGroup())];
		epiMetrics[23] = nShared[badgerGroupIndices.get(a.getBadgerGroup())][badgerGroupIndices.get(b.getBadgerGroup())];
		epiMetrics[24] = nShared[badgerGroupIndices.get(aCaptureData.getGroupWhenFirstInfected())][badgerGroupIndices.get(bCaptureData.getGroupWhenFirstInfected())];
		
		// Look at shortest path between Main/Sampled/Infected herds/groups excluding certain premises types?
		epiMetrics[25] = -1;
		epiMetrics[26] = -1;
		epiMetrics[27] = -1;
		epiMetrics[28] = -1;
		epiMetrics[29] = -1;
		epiMetrics[30] = -1;
		
		// Calculate the Spatial Distance between Main/Sampled/Infected herds/groups using land parcel centroids
		epiMetrics[31] = -1;
		epiMetrics[32] = -1;
		
		// Note the relatedness of the current badgers
		epiMetrics[33] = CreateDescriptiveEpidemiologicalStats.getRelatedness(aCaptureData, bCaptureData, hostRelatenessMatrix);
		
		return epiMetrics;
	}
	
	public static void setIsolateIds(Sequence[] sequences){
		
		// Initialise the necessary variables for parsing the file name
		String[] parts;
		char species;
		
		// Examine each of the sequence names (file names) and extract the isolate ID
		for(int i = 0; i < sequences.length; i++){
			
			parts = sequences[i].getName().split("_");

			sequences[i].setName(parts[0]);
			
			// Note what species this isolate was taken from
			species = 'C';
			if(parts[0].matches("WB(.*)") == true){
				species = 'B';
			}
			sequences[i].setSpecies(species);
		}
	}

	// Land Parcel Centroids
	public static void getLocationCentroidInformation(String landParcelCentroidsFile,
			Hashtable<String, Location> locations) throws IOException{
		
		/**
		 * Land Parcel Centroids file structure:
		 * X	Y	SH_MAP_REF	FIELD_NO	PARCEL_ID	LP_MAP_REF	POLYGON_ID	AGREE_AREA	HECTARAGE	LFA	LP_TO
		 * 0	1	2			3			4			5			6			7			8			9	10
		 * 
		 * LP_FROM	LP_PU_FROM	LP_PU_TO	PU_FROM	PU_TO	PU_MAP_REF	PU_CPH	PU_HOLD_TY	POB_TENURE	POB_CPH
		 * 11		12			13			14		15		16			17		18			19			20
		 * 
		 * SCHEME	POB_INV_FR	POB_INV_TO	YEAR	USE_CD_YR	USE_CD_03	TOT_SIZE	AREA_EST	AREA_ACT	
		 * 21		22			23			24		25			26			27			28			29
		 * 
		 * AREA_CLAIM	SP5_CPH	SRC_SYSTEM	ETL_RUN_NO
		 * 30			31		32			33
		 * 
		 * POB: Place of Business
		 * PU: Production Unit		- Using this to link to locations
		 * SP5: CPH used on claim form
		 * 
		 * USE the X and Y British grid coordinates!!
		 * 
		 * X = X
		 * Y = Y
		 */
		
		// Open the input file
		InputStream input = new FileInputStream(landParcelCentroidsFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Note the CPHs for the Locations
		Hashtable<String, String> cphLocationLink = linkCPHsToLocationIds(locations);
		
		// Initialise an array to store the X and Y of each land parcel centroid
		double[] xy = new double[2];
		String cph;
		
		// Initialise variables necessary for parsing the file
		String line = null;
		int lineNo = 0;
		String[] cols;
				
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			lineNo++;
			
			// Skip the header and last line
			if(lineNo == 1){
				continue;
			}
			
			// Split the current line into it's columns
			cols = line.split(",");
			
			// Check that a CPH exists
			if(cols[17].matches("") == false && cols[17].matches("//") == false){
			
				// Parse the CPH to remove the separator
				cph = parseLandParcelCPH(cols[17]);
						
				// Check if CPH for the current land parcel centroid is one we're interested in
				if(cphLocationLink.get(cph) != null){
				
					// Get the latitude and longitude of the land parcel centroid
					xy[0] = Double.parseDouble(cols[0]);
					xy[1] = Double.parseDouble(cols[1]);
					
					// Add the centroid information to the current location
					locations.get(cphLocationLink.get(cph)).appendLandParcelCentroid(xy);
				}
			}
		}
		
		// Close the current movements file
		input.close();
		reader.close();
		
		// For how  many of the locations did we find centroid data?
		int count = 0;
		for(String key : HashtableMethods.getKeysString(locations)){
			
			if(locations.get(key).getLandParcelCentroids() != null){
				count++;
			}
		}
		//System.out.println("Found centroid data for " + count + " locations. Out of " + locations.size() + " locations.");
	}
	
	public static String parseLandParcelCPH(String cph){
		String[] parts = cph.split("/");
		
		return parts[0] + "" + parts[1] + "" + parts[2];
	}

	public static Hashtable<String, String> linkCPHsToLocationIds(Hashtable<String, Location> locations){
		
		// Initialise a hashtable to store the location ID associated with each CPH
		Hashtable<String, String> idsForCPHs = new Hashtable<String, String>();
		
		// Examine each location
		Location location;
		for(String id : HashtableMethods.getKeysString(locations)){
			
			// Get the information for the current location
			location = locations.get(id);
			
			// Skip locations that don't have a CPH
			if(location.getCph().matches("null")){
				continue;
			}
			
			// Does the current CPH already have an ID?
			if(idsForCPHs.get(location.getCph()) != null){
				System.out.println(location.getCph() + " has multiple Location IDs: " + id + "\t" + idsForCPHs.get(location.getCph()));
			}else{
				idsForCPHs.put(location.getCph(), id);
			}
		}
		
		return idsForCPHs;
	}

	public static double calculateMinDistanceBetweenCentroidArrays(double[][] a, double[][] b){
		
		// Initialise a variable to store the minimum distance
		double min = 99999999;
		double distance;
		
		// Calculate the distance between all centroids
		for(int i = 0; i < a.length; i++){
			for(int j = 0; j < b.length; j++){
				
				distance = Math.pow(b[j][0] - a[i][0], 2);
				distance += Math.pow(b[j][1] - a[i][1], 2);
				distance = Math.sqrt(distance);
				
				if(distance < min){
					min = distance;
				}
			}
		}
		
		return min;
	}
	
	// Shortest Path Methods
	public static Hashtable<Integer, int[][]> findShortestPathsBetweenAllNodesExcludePremiseTypes(
			int[][] adjacencyMatrix, Hashtable<String, Location> locations, String[] premisesTypesToIgnore){
		
		// Create an adjacency matrix where the links to the Premises to ignore have been removed
		int[][] linksRemoved = removeLinksToPremisesTypes(adjacencyMatrix, locations, premisesTypesToIgnore);
		
		// Calculate the shortest paths between all nodes
		return findShortestPathsBetweenAllNodes(linksRemoved);
	}
	
	public static int[][] removeLinksToPremisesTypes(int[][] adjacencyMatrix,
			Hashtable<String, Location> locations, String[] premisesTypesToIgnore){
		
		// Convert the array to a hashtable
		Hashtable<String, Integer> typesToIgnore = HashtableMethods.indexArray(premisesTypesToIgnore);
		
		// Copy the Adjacency matrix
		int[][] copy = MatrixMethods.copy(adjacencyMatrix);
		
		// Examine each of the locations
		Location location;
		for(String id : HashtableMethods.getKeysString(locations)){
			
			// Get the location information
			location = locations.get(id);
			
			// Check that location is in adjacency matrix - i.e. has been sampled
			if(locations.get(id).getPosInAdjacencyMatrix() == -1){
				continue;
			}
			
			// Check if the location is not an Agricultural Holding
			if(typesToIgnore.get(location.getPremisesType()) != null){
				
				// Remove the links to this location - SlaughterHouse or Collection Centre
				removeLinks(locations.get(id).getPosInAdjacencyMatrix(), copy);
				//System.out.println("Removed links: " + location.getLocationId() + "\t" + location.getPremisesType());
			}			
		}
		return copy;
	}
	
	public static void removeLinks(int index, int[][] adjacencyMatrix){
		
		// Remove all the links from the index
		adjacencyMatrix[index] = ArrayMethods.repeat(0, adjacencyMatrix[index].length);
		
		// Remove all the links to the index
		for(int row = 0; row < adjacencyMatrix.length; row++){
			adjacencyMatrix[row][index] = 0;
		}
	}
	
	public static Hashtable<Integer, int[][]> findShortestPathsBetweenAllNodes(int[][] adjacencyMatrix){
		
		// Initialise a Hashtable to store the paths from each node to every other node
		Hashtable<Integer, int[][]> shortestPaths = new Hashtable<Integer, int[][]>();
		
		// Get the shortest paths from all nodes to all other nodes
		for(int i = 0; i < adjacencyMatrix.length; i++){
			shortestPaths.put(i, findShortestPathsFromNode(i,adjacencyMatrix));
		}
		
		return shortestPaths;
	}
	
	public static int[][] findShortestPathsFromNode(int a, int[][] adjacencyMatrix){
		
		/**
		 * Dijkstra's algorithm
		 * 1. Assign to every node a tentative distance value: set it to zero for our initial node and to 
		 * 	  infinity for all other nodes
		 * 2. Set the initial node as current. Mark all other nodes unvisited. Create a set of all the unvisited 
		 * 	  nodes called the unvisited set
		 * 3. For the current node, consider all of its unvisited neighbours and calculate their tentative 
		 * 	  distances. Compare the newly calculated tentative distance to the current assigned value and
		 * 	  assign the smaller one
		 * 4. When we are done considering all of the neighbours of the current node, mark the current node as
		 * 	  visited and remove it from the unvisited set
		 * 5. If the destination node has been visited, then stop
		 * 6. Otherwise select the unvisited node that is marked with the smallest tentative distance, set it as
		 * 	  the new current node and go back to step 3
		 */
		
		// Initialise a Hashtable to note which nodes have been visited
		Hashtable<Integer, Integer> visited = new Hashtable<Integer, Integer>();
		
		// Initialise a Hashtable to store the tentative path to each node
		int[][] pathToNodes = new int[adjacencyMatrix.length][0];
		
		// Make the source node the current node
		int currentNode = a;
		
		// Start looking for the shortest path
		while(currentNode != -1){
			
			// Examine each of the connections to the currentNode
			for(int i = 0; i < adjacencyMatrix.length; i++){
				
				// Ignore individuals if there is no connection and any that have been visited
				if(adjacencyMatrix[currentNode][i] + adjacencyMatrix[i][currentNode] == 0 || visited.get(i) != null){
					continue;
				}
				
				// Check if the length of the path to the current neighbour from the source is short through the 
				// current individual
				if(pathToNodes[currentNode].length + 1 < pathToNodes[i].length || pathToNodes[i].length == 0){
					
					pathToNodes[i] = ArrayMethods.append(pathToNodes[currentNode], currentNode);
				}
			}
			
			// Note the next currentNode
			visited.put(currentNode, 1);
			currentNode = getNextNode(pathToNodes, visited);			
		}
		
		return pathToNodes;		
	}
	
	public static int getNextNode(int[][] pathToNodes, Hashtable<Integer, Integer> visited){
		
		// Of the nodes that have not yet been visited which one has the shortest path length to the source?
		
		int min = 999999;
		int index = -1;
		for(int i = 0; i < pathToNodes.length; i++){
			
			if(pathToNodes[i].length != 0 && pathToNodes[i].length < min && visited.get(i) == null){
				index = i;
				min = pathToNodes[i].length;
			}
		}
		
		return index;
	}
	
	public static double calculateMeanNMovementsOnEdgesOfShortestPath(int[] shortestPath, int sink,
			int[][] adjacencyMatrix){
		
		/**
		 *  The Shortest path lists the indices of the nodes that are on the path from the source (included) to
		 *  the sink (not included).
		 *  Here we calculate the mean number of movements on each edge of this path
		 */

		// Initialise a variable for calculate the mean
		double mean = 0;
		
		// Examine each of the edges on the path from the source to the sink
		for(int i = 0; i < shortestPath.length; i++){
			
			if(i + 1 < shortestPath.length){
				
				mean += adjacencyMatrix[shortestPath[i]][shortestPath[i + 1]];
				mean += adjacencyMatrix[shortestPath[i + 1]][shortestPath[i]];
			}else{
				mean += adjacencyMatrix[shortestPath[i]][sink] + adjacencyMatrix[sink][shortestPath[i]];
			}
		}
		
		// Finish calculating the mean
		mean = mean / (double) shortestPath.length;

		return mean;
	}
}
