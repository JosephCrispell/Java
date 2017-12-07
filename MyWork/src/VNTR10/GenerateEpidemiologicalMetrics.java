package VNTR10;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Hashtable;

import javax.swing.plaf.synth.SynthSeparatorUI;

import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.HashtableMethods;
import methods.WriteToFile;
import methods.LatLongMethods;
import woodchesterCattle.Location;
import woodchesterCattle.MakeEpidemiologicalComparisons;
import woodchesterGeneticVsEpi.CompareIsolates;

public class GenerateEpidemiologicalMetrics {

	public static void main(String[] args) throws IOException{
		
		// Note the path to the main directory
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/VNTR10/NewAnalyses_18-05-16/";
		int limit = 9999;
		
		// Read in the isolate information
		String animalLookupFile = path + "SampleInformation/IsolateInformation_07-08-16.csv";
		Hashtable<String, IsolateData> isolates = readIsolateData(animalLookupFile);
		
		// Build the spatial and temporal distance matrices
		Distances distances = buildSpatialAndTemporalDistanceMatrices(isolates);
		
		// Begin reading the movement data and record the number of movements linking herds
		String movementsFile = path + "SampleInformation/inter_herd_links_all_orderedByStartDate.csv";
		Network adjacencyMatrix = readCattleMovementsTable(movementsFile, isolates, limit);
		
		// Read in the herd episode information
		String episodesFile = path + "SampleInformation/herd_episodes.csv";
		Hashtable<String, Herd> herds = readEpisodeInformation(episodesFile, adjacencyMatrix.getHerdIndices(), limit);
		
		// Examine the isolates movement histories
		examineAnimalMovementHistories(isolates, herds);
				
		// Get the order of the isolates from the merged file
		String mergedFile = path + "FilterSensitivity/merged_Clade0_Cov-0.9_06-06-16.txt";
		String[] orderedIsolates = getIsolateOrderFromMergedFile(mergedFile, isolates);
		
		// Calculate the epidemiological metrics - ORDER by merged file!!!!!!!!!!!!!!!!!!
		String epidemiologicalMetricsFile = path + "FilterSensitivity/epiVariables_25-08-16.txt";
		generateEpidemiologicalMetrics(epidemiologicalMetricsFile, orderedIsolates, isolates, distances, adjacencyMatrix, herds);
	}
	
