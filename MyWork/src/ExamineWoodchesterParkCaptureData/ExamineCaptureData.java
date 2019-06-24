package ExamineWoodchesterParkCaptureData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.HashtableMethods;
import methods.WriteToFile;

public class ExamineCaptureData {

	public static void main(String[] args) throws IOException{
		// Get the date
		String date = CalendarMethods.getCurrentDate("dd-MM-yyyy");
		
		// Set the path
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/NewAnalyses_13-07-17/BadgerCaptureData/";
		
		// Open the capture data file
		String captureDataFile = path + "WP_CaptureData.csv";
		CaptureEvent[] captureData = readBadgerCaptureData(captureDataFile);
		
		// Assign the capture events to individual badgers
		Hashtable<String, CaptureEvent[]> badgerCaptureHistories = assignCaptureEventsToBadgers(captureData);
		
		// Order each badgers capture events by their date
		sortCaptureEventInfoForEachBadgerByDate(badgerCaptureHistories);
		
		// Informatively remove NA social groups (If caught in same group before and after - replace)
		informativelyReplaceNASocialGroups(badgerCaptureHistories);
		
		/**
		 *  Count the number of negative, exposed, excretor and super-excretor animals present in each social
		 *  group across a defined date range.
		 */
		
//		String[] dateRangeStrings = {"11-01-2000", "25-05-2011"}; // Day before and day after first and last sampling events
//		int[] dayMonthYear = {0, 1, 2};
//		Calendar[] dateRange = CalendarMethods.parseDates(dateRangeStrings, "-", dayMonthYear, true);
//		int nDays = (int) CalendarMethods.calculateNDaysBetweenDates(dateRange[0], dateRange[1]) - 1;
//		Hashtable<String, int[][]> socialGroupCounts = countIndividualsInEachInfectionCategoryInEachGroupDuringDateRange(
//				badgerCaptureHistories, dateRange, nDays);
//		
//		// Print the counts out to file
//		String[] socialGroups = HashtableMethods.getKeysString(socialGroupCounts); 
//		String[][] outputTable = createOutputTable(socialGroupCounts, nDays, socialGroups);
//		String outputFile = path + "InfectionCategoryCounts_" + dateRangeStrings[0] + "_" + dateRangeStrings[1] +
//				"_" + date + ".csv";
//		printOutputTableToFile(outputFile, outputTable, socialGroups, dateRange);
		
		int[] yearRange = {2000, 2011};
		Hashtable<String, int[][]> socialGroupCounts = countIndividualsInEachInfectionCategoryInEachGroupDuringYearRange(
				badgerCaptureHistories, yearRange);
		String[] socialGroups = HashtableMethods.getKeysString(socialGroupCounts);
		String[][] outputTable = createOutputTable(socialGroupCounts, (yearRange[1] - yearRange[0]) + 1, socialGroups);
		String outputFile = path + "InfectionCategoryCounts_" + yearRange[0] + "-" + yearRange[1] + "_" + date + ".csv";
		printOutputTableToFile(outputFile, outputTable, socialGroups, yearRange);
		
	}
	
	public static void printOutputTableToFile(String fileName, String[][] outputTable, String[] socialGroups,
			int[] yearRange) throws IOException{
		
		// Get the start date
		int startYear = yearRange[0] - 1;
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Print the header
		WriteToFile.writeLn(bWriter, "Date\t" + ArrayMethods.toString(socialGroups, "\t")); 
		
		// Print the output table to file
		String output = "";
		for(int row = 0; row < outputTable.length; row++){
			startYear++;
			output += startYear + "\t" + ArrayMethods.toString(outputTable[row], "\t") + "\n";
		}
		WriteToFile.write(bWriter, output);
		
		
		// Close the output file
		WriteToFile.close(bWriter);
	}
	
	public static Hashtable<Integer, Hashtable<String, Integer>> createHashtableToNoteBadgersCountedInEachYear(int[] yearRange){
		
		Hashtable<Integer, Hashtable<String, Integer>> badgersAlreadyCounted = 
				new Hashtable<Integer, Hashtable<String, Integer>>();
		
		for(int year = yearRange[0]; year <= yearRange[1]; year++){
			badgersAlreadyCounted.put(year, new Hashtable<String, Integer>());
		}
		
		return badgersAlreadyCounted;
	}
	
