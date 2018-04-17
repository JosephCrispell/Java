package ExamineWPInterspeciesTransmission;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Hashtable;

import javax.swing.plaf.synth.SynthSeparatorUI;

import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.GeneralMethods;
import methods.HashtableMethods;
import woodchesterCattle.BuildCattleLifeHistories;
import woodchesterCattle.IsolateData;
import woodchesterCattle.Movement;

public class GetCattleLifeHistories {

	public static void main(String[] args) throws IOException{
		
		// Get the date
		String date = CalendarMethods.getCurrentDate("dd-MM-yyyy");
				
		// Set the path
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/NewAnalyses_02-06-16/";
		
		/**
		 *  Get a list of the locations that are within 15km of Woodchester Park
		 */
		
		// Note the centroid of the Badger Territories within Woodchester Park
		double[] wpCentroid = {381761.7, 200964.3};
		double thresholdDistance = 1000;
		
		// Note the premises types to ignore
		Hashtable<String, Integer> premisesToIgnore = new Hashtable<String, Integer>();
		premisesToIgnore.put("SR", 1); // Slaughterhouse
		premisesToIgnore.put("CC", 1); // Collection Centres
		
		// Initialise a hashtable to store the location information - key: location ID
		Hashtable<String, Location> locations = new Hashtable<String, Location>();
		
		// Initalise a hashtable to store the premises type of each location
		Hashtable<String, String> locationsToIgnore = new Hashtable<String, String>();
		
		// Read in the locations table for pre- and post-2001 movement data
		String locationInfoFilePre2001 = path + "CattleMovementData-Pre2001/viewLocationNoAddress.csv";
		getLocationInformationPre2001(locationInfoFilePre2001, wpCentroid, premisesToIgnore, thresholdDistance,
				locations, locationsToIgnore);
		String locationInfoFilePost2001 = path + "CattleMovementData/20160314_joe_cts_locations.csv";
		getLocationInformationPost2001(locationInfoFilePost2001, wpCentroid, premisesToIgnore, thresholdDistance,
				locations, locationsToIgnore);
		
		System.out.println("Found " + locations.size() + " herds within " + (int) (thresholdDistance / 1000) + "km of Woodchester Park.\n\n");
		String[] keys = HashtableMethods.getKeysString(locations);
		
		/**
		 * 1) Note the IDs of the any animals that lived on the locations within 15km of Woodchester Park
		 */
		
		System.out.println("Round 1.");
		
		// Initialise a hashtable to store the IDs of any animals found
		Hashtable<String, IsolateData> animals = new Hashtable<String, IsolateData>();
		
		// Read the pre-2001 movements and find animals that lived on the herds of interest
		String movementsPre2001File = path + "CattleMovementData-Pre2001/viewMovementTransition.csv";
		findAnimalsOnHerdsPre2001(movementsPre2001File, locations, animals);
		
		// Read the post-2001 movements and find animals that lived on the herds of interest
		String movementsFilePost2001Prefix = path + "CattleMovementData/20160123_joe_cts_movements";
		findAnimalsOnHerdsPost2001(movementsFilePost2001Prefix, locations, animals);
		
		System.out.println("Found " + animals.size() + " animals living on herds within " + (int) (thresholdDistance / 1000) + "km of Woodchester Park.\n\n");
				
		/**
		 * 2) Note the movements of all animals found and keep note of any additional herds they encounter
		 */
		
		System.out.println("Round 2.");
		
		// Initialise a hashtable to record the new locations encountered
		Hashtable<String, Location> locationsEncountered = new Hashtable<String, Location>();
		
		// Record all the pre-2001 movements of the animals of interest
		noteMovementsOfAnimalsPre2001(movementsPre2001File, locations, animals, locationsEncountered,
				locationsToIgnore);
		
		// Record all the post-2001 movements of the animals of interest
		noteMovementsOfAnimalsPost2001(movementsFilePost2001Prefix, locations, animals, locationsEncountered,
				locationsToIgnore);
		
		System.out.println("Found " + locationsEncountered.size() + " additional herds that the animals found lived on.\n\n");
		
		/**
		 * 3) Note the IDs of animals that lived on the herds encountered
		 */
		
		System.out.println("Round 3.");
		
		// Initialise a hashtable to store the IDs of any animals found
		Hashtable<String, IsolateData> animalsEncountered = new Hashtable<String, IsolateData>();
				
		// Read the pre-2001 movements and find animals that lived on the herds of interest
		findAnimalsOnHerdsPre2001(movementsPre2001File, locationsEncountered, animalsEncountered);
			
		// Read the post-2001 movements and find animals that lived on the herds of interest
		findAnimalsOnHerdsPost2001(movementsFilePost2001Prefix, locationsEncountered, animalsEncountered);
		
		// Remove animals that were found in Round 1
		HashtableMethods.removeKeys(animalsEncountered, HashtableMethods.getKeysString(animals));
		
		System.out.println("Found " + animalsEncountered.size() + " animals that may have encountered the animals found.\n\n");
		
		/**
		 * 4) Note the movements of all the animals found and keep note of any additional herds they encounter
		 */
		
		System.out.println("Round 4.");
		
		// Record all the pre-2001 movements of the animals of interest
		noteMovementsOfAnimalsPre2001(movementsPre2001File, locations, animalsEncountered, locationsEncountered,
				locationsToIgnore);
		
		// Record all the post-2001 movements of the animals of interest
		noteMovementsOfAnimalsPost2001(movementsFilePost2001Prefix, locations, animalsEncountered, locationsEncountered,
				locationsToIgnore);
		
		// Combine the animal hashtables
		HashtableMethods.combineHashtablesUsingStringsForKeys(animals, animalsEncountered);
		
		// Combine the locations tables
		HashtableMethods.combineHashtablesUsingStringsForKeys(locations, locationsEncountered);
		
		/**
		 * Get the information for the locations (outwith 15km of Woodchester Park) that were encountered
		 */
		
		// Note run post-2001 first so as to ensure each location has the most up-to-date information
		addLocationInformationForEncounteredHerdsPost2001(locationInfoFilePost2001, locations);			
		addLocationInformationForEncounteredHerdsPre2001(locationInfoFilePre2001, locations);
	
		/*
		 * Next stage is close to what is done in: ExamineWPInterspecuesTransmission-ExamineCluster
		 * 
		 */
		
		/*
		 * Working towards this output file:
		 * AnimalId	Species	Isolates	Clusters	SamplingDates	DistancesToRef	DistancesToMRCA	DetectionDate
		 * 0		1		2			3			4				5				6				7
		 * 
		 * CattleTestDates	CattleTestResults	Xs	Ys	MovementDates	PremisesTypes	GroupIds	SequencingQuality
		 * 8				9					10	11	12				13				14			15
		 * 
		 * AnimalsEncountered	ContactStartDates	ContactEndDates	ContactHerds
		 * 17					18					19				20
		 */
		
		/**
		 * Add animal testing histories?
		 */
		
		/**
		 * Need to note who contacted who
		 */
		
		/**
		 * Add the sampling information - hopefully not necessary!
		 */
		
		/**
		 * Add cluster affiliations - ?!?!??!!!?!?!?!?!?!
		 */
		
		/**
		 * Print out animal life histories
		 */
		
	}
	
