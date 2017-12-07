package woodchesterBadgers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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
import methods.CalendarMethods;
import methods.GeneralMethods;
import methods.GeneticMethods;
import methods.HashtableMethods;
import methods.MatrixMethods;
import methods.WriteToFile;


import filterSensitivity.DistanceMatrixMethods;
import geneticDistances.Sequence;

public class CreateDescriptiveEpidemiologicalStats {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		
		// Get the date
		String date = CalendarMethods.getCurrentDate("dd-MM-yyyy");
		
		// Set the path
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/NewAnalyses_13-07-17/";
		
		// Read in the badger social group territory centroid information
		String territoryCentroidsFile = path + "BadgerCaptureData/BadgerTerritoryMarkingData/" + 
				"SocialGroupsCentroidsPerYear_16-05-17.txt";
		Hashtable<String, TerritoryCentroids> territoryCentroids = readSocialGroupsCentroidsPerYearFile(territoryCentroidsFile);
		
		// Read the Sample Fasta Sequences
		String fastaFile = path + "vcfFiles/sequences_Prox-10_27-09-2017.fasta";
		Sequence[] sequences = GeneticMethods.readFastaFile(fastaFile);
		
		// Remove cattle
		sequences = removeCattleIsolateSequences(sequences);
		
		// Read in the Sample Information file
		String sampleInfoFile = path + "IsolateData/BadgerInfo_08-04-15_LatLongs_XY_Centroids.csv";
		Hashtable<String, SampleInfo> sampleInfo = CreateDescriptiveEpidemiologicalStats.getSampleInformation(sampleInfoFile);
		
		// Read in the Badger Trapping Data
		String consolidatedCaptureData = path + "BadgerCaptureData/WP_CaptureData_Consolidated_31-07-2017.txt";
		Hashtable<String, CaptureData> badgerCaptureHistories = readConsolidatedBadgerCaptureInfo(consolidatedCaptureData);

		// Read in the badger relatedness matrix
		String relatednessMatrixFile = path + "BadgerRelatedness/GenotypedBadgerRelatedness_ImputationOnly_12-07-17.csv";
		double[][] relatednessMatrix = readBadgerRelatednessFile(relatednessMatrixFile, badgerCaptureHistories);
		
		// Initialise a Hashtable to note isolates to ignore - NOT NEEDED THIS RUN
		Hashtable<String, Integer> ignore = new Hashtable<String, Integer>();
		
		// Get a list of all the badger groups
		Hashtable<String, Integer> badgerGroupIndices = findAllGroups(badgerCaptureHistories);
			
		// Create a Weighted Adjacency Matrix for Inhabited groups and note animals that have inhabited each group
		Hashtable<String, Hashtable<String, Integer>> badgerGroupInhabitants = initialiseHashtableToStoreGroupInhabitants(HashtableMethods.getKeysString(badgerGroupIndices));
		int[][] groupAdjacencyMatrix = createGroupAdjacencyMatrix(badgerGroupIndices, badgerCaptureHistories, badgerGroupInhabitants);
		
