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

import contactNetworks.NetworkMethods;
import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.GeneralMethods;
import methods.HashtableMethods;
import methods.MatrixMethods;
import methods.WriteToFile;

public class BuildCattleLifeHistories {

	public static void main(String[] args) throws IOException {
		
		// Get the date
		String date = CalendarMethods.getCurrentDate("dd-MM-yyyy");
		
		// Read in the Isolate data
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/";
		String isolateInfo = path + "NewAnalyses_02-06-16/IsolateData/CattleIsolateInfo_LatLongs_plusID.csv";
		Hashtable<String, IsolateData> isolateData = getCattleIsolateData(isolateInfo);
		
		// Set a number of days that we'll allow culture days to be before the death date
		int nDays = 90;
		
		// Get a list of the years CPHs were sampled in
		Hashtable<String, int[]> sampledCphs = getSampledCPHs(isolateData);
		
		/**
		 * DEALING WITH POST-2001 ISOLATES
		 */
		
		// Get the Movement IDs of the isolates
		String animalsTablePost2001 = path + "CattleMovementData/20160124_joe_cts_animals.csv";
		Hashtable<String, String> movementIds = getIsolateMovementIdsPost2001(animalsTablePost2001,
				isolateData, nDays);
		
		// Examine the movements associated with each movement Id
		String movementsFilePost2001Start = path + "CattleMovementData/20160123_joe_cts_movements";
		int[] yearsToExamine = ArrayMethods.range(2002, 2014, 1);
		Hashtable<String, Location> locations = findMovementsForIdsPost2001(movementsFilePost2001Start,
				yearsToExamine, movementIds, isolateData, 99);
		
		// Read the locations file and get location information for those locations of interest
		String locationInfoFilePost2001 = path + "CattleMovementData/20160314_joe_cts_locations.csv";
		getLocationInformationPost2001(locationInfoFilePost2001, locations, sampledCphs);
		
			
		/**
		 * DEALING WITH THE PRE-2001 ISOLATES
		 */
		
		// Get the Movement IDs of the Isolates
		String animalsTablePre2001 = path + "CattleMovementData-Pre2001/tblAnimal.csv";
		movementIds = getIsolateMovementIdsPre2001(animalsTablePre2001, isolateData, nDays);
		
		// Examine the movements associated with each movement Id
		String movementsPre2001File = path + "CattleMovementData-Pre2001/viewMovementTransition.csv";
		locations = findMovementsForIdsPre2001(movementsPre2001File, movementIds, isolateData, 99,
				locations);
		
		// Read the locations file and get location information for those locations of interest
		String locationInfoFilePre2001 = path + "CattleMovementData-Pre2001/viewLocationNoAddress.csv";
		getLocationInformationPre2001(locationInfoFilePre2001, locations, sampledCphs);
		
		/**
		 * Examine the sampled animal movements
		 */
		
		// Print the Collated isolated data
		String consolidatedDataFile = path + "NewAnalyses_02-06-16/IsolateData/ConsolidatedCattleIsolateData_" + date + ".txt";
		printSampledAnimalLifeHistories(isolateData, consolidatedDataFile);
		
		/**
		 * Examine the relationships between the locations
		 */
		
		MovementNetwork network = buildLocationMovementNetwork(locations, movementsFilePost2001Start, yearsToExamine, movementsPre2001File);
		calculateSpatialDistancesBetweenLocations(network);
		
		// Print the location data
		String locationInfoCollated = path + "NewAnalyses_02-06-16/IsolateData/CollatedCattleLocationInfo_" + date + ".txt";
		printLocationInfo(locations, locationInfoCollated);
		
		// Print the adjacency matrix
		String adjacencyMatrix = path + "NewAnalyses_02-06-16/IsolateData/CattleAdjacencyMatrix_" + date + ".txt";
		MatrixMethods.print(network.getAdjacencyMatrix(), adjacencyMatrix, "\t");
		
		// Print the spatial distance matrix
		String spatialDistanceMatrix = path + "NewAnalyses_02-06-16/IsolateData/CattleSpatialDistanceMatrix_" + date + ".txt";
		MatrixMethods.print(network.getSpatialDistanceMatrix(), spatialDistanceMatrix, "\t");
		
		/**
		 * Count the number of animals that occupied two different CPHs - pairwise comparisons
		 */
		
		int[][] nShared = countNumberOfSharedAnimalsBetweenLocations(locations);
		
		String nSharedMatrix = path + "NewAnalyses_02-06-16/IsolateData/NumberOfAnimalsSharedBetweenLocations_" + date + ".txt";
		MatrixMethods.print(nShared, nSharedMatrix, "\t");
		
	}	
	