	public static void addToCountForCurrentCapture(int year, Hashtable<String, int[][]> socialGroupCounts, 
			int[] yearRange, String socialGroup, int statusIndex){
		
		// Check that date is within range
		if(year >= yearRange[0] && year <= yearRange[1]){
			
			// Update the counts for the current group
			if(socialGroupCounts.get(socialGroup) != null){
				socialGroupCounts.get(socialGroup)[statusIndex][year - yearRange[0]]++;
			}else{

				int[][] counts = new int[4][(yearRange[1] - yearRange[0]) + 1];
				counts[statusIndex][year - yearRange[0]]++;
				
				socialGroupCounts.put(socialGroup, counts);
			}			
		}
	}
	
	public static Hashtable<String, int[][]> countIndividualsInEachInfectionCategoryInEachGroupDuringYearRange(
			Hashtable<String, CaptureEvent[]> badgerCaptureHistories, int[] yearRange){
		
		// Badger only counted based upon first capture in year
		
		Hashtable<String, int[][]> socialGroupCounts = new Hashtable<String, int[][]>();
		
		// Create hashtable for each year (in range) to record whether badger was already counted
		Hashtable<Integer, Hashtable<String, Integer>> badgersAlreadyCounted = 
				createHashtableToNoteBadgersCountedInEachYear(yearRange);
		
		// Examine the capture history of each badger
		CaptureEvent[] captureInfo;
		for(String tattoo : HashtableMethods.getKeysString(badgerCaptureHistories)){
			
			captureInfo = badgerCaptureHistories.get(tattoo);
			
			for(int i = 0; i < captureInfo.length; i++){
				
				// Skip capture event if no status available
				if(captureInfo[i].getStatusIndex() == -1){
					continue;
				}
				
				// Skip capture dates that aren't in the range of interest
				if(captureInfo[i].getDate().get(Calendar.YEAR) < yearRange[0] ||
						captureInfo[i].getDate().get(Calendar.YEAR) > yearRange[1]){
					continue;
				}
				
				// Note the status at the current capture
				if(badgersAlreadyCounted.get(captureInfo[i].getDate().get(Calendar.YEAR)).get(captureInfo[i].getTattoo()) == null){
					addToCountForCurrentCapture(captureInfo[i].getDate().get(Calendar.YEAR), socialGroupCounts, yearRange,
							captureInfo[i].getSocialGroup(), captureInfo[i].getStatusIndex());
					badgersAlreadyCounted.get(captureInfo[i].getDate().get(Calendar.YEAR)).put(captureInfo[i].getTattoo(), 1);
				}
				
				
				// Add to counts for previous period if stayed in the same group
				if(i > 0 && 
						captureInfo[i].getSocialGroup().matches("NA") == false &&
						captureInfo[i-1].getSocialGroup().matches("NA") == false &&
						captureInfo[i].getSocialGroup().matches(captureInfo[i-1].getSocialGroup()) == true &&
						captureInfo[i-1].getDate().get(Calendar.YEAR) != captureInfo[i].getDate().get(Calendar.YEAR)){
					
					for(int year = captureInfo[i-1].getDate().get(Calendar.YEAR) + 1; 
							year < captureInfo[i].getDate().get(Calendar.YEAR); year++){
						
						// Skip years that aren't in range
						if(year < yearRange[0] || year > yearRange[1]){
							continue;
						}
						
						if(badgersAlreadyCounted.get(year).get(captureInfo[i].getTattoo()) == null){
							addToCountForCurrentCapture(year, socialGroupCounts, yearRange, captureInfo[i].getSocialGroup(),
									captureInfo[i-1].getStatusIndex());
							badgersAlreadyCounted.get(captureInfo[i].getDate().get(Calendar.YEAR)).put(captureInfo[i].getTattoo(), 1);
							badgersAlreadyCounted.get(year).put(captureInfo[i].getTattoo(), 1);
						}						
					}
				}
			}
		}
		
		return socialGroupCounts;
	}
	
	public static void printOutputTableToFile(String fileName, String[][] outputTable, String[] socialGroups,
			Calendar[] dateRange) throws IOException{
		
		// Get the start date
		Calendar startDate = CalendarMethods.copy(dateRange[0]);
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Print the header
		WriteToFile.writeLn(bWriter, "Date\t" + ArrayMethods.toString(socialGroups, "\t")); 
		
		// Print the output table to file
		String output = "";
		for(int row = 0; row < outputTable.length; row++){
			startDate.add(Calendar.DATE, 1);
			output += CalendarMethods.toString(startDate, "-") + "\t" +
					ArrayMethods.toString(outputTable[row], "\t") + "\n";
		}
		WriteToFile.write(bWriter, output);
		
		
		// Close the output file
		WriteToFile.close(bWriter);
	}
	