		// Compare the Sampled Badgers to one another -> Genetic Distance vs. Epidemiological Distance table produced
		String outputFileName = path + "Mislabelling/Badger-RF-BR/" + "geneticVsEpiTable_" + date + ".txt";
		buildGeneticVsEpiDistanceTable(sequences, sampleInfo, badgerCaptureHistories, badgerGroupIndices,
				groupAdjacencyMatrix, territoryCentroids, outputFileName, ignore, relatednessMatrix);
		
	}
	
	public static Sequence[] removeCattleIsolateSequences(Sequence[] sequences){
		
		// Initialise a hashtable to note the indices of the cattle isolates
		Hashtable<Integer, String> indicesOfIsolatesToRemove = new Hashtable<Integer, String>();
		
		// Initialise a variable to store the parse sequence names
		String isolateId;
		
		// Examine each of the sequences IDs and note the cattle isolate indices
		for(int i = 0; i < sequences.length; i++){
			
			// Parse the Isolate ID out of the sequence name
			isolateId = sequences[i].getName().split("_")[0];
			sequences[i].setName(isolateId);
			
			// Note cattle and reference isolates
			if(isolateId.matches("TB(.*)") == true || isolateId.matches("Ref(.*)") == true){
				indicesOfIsolatesToRemove.put(i, isolateId);
			}			
		}
				
		return Sequence.remove(sequences, indicesOfIsolatesToRemove);
	}
	
	public static double[][] readBadgerRelatednessFile(String fileName, 
			Hashtable<String, CaptureData> badgerCaptureHistories) throws IOException{
		
		// Initialise a matrix to store the relatedness values
		double[][] relatedness = new double[0][0];
		
		// Open file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
				
		// Initialise variables for reading the file
		String line = "";
		String[] cols;
		int lineNo = -1;
		
		while(( line = reader.readLine()) != null){
						
			// Split the current line into its columns
			cols = line.split(",", -1);
			
			// Note the indices of each badger in matrix - from header
			if(line.matches(",(.*)")){
				
				// Set the size of the relatedness matrix
				relatedness = new double[cols.length - 1][cols.length - 1];
				
				// Note first index is for row names - ignore and take from from index
				for(int i = 1; i < cols.length; i++){
					
					if(badgerCaptureHistories.get(cols[i]) != null){
						badgerCaptureHistories.get(cols[i]).setIndexInRelatednessMatrix(i - 1);
					}
				}				
				continue;
			}

			// Note the row number
			lineNo++;
			
			// Store the relatedness values
			for(int i = 1; i < cols.length; i++){
				
				// Break out if hit NA - bottom triangle of matrix filled
				if(cols[i].matches("NA") == false){
					relatedness[lineNo][i - 1] = Double.parseDouble(cols[i]);
					relatedness[i - 1][lineNo] = relatedness[lineNo][i - 1];
				}else{
					break;
				}
				
			}
		}
		
		input.close();
		reader.close();
	
		return relatedness;
	}
	
	public static Hashtable<String, TerritoryCentroids> readSocialGroupsCentroidsPerYearFile(String fileName) throws IOException{
		
		// Open file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
				
		// Initialise a Hashtable to store the social group territory centroids
		Hashtable<String, TerritoryCentroids> groupCentroids = new Hashtable<String, TerritoryCentroids>();
				
		/**
		 * Sample Information File Structure: CSV
		 * 	SocialGroup	2000						2001	...
		 *	Beech		380814.733333333:201180.8	NA:NA	...
		 *	0			1							2		...
		 */
		
		// Initialise variables for reading the file
		String line = "";
		String[] cols;
		String[] years = new String[0];
		
		while(( line = reader.readLine()) != null){
			
			// Split the current line into its columns
			cols = line.split("\t");
			
			// Get number of years from header line
			if(line.matches("SocialGroup(.*)")){
				years = ArrayMethods.subset(cols, 1, cols.length - 1);
				continue;
			}

			// Store the territory centroids available
			groupCentroids.put(cols[0].toUpperCase(), new TerritoryCentroids(cols[0], noteCentroidsForEachYear(years, 
					ArrayMethods.subset(cols, 1, cols.length - 1))));
			
		}
		
		input.close();
		reader.close();
		
		return groupCentroids;
	}
	
	public static Hashtable<String, double[]> noteCentroidsForEachYear(String[] years, String[] values){
		
		// Initialise a hashtable to store the centroid of the current social group's territory in each year
		Hashtable<String, double[]> territoryCentroids = new Hashtable<String, double[]>();
		
		// Examine the values
		for(int i = 0; i < values.length; i++){
			
			// Ignore years where no centroid available and extract coordinates from those that are present
			if(values[i].matches("(.*)NA(.*)") == false){
				
				territoryCentroids.put(years[i], ArrayMethods.convert2Double(values[i].split(":")));
			}			
		}
		
		return territoryCentroids;
	}
	
	public static CapturedBadgerLifeHistoryData collateCaptureBadgerInformation(String sampledIsolateInfo, String consolidatedCaptureData,
			String badgerTerritoryCentroidsFile, boolean latLong, String hostRelatednessFile) throws IOException{
			
		// Read in the Sampling Information file
		Hashtable<String, SampleInfo> sampleInfo = CreateDescriptiveEpidemiologicalStats.getSampleInformation(sampledIsolateInfo);
				
		// Read in the Badger Trapping Data
		Hashtable<String, CaptureData> badgerCaptureHistories = readConsolidatedBadgerCaptureInfo(consolidatedCaptureData);
		
		// Read in the host relatedness data
		double[][] relatednessMatrix = readBadgerRelatednessFile(hostRelatednessFile, badgerCaptureHistories);
		
		// Read in the Badger Group Location Information
		Hashtable<String, TerritoryCentroids> territoryCentroids = readSocialGroupsCentroidsPerYearFile(badgerTerritoryCentroidsFile);
				
		// Get a list of all the badger groups
		Hashtable<String, Integer> badgerGroupIndices = findAllGroups(badgerCaptureHistories);
					
		// Create a Weighted Adjacency Matrix for Inhabited groups
		Hashtable<String, Hashtable<String, Integer>> badgerGroupInhabitants = initialiseHashtableToStoreGroupInhabitants(HashtableMethods.getKeysString(badgerGroupIndices));
		int[][] groupAdjacencyMatrix = createGroupAdjacencyMatrix(badgerGroupIndices, badgerCaptureHistories, badgerGroupInhabitants);
		
		// Count the number of shared animals between groups
		int[][] nShared = countNumberOfSharedBadgersBetweenGroups(badgerGroupIndices, badgerGroupInhabitants);
				
		// Store all the Badger Information
		return new CapturedBadgerLifeHistoryData(badgerCaptureHistories, badgerGroupIndices, groupAdjacencyMatrix,
				sampleInfo, territoryCentroids, nShared, relatednessMatrix);
	}
	
	public static Hashtable<String, Integer> ignoreTimePeriod(Hashtable<String, SampleInfo> sampleInfo, String start, String end,
			Hashtable<String, Integer> ignore){
		
		// Convert the date Strings into Milliseconds
		long startDate = parseDate(start);
		long endDate = parseDate(end);
		long samplingDate;
		
		// Examine each of the isolate's sampling information in turn
		String[] keys = HashtableMethods.getKeysString(sampleInfo);
		
		for(String key : keys){
			
			samplingDate = sampleInfo.get(key).getDate().getTimeInMillis();
						
			// Was the current isolate sampled within the time period being ignored?
			if(samplingDate >= startDate && samplingDate <= endDate){
				
				ignore.put(key, 1);
			}
		}
		
		return ignore;
	}
	
	public static long parseDate(String dateString){
		
		Calendar date = Calendar.getInstance();
		
		String[] parts = dateString.split("/");
	
		date.set(Integer.parseInt(parts[2]),Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
		
		return date.getTimeInMillis();
		
	}
	
	public static String[] getSampleIds(Sequence[] sequences){
		String[] sampleIds = new String[sequences.length];
		
		for(int i = 0; i < sequences.length; i++){
			sampleIds[i] = sequences[i].getName().split("_")[0];
		}
		
		return sampleIds;
	}	

	public static Hashtable<String, Integer> notePoorlyMappedWbIds(String fileName) throws IOException{
		
		// Open the Sample Information File
		InputStream input = new FileInputStream(fileName);
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
			
			ignore.put(wbId, 1);
		}
				
		input.close();
		reader.close();
		
		return ignore;
	}
	
	public static Hashtable<String, Integer> noteSpoligotypes(String[] spoligotypes){
		
		Hashtable<String, Integer> types = new Hashtable<String, Integer>();
		
		for(String spoligotype : spoligotypes){
			types.put(spoligotype, 1);
		}
		
		return types;
	}
	
	public static Hashtable<String, Integer> noteWBidsToIgnore(Hashtable<String, SampleInfo> sampleInfo, String[] spoligotypes, String poorlyMappedFile) throws IOException{
		
		// Initialise Hashtable to store the WBids to be ignored
		Hashtable<String, Integer> ignore = new Hashtable<String, Integer>();
		
		// Note the spoligotypes we are interested in
		Hashtable<String, Integer> types = noteSpoligotypes(spoligotypes);
		
		// Note the poorly mapped samples
		Hashtable<String, Integer> poor = notePoorlyMappedWbIds(poorlyMappedFile);
		
		// Examine each of the Samples
		String[] wbIds = HashtableMethods.getKeysString(sampleInfo);
		for(String id : wbIds){
			
			// Check if WBid sample has a spoligotype we are interested in
			if(spoligotypes.length != 0 && types.get(sampleInfo.get(id).getSpoligotype()) == null){
				
				ignore.put(id, 1);
			}
			
			// Check that WBid isn't associated with a poorly mapped isolate
			if(poor.get(id) != null){
				
				ignore.put(id, 1);
			}
		}
		
		return ignore;
	}
	
	public static Hashtable<String, SampleInfo> getSampleInformation(String sampleInfoFile) throws IOException{
		
		// Open the Sample Information file for reading
		InputStream input = new FileInputStream(sampleInfoFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise a Hashtable to store the information for each sample
		Hashtable<String, SampleInfo> sampleInfo = new Hashtable<String, SampleInfo>();
		
		/**
		 * Sample Information File Structure: CSV
		 * WB_id	CB_id	Batch	tattoo	date	pm	sample	AHVLA_afno	lesions	abscesses	AHVLASpoligo
		 * 0		1		2		3		4		5	6		7			8		9			10
		 * 
		 * Social.Group.Trapped.At	AFBI_VNTRNo	AFBI_String	AFBI_Genotype	AFBI_Spoligotype	AFBI_GenSpol	notes
		 * 11						12			13			14				15					16				17
		 * 
		 * SampledGrpLat	SampledGrpLong
		 * 18				19
		 */
		
		String line = "";
		String[] parts;
		
		while(( line = reader.readLine()) != null){
			
			if(line.matches("WB_id,CB_id(.*)")){
				continue;
			}
			
			parts = line.split(",");
			
			// SampleInfo(String id, String tat, String dateString, String group, String type, String sample, String[] otherInfo)
			sampleInfo.put(parts[0], new SampleInfo(parts[0], parts[3], parts[4], parts[11], parts[15], parts[6], parts));
		}
		
		input.close();
		reader.close();
		
		return sampleInfo;
	}
	
	public static Hashtable<String, Integer> findWBidsToIgnore(String filename) throws IOException{
		// Open the Sample Information File
		InputStream input = new FileInputStream(filename);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
						
		// Initialise a Hashtable to store the Sample Information
		Hashtable<String, Integer> ignore = new Hashtable<String, Integer>();
						
		// Initialise Variables for processing the file lines
		String line = "";
		String[] parts;
						
		// Reader the file
		while(( line = reader.readLine()) != null){
			
			ignore.put(line, 1);
			
		}
		
		input.close();
		reader.close();
		
		return ignore;
	}
	
	public static double calculateSpatialDistanceBetweenGroups(String groupA, String groupB, int yearA,
			int yearB, Hashtable<String, TerritoryCentroids> territoryCentroids){
		
		double distance = -1;
		
		if(groupA.matches("NA") == false && groupB.matches("NA") == false &&
		   territoryCentroids.get(groupA) != null && territoryCentroids.get(groupB) != null){
			
			distance = GeneralMethods.calculateEuclideanDistance(
					territoryCentroids.get(groupA).getCoords(Integer.toString(yearA)),
					territoryCentroids.get(groupB).getCoords(Integer.toString(yearB)));
		}
		
		return distance;
	}
	
	public static double getRelatedness(CaptureData captureInfoA, CaptureData captureInfoB, double[][] relatednessMatrix){
		
		double relatedness = -1;
		
		if(captureInfoA.getIndexInRelatednessMatrix() != -1 && captureInfoB.getIndexInRelatednessMatrix() != -1 ){
			
			relatedness = relatednessMatrix[captureInfoA.getIndexInRelatednessMatrix()][captureInfoB.getIndexInRelatednessMatrix()];
		}
		
		return relatedness;
	}
	
	public static void buildGeneticVsEpiDistanceTable(Sequence[] sequences,
			Hashtable<String, SampleInfo> sampleInfo, Hashtable<String, CaptureData> badgerCaptureHistories,
			Hashtable<String, Integer> badgerGroupIndices, int[][] groupAdjacencyMatrix,
			Hashtable<String, TerritoryCentroids> territoryCentroids, String outputFileName,
			Hashtable<String, Integer> ignore, double[][] hostRelatenessMatrix) throws IOException{
		
		// Get the isolate Ids
		String[] wbIds = getSampleIds(sequences);
		
		// Open the output file for the genetic vs. epi distance table to written to
		BufferedWriter bWriter = WriteToFile.openFile(outputFileName, false);
		
		// Initialise variables available Badger Information
		Sequence sequenceI;
		Sequence sequenceJ;
		SampleInfo sampleInfoI;
		SampleInfo sampleInfoJ;
		CaptureData captureHistoryI;
		CaptureData captureHistoryJ;
		long[] captureDatesMilliI;
		long[] captureDatesMilliJ;
				
		// Initialise a variable to store the genetic distance
		double geneticDistance = 0;
		
		// Initialise variables necessary for comparing Badger pairs - DISCRETE
		int sameMainGroup = 0;
		int sameGroupWhenSampled = 0;
		int sameGroupWhenInfected = 0;
		
		// Initialise variables necessary for comparing Badger pairs - CONTINUOUS
		double periodAliveTogether = 0;
		double periodInfectedTogether = 0;
		double periodSpentInSameGroup = 0;
		
		double timeBetweenInfectionDetection = 0;
		double timeBetweenSamplingEvents = 0;
		
		double spatialDistanceBetweenMainGroups = 0;
		double spatialDistanceBetweenGroups1stInfectedIn = 0;
		double spatialDistanceBetweenGroupsSampledIn = 0;
		
		int noMovementsBetweenMainGroups = 0;
		int noMovementsBetweenGroups1stInfectedIn = 0;
		int noMovementsBetweenGroupsSampledIn = 0;
		
		double relatedness;
		
		// Initialise Strings to store output information
		String header = "GeneticDistance\tSameMainGroup\tSameSampledGroup\tSameGroupWhenInfected\tPeriodAliveTogether\tPeriodInfectedTogether\tPeriodInSameGroup\t";
		header += "TimeBetweenDetection\tTimeBetweenSampling\tDistMainGroups\tDistSampledGroup\tDistGroupWhenInfected\tnoMovementsMainGroups\t";
		header += "noMovementsSampledGroup\tnoMovementsGroupsWhenInfected\tRelatedness\tFROM\tTO";
		
		String output = "";
		
		WriteToFile.writeLn(bWriter, header);
		
		for(int i = 0; i < wbIds.length; i++){
			
			// Skip the WBids we want to ignore
			if(ignore.get(wbIds[i]) != null){
				continue;
			}
					
			// Get the Sequence for Badger I
			sequenceI = sequences[i];
			
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
				
				// Skip the WBids we want to ignore
				if(ignore.get(wbIds[j]) != null){
					continue;
				}
				
				// Get the Sequence for Badger J
				sequenceJ = sequences[j];
				
				// Get the Sample Information for Badger J
				sampleInfoJ = sampleInfo.get(wbIds[j]);
				
				// Get the Capture History for Badger J
				captureHistoryJ = badgerCaptureHistories.get(sampleInfoJ.getTattoo());
				
				captureDatesMilliJ = new long[0];
				if(captureHistoryJ != null){
					captureDatesMilliJ = captureHistoryJ.getDatesInMilliSeconds();
				}
				
				// Find the genetic Distance between the isolates taken from Badger I and J
				geneticDistance = GeneticMethods.calculateNumberDifferencesBetweenSequences(sequenceI.getSequence(), sequenceJ.getSequence());
				
				// DISCRETE comparisons
				sameMainGroup = checkIfInSameGroup(captureHistoryI.getMainGroup(), captureHistoryJ.getMainGroup());
				sameGroupWhenSampled = checkIfInSameGroup(sampleInfoI.getBadgerGroup(), sampleInfoJ.getBadgerGroup());
				sameGroupWhenInfected = checkIfInSameGroup(captureHistoryI.getGroupWhenFirstInfected(), captureHistoryJ.getGroupWhenFirstInfected());
				
				// CONTINUOUS comparisons
				periodAliveTogether = findTimeTogether(captureDatesMilliI[0], captureDatesMilliI[captureDatesMilliI.length - 1], captureDatesMilliJ[0], captureDatesMilliJ[captureDatesMilliJ.length - 1]);
				periodInfectedTogether = findTimeTogether(captureDatesMilliI[captureHistoryI.getWhenInfectionDetected()], captureDatesMilliI[captureDatesMilliI.length - 1], captureDatesMilliJ[captureHistoryJ.getWhenInfectionDetected()], captureDatesMilliJ[captureDatesMilliJ.length - 1]);
				periodSpentInSameGroup = findPeriodSpentInSameGroup(captureHistoryI.getPeriodsInEachGroup(), captureHistoryJ.getPeriodsInEachGroup());
				
				timeBetweenSamplingEvents = StepwiseMatching.calculateTempDistance(sampleInfoI.getDate(), sampleInfoJ.getDate());
				timeBetweenInfectionDetection = Math.abs(captureDatesMilliI[captureHistoryI.getWhenInfectionDetected()] - captureDatesMilliJ[captureHistoryJ.getWhenInfectionDetected()]) / (24 * 60 * 60 * 1000);
				
				spatialDistanceBetweenMainGroups = calculateSpatialDistanceBetweenGroups(
						captureHistoryI.getMainGroup(), captureHistoryJ.getMainGroup(), 
						-1, -1, territoryCentroids);
				spatialDistanceBetweenGroups1stInfectedIn = calculateSpatialDistanceBetweenGroups(
						captureHistoryI.getGroupWhenFirstInfected(), captureHistoryJ.getGroupWhenFirstInfected(), 
						captureHistoryI.getCaptureDates()[captureHistoryI.getWhenInfectionDetected()].get(Calendar.YEAR),
						captureHistoryJ.getCaptureDates()[captureHistoryJ.getWhenInfectionDetected()].get(Calendar.YEAR),
						territoryCentroids);
				spatialDistanceBetweenGroupsSampledIn = calculateSpatialDistanceBetweenGroups(
						sampleInfoI.getBadgerGroup(), sampleInfoJ.getBadgerGroup(), 
						sampleInfoI.getDate().get(Calendar.YEAR),
						sampleInfoJ.getDate().get(Calendar.YEAR),
						territoryCentroids);
				noMovementsBetweenMainGroups = getNoMovementsBetweenGroups(captureHistoryI.getMainGroup(), captureHistoryJ.getMainGroup(), badgerGroupIndices, groupAdjacencyMatrix);
				noMovementsBetweenGroups1stInfectedIn = getNoMovementsBetweenGroups(captureHistoryI.getGroupWhenFirstInfected(), captureHistoryJ.getGroupWhenFirstInfected(), badgerGroupIndices, groupAdjacencyMatrix);
				noMovementsBetweenGroupsSampledIn = getNoMovementsBetweenGroups(sampleInfoI.getBadgerGroup(), sampleInfoJ.getBadgerGroup(), badgerGroupIndices, groupAdjacencyMatrix);
				
				// Badger relatedness
				relatedness = getRelatedness(captureHistoryI, captureHistoryJ, hostRelatenessMatrix);
				
				output = geneticDistance + "\t" + sameMainGroup + "\t" + sameGroupWhenSampled + "\t" + sameGroupWhenInfected + "\t" + periodAliveTogether + "\t" + periodInfectedTogether + "\t";
				output += periodSpentInSameGroup + "\t" + timeBetweenInfectionDetection + "\t" + timeBetweenSamplingEvents + "\t" + spatialDistanceBetweenMainGroups + "\t";
				output += spatialDistanceBetweenGroupsSampledIn + "\t" + spatialDistanceBetweenGroups1stInfectedIn + "\t" + noMovementsBetweenMainGroups + "\t";
				output += noMovementsBetweenGroupsSampledIn + "\t" + noMovementsBetweenGroups1stInfectedIn + "\t";
				output += relatedness + "\t" + wbIds[i] + "\t" + wbIds[j];
				
				WriteToFile.writeLn(bWriter, output);
			}
		}
		
		WriteToFile.close(bWriter);
	}
	
	public static double[][] createSpatialDistanceMatrix(Hashtable<String, Integer> groupIndexes, Hashtable<String, double[]> territoryCentroids){
		
		// Get the badger group names
		String[] badgerGroups = HashtableMethods.getKeysString(groupIndexes);
		
		// Initialise a distance matrix to store spatial distances
		double[][] distanceMatrix = new double[badgerGroups.length][badgerGroups.length];
		double distance;
		
		// Compare each group to every other group
		for(int i = 0; i < badgerGroups.length; i++){
			for(int j = 0; j < badgerGroups.length; j++){
				
				if(i >= j){
					continue;
				}
				
				// Calculate spatial distance
				distance = calculateSpatialDistance(badgerGroups[i], badgerGroups[j], territoryCentroids);
				
				// Store the distance calculated
				distanceMatrix[groupIndexes.get(badgerGroups[i])][groupIndexes.get(badgerGroups[j])] = distance;
				distanceMatrix[groupIndexes.get(badgerGroups[j])][groupIndexes.get(badgerGroups[i])] = distance;				
			}
		}
		
		return distanceMatrix;
	}
	
	public static double findPeriodSpentInSameGroup(Hashtable<String, long[][]> groupInfoA, Hashtable<String, long[][]> groupInfoB){
		
		// Find the groups that Badger A has ever inhabited
		String[] groupsA = HashtableMethods.getKeysString(groupInfoA);
		
		// Initialise a variable to store the amount of time shared in the same group
		double time = 0;
		
		// Initialise variables necessary for comparing the periods spent in a given group
		long[][] periodsSpentA;
		long[][] periodsSpentB;
		
		// Look at each of the groups badger A inhabited - are there any that badger B inhabited?
		for(String group : groupsA){
			
			// Did B never enter this group - if so skip it
			if(groupInfoB.get(group) == null){
				continue;
			}
			
			// Do any of the periods that A spent in the current group overlap with any of the periods that B spent in the current group?
			periodsSpentA = groupInfoA.get(group);
			periodsSpentB = groupInfoB.get(group);
			
			// Examine each of the periods that badger A spent in the current group and compare those to the periods that badger B spent in the group
			for(int rowA = 0; rowA < periodsSpentA.length; rowA++){
				for(int rowB = 0; rowB < periodsSpentB.length; rowB++){
					
					// Sum up the time spent together across groups and periods
					
					//System.out.println(periodsSpentA[rowA][0] + "\t" + periodsSpentA[rowA][1] + "\t" + periodsSpentB[rowB][0] + "\t" + periodsSpentB[rowB][1] + "\t\t" + findTimeTogether(periodsSpentA[rowA][0], periodsSpentA[rowA][1], periodsSpentB[rowB][0], periodsSpentB[rowB][1]));
					
					time += findTimeTogether(periodsSpentA[rowA][0], periodsSpentA[rowA][1], periodsSpentB[rowB][0], periodsSpentB[rowB][1]);
				}
			}
		}
		
		return time;
	}
	
	public static int getNoMovementsBetweenGroups(String groupA, String groupB, Hashtable<String, Integer> groupIndexes, int[][] adjacencyMatrix){
		
		// Initialise variable to store no of movements
		int noMovements = 0;
		
		// Get the indexes associated with each of the groups - gives position in adjacency matrix
		int groupIndexA = groupIndexes.get(groupA);
		int groupIndexB = groupIndexes.get(groupB);
		
		// Check that not in the same group
		if(groupIndexA != groupIndexB){
			
			// Find the no. of movements linking the two groups - note that the adjacency matrix is not symmetric
			noMovements = adjacencyMatrix[groupIndexA][groupIndexB] + adjacencyMatrix[groupIndexB][groupIndexA];
		}
		
		return noMovements;
	}
	
	public static Hashtable<String, Integer> findAllGroups(Hashtable<String, CaptureData> badgerCaptureHistories){
		
		// Get all the tattoos (keys) from the capture history hashtable
		String[] keys = HashtableMethods.getKeysString(badgerCaptureHistories);
		
		// Initialise a hashtable to store the groups and their associated indices
		Hashtable<String, Integer> groups = new Hashtable<String, Integer>();
		int index = -1;
		
		// Examine each badger
		for(String tattoo : keys){
			
			// Look at all the groups the current badger inhabited
			for(String group : badgerCaptureHistories.get(tattoo).getGroupsInhabited()){
				
				// Has the current group been encountered before?
				if(groups.get(group) == null){
					
					// If not then store the group name and give it a index
					index++;
					groups.put(group, index);
				}
			}			
		}
		
		return groups;		
	}
	
	public static double calculateSpatialDistance(String groupA, String groupB, Hashtable<String, double[]> territoryCentroids){
		
		// Initialise a variable to store the spatial distance
		double spatialDist = 0;
		
		// Check the groups aren't the same
		if(groupA.matches(groupB) == false){
			
			// Check that territory centroid information is available for both groups
			if(territoryCentroids.get(groupA) != null && territoryCentroids.get(groupB) != null){
				spatialDist = StepwiseMatching.calculateSpatialDistance(territoryCentroids.get(groupA), territoryCentroids.get(groupB));
			}
		}
		
		return spatialDist;
	}
	
	public static int checkIfInSameGroup(String a, String b){
		
		// Initialise a variable to store the result
		int result = 0;
		
		// Check if the groups are the same and that neither are NA
		if(a.matches(b) == true && a.matches("NA") == false){
			result = 1;
		}
		
		return result;
	}
	
	public static double findTimeTogether(long aStart, long aEnd, long bStart, long bEnd){
		
		// Initialise a variable to store the time spent together
		double time = 0;
		
		// Check that neither start after the other has finished - i.e. check if intervals overlap
		if(aStart < bEnd && bStart < aEnd){
		
			// Find which badger's first capture was latest
			long start = aStart;
			if(bStart > aStart){
				start = bStart;
			}
		
			// Find which badger's last capture was earliest
			long end = aEnd;
			if(bEnd < aEnd){
				end = bEnd;
			}
		
			// Calculate time alive together in Days
			time = (end - start) / (24 * 60 * 60 * 1000);
		}
			
		return time;		
	}
	
	public static Hashtable<String, CaptureData> readConsolidatedBadgerCaptureInfo(String fileName) throws IOException{
		
		// Open the condolidated data file for reading
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
		
		// Initialise a Hashtable to store each badger's capture history object assigned to it's tattoo (key)
		Hashtable<String, CaptureData> captureHistories = new Hashtable<String, CaptureData>();
		String tattoo;
		
		// Examine each line of the file
		String line = null;
		while(( line = reader.readLine()) != null){
			
			// Skip the file Header
			if(line.matches("Tattoo(.*)")){
				continue;
			}
			
			// Select the badger tattoo - used as key in hashtable
			tattoo = line.split("\t")[0];
			
			// Get the capture history for the current badger and store it in the hashtable - note that the Capture Data method contains all the necessary parsing methods
			captureHistories.put(tattoo, new CaptureData(line));
		}
		
		// Close the open file
		reader.close();
		
		return captureHistories;
	}

	// Methods to record the number of animals shared between badger groups
	public static int[][] countNumberOfSharedBadgersBetweenGroups(Hashtable<String, Integer> badgerGroupIndices,
			Hashtable<String, Hashtable<String, Integer>> groupInhabitants){
		
		// Initialise a matrix to store the counts
		int[][] nShared = new int[groupInhabitants.size()][groupInhabitants.size()];
		int count;
		
		// Compare each of the groups to one another
		String[] groupNames = HashtableMethods.getKeysString(groupInhabitants);
		for(int i = 0; i < groupNames.length; i++){
			for(int j = 0; j < groupNames.length; j++){
				
				// Avoid the same comparison twice
				if(i > j){
					continue;
				}
				
				// Count the number of shared animals
				count = HashtableMethods.countSharedKeysString(groupInhabitants.get(groupNames[i]),
						groupInhabitants.get(groupNames[j]));
				
				// Store the count
				nShared[badgerGroupIndices.get(groupNames[i])][badgerGroupIndices.get(groupNames[j])] = count;
				nShared[badgerGroupIndices.get(groupNames[j])][badgerGroupIndices.get(groupNames[i])] = count;
			}
		}
		
		return nShared;
	}

	public static int[][] createGroupAdjacencyMatrix(Hashtable<String, Integer> groups, Hashtable<String, CaptureData> badgerCaptureHistories,
			Hashtable<String, Hashtable<String, Integer>> groupInhabitants){
		
		// Initialise a matrix to store the no. of movements linking the badger groups - Mij = no. Movements from group i to group j
		int[][] matrix = new int[groups.size()][groups.size()];
		
		// Get the tattoos of all the sampled badgers
		String[] tattoos = HashtableMethods.getKeysString(badgerCaptureHistories);
				
		// Initialise variables for obtaining badger capture history information
		String[] badgerGroups;
		
		// Examine each of the badgers
		for(String tattoo : tattoos){
			
			// Get the badger groups inhabited by the current badger
			badgerGroups = badgerCaptureHistories.get(tattoo).getGroupsInhabited();
			
			// Note the groups that the current badger has inhabited
			addTattooToGroupsInhabited(groupInhabitants, tattoo, badgerGroups);
			
			// Look at the list of badger groups that the current badger has inhabited - are there any dispersal events?
			for(int i = 1; i < badgerGroups.length; i++){
				
				// Check if current group doesn't match the previous group (dispersal event) and also don't record movements to and from NA
				if(badgerGroups[i].matches(badgerGroups[i - 1]) == false && badgerGroups[i].matches("NA") == false && badgerGroups[i - 1].matches("NA") == false){
					
					// A Dispersal Event has occurred
					matrix[groups.get(badgerGroups[i - 1])][groups.get(badgerGroups[i])]++;					
				}				
			}			
		}
		
		return matrix;
	}
	
	public static void addTattooToGroupsInhabited(Hashtable<String, Hashtable<String, Integer>> groupInhabitants, String tattoo, 
			String[] groupsInhabited){
		
		for(String group : groupsInhabited){
			groupInhabitants.get(group).put(tattoo, 1);
		}
	}

	public static Hashtable<String, Hashtable<String, Integer>> initialiseHashtableToStoreGroupInhabitants(String[] groupNames){
		Hashtable<String, Hashtable<String, Integer>> groupInhabitants = new Hashtable<String, Hashtable<String, Integer>>();
		
		for(String name : groupNames){
			groupInhabitants.put(name, new Hashtable<String, Integer>());
		}
		
		return groupInhabitants;
	}
}
