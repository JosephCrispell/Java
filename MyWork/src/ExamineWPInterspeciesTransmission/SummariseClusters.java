package ExamineWPInterspeciesTransmission;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Random;

import org.uncommons.maths.random.MersenneTwisterRNG;

import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.GeneralMethods;
import methods.HashtableMethods;
import methods.MatrixMethods;
import methods.WriteToFile;

public class SummariseClusters {

	public static void main(String[] args) throws IOException{
		
		// Set the path
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/NewAnalyses_13-07-17/";
		
		// Get the current date
		String date = CalendarMethods.getCurrentDate("dd-MM-yyyy");
		
		//########################################
		//# Read in the Animal Life History Data #
		//########################################
		
		// Open the file containing Life Histories of animals associated with clusters
		String lifeHistoryFile = path + "InterSpeciesClusters/sampledAnimalsLifeHistories_02-10-2017.txt";
		Hashtable<String, LifeHistorySummary> lifeHistories = readLifeHistories(lifeHistoryFile);
		
		// Remove test results that we're not interested in
		Hashtable<String, Integer> testTypesToIgnore = new Hashtable<String, Integer>();
		testTypesToIgnore.put("CT", 1); // Clear
		testTypesToIgnore.put("DC", 1); // Direct Contact
		removeTestInformation(testTypesToIgnore,  lifeHistories);

		// ** Add the Breakdown location coordinates
		//  Get the location information
		String locationsFile = path + "CattleMovementData-Post2001/20160314_joe_cts_locations.csv";
		Hashtable<String, Location> locationInfo = readLocationsTable(locationsFile);
		//  Get the cattle isolate breakdown locations
		String cattleIsolateSamplingInfo = path + "IsolateData/" +
	              "CattleIsolateInfo_LatLongs_plusID_outbreakSize_Coverage_AddedStrainIDs.csv";
		getIsolateBreakdownCphs(lifeHistories, cattleIsolateSamplingInfo, locationInfo);
		
		// ** Add the Land Parcel Centroids of Breakdown herds
		// Get the Land Parcel Centroids
		String centroidsFile = path + "LandParcelData/RPA_CLAD_ASI_CURRENT_SP_ST-SO_NE-SE/" + 
		"RPA_CLAD_ASI_CURRENT_SP_ST-SO_NE-SE_REDUCED_Centroids-XY.csv";
		Hashtable<String, double[][]> landParcelCentroids = readLandParcelCentroidsFile(centroidsFile);
		
		// Add Centroids of Breakdown herds into life histories
		addLandParcelCentroidsOfBreakdownHerdsToLifeHistories(lifeHistories, landParcelCentroids);
		
		// ** Add the sampling location coordinates
		String badgerSamplingInfo = path + "IsolateData/BadgerInfo_08-04-15_LatLongs_XY_Centroids.csv";
		Hashtable<String, String> isolateSampledGroup = getBadgerIsolateSampledHerds(badgerSamplingInfo);
		addBadgerIsolateSampledGroups(isolateSampledGroup, lifeHistories);
		
		// Remove movements to premises we aren't interested in
		Hashtable<String, Integer> premisesTypesToIgnore = new Hashtable<String, Integer>();
		premisesTypesToIgnore.put("SR", 1); // Slaughterhouse Red Meat
		premisesTypesToIgnore.put("SW", 1); // Slaughterhouse White Meat
		premisesTypesToIgnore.put("EX", 1); // Export Assembly Centre
		premisesTypesToIgnore.put("CC", 1); // Collection Centre
		removeMovementsToPremises(premisesTypesToIgnore, lifeHistories);
		
		//######################################################
		//# Get list of in-contact cattle that tested positive #
		//######################################################
		
//		lookForTestPositiveInContactCattle(lifeHistories);
		
		//############################################
		//# Store the Information About the Clusters #
		//############################################		
		
		// Note which animals are associated with which clusters
		boolean buildAdjacency = true;
		boolean directional = true;
		Hashtable<Integer, ClusterSummary> animalsInClusters = assignAnimalsToClusters(lifeHistories,
				buildAdjacency, directional);
		int[] clusters = ArrayMethods.sort(HashtableMethods.getKeysInt(animalsInClusters));

		// Examine the sampled cattle herds from each cluster
//		int[] mansionXY = {380909, 201377};
//		calculateMeanSpatialDistanceOfSampledHerdsToWPAllClusters(animalsInClusters, mansionXY);
		double[] badgerCentre = {381761.7, 200964.3};
		calculateMeanSpatialDistanceOfLandParcelCentroidsOfSampledBreakdownHerdsInClusterToPointAllClusters(
				animalsInClusters, badgerCentre);
		
		//###################################
		//# Print out CLuster Summary table #
		//###################################

//		int[] clustersToSummarise = {0, 1, 2, 3, 4, 5, 6};
//		String clusterSummaryTable = path + "InterSpeciesClusters/clustersSummaryTable_" + date + ".csv";
//		writeClusterSummaryTable(animalsInClusters, clusterSummaryTable, clustersToSummarise, 9464, 2);
		
		//###################################################################
		//# Create Clusters of Randomly Selected Sampled Cattle and Badgers #
		//###################################################################
		
		// Initialise a random number generator
		Random random = new MersenneTwisterRNG();
		
		// Get the ids of the sampled cattle and badgers in the population
		Hashtable<String, String[]> sampled = getIdsOfSampledCattleAndBadgers(lifeHistories);
		
		// Initialise variables to record the number of cattle and badgers present
		int nSampledBadgers;
		int nSampledCattle;
		
		// Set the number of times we want to randomly select isolates per cluster
		int nRepeats = 1000;
		
		// Initialise a hashtable to store the selected animals
		Hashtable<String, LifeHistorySummary> selected;
		
		// Initialise a ClusterSummary object to store the selected animals
		ClusterSummary randomCluster;
		
		// Ignore the final cluster - involving all sampled animals
		int[] clustersToExamine = {0, 1, 2, 3};
		
		// Initialise an array to store the summary metrics describing the original and randomly created clusters
		ClusterSummaryMetrics[] clusterSummaries = new ClusterSummaryMetrics[clustersToExamine.length];
				
		// For each cluster randomly select the same number of sampled cattle and badgers
		for(int cluster : clustersToExamine){
			
			System.out.println("Randomly selecting animals for Cluster: " + cluster);

			// Store the summary metrics describing the original cluster
			clusterSummaries[cluster] = new ClusterSummaryMetrics(animalsInClusters.get(cluster), nRepeats);
				
			// Count how many sampled cattle and badgers are present
			nSampledBadgers = animalsInClusters.get(cluster).getNSampledBadgers();
			nSampledCattle = animalsInClusters.get(cluster).getNSampledCattle();
			
			// Create the Random clusters for comparison
			for(int repeat = 0; repeat < nRepeats; repeat++){
				
				if(repeat % 100 == 0){
					System.out.print(".");
				}				
				
				// Randomly select sampled cattle and badgers
				selected = randomlySelectedSampledCattleAndBadgers(sampled.get("BADGERS"), sampled.get("CATTLE"),
						nSampledBadgers, nSampledCattle, random, lifeHistories);
				
				// Add in the in-contact animals
				addInContactAnimals(selected, lifeHistories);
				
				// Add selected individuals to cluster
				randomCluster = new ClusterSummary(cluster, selected, buildAdjacency, directional);
				
				// Calculate the mean spatial distance of sampled herds to WP
				calculateMeanSpatialDistanceOfLandParcelCentroidsOfSampledBreakdownHerdsInClusterToPoint(
						randomCluster, badgerCentre);
				
				// Store some summary metrics
				clusterSummaries[cluster].addSummaryMetricsForCluster(randomCluster);				
			}
			System.out.println();
		}
		
		//###########################################################################
		//# Print out a Summary of each Cluster and it's associated Random Clusters #
		//###########################################################################
		
		// Create a file
		String outputFile = path + "InterSpeciesClusters/ClusterSummaryWithRandomNullDistributions_" + date + ".txt";
		writeClusterSummariesWithNullsToFile(outputFile, clusterSummaries);
	}
	
