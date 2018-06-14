package woodchesterCattle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.GeneralMethods;
import methods.HashtableMethods;
import methods.WriteToFile;

public class FindAnimalsInHerdOnDate {

	public static void main(String[] args) throws IOException {
		
		// Get the date
		String date = CalendarMethods.getCurrentDate("dd-MM-yyyy");
		
		// Set the path
		String path = "/home/josephcrispell/Desktop/Research/Woodchester_CattleAndBadgers/NewAnalyses_22-03-18/CattleMovementData-Post2001/";

		// Read in the cattle locations table - keep information for those within X metres from badger centre
		double[] badgerCentre = {381761.7, 200964.3};
		String locationsTable = path + "20160314_joe_cts_locations.csv";
		double threshold = 15000;
		Hashtable<String, Location> locations = readLocationsTablePost2001SelectHerdsCloseToWP(locationsTable, badgerCentre, threshold);

		// Remove locations types that we aren't interested in
		String[] premisesTypesToIgnore = {"SR", "CC", "SW", "EX"};
		removeLocationsOfTypes(locations, premisesTypesToIgnore);
		System.out.println("Found " + locations.size() + " locations");
		
		// Read the post-2001 cattle movements and record the number of animals present on each herd on the date of interest
		int[] yearsToExamine = ArrayMethods.seq(2002, 2014, 1);
		String movementFilePrefix = path + "20160123_joe_cts_movements_";
		recordMovementsOnHerdsPost2001(movementFilePrefix, yearsToExamine, locations);
		
//		// STOPPING POINT - print information gathered for locations of interest
//		printDatesOfAnimalsInhabitedOfLocationsOfInterest(path + "herdOfInterestInfo_" + date + ".txt", locations);
//		Hashtable<String, Location> locations = readInDatesOfAnimalsInhabitedOfLocationsOfInterest(path + "herdOfInterestInfo_" + date + ".txt");
		
		// Set the date of interest
		int[] dayMonthYear = {0, 1, 2};
		String[] datesAsStrings = {"15/06/2003", "15/06/2004", "15/06/2005", "15/06/2006", "15/06/2007", "15/06/2008", "15/06/2009", "15/06/2010", "15/06/2011", "15/06/2012", "15/06/2013"};
		Calendar[] datesOfInterest = CalendarMethods.parseDates(datesAsStrings, "/", dayMonthYear, true);
		
		// Calculate the herd sizes on each of the dates of interest
		String outputFile = path + "herdSizeDistribution_" + date + ".txt";
		calculateHerdSizesOnDates3(outputFile, locations, datesOfInterest, yearsToExamine);
	}
	
	public static Hashtable<String, Location> readInDatesOfAnimalsInhabitedOfLocationsOfInterest(String fileName) throws IOException{
		
		// Open the input file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
		String[] coordinates = new String[2];

		// Initialise variable to store location information
		Hashtable<String, Location> locations = new Hashtable<String, Location>();
		
		// Initialise variables to parse the movement data
		int[] dateFormat = {0, 1, 2};
		
		// Begin reading the file
		while(( line = reader.readLine()) != null){
								
			// Skip the header
			if(line.matches("Location(.*)") == true){
				continue;
			}
					
			// Split the current line into its columns
			cols = line.split("\t", -1);
			
			// Create the location record
			locations.put(cols[0], new Location(cols[0]));
			
			// Store CPH
			locations.get(cols[0]).setCph(cols[1]);
			
			// Store the herd type
			if(cols[2].matches("null") == false && cols[2].matches("") == false) {
				locations.get(cols[0]).setHerdType(cols[2]);
			}		
			
			// Store Premises type
			if(cols[3].matches("null") == false && cols[3].matches("") == false) {
				locations.get(cols[0]).setPremisesType(cols[3]);
			}
			
			// Store Location
			coordinates = cols[4].split(":");
			locations.get(cols[0]).setX(coordinates[0]);
			locations.get(cols[0]).setY(coordinates[1]);
			
			// Store dates for animals inhabited
			if(cols.length > 5) {
				
				// Iterate through animal dates - each in new column
				for(int colIndex = 5; colIndex < cols.length; colIndex++) {
					
					// Examine the dates for the current animal
					String[] animalInfo = cols[colIndex].split(",", -1);
					for(int i = 1; i < animalInfo.length; i++) {
						
						// Split the dates into ON and OFF
						String[] dates = animalInfo[i].split(":");
						
						// Store the ON date
						if(dates[0].matches("NA") == false) {
							Calendar date = CalendarMethods.parseDate(dates[0], "-", dateFormat, true);
							locations.get(cols[0]).addInhabitant(animalInfo[0], date, false);
						}
						
						// Store the OFF date
						if(dates[1].matches("NA") == false) {
							Calendar date = CalendarMethods.parseDate(dates[1], "-", dateFormat, true);
							locations.get(cols[0]).addInhabitant(animalInfo[0], date, false);
						}
					}					
				}
			}
		}
		
		// Close the input file
		input.close();
		reader.close();
		
		return(locations);
	}
	