	public static String[][] createOutputTable(Hashtable<String, int[][]> socialGroupCounts, int n, 
			String[] socialGroups){
		
		String[][] outputTable = new String[n][socialGroups.length];
		int[][] counts;
		
		for(int i = 0; i < socialGroups.length; i++){
			counts = socialGroupCounts.get(socialGroups[i]);
			for(int row = 0; row < counts[0].length; row++){
				outputTable[row][i] = counts[0][row] + ":" + counts[1][row] + ":" + counts[2][row] + ":" + counts[3][row];
			}
		}
		
		return outputTable;
	}
	
 	public static void informativelyReplaceNASocialGroups(Hashtable<String, CaptureEvent[]> badgerCaptureHistories){
		
		CaptureEvent[] captureInfo;
		String[] groupsBeforeAndAfter;
		
		int count = 0;
		for(String tattoo : HashtableMethods.getKeysString(badgerCaptureHistories)){
			
			captureInfo = badgerCaptureHistories.get(tattoo);
			
			for(int i = 0; i < captureInfo.length; i++){
				
				if(captureInfo[i].getSocialGroup().matches("NA")){
					
					// Get the groups the animal was present before and after the current capture
					groupsBeforeAndAfter = getSocialGroupBeforeAndAfter(captureInfo, i);
					
					// If they are the same and not both NA - replace the current capture events social group
					if(groupsBeforeAndAfter[0].matches("NA") == false && groupsBeforeAndAfter[1].matches("NA") == false &&
							groupsBeforeAndAfter[0].matches(groupsBeforeAndAfter[1])){
						count++;
						captureInfo[i].setSocialGroup(groupsBeforeAndAfter[0]);
						
					}					
				}
			}
		}
		
		System.out.println("Replaced " + count + " social groups that were NAs");
	}
	
	public static String[] getSocialGroupBeforeAndAfter(CaptureEvent[] captureInfo, int index){
		
		String[] groups = {"NA", "NA"};
		
		for(int i = 0; i < captureInfo.length; i++){
			
			// Checking dates before
			if(i < index && captureInfo[i].getSocialGroup().matches("NA") == false){
				groups[0] = captureInfo[i].getSocialGroup();
			}else if(i > index && captureInfo[i].getSocialGroup().matches("NA") == false){
				groups[1] = captureInfo[i].getSocialGroup();
				break;
			}			
		}
		
		return groups;
	}
	
	public static void addCountForDate(Calendar date, Hashtable<String, int[][]> socialGroupCounts,
			Calendar[] dateRange, int statusIndex, String group, int nDays){
		
		// Check that date is within range
		if(CalendarMethods.checkIfDateIsWithinRange(date, dateRange) == true){
			
			// Update the counts for the current group
			if(socialGroupCounts.get(group) != null){
				socialGroupCounts.get(group)[statusIndex][(int) CalendarMethods.calculateNDaysBetweenDates(date, dateRange[0]) - 1]++;
			}else{

				int[][] counts = new int[4][(int) nDays];
				counts[statusIndex][(int) CalendarMethods.calculateNDaysBetweenDates(date, dateRange[0]) - 1]++;
				
				socialGroupCounts.put(group, counts);
			}			
		}
	}
	