	public static void lookForTestPositiveInContactCattle(Hashtable<String, LifeHistorySummary> lifeHistories){
		
		// Initialise an object to store each animal's life history
		LifeHistorySummary lifeHistory;
		
		// Examine each animal
		for(String animalID : HashtableMethods.getKeysString(lifeHistories)){
			
			// Get the current animal's life history
			lifeHistory = lifeHistories.get(animalID);
			
			// Skip badgers and sampled
			if(lifeHistory.getSpecies() == "BADGER" || lifeHistory.sampled() == true){
				continue;
			}
			
			// Check if ever tested positive
			if(checkTestResultsForPositives(lifeHistory.getTestResults()) == true){
				System.out.println(lifeHistory.getAnimalId() + "\t" + 
						ArrayMethods.toString(lifeHistory.getTestResults(), ",") + "\t" + 
						CalendarMethods.toString(lifeHistory.getTestDates(), "-", ","));
			}
		}
	}
	
	public static boolean checkTestResultsForPositives(String[] testResults){
		
		boolean result = false;
		if(testResults != null){
			for(int i = 0; i < testResults.length; i++){
				if(testResults[i].matches("R") == true || testResults[i].matches("SL") == true){
					result = true;
					break;
				}
			}
		}
		
		return result;
	}
	
	public static void addLandParcelCentroidsOfBreakdownHerdsToLifeHistories(
			Hashtable<String, LifeHistorySummary> lifeHistories, Hashtable<String, double[][]> landParcelCentroids){
		
		// Examine the life histories of each animal
		for(String id : HashtableMethods.getKeysString(lifeHistories)){
			
			// Skip badgers or animals with no breakdown CPH
			if(lifeHistories.get(id).getSpecies() == "BADGER" || lifeHistories.get(id).getBreakdownCph() == null){
				continue;
			}
			
			// Check if breakdown CPH exists in land parcel information
			if(landParcelCentroids.get(lifeHistories.get(id).getBreakdownCph()) != null){
				
				// Add the centroids of each of the land parcels associated with the breakdown CPH
				lifeHistories.get(id).setBreakdownCphCentroids(
						landParcelCentroids.get(lifeHistories.get(id).getBreakdownCph()));
			}
		}		
	}
	
	public static Hashtable<String, double[][]> readLandParcelCentroidsFile(String fileName) throws IOException{
		/**
		 * Land parcel centroids file structure:
		 * 	X	Y	SH_MAP_REF	FIELD_NO	PARCEL_ID	LP_MAP_REF	POLYGON_ID	AGREE_AREA	HECTARAGE	LFA	LP_TO
		 * 	0	1	2			3			4			5			6			7			8			9	10
		 * 
		 * 	LP_FROM	LP_PU_FROM	LP_PU_TO	PU_FROM	PU_TO	PU_MAP_REF	PU_CPH	PU_HOLD_TY	POB_TENURE	POB_CPH	SCHEME
		 * 	11		12			13			14		15		16			17		18			19			20
		 * 
		 * 	POB_INV_FR	POB_INV_TO	YEAR	USE_CD_YR	USE_CD_03	TOT_SIZE	AREA_EST	AREA_ACT	AREA_CLAIM
		 * 	21			22			23		24			25			26			27			28			29
		 * 
		 * 	SP5_CPH	SRC_SYSTEM	ETL_RUN_NO
		 * 	30		31			32
		 */
			
		// Initialise a hashtable to store the centroids associated with each CPH
		Hashtable<String, double[][]> cphCentroids = new Hashtable<String, double[][]>();
		
		// Open the input file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
		double[] coords = new double[2];
		String cph;
		
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			
			// Skip the header line
			if(line.matches("X,Y(.*)") == true){
				continue;
			}
			
			// Split the current line into its columns
			cols = line.split(",", -1);
			
			// Parse the cph
			cph = GeneralMethods.removeDelimiter(cols[17], "/");
			
			// Reset the coordinates
			coords = new double[2];
			
			// Have we encountered the current CPH before?
			if(cphCentroids.get(cph) != null){
				
				// Add the current centroid
				coords[0] = Double.parseDouble(cols[0]);
				coords[1] = Double.parseDouble(cols[1]);
				cphCentroids.put(cph, MatrixMethods.addRow(cphCentroids.get(cph), coords));
				
			}else{
				
				coords[0] = Double.parseDouble(cols[0]);
				coords[1] = Double.parseDouble(cols[1]);
				cphCentroids.put(cph, MatrixMethods.addRow(new double[0][2], coords));
			}
		}
		