	public static void printDatesOfAnimalsInhabitedOfLocationsOfInterest(String fileName, Hashtable<String, Location> locations) throws IOException {
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		bWriter.write("Location\tCPH\tHerdType\tPremisesType\tLocation\tDatesForAnimalsInhabited\n");
		
		// Examine each location
		for(String locationId : HashtableMethods.getKeysString(locations)){
			
			// Write out the information available for the current location
			bWriter.write(locationId + "\t" + locations.get(locationId).getCph() + "\t" + locations.get(locationId).getHerdType() + "\t" + locations.get(locationId).getPremisesType() +
					"\t" + locations.get(locationId).getX() + ":" + locations.get(locationId).getY());
			
			// Examine each animal associated with the current herd
			for(String animalId : locations.get(locationId).getInhabitantIds()){
							
				// Get the ON and OFF dates for the current animal
				Calendar[] onDates = locations.get(locationId).getAnimalsOnMovementDates().get(animalId);
				Calendar[] offDates = locations.get(locationId).getAnimalsOffMovementDates().get(animalId);
				
				// Iterate through the dates
				int length = CalendarMethods.getLength(onDates);
				if(CalendarMethods.getLength(offDates) > CalendarMethods.getLength(onDates)) {
					length = CalendarMethods.getLength(offDates);
				}
				for(int dateIndex = 0; dateIndex < length; dateIndex++) {
					
					// Check if first date - add animal separator
					if(dateIndex == 0) {
						bWriter.write("\t" + animalId);
					}
					
					// Print the current ON date
					if(CalendarMethods.getLength(onDates) > dateIndex) {
						bWriter.write("," + CalendarMethods.toString(onDates[dateIndex], "-") + ":");
					}else {
						bWriter.write(",NA:");
					}
					
					// Print the current OFF date
					if(CalendarMethods.getLength(offDates) > dateIndex) {
						bWriter.write(CalendarMethods.toString(offDates[dateIndex], "-"));
					}else {
						bWriter.write("NA");
					}
				}
			}
			
			bWriter.write("\n");
		}
		
		bWriter.close();
	}