	public static Hashtable<String, int[][]> countIndividualsInEachInfectionCategoryInEachGroupDuringDateRange(
			Hashtable<String, CaptureEvent[]> badgerCaptureHistories, Calendar[] dateRange, int nDays){
		
		Hashtable<String, int[][]> socialGroupCounts = new Hashtable<String, int[][]>();
		
		CaptureEvent[] captureInfo;
		for(String tattoo : HashtableMethods.getKeysString(badgerCaptureHistories)){
			
			captureInfo = badgerCaptureHistories.get(tattoo);
			
			for(int i = 0; i < captureInfo.length; i++){
				
				// Skip capture event if no status available
				if(captureInfo[i].getStatusIndex() == -1){
					continue;
				}
				
				// Note the status at the current capture
				addCountForDate(captureInfo[i].getDate(), socialGroupCounts, dateRange, captureInfo[i].getStatusIndex(),
						captureInfo[i].getSocialGroup(), nDays);
				
				// Add to counts for previous period if stayed in the same group
				if(i > 0 && 
						captureInfo[i].getSocialGroup().matches("NA") == false &&
						captureInfo[i-1].getSocialGroup().matches("NA") == false &&
						captureInfo[i].getSocialGroup().matches(captureInfo[i-1].getSocialGroup()) == true){
					
					updateCounts(captureInfo[i-1].getDate(), captureInfo[i].getDate(), socialGroupCounts,
							captureInfo[i-1].getSocialGroup(), captureInfo[i-1].getStatusIndex(), dateRange, nDays);
				}
			}
		}
		
		return socialGroupCounts;
	}
	
	public static void updateCounts(Calendar start, Calendar end, Hashtable<String, int[][]> socialGroupCounts,
			String group, int statusIndex, Calendar[] dateRange, int nDays){
		
		
				
		// Update the counts for the current group
		if(socialGroupCounts.get(group) != null){
			addToCountsForPeriod(start, end, socialGroupCounts.get(group), dateRange, statusIndex);
		}else{

			int[][] counts = new int[4][(int) nDays];
			addToCountsForPeriod(start, end, counts, dateRange, statusIndex);
			
			socialGroupCounts.put(group, counts);
		}
	}
	
	public static void addToCountsForPeriod(Calendar start, Calendar end, int[][] counts, Calendar[] dateRange, 
			int index){
		
		// NOTE: Doesn't include start or end
		
		// Find the start and end indices of the period
		int startIndex = 0;
		int endIndex = counts[index].length - 1;

		if(CalendarMethods.before(dateRange[0], start) == true){
			startIndex = (int) CalendarMethods.calculateNDaysBetweenDates(dateRange[0], start);
		}
		
		if(CalendarMethods.before(end, dateRange[1])){
			endIndex = counts[index].length - (int) CalendarMethods.calculateNDaysBetweenDates(end, dateRange[1]) - 1;
		}
		
		// Add to counts for days in period
		for(int i = startIndex; i <= endIndex; i++){
			counts[index][i]++;
		}
		
	}
	
	public static void sortCaptureEventInfoForEachBadgerByDate(Hashtable<String, CaptureEvent[]> badgerCaptureHistories){
		
		for(String tattoo : HashtableMethods.getKeysString(badgerCaptureHistories)){
			badgerCaptureHistories.put(tattoo, CaptureEvent.sort(badgerCaptureHistories.get(tattoo)));
		}		
	}
	
	public static Hashtable<String, CaptureEvent[]> assignCaptureEventsToBadgers(CaptureEvent[] captureData){
		Hashtable<String, CaptureEvent[]> badgerCaptureHistories = new Hashtable<String, CaptureEvent[]>();
		CaptureEvent[] captureEvents;
		
		for(int i = 0; i < captureData.length; i++){
			
			if(badgerCaptureHistories.get(captureData[i].getTattoo()) != null){
				captureEvents = CaptureEvent.append(badgerCaptureHistories.get(captureData[i].getTattoo()), captureData[i]);
				badgerCaptureHistories.put(captureData[i].getTattoo(), captureEvents);
			}else{
				captureEvents = new CaptureEvent[1];
				captureEvents[0] = captureData[i];
				badgerCaptureHistories.put(captureData[i].getTattoo(), captureEvents);
			}
		}
		
		return badgerCaptureHistories;
	}
	
	public static CaptureEvent[] readBadgerCaptureData(String fileName) throws IOException{
		
		// Open the input file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
	
		// Initialise an array to store the capture data
		CaptureEvent[] captureData = new CaptureEvent[99999];
		int pos = -1;
		
		// Initialise variables necessary for parsing the file
		String line = null;
							
		// Begin reading the file
		while(( line = reader.readLine()) != null){
		
			// Skip the header line
			if(line.matches("tattoo(.*)") == true){
				continue;
			}
			
			// Store the capture data from the current line
			pos++;
			captureData[pos] = new CaptureEvent(line);
			
		}
				
		// Close the current movements file
		input.close();
		reader.close();
		
		return(CaptureEvent.subset(captureData, 0, pos));
	}
}
