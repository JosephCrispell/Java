package woodchesterCattle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.GeneralMethods;
import methods.HashtableMethods;
import methods.MatrixMethods;
import methods.WriteToFile;

public class BuildConsolidatedCattleData {

	public static void main(String[] args) throws IOException {
		
		// Get the date
		String date = CalendarMethods.getCurrentDate("dd-MM-yyyy");
		
		// Set the path
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/NewAnalyses_13-07-17/";
				
		// Read in the Isolate data
		String isolateInfo = path + "IsolateData/" +
		"CattleIsolateInfo_LatLongs_plusID_outbreakSize_Coverage_AddedStrainIDs.csv";
		Hashtable<String, IsolateData> isolateData = BuildCattleLifeHistories.getCattleIsolateData(
				isolateInfo);
				
		// Set a number of days that we'll allow culture days to be before the death date
		int nDays = 90;

		// Get a list of the years CPHs were sampled in - just in case they aren't encountered during movements
		Hashtable<String, int[]> sampledCphs = BuildCattleLifeHistories.getSampledCPHs(isolateData);
		
		/**
		 * DEALING WITH POST-2001 ISOLATES
		 */
		
		// Get the Movement IDs of the isolates
		String animalsTablePost2001 = path + "CattleMovementData-Post2001/20160124_joe_cts_animals.csv";
		Hashtable<String, String> movementIds = BuildCattleLifeHistories.getIsolateMovementIdsPost2001(
				animalsTablePost2001, isolateData, nDays);
		
		// Examine the movements associated with each movement Id
		String movementsFilePost2001Start = path + "CattleMovementData-Post2001/20160123_joe_cts_movements";
		int[] yearsToExamine = ArrayMethods.range(2002, 2014, 1);
		Hashtable<String, Location> locations =  BuildCattleLifeHistories.findMovementsForIdsPost2001(
				movementsFilePost2001Start, yearsToExamine, movementIds, isolateData, 99);
		
		// Read the locations file and get location information for those locations of interest
		String locationInfoFilePost2001 = path + "CattleMovementData-Post2001/20160314_joe_cts_locations.csv";
		BuildCattleLifeHistories.getLocationInformationPost2001(locationInfoFilePost2001, locations,
				sampledCphs);		
		
		/**
		 * DEALING WITH THE PRE-2001 ISOLATES
		 */
		
		// Get the Movement IDs of the Isolates
		String animalsTablePre2001 = path + "CattleMovementData-Pre2001/tblAnimal.csv";
		movementIds = BuildCattleLifeHistories.getIsolateMovementIdsPre2001(animalsTablePre2001,
				isolateData, nDays);
		
		// Examine the movements associated with each movement Id
		String movementsPre2001File = path + "CattleMovementData-Pre2001/viewMovementTransition.csv";
		locations = BuildCattleLifeHistories.findMovementsForIdsPre2001(movementsPre2001File,
				movementIds, isolateData, 99, locations);
		
		// Read the locations file and get location information for those locations of interest
		String locationInfoFilePre2001 = path + "CattleMovementData-Pre2001/viewLocationNoAddress.csv";
		BuildCattleLifeHistories.getLocationInformationPre2001(locationInfoFilePre2001, locations,
				sampledCphs);
		
		/**
		 * Examine the sampled animal movements
		 */
		
		// Print the Collated isolated data
		String consolidatedDataFile = path + "IsolateData/ConsolidatedCattleIsolateData_" + date + ".txt";
		BuildCattleLifeHistories.printSampledAnimalLifeHistories(isolateData, consolidatedDataFile);
		
		/**
		 * Examine the relationships between the locations
		 */
		
		MovementNetwork network = BuildCattleLifeHistories.buildLocationMovementNetwork(locations,
				movementsFilePost2001Start, yearsToExamine, movementsPre2001File);
		BuildCattleLifeHistories.calculateSpatialDistancesBetweenLocations(network);
		
		/**
		 * Get the movement IDs of the animals encountered whilst
		 */
		
		// Get the IDs of the animals encountered whilst examined the sampled animals and their herds
		Hashtable[] idsOfAnimalsEncountered = getMovementIdsOfAnimalsThatInhabitedHerds(locations, movementIds);
		
		// Create IsolateData objects for these encountered animals, also note eartags associated with movement IDs
		Hashtable<String, IsolateData> encounteredAnimalDataPre2001 = buildIsolateDataForEncounteredAnimalsPre2001(
				idsOfAnimalsEncountered, animalsTablePre2001);
		
		Hashtable<String, IsolateData> encounteredAnimalDataPost2001 = buildIsolateDataForEncounteredAnimalsPost2001(
				idsOfAnimalsEncountered, animalsTablePost2001);
		
		// Examine the movements of the encountered animals
		findMovementsForIdsPre2001(movementsPre2001File, idsOfAnimalsEncountered[0], encounteredAnimalDataPre2001, 9999);
		findMovementsForIdsPost2001(movementsFilePost2001Start, yearsToExamine, idsOfAnimalsEncountered[1],
				encounteredAnimalDataPost2001, 9999);
		
		// Print out the information for the animals encountered
		//printSampledAnimalLifeHistoriesForAnimalsEncountered(consolidatedDataFile, encounteredAnimalDataPre2001, "pre");
		//printSampledAnimalLifeHistoriesForAnimalsEncountered(consolidatedDataFile, encounteredAnimalDataPost2001, "post");
		printSampledAnimalLifeHistoriesForAnimalsEncountered(consolidatedDataFile,
				encounteredAnimalDataPre2001, encounteredAnimalDataPost2001, isolateData);
		
		// Get the information for the locations that the animals encountered passed through
		Hashtable<String, Location> locationsPre2001 = getLocationsEncounteredAnimalsPassedThroughPre2001(encounteredAnimalDataPre2001,
				locations);
		getLocationInformationPre2001ForAnimalsEncountered(locationInfoFilePre2001, locationsPre2001);
		Hashtable<String, Location> locationsPost2001 = getLocationsEncounteredAnimalsPassedThroughPost2001(encounteredAnimalDataPost2001,
				locations, locationsPre2001);
		getLocationInformationPost2001ForAnimalsEncountered(locationInfoFilePost2001, locationsPost2001);
		
		/**
		 * Print out the information gleaned from movement database
		 */
		
		// Print the location data
		String locationInfoCollated = path + "IsolateData/CollatedCattleLocationInfo_" + date + ".txt";
		BuildCattleLifeHistories.printLocationInfo(locations, locationInfoCollated);
		printLocationInfoForAnimalsEncountered(locationsPre2001, locationInfoCollated);
		printLocationInfoForAnimalsEncountered(locationsPost2001, locationInfoCollated);
		
		// Print the adjacency matrix
		String adjacencyMatrix = path + "IsolateData/CattleAdjacencyMatrix_" + date + ".txt";
		MatrixMethods.print(network.getAdjacencyMatrix(), adjacencyMatrix, "\t");
		
		// Print the spatial distance matrix
		String spatialDistanceMatrix = path + "IsolateData/CattleSpatialDistanceMatrix_" + date + ".txt";
		MatrixMethods.print(network.getSpatialDistanceMatrix(), spatialDistanceMatrix, "\t");
		
		/**
		 * Count the number of animals that occupied two different CPHs - pairwise comparisons
		 */
		
		int[][] nShared = BuildCattleLifeHistories.countNumberOfSharedAnimalsBetweenLocations(locations);
		
		String nSharedMatrix = path + "IsolateData/NumberOfAnimalsSharedBetweenLocations_" + date + ".txt";
		MatrixMethods.print(nShared, nSharedMatrix, "\t");
	}
	