		// Close the input file
		input.close();
		reader.close();
		
		return cphCentroids;
	}
	
 	public static void writeClusterSummaryTable(Hashtable<Integer, ClusterSummary> animalsInClusters, String fileName,
			int[] clusters, double fastaLength, double decimalPlaces) throws IOException{
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Write a header into the output file
		String line = ",Cluster-" + clusters[0];
		for(int i = 1; i < clusters.length; i++){
			line += ",Cluster-" + clusters[i];
		}
		
		// Write out the table information
		// Counts:
		WriteToFile.writeLn(bWriter, line);
		line = "Number of badgers sampled";
		for(int i = 0; i < clusters.length; i++){
			line += "," + animalsInClusters.get(clusters[i]).getNSampledBadgers();
		}
		WriteToFile.writeLn(bWriter, line);
		line = "Number of cattle sampled";
		for(int i = 0; i < clusters.length; i++){
			line += "," + animalsInClusters.get(clusters[i]).getNSampledCattle();
		}
		WriteToFile.writeLn(bWriter, line);
		line = "Number of in-contact badgers that tested positive";
		for(int i = 0; i < clusters.length; i++){
			line += "," + animalsInClusters.get(clusters[i]).getNUnSampledDetectedBadgers();
		}
		WriteToFile.writeLn(bWriter, line);
		line = "Number of in-contact cattle that tested positive";
		for(int i = 0; i < clusters.length; i++){
			line += "," + animalsInClusters.get(clusters[i]).getNUnSampledDetectedCattle();
		}
		WriteToFile.writeLn(bWriter, line);
		line = "Number of in-contact badgers that NEVER tested positive";
		for(int i = 0; i < clusters.length; i++){
			line += "," + animalsInClusters.get(clusters[i]).getNNegativeBadgers();
		}
		WriteToFile.writeLn(bWriter, line);
		line = "Number of in-contact cattle that NEVER tested positive";
		for(int i = 0; i < clusters.length; i++){
			line += "," + animalsInClusters.get(clusters[i]).getNNegativeCattle();
		}
		WriteToFile.writeLn(bWriter, line);
		
		// Earliest dates
		line = "Earliest date that a sampled badger tested positive";
		for(int i = 0; i < clusters.length; i++){
			line += "," + CalendarMethods.toString(
					animalsInClusters.get(clusters[i]).getEarliestDateSampledBadgerTestedPositive(), "-");
		}
		WriteToFile.writeLn(bWriter, line);
		line = "Earliest date that a sampled cow tested positive";
		for(int i = 0; i < clusters.length; i++){
			line += "," + CalendarMethods.toString(
					animalsInClusters.get(clusters[i]).getEarliestDateSampledCowTestedPositive(), "-");
		}
		WriteToFile.writeLn(bWriter, line);
		line = "Earliest date that an in-contact badger tested positive";
		for(int i = 0; i < clusters.length; i++){
			line += "," + CalendarMethods.toString(
					animalsInClusters.get(clusters[i]).getEarliestDateUnSampledBadgerTestedPositive(), "-");
		}
		WriteToFile.writeLn(bWriter, line);
		line = "Earliest date that an in-contact cow tested positive";
		for(int i = 0; i < clusters.length; i++){
			line += "," + CalendarMethods.toString(
					animalsInClusters.get(clusters[i]).getEarliestDateUnSampledCowTestedPositive(), "-");
		}
		WriteToFile.writeLn(bWriter, line);
		
		// Distance to MRCA
		line = "Minimum patristic distance (SNPs) of the sampled badgers to the MRCA of cluster";
		for(int i = 0; i < clusters.length; i++){
			line += "," + GeneralMethods.round(
					ArrayMethods.min(animalsInClusters.get(clusters[i]).getDistancesToMRCABadgers()) * fastaLength,
					decimalPlaces);
		}
		WriteToFile.writeLn(bWriter, line);
		line = "Minimum patristic distance (SNPs) of the sampled cattle to the MRCA of cluster";
		for(int i = 0; i < clusters.length; i++){
			line += "," + GeneralMethods.round(
					ArrayMethods.min(animalsInClusters.get(clusters[i]).getDistancesToMRCACattle()) * fastaLength, 
					decimalPlaces);
		}
		WriteToFile.writeLn(bWriter, line);
		
		// Spatial distance
		line = "Mean spatial distance (KM) from the sampled herds to Woodchester Park";
		for(int i = 0; i < clusters.length; i++){
			line += "," + GeneralMethods.round(
					animalsInClusters.get(clusters[i]).getMeanSpatialDistanceOfSampledHerdsToWP()/1000, decimalPlaces);
		}
		WriteToFile.writeLn(bWriter, line);
		
		// Network degree
		line = "Mean number of movements of sampled cattle to or from the sampled herds";
		for(int i = 0; i < clusters.length; i++){
			line += "," + GeneralMethods.round(
					animalsInClusters.get(clusters[i]).getMeanDegreesOfSampledHerds()[0], decimalPlaces);
		}
		WriteToFile.writeLn(bWriter, line);
		line = "Mean number of movements of in-contact animals that tested positive to or from the sampled herds";
		for(int i = 0; i < clusters.length; i++){
			line += "," + GeneralMethods.round(
					animalsInClusters.get(clusters[i]).getMeanDegreesOfSampledHerds()[1], decimalPlaces);
		}
		WriteToFile.writeLn(bWriter, line);
		line = "Mean number of movements of in-contact animals that NEVER tested positive to or from the sampled herds";
		for(int i = 0; i < clusters.length; i++){
			line += "," + GeneralMethods.round(
					animalsInClusters.get(clusters[i]).getMeanDegreesOfSampledHerds()[3], decimalPlaces);
		}
		WriteToFile.writeLn(bWriter, line);
		
		// Close the output file
		WriteToFile.close(bWriter);		
		
	}

 	public static void writeClusterSummariesWithNullsToFile(String fileName, ClusterSummaryMetrics[] clusterSummaries) throws IOException{
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Write a header into the output file
		String header = "Cluster";
		header += "\t" + "MeanDistToRefBadgers";
		header += "\t" + "MeanDistToRefCattle";
		header += "\t" + "MeanSeqQualBadgers";
		header += "\t" + "MeanSeqQualCattle";
		
		header += "\t" + "NBadgersSampled";
		header += "\t" + "NCattleSampled";
		header += "\t" + "NUnSampledDetectedBadgers";
		header += "\t" + "NUnSampledDetectedCattle";
		header += "\t" + "NUnSampledInconclusive";
		header += "\t" + "NNegativeBadgers";
		header += "\t" + "NNegativeCattle";
		
		header += "\t" + "EarliestDetectionDateSampledBadgers";
		header += "\t" + "EarliestDetectionDateSampledCattle";
		header += "\t" + "EarliestDetectionDateUnSampledBadgers";
		header += "\t" + "EarliestDetectionDateUnSampledCattle";
		
		header += "\t" + "MeanSpatialDist";
		
		header += "\t" + "MeanShortestPathLengthBetweenSampledGroups";
		header += "\t" + "MeanShortestPathLengthBetweenSampledHerds";
		header += "\t" + "ProportionShortestPathsBetweenSampledGroupsThatExist";
		header += "\t" + "ProportionShortestPathsBetweenSampledHerdsThatExist";
		
		header += "\t" + "NumberSampledGroups";
		header += "\t" + "NumberSampledHerds";
		
		WriteToFile.writeLn(bWriter, header);
		
		// Initialise a variable to store the next file line
		String line;
		
		// Write the summary metrics for each cluster out to file
		for(int i = 0; i < clusterSummaries.length; i++){
			
			// Build a line combining the summary metrics for the current cluster
			line = i + "\t";
			line += clusterSummaries[i].getMeanDistanceOfIsolatesToRef(0) + "\t";
			line += clusterSummaries[i].getMeanDistanceOfIsolatesToRef(1) + "\t";
			line += clusterSummaries[i].getMeanSequenceQualityOfIsolates(0) + "\t";
			line += clusterSummaries[i].getMeanSequenceQualityOfIsolates(1) + "\t";
			
			line += ArrayMethods.toString(clusterSummaries[i].getNSampled(0), ",") + "\t";
			line += ArrayMethods.toString(clusterSummaries[i].getNSampled(0), ",") + "\t";
			line += ArrayMethods.toString(clusterSummaries[i].getNUnSampledDetected(0), ",") + "\t";
			line += ArrayMethods.toString(clusterSummaries[i].getNUnSampledDetected(1), ",") + "\t";
			line += ArrayMethods.toString(clusterSummaries[i].getNUnSampledInconclusive(), ",") + "\t";
			line += ArrayMethods.toString(clusterSummaries[i].getNNegative(0), ",") + "\t";
			line += ArrayMethods.toString(clusterSummaries[i].getNNegative(1), ",") + "\t";
			
			line += CalendarMethods.toString(clusterSummaries[i].getEarliestDetectionDateOfSampled(0), "-", ",") + "\t";
			line += CalendarMethods.toString(clusterSummaries[i].getEarliestDetectionDateOfSampled(1), "-", ",") + "\t";
			line += CalendarMethods.toString(clusterSummaries[i].getEarliestDetectionDateOfUnSampled(0), "-", ",") + "\t";
			line += CalendarMethods.toString(clusterSummaries[i].getEarliestDetectionDateOfUnSampled(1), "-", ",") + "\t";
			
			line += ArrayMethods.toString(clusterSummaries[i].getMeanSpatialDistance(), ",") + "\t";

			line += ArrayMethods.toString(clusterSummaries[i].getMeanShortestPathLength(0), ",") + "\t";
			line += ArrayMethods.toString(clusterSummaries[i].getMeanShortestPathLength(1), ",") + "\t";
			
			line += ArrayMethods.toString(clusterSummaries[i].getProportionShortestPathsThatExist(0), ",") + "\t";
			line += ArrayMethods.toString(clusterSummaries[i].getProportionShortestPathsThatExist(1), ",") + "\t";

			line += ArrayMethods.toString(clusterSummaries[i].getNSampledLocations(0), ",") + "\t";
			line += ArrayMethods.toString(clusterSummaries[i].getNSampledLocations(1), ",");
			
			// Print the line out to file
			WriteToFile.writeLn(bWriter, line);
			
		}		
		
		// Close the output file
		WriteToFile.close(bWriter);		
	}
	
	public static void addInContactAnimals(Hashtable<String, LifeHistorySummary> selected, 
			Hashtable<String, LifeHistorySummary> lifeHistoriesOfAll){
		
		// Initialise a vector to store the ids of the in-contact animals
		String[] inContactIds;
		
		// For each sampled animal randomly selected - add its associated in-contact animals
		for(String selectedAnimalId : HashtableMethods.getKeysString(selected)){
			
			// Does the current animal have any in-contact animals associated with it?
			if(selected.get(selectedAnimalId).hasInContactAnimals() != false){
				
				// Get the ids of the in-contact animals
				inContactIds = selected.get(selectedAnimalId).getInContactAnimals();
				
				// Add the in-contact animal lifeHistories
				for(String inContactId : inContactIds){
					selected.put(inContactId, lifeHistoriesOfAll.get(inContactId));
				}
			}
		}		
	}
	
	public static Hashtable<String, String[]> getIdsOfSampledCattleAndBadgers(
			Hashtable<String, LifeHistorySummary> lifeHistories){
		
		// Initialise arrays to store the sampled cattle and badger Ids
		String[] badgers = new String[lifeHistories.size()];
		int badgerIndex = -1;
		String[] cattle = new String[lifeHistories.size()];
		int cattleIndex = -1;
		
		// Examine each animal life history
		for(String id : HashtableMethods.getKeysString(lifeHistories)){
			
			// Skip unsampled animals
			if(lifeHistories.get(id).sampled() == false){
				continue;
			}
			
			// Is the current animal a badger?
			if(lifeHistories.get(id).getSpecies().matches("BADGER") == true){
				
				badgerIndex++;
				badgers[badgerIndex] = id;
			}else{
				cattleIndex++;
				cattle[cattleIndex] = id;
			}
		}
		
		// Store the arrays of the sampled cattle and badgers
		Hashtable<String, String[]> sampled = new Hashtable<String, String[]>();
		sampled.put("BADGERS", ArrayMethods.subset(badgers, 0, badgerIndex));
		sampled.put("CATTLE", ArrayMethods.subset(cattle, 0, cattleIndex));
		
		return sampled;
	}
	
	public static Hashtable<String, LifeHistorySummary> randomlySelectedSampledCattleAndBadgers(String[] badgers,
			String[] cattle, int nBadgers, int nCattle, Random random,
			Hashtable<String, LifeHistorySummary> lifeHistories){
		
		// Initialise a hashtable to store the randomly selected animals
		Hashtable<String, LifeHistorySummary> selected = new Hashtable<String, LifeHistorySummary>();
		
		// Randomly select the badgers
		String[] selectedBadgerIds = ArrayMethods.randomChoices(badgers, nBadgers, random, false);
		
		// Randomly select the cattle
		String[] selectedCattleIds = ArrayMethods.randomChoices(cattle, nCattle, random, false);
		
		// Add the selected animals to the hashtable
		for(String id : selectedBadgerIds){
			selected.put(id, lifeHistories.get(id));
		}
		for(String id : selectedCattleIds){
			selected.put(id, lifeHistories.get(id));
		}
		
		return selected;
	}
	
	public static void addBadgerIsolateSampledGroups(Hashtable<String, String> isolateSampledGroup,
			Hashtable<String, LifeHistorySummary> lifeHistories){
		
		// Initialise variables to store the social groups that each sampled badger resided in when isolate was taken
		String[] isolates;
		String[] socialGroups;
		
		// Examine each of the sampled animals
		for(String id : HashtableMethods.getKeysString(lifeHistories)){
			
			// Skip cattle
			if(lifeHistories.get(id).getSpecies().matches("COW") == true){
				continue;
			}
			
			// For each badger get its associated isolates
			isolates = lifeHistories.get(id).getIsolateIds();
			socialGroups = new String[isolates.length];
			
			// Note the social group associated with each isolate from the current badger
			for(int i = 0; i < isolates.length; i++){
				socialGroups[i] = isolateSampledGroup.get(isolates[i]);
			}
			
			// Store the badger social groups
			lifeHistories.get(id).setSampledGroups(socialGroups);
		}
		
	}
	
	public static Hashtable<String, String> getBadgerIsolateSampledHerds(String fileName) throws IOException{
		
		/**
		 * Location information file structure:
		 * 	WB_id	CB_id	Batch	tattoo	date	pm	sample	AHVLA_afno	lesions	abscesses	AHVLASpoligo
		 * 	0		1		2		3		4		5	6		7			8		9			10
		 * 
		 * 	Social.Group.Trapped.At	AFBI_VNTRNo	AFBI_String	AFBI_Genotype	AFBI_Spoligotype	AFBI_GenSpol
		 * 	11						12			13			14				15					16
		 * 
		 * 	notes	SampledGrpLat	SampledGrpLong	SampledGrpX	SampledGrpY
		 * 	17		18				19				20			21 	
		 */
		
		// Initialise a hashtable to store the isolate sampled groups
		Hashtable<String, String> isolateSampledGroups = new Hashtable<String, String>();
		
		// Open the input file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
		
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			
			// Skip the header line
			if(line.matches("location_id(.*)") == true){
				continue;
			}

			// Split the current line into its columns
			cols = line.split(",", -1);
			
			// Parse the social group name
			cols[11] = GeneralMethods.removeDelimiter(cols[11], " ");
			
			// Store the isolate ID with the sampled social group name
			isolateSampledGroups.put(cols[0], cols[11]);			
		}
		
		// Close the input file
		input.close();
		reader.close();
		
		return isolateSampledGroups;
	}
	
	public static void calculateMeanSpatialDistanceOfSampledHerdsToWPAllClusters(Hashtable<Integer, ClusterSummary> animalsInClusters, 
			int[] mansionXY){
		
		// Get a list of the clusters
		int[] clusters = HashtableMethods.getKeysInt(animalsInClusters);
		
		// Examine each cluster
		for(int cluster : clusters){
			
			calculateMeanSpatialDistanceOfSampledHerdsInClusterToWP(animalsInClusters.get(cluster), mansionXY);
		}
	}

	public static void calculateMeanSpatialDistanceOfSampledHerdsInClusterToWP(ClusterSummary cluster, 
			int[] mansionXY){
		
		// Reset mean
		double meanSpatialDistance = 0;
		
		// Get the sampled herd information from the current cluster
		Hashtable<String, int[]> sampledCattleHerdLocations = cluster.getBreakdownHerdLocations();
		
		// Examine each sampled herd
		for(String herdId : HashtableMethods.getKeysString(sampledCattleHerdLocations)){
			
			// Calculate the distance from current herd to mansion
			meanSpatialDistance += GeneralMethods.calculateEuclideanDistance(mansionXY, 
					sampledCattleHerdLocations.get(herdId));
		}
		
		// Finish calculating means
		meanSpatialDistance = meanSpatialDistance / (double) sampledCattleHerdLocations.size();
			
		// Store information
		cluster.setMeanSpatialDistanceOfSampledHerdsToWPMansion(meanSpatialDistance);
	}
	
	public static void calculateMeanSpatialDistanceOfLandParcelCentroidsOfSampledBreakdownHerdsInClusterToPointAllClusters(
			Hashtable<Integer, ClusterSummary> animalsInClusters, 
			double[] coordinates){
		
		// Get a list of the clusters
		int[] clusters = HashtableMethods.getKeysInt(animalsInClusters);
		
		// Examine each cluster
		for(int cluster : clusters){
			
			calculateMeanSpatialDistanceOfLandParcelCentroidsOfSampledBreakdownHerdsInClusterToPoint(
					animalsInClusters.get(cluster), coordinates);
		}
	}
	
	public static void calculateMeanSpatialDistanceOfLandParcelCentroidsOfSampledBreakdownHerdsInClusterToPoint(
			ClusterSummary cluster, double[] coordinates){
		
		// Reset mean
		double meanSpatialDistance = 0;
		
		// Get the sampled herd information from the current cluster
		Hashtable<String, double[][]> sampledBreakdownHerdLandParcelCentroids = cluster.getBreakdownHerdCentroids();
		
		// Initialise a counter
		int nCentroids = 0;
		
		// Initialise an array to store the land parcels associated with a particular herd
		double[][] centroids;
		
		// Examine each sampled herd
		for(String cph : HashtableMethods.getKeysString(sampledBreakdownHerdLandParcelCentroids)){
			
			// Get the land parcel centroids associated with the current breakdown herd
			centroids = sampledBreakdownHerdLandParcelCentroids.get(cph);
			
			// Examine each of the centroids
			for(int i = 0; i < centroids.length; i++){
				
				nCentroids++;
				
				// Calculate the distance of the current centroid to point specified
				meanSpatialDistance += GeneralMethods.calculateEuclideanDistance(centroids[i], coordinates);
			}			
		}
		
		// Finish calculating means
		meanSpatialDistance = meanSpatialDistance / (double) nCentroids;
			
		// Store information
		cluster.setMeanSpatialDistanceOfSampledHerdsToWPMansion(meanSpatialDistance);
	}
	
	public static void examineSampledCattleHerdInformationForEachCluster(Hashtable<Integer, ClusterSummary> animalsInClusters, 
			int[] mansionXY){
		
		// Get a list of the clusters
		int[] clusters = HashtableMethods.getKeysInt(animalsInClusters);
		
		// Examine each cluster
		for(int cluster : clusters){
			
			examineSampledCattleHerdInformation(animalsInClusters.get(cluster), mansionXY);
		}
	}
	
	public static void examineSampledCattleHerdInformation(ClusterSummary cluster, int[] mansionXY){
		
		// Reset means
		double[] info = new double[6];
		double meanSpatialDistance = 0;
		int[] coords;
		
		// Get the sampled herd information from the current cluster
		Hashtable<String, int[]> sampledCattleHerdDegree = cluster.getSampledCattleHerdDegree();
		
		// Examine each sampled herd
		for(String herdCoord : HashtableMethods.getKeysString(sampledCattleHerdDegree)){
			
			// Examine the degree for each category
			info = ArrayMethods.add(info, sampledCattleHerdDegree.get(herdCoord));
			
			// Calculate the distance from current herd to mansion
			coords = new int[2];
			coords[0] = sampledCattleHerdDegree.get(herdCoord)[4];
			coords[1] = sampledCattleHerdDegree.get(herdCoord)[5];
			meanSpatialDistance += GeneralMethods.calculateEuclideanDistance(mansionXY, coords);
		}
		
		// Finish calculating means
		info = ArrayMethods.divide(info, (double) sampledCattleHerdDegree.size());
		meanSpatialDistance = meanSpatialDistance / (double) sampledCattleHerdDegree.size();
		
		// Store information
		cluster.setMeanSpatialDistanceOfSampledHerdsToWPMansion(meanSpatialDistance);
		cluster.setMeanDegreesOfSampledHerds(info);
	}
	
	public static void examineSampledBadgerGroupInformation(ClusterSummary cluster){
		
		// Initialise a variable to store the degree of each social group
		double[] info = new double[3];
		
		// Get the sampled social group information from the current cluster
		Hashtable<String, int[]> sampledBadgerGroupDegree = cluster.getSampledBadgerGroupDegree();
		
		// Examine each sampled socialGroup
		for(String herdCoord : HashtableMethods.getKeysString(sampledBadgerGroupDegree)){
			
			// Examine the degree for each category
			info = ArrayMethods.add(info, sampledBadgerGroupDegree.get(herdCoord));

		}
		
		// Finish calculating means
		info = ArrayMethods.divide(info, (double) sampledBadgerGroupDegree.size());
		
		// Store information
		cluster.setMeanDegreesOfSampledGroups(info);
	}
	
	public static void examineSampledBadgerGroupInformationForEachCluster(Hashtable<Integer, ClusterSummary> animalsInClusters){
		
		// Get a list of the clusters
		int[] clusters = HashtableMethods.getKeysInt(animalsInClusters);
		
		// Examine each cluster
		for(int cluster : clusters){
			
			examineSampledBadgerGroupInformation(animalsInClusters.get(cluster));
		}		
	}
	
	public static Hashtable<Integer, ClusterSummary> assignAnimalsToClusters(
			Hashtable<String, LifeHistorySummary> lifeHistories, boolean buildAdjacency, boolean directional){
		
		// Initialise a temporary hashtable to record the animals associated with each cluster
		Hashtable<Integer, String[]> animalsForClusters = new Hashtable<Integer, String[]>();
				
		// Get a list of the animal IDs
		String[] animalIds = HashtableMethods.getKeysString(lifeHistories);
		int[] associatedClusters;
		String[] animals;
		
		// Examine each animal's life history
		for(String id : animalIds){
						
			// Get a list of the clusters that the current animal is associated with
			associatedClusters = new int[0];
			if(lifeHistories.get(id).sampled() == true){
				associatedClusters = ArrayMethods.unique(lifeHistories.get(id).getClusters());
			}else{
				associatedClusters = ArrayMethods.unique(lifeHistories.get(id).getAssociatedClusters());
			}		
			
			// For each of the associated clusters note the current animal's ids
			for(int cluster : associatedClusters){
				
				// Have we encountered this cluster before?
				if(animalsForClusters.get(cluster) != null){
					
					// Append the current animal
					animalsForClusters.put(cluster, 
							ArrayMethods.append(animalsForClusters.get(cluster), id));
				}else{
					animals = new String[1];
					animals[0] = id;
					animalsForClusters.put(cluster, animals);
				}
			}
		}
		
		// Create a cluster object for each cluster
		Hashtable<Integer, ClusterSummary> animalsAssociatedWithClusters = new Hashtable<Integer, ClusterSummary>();
		int[] clusters = HashtableMethods.getKeysInt(animalsForClusters);
		
		Hashtable<String, LifeHistorySummary> animalLifeHistories;
		for(int cluster : clusters){
					
			animals = animalsForClusters.get(cluster);
			animalLifeHistories = new Hashtable<String, LifeHistorySummary>();
			for(int i = 0; i < animals.length; i++){
				
				animalLifeHistories.put(lifeHistories.get(animals[i]).getAnimalId(), lifeHistories.get(animals[i]));
			}
			
			animalsAssociatedWithClusters.put(cluster, new ClusterSummary(cluster, animalLifeHistories, buildAdjacency, directional));
		}
		
		return animalsAssociatedWithClusters;
	}
	
	public static void removeMovementsToPremises(Hashtable<String, Integer> premisesTypesToIgnore,
			Hashtable<String, LifeHistorySummary> lifeHistories){
		
		// Get a list of all of the animal Ids
		String[] animalIds = HashtableMethods.getKeysString(lifeHistories);
		
		// Initialise a variable to record the premises types
		String[] premisesTypes;
		Hashtable<Integer, Integer> remove;
		
		// Examine each animals life history
		for(String id : animalIds){
			
			// Skip if movement data isn't available
			if(lifeHistories.get(id).movementDataAvailable() == false){
				continue;
			}
			
			// Examine the premises types and note which premises to ignore
			premisesTypes = lifeHistories.get(id).getPremisesTypes();
			remove = new Hashtable<Integer, Integer>();
			for(int i = 0; i < premisesTypes.length; i++){
				if(premisesTypesToIgnore.get(premisesTypes[i]) != null){
					remove.put(i, 1);
				}
			}
						
			// Remove the information from tests to ignore
			if(remove.size() != 0){
				lifeHistories.get(id).removeMovementData(remove);
			}
		}		
	}
	
	public static void getIsolateBreakdownCphs(Hashtable<String, LifeHistorySummary> lifeHistories, 
			String fileName, Hashtable<String, Location> locationInfo) throws IOException{
		
		// Open the input file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
		String[] parts;
		
		// Initialise variables to store the date information
		String eartag;
		String breakdownCph;
		int[] dayMonthYear = {0, 1, 2};
		Location breakdownLocationInfo;
		int breakdownX;
		int breakdownY;
		
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			
			// Skip the header line
			if(line.matches("CPH_10km(.*)") == true){
				continue;
			}

			// Split the current line into it columns
			cols = line.split(",", -1);
			
			// Get the eartag
			eartag = cols[35];
			
			// Get the breakdown ID
			parts = cols[16].split("-");
			breakdownCph = parts[0];
			breakdownCph = breakdownCph.substring(0, breakdownCph.length() - 2);

			// Get the breakdown location
			breakdownX = -1;
			breakdownY = -1;
			if(locationInfo.get(breakdownCph) != null){
				breakdownLocationInfo = locationInfo.get(breakdownCph);
				breakdownX = breakdownLocationInfo.getX();
				breakdownY = breakdownLocationInfo.getY();
			}			
			
			// Store the breakdown Info for the current cow
			if(lifeHistories.get(eartag) != null){
				lifeHistories.get(eartag).setBreakdownInfo(breakdownCph, 
						CalendarMethods.parseDate(parts[1], "/", dayMonthYear, true),
						breakdownX, breakdownY);
			}			
		}
		
		// Close the input file
		input.close();
		reader.close();
	}
	
	public static Hashtable<String, Location> readLocationsTable(String fileName) throws IOException{
		
		/**
		 * Location information file structure:
		 * 	location_id	cph	post_code	map_ref	x	y	holding_type	premises_type
		 * 	0			1	2			3		4	5	6				7
		 */
		
		// Initialise a hashtable to store the location information
		Hashtable<String, Location> locationInfo = new Hashtable<String, Location>();
		
		// Open the input file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
		
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			
			// Skip the header line
			if(line.matches("location_id(.*)") == true){
				continue;
			}

			// Split the current line into its columns
			cols = line.split(",", -1);
			
			// Parse the CPH
			cols[1] = ArrayMethods.toString(cols[1].split("-")[0].split("/"), "");
			
			// Build the current location
			locationInfo.put(cols[1], new Location(cols[0], cols[1], cols[4], cols[5], cols[6], cols[7]));
		}
		
		// Close the input file
		input.close();
		reader.close();

		return locationInfo;
	}
	
	public static void removeTestInformation(Hashtable<String, Integer> testTypesToIgnore, 
			Hashtable<String, LifeHistorySummary> lifeHistories){
		
		// Get a list of all of the animal Ids
		String[] animalIds = HashtableMethods.getKeysString(lifeHistories);
		
		// Initialise a variable to record the test data
		String[] testResults;
		Hashtable<Integer, Integer> remove;
		
		// Examine each animals life history
		for(String id : animalIds){
			
			// Skip if test data isn't available
			if(lifeHistories.get(id).getTestDates() == null){
				continue;
			}
			
			// Examine the test data and note which tests to ignore
			testResults = lifeHistories.get(id).getTestResults();
			remove = new Hashtable<Integer, Integer>();
			for(int i = 0; i < testResults.length; i++){
				if(testTypesToIgnore.get(testResults[i]) != null){
					remove.put(i, 1);
				}
			}
						
			// Remove the information from tests to ignore
			if(remove.size() != 0){
				lifeHistories.get(id).removeTestData(remove);
			}
		}
		
	}
	
	public static LifeHistorySummary storeLifeHistory(String[] cols, Hashtable<String, LifeHistorySummary> lifeHistories){
		
		// Date format
		int[] dayMonthYear = {0, 1, 2};
		
		// Initialise the Life History object
		LifeHistorySummary lifeHistory = new LifeHistorySummary(cols[0], cols[1]); // AnimalId and Species
		
		// Detection date
		if(cols[7].matches("NA") == false){
			lifeHistory.setDetectionDate(CalendarMethods.parseDate(cols[7], "-", dayMonthYear, true));
		}
		
		// Test dates and results
		if(cols[8].matches("NA") == false){
			lifeHistory.setTestInfo(CalendarMethods.parseDates(cols[8].split(","), "-", dayMonthYear, true),
					cols[9].split(","));
		}
		
		// UNSAMPLED - contact information
		if(cols[2].matches("NA")){
			
			// Cluster Associations
			int[] clusters = ArrayMethods.convertToInteger(cols[3].split(","));
			for(int cluster : clusters){
				lifeHistory.addClusterAssociation(cluster);
			}
			
			// Contact Events
			String[] idsOfContactAnimals = cols[16].split(",");
			Calendar[] startDatesOfContacts = CalendarMethods.parseDates(cols[17].split(","), "-", dayMonthYear, true);
			Calendar[] endDatesOfContacts = CalendarMethods.parseDates(cols[18].split(","), "-", dayMonthYear, true);
			String[] contactHerds = cols[19].split(",");
			lifeHistory.setContactInfo(storeContactEventInfo(cols[0], idsOfContactAnimals, 
					startDatesOfContacts, endDatesOfContacts, contactHerds));
			
			// Make of current in-contact animal in lifeHistories of sampleld animals it encountered
			noteInContactAnimalOfSampledAnimals(cols[0], idsOfContactAnimals, lifeHistories);
		
		// SAMPLED - sampling information
		}else{
			
			lifeHistory.setIsolateIds(cols[2].split(","));
			lifeHistory.setSamplingDates(CalendarMethods.parseDates(cols[4].split(","), "-", dayMonthYear, true));
			lifeHistory.setClusters(ArrayMethods.convertToInteger(cols[3].split(",")));
			lifeHistory.setDistancesToRef(ArrayMethods.convertToInteger(cols[5].split(",")));
			lifeHistory.setDistancesToMRCA(ArrayMethods.convert2Double(cols[6].split(",")));
			lifeHistory.setSequencingQualities(ArrayMethods.convert2Double(cols[15].split(",")));
			lifeHistory.setIsolateInfoSubsetted(true);
		}
		
		// Movement Information
		if(cols[10].matches("NA") == false){
			
			// Coordinate data
			String[] Xs = cols[10].split(",");
			String[] Ys = cols[11].split(",");
			double[][] coordinates = new double[Xs.length][2];
			
			for(int i = 0; i < Xs.length; i++){
				if(Xs[i].matches("NA")){
					continue;
				}
				coordinates[i][0] = Double.parseDouble(Xs[i]);
				coordinates[i][1] = Double.parseDouble(Ys[i]);
			}
			lifeHistory.setCoordinates(coordinates);
			
			// MovementDates
			lifeHistory.setMovementDates(CalendarMethods.parseDates(cols[12].split(","), "-", dayMonthYear, true));
			
			// Premises types
			lifeHistory.setPremisesTypes(cols[13].split(","));
			
			// CPHs or group names
			lifeHistory.setGroupIds(cols[14].split(","));
			
			// Note that movements have been recored
			lifeHistory.setMovementsSubsetted(true);
		}
		
		return lifeHistory;	
	}
	
	public static void noteInContactAnimalOfSampledAnimals(String inContactId, String[] sampledAnimalIds, 
			Hashtable<String, LifeHistorySummary> lifeHistories){
		
		for(String sampledAnimalId : sampledAnimalIds){
			
			lifeHistories.get(sampledAnimalId).addInContactAnimal(inContactId);
		}
		
	}

	public static ContactEvent[] storeContactEventInfo(String animalId, String[] contactIds, Calendar[] starts,
			Calendar[] ends, String[] herds){
		
		
		// Initialise a large array to store the contact event information
		ContactEvent[] events = new ContactEvent[contactIds.length];
		int usedPos = -1;
		
		// Initialise variables to individual contact information
		String contactId = contactIds[0];
		Calendar[] contactStarts = {starts[0]};
		Calendar[] contactEnds = {ends[0]};
		String[] contactHerds = {herds[0]};
		double nDays = CalendarMethods.calculateNDaysBetweenDates(starts[0], ends[0]);
		
		// Examine the contact information
		for(int i = 1; i < contactIds.length; i++){
			
			// Is this contact with a new animal?
			if(contactId.matches(contactIds[i]) == false){
				
				// Store the information for the previous in-contact animal
				usedPos++;
				events[usedPos] = new ContactEvent(animalId, contactId, contactStarts, contactEnds, nDays, contactHerds);
				
				// Start a new contact record
				contactId = contactIds[i];
				contactStarts = new Calendar[1];
				contactStarts[0] = starts[i];
				contactEnds = new Calendar[1];
				contactEnds[0] = ends[i];
				contactHerds = new String[1];
				contactHerds[0] = herds[i];
				nDays = CalendarMethods.calculateNDaysBetweenDates(starts[i], ends[i]);
			
			// Otherwise add information to current contact record
			}else{
				
				contactStarts = CalendarMethods.append(contactStarts, starts[i]);
				contactEnds = CalendarMethods.append(contactEnds, ends[i]);
				contactHerds = ArrayMethods.append(contactHerds, herds[i]);
				nDays = nDays + CalendarMethods.calculateNDaysBetweenDates(starts[i], ends[i]);
			}
		}		
		
		// Store the information for the last contact event
		usedPos++;
		events[usedPos] = new ContactEvent(animalId, contactId, contactStarts, contactEnds, nDays, contactHerds);
				
		return ContactEvent.subset(events, 0, usedPos);
	}
	
	public static Hashtable<String, LifeHistorySummary> readLifeHistories(String fileName) throws IOException{
		/**
		 * Life history file structure:
		 * 	AnimalId	Species	Isolates	Clusters	SamplingDates	DistancesToRef
		 * 	0			1		2			3			4				5
		 * 
		 *  DistancesToMRCA	DetectionDate	CattleTestDates	CattleTestResults	Xs	Ys
		 *  6				7				8				9					10	11
		 *  
		 *  MovementDates	PremisesTypes	GroupIds	SequencingQuality	AnimalsEncountered
		 *  12				13				14			15					16
		 *  	
		 *  ContactStartDates	ContactEndDates	ContactHerds
		 *  17					18				19
		 */
		
		// Initialise a hashtable to store the life history of each animal
		Hashtable<String, LifeHistorySummary> lifeHistories = new Hashtable<String, LifeHistorySummary>();
		
		// Open the input file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
		
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			
			// Skip the header line
			if(line.matches("AnimalId(.*)") == true){
				continue;
			}
			
			// Store the current animal's life history
			cols = line.split("\t", -1);
			lifeHistories.put(cols[0], storeLifeHistory(cols, lifeHistories));
		}
		
		// Close the input file
		input.close();
		reader.close();
		
		return lifeHistories;
	}
}
