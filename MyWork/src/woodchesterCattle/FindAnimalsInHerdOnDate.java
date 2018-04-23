package woodchesterCattle;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.HashtableMethods;

import phylogeneticTree.FindClades;

public class FindAnimalsInHerdOnDate {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		// Note the Movements file prefix and path
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/CattleMovementData/";
		String movementsFilePrefix = "20160123_joe_cts_movements";
		
		// State the years (for the movements file) we're interested in
		int[] yearsToExamine = ArrayMethods.seq(2005, 2014, 1);
		
		// Note the date of interest
		int[] inputDateFormat = {0, 1, 2}; // Indices of Day, Month, Year
		Calendar date = CalendarMethods.parseDate("23/02/1999", "/", inputDateFormat, true);
		
		// Note the CPH
		String CPHH = "14082000501"; // 14/082/0005-01
		String locationsFile = "20160124_joe_cts_locations.csv";
		
		// Get a list of CPHHs and sampling dates
		
		// Open the Cattle Sampling Information
		path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/NewAnalyses_02-06-16/IsolateData/";
		String isolateInfo = "CattleIsolateInfo_LatLongs_plusID_outbreakSize_Coverage_AddedTB1453-TB1456.csv";
				
		// Find the animals present at a CPH on a given date
		int[] movementsDateFormat = {2, 1, 0};
		path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/NewAnalyses_02-06-16/CattleMovementData/";
		String[] animals = findAnimalsPresentOnHerd(path, movementsFilePrefix, CPHH, yearsToExamine, date, movementsDateFormat, locationsFile);
		
		System.out.println("\nNumber of animals found = " + animals.length);
		
		// Get the eartag numbers of the animals found
		String animalsTable = "20160124_joe_cts_animals.csv";
		Hashtable<String, String> animalIds = getEartagsOfAnimalsFound(path + animalsTable, animals);
		
