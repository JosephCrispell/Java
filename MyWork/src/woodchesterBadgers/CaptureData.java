package woodchesterBadgers;

import java.util.Calendar;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.HashtableMethods;
import methods.MatrixMethods;

public class CaptureData {

	// Directly from data
	public String tattoo;
	public char sex; // M or F - ""/UNKNOWN are stored as U
	public char ageAtFirstCapture; // CUB	YEARLING	ADULT	-> C	Y	A			Also "" is stored as U
	public Calendar[] captureDates;
	public String[] groupsInhabited;
	public char[] gammaIFNResults; // 	Negative	Positive	->	Z	N	P
	public char[] statpakResults; //	Negative	Positive	->	Z	N	P
	public char[] elisaResults; //		Negative	Positive	->	Z	N	P
	public char[] cultureResults; //	NEGATIVE	Positive	->	Z	N	P
	public String[] vntrTypes; //		NA	Type
	public String[] spoligotypes; //	NA	Type
	public char[] postMortem; //		YES NO	->	Z	N	Y
	public char[] overallStatuses; //	NA	Negative	Exposed		Excretor	Superexcretor	->	Z	N	E	X	S
	
	// Additional Information taken from data
	public long[] datesInMilliSeconds;
	public double[] timeBetweenCaptureEvents; // In days
	public String mainGroup; // Group Badger spent the most time in
	public String groupWhenFirstInfected;
	public int whenInfectionDetected = -1;
	public Hashtable<String, long[][]> periodsSpentInEachGroup;
	
	public long start;
	public long end;

	public int[] dayMonthYear = {0, 1, 2};
	
	public int indexInRelatednessMatrix = -1;
	
	public CaptureData(String line){
		
		/**
		 * Each Line in the Consolidated Badger Capture Data represents the capture history for a single badger in the following format:
		 * 		Tattoo	Sex	Age_FC	@CaptureDates	@Groups	@GammaIFN	@StatPak	@ELISA	@Culture	@VNTRs	@Spoligotypes	@PostMortem	@Statuses
		 * 		0		1	2		3				4		5			6			7		8			9		10				11			12
		 * 
		 * 	@name means values are stored in a ";" separated string
		 */
		
		String[] parts = line.split("\t");
		
		// Parse the Data to get Information
		this.tattoo = parts[0];
		this.sex = parseSex(parts[1]);
		this.ageAtFirstCapture = parseAgeCategory(parts[2]);
		this.captureDates = CalendarMethods.parseDates(parts[3].split(";"), "-", dayMonthYear, true);
		this.groupsInhabited = parts[4].split(";", -1);
		this.gammaIFNResults = parseTestResults(parts[5].split(";", -1));
		this.statpakResults = parseTestResults(parts[6].split(";", -1));
		this.elisaResults = parseTestResults(parts[7].split(";", -1));
		this.cultureResults = parseTestResults(parts[8].split(";", -1));
		this.vntrTypes = parseVntrTypes(parts[9].split(";", -1));
		this.spoligotypes = parseSpoligotypes(parts[10].split(";", -1));
		this.postMortem = parsePmStatuses(parts[11].split(";"));
		this.overallStatuses = parseOverallStatuses(parts[12].split(";", -1));
		
		// Order the capture information by date
		orderCaptureDataByDate();		
		
		// Calculate some Additional Information
		this.datesInMilliSeconds = convertDates2MilliSeconds(this.captureDates);
		this.timeBetweenCaptureEvents = calculateTimeBetweenCaptureEvents(this.datesInMilliSeconds);
		this.mainGroup = findMainGroup(this.timeBetweenCaptureEvents, this.groupsInhabited);
		this.groupWhenFirstInfected = findGroupWhenInfectionFirstDetected(this.overallStatuses, this.groupsInhabited); // Also sets this.whenInfectionDetected
		this.periodsSpentInEachGroup = findWhenBadgerWasPresentInGroups(this.datesInMilliSeconds, this.groupsInhabited);
		
	}