	// Methods
	public static void addLocationInformationForEncounteredHerdsPost2001(String locationInfoFile, 
			Hashtable<String, Location> locations) throws IOException{
		
		/**
		 * Location information file structure:
		 * 	location_id	cph	post_code	map_ref	x	y	holding_type	premises_type
		 * 	0			1	2			3		4	5	6				7
		 */
		
		// Open the movements file
		InputStream input = new FileInputStream(locationInfoFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
									
		// Note the progress
		System.out.println("Beginning to parse locations information table...");
		
		// Initialise variables necessary for parsing the file
		String line = null;
		int lineNo = 0;
		String[] cols;
					
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			lineNo++;
						
			// Skip the header and last line
			if(lineNo == 1 || line.matches("(.*)rows(.*)") == true){
				continue;
			}
			
			// Split the line into its columns
			cols = line.split(",", -1);
									
			// Is this a location that we encountered?
			if(locations.get(cols[0]) != null && locations.get(cols[0]).getCph() == null){
				
				// Add CPH
				locations.get(cols[0]).setCph(cols[1]);
				
				// Add location information
				locations.get(cols[0]).setX(Integer.parseInt(cols[4]));
				locations.get(cols[0]).setY(Integer.parseInt(cols[5]));
				
				// Add herd type description
				locations.get(cols[0]).setHerdType(cols[6]);
				locations.get(cols[0]).setPremisesType(cols[7]);
			}			
			
			// Note the progress
			if(lineNo % 100000 == 0){
				System.out.print(".");
			}
		}
		System.out.println();
		
		// Close the locations information file
		input.close();
		reader.close();
		
	}