		// Print out the eartags of the animals found
		printAnimalIds(animalIds);
	}

	public static String getLocationIdForCPHH(String locationsTable, String CPHH) throws IOException{
		
		// Change the format of the CPHH
		CPHH = changeFormatOfCPHH(CPHH);
		String locationId = "NA";
		
		// NOTE THAT CPHH not recored in locations file all under CPH-00
		CPHH = CPHH.split("-")[0] + "-00";
		
		// Open the animals table file
		InputStream input = new FileInputStream(locationsTable);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
									
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
			
			// Is the current line for this CPHH?
			if(line.matches("(.*)" + CPHH + "(.*)")){
				
				cols = line.split(",");
				locationId = cols[0];
				break;
			}
		}
					
		// Close the animals table file
		input.close();
		reader.close();
		
		return locationId;
	}
	
	public static String changeFormatOfCPHH(String CPHH){
		
		// Need to convert from this: 01001000300 to this: 01/001/0003-00
		
		// Split the string into its characters
		char[] chars = CPHH.toCharArray();
		
		return chars[0] + "" + chars[1] + "/" + chars[2] + chars[3] + chars[4] + "/" + chars[5] + chars[6] + chars[7] + chars[8] + "-" + chars[9] + chars[10];
	}
	
	public static void printAnimalIds(Hashtable<String, String> animalIds){
		
		// Get the keys
		String[] keys = HashtableMethods.getKeysString(animalIds);
		
		// Print the Ids
		for(String key : keys){
			System.out.println(key + "\t" + animalIds.get(key));
		}
	}
	
	public static Hashtable<String, String> getEartagsOfAnimalsFound(String animalsTable, String[] animals) throws IOException{
		
		// Put the animals found into a hashtable
		Hashtable<String, String> animalIds = createAnimalIDHashtable(animals);
		
		// Open the animals table file
		InputStream input = new FileInputStream(animalsTable);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
									
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
		int lineNo = 0;
								
		// Begin reading the file
		System.out.println("Beginning to parse animals table...");
		while(( line = reader.readLine()) != null){
			lineNo++;
			
			// Skip the header line
			if(lineNo == 1){
				continue;
			}
			
			// Split the line into its columns
			cols = line.split(",");
			
			// Check if we are interested in the current animal
			if(animalIds.get(cols[0]) != null){
				
				// Store the animals eartag
				animalIds.put(cols[0], cols[1]);
			}
			
			// Note the progress
			if(lineNo % 1000000 == 0){
				System.out.print(".");
			}
		}
			
		// Close the animals table file
		input.close();
		reader.close();
		
		System.out.println("\nFinished Examining animal table.");
		
		return animalIds;
	}
	
	public static Hashtable<String, String> createAnimalIDHashtable(String[] animals){
		
		Hashtable<String, String> animalIds = new Hashtable<String, String>();
		
		for(String animalId : animals){
			animalIds.put(animalId, "");
		}
		
		return animalIds;
	}
	
	public static String[] findAnimalsPresentOnHerd(String path, String movementsFilePrefix, String CPHH,
			int[] yearsToExamine, Calendar date, int[] dateFormat, String locationsTable) throws IOException{
		
		// Get the locationId for the CPH
		String locationId = getLocationIdForCPHH(path + locationsTable, CPHH);
		
		// Initialise a hashtable to record which animals are present at the CPH 
		Hashtable<String, Integer> animalsPresent = new Hashtable();
		Hashtable<String, Integer> animalsToIgnore = new Hashtable();
		
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
			InputStream input = new FileInputStream(path + movementsFilePrefix + "_" + year + ".csv");
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
							
			// Initialise variables necessary for parsing the file
			String line = null;
			int lineNo = 0;
						
			// Note the progress
			System.out.println("Beginning to parse movements in year:\t" + year);
			
			// Begin reading the file
			while(( line = reader.readLine()) != null){
				lineNo++;
				
				// Skip the header and last line
				if(line.matches("animal_id(.*)") == true || line.matches("(.*)rows(.*)") == true){
					continue;
				}
				
				// Note whether the movement links the to the CPH on the given date
				determineIfMovementPlacesAnimalAtCPHOnGivenDate(line, locationId, date, animalsPresent, dateFormat, animalsToIgnore);
				
				// Note the progress
				if(lineNo % 1000000 == 0){
					System.out.print(".");
				}
			}
			System.out.println("");
			
			// Close the current movements file
			input.close();
			reader.close();
			
			System.out.println("Finished parsing movements from year:\t" + year);
		}
		
		return HashtableMethods.getKeysString(animalsPresent);
	}

	public static void determineIfMovementPlacesAnimalAtCPHOnGivenDate(String line, String CPH, Calendar date,
			Hashtable<String, Integer> animalsPresent, int[] dateFormat,
			Hashtable<String, Integer> animalsToIgnore){

		// NOTE will be most efficient and CORRECT if movements are ordered by date
		
		// Initialise a variable to store the date of the current movement
		Calendar movementDate;
		
		// Split the line into its columns
		String[] cols = line.split(",");
				
		// Skip movements not involving the CPH?
		// Skip entries where transitory movements are individually recorded (is.trans = f)
		if(line.matches("(.*)," + CPH + ",(.*)") == true && cols[4].equals("t") == true){
			
			// Get the date for the current movement
			movementDate = CalendarMethods.parseDate(cols[2], "-", dateFormat, true);
			
			// Is this a ON movement before or on the date?
			if(cols[6].matches(CPH) == true && movementDate.compareTo(date) <= 0){
				
				// Add the current animal
				animalsPresent.put(cols[0], 1);
			
			// Is this a ON movement after the date?
			}else if(cols[6].matches(CPH) == true && movementDate.compareTo(date) > 0){
				
				// Note not to include animal
				animalsToIgnore.put(cols[0], 1);
			
			// Is this an OFF movement after or on the date?
			}else if(cols[3].matches(CPH) == true && movementDate.compareTo(date) >= 0 && animalsToIgnore.get(cols[0]) == null){
				
				// Add the current animal if not already added
				if(animalsPresent.get(cols[0]) == null){
					animalsPresent.put(cols[0], 1);
				}
				
			// Is this an OFF movement before the date?
			}else if(cols[3].matches(CPH) == true && movementDate.compareTo(date) < 0){
				
				// Remove the animal if it had been previously added
				if(animalsPresent.get(cols[0]) != null){
					animalsPresent.remove(cols[0]);
				}				
			}			
		}		
	}	
}
