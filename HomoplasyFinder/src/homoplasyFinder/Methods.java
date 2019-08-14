package homoplasyFinder;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Set;

public class Methods {
	
	public static String getCurrentDate(String format){
		
		// Get the current date and time
		DateFormat dateFormat = new SimpleDateFormat(format);
		Calendar cal = Calendar.getInstance();
		
		return dateFormat.format(cal.getTime());
	}
	
	public static Hashtable<Integer, Integer> indexArrayListInteger(ArrayList<Integer> array){
		
		Hashtable<Integer, Integer> indexed = new Hashtable<Integer, Integer>();
		for(int i = 0; i < array.size(); i++) {
			indexed.put(array.get(i), i);
		}
		
		return indexed;
	}
	
	public static char[] deletePositions(char[] array, Hashtable<Integer, Integer> positionsToIgnore){
		
		// Initialise a new array to store the sequence
		char[] output = new char[array.length - positionsToIgnore.size()];
		int pos = -1;
		for(int i = 0; i < array.length; i++){
			
			if(positionsToIgnore.containsKey(pos) == true){
				continue;
			}
			
			pos++;
			output[pos] = array[i];
		}
		
		return output;
	}
	
	public static String toString(char[] array){
		StringBuilder string = new StringBuilder();
		
		for(int i = 0; i < array.length; i++){
			string.append(array[i]);
		}
		
		return string.toString();
	}
	
	public static String toString(int[] array, String sep){
		String string = Integer.toString(array[0]);
		
		for(int i = 1; i < array.length; i++){
			string = string + sep + Integer.toString(array[i]);
		}
		
		return string;
	}
	