	public static int[][] countNumberOfSharedAnimalsBetweenLocations(Hashtable<String, Location> locations){
		
		// Initialise a matrix to store the counts
		int[][] nShared = new int[locations.size()][locations.size()];
		int count;
		
		// Compare the Animals that have occupied each Locations
		String[] ids = HashtableMethods.getKeysString(locations);
		for(int i = 0; i < locations.size(); i++){
			for(int j = 0; j < locations.size(); j++){
				
				// Avoid making the same comparison twice
				if(i > j){
					continue;
				}
				
				// Count the number of shared animals
				count = HashtableMethods.countSharedKeysString(locations.get(ids[i]).getAnimalsInhabited()[0],
						locations.get(ids[j]).getAnimalsInhabited()[0]); // Pre 2001
				count += HashtableMethods.countSharedKeysString(locations.get(ids[i]).getAnimalsInhabited()[1],
						locations.get(ids[j]).getAnimalsInhabited()[1]); // Post 2001
				
				// Store the count
				nShared[locations.get(ids[i]).getPosInAdjacencyMatrix()][locations.get(ids[j]).getPosInAdjacencyMatrix()] = count;
				nShared[locations.get(ids[j]).getPosInAdjacencyMatrix()][locations.get(ids[i]).getPosInAdjacencyMatrix()] = count;				
			}
		}
		
		return nShared;
	}
	
	public static Hashtable<String, Location> readLocationInfo(String collatedLocationInfoFile) throws IOException{
		
		/**
		 *  Output file structure:
		 *  LocationId	CPH	AdjacencyPos	X	Y	HerdType	PremisesType
		 *  0			1	2				3	4	5			6
		 */
		
		// Open the input file
		InputStream input = new FileInputStream(collatedLocationInfoFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise a Hashtable to store the Location info
		Hashtable<String, Location> locations = new Hashtable<String, Location>();
		Location info;
		
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
			cols = line.split("\t", -1);
			
			// Store the Location information
			info = new Location(cols[0]);
			info.setCph(cols[1]);
			info.setPosInAdjacencyMatrix(Integer.parseInt(cols[2]));
			info.setX(cols[3]);
			info.setY(cols[4]);
			info.setHerdType(cols[5]);
			info.setPremisesType(cols[6]);
			
			// Store the Location
			locations.put(cols[0], info);		
		}
		
		// Close the current movements file
		input.close();
		reader.close();
		
		return locations;
	}
	
	public static void printLocationInfo(Hashtable<String, Location> locations, String outputFile) throws IOException{
		
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
		BufferedWriter bWriter = WriteToFile.openFile(outputFile, false);
		
		// Print a header into the output file
		WriteToFile.writeLn(bWriter, "LocationId\tCPH\tAdjacencyPos\tX\tY\tHerdType\tPremisesType");
		
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
			output = info.getLocationId() + "\t" + info.getCph() + "\t" + info.getPosInAdjacencyMatrix() + "\t";
			output += info.getX() + "\t" + info.getY() + "\t" + info.getHerdType() + "\t" + info.getPremisesType();
			
			// Print the output line
			WriteToFile.writeLn(bWriter, output);					
		}
		