	// Setting Methods
	public void setIndexInRelatednessMatrix(int index){
		this.indexInRelatednessMatrix = index;
	}
	
	// Getting Methods
	public int getIndexInRelatednessMatrix(){
		return this.indexInRelatednessMatrix;
	}
	public String getTattoo(){
		return this.tattoo;
	}
	public char getSex(){
		return this.sex;
	}
	public char getAgeAtFirstCapture(){
		return this.ageAtFirstCapture;
	}
	public Calendar[] getCaptureDates(){
		return this.captureDates;
	}
	public String[] getGroupsInhabited(){
		return this.groupsInhabited;
	}
	public char[] getGammaIfnResults(){
		return this.gammaIFNResults;
	}
	public char[] getStatpakResults(){
		return this.statpakResults;
	}
	public char[] getElisaResults(){
		return this.elisaResults;
	}
	public char[] getCultureResults(){
		return this.cultureResults;
	}
	public String[] getVntrTypes(){
		return this.vntrTypes;
	}
	public String[] getSpoligotypes(){
		return this.spoligotypes;
	}
	public char[] getPostMortem(){
		return this.postMortem;
	}
	public char[] getOverallStatues(){
		return this.overallStatuses;
	}
	public long[] getDatesInMilliSeconds(){
		return this.datesInMilliSeconds;
	}
	public double[] getTimeBetweenCaptureEvents(){
		return this.timeBetweenCaptureEvents;
	}
	public String getMainGroup(){
		return this.mainGroup;
	}
	public String getGroupWhenFirstInfected(){
		return this.groupWhenFirstInfected;
	}
	public int getWhenInfectionDetected(){
		return this.whenInfectionDetected;
	}
	public Hashtable<String, long[][]> getPeriodsInEachGroup(){
		return this.periodsSpentInEachGroup;
	}
	public long getStart(){
		return this.start;
	}
	public long getEnd(){
		return this.end;
	}
	public int getNMovements(){
		return this.captureDates.length;
	}
	
	// GENERAL METHODS
	public long[] convertDates2MilliSeconds(Calendar[] dates){

		// Initialise array to store dates as milliseconds
		long[] array = new long[dates.length];
		
		// Examine each date and get it in milliseconds
		for(int i = 0; i < dates.length; i++){
			array[i] = dates[i].getTimeInMillis();
		}
		
		// Set the start and end of this badgers lifespan
		this.start = array[0];
		this.end = array[array.length - 1];
		
		return array;
	}
	
	public double[] calculateTimeBetweenCaptureEvents(long[] datesInMilliSeconds){
		
		// Initialise an array to store times between capture events
		double[] array = new double[datesInMilliSeconds.length];
		
		// Look at each each date in MilliSeconds - note start at second position
		for(int i = 1; i < datesInMilliSeconds.length; i++){
			
			// Store the result in the previous index - current element states when that period ended
			array[i - 1] = (datesInMilliSeconds[i] - datesInMilliSeconds[i-1]) / (24 * 60 * 60 * 1000);
		}
		
		return array;
	}
	
	public String findMainGroup(double[] timeBetweenCaptureEvents, String[] groups){
		
		// Initialise hashtable to the periods spent in each of the groups a badger inhabited
		Hashtable<String, Double> timeSpentInGroups = new Hashtable<String, Double>();
		
		// Examine each group the current badger inhabited
		for(int i = 0; i < groups.length; i++){
			
			// Has the Badger spent time in this group previously?
			if(timeSpentInGroups.get(groups[i]) == null){
				
				// If not then add then time spent between capture events in under new group name
				timeSpentInGroups.put(groups[i], timeBetweenCaptureEvents[i]);
			}else{
				
				// If so then add time spent between capture events onto the time for the group 
				timeSpentInGroups.put(groups[i], timeSpentInGroups.get(groups[i]) + timeBetweenCaptureEvents[i]);
			}
		}

		// Find the group the badger spent the most time in
		String[] keys = HashtableMethods.getKeysString(timeSpentInGroups);
		String group = keys[0]; // Start with first position and examine the rest
		for(int i = 1; i < keys.length; i++){
			
			// If the badger spent more time in the current group than previously recorded then change the group name being stored
			if(timeSpentInGroups.get(keys[i]) > timeSpentInGroups.get(group)){
				group = keys[i];
			}
		}
		
		return group;
	}
	