	public static PresenceAbsence readPresenceAbsenceTable(String fileName, boolean verbose) throws NumberFormatException, IOException{
		/**
		 * Presence absence table structure:
		 * 		start,end,isolateA,isolateB,...
		 * 		7,53,1,0,1,...
		 * 		1045,1054,1,0,0,...
		 * 
		 * - Comma separated. 
		 * - Columns are tips in phylogeny
		 * - Rows are regions on genome
		 * - Presence = 1
		 * - Absence = 0
		 */
		
		// Print progress if requested
		if(verbose == true){
			System.out.println("Reading presence/absence file: " + fileName + "...");
		}
		
		// Try to open the input file
		InputStream input = null;
		  	try {
		  		input = new FileInputStream(fileName);
		   	}catch(FileNotFoundException e){
		   		System.err.println((char)27 + "[31mERROR!! The input presence/absence file: \"" + fileName + "\" could not be found!" + (char)27 + "[0m");
		   		System.exit(0);
		   	}
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
    	
		// Initialise an Array list to store the region coordinates
		ArrayList<int[]> regionCoords = new ArrayList<int[]>();
		
    	// Initialise variables to store the sequence information
    	String[] names = null;
    	StringBuilder[] sequences = null;
    	    	
    	// Initialise a line counter
    	int lineNo = -1;
    	
    	// Begin reading the presence/absence file
    	String line = null;
    	String[] parts;
    	while(( line = reader.readLine()) != null){
    		
    		// Iterate the line counter
    		lineNo++;
    		
    		// Split the line into its parts
    		parts = line.split(",", -1);
    		
    		// Get information from header line
    		if(lineNo == 0) {
    			
    			// Store the isolate names (should link to tips in phylogeny
    			names = Arrays.copyOfRange(parts, 2, parts.length);
    			
    			// Initialise strings to store the zeros and ones
    			sequences = new StringBuilder[names.length];
    			for(int i = 0; i < names.length; i++) {
    				sequences[i] = new StringBuilder();
    			}
    			
    			// Skip line
    			continue;
    		}
    		
    		// Store the coordinates of the current region
    		int[] coords = {Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    		regionCoords.add(coords);
    		
    		// Add the zeros and ones to the growing sequences
    		for(int i = 0; i < names.length; i++) {
   				sequences[i].append(parts[i + 2]);
    		}
    	}
    	
    	// Close the presence/absence file
    	reader.close();
    	
    	// Initialise an ArrayList to store the sequences of zeros and ones
    	ArrayList<Sequence> presenceAbsenceSequences = new ArrayList<Sequence>(names.length);
    	
    	// Examine each isolate
    	for(int i = 0; i < names.length; i++) {
    		
    		// Create a Sequence object for each isolate (sequence of zeros and ones)
    		presenceAbsenceSequences.add(new Sequence(names[i], sequences[i].toString().toCharArray()));
    	}
		
		return new PresenceAbsence(regionCoords, presenceAbsenceSequences);		
	}
	
	public static ArrayList<Sequence> readFastaFile(String fileName, boolean verbose) throws IOException{
		
		/**
		 * FASTA file structure:
		 * 		220 3927
		 *		>WB98_S53_93.vcf
		 *		GGGCCTCTNNNCTTCAATACCCCCGATACAC
		 *		>WB99_S59_94.vcf
		 *		GGGCCTCTNNNNTTCAATACCCCCGATACAC
		 *		... 
		 * 
		 */
		
		// Print progress if requested
		if(verbose == true){
			System.out.println("Reading fasta file: " + fileName + "...");
		}
		
		// Try to open the input file
		InputStream input = null;
		  	try {
		  		input = new FileInputStream(fileName);
		   	}catch(FileNotFoundException e){
		   		System.err.println((char)27 + "[31mERROR!! The input FASTA file: \"" + fileName + "\" could not be found!" + (char)27 + "[0m");
		   		System.exit(0);
		   	}
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
    	
    	// Initialise Variables to Store the Sequence Information
    	String isolateName = "";
    	StringBuilder sequence = new StringBuilder();
    	ArrayList<Sequence> sequences = new ArrayList<Sequence>();
    	
    	int noSamples = -1;
    	
    	// Begin Reading the Fasta File
    	String line = null;
    	String[] parts;
    	while(( line = reader.readLine()) != null){
    		
    		// Read the Header Information
    		if(line.matches("(^[0-9])(.*)")){
    			parts = line.split(" ");
    			
    			noSamples = Integer.parseInt(parts[0]);
    			
    			sequences = new ArrayList<Sequence>(noSamples);
    		
    		// Deal with the Isolate Sequences
    		}else if(line.matches("(^>)(.*)")){
    			
    			// Store the previous Sequence
    			if(isolateName != ""){
    				
  					sequences.add(new Sequence(isolateName, sequence.toString().toCharArray()));
       			}
    			
    			// Get the current isolates Information
    			isolateName = line.substring(1);
    			sequence = new StringBuilder();
    		
    		// Store the isolates sequence
    		}else{
    			
    			sequence.append(line);
       		}  		
    	}
    	
    	// Close the FASTA file
		reader.close();
		
		// Store the last isolate
		sequences.add(new Sequence(isolateName, sequence.toString().toCharArray()));
		
		return sequences;
	}
	
	public static String[] getKeysString(Hashtable table){
		
		Set<String> keys = table.keySet();
		String[] values = new String[table.size()];
		
		int pos = -1;
		for(String key : keys){
			pos++;
			
			values[pos] = key;
		}
		
		return values;
		
	}
	
	public static String toStringDouble(ArrayList<Double> array, String sep){
		StringBuilder string = new StringBuilder(array.size());
		string.append(array.get(0));
		for(int i = 1; i < array.size(); i++){
			string.append(sep);
			string.append(array.get(i));
		}
		
		return string.toString();
	}

	public static String toStringChar(ArrayList<Character> array){
		StringBuilder string = new StringBuilder(array.size());
		for(int i = 0; i < array.size(); i++){
			string.append(array.get(i));
		}
		
		return string.toString();
	}
	
	public static String toStringInt(ArrayList<Integer> array, String sep){
		StringBuilder string = new StringBuilder(array.size());
		string.append(array.get(0));
		for(int i = 1; i < array.size(); i++){
			string.append(sep + array.get(i));
		}
		
		return string.toString();
	}

	public static ArrayList<Character> subsetChar(ArrayList<Character> array, int start, int end){
		
		return new ArrayList<Character>(array.subList(start, end));
	}
	
	public static ArrayList<Character> toArrayList(char[] array){
		
		ArrayList<Character> arrayList = new ArrayList<Character>();
		for(char value : array) {
			arrayList.add(value);
		}
		
		return arrayList;
	}
	
	public static ArrayList<Double> copyDouble(ArrayList<Double> array){
		
		ArrayList<Double> copy = new ArrayList<Double>();
		for(double value : array){
			copy.add(value);
		}
		
		return copy;
	}
}