	public static void addLocationInformationForEncounteredHerdsPre2001(String locationInfoFile, 
			Hashtable<String, Location> locations) throws IOException{
		/**
		 * Location information file structure:
		 * 	LocationKey	LocationType	LocationId/CPH	PremisesType	MapRef	X	Y	HoldingType	CtyKey	CtyPar
		 * 	0			1				2				3				4		5	6	7			8		9
		 * 
		 * 	vLocationId	Region	Live	Has_Movements
		 * 	10			11		12		13
		 */
		
		// Open the movements file
		InputStream input = new FileInputStream(locationInfoFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
									
		// Note the progress
		System.out.println("Beginning to parse locations information table...");
		
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
			
			// Split the line into its columns
			cols = line.split(",", -1);
			
			// Is this a location that we encountered?
			if(locations.get(cols[0]) != null && locations.get(cols[0]).getCph() == null){
				
				// Add CPH
				locations.get(cols[0]).setCph(cols[10]);
				
				// Add location information
				locations.get(cols[0]).setX(Integer.parseInt(cols[5]));
				locations.get(cols[0]).setY(Integer.parseInt(cols[6]));
				
				// Add herd type description
				locations.get(cols[0]).setHerdType(cols[7]);
				locations.get(cols[0]).setPremisesType(cols[3]);
			}
			
			// Note the progress
			if(lineNo % 100000 == 0){
				System.out.print(".");
			}
		}
		System.out.println();
		
		// Close the locations information file
		input.close();
		reader.close();
	}

	public static void noteMovementsOfAnimalsPost2001(String movementsFilePrefix,
			Hashtable<String, Location> locations, Hashtable<String, IsolateData> animals, 
			Hashtable<String, Location> locationsEncountered,
			Hashtable<String, String> locationsToIgnore) throws IOException{
		
		/**
		 * Movements File Structure:
		 * animal_id	movement_number	movement_date	off_location_id	is_trans	trans_location_id
		 * 0			1				2				3				4			5
		 * 
		 * on_location_id	is_birth	is_death	stay_length	is_valid_history
		 * 6				7			8			9			10
		 */
		
		// Note the years to examine
		int[] yearsToExamine = ArrayMethods.seq(2002, 2014, 1);
		
		// Initialise variables necessary for parsing the file
		String line = null;
		int lineNo = 0;
		String[] cols;
		
		// Initialise a variable to store the movement information
		Movement movementRecord;
		Calendar date;
		int[] dateFormat = {2, 1, 0};
		boolean birth;
		boolean death;
		
		System.out.println("Finding movements of animals (Post-2001)...");
		
		// The movements in any given year are recorded in separate files
		for(int year : yearsToExamine){
			
			// Open the movements file
			InputStream input = new FileInputStream(movementsFilePrefix + "_" + year + ".csv");
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
			
			// Reset file parsing variables
			line = null;
			
			// Begin reading the file
			while(( line = reader.readLine()) != null){
				lineNo++;
				
				// Skip the header and last line
				if(line.matches("animal_id(.*)") == true || line.matches("(.*)rows(.*)") == true){
					continue;
				}
				
				// Split the line into its columns
				cols = line.split(",", -1);
				
				// Skip transitory movement records
				if(cols[4].matches("f")){
					continue;
				}
				
				// Check if this movement involves an animal of interest
				if(animals.get(cols[0]) != null){
					
					// Get the date for the current movement
					date = CalendarMethods.parseDate(cols[2], "-", dateFormat, true);
					
					// Determine whether the movement is a birth or death
					birth = BuildCattleLifeHistories.checkIfTrue(cols[7]);
					death = BuildCattleLifeHistories.checkIfTrue(cols[8]);
					
					// Create a record of the current movement
					movementRecord = new Movement(cols[0], cols[1], date, cols[3], cols[6], birth, death, cols[9]);
					
					// Store the movement record
					animals.get(cols[0]).appendMovement(movementRecord, 99);
					
					// Have we encountered the OFF or ON locations before?
					if(cols[3].matches("") == false && locations.get(cols[3]) == null && 
							locationsEncountered.get(cols[3]) == null &&
							locationsToIgnore.get(cols[3]) == null){
						locationsEncountered.put(cols[3], new Location(cols[3], null, null, null, null, null));
					}
					if(cols[6].matches("") == false && 
							locations.get(cols[6]) == null && locationsEncountered.get(cols[6]) == null &&
							locationsToIgnore.get(cols[6]) == null){
						locationsEncountered.put(cols[6], new Location(cols[6], null, null, null, null, null));
					}
				}			
				
				// Note the progress
				if(lineNo % 10000000 == 0){
					System.out.print(".");
				}
			}
						
			// Close the current movements file
			input.close();
			reader.close();
		}
		System.out.println();
	}
	