	public static String[] getIsolateOrderFromMergedFile(String fileName, Hashtable<String, IsolateData> isolates) throws IOException{
		
		/**
		 * merged file structure:
		 * 	##fileformat=VCFv4.1
		 * 	##samtoolsVersion=0.1.18 (r982:295)
		 * 	...
		 * 	#CHROM	POS	FC099_s8_1_013_RK142_106.vcf:FC099_s8_1_014_RK136_107.vcf:FC099_s8_1_015_RK137_108.vcf:FC099_s8_1_016_RK147_109.vcf:FC099_s8_1_017_RK139_110.vcf:FC099_s8_1_018_RK149_111.vcf:FC099_s8_1_019_RK134_112.vcf:FC099_s8_1_020_RK128_113.vcf:FC099_s8_1_021_RK148_114.vcf:FC099_s8_1_022_RK144_115.vcf:FC099_s8_1_023_RK126_116.vcf:FC099_s4_1_001_RK51_1.vcf:FC099_s4_1_002_RK34_2.vcf:FC099_s4_1_003_RK47_3.vcf:FC099_s4_1_004_RK52_4.vcf:FC099_s4_1_005_RK49_5.vcf:FC099_s4_1_006_RK46_6.vcf:FC099_s4_1_007_RK39_7.vcf:FC099_s4_1_008_RK38_8.vcf:FC099_s4_1_009_RK36_9.vcf:FC099_s4_1_010_RK37_10.vcf:FC099_s4_1_011_RK44_11.vcf:FC099_s4_1_012_RK41_12.vcf:FC099_s4_1_013_RK48_13.vcf:FC099_s4_1_014_RK42_14.vcf:FC099_s4_1_015_RK43_15.vcf:FC099_s4_1_016_RK54_16.vcf:FC099_s4_1_017_RK45_17.vcf:FC099_s4_1_018_RK56_18.vcf:FC099_s4_1_019_RK40_19.vcf:FC099_s4_1_020_RK35_20.vcf:FC099_s4_1_021_RK55_21.vcf:FC099_s4_1_022_RK50_22.vcf:FC099_s4_1_023_RK33_23.vcf:FC099_s4_1_024_RK53_24.vcf:FC099_s5_1_001_RK74_25.vcf:FC099_s5_1_002_RK58_26.vcf:FC099_s5_1_003_RK70_27.vcf:FC099_s5_1_004_RK75_28.vcf:FC099_s5_1_005_RK72_29.vcf:FC099_s5_1_006_RK69_30.vcf:FC099_s5_1_007_RK63_31.vcf:FC099_s5_1_008_RK62_32.vcf:FC099_s5_1_009_RK60_33.vcf:FC099_s5_1_010_RK61_34.vcf:FC099_s5_1_011_RK68_35.vcf:FC099_s5_1_012_RK65_36.vcf:FC099_s5_1_013_RK71_37.vcf:FC099_s5_1_014_RK66_38.vcf:FC099_s5_1_015_RK67_39.vcf:FC099_s5_1_016_RK77_40.vcf:FC099_s5_1_017_RK79_41.vcf:FC099_s5_1_018_RK64_42.vcf:FC099_s5_1_019_RK59_43.vcf:FC099_s5_1_020_RK78_44.vcf:FC099_s5_1_021_RK73_45.vcf:FC099_s5_1_022_RK57_46.vcf:FC099_s5_1_023_RK76_47.vcf:FC099_s6_1_001_RK98_48.vcf:FC099_s6_1_002_RK81_49.vcf:FC099_s6_1_003_RK94_50.vcf:FC099_s6_1_004_RK99_51.vcf:FC099_s6_1_005_RK96_52.vcf:FC099_s6_1_006_RK93_53.vcf:FC099_s6_1_007_RK86_54.vcf:FC099_s6_1_008_RK85_55.vcf:FC099_s6_1_009_RK83_56.vcf:FC099_s6_1_010_RK84_57.vcf:FC099_s6_1_011_RK91_58.vcf:FC099_s6_1_012_RK88_59.vcf:FC099_s6_1_013_RK95_60.vcf:FC099_s6_1_014_RK89_61.vcf:FC099_s6_1_015_RK90_62.vcf:FC099_s6_1_016_RK101_63.vcf:FC099_s6_1_017_RK92_64.vcf:FC099_s6_1_018_RK87_65.vcf:FC099_s6_1_019_RK82_66.vcf:FC099_s6_1_020_RK102_67.vcf:FC099_s6_1_021_RK97_68.vcf:FC099_s6_1_022_RK80_69.vcf:FC099_s6_1_023_RK100_70.vcf:FC099_s7_1_001_RK121_71.vcf:FC099_s7_1_002_RK104_72.vcf:FC099_s7_1_003_RK117_73.vcf:FC099_s7_1_004_RK122_74.vcf:FC099_s7_1_005_RK119_75.vcf:FC099_s7_1_006_RK116_76.vcf:FC099_s7_1_007_RK109_77.vcf:FC099_s7_1_008_RK108_78.vcf:FC099_s7_1_009_RK106_79.vcf:FC099_s7_1_010_RK107_80.vcf:FC099_s7_1_011_RK114_81.vcf:FC099_s7_1_012_RK111_82.vcf:FC099_s7_1_013_RK118_83.vcf:FC099_s7_1_014_RK112_84.vcf:FC099_s7_1_015_RK113_85.vcf:FC099_s7_1_016_RK123_86.vcf:FC099_s7_1_017_RK115_87.vcf:FC099_s7_1_018_RK125_88.vcf:FC099_s7_1_019_RK110_89.vcf:FC099_s7_1_020_RK105_90.vcf:FC099_s7_1_021_RK124_91.vcf:FC099_s7_1_022_RK120_92.vcf:FC099_s7_1_023_RK103_93.vcf:FC099_s8_1_001_RK145_94.vcf:FC099_s8_1_002_RK127_95.vcf:FC099_s8_1_003_RK141_96.vcf:FC099_s8_1_004_RK146_97.vcf:FC099_s8_1_005_RK143_98.vcf:FC099_s8_1_006_RK140_99.vcf:FC099_s8_1_007_RK133_100.vcf:FC099_s8_1_008_RK132_101.vcf:FC099_s8_1_009_RK130_102.vcf:FC099_s8_1_010_RK131_103.vcf:FC099_s8_1_011_RK138_104.vcf:FC099_s8_1_012_RK135_105.vcf
		 * 	0		1	2
		 * 
		 * Note that the 3rd column is a ":" delimited list of the VCF file names:
		 * 	FC099	s8	1	013	RK142	106.vcf
		 * 	0		1	2	3	4		5
		 */
		
		// Open the merged file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise an array to store the isolate ids;
		String[] isolateIds = new String[0];
		
		// Initialise variables necessary for parsing the file
		String line = null;
		int lineNo = 0;
		String[] cols;
		String[] parts;
								
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			lineNo++;
					
			// Find the fields line
			if(line.matches("(^#)CHROM(.*)")){
				
				// Split the VCF file names into an array
				cols = line.split("\t")[2].split(":");
				
				// Examine each of the VCF file names and select the isolate ID
				for(int i = 0; i < cols.length; i++){
					parts = cols[i].split("_");
					
					// Remove the RK from the front of the isolate ID
					isolateIds = ArrayMethods.append(isolateIds, parts[4].substring(2));
				}
				
				// Stop reading the file
				break;
			}					
		}