		// Close the output file
		WriteToFile.close(bWriter);
	}
	
	public static Hashtable<String, IsolateData> readSampledAnimalLifeHistories(String consolidatedDataFile) throws IOException{
		
		/**
		 * Consolidated Isolate info file structure:
		 * 	Eartag	CPH	CPHH	CultureDate	StrainId	BreakdownDate	MovementId	BirthDate	DeathDate
		 * 	0		1	2		3			4			5				6			7			8
		 * 
		 * 	Movements			Number	Date	OFF	ON	isBirth	isDeath	stayLength
		 * 	9 ---------------> 	0		1		2	3	4		5		6
		 */
		
		// Open the input file
		InputStream input = new FileInputStream(consolidatedDataFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise a Hashtable to store the IsolateData
		Hashtable<String, IsolateData> isolates = new Hashtable<String, IsolateData>();
		IsolateData info;
		
		// Initialise variables necessary for parsing the file
		String line = null;
		int lineNo = 0;
		String[] cols;
		String[] movementInfo;
		String[] movementCols;
		
		// Initialise variables to deal with dates
		Calendar cultureDate;
		Calendar birthDate;
		Calendar deathDate;
		Calendar breakdownDate;
		Calendar movementDate;
		int[] dateFormat = {0, 1, 2};
		
		// Initialise variables to deal with the Movements
		Movement[] movements;
		
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			lineNo++;
			
			// Skip the header and last line
			if(lineNo == 1){
				continue;
			}
						
			// Split the line into its columns
			cols = line.split("\t", -1);
			
			// Has this eartag already been encountered?
			if(isolates.get(cols[0]) != null){
				System.out.println(cols[0] + " has been found before!");
			}
		
			// Deal with the dates
			cultureDate = null;
			if(cols[3].matches("NA") == false){
				cultureDate = CalendarMethods.parseDate(cols[3], "-", dateFormat, true);
			}
			birthDate = null;
			if(cols[7].matches("NA") == false){
				birthDate = CalendarMethods.parseDate(cols[7], "-", dateFormat, true);
			}
			deathDate = null;
			if(cols[8].matches("NA") == false){
				deathDate = CalendarMethods.parseDate(cols[8], "-", dateFormat, true);
			}
			breakdownDate = null;
			if(cols[5].matches("NA") == false){
				breakdownDate = CalendarMethods.parseDate(cols[5], "-", dateFormat, true);
			}
			
			// Store the Isolate data
			info = new IsolateData(cols[0], cols[2], cultureDate, cols[4], new String[0]);
			info.setBirth(birthDate);
			info.setDeath(deathDate);
			info.setBreakdownDate(breakdownDate);
			info.setMovementId(cols[6]);
			info.setCph(cols[1]);
			
			//	Number	Date	OFF	ON	isBirth	isDeath	stayLength
			//	0		1		2	3	4		5		6
			
			// Examine the Movements
			movements = new Movement[0];
			movementInfo = cols[9].split(",", -1);
			
			// Check that there are movements
			if(cols[9].matches("") == false){
				movements = new Movement[movementInfo.length];
				
				for(int i = 0; i < movementInfo.length; i++){
					
					movementCols = movementInfo[i].split(":", -1);
				
					// Get the Movement date
					movementDate = null;
					if(movementCols[1].matches("NA") == false){
						movementDate = CalendarMethods.parseDate(movementCols[1], "-", dateFormat, true);
					}
				
					// Store the Movement
					movements[i] = new Movement(cols[6], movementCols[0], movementDate, movementCols[2], movementCols[3], Boolean.parseBoolean(movementCols[4]), Boolean.parseBoolean(movementCols[5]), movementCols[6]);
				}
				
				// Store the movement data
				info.setMovementRecords(movements);
			}
			
			// Store all the isolate data under the eartag
			isolates.put(cols[0], info);
			
			// Print progress
			if(lineNo % 10000 == 0){
				System.out.print(".");
			}
		}
		System.out.println();
		
		// Close the current movements file
		input.close();
		reader.close();
		
		return isolates;
	}
	
	public static void printSampledAnimalLifeHistories(Hashtable<String, IsolateData> isolateInfo, String outputFile) throws IOException{
		
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
		 * 	Movements			Number	Date	OFF	ON	isBirth	isDeath	stayLength		Sampled
		 * 	9 ---------------> 	0		1		2	3	4		5		6				10
		 */
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(outputFile, false);
		
		// Insert a file header
		String header = "Eartag\tCPH\tCPHH\tCultureDate\tStrainId\tBreakdownDate\tMovementId\tBirthDate\tDeathDate\t";
		header += "Movements-Number:Date:OFF:ON:isBirth:isDeath:stayLength";
		header += "\tSampled";
		WriteToFile.writeLn(bWriter, header);
		
		// Initialise a variable to store the output
		String output = "";
		
		// Initialise a variable to store the movement data
		Movement[] movements;
		
		// Initialise a variable to store the isolate data
		IsolateData isolate;
		
		// Get a list of the isolate Ids
		String[] isolateIds = HashtableMethods.getKeysString(isolateInfo);
		
		// Initialise variables to parse necessary dates
		String cultureDate = "NA";
		String breakdownDate = "NA";
		String birthDate = "NA";
		String deathDate = "NA";
		
		// Examine each isolate
		for(String id : isolateIds){
			
			// Get the isolate data
			isolate = isolateInfo.get(id);
			
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
			WriteToFile.writeLn(bWriter, output + "\t1");
		}
		
		// Close the output file
		WriteToFile.close(bWriter);
	}
	
	public static void calculateSpatialDistancesBetweenLocations(MovementNetwork network){
		
		// Initialise a matrix to store the spatial distances between locations
		double[][] distances = new double[network.getNLocations()][network.getNLocations()];
		
		// Get the Location information
		Hashtable<String, Location> locationInfo = network.getLocations();
		
		// Initialise a variable to store the spatial distance
		double distance = 0;
		
		// Compare each of the Locations to one another
		String[] locations = HashtableMethods.getKeysString(locationInfo);
		for(int a = 0; a < locations.length; a++){
			
			for(int b = 0; b < locations.length; b++){
				
				// Skip self comparisons and making the same comparison twice
				if(a >= b){
					continue;
				}
				
				// Calculate the spatial distance between the current two locations being compared
				distance = calculateDistanceBetweenLocations(locationInfo.get(locations[a]), locationInfo.get(locations[b]));
				
				// Store the calculated distance
				distances[locationInfo.get(locations[a]).getPosInAdjacencyMatrix()][locationInfo.get(locations[b]).getPosInAdjacencyMatrix()] = distance;
				distances[locationInfo.get(locations[b]).getPosInAdjacencyMatrix()][locationInfo.get(locations[a]).getPosInAdjacencyMatrix()] = distance;
			}
		}
		
		// Store the spatial distance matrix
		network.setSpatialDistanceMatrix(distances);
	}
	
	public static double calculateDistanceBetweenLocations(Location a, Location b){
		
		double distance = -1;
		
		double value;
		
		// Check that both the locations have x and y coordinates
		if(a.getX() != -1 && a.getY() != -1 && b.getX() != -1 && b.getY() != -1){
			
			value = Math.pow((double)(b.getX() - a.getX()), 2);
			value += Math.pow((double)(b.getY() - a.getY()), 2);
			distance = Math.sqrt(value);
		}
		
		return distance;
	}
	
	public static MovementNetwork buildLocationMovementNetwork(Hashtable<String, Location> locations,
			String movementsFileStartPost2001, int[] yearsToExaminePost2001,
			String movementsFilePre2001) throws IOException{
		
		// Initialise a weighted adjacency matrix to record the number of movements linking locations
		int[][] adjacencyMatrix = new int[locations.size()][locations.size()];
		
		// Assign each location an index in the adjacency matrix
		assignLocationsWithIndices(locations);
		
		// Read the Post-2001 movements
		countMovementsBetweenLocationsPost2001(movementsFileStartPost2001, yearsToExaminePost2001,
				adjacencyMatrix, locations);
		
		// Read the Pre-2001 movements
		countMovementsBetweenLocationsPre2001(movementsFilePre2001, adjacencyMatrix, locations);
		
		// Store the weighted adjacency matrix
		MovementNetwork network = new MovementNetwork(locations, adjacencyMatrix);
		
		return network;		
	}
	
	public static void countMovementsBetweenLocationsPre2001(String movementsFile, int[][] adjacencyMatrix,
			Hashtable<String, Location> locations) throws IOException{
		
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
		System.out.println("Beginning to parse the movements file...");
		while(( line = reader.readLine()) != null){
			lineNo++;
						
			// Skip the header
			if(lineNo == 1){
				continue;
			}
						
			// Split the line into its columns
			cols = line.split(",", -1);
		
			// Does the current movement connect any of our herds of interest?
			if(locations.get(cols[3]) != null && locations.get(cols[4]) != null){
				adjacencyMatrix[locations.get(cols[3]).getPosInAdjacencyMatrix()][locations.get(cols[4]).getPosInAdjacencyMatrix()]++;
				
				// Add the animal to the list of animals inhabited
				locations.get(cols[3]).addAnimal(0, cols[0]);
				locations.get(cols[4]).addAnimal(0, cols[0]);
			}
			
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
	
	public static void countMovementsBetweenLocationsPost2001(String movementsFileStart, int[] yearsToExamine,
			int[][] adjacencyMatrix, Hashtable<String, Location> locations) throws IOException{
		/**
		 * Movements File Structure:
		 * animal_id	movement_number	movement_date	off_location_id	is_trans	trans_location_id
		 * 0			1				2				3				4			5
		 * 
		 * on_location_id	is_birth	is_death	stay_length	is_valid_history
		 * 6				7			8			9			10
		 */
		
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
				if(lineNo == 1 || line.matches("(.*)rows(.*)") == true){
					continue;
				}
				
				// Split the line into its columns
				cols = line.split(",", -1);
				
				// Skip transitory movement records
				if(cols[4].matches("f")){
					continue;
				}
				
				// Does the current movement connect any of our herds of interest?
				if(locations.get(cols[3]) != null && locations.get(cols[6]) != null){
					adjacencyMatrix[locations.get(cols[3]).getPosInAdjacencyMatrix()][locations.get(cols[6]).getPosInAdjacencyMatrix()]++;
				
					// Add the animal to the list of animals inhabited
					locations.get(cols[3]).addAnimal(1, cols[0]);
					locations.get(cols[6]).addAnimal(1, cols[0]);
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
	
	public static void assignLocationsWithIndices(Hashtable<String, Location> locations){
		
		int index = -1;
		for(String locationId : HashtableMethods.getKeysString(locations)){
			index++;
			locations.get(locationId).setPosInAdjacencyMatrix(index);
		}
	}
	
	public static Hashtable<String, Location> findMovementsForIdsPre2001(String movementsFile,
			Hashtable<String, String> movementIds, Hashtable<String, IsolateData> isolateData,
			int limit, Hashtable<String, Location> locations) throws IOException{
		
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
			if(movementIds.get(cols[0]) != null){
				
				// Get the date for the current movement
				date = CalendarMethods.parseDate(cols[2].split(" ", -1)[0], "-", dateFormat, true);
				
				// Determine whether the movement is a birth or death
				birth = checkIfTrue(cols[5]);
				death = checkIfTrue(cols[6]);
				
				// Create a record of the current movement
				movementRecord = new Movement(cols[0], cols[1], date, cols[3], cols[4], birth, death, cols[10]);
				
				// Store the movement record
				isolateData.get(movementIds.get(cols[0])).appendMovement(movementRecord, limit);
				
				// Have we come across these locations before?
				checkIfNewLocation(cols[3], locations);
				checkIfNewLocation(cols[4], locations);					
			}
			
			// Note the progress
			if(lineNo % 1000000 == 0){
				System.out.print(".");
			}			
		}
		System.out.println();
		
		// Close the movements file
		input.close();
		reader.close();
		
		return locations;
	}
	
	public static void getLocationInformationPre2001(String locationInfoFile, Hashtable<String, Location> locations,
			Hashtable<String, int[]> sampledCphs) throws IOException{
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
		
		// Initialise variable to store location information
		Location location;
					
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
			
			// Is this a Location that was sampled but not encountered in the movements?
			}else if(sampledCphs.get(cols[10]) != null && checkIfCPHSampledPre2002(sampledCphs.get(cols[10]))){
								
				// Create the location
				location = new Location(cols[0]);
				locations.put(cols[0], location);
								
				// Store the CPH
				if(cols[1].matches("") == false){
					locations.get(cols[0]).setCph(cols[10]);
				}
								
				// Store the coordinate information
				if(cols[4].matches("") == false){
					locations.get(cols[0]).setX(cols[5]);
				}
				if(cols[5].matches("") == false){
					locations.get(cols[0]).setY(cols[6]);
				}				
								
				// Store the herd type
				if(cols[6].matches("Not matched") == false){
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
	
	public static Hashtable<String, int[]> getSampledCPHs(Hashtable<String, IsolateData> isolateData){
		
		// Initialise a Hashtable to store the years that CPHs were sampled
		Hashtable<String, int[]> sampledCphs = new Hashtable<String, int[]>();
		
		// Initialise an array to store years
		int[] years;
		
		// Examine each of the sampled individuals and record the years that each CPH were sampled in
		for(String key : HashtableMethods.getKeysString(isolateData)){
			
			// Have we come across this CPH before?
			if(sampledCphs.get(isolateData.get(key).getCph()) != null){
				
				sampledCphs.put(isolateData.get(key).getCph(), ArrayMethods.append(sampledCphs.get(isolateData.get(key).getCph()), isolateData.get(key).getCultureDate().get(Calendar.YEAR)));
			}else{
				
				years = new int[1];
				years[0] = isolateData.get(key).getCultureDate().get(Calendar.YEAR);
				sampledCphs.put(isolateData.get(key).getCph(), years);
			}			
		}
		
		return sampledCphs;
	}
	
	public static void getLocationInformationPost2001(String locationInfoFile, Hashtable<String, Location> locations,
			Hashtable<String, int[]> sampledCPHs) throws IOException{
		
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
		
		// Initialise variable to store location information
		Location location;
					
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
				cols[1] = parseCPHPost2001(cols[1]);
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
			
			// Is this a Location that was sampled but not encountered in the movements?
			}else if(sampledCPHs.get(cols[1]) != null && checkIfCPHSampledPost2001(sampledCPHs.get(cols[1]))){
				
				// Create the location
				location = new Location(cols[0]);
				locations.put(cols[0], location);
				
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
	
	public static boolean checkIfCPHSampledPre2002(int[] years){
		boolean result = false;
		for(int year : years){
			if(year < 2002){
				result = true;
				break;
			}
		}
		
		return result;
	}
	
	public static boolean checkIfCPHSampledPost2001(int[] years){
		
		boolean result = false;
		for(int year : years){
			if(year > 2001){
				result = true;
				break;
			}
		}
		
		return result;
	}
	
	public static String parseCPHPost2001(String input){
		
		String[] parts = input.split("/", -1);
		
		return parts[0] + "" + parts[1] + "" + parts[2].split("-", -1)[0];
	}
	
	public static Hashtable<String, Location> findMovementsForIdsPost2001(String movementsFileStart, int[] yearsToExamine,
			Hashtable<String, String> movementIds, Hashtable<String, IsolateData> isolateData,
			int limit) throws IOException{
		
		/**
		 * Movements File Structure:
		 * animal_id	movement_number	movement_date	off_location_id	is_trans	trans_location_id
		 * 0			1				2				3				4			5
		 * 
		 * on_location_id	is_birth	is_death	stay_length	is_valid_history
		 * 6				7			8			9			10
		 */
		
		// Initialise the Movement Network - in this method we are only recording locations encountered
		Hashtable<String, Location> locations = new Hashtable<String, Location>();
		
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
				if(movementIds.get(cols[0]) != null){
					
					// Get the date for the current movement
					date = CalendarMethods.parseDate(cols[2], "-", dateFormat, true);
					
					// Determine whether the movement is a birth or death
					birth = checkIfTrue(cols[7]);
					death = checkIfTrue(cols[8]);
					
					// Create a record of the current movement
					movementRecord = new Movement(cols[0], cols[1], date, cols[3], cols[6], birth, death, cols[9]);
					
					// Store the movement record
					isolateData.get(movementIds.get(cols[0])).appendMovement(movementRecord, limit);
					
					// Have we come across these locations before?
					checkIfNewLocation(cols[3], locations);
					checkIfNewLocation(cols[6], locations);					
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
		
		return locations;
	}
	
	public static void checkIfNewLocation(String id, Hashtable<String, Location> locations){

		if(locations.get(id) == null && id.matches("") == false){
			locations.put(id, new Location(id));
		}
	}
	
	public static boolean checkIfTrue(String input){
		
		boolean result = false;
		if(input.matches("t")){
			return true;
		}
		
		return result;
	}
	
	public static Hashtable<String, String> getIsolateMovementIdsPre2001(String animalsTable, Hashtable<String, IsolateData> isolateData,
			int nDays) throws IOException{
		
		/**
		 * Animals table file structure:
		 * 	AnimalId	Breed	Sex	Eartag	CTS_Indicator	BirthDate	DeathDate	ImportCountry
		 * 	0			1		2	3		4				5			6			7
		 * 
		 * 	ImportDate	StandardEartag
		 * 	8			9
		 */
		
		// Open the isolate information file
		InputStream input = new FileInputStream(animalsTable);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
				
		// Note the format of the birth and death dates
		Calendar birth = null;
		Calendar death = null;
		int[] dateFormat = {2, 1, 0};
		
		// Initialise a Hashtable to link movement IDs to isolate data
		Hashtable<String, String> linkTable = new Hashtable<String, String>();
		
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
		int lineNo = 0;
		
		// Initialise a variable to keep track of how many isolates were found
		int nFound = 0;
												
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
			
			if(isolateData.get(cols[9]) != null){
				
				// Check that the isolate was sampled before 2002
				if(isolateData.get(cols[9]).getBreakdownDate().get(Calendar.YEAR) < 2002){
				
					nFound++;
					
					// Get the birth and death dates for the current individual if available
					birth = null;
					death = null;
					if(cols[5].matches("") == false){
						birth = CalendarMethods.parseDate(cols[5].split(" ", -1)[0], "-", dateFormat, true);
					}
					if(cols[6].matches("") == false){
						death = CalendarMethods.parseDate(cols[6].split(" ", -1)[0], "-", dateFormat, true);
					}
					
					// Check that the isolate's culture date is after/close to the death date
					if(death != null && isolateData.get(cols[9]).getCultureDate().compareTo(death) < 0 &&
							CalendarMethods.checkIfDatesAreWithinRange(isolateData.get(cols[9]).getCultureDate(), death, nDays) == false){
						System.out.println(isolateData.get(cols[9]).getEartag());
						System.out.println(CalendarMethods.toString(isolateData.get(cols[9]).getCultureDate(), "-"));
						System.out.println(CalendarMethods.toString(death, "-"));
						System.out.println("------------------------------------------------------------");
						continue;
					}
					
					// Store the isolate's movement id
					isolateData.get(cols[9]).setMovementId(cols[0]);
					isolateData.get(cols[9]).setBirth(birth);
					isolateData.get(cols[9]).setDeath(death);
					
					// Add the movement ID into the link table
					linkTable.put(cols[0], cols[9]);
				}
			}
			
			// Print some progress information
			if(lineNo % 1000000 == 0){
				System.out.print(".");
			}
		}		
		System.out.println("\nNumber Isolates Found = " + nFound);
		
		// Close the animals table
		input.close();
		reader.close();	
		
		return linkTable;
	}
	
	public static Hashtable<String, String> getIsolateMovementIdsPost2001(String animalsTable, Hashtable<String, IsolateData> isolateData,
			int nDays) throws IOException{
		
		/**
		 * Animals table file structure:
		 * 	animal_id	eartag	birth_date	death_date	import_country_code	import_date
		 * 	0			1		2			3			4					5
		 */
		
		// Open the isolate information file
		InputStream input = new FileInputStream(animalsTable);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
				
		// Note the format of the birth and death dates
		Calendar birth = null;
		Calendar death = null;
		int[] dateFormat = {2, 1, 0};
		
		// Initialise a Hashtable to link movement IDs to isolate data
		Hashtable<String, String> linkTable = new Hashtable<String, String>();
		
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
		int lineNo = 0;
		
		// Initialise a variable to keep track of how many isolates were found
		int nFound = 0;
												
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
			
			// Is this an isolate that we are interested in?
			if(isolateData.get(cols[1]) != null){
				
				// Check that the isolate was sampled after 2001
				if(isolateData.get(cols[1]).getBreakdownDate().get(Calendar.YEAR) > 2001){
				
					nFound++;
					
					// Get the birth and death dates for the current individual if available
					birth = null;
					death = null;
					if(cols[2].matches("") == false){
						birth = CalendarMethods.parseDate(cols[2].split(" ", -1)[0], "-", dateFormat, true);
					}
					if(cols[3].matches("") == false){
						death = CalendarMethods.parseDate(cols[3].split(" ", -1)[0], "-", dateFormat, true);
					}	
					
					// Check that the isolate's culture date is after/close the death date
					if(death != null && isolateData.get(cols[1]).getCultureDate().compareTo(death) < 0 &&
							CalendarMethods.checkIfDatesAreWithinRange(isolateData.get(cols[1]).getCultureDate(), death, nDays) == false){
						System.out.println(isolateData.get(cols[1]).getEartag());
						System.out.println(CalendarMethods.toString(isolateData.get(cols[1]).getCultureDate(), "-"));
						System.out.println(CalendarMethods.toString(death, "-"));
						System.out.println("------------------------------------------------------------");
						continue;
					}
					
					// Store the isolate's movement id
					isolateData.get(cols[1]).setMovementId(cols[0]);
					isolateData.get(cols[1]).setBirth(birth);
					isolateData.get(cols[1]).setDeath(death);
					
					// Add the movement ID into the link table
					linkTable.put(cols[0], cols[1]);
				}
			}
			
			// Print some progress information
			if(lineNo % 1000000 == 0){
				System.out.print(".");
			}
		}		
		System.out.println("\nNumber Isolates Found = " + nFound);
		
		// Close the animals table
		input.close();
		reader.close();
		
		return linkTable;
	}
	
	public static Hashtable<String, IsolateData> getCattleIsolateData(String isolateInfoFileName) throws IOException{
		
		/**
		 * Structure of the cattle isolate information table:
		 * 	CPH_10km	SampleRef	CultureResult	DateCultured	ReasonForSlaughter	SkinTestType
		 * 	0			1			2				3				4					5
		 * 	
		 * 	Species	DeerType	Mapx	Mapy	MapRef	LesionsFound	Eartag	Database	CPH	CPHH	
		 * 	6		7			8		9		10		11				12		13			14	15
		 * 
		 * 	BreakdownID	County	BadgerInvestigationNo	Year	Spoligotype	Genotype	ComplVNTR	VNTR
		 * 	16			17		18						19		20			21			22			23
		 * 
		 * 	Profile2	VNStatus	VNPtA	VNPtB	VNPtC	VNPtD	VNPtE	VNPtF	VNPtG	VNPtH	VNPtI	
		 * 	24			25			26		27		28		29		30		31		32		33		34
		 * 
		 * 	Rawtag	BreakYr	VLA.Genotype	GenotypeFirst	GenotypeSecond	GenotypeThird	TypingYear	Name
		 * 	35		36		37				38				39				40				41			42
		 * 
		 * 	Location	Expected_Genotype1	Expected_Genotype2	Expected_Genotype3	Expected_Genotype4	
		 * 	43			44					45					46					47
		 * 
		 * 	Expected_Genotype5	Latitude	Longitude	StrainId	MovementsAnimalID
		 * 	48					49			50			51			52
		 * 
		 * BreakdownID: CPHH-Date: 14082000501-23/02/1999
		 */
		
		// Initialise a Hashtable to store each isolate's data
		Hashtable<String, IsolateData> isolateData = new Hashtable<String, IsolateData>();
		IsolateData info;
		String cphh;
		
		// Open the isolate information file
		InputStream input = new FileInputStream(isolateInfoFileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise variables to deal with the date
		int[] dateFormat = {0, 1, 2};
		Calendar cultureDate;
		Calendar breakdownDate;
		
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
		int lineNo = 0;
										
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			lineNo++;
			
			// Skip the header line
			if(lineNo == 1){
				continue;
			}
			
			// Split the line into its columns
			cols = line.split(",", -1);
			
			// Get the date of the current isolate
			cultureDate = CalendarMethods.parseDate(cols[3], "/", dateFormat, true);
			breakdownDate = CalendarMethods.parseDate(cols[16].split("-", -1)[1], "/", dateFormat, true);
			
			// Get the CPHH
			cphh = cols[16].split("-", -1)[0];
			
			// Store the isolates information
			info = new IsolateData(cols[35], cphh, cultureDate, cols[51], cols);
			info.setCph(cphh.substring(0, cphh.length() - 2));
			info.setBreakdownDate(breakdownDate);
			
			// Add the isolate's info into the hashtable
			isolateData.put(cols[35], info);
		}
		
		// Close the cattle isolate information table
		input.close();
		reader.close();
		
		return isolateData;
	}
}