	public String findGroupWhenInfectionFirstDetected(char[] statuses, String[] groups){
		
		// Initialise a variable to store the group name string
		String group = "";
		
		// Look at the status of the current badger at each capture event
		for(int i = 0; i < statuses.length; i++){
			
			// Is the status neither negative or unknown - if so then has moved into a disease state
			if(statuses[i] != 'Z' && statuses[i] != 'N'){
				
				// Note the group and the index of the capture event when this occurred
				group = groups[i];
				this.whenInfectionDetected = i;
				
				// Finish
				break;
			}
		}
	
		return group;
	}
	
	public Hashtable<String, long[][]> findWhenBadgerWasPresentInGroups(long[] datesInMilliSecs, String[] groups){
		
		// Initialise a hashtable to record the periods the current badger spent in each of the groups it was captured in
		Hashtable<String, long[][]> groupInfo = new Hashtable<String, long[][]>();
		
		// Initialise variables for recording the period info
		long[][] presentPeriods;
		long[] row;
		
		// Examine each of the groups the current badger inhabited
		for(int i = 0; i < groups.length - 1; i++){ // Note that ignoring the last group - as with no future capture events - can't know how long the badger spent there
			
			// Has the Badger already been in this group?
			if(groupInfo.get(groups[i]) != null){
				
				// Get the Periods spent in the current group
				presentPeriods = groupInfo.get(groups[i]);
				
				// Has it consistently been in this group? - check if the end of the stored period is equal to the current value for the current group
				if(presentPeriods[presentPeriods.length - 1][1] == datesInMilliSecs[i]){
					presentPeriods[presentPeriods.length - 1][1] = datesInMilliSecs[i + 1];
										
				}else{
					// Add a new row for this new period that the current badger was in the current group
					row = new long[2];
					row[0] = datesInMilliSecs[i]; // When it left the previous group - and entered this one
					row[1] = datesInMilliSecs[i + 1]; // When it potentially left the current group
					presentPeriods = MatrixMethods.addRow(presentPeriods, row);
				}
				
				// Put the Information back for the current group
				groupInfo.put(groups[i], presentPeriods);
				
			}else{
				
				// Create a new period spent matrix for the current group
				presentPeriods = new long[1][2];
				
				// Fill in the present period
				presentPeriods[0][0] = datesInMilliSecs[i]; // When it left the previous group - and entered this one
				presentPeriods[0][1] = datesInMilliSecs[i + 1]; // When it potentially left the current group
				
				// Put the periods spent information into the hashtable under the key associated with the current group
				groupInfo.put(groups[i], presentPeriods);
			}			
		}
		
		return groupInfo;
	}	
	
	// PARSING METHODS
	
	// General Methods
	public void orderCaptureDataByDate(){
		int[] orderedIndices = CalendarMethods.getOrder(this.captureDates);
		this.captureDates = CalendarMethods.orderArray(this.captureDates, orderedIndices);
		this.groupsInhabited = ArrayMethods.orderArray(this.groupsInhabited, orderedIndices);
		this.gammaIFNResults = ArrayMethods.orderArray(this.gammaIFNResults, orderedIndices);
		this.statpakResults = ArrayMethods.orderArray(this.statpakResults, orderedIndices);
		this.elisaResults = ArrayMethods.orderArray(this.elisaResults, orderedIndices);
		this.cultureResults = ArrayMethods.orderArray(this.cultureResults, orderedIndices);
		this.vntrTypes = ArrayMethods.orderArray(this.vntrTypes, orderedIndices);
		this.spoligotypes = ArrayMethods.orderArray(this.spoligotypes, orderedIndices);
		this.postMortem = ArrayMethods.orderArray(this.postMortem, orderedIndices);
		this.overallStatuses = ArrayMethods.orderArray(this.overallStatuses, orderedIndices);
	}
	