	public static void noteMovementsOfAnimalsPre2001(String movementsFile,
			Hashtable<String, Location> locations, Hashtable<String, IsolateData> animals, 
			Hashtable<String, Location> locationsEncountered,
			Hashtable<String, String> locationsToIgnore) throws IOException{
		
		/**
		 * Movements File Structure: viewMovementTransition.csv
		 * 	AnimalId	MovementId	MovementDate	OffLocationKey	OnLocationKey	Birth	Death
		 * 	0			1			2				3				4				5		6
		 * 	
		 * 	TransitoryPremisesType	TransitoryLocationKey	TransitoryCount	Stay_Length 	Valid_History
		 * 	7						8						9				10				11
		 */
		
		// Open the movements file
		InputStream input = new FileInputStream(movementsFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
									
		// Initialise variables necessary for parsing the file
		String line = null;
		int lineNo = 0;
		String[] cols;
		
		// Initialise a variable to store the movement information
		Movement movementRecord;
		Calendar date;
		int[] dateFormat = {2, 1, 0};
		boolean birth;
		boolean death;
				
		// Begin reading the file
		System.out.println("Finding movements of animals (Pre-2001)...");
		while(( line = reader.readLine()) != null){
			lineNo++;
						
			// Skip the header and last line
			if(lineNo == 1){
				continue;
			}
						
			// Split the line into its columns
			cols = line.split(",", -1);
		
			// Check if this movement involves an animal of interest
			if(animals.get(cols[0]) != null){
				
				// Get the date for the current movement
				date = CalendarMethods.parseDate(cols[2].split(" ", -1)[0], "-", dateFormat, true);
				
				// Determine whether the movement is a birth or death
				birth = BuildCattleLifeHistories.checkIfTrue(cols[5]);
				death = BuildCattleLifeHistories.checkIfTrue(cols[6]);
				
				// Create a record of the current movement
				movementRecord = new Movement(cols[0], cols[1], date, cols[3], cols[4], birth, death, cols[10]);
				
				// Store the movement record
				animals.get(cols[0]).appendMovement(movementRecord, 99);
				
				// Have we encountered the OFF or ON locations before?
				if(cols[3].matches("") == false && locations.get(cols[3]) == null && 
						locationsEncountered.get(cols[3]) == null &&
						locationsToIgnore.get(cols[3]) == null){
					locationsEncountered.put(cols[3], new Location(cols[3], null, null, null, null, null));
				}
				if(cols[4].matches("") == false && locations.get(cols[4]) == null && 
						locationsEncountered.get(cols[4]) == null &&
						locationsToIgnore.get(cols[4]) == null){
					locationsEncountered.put(cols[4], new Location(cols[4], null, null, null, null, null));
				}
			}
			
			// Note the progress
			if(lineNo % 10000000 == 0){
				System.out.print(".");
			}			
		}
		System.out.println();
		
		// Close the movements file
		input.close();
		reader.close();
	}

	public static void findAnimalsOnHerdsPost2001(String movementsFilePrefix,
			Hashtable<String, Location> locations, Hashtable<String, IsolateData> animals) throws IOException{
		
		/**
		 * Movements File Structure:
		 * animal_id	movement_number	movement_date	off_location_id	is_trans	trans_location_id
		 * 0			1				2				3				4			5
		 * 
		 * on_location_id	is_birth	is_death	stay_length	is_valid_history
		 * 6				7			8			9			10
		 */
		
		// Note the years to examine
		int[] yearsToExamine = ArrayMethods.seq(2002, 2014, 1);
		
		// Initialise variables necessary for parsing the file
		String line = null;
		int lineNo = 0;
		String[] cols;
		
		System.out.println("Finding animal IDs for animals that lived on herds of interest (Post-2001)...");
		
		// The movements in any given year are recorded in separate files
		for(int year : yearsToExamine){
			
			// Open the movements file
			InputStream input = new FileInputStream(movementsFilePrefix + "_" + year + ".csv");
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
			
			// Reset file parsing variables
			line = null;
			
			// Begin reading the file
			while(( line = reader.readLine()) != null){
				lineNo++;
				
				// Skip the header and last line
				if(line.matches("animal_id(.*)") == true || line.matches("(.*)rows(.*)") == true){
					continue;
				}
				
				// Split the line into its columns
				cols = line.split(",", -1);
				
				// Skip transitory movement records
				if(cols[4].matches("f")){
					continue;
				}
				
				// Check if OFF and ON locations are of interest and we haven't already encountered this animal
				if(((cols[3].matches("") == false && locations.get(cols[3]) != null) || 
					(cols[6].matches("") == false && locations.get(cols[6]) != null)) && animals.get(cols[0]) == null){
					
					animals.put(cols[0], new IsolateData(null, null, null, null, null));
				}				
				
				// Note the progress
				if(lineNo % 10000000 == 0){
					System.out.print(".");
				}
			}
						
			// Close the current movements file
			input.close();
			reader.close();
		}
		System.out.println();
	}
	
	public static void findAnimalsOnHerdsPre2001(String movementsFile,
			Hashtable<String, Location> locations, Hashtable<String, IsolateData> animals) throws IOException{
		
		/**
		 * Movements File Structure: viewMovementTransition.csv
		 * 	AnimalId	MovementId	MovementDate	OffLocationKey	OnLocationKey	Birth	Death
		 * 	0			1			2				3				4				5		6
		 * 	
		 * 	TransitoryPremisesType	TransitoryLocationKey	TransitoryCount	Stay_Length 	Valid_History
		 * 	7						8						9				10				11
		 */
		
		// Open the movements file
		InputStream input = new FileInputStream(movementsFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
									
		// Initialise variables necessary for parsing the file
		String line = null;
		int lineNo = 0;
		String[] cols;
				
		// Begin reading the file
		System.out.println("Finding animal IDs for animals that lived on herds of interest (Pre-2001)...");
		while(( line = reader.readLine()) != null){
			lineNo++;
						
			// Skip the header line
			if(lineNo == 1){
				continue;
			}
						
			// Split the line into its columns
			cols = line.split(",", -1);
		
			// Check if OFF and ON locations are of interest and we haven't already encountered this animal
			if(((cols[3].matches("") == false && locations.get(cols[3]) != null) || 
				(cols[4].matches("") == false && locations.get(cols[4]) != null)) && animals.get(cols[0]) == null){
				
				animals.put(cols[0], new IsolateData(null, null, null, null, null));
			}			
			
			// Note the progress
			if(lineNo % 10000000 == 0){
				System.out.print(".");
			}			
		}
		System.out.println();
		
		// Close the movements file
		input.close();
		reader.close();
	}
	
	public static void getLocationInformationPost2001(String locationInfoFile, double[] wpCentroid, 
			Hashtable<String, Integer> premisesToIgnore, double thresholdDistance, 
			Hashtable<String, Location> locations, Hashtable<String, String> locationsToIgnore) throws IOException{
		
		/**
		 * Location information file structure:
		 * 	location_id	cph	post_code	map_ref	x	y	holding_type	premises_type
		 * 	0			1	2			3		4	5	6				7
		 */
		
		// Open the movements file
		InputStream input = new FileInputStream(locationInfoFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
									
		// Note the progress
		System.out.println("Beginning to parse locations information table...");
		
		// Initialise variables necessary for parsing the file
		String line = null;
		int lineNo = 0;
		String[] cols;
		
		// Initialise a variable to record the distance from each herd from Woodchester Park
		double distance;
		double[] coords = new double[2];
					
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			lineNo++;
						
			// Skip the header and last line
			if(lineNo == 1 || line.matches("(.*)rows(.*)") == true){
				continue;
			}
			
			// Split the line into its columns
			cols = line.split(",", -1);
			
			// Parse the premises description
			cols[7] = parsePremisesTypePost2001(cols[7]);
			
			// Skip locations with premises type we want to ignore
			if(cols[7] == null){
				locationsToIgnore.put(cols[0], "Unknown");
				continue;
			}else if(premisesToIgnore.get(cols[7]) != null){
				locationsToIgnore.put(cols[0], cols[7]);
				continue;
			}
						
			// Skip if no location information available
			if(cols[4].matches("") == true || cols[5].matches("") == true){
				continue;
			}
						
			// Is this a location with the threshold distance from Woodchester Park?
			coords[0] = Double.parseDouble(cols[4]);
			coords[1] = Double.parseDouble(cols[5]);
			distance = GeneralMethods.calculateEuclideanDistance(wpCentroid, coords);
			if(distance <= thresholdDistance){
				
				// Parse the CPH
				if(cols[1].matches("") == false){
					cols[1] = BuildCattleLifeHistories.parseCPHPost2001(cols[1]);
				}
				
				// Check if we encountered location in the pre-2001 dataset
				if(locations.get(cols[0]) == null){
					locations.put(cols[0], new Location(cols[0], cols[1], cols[4], cols[5], cols[6], cols[7]));
				
				// Compare the information between the pre- and post-2001 datasets
				}else{
					
					// Do the CPHs match?
					if(locations.get(cols[0]).getCph().matches(cols[1]) == false){
						
						System.out.println("ERROR: location id (" + cols[0] + ") already present and CPHs don't match.");
						System.out.println(locations.get(cols[0]).getCph() + "\t" + cols[1]);
					}
					
					// Do the coordinates match?
					if(locations.get(cols[0]).getX() != Integer.parseInt(cols[4]) ||
							locations.get(cols[0]).getY() != Integer.parseInt(cols[5])){
						System.out.println("ERROR: location id (" + cols[0] + ") already present and coordinates don't match.");
						System.out.println("Updating location coordinates.");
						locations.get(cols[0]).setX(Integer.parseInt(cols[4]));
						locations.get(cols[0]).setY(Integer.parseInt(cols[5]));
					}
					
					// Do the premises types match?
					if(locations.get(cols[0]).getPremisesType().matches(cols[7]) == false){
						System.out.println("ERROR: location id (" + cols[0] + ") already present and premises don't match.");
						System.out.println(locations.get(cols[0]).getPremisesType() + "\t" + cols[7]);
					}
					
					// Do the holding types match?
					if(locations.get(cols[0]).getHerdType().matches(cols[6]) == false){
						System.out.println("ERROR: location id (" + cols[0] + ") already present and holding types don't match.");
						System.out.println(locations.get(cols[0]).getHerdType() + "\t" + cols[6]);
					}					
				}
			}			
			
			// Note the progress
			if(lineNo % 100000 == 0){
				System.out.print(".");
			}
		}
		System.out.println();
		
		// Close the locations information file
		input.close();
		reader.close();
		
	}

	public static void getLocationInformationPre2001(String locationInfoFile, double[] wpCentroid, 
			Hashtable<String, Integer> premisesToIgnore, double thresholdDistance, 
			Hashtable<String, Location> locations, Hashtable<String, String> locationsToIgnore) throws IOException{
		/**
		 * Location information file structure:
		 * 	LocationKey	LocationType	LocationId/CPH	PremisesType	MapRef	X	Y	HoldingType	CtyKey	CtyPar
		 * 	0			1				2				3				4		5	6	7			8		9
		 * 
		 * 	vLocationId	Region	Live	Has_Movements
		 * 	10			11		12		13
		 */
		
		// Open the movements file
		InputStream input = new FileInputStream(locationInfoFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
									
		// Note the progress
		System.out.println("Beginning to parse locations information table...");
		
		// Initialise variables necessary for parsing the file
		String line = null;
		int lineNo = 0;
		String[] cols;
		
		// Initialise a variable to record the distance from each herd from Woodchester Park
		double distance;
		double[] coords = new double[2];
					
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			lineNo++;
						
			// Skip the header and last line
			if(lineNo == 1){
				continue;
			}
			
			// Split the line into its columns
			cols = line.split(",", -1);
			
			// Skip if it is a location type we want to ignore
			if(premisesToIgnore.get(cols[3]) != null){
				
				// Store the premise type for the current location
				locationsToIgnore.put(cols[0], cols[3]);
				continue;
			}
			
			// Skip if no location information available
			if(cols[5].matches("") == true || cols[6].matches("") == true){
				continue;
			}
			
			// Is this a location with the threshold distance from Woodchester Park?
			coords[0] = Double.parseDouble(cols[5]);
			coords[1] = Double.parseDouble(cols[6]);
			distance = GeneralMethods.calculateEuclideanDistance(wpCentroid, coords);
			if(distance <= thresholdDistance){
				
				if(locations.get(cols[0]) == null){
					locations.put(cols[0], new Location(cols[0], cols[10], cols[5], cols[6], cols[7], cols[3]));
				}				
			}
			
			// Note the progress
			if(lineNo % 100000 == 0){
				System.out.print(".");
			}
		}
		System.out.println();
		
		// Close the locations information file
		input.close();
		reader.close();
	}

	public static String parsePremisesTypePost2001(String description){
		Hashtable<String, String> acronymesForTypes = new Hashtable<String, String>();
		acronymesForTypes.put("Agency", "AG");
		acronymesForTypes.put("Agricultural Holding","AH");
		acronymesForTypes.put("AI Sub Centre","AI");
		acronymesForTypes.put("Article 18 Premises","AR");
		acronymesForTypes.put("Calf Collection Centre","CA");
		acronymesForTypes.put("Collection Centre (for BSE material)","CC");
		acronymesForTypes.put("Cutting Room","CR");
		acronymesForTypes.put("Cold Store","CS");
		acronymesForTypes.put("Embryo Transfer Unit","ET");
		acronymesForTypes.put("Export Assembly Centre","EX");
		acronymesForTypes.put("Head Boning Plant","HB");
		acronymesForTypes.put("Hunt Kennel","HK");
		acronymesForTypes.put("Incinerator","IN");
		acronymesForTypes.put("Imported Protein Premises","IP");
		acronymesForTypes.put("Knackers Yard","KY");
		acronymesForTypes.put("Landless Keeper","LK");
		acronymesForTypes.put("Market","MA");
		acronymesForTypes.put("Meat Products Plant","MP");
		acronymesForTypes.put("Protein Processing Plant","PP");
		acronymesForTypes.put("Showground","SG");
		acronymesForTypes.put("Slaughterhouse, Both MP and Cold Store","SM");
		acronymesForTypes.put("Slaughterhouse","SR");
		acronymesForTypes.put("Slaughterhouse (Red Meat)","SR");
		acronymesForTypes.put("Semen Shop","SS");
		acronymesForTypes.put("Slaughterhouse (White Meat)","SW");
		acronymesForTypes.put("Waste Food Premises","WF");
		acronymesForTypes.put("No Premises Type Specified","ZZ");
		
		return acronymesForTypes.get(description);
	}
	
}