		// Close the lookup file
		reader.close();
		input.close();
		
		// Find the animal IDs for the isolate IDs
		return getAnimalIdsForIsolateIds(isolateIds, isolates);		
	}
	
	public static String[] getAnimalIdsForIsolateIds(String[] isolateIds, Hashtable<String, IsolateData> isolates){
		
		// Get a list of the animal IDs
		String[] ids = HashtableMethods.getKeysString(isolates);
		
		// Initialise a hashtable to store the animal ID and isolate ID pairs
		Hashtable<String, String> animalIdsForIsolateIds = new Hashtable<String, String>();
		
		// Get the information for each isolate
		for(String id : ids){
			animalIdsForIsolateIds.put(isolates.get(id).getIsolateId(), id);
		}
		
		// Initialise an array to store the animal IDs corresponding to the isolate IDs
		String[] animalIds = new String[isolateIds.length];
		for(int i = 0; i < isolateIds.length; i++){
			animalIds[i] = animalIdsForIsolateIds.get(isolateIds[i]);
		}
		
		return animalIds;
	}
	
	// Comparison methods
	public static void generateEpidemiologicalMetrics(String fileName, String[] isolateIds, Hashtable<String, IsolateData> isolates,
			Distances spatialTemporalDistances, Network herdAdjacencyMatrix, Hashtable<String, Herd> herdInfo) throws IOException{
		
		/**
		 * Epidemiological comparisons to be made:
		 * 	0	Spatial distance									Double
		 * 	1	Number of years between isolation years				Integer
		 * 	2	Number of movements between sampled herds			Integer	
		 * 	3	Same species?										[0, 1]
		 * 	4	Number of days that lifespans overlap				Integer
		 * 	5	Shortest path between sampled herds					Integer
		 * 	6	Mean number of movements between sampled herds		Double
		 * 	7	Number of days spent in the same herd				Integer
		 * 	8	Same episode?										[0, 1]
		 */
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Print a header into the output file
		String header = "SpatialDist\tNYearsBetweenSamplingYears\tNMovementsBetweenSampledHerds\t";
		header += "SameSpecies\tNDaysLifespansOverlap\tShortestPathBetweenSampledHerds\t";
		header += "MeanNMovementsOnShortestPathBetweenSampledHerds\tNDaysInSameHerd\tSameEpisode";
		WriteToFile.writeLn(bWriter, header);
		
		// Calculate the Shortest paths between all the herds
		Hashtable<Integer, int[][]> shortestPaths = CompareIsolates.findShortestPathsBetweenAllNodes(
				herdAdjacencyMatrix.getNMovementsBetweenHerds());
		
		// Initialise an array to store the epidemiological metrics
		double[] epiMetrics;
		
		// Initialise variables to store the isolateData
		IsolateData infoI;
		IsolateData infoJ;
		
		// Compare the isolates to one another
		for(int i = 0; i < isolateIds.length; i++){
			
			// Get the information for isolate i
			infoI = isolates.get(isolateIds[i]);
			
			for(int j = 0; j < isolateIds.length; j++){
				
				// Avoid multi- and self-comparisons
				if(i >= j){
					continue;
				}
				
				// Get the information for isolate j
				infoJ = isolates.get(isolateIds[j]);
				
				// Generate the epidemiological metrics
				epiMetrics = compareSampledAnimalsToGenerateEpidemiologicalMetrics(infoI, infoJ,
						spatialTemporalDistances, herdAdjacencyMatrix, shortestPaths, herdInfo);
				
				// Write the epidemiological metrics out to file
				WriteToFile.writeLn(bWriter, ArrayMethods.toString(epiMetrics, "\t"));
			}
		}
		
		// Close the output file
		WriteToFile.close(bWriter);
		
	}
	
	public static double[] compareSampledAnimalsToGenerateEpidemiologicalMetrics(IsolateData a, IsolateData b,
			Distances spatialTemporalDistances, Network herdAdjacencyMatrix, 
			Hashtable<Integer, int[][]> shortestPaths, Hashtable<String, Herd> herdInfo){
		
		/**
		 * Epidemiological comparisons to be made:
		 * 	0	Spatial distance									Double
		 * 	1	Number of years between isolation years				Integer
		 * 	2	Number of movements between sampled herds			Integer	
		 * 	3	Same species?										[0, 1]
		 * 	4	Number of days that lifespans overlap				Integer
		 * 	5	Shortest path between sampled herds					Integer
		 * 	6	Mean number of movements between sampled herds		Double
		 * 	7	Number of days spent in the same herd				Integer
		 * 	8	Same episode?										[0, 1]
		 */
		
		// Initialise an array to store the epidemiological metrics
		double[] epiMetrics = new double[9];
		
		// Spatial distance between the isolates
		epiMetrics[0] = spatialTemporalDistances.getSpatialDistanceBetweenTwoIsolates(a.getAnimalId(), b.getAnimalId());
		
		// Temporal distance between the isolates
		epiMetrics[1] = spatialTemporalDistances.getTemproalDistanceBetweenTwoIsolates(a.getAnimalId(), b.getAnimalId());
		
		// Number of movements between sampled herds
		epiMetrics[2] = -1;
		if(a.getIsBadger() == false && b.getIsBadger() == false){
			epiMetrics[2] = herdAdjacencyMatrix.getNMovementsBetweenHerds(a.getHerdId(), b.getHerdId());
		}
		
		// Same species
		epiMetrics[3] = checkIfSameSpecies(a, b);
		
		// Number of days that lifespans overlap
		epiMetrics[4] = -1;
		if(a.getGotStartEnd() == true && b.getGotStartEnd() == true){
			epiMetrics[4] = MakeEpidemiologicalComparisons.calculateNDaysOverlapped(a.getStartEnd()[0].getTimeInMillis(),
					a.getStartEnd()[1].getTimeInMillis(), b.getStartEnd()[0].getTimeInMillis(),
					b.getStartEnd()[1].getTimeInMillis());
		}
		
		// Shortest path between the sampled herds
		epiMetrics[5] = -1;
		epiMetrics[6] = -1;
		if(a.getIsBadger() == false && b.getIsBadger() == false){
			int[] shortestPath = shortestPaths.get(herdInfo.get(a.getHerdId()).getIndex())[herdInfo.get(b.getHerdId()).getIndex()];
			epiMetrics[5] = shortestPath.length;
			epiMetrics[6] = 0;
			if(shortestPath.length != 0){
				epiMetrics[6] = CompareIsolates.calculateMeanNMovementsOnEdgesOfShortestPath(shortestPath,
						herdInfo.get(b.getHerdId()).getIndex(), herdAdjacencyMatrix.getNMovementsBetweenHerds());
			}
		}
		
		
		// Number of days spent in the same herd
		epiMetrics[7] = -1;
		if(a.getIsBadger() == false && b.getIsBadger() == false){
			epiMetrics[7] = calculatePeriodSpentInSameHerd(a.getHerdsInhabited(), b.getHerdsInhabited());
		}
		
		// Same episode?
		epiMetrics[8] = -1;
		if(a.getIsBadger() == false && b.getIsBadger() == false){
			epiMetrics[8] = checkIfSameEpisode(a, b);
		}
		
		return epiMetrics;
	}
	
	public static double calculatePeriodSpentInSameHerd(Hashtable<String, Herd> infoForHerdsAInhabited,
			Hashtable<String, Herd> infoForHerdsBInhabited){
		
		// Initialise a variable to store the number of days the individuals spent on the same herd
		double nDays = 0;
		
		// Get a list of all the herds that A inhabited
		String[] herdsAInhabited = HashtableMethods.getKeysString(infoForHerdsAInhabited);
		
		// Initialise variables to store the starts and ends of each period that the individuals spent on a herd
		long[] aStarts;
		long[] aEnds;
		long[] bStarts;
		long[] bEnds;
		
		// Did individual B spend any time in the herds that individual A inhabited?
		for(String herd : herdsAInhabited){
			
			// Skip this herd if B never inhabited it
			if(infoForHerdsBInhabited.get(herd) == null){
				continue;
			}
			
			// Get the starts and ends for each period that the individuals spent on the current herd
			aStarts = infoForHerdsAInhabited.get(herd).getStarts();
			aEnds = infoForHerdsAInhabited.get(herd).getEnds();
			bStarts = infoForHerdsBInhabited.get(herd).getStarts();
			bEnds = infoForHerdsBInhabited.get(herd).getEnds();
			
			// Do any of the periods that A inhabited the current herd overlap with the periods that B inhabited it?
			for(int a = 0; a < aStarts.length; a++){
				
				for(int b = 0; b < bStarts.length; b++){
					
					nDays += MakeEpidemiologicalComparisons.calculateNDaysOverlapped(aStarts[a], aEnds[a], bStarts[b], bEnds[b]);
				}
			}			
		}		
		
		return nDays;
	}
	
	public static int checkIfSameSpecies(IsolateData a, IsolateData b){
		int result = 0;
		if(a.getIsBadger() == b.getIsBadger()){
			result = 1;
		}
		return result;
	}
	
	public static int checkIfSameEpisode(IsolateData a, IsolateData b){
		int result = 0;
		if(a.getEpisode() != null && b.getEpisode() != null &&
				a.getEpisode().getId().matches(b.getEpisode().getId()) == true){
			result = 1;
		}
		return result;
	}
	
	public static void examineAnimalMovementHistories(Hashtable<String, IsolateData> isolates,
			Hashtable<String, Herd> herdInfo){
		
		// Get a list of the isolate IDs
		String[] ids = HashtableMethods.getKeysString(isolates);
		
		// Initialise a hashtable to record the herds inhabited
		Hashtable<String, Herd> herdsInhabited = new Hashtable<String, Herd>();
		Herd herd;
		Movement[] movements;
		
		// Examine each isolate
		for(String id : ids){
			
			// Get the start and end dates of the sampled animal start = first movement, end = half way through sampling year
			getStartAndEndOfMovementHistory(isolates.get(id));
			
			// Check that there are movements to examine
			if(isolates.get(id).getNMovements() == 0){
				continue;
			}else if(isolates.get(id).getNMovements() == 1){
				
				// Get the movements for the current isolate
				movements = isolates.get(id).getMovements();
				
				// Note the sampled herd as the herd inhabited
				herd = new Herd(movements[0].getOnId());
				
				// Check that the movement herd matches the sampled herd
				if(movements[0].getOnId().matches(isolates.get(id).getHerdId()) == false){
					System.err.println("Error: Animal with a single movement. ON herd doesn't match the herd animal was sampled in!: ");
					System.err.println("ON: " + movements[0].getOnId() + "\tSampled: " + isolates.get(id).getHerdId());
				}
				
				// Add the information for the visit to the current herd - From ON date to middle of sampling year
				herd.appendStayInfo(movements[0].getStartEnd()[1], CalendarMethods.createDate(isolates.get(id).getYearOfIsolation()));
				
				// Store the location period info
				herdsInhabited.put(movements[0].getOnId(), herd);
			}
			
			// Get the movements for the current isolate
			herdsInhabited = new Hashtable<String, Herd>();
			movements = isolates.get(id).getMovements();
			
			// Examine each of the movement events
			for(int i = 1; i < movements.length; i++){
				
				// Check that the OFF location for the current movement matches the ON location for the previous movement
				if(movements[i].getOffId().matches(movements[i-1].getOnId()) == true){
					
					// Check if information exists for the current herd
					if(herdsInhabited.get(movements[i].getOffId()) != null){
						
						// Add the information for the visit to the current herd
						herdsInhabited.get(movements[i].getOffId()).appendStayInfo(movements[i-1].getStartEnd()[1], movements[i].getStartEnd()[0]);
					}else{
						
						// Create a new entry for the current herd
						herd = new Herd(movements[i].getOffId());
						
						// Add the information for the visit to the current herd
						herd.appendStayInfo(movements[i-1].getStartEnd()[1], movements[i].getStartEnd()[0]);
						
						// Store the location period info
						herdsInhabited.put(movements[i].getOffId(), herd);
					}				
				}else{
					
					System.err.println("Error: The OFF location for the current movement doen's match the ON location of the previous movement");
				}
			}
			
			// Store the herds inhabited
			isolates.get(id).setHerdsInhabited(herdsInhabited);
			
			// Record the episode the current isolate was in, if known
			findEpisode(isolates.get(id), herdInfo);
		}
	}
	
	public static void getStartAndEndOfMovementHistory(IsolateData isolate){
		
		// Check that there are movements to examine
		if(isolate.getNMovements() > 0){
			
			Calendar[] startEnd = new Calendar[2];
			Movement[] movements = isolate.getMovements();
			startEnd[0] = movements[0].getStartEnd()[0];
			startEnd[1] = CalendarMethods.createDate(isolate.getYearOfIsolation());
			
			// If half way through the year is before the end of the last movement then change last date
			if(startEnd[1].compareTo(movements[movements.length - 1].getStartEnd()[1]) < 0){ 
				startEnd[1] = movements[movements.length - 1].getStartEnd()[1];
			}
			isolate.setStartEnd(startEnd);
		}
	}
	
	public static void findEpisode(IsolateData isolate, Hashtable<String, Herd> herdInfo){

		// If sampling year could fall in multiple episodes - choose the latest
		
		// Get the info for the isolates sampled herd
		Herd herd = herdInfo.get(isolate.getHerdId());
		
		// Initialise a variable to store the episode information
		Episode[] episodes;

		// Were there any recorded episodes in the herd?
		if(herd.getNEpisodes() != 0){
			
			// Examine each of the episodes at the current herd
			episodes = herd.getEpisodes();
			
			for(Episode episode : episodes){

				// Does the isolate's sampling year fall within the current episode?
				if(isolate.getYearOfIsolation() >= episode.getStartEnd()[0].get(Calendar.YEAR) && (
						episode.getStartEnd()[1] == null || 
						isolate.getYearOfIsolation() <= episode.getStartEnd()[1].get(Calendar.YEAR))){
					isolate.setEpisode(episode);
				}
			}
		}
	}
	
	// Reading in the Data methods
 	public static Hashtable<String, Herd> readEpisodeInformation(String fileName,
			Hashtable<String, Integer> indexedHerds, int arraySizeLimit) throws IOException{
		/**
		 * Cattle movements file structure:
		 * 	herd_ref	ep_id	ep_start	ep_end		num_strain_tested	unique_strains
		 *	50387		1		1995-06-29	1995-09-01	0					0
		 *	0			1		2			3			4					5
		 */
		
		// Initialise the herds
		Hashtable<String, Herd> herds = new Hashtable<String, Herd>();
		String[] ids = HashtableMethods.getKeysString(indexedHerds);
		for(String id : ids){
			herds.put(id, new Herd(id));
			herds.get(id).setIndex(indexedHerds.get(id));
		}
		
		// Open the movements file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise a variable to store the episode information
		Calendar[] startEnd = new Calendar[2];
		int[] dateFormat = {2,1,0}; // Index of day, month, year
		
		// Initialise variables necessary for parsing the file
		String line = null;
		int lineNo = 0;
		String[] cols;
						
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			lineNo++;
					
			// Skip the header
			if(lineNo == 1){
				continue;
			}
			
			// Split the line into its columns
			cols = line.split(",");
			
			// Did the current episode occur in one of the sampled herds?
			if(indexedHerds.get(cols[0]) != null){
				
				// Store the episode information
				startEnd[0] = CalendarMethods.parseDate(cols[2], "-", dateFormat);
				
				// Check that end date exists
				startEnd[1] = null;
				if(cols[3].matches("NA") == false){
					startEnd[1] = CalendarMethods.parseDate(cols[3], "-", dateFormat);
				}				
				herds.get(cols[0]).appendEpisode(new Episode(cols[1], startEnd), arraySizeLimit);
			}
		}

		// Close the lookup file
		reader.close();
		input.close();
		
		return herds;
	}
	
	public static Network readCattleMovementsTable(String fileName, Hashtable<String, IsolateData> isolates,
			int arraySizeLimit) throws IOException{
		
		/**
		 * Cattle movements file structure:
		 * 	#from_herd_ref	to_herd_ref	animal_ref	move_start	move_end	path_length
		 *	50393			3736		15119553	2012-02-02	2012-02-02	1
		 *	0				1			2			3			4			5
		 *
		 * As we read the movements we want to store movements of our sampled animals as well
		 * as record the number of movements between our herds.
		 */
		
		// Initialise the herd adjacency matrix
		Hashtable<String, Integer> indexedHerds = indexSampledHerds(isolates);
		Network adjacencyMatrix = new Network(indexedHerds);
		
		// Open the movements file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise a variable to store the movement information
		Movement movement;
		Calendar[] startEnd = new Calendar[2];
		int[] dateFormat = {2,1,0}; // Index of day, month, year
		
		// Initialise variables necessary for parsing the file
		String line = null;
		int lineNo = 0;
		String[] cols;
						
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			lineNo++;
					
			// Skip the header
			if(lineNo == 1){
				continue;
			}
			
			// Split the line into its columns
			cols = line.split(",");
			
			// Store the movement information
			startEnd[0] = CalendarMethods.parseDate(cols[3], "-", dateFormat);
			startEnd[1] = CalendarMethods.parseDate(cols[4], "-", dateFormat);
			movement = new Movement(cols[0], cols[1], startEnd, cols[2], Integer.parseInt(cols[5]));
			
			// Did this movement involve one of the sampled animals?
			if(isolates.get(cols[2]) != null){
				
				// Add the movement to the animals movement history
				isolates.get(cols[2]).appendMovement(movement, arraySizeLimit);
			}
			
			// Did this movement involve the sampled herds?
			if(indexedHerds.get(cols[0]) != null && indexedHerds.get(cols[1]) != null){
				adjacencyMatrix.addMovementToCountOfMovementsBetweenHerds(cols[0], cols[1]);
			}
			
			// Note the progress
			if(lineNo % 1000000 == 0){
				System.out.print(".");
			}			
		}
		System.out.println();
			
		// Close the lookup file
		reader.close();
		input.close();
		
		return adjacencyMatrix;
	}
	
	public static Hashtable<String, Integer> indexSampledHerds(Hashtable<String, IsolateData> isolates){
		
		// Initialise a hashtable to store the index herd IDs
		Hashtable<String, Integer> indexedHerds = new Hashtable<String, Integer>();
		int index = -1;
		
		// Examine each of the isolate's herds
		String[] ids = HashtableMethods.getKeysString(isolates);
		for(String id : ids){
			
			// Index the current isolate's herd if we haven't already encountered it
			if(indexedHerds.get(isolates.get(id).getHerdId()) == null){
				
				index++;
				indexedHerds.put(isolates.get(id).getHerdId(), index);
			}
		}
		
		return indexedHerds;
	}
	
	public static Hashtable<String, Herd> storeHerdEpisodeInformation(String fileName,
			Hashtable<String, Herd> herdInfo) throws IOException{
		/**
		 * Herd episodes file structure:
		 * 	herd_ref	ep_id	ep_start	ep_end		num_strain_tested	unique_strains
		 *	50387		1		1995-06-29	1995-09-01	0					0
		 *	0			1		2			3			4					5
		 */
		
		// Initialise an array to store the start and end dates for a given episode
		Calendar[] startEnd = new Calendar[2];
		int[] dateFormat = {2,1,0}; // Index of day, month, year
		
		// Open the herd episodes file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise variables necessary for parsing the file
		String line = null;
		int lineNo = 0;
		String[] cols;
						
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			lineNo++;
			
			// Skip the header
			if(lineNo == 1){
				continue;
			}
			
			// Split the line into its columns
			cols = line.split(",");
			
			// Did the current episode happen in any of our herds of interest?
			if(herdInfo.get(cols[0]) != null){
				
				// Store the episode information
				startEnd[0] = CalendarMethods.parseDate(cols[2], "-", dateFormat);
				startEnd[1] = CalendarMethods.parseDate(cols[3], "-", dateFormat);
			}
		}
		
		// Close the lookup file
		reader.close();
		input.close();
		
		return herdInfo;
	}
	
	public static Distances buildSpatialAndTemporalDistanceMatrices(Hashtable<String, IsolateData> isolateInfo){
		
		// Get a list of the isolate IDs
		String[] ids = HashtableMethods.getKeysString(isolateInfo);
		IsolateData infoI;
		IsolateData infoJ;
		
		// Initialise a matrix to store the spatial distances
		double[][] spatialDistances = new double[ids.length][ids.length];
		double spatialDistance;
				
		// Initialise a matrix to store the temporal distances - years
		int[][] temporalDistances = new int[ids.length][ids.length];
		int temporalDistance;
		
		// Compare all the isolates to one another
		for(int i = 0; i < ids.length; i++){
			
			// Note the assigned index for the current isolate
			isolateInfo.get(ids[i]).setIndex(i);
			
			// Get the information for isolate i
			infoI = isolateInfo.get(ids[i]);
			
			for(int j = 0; j < ids.length; j++){
				
				// Don't make multiple or self comparisons
				if(i >= j){
					continue;
				}
				
				// Get the information for isolate j
				infoJ = isolateInfo.get(ids[j]);
				
				// Calculate the spatial distance between the current two isolates
				spatialDistance = LatLongMethods.distance(infoI.getLatLongs()[0], infoI.getLatLongs()[1],
						infoJ.getLatLongs()[0], infoJ.getLatLongs()[1], 'K');
				
				// Calculate the temporal distance between the current two isolates
				temporalDistance = Math.abs(infoI.yearOfIsolation - infoJ.yearOfIsolation);

				// Store the calculated distances
				spatialDistances[i][j] = spatialDistance;
				spatialDistances[j][i] = spatialDistance;
				temporalDistances[i][j] = temporalDistance;
				temporalDistances[j][i] = temporalDistance;
			}
		}
		
		// Store the spatial and temporal distance matrices
		return new Distances(ids, spatialDistances, temporalDistances);
		
	}
	
	public static double[] getLatLongs(String latitude, String longitude){
		
		double[] latLongs = new double[2];
		latLongs[0] = Double.parseDouble(latitude);
		latLongs[1] = Double.parseDouble(longitude);
		
		return latLongs;
	}
	
	public static Hashtable<String, IsolateData> readIsolateData(String fileName) throws IOException{
		
		/**
		 * Lookup file structure:
		 * 	sample_id	animal_ref	RK_herd_ref	AFBINI_herd_ref	year	badger	genotype	
		 *	1			M/04/36		badger		M/04/36			2004	1		10
		 *	0			1			2			3				4		5		6
		 *
		 *	easting	northing	Latitude	Longitude
		 *	351700	360000		54.46581196	-5.661068463
		 *	7		8			9			10
		 */
		
		// Initialise a hashtable to store the isolate information
		Hashtable<String, IsolateData> isolateInfo = new Hashtable<String, IsolateData>();
		
		// Open the lookup file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise variables necessary for parsing the file
		String line = null;
		int lineNo = 0;
		String[] cols;
						
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			lineNo++;
			
			// Skip the header
			if(lineNo == 1){
				continue;
			}
			
			// Split the line into its columns
			cols = line.split(",");
			
			// Store the isolate information
			isolateInfo.put(cols[1], new IsolateData(cols[0], cols[1], Integer.parseInt(cols[4]),
					checkIfBadger(cols[5]),	cols[3], getLatLongs(cols[9], cols[10])));
		}
		
		// Close the lookup file
		reader.close();
		input.close();
		
		
		return isolateInfo;
	}
	
	public static boolean checkIfBadger(String value){
		
		boolean result = false;
		if(value.matches("1")){
			result = true;
		}
		
		return result;
	}
	
}
