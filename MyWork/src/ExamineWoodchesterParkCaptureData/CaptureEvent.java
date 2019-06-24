package ExamineWoodchesterParkCaptureData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;

import methods.ArrayListMethods;
import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.GeneralMethods;

public class CaptureEvent {

	public String tattoo;
	public boolean postMortem;
	public String ageAtFirstCapture; // CUB, YEARLING, ADULT, null
	public char sex; // M, F, N
	public Calendar date;
	public String socialGroup; // Group name with no spaces or NA
	public boolean gamma;
	public boolean statpak;
	public boolean elisa;
	public boolean culture;
	public int statusIndex; // Negative 0, Exposed 1, Excretor 2, Superexcretor 3, Null -1
	public String spoligotype; // NA or value
	public String vntr;
	
	public CaptureEvent(String line){
		
		/**
		 * 	Capture Data file structure:
		 * 	tattoo	pm	age_fc	year_fc	sex	date	Capture Year	socg	Gamma IFN Result	StatPak Result	weight
		 * 	0		1	2		3		4	5		6				7		8					9				10
		 * 
		 * 	culture	Spoligo	VNTR	ELISA	body_length	PWM_1	PWM_2	PPD-A_1	PPD-A_2	PPD-B_1	PPD-B_2	Nil_1	Nil_2
		 * 	11		12		13		14		15			16		17		18		19		20		21		22		23
		 * 
		 * 	Test_Info	Overall DiseaseStatus
		 * 	24			25
		 */
		
		// Check the line for quotes - some of the cells have quotes and contain commas
		line = checkForQuotesContainingCommas(line);
		
		// Spliot the current line into its parts
		String[] parts = line.split(",", -1);
		int[] dateFormat = {0, 1, 2}; // day, month, year
		
		this.tattoo = parts[0];
		this.postMortem = checkPostMortemColumn(parts[1]);
		this.ageAtFirstCapture = getAgeAtFirstCapture(parts[2]);
		this.sex = returnSex(parts[4]);
		this.date = CalendarMethods.parseDateWithMonthInText(parts[5], "-", dateFormat, 3, true);
		this.socialGroup = GeneralMethods.removeDelimiter(parts[7], " ");
		this.gamma = parts[8].matches("Positive");
		this.statpak = parts[9].matches("P");
		this.elisa = parts[14].matches("Positive");
		this.culture = parts[11].matches("M.BOVIS");
		this.statusIndex = returnStatusIndex(GeneralMethods.removeDelimiter(parts[25], " "));
		this.spoligotype = parseSpoligotype(parts[12]);
		this.vntr = parseVntr(parts[13]);
	}
	
	// Getting methods
	public String getTattoo(){
		return this.tattoo;
	}
	public boolean getPostMortem(){
		return this.postMortem;
	}
	public String getAgeAtFirstCapture(){
		return this.ageAtFirstCapture;
	}
	public char getSex(){
		return this.sex;
	}
	public Calendar getDate(){
		return this.date;
	}
	public String getSocialGroup(){
		return this.socialGroup;
	}
	public boolean getGamma(){
		return this.gamma;
	}
	public boolean getStatpak(){
		return this.statpak;
	}
	public boolean getElisa(){
		return this.elisa;
	}
	public boolean getCulture(){
		return this.culture;
	}
	public int getStatusIndex(){
		
		return statusIndex;
	}
	public String getStatus(){
		// Negative 0, Exposed 1, Excretor 2, Superexcretor 3, Null -1
		
		String[] statuses = {"Negative", "Exposed", "Excretor", "Superexcretor"};
		String output = "NA";
		if(this.statusIndex != -1){
			output = statuses[this.statusIndex];
		}
		
		return output;
	}
	public String getSpoligotype(){
		return this.spoligotype;
	}
	public String getVntr(){
		return this.vntr;
	}
	
	// Setting methods
	public void setSocialGroup(String group){
		this.socialGroup = group;
	}
	