	public static void calculateHerdSizesOnDates3(String fileName, Hashtable<String, Location> locations, Calendar[] datesOfInterest, int[] yearsToExamine) throws IOException{
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		bWriter.write("Location\tCPH\t" + CalendarMethods.toString(datesOfInterest, "-", "\t") + "\tHerdType\tPremisesType\n");
		
		// Examine each location
		for(String locationId : HashtableMethods.getKeysString(locations)){
			
			int[] herdSizes = new int[datesOfInterest.length];
			
			// Examine each animal associated with the current herd
			for(String animalId : locations.get(locationId).getInhabitantIds()){
				
				// Get the ON and OFF dates for the current animal
				Calendar[] onDates = new Calendar[0];
				if(locations.get(locationId).getAnimalsOnMovementDates().get(animalId) != null) {
					onDates = CalendarMethods.copy(locations.get(locationId).getAnimalsOnMovementDates().get(animalId));
				}
				Calendar[] offDates = new Calendar[0];
				if(locations.get(locationId).getAnimalsOffMovementDates().get(animalId) != null) {
					offDates = CalendarMethods.copy(locations.get(locationId).getAnimalsOffMovementDates().get(animalId));
				}
				
				// Skip when no ON or OFF dates available
				if(onDates.length == 0 && offDates.length == 0) {
					continue;
				}
				
				// Order the dates
				if(onDates.length > 1){
					onDates = CalendarMethods.orderArray(onDates, CalendarMethods.getOrder(onDates));
				}
				if(offDates.length > 1){
					offDates = CalendarMethods.orderArray(offDates, CalendarMethods.getOrder(offDates));
				}
				
				// Check if we are missing the first ON and last OFF [K-M]
				if(onDates.length == offDates.length && offDates[0].compareTo(onDates[0]) < 0) {
					
					// Add first ON as first day of the first year of interest
					onDates = CalendarMethods.appendToFront(onDates, CalendarMethods.dateForYearStart(yearsToExamine[0]));
					
					// Add last OFF as last day of the last year of interest
					offDates = CalendarMethods.append(offDates, CalendarMethods.dateForYearEnd(yearsToExamine[1]));
				}
				
				// Check if we are missing the first ON date [C-F]
				if(offDates.length > onDates.length) {
					
					// Add first ON as first day of the first year of interest
					onDates = CalendarMethods.appendToFront(onDates, CalendarMethods.dateForYearStart(yearsToExamine[0]));
				}
				
				// Check if we are missing the last OFF date [G-J]
				if(onDates.length > offDates.length) {
					
					// Add last OFF as last day of the last year of interest
					offDates = CalendarMethods.append(offDates, CalendarMethods.dateForYearEnd(yearsToExamine[1]));
				}
				
				// Examine each of the dates of interest
				for(int dateOfInterestIndex = 0; dateOfInterestIndex < datesOfInterest.length; dateOfInterestIndex++){
					
					// Search for an overlapping period with NOW paired ON and OFF dates [A,B,D,E,H,I,K]
					for(int movementDateIndex = 0; movementDateIndex < onDates.length; movementDateIndex++) {
						
						// Check if the current paired ON and OFF dates overlap the DOI
						if(onDates[movementDateIndex].compareTo(datesOfInterest[dateOfInterestIndex]) < 0 && offDates[movementDateIndex].compareTo(datesOfInterest[dateOfInterestIndex]) > 0) {
							
							// Add to herd size count and skip
							herdSizes[dateOfInterestIndex]++;
							break;
						}
					}
				}
			}
			
			bWriter.write(locationId + "\t" + locations.get(locationId).getCph() + "\t" + ArrayMethods.toString(herdSizes, "\t") + "\t" + locations.get(locationId).getHerdType() + "\t" + locations.get(locationId).getPremisesType() + "\n");
		}
		
		bWriter.close();		
	}
	