	public static void examineCattleTestHistory(String testHistoryFile, Hashtable<String, IsolateData> isolates) throws IOException{
		
		/**
		 * Cattle Test Data File Structure:
		 * caAnCoreId	caAnCoreId_old	caCphh	caTestDate	caEartag	caAssetPK	caAnimalPK	caTestRes
		 * 0			1				2		3	***		4	***		5			6			7	***
		 * 
		 * caTestRes2	caAction	caLesSH	caLesCVL	caCVLRef	caCult	caHist	caGenotype	caIRType
		 * 8			9			10		11			12			13		14		15			16
		 * 
		 * caBreakId	caCounty	caRefYr	caAge	caIFNTest	caAvianResult	caBovineResult	caRawEartag
		 * 17			18			19		20		21			22				23				24
		 * 
		 * caCPH	caTestSubjectPK	caTestPK	caPartTestPK	caInterp	caPrevTestDate	caPrevTestRes
		 * 25		26				27			28				29			30				31
		 * 
		 * caReasonNotTested	caRemarks	caTestStatus	caSkinTestDesc	caSkinTestDesc_Avi	caSAMEartag
		 * 32					33			34				35				36					37
		 * 
		 * caReinterpretedRea
		 * 38
		 */
		
		// Open the input file
		InputStream input = new FileInputStream(testHistoryFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
				
		// Initialise variables necessary for parsing the file
		String line = null;
		int lineNo = 0;
		String[] cols;
		
		// Initialise a counter
		int count = 0;
		
		// Initialise variables to parse the test dates: 7/8/1956 00:00:00
		int[] dateFormat = {0, 1, 2}; // Index for Day, Month, Year
		String dateSep = "/";
		Calendar date;
		String dateString;

		// Begin reading the file
		while(( line = reader.readLine()) != null){
			lineNo++;
					
			// Skip the header and last line
			if(lineNo == 1){
				continue;
			}
			
			// Split the line into its columns
			cols = line.split(",");
			
			// Is this an animal that we are interested in?
			cols[4] = GeneralMethods.removeDelimiter(cols[4], " ");
			
			if(isolates.get(cols[4]) != null){
				count++;
				
				// Get the test date
				dateString = cols[3].split(" ")[0];
				date = CalendarMethods.parseDate(dateString, dateSep, dateFormat, true);
				isolates.get(cols[4]).addTestData(date, cols[7]);
			}
			
			// Print some progress information
			if(lineNo % 100000 == 0){
				System.out.print(".");
			}
		}
		
		System.out.println(" (Found " + count + " test results)");
		
		// Close the input file
		input.close();
		reader.close();		
	}
	
	public static void printLocationInfoForAnimalsEncountered(Hashtable<String, Location> locations, String outputFile) throws IOException{
		
		/**
		 * Location Info:
		 *  String locationId;
		 *  int posInAdjacencyMatrix;
		 *  String cph;
		 *  int x = -1;
		 *  int y = -1;
		 *  String herdType;
		 *  String premisesType;
		 *  
		 *  Output file structure:
		 *  LocationId	CPH	AdjacencyPos	X	Y	Herd	PremisesType
		 *  0			1	2				3	4	5		6
		 */
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(outputFile, true);
		
		// Get a list of the Location IDs
		String[] locationIds = HashtableMethods.getKeysString(locations);
		
		// Initialise a variable to store the location info
		Location info;
		
		// Initialise a variable to store the output line
		String output = "";
		
		// Examine each location
		for(String id : locationIds){
			
			if(id.matches("null")){
				continue;
			}
			
			info = locations.get(id);
			
			// Get the location information into the output
			output = info.getLocationId() + "\t" + info.getCph() + "\t" + -1 + "\t";
			output += info.getX() + "\t" + info.getY() + "\t" + info.getHerdType() + "\t" + info.getPremisesType();
			
			// Print the output line
			WriteToFile.writeLn(bWriter, output);					
		}
		
		// Close the output file
		WriteToFile.close(bWriter);
	}
	
	public static void getLocationInformationPost2001ForAnimalsEncountered(String locationInfoFile, Hashtable<String, Location> locations) throws IOException{
		
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
			
			// Parse the CPH
			if(cols[1].matches("") == false){
				cols[1] = BuildCattleLifeHistories.parseCPHPost2001(cols[1]);
			}
			
			// Is this a location that we're interested in?
			if(locations.get(cols[0]) != null){
				
				// Store the CPH
				if(cols[1].matches("") == false){
					locations.get(cols[0]).setCph(cols[1]);
				}
				
				// Store the coordinate information
				if(cols[4].matches("") == false){
					locations.get(cols[0]).setX(cols[4]);
				}
				if(cols[5].matches("") == false){
					locations.get(cols[0]).setY(cols[5]);
				}				
				
				// Store the herd type
				if(cols[6].matches("Not matched") == false){
					locations.get(cols[0]).setHerdType(cols[6]);
				}
				
				// Store the premises type
				if(cols[7].matches("") == false){
					locations.get(cols[0]).setPremisesType(cols[7]);
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
	
	public static void getLocationInformationPre2001ForAnimalsEncountered(String locationInfoFile, Hashtable<String, Location> locations) throws IOException{
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
			
			// Is this a location that we're interested in?
			if(locations.get(cols[0]) != null){
				
				// Store the CPH
				if(cols[10].matches("") == false){
					locations.get(cols[0]).setCph(cols[10]);
				}
				
				// Store the coordinate information
				if(cols[5].matches("") == false){
					locations.get(cols[0]).setX(cols[5]);
				}
				if(cols[6].matches("") == false){
					locations.get(cols[0]).setY(cols[6]);
				}				
				
				// Store the herd type
				if(cols[7].matches("Not matched") == false){
					locations.get(cols[0]).setHerdType(cols[7]);
				}
				
				// Store the premises type
				if(cols[3].matches("") == false){
					locations.get(cols[0]).setPremisesType(cols[3]);
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
	
	public static Hashtable<String, Location> getLocationsEncounteredAnimalsPassedThroughPre2001(
			Hashtable<String, IsolateData> encounteredAnimalDataPre2001,
			Hashtable<String, Location> sampledLocations){
		
		// Get a list of the animal eartags
		String[] eartags = HashtableMethods.getKeysString(encounteredAnimalDataPre2001);
		
		// Initialise a hashtable to store the locations passed through
		Hashtable<String, Location> locations = new Hashtable<String, Location>();
		
		// Examine the movement history of each animal and record the locations that they passed through
		for(int i = 0; i < eartags.length; i++){
			
			for(Movement movement : encounteredAnimalDataPre2001.get(eartags[i]).getMovementRecords()){
				
				// Check if we have already noted the ON and OFF Locations
				if(movement.getOffLocation() != null && sampledLocations.get(movement.getOffLocation()) == null &&
						locations.get(movement.getOffLocation()) == null){
					locations.put(movement.getOffLocation(), new Location(movement.getOffLocation()));
				}
				if(movement.getOnLocation() != null && sampledLocations.get(movement.getOnLocation()) == null &&
						locations.get(movement.getOnLocation()) == null){
					locations.put(movement.getOnLocation(), new Location(movement.getOnLocation()));
				}
			}
		}
		
		return locations;
	}
	
	public static Hashtable<String, Location> getLocationsEncounteredAnimalsPassedThroughPost2001(
			Hashtable<String, IsolateData> encounteredAnimalDataPost2001,
			Hashtable<String, Location> sampledLocations,
			Hashtable<String, Location> locationsPre2001){
		
		// Get a list of the animal eartags
		String[] eartags = HashtableMethods.getKeysString(encounteredAnimalDataPost2001);
		
		// Initialise a hashtable to store the locations passed through
		Hashtable<String, Location> locations = new Hashtable<String, Location>();
		
		// Examine the movement history of each animal and record the locations that they passed through
		for(int i = 0; i < eartags.length; i++){
			
			for(Movement movement : encounteredAnimalDataPost2001.get(eartags[i]).getMovementRecords()){
				
				// Check if we have already noted the ON and OFF Locations
				if(movement.getOffLocation() != null && sampledLocations.get(movement.getOffLocation()) == null &&
						locations.get(movement.getOffLocation()) == null &&
						locationsPre2001.get(movement.getOffLocation()) == null){
					locations.put(movement.getOffLocation(), new Location(movement.getOffLocation()));
				}
				if(movement.getOnLocation() != null && sampledLocations.get(movement.getOnLocation()) == null &&
						locations.get(movement.getOnLocation()) == null &&
						locationsPre2001.get(movement.getOnLocation()) == null){
					locations.put(movement.getOnLocation(), new Location(movement.getOnLocation()));
				}
			}
		}
		
		return locations;
	}
	
	public static boolean checkIfAliveBefore2001(IsolateData isolate){
		
		// Initialise a date for the comparison
		Calendar year = CalendarMethods.createDate(2001);
		
		boolean result = false;
		if(isolate.getBirth() != null && isolate.getBirth().before(year)){
			result = true;
		}else if(isolate.getNMovements() != 0 && isolate.getMovementRecords()[0].getDate().before(year)){
			result = true;
		}
		
		return result;
	}
	
	public static boolean checkIfAliveAfter2001(IsolateData isolate){
		
		// Initialise a date for the comparison
		Calendar year = CalendarMethods.createDate(2001);
		
		boolean result = false;
		if(isolate.getBirth() != null && isolate.getBirth().after(year)){
			result = true;
		}else if(isolate.getMovementRecords() != null && isolate.getMovementRecords()[0].getDate().after(year)){
			result = true;
		}
		
		return result;
	}
	
	public static void printSampledAnimalLifeHistoriesForAnimalsEncountered(String outputFile,
			Hashtable<String, IsolateData> encounteredAnimalData, String dataset) throws IOException{
		
		/**
		 * Isolate info:
		 * 	String eartag;
		 *	String cph;
		 *	String cphh;
		 *	Calendar cultureDate;
		 *	String strainId;
		 *	Calendar breakdownDate;
		 *	
		 *	String movementId;
		 *	Calendar birth;
		 *	Calendar death;
		 *	
		 *	Movement[] movementRecords;
		 *
		 * Movement info:
		 *	 int movementNumber = -1;
		 *	 Calendar date;
		 *	 String offLocation;
		 *	 String onLocation;
		 *	 boolean isBirth;
		 *	 boolean isDeath;
		 *	 int stayLength = -1;
		 *
		 * Output file structure:
		 * 	Eartag	CPH	CPHH	CultureDate	StrainId	BreakdownDate	MovementId	BirthDate	DeathDate
		 * 	0		1	2		3			4			5				6			7			8
		 * 
		 * 	Movements			Number	Date	OFF	ON	isBirth	isDeath	stayLength
		 * 	9 ---------------> 	0		1		2	3	4		5		6
		 */
		
		// Open the output file - APPENDING
		BufferedWriter bWriter = WriteToFile.openFile(outputFile, true);
		
		// Initialise a variable to store the output
		String output = "";
		
		// Initialise a variable to store the movement data
		Movement[] movements;
		
		// Initialise a variable to store the isolate data
		IsolateData isolate;
		
		// Get a list of the isolate Ids
		String[] isolateIds = HashtableMethods.getKeysString(encounteredAnimalData);
		
		// Initialise variables to parse necessary dates
		String cultureDate = "NA";
		String breakdownDate = "NA";
		String birthDate = "NA";
		String deathDate = "NA";
		
		// Examine each isolate
		for(String id : isolateIds){
			
			// Get the isolate data
			isolate = encounteredAnimalData.get(id);
			
			/**
			 *  Check the animals history. Some cattle are duplicated in the two databases
			 *  - If the lifespan of the animal overlaps 2001, it's whole life should be in pre2001 dataset
			 *    Ignore it from post 2001 dataset
			 *  - Some animals born after 2001 are recorded in both. 
			 *    Ignore from pre2001 dataset
			 */
			
//			// If born before 2001, ignore from post2001 dataset
//			if(dataset.matches("post") == true && isolate.getBirth() != null && 
//					checkIfBirthBefore2001(isolate.getBirth()) == true){
//				continue;
//			}
//			
//			// If born after 2001, ignore from pre2001 dataset
//			if(dataset.matches("pre") == true && isolate.getBirth() != null && 
//					checkIfBirthBefore2001(isolate.getBirth()) == false){
//				continue;
//			}
			
			// Create a value to note which dataset the animal was recorded in
			int set = 0;
			if(dataset.matches("pre") == true){
				set = -1;
			}else if(dataset.matches("post") == true){
				set = -2;
			}
			
			// Get the necessary dates
			cultureDate = "NA";
			if(isolate.getCultureDate() != null){
				cultureDate = CalendarMethods.toString(isolate.getCultureDate(), "-");
			}
			breakdownDate = "NA";
			if(isolate.getBreakdownDate() != null){
				breakdownDate = CalendarMethods.toString(isolate.getBreakdownDate(), "-");
			}
			birthDate = "NA";
			if(isolate.getBirth() != null){
				birthDate = CalendarMethods.toString(isolate.getBirth(), "-");
			}
			deathDate = "NA";
			if(isolate.getDeath() != null){
				deathDate = CalendarMethods.toString(isolate.getDeath(), "-");
			}
			
			// Print the sampled animal info
			output = isolate.getEartag() + "\t" + isolate.getCph() + "\t" + isolate.getCphh() + "\t";
			output += cultureDate + "\t" + isolate.getStrainId() + "\t" + breakdownDate + "\t";
			output += isolate.getMovementId() + "\t" + birthDate + "\t" + deathDate + "\t";
			
			// Get the movements
			movements = new Movement[0];
			if(isolate.getNMovements() != 0){
				movements = isolate.getMovementRecords();
			}
			
			// Print information for each movement
			for(int i = 0; i < movements.length; i++){

				output += movements[i].getMovementNumber() + ":" + CalendarMethods.toString(movements[i].getDate(), "-") + ":";
				output += movements[i].getOffLocation() + ":" + movements[i].getOnLocation() + ":" + movements[i].getIsBirth() + ":";
				output += movements[i].getIsDeath() + ":" + movements[i].getStayLength();
				
				if(i < movements.length - 1){
					output += ",";
				}
			}
			
			// Print the finished line
			WriteToFile.writeLn(bWriter, output + "\t" + set); // -1 for pre, -2 for post
		}
		
		// Close the output file
		WriteToFile.close(bWriter);
	}
	
	public static void printSampledAnimalLifeHistoriesForAnimalsEncountered(String outputFile,
			Hashtable<String, IsolateData> encounteredAnimalDataPre2001,
			Hashtable<String, IsolateData> encounteredAnimalDataPost2001,
			Hashtable<String, IsolateData> sampled) throws IOException{
		
		/**
		 * Isolate info:
		 * 	String eartag;
		 *	String cph;
		 *	String cphh;
		 *	Calendar cultureDate;
		 *	String strainId;
		 *	Calendar breakdownDate;
		 *	
		 *	String movementId;
		 *	Calendar birth;
		 *	Calendar death;
		 *	
		 *	Movement[] movementRecords;
		 *
		 * Movement info:
		 *	 int movementNumber = -1;
		 *	 Calendar date;
		 *	 String offLocation;
		 *	 String onLocation;
		 *	 boolean isBirth;
		 *	 boolean isDeath;
		 *	 int stayLength = -1;
		 *
		 * Output file structure:
		 * 	Eartag	CPH	CPHH	CultureDate	StrainId	BreakdownDate	MovementId	BirthDate	DeathDate
		 * 	0		1	2		3			4			5				6			7			8
		 * 
		 * 	Movements			Number	Date	OFF	ON	isBirth	isDeath	stayLength
		 * 	9 ---------------> 	0		1		2	3	4		5		6
		 */
		
		// Open the output file - APPENDING
		BufferedWriter bWriter = WriteToFile.openFile(outputFile, true);
		
		// Initialise a variable to store the output
		String output = "";
		
		// Initialise a variable to store the movement data
		Movement[] movements;
		
		// Initialise a variable to store the isolate data
		IsolateData isolate;
		
		
		/**
		 * PRE-2001 DATASET FIRST
		 */
		
		// Initialise variables to parse necessary dates
		String cultureDate = "NA";
		String breakdownDate = "NA";
		String birthDate = "NA";
		String deathDate = "NA";
		
		// Examine each isolate
		for(String id : HashtableMethods.getKeysString(encounteredAnimalDataPre2001)){
			
			// Get the isolate data
			isolate = encounteredAnimalDataPre2001.get(id);
			
			// Skip any sampled animals
			if(sampled.get(isolate.getEartag()) != null){
				continue;
			}
			
			// If present in post2001 dataset and born after 2001 - IGNORE
			if(encounteredAnimalDataPost2001.get(id) != null && checkIfAliveAfter2001(isolate) == true){
				continue;
			}
			
			// Get the necessary dates
			cultureDate = "NA";
			if(isolate.getCultureDate() != null){
				cultureDate = CalendarMethods.toString(isolate.getCultureDate(), "-");
			}
			breakdownDate = "NA";
			if(isolate.getBreakdownDate() != null){
				breakdownDate = CalendarMethods.toString(isolate.getBreakdownDate(), "-");
			}
			birthDate = "NA";
			if(isolate.getBirth() != null){
				birthDate = CalendarMethods.toString(isolate.getBirth(), "-");
			}
			deathDate = "NA";
			if(isolate.getDeath() != null){
				deathDate = CalendarMethods.toString(isolate.getDeath(), "-");
			}
			
			// Print the sampled animal info
			output = isolate.getEartag() + "\t" + isolate.getCph() + "\t" + isolate.getCphh() + "\t";
			output += cultureDate + "\t" + isolate.getStrainId() + "\t" + breakdownDate + "\t";
			output += isolate.getMovementId() + "\t" + birthDate + "\t" + deathDate + "\t";
			
			// Get the movements
			movements = new Movement[0];
			if(isolate.getNMovements() != 0){
				movements = isolate.getMovementRecords();
			}
			
			// Print information for each movement
			for(int i = 0; i < movements.length; i++){

				output += movements[i].getMovementNumber() + ":" + CalendarMethods.toString(movements[i].getDate(), "-") + ":";
				output += movements[i].getOffLocation() + ":" + movements[i].getOnLocation() + ":" + movements[i].getIsBirth() + ":";
				output += movements[i].getIsDeath() + ":" + movements[i].getStayLength();
				
				if(i < movements.length - 1){
					output += ",";
				}
			}
			
			// Print the finished line
			WriteToFile.writeLn(bWriter, output + "\t" + -1); // -1 for pre, -2 for post
		}
		
		/**
		 * POST-2001 DATASET
		 */

		// Examine each isolate
		for(String id : HashtableMethods.getKeysString(encounteredAnimalDataPost2001)){
			
			// Get the isolate data
			isolate = encounteredAnimalDataPost2001.get(id);
			
			// Skip any sampled animals
			if(sampled.get(isolate.getEartag()) != null){
				continue;
			}
			
			// If present in pre2001 dataset and born before 2001 - IGNORE
			if(encounteredAnimalDataPre2001.get(id) != null && 
					checkIfAliveBefore2001(encounteredAnimalDataPre2001.get(id)) == true){
				continue;
			}
			
			// Get the necessary dates
			cultureDate = "NA";
			if(isolate.getCultureDate() != null){
				cultureDate = CalendarMethods.toString(isolate.getCultureDate(), "-");
			}
			breakdownDate = "NA";
			if(isolate.getBreakdownDate() != null){
				breakdownDate = CalendarMethods.toString(isolate.getBreakdownDate(), "-");
			}
			birthDate = "NA";
			if(isolate.getBirth() != null){
				birthDate = CalendarMethods.toString(isolate.getBirth(), "-");
			}
			deathDate = "NA";
			if(isolate.getDeath() != null){
				deathDate = CalendarMethods.toString(isolate.getDeath(), "-");
			}
			
			// Print the sampled animal info
			output = isolate.getEartag() + "\t" + isolate.getCph() + "\t" + isolate.getCphh() + "\t";
			output += cultureDate + "\t" + isolate.getStrainId() + "\t" + breakdownDate + "\t";
			output += isolate.getMovementId() + "\t" + birthDate + "\t" + deathDate + "\t";
			
			// Get the movements
			movements = new Movement[0];
			if(isolate.getNMovements() != 0){
				movements = isolate.getMovementRecords();
			}
			
			// Print information for each movement
			for(int i = 0; i < movements.length; i++){

				output += movements[i].getMovementNumber() + ":" + CalendarMethods.toString(movements[i].getDate(), "-") + ":";
				output += movements[i].getOffLocation() + ":" + movements[i].getOnLocation() + ":" + movements[i].getIsBirth() + ":";
				output += movements[i].getIsDeath() + ":" + movements[i].getStayLength();
				
				if(i < movements.length - 1){
					output += ",";
				}
			}
			
			// Print the finished line
			WriteToFile.writeLn(bWriter, output + "\t" + -2); // -1 for pre, -2 for post
		}
		
		// Close the output file
		WriteToFile.close(bWriter);
	}
		
	public static void findMovementsForIdsPost2001(String movementsFileStart, int[] yearsToExamine,
			Hashtable<String, String> animalsEncounteredIds, 
			Hashtable<String, IsolateData> encounteredAnimalDataPre2001,
			int limit) throws IOException{
		
		/**
		 * Movements File Structure:
		 * animal_id	movement_number	movement_date	off_location_id	is_trans	trans_location_id
		 * 0			1				2				3				4			5
		 * 
		 * on_location_id	is_birth	is_death	stay_length	is_valid_history
		 * 6				7			8			9			10
		 */
		
		// Initialise a variable to store the movement information
		Movement movementRecord;
		Calendar date;
		int[] dateFormat = {2, 1, 0};
		boolean birth;
		boolean death;
		
		// The movements in any given year are recorded in separate files
		for(int year : yearsToExamine){
			
			// Open the movements file
			InputStream input = new FileInputStream(movementsFileStart + "_" + year + ".csv");
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
							
			// Note the progress
			System.out.println("Beginning to parse movements in year:\t" + year);
			
			// Initialise variables necessary for parsing the file
			String line = null;
			int lineNo = 0;
			String[] cols;
			
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
				
				// Does this movement involve an animal of interest?
				if(animalsEncounteredIds.get(cols[0]) != null){
					
					// Get the date for the current movement
					date = CalendarMethods.parseDate(cols[2], "-", dateFormat, true);
					
					// Determine whether the movement is a birth or death
					birth = BuildCattleLifeHistories.checkIfTrue(cols[7]);
					death = BuildCattleLifeHistories.checkIfTrue(cols[8]);
					
					// Create a record of the current movement
					movementRecord = new Movement(cols[0], cols[1], date, cols[3], cols[6], birth, death, cols[9]);
					
					// Store the movement record
					encounteredAnimalDataPre2001.get(animalsEncounteredIds.get(cols[0])).appendMovement(movementRecord, limit);
				}

				// Note the progress
				if(lineNo % 1000000 == 0){
					System.out.print(".");
				}
			}
			System.out.println();
			
			// Close the current movements file
			input.close();
			reader.close();
			
			System.out.println("Finished parsing movements from year:\t" + year);
		}
	}
	
	public static void findMovementsForIdsPre2001(String movementsFile,
			Hashtable<String, String> animalsEncounteredIds, 
			Hashtable<String, IsolateData> encounteredAnimalDataPre2001,
			int limit) throws IOException{
		
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
		System.out.println("Beginning to parse the movements file...");
		while(( line = reader.readLine()) != null){
			lineNo++;
						
			// Skip the header and last line
			if(lineNo == 1){
				continue;
			}
						
			// Split the line into its columns
			cols = line.split(",", -1);
		
			// Does this movement involve an animal of interest?
			if(animalsEncounteredIds.get(cols[0]) != null){
				
				// Get the date for the current movement
				date = CalendarMethods.parseDate(cols[2].split(" ", -1)[0], "-", dateFormat, true);
				
				// Determine whether the movement is a birth or death
				birth = BuildCattleLifeHistories.checkIfTrue(cols[5]);
				death = BuildCattleLifeHistories.checkIfTrue(cols[6]);
				
				// Create a record of the current movement
				movementRecord = new Movement(cols[0], cols[1], date, cols[3], cols[4], birth, death, cols[10]);
				
				// Store the movement record
				encounteredAnimalDataPre2001.get(animalsEncounteredIds.get(cols[0])).appendMovement(movementRecord, limit);			}
			
			// Note the progress
			if(lineNo % 1000000 == 0){
				System.out.print(".");
			}			
		}
		System.out.println();
		
		// Close the movements file
		input.close();
		reader.close();
	}
	
	public static Hashtable<String, IsolateData> buildIsolateDataForEncounteredAnimalsPre2001(
			Hashtable[] animalsEncounteredIds, String animalsTablePost2001) throws IOException{
		
		/**
		 * Animals table file structure:
		 * 	AnimalId	Breed	Sex	Eartag	CTS_Indicator	BirthDate	DeathDate	ImportCountry
		 * 	0			1		2	3		4				5			6			7
		 * 
		 * 	ImportDate	StandardEartag
		 * 	8			9
		 */
		
		// Open the isolate information file
		InputStream input = new FileInputStream(animalsTablePost2001);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
				
		// Note the format of the birth and death dates
		Calendar birth = null;
		Calendar death = null;
		int[] dateFormat = {2, 1, 0};
		
		// Initialise a Hashtable to store the IsolateData classes - stored under eartag
		Hashtable<String, IsolateData> encounteredAnimalData = new Hashtable<String, IsolateData>();
				
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
		int lineNo = 0;
		
		// Begin reading the file
		System.out.println("Beginning to read the animals table...");
		while(( line = reader.readLine()) != null){
			lineNo++;
					
			// Skip the header line
			if(lineNo == 1){
				continue;
			}
			
			// Split the line into its columns
			cols = line.split(",", -1);
			
			// Skip the line if no eartag is available
			if(cols.length < 10 && cols[3].matches("")){
				continue;
			}
			
			// Is this an animal that we are interested in?
			if(animalsEncounteredIds[0].get(cols[0]) != null){
				
				// Remove any spaces from the eartag
				cols[3] = GeneralMethods.removeDelimiter(cols[3], " ");
				
				// Note the eartag of the current animal
				animalsEncounteredIds[0].put(cols[0], cols[3]);
				
				// Create an IsolateData object for the current animal
				encounteredAnimalData.put(cols[3], new IsolateData(cols[3], null, null, null, null));
				
				// Get the birth and death dates for the current individual if available
				birth = null;
				death = null;
				if(cols[5].matches("") == false){
					birth = CalendarMethods.parseDate(cols[5].split(" ", -1)[0], "-", dateFormat, true);
				}
				if(cols[6].matches("") == false){
					death = CalendarMethods.parseDate(cols[6].split(" ", -1)[0], "-", dateFormat, true);
				}
					
				// Store the isolate's movement id
				encounteredAnimalData.get(cols[3]).setMovementId(cols[0]);
				encounteredAnimalData.get(cols[3]).setBirth(birth);
				encounteredAnimalData.get(cols[3]).setDeath(death);			
			}
			
			// Print some progress information
			if(lineNo % 1000000 == 0){
				System.out.print(".");
			}
		}
		System.out.println();
		
		// Close the animals table
		input.close();
		reader.close();	
		
		return encounteredAnimalData;
		
	}
	
	public static Hashtable<String, IsolateData> buildIsolateDataForEncounteredAnimalsPost2001(
			Hashtable[] animalsEncounteredIds, String animalsTablePost2001) throws IOException{
		
		/**
		 * Animals table file structure:
		 * 	animal_id	eartag	birth_date	death_date	import_country_code	import_date
		 * 	0			1		2			3			4					5
		 */
		
		// Open the isolate information file
		InputStream input = new FileInputStream(animalsTablePost2001);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
				
		// Note the format of the birth and death dates
		Calendar birth = null;
		Calendar death = null;
		int[] dateFormat = {2, 1, 0};
		
		// Initialise a Hashtable to store the IsolateData classes - stored under eartag
		Hashtable<String, IsolateData> encounteredAnimalData = new Hashtable<String, IsolateData>();
		
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
		int lineNo = 0;
												
		// Begin reading the file
		System.out.println("Beginning to read the animals table...");
		while(( line = reader.readLine()) != null){
			lineNo++;
			
			// Skip the header and last line
			if(lineNo == 1 || line.matches("(.*)rows(.*)")){
				continue;
			}
			
			// Split the line into its columns
			cols = line.split(",", -1);
			
			// Is this an animal that we are interested in?
			if(animalsEncounteredIds[1].get(cols[0]) != null){
				
				// Remove any spaces from the eartag
				cols[1] = GeneralMethods.removeDelimiter(cols[1], " ");
				
				// Note the eartag of the current animal
				animalsEncounteredIds[1].put(cols[0], cols[1]);
				
				// Create an IsolateData object for the current animal
				encounteredAnimalData.put(cols[1], new IsolateData(cols[1], null, null, null, null));
				
				// Get the birth and death dates for the current individual if available
				birth = null;
				death = null;
				if(cols[2].matches("") == false){
					birth = CalendarMethods.parseDate(cols[2].split(" ", -1)[0], "-", dateFormat, true);
				}
				if(cols[3].matches("") == false){
					death = CalendarMethods.parseDate(cols[3].split(" ", -1)[0], "-", dateFormat, true);
				}
				
				// Store the isolate's movement id
				encounteredAnimalData.get(cols[1]).setMovementId(cols[0]);
				encounteredAnimalData.get(cols[1]).setBirth(birth);
				encounteredAnimalData.get(cols[1]).setDeath(death);
			}
			
			// Print some progress information
			if(lineNo % 1000000 == 0){
				System.out.print(".");
			}
		}
		System.out.println();
		
		// Close the animals table
		input.close();
		reader.close();		
		
		return encounteredAnimalData;
	}
	
 	public static Hashtable[] getMovementIdsOfAnimalsThatInhabitedHerds(Hashtable<String, Location> locations,
 			Hashtable<String, String> movementIdsOfSampledAnimals){
		
		// Initialise an array of hashtables to store the movement IDs of the animals
		Hashtable[] movementIds = new Hashtable[2];
		movementIds[0] = new Hashtable<String, String>();
		movementIds[1] = new Hashtable<String, String>();
		
		// Get a list of the location IDs
		String[] locationIds = HashtableMethods.getKeysString(locations);
		
		// Initialise a hashtable to store the list of animals inhabited
		Hashtable[] animalsInhabited;
		String[] pre2001;
		String[] post2001;
		
		// Store the animals that inhabited each of the locations
		for(int i = 0; i < locationIds.length; i++){
			
			animalsInhabited = locations.get(locationIds[i]).getAnimalsInhabited();
			pre2001 = HashtableMethods.getKeysString(animalsInhabited[0]);
			for(int pos = 0; pos < pre2001.length; pos++){
				
				if(movementIdsOfSampledAnimals.get(pre2001[pos]) == null){
					movementIds[0].put(pre2001[pos], "NA");
				}
			}
			
			post2001 = HashtableMethods.getKeysString(animalsInhabited[1]);
			for(int pos = 0; pos < post2001.length; pos++){
				if(movementIdsOfSampledAnimals.get(post2001[pos]) == null){
					movementIds[1].put(post2001[pos], "NA");
				}
			}
		}
		
		return movementIds;
	}
}