	// Parsing capture information
	public String parseVntr(String value){

		if(value.matches("") == true){
			value = "NA";
		}
		
		// Remove semi-colons if present
		value = GeneralMethods.replaceDelimiter(value, ";", ",");
		
		// Remove quotes if present
		value = GeneralMethods.replaceDelimiter(value, "\"", "");
		
		return value;
	}
	public String parseSpoligotype(String value){

		if(value.matches("") == true){
			value = "NA";
		}
		
		// Remove semi-colons if present
		value = GeneralMethods.replaceDelimiter(value, ";", ",");
		
		// Remove quotes if present
		value = GeneralMethods.replaceDelimiter(value, "\"", "");
		
		return value;
	}
  	public int returnStatusIndex(String status){
		
 		int statusIndex = -1;
 		
 		// Define the status indices
		Hashtable<String, Integer> statusIndices = new Hashtable<String, Integer>();
		statusIndices.put("Negative", 0);
		statusIndices.put("Exposed", 1);
		statusIndices.put("Excretor", 2);
		statusIndices.put("Superexcretor", 3);
		
		if(statusIndices.get(status) != null){
			statusIndex = statusIndices.get(status);
		}
		
		// Return the index for the input status
		return statusIndex;
 	}
	public char returnSex(String value){
		
		char result = 'N';
		if(value.matches("MALE") == true){
			result = 'M';
		}else if(value.matches("FEMALE") == true){
			result = 'F';
		}
		
		return result;
	}
	public String getAgeAtFirstCapture(String value){
		if(value.matches("CUB") == false && value.matches("ADULT") == false && value.matches("YEARLING") == false){
			value = null;
		}
		
		return value;
	}
	public boolean checkPostMortemColumn(String value){
		
		boolean result = false;
		if(value.matches("Yes") == true){
			result = true;
		}
		
		return result;
	}
	
	// General methods
	public static String checkForQuotesContainingCommas(String line) {
		
		// Check if the current line contains a quote
		if(line.contains("\"")) {
			
			// Identify the index of each quote
			ArrayList<Integer> indices = ArrayListMethods.getIndicesOfCharacterInString(line, '\"');
			
			// Examine each pair of indices
			for(int i = 0; i < indices.size(); i = i + 2) {
				
				// Get the string between the current quotes
				String substring = line.substring(indices.get(i)+1, indices.get(i + 1));
				
				// Replace the comma in the substring - if present
				String substringWithoutComma = GeneralMethods.replaceDelimiter(substring, ",", "-");
				
				// Replace the original quoted block in the input line
				line = line.replaceAll("\"" + substring + "\"", substringWithoutComma);
			}
		}
		
		return(line);
	}
	
	public static CaptureEvent[] copy(CaptureEvent[] array){
		CaptureEvent[] copy = new CaptureEvent[array.length];
		
		for(int i = 0; i < array.length; i++){
			copy[i] = array[i];
		}
		
		return copy;
	}
	
	public static CaptureEvent[] sort(CaptureEvent[] array){
		
		CaptureEvent[] srtdArray = copy(array);
		
		/**
		 * This Method Uses the Bubble Sort Algorithm
		 * 		Described here: http://en.wikipedia.org/wiki/Bubble_sort
		 * 
		 * 	For each element, compare it to the next element. If it is larger than the next element, swap the elements.
		 * 	Do this for each element of the list (except the last). Continue to iterate through the list elements and
		 *  make swaps until no swaps can be made.
		 */
		
		CaptureEvent a;
		CaptureEvent b;
		
		int swappedHappened = 1;
		while(swappedHappened == 1){ // Continue to compare the List elements until no swaps are made
		
			int swapped = 0;
			for(int index = 0; index < array.length - 1; index++){
				
				// Compare Current Element to Next Element
				if(CalendarMethods.after(srtdArray[index].getDate(), srtdArray[index + 1].getDate()) == true){
					
					// Swap Current Element is Larger
					a = srtdArray[index];
					b = srtdArray[index + 1];
					
					srtdArray[index] = b;
					srtdArray[index + 1] = a;
					
					// Record that a Swap occurred
					swapped++;
				}
			}
			
			// Check if any swaps happened during the last iteration - if none then finished
			if(swapped == 0){
				swappedHappened = 0;
			}
		}
		
		return srtdArray;
	}
	
	public static CaptureEvent[] subset(CaptureEvent[] array, int start, int end){
		CaptureEvent[] part = new CaptureEvent[end - start + 1];
		
		int pos = -1;
		for(int index = 0; index < array.length; index++){
			
			if(index >= start && index <= end){
				pos++;
				part[pos] = array[index];
			}
		}
		
		return part;
	}
	
	public static CaptureEvent[] append(CaptureEvent[] array, CaptureEvent value){
		CaptureEvent[] newArray = new CaptureEvent[array.length + 1];
		
		for(int index = 0; index < array.length; index++){
			newArray[index] = array[index];
		}
		newArray[newArray.length - 1] = value;
		
		return newArray;
	}
}