	public static void calculateHerdSizesOnDates2(String fileName, Hashtable<String, Location> locations, Calendar[] datesOfInterest) throws IOException{
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		bWriter.write("Location\tCPH\t" + CalendarMethods.toString(datesOfInterest, "-", "\t") + "\tHerdType\tPremisesType\n");
		
		// Examine each location
		for(String locationId : HashtableMethods.getKeysString(locations)){
			
			int[] herdSizes = new int[datesOfInterest.length];
			
			// Examine each animal associated with the current herd
			for(String animalId : locations.get(locationId).getInhabitantIds()){
				
				// Examine each of the dates of interest
				for(int dateOfInterestIndex = 0; dateOfInterestIndex < datesOfInterest.length; dateOfInterestIndex++){
					
					// Get the ON and OFF dates for the current animal
					Calendar[] onDates = new Calendar[0];
					if(locations.get(locationId).getAnimalsOnMovementDates().get(animalId) != null) {
						onDates = CalendarMethods.copy(locations.get(locationId).getAnimalsOnMovementDates().get(animalId));
					}
					Calendar[] offDates = new Calendar[0];
					if(locations.get(locationId).getAnimalsOffMovementDates().get(animalId) != null) {
						offDates = CalendarMethods.copy(locations.get(locationId).getAnimalsOffMovementDates().get(animalId));
					}
					
					// Skip when no ON or OFF dates available
					if(onDates.length == 0 && offDates.length == 0) {
						continue;
					}
					
					// Order the dates
					if(onDates.length > 1){
						onDates = CalendarMethods.orderArray(onDates, CalendarMethods.getOrder(onDates));
					}
					if(offDates.length > 1){
						offDates = CalendarMethods.orderArray(offDates, CalendarMethods.getOrder(offDates));
					}
					
					/**
					 * Dealing with periods of time spent on herd
					 * 
					 * Things to consider:
					 * A  --           ----|---	-----         Paired ON and OFF dates				onDates.length = offDates.length & offDates[0] > onDates[1]
					 * B                 --|--------------    "										"
					 * 
					 * C-------------------|-                 Missing first ON date 				offDates.length > onDates.length							offDates[0] after DOI
					 * D--- -----   ----  -|- ------- ------  "										"															remove(offDates[0])
					 * E------     --------|-                 "										"															remove(offDates[0])
					 * F-------------------|--- -------       "										"															offDates[0] after DOI
					 * 
					 * G                  -|------------------Missing last OFF date					onDates.length > offDates.length							onDates[-1] before DOI
					 * H  --------- -------|- ----------------"										"															remove(onDates[-1])
					 * I                ---|----- ------------"										"															remove(onDates[-1])
					 * J   ---    ----   --|------------------"										"															onDates[-1] before DOI
					 * 
					 * K----  --------   --|-        ---------Missing 1st ON date and lst OFF date	onDates.length = offDates.length & offDates[0] < onDates[1]	remove(offDates[0]) & remove(onDates[-1])
					 * L---     -----------|------------------"										"															onDates[-1] before DOI
					 * M-------------------|----         -----"										"															offDates[0] after DOI
					 * 
					 * N-------------------|------------------No dates! No way we can find this
					 * O                   |                  "
					 * 
					 * date.compareTo(argument) == 0	date == argument
					 * date.compareTo(argument) < 0		date before argument
					 * date.compareTo(argument) > 0		date after argument
					 */
					
					// Note that using custom get length function to distinguish between a vector being null and being empty
					
					// Check if ON and OFF dates exist and are of the same length and first OFF before first ON [K-M]
					if(onDates.length == offDates.length && offDates[0].compareTo(onDates[0]) < 0) {
						
						// Check if last ON before DOI [L] OR first OFF after DOI [M]
						if(onDates[onDates.length - 1].compareTo(datesOfInterest[dateOfInterestIndex]) < 0 ||
						   offDates[0].compareTo(datesOfInterest[dateOfInterestIndex]) > 0) {
							
							herdSizes[dateOfInterestIndex]++;
							continue;
							
						// Otherwise remove both last ON date and first OFF date [K] and don't skip
						}else {
							onDates = CalendarMethods.deletePosition(onDates, onDates.length -1);
							offDates = CalendarMethods.deletePosition(offDates, 0);
						}
						
					// Check if more OFF dates than ON dates [C-F]
					}else if(offDates.length > onDates.length){
						
						// Check if first OFF after DOI [C & F]
						if(offDates[0].compareTo(datesOfInterest[dateOfInterestIndex]) > 0) {
							
							// Add to herd size count and skip
							herdSizes[dateOfInterestIndex]++;
							continue;
						
						// Else remove first OFF date [D & E] and don't skip
						}else {
							offDates = CalendarMethods.deletePosition(offDates, 0);
						}
						
					// Check if more ON dates than OFF dates [G-J]
					}else if(onDates.length > offDates.length) {
						
						// Check if last ON date before DOI [G & J]
						if(onDates[onDates.length - 1].compareTo(datesOfInterest[dateOfInterestIndex]) < 0) {
							
							// Add to herd size count and skip
							herdSizes[dateOfInterestIndex]++;
							continue;
						
						// Else remove last ON date [H & I] and don't skip
						}else {
							onDates = CalendarMethods.deletePosition(onDates, onDates.length - 1);
						}
					}
					
					// Search for an overlapping period with NOW paired ON and OFF dates [A,B,D,E,H,I,K]
					for(int movementDateIndex = 0; movementDateIndex < onDates.length; movementDateIndex++) {
						
						// Check if the current paired ON and OFF dates overlap the DOI
						if(onDates[movementDateIndex].compareTo(datesOfInterest[dateOfInterestIndex]) < 0 && offDates[movementDateIndex].compareTo(datesOfInterest[dateOfInterestIndex]) > 0) {
							
							// Add to herd size count and skip
							herdSizes[dateOfInterestIndex]++;
							break;
						}
					}
				}
			}
			
			bWriter.write(locationId + "\t" + locations.get(locationId).getCph() + "\t" + ArrayMethods.toString(herdSizes, "\t") + "\t" + locations.get(locationId).getHerdType() + "\t" + locations.get(locationId).getPremisesType() + "\n");
		}
		
		bWriter.close();		
	}
	
