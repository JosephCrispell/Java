package woodchesterCattle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/NewAnalyses_22-03-18/CattleMovementData-Post2001/";

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
		int[] yearsToExamine = ArrayMethods.seq(2002, 2013, 1);
		String movementFilePrefix = path + "20160123_joe_cts_movements_";
		recordMovementsOnHerdsPost2001(movementFilePrefix, yearsToExamine, locations);
		
		// Set the date of interest
		int[] dayMonthYear = {0, 1, 2};
		String[] datesAsStrings = {"15/06/2003", "15/06/2004", "15/06/2005", "15/06/2006", "15/06/2007", "15/06/2008", "15/06/2009", "15/06/2010", "15/06/2011", "15/06/2012", "15/06/2013"};
		Calendar[] datesOfInterest = CalendarMethods.parseDates(datesAsStrings, "/", dayMonthYear, true);
		
		// Calculate the herd sizes on each of the dates of interest
		String outputFile = path + "herdSizeDistribution_" + date + ".txt";
		calculateHerdSizesOnDates(outputFile, locations, datesOfInterest);
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
						 * 							|
						 * A ---------			----|---			----- ON and OFF dates available and overlap with DOI
						 * B                        |             ------- ON date available but no overlap
						 * C------------------------|-                    No ON date but OFF date after DOI
						 * D                       -|-------------------- No OFF date but ON date before DOI
						 * E------ -------   ----  -|-------------------- More ON dates than OFF dates and last ON date before DOI
						 */
						
						// Check whether paired OFF date available
						if(onDates != null && offDates != null && i < offDates.length - 1 && i < onDates.length - 1 && 
						   onDates[i].compareTo(dates[dateIndex]) < 0 && offDates[i].compareTo(dates[dateIndex]) > 0){ // ON and OFF dates available and overlap with DOI
							herdSizes[dateIndex]++;
							break;
						}else if(onDates != null && onDates[i].compareTo(dates[dateIndex]) < 0 && (offDates == null || i >= offDates.length)){ // ON date available and before DOI
							herdSizes[dateIndex]++;
							break;
						}else if(offDates != null && onDates == null && i < offDates.length && offDates[i].compareTo(dates[dateIndex]) > 0){ // OFF date available and after DOI OR More ON dates than OFF dates and last ON date before DOIs
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
						
					// Add the animal and its on movement date
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
			
			System.out.println("Finished parsing movements from year:\t" + year);
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
					
					// Store the CPH
					if(cols[1].matches("") == false){
						locations.get(cols[0]).setCph(cols[1]);
					}
					
					// Store the coordinate information
					locations.get(cols[0]).setX(cols[4]);
					locations.get(cols[0]).setY(cols[5]);			
					
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