	public char[] parseOverallStatuses(String[] statuses){
		
		// Create an empty array to store the disease status of the current badger at each of its capture events - as char
		char[] states = new char[statuses.length];
		
		// Examine each of the disease states of the badger at each capture event - store as char
		for(int i = 0; i < statuses.length; i++){
			states[i] = returnOverallStatus(statuses[i]);
		}
		
		return states;		
	}
	
	public char returnOverallStatus(String value){
		char status = 'Z';
		if(value.matches("Negative")){
			status = 'N';
		}else if(value.matches("Exposed")){
			status = 'E';
		}else if(value.matches("Excretor")){
			status = 'X';
		}else if(value.matches("Superexcretor")){
			status = 'S';
		}else if(value.matches("NA") == false){
			System.out.println("ERROR: Unknown Disease Status: " + value);
		}
		
		return status;
	}
	
	public char[] parsePmStatuses(String[] pm){
		
		// Create an array to store whether or not the badger was examined post mortem - as char
		char[] statuses = new char[pm.length];
		
		// Examine each PM status for the capture events and record as char
		for(int i = 0; i < pm.length; i++){
			statuses[i] = returnPmStatus(pm[i]);
		}
		
		return statuses;
	}
	
	public char returnPmStatus(String status){
		
		char result = 'Z';
		if(status.matches("Yes") || status.matches("YES")){
			result = 'Y';
		}else if(status.matches("No") || status.matches("NO")){
			result = 'N';
		}else{
			System.out.println("ERROR: Unknown PM Status Result: " + status);
		}
		
		return result;
	}
	
	public String[] parseSpoligotypes(String[] spoligo){
		
		// Create an array to store the spoligotypes
		String[] types = new String[spoligo.length];
		
		// Examine each spoligotype and insert "NA"s where appropriate
		for(int i = 0; i < spoligo.length; i++){
			if(spoligo[i].matches("")){
				types[i] = "NA";
			}else{
				types[i] = spoligo[i];
			}
		}
		
		return types;
	}
	
	public String[] parseVntrTypes(String[] vntr){
		
		// Create an array to store the VNTR types
		String[] types = new String[vntr.length];
		
		// Examine each of the VNTR types - inserting "NA"s where appropriate
		for(int i = 0; i < vntr.length; i++){
			if(vntr[i].matches("")){
				types[i] = "NA";
			}else{
				types[i] = vntr[i];
			}
		}
		
		return types;
	}
	
	public char[] parseTestResults(String[] values){
		
		char[] results = new char[values.length];
		
		for(int i = 0; i < values.length; i++){
			
			results[i] = 'Z';
			if(values[i].matches("Negative") == true){
				results[i] = 'N';
			}else if(values[i].matches("Positive") == true){
				results[i] = 'Y';
			}
		}

		return results;
	}
	
	public char parseAgeCategory(String ageCategory){
		
		char age = 'U';
		
		if(ageCategory.matches("CUB")){
			age = 'C';
		}else if(ageCategory.matches("YEARLING")){
			age = 'Y';
		}else if(ageCategory.matches("ADULT")){
			age = 'A';
		}else if(ageCategory.matches("NA")){
			age = 'U';
		}else{
			System.out.println("ERROR: Unknown age category: " + ageCategory);
		}
		
		return age;
	}
	
	public char parseSex(String sexString){
		
		char badgerSex = 'U';
		
		if(sexString.matches("MALE")){
			badgerSex = 'M';
		}else if(sexString.matches("FEMALE")){
			badgerSex = 'F';
		}else if(sexString.matches("") || sexString.matches("UNKNOWN") || sexString.matches("NA")){
			badgerSex = 'U';
		}else{
			System.out.println("ERROR: Unknown sex: " + sexString);
		}
		
		return badgerSex;
	}
}