	public static void calculateHerdSizesOnDates(String fileName, Hashtable<String, Location> locations, Calendar[] dates) throws IOException{
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		bWriter.write("Location\tCPH\t" + CalendarMethods.toString(dates, "-", "\t") + "\tHerdType\tPremisesType\n");
		
		// Examine each location
		for(String locationId : HashtableMethods.getKeysString(locations)){
			
			int[] herdSizes = new int[dates.length];
			
			// Examine each animal associated with the current herd
			for(String animalId : locations.get(locationId).getInhabitantIds()){
				
				// Get the ON and OFF dates for the current animal
				Calendar[] onDates = locations.get(locationId).getAnimalsOnMovementDates().get(animalId);
				Calendar[] offDates = locations.get(locationId).getAnimalsOffMovementDates().get(animalId);
				
				// Order the dates
				if(onDates != null && onDates.length > 1){
					onDates = CalendarMethods.orderArray(onDates, CalendarMethods.getOrder(onDates));
				}
				if(offDates != null && offDates.length > 1){
					offDates = CalendarMethods.orderArray(offDates, CalendarMethods.getOrder(offDates));
				}
				
				// Examine the ordered dates
				for(int dateIndex = 0; dateIndex < dates.length; dateIndex++){
					
					// Note the number of dates available
					int nDates = 0;
					if(onDates != null){
						nDates = onDates.length;
					}else{
						nDates = offDates.length;
					}
					
					for(int i = 0; i < nDates; i++){
						
						/**
						 * Dealing with periods of time spent on herd
						 * 
						 * Add to herd size count when:
						 * A ---------			----|---	-----         Paired ON and OFF dates					onDates.length == offDates.length && offDates[0] > onDates[1]
						 * B------------------------|-                    Missing first ON date						onDates.length == 0 & offDates[0] -> remove(offDates[0]) unless after DOI
						 * C-------- -----   ----  -|- ------- ------     Missing first ON date						onDates.length < offDates.length -> remove(offDates[0]) unless after DOI
						 * D-----------     --------|-                    Missing first ON date						onDates.length < offDates.length -> remove(offDates[0]) unless after DOI
						 * E                       -|-------------------- Missing last OFF date						offDates.length == 0 & onDates[0] -> remove(onDates[-1]) unless before DOI
						 * F       --------- -------|- ------------------ Missing last OFF date						onDates.length > offDates.length -> remove(onDates[-1]) unless before DOI
						 * G                     ---|----- -------------- Missing last OFF date						onDates.length > offDates.length -> remove(onDates[-1]) unless before DOI
						 * H---------  --------   --|-        -------- -- Missing first ON date and last OFF date	onDates.length == offDates.length && offDates[0] < onDates[1]
						 * 																								-> remove(offDates[0]) unless after DOI
						 * 																								-> remove(onDates[-1]) unless before DOI 
						 */
						
						// Check whether paired OFF date available
						if(onDates != null && offDates != null && i < offDates.length - 1 && i < onDates.length - 1 && 
						   onDates[i].compareTo(dates[dateIndex]) < 0 && offDates[i].compareTo(dates[dateIndex]) > 0){ // [A] ON and OFF dates available and overlap with DOI
							herdSizes[dateIndex]++;
							break;
						}else if(onDates != null && onDates[i].compareTo(dates[dateIndex]) < 0 && (offDates == null || i >= offDates.length)){ // [C & D] ON date available and before DOI
							herdSizes[dateIndex]++;
							break;
						}else if(offDates != null && onDates == null && i < offDates.length && offDates[i].compareTo(dates[dateIndex]) > 0){ // [OFF date available and after DOI
							herdSizes[dateIndex]++;
							break;
						}
					}
				}
			}
			
			bWriter.write(locationId + "\t" + locations.get(locationId).getCph() + "\t" + ArrayMethods.toString(herdSizes, "\t") + "\t" + locations.get(locationId).getHerdType() + "\t" + locations.get(locationId).getPremisesType() + "\n");
		}
		
		bWriter.close();		
	}
	
	public static void removeLocationsOfTypes(Hashtable<String, Location> locations, String[] premisesTypesToIgnore){
		
		// Convert the array of types to hashtable
		Hashtable<String, Integer> typesToIgnore = HashtableMethods.indexArray(premisesTypesToIgnore);
		
		// Examine each location
		for(String key : HashtableMethods.getKeysString(locations)){
			
			if(locations.get(key).getPremisesType() != null && typesToIgnore.containsKey(locations.get(key).getPremisesType())){
				locations.remove(key);
			}
		}
	}
	
	public static void recordMovementsOnHerdsPost2001(String filePrefix, int[] yearsToExamine, Hashtable<String, Location> locations) throws IOException{
		/**
		 * Movements File Structure:
		 * animal_id	movement_number	movement_date	off_location_id	is_trans	trans_location_id
		 * 0			1				2				3				4			5
		 * 
		 * on_location_id	is_birth	is_death	stay_length	is_valid_history
		 * 6				7			8			9			10
		 */
		
		// Initialise variables to parse the movement data
		int[] dateFormat = {2, 1, 0};
		
		// Initialise variables necessary for parsing the movements file
		String line = null;
		int lineNo = 0;
		String[] cols;
		
		// The movements in any given year are recorded in separate files
		for(int year : yearsToExamine){
			
			// Open the movements file
			InputStream input = new FileInputStream(filePrefix + year + ".csv");
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
							
			// Note the progress
			System.out.println("Beginning to parse movements in year:\t" + year);
						
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
				
				// Is the OFF location one we're interested in?
				if(locations.containsKey(cols[3]) == true){
					
					// Parse the movement data
					Calendar date = CalendarMethods.parseDate(cols[2], "-", dateFormat, true);
						
					// Add the animal and its off movement date
					locations.get(cols[3]).addInhabitant(cols[0], date, false);
				}
				
				// Is the ON location one we're interested in?
				if(locations.containsKey(cols[6]) == true){
					
					// Parse the movement data
					Calendar date = CalendarMethods.parseDate(cols[2], "-", dateFormat, true);
					
					// Add the animal and its on movement date
					locations.get(cols[6]).addInhabitant(cols[0], date, true);
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
		}
	}
	
 	public static Hashtable<String, Location> readLocationsTablePost2001SelectHerdsCloseToWP(String fileName, double[] badgerCentre, double threshold) throws IOException{
		
		// Open the movements file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
		double[] coordinates = new double[2];
		
		// Initialise variable to store location information
		Hashtable<String, Location> locations = new Hashtable<String, Location>();
					
		// Begin reading the file
		while(( line = reader.readLine()) != null){
						
			// Skip the header
			if(line.matches("location_id(.*)") == true){
				continue;
			}
			
			// Split the current line into its columns
			cols = line.split(",", -1);
			
			// Check coordinate information exists
			if(cols[4].matches("") == false && cols[5].matches("") == false){
				
				// Parse the coordinates
				coordinates[0] = Double.parseDouble(cols[4]);
				coordinates[1] = Double.parseDouble(cols[5]);
				
				// Check if current location within threshold distance
				if(GeneralMethods.calculateEuclideanDistance(badgerCentre, coordinates) <= threshold){
					
					// Create the location record
					locations.put(cols[0], new Location(cols[0]));
					
					// Store the coordinate information
					locations.get(cols[0]).setX(cols[4]);
					locations.get(cols[0]).setY(cols[5]);
					
					// Store the CPH
					if(cols[1].matches("") == false){
						locations.get(cols[0]).setCph(cols[1]);
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
			}
		}
		input.close();
		reader.close();
		
		return locations;
	}
}
