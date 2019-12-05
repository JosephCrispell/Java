package homoplasyFinderV2;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

public class States {

	
	private String[][] statesTable; // State for each tip at each site
	private String[] sites; // Identifier for each column in states table (position for FASTA, trait name for traits table) 
	private Hashtable<String, Integer> tipIndices; // Tip index - used to order state information
	
	private Hashtable<String, boolean[]> stateOptions; // State options for each position/trait
	private Hashtable<String, Integer> stateIndices; // Index of each state
	private String[] states;
	
	private int nStates; // Number of states for position/trait
	private int nSites; // Number of positions/traits examining
	
	private String fileType; // Either "fasta" or "traits"
	
	private boolean verbose; // Print detailed output?
	
	// Initialise the states object from sequences
	public States(ArrayList<Node> terminalNodes, String fileName, String type, Boolean verbose) throws IOException {
		
		// Check if looking for verbose output
		this.verbose = verbose;
		
		// Index the terminal node IDs
		indexTerminalNodes(terminalNodes);
		
		// Check the file type
		checkFileType(type);
		
		// Check if reading in states file as FASTA
		if(this.fileType.matches("fasta")) {
		
			// Read in the FASTA file
			readInFASTA(fileName);
			
			// Note the boolean state options of each character
			noteNucleotideStateOptions();
			
		// Otherwise read in as traits file
		}else {
			
			// Read in the traits table
			readInTraitsTable(fileName);
		}
	}
	
	// Getting methods
	public int getNSites() {
		return this.nSites;
	}
	public String[] getSites() {
		return this.sites;
	}
	public int getNStates() {
		return this.nStates;
	}
	public String getFileType() {
		return this.fileType;
	}
	public String[] getStates() {
		
		return this.states;
	}

	// Methods - printing FASTA
	public String getSequence(int sequenceIndex, Hashtable<Integer, Integer> positionsToIgnore) {
		
		// Initialise a string to store the nucleotide sequence
		StringBuilder sequence = new StringBuilder(this.nSites - positionsToIgnore.size());
		
		// Examine each site in the current sequence
		for(int position = 0; position < this.statesTable[sequenceIndex].length; position++) {
			
			// Check if ignoring current position
			if(positionsToIgnore.get(position) != null) {
				continue;
			}
			
			// Add current nucleotide to growing sequence
			sequence.append(this.statesTable[sequenceIndex][position]);
		}
		
		return sequence.toString();
	}
	
	// Methods - getting position/trait states	
	public boolean[][] getTipStates(int position, int positionIndex, int[][] stateCountsAtEachPosition, String[][] statesAtEachPosition){
		
		// Check if states from traits file - redefine state options for each trait
		if(this.fileType.matches("traits")) {

			// Define the state options for the current trait
			defineStateOptionsForTrait(position);
		}
		
		// Define an array to record the
		stateCountsAtEachPosition[positionIndex] = new int[this.nStates];
		statesAtEachPosition[positionIndex] = this.states;
		
		// Initialise a matrix to store the state options for each tip
		boolean[][] stateOptionsForTips = new boolean[this.statesTable.length][this.nStates];
		
		// Examine each tip's state are the current position
		for(int row = 0; row < this.statesTable.length; row++) {
			
			// Count current state if not NA
			if(this.stateIndices.get(this.statesTable[row][position]) != null) {
				stateCountsAtEachPosition[positionIndex][this.stateIndices.get(this.statesTable[row][position])]++;
			}
			
			// Store the state options for the current state
			stateOptionsForTips[row] = this.stateOptions.get(this.statesTable[row][position]);
		}
		
		return stateOptionsForTips;
	}
	
	private void defineStateOptionsForTrait(int position){
		
		// Initialise a Hashtable to store the index of each new state as you encounter them
		this.stateIndices = new Hashtable<String, Integer>();
		int index = -1;
		
		// Examine each state for the current trait
		for(int row = 0; row < this.statesTable.length; row++) {
			
			// Skip missing values
			if(this.statesTable[row][position].matches("NA|")) {
				continue;
			}
			
			// Check encountered current state
			if(this.stateIndices.get(this.statesTable[row][position]) == null) {
				
				// Increment the index
				index++;
				
				// Index the current state
				this.stateIndices.put(this.statesTable[row][position], index);
			}
		}
		
		// Note the number of states found for current trait
		this.nStates = this.stateIndices.size();
		
		// Define the boolean state options based upon the state indices
		this.stateOptions = new Hashtable<String, boolean[]>();
		boolean[] options;
		this.states = new String[this.nStates];
		
		// Examine each of the states
		for(String state : Methods.getKeysString(this.stateIndices)) {
			
			// Create the option boolean array
			options = new boolean[this.nStates];
			
			// Turn on the option for the current state
			options[this.stateIndices.get(state)] = true;
			
			// Store the option boolean array for the current state
			this.stateOptions.put(state, options);
			
			// Store the current state
			this.states[this.stateIndices.get(state)] = state;
		}
		
		// Add boolean state options for missing values
		options = new boolean[this.nStates];
		for(int i = 0; i < options.length; i++) {
			options[i] = true;
		}
		this.stateOptions.put("NA", options);
		this.stateOptions.put("", options);
	}
	
	// Methods - reading in states table
 	private void readInTraitsTable(String fileName) throws IOException {
		/**
		 * Traits file structure:
		 * 	ID,traitA,traitB,traitC
		 *  ID,Treatment,Sex,Location
		 *  149,Placebo,F,South
		 *  184,Vaccine,F,North
		 *  152,Vaccine,M,South
		 *  137,Vaccine,F,East
		 *  ...
		 *  
		 * Same structure will work for INDELs:
		 *  ID,51:62,651:662,451:458
		 *  149,0,0,1
		 *  184,1,0,1
		 *  152,0,0,1
		 *  137,0,0,0
		 *  ...
		 */
		
		// Print progress if requested
		if(this.verbose == true){
			System.out.println("Reading traits file: " + fileName + "...");
		}
		
		// Try to open the input file
		InputStream input = null;
		  	try {
		  		input = new FileInputStream(fileName);
		   	}catch(FileNotFoundException e){
		   		System.err.println((char)27 + "[31mERROR!! The input traits file: \"" + fileName + "\" could not be found!" + (char)27 + "[0m");
		   		System.exit(0);
		   	}
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));

		// Initialise a state table to store the sequences
		this.statesTable = new String[this.tipIndices.size()][0];
		
    	// Initialise variables to parse the traits file
		String[] parts;
		String id;
		String[] states;
		int lineNo = 0;
    	
    	// Begin Reading the Fasta File
    	String line = null;
    	while(( line = reader.readLine()) != null){
    		
    		// Increment the line counter
    		lineNo++;
    		
    		// Split the current line into its columns
    		parts = line.split(",");
    		id = parts[0];
    		states = Arrays.copyOfRange(parts, 1, parts.length);
    		
    		// Get the trait names from the header line
    		if(lineNo == 1){
    			
    			this.sites = states;
    			continue;    		
    		}
    		
    		// Store the current line of states for each trait
    		this.statesTable[this.tipIndices.get(id)] = states;
    	}
    	
    	// Close the traits file
		reader.close();
		
		// Store the number of traits to be examined
		this.nSites = this.statesTable[0].length;
		
		// Print progress if requested
		if(this.verbose == true){
			System.out.println("Finished encoding traits file as states table.");
		}
	}
	
	private void noteNucleotideStateOptions() {
		
		// Initialise the state options hashtable - records the possible state for each nucleotide code
		this.stateOptions = new Hashtable<String, boolean[]>();
		this.nStates = 4;
		
		// Encode all the possible nucleotide options
		boolean[] possibleForA = {true, false, false, false};
		this.stateOptions.put("A", possibleForA);
		this.stateOptions.put("a", possibleForA);
		
		boolean[] possibleForC = {false, true, false, false};
		this.stateOptions.put("C", possibleForC);
		this.stateOptions.put("c", possibleForC);
		
		boolean[] possibleForG = {false, false, true, false};
		this.stateOptions.put("G", possibleForG);
		this.stateOptions.put("g", possibleForG);
		
		boolean[] possibleForT = {false, false, false, true};
		this.stateOptions.put("T", possibleForT);
		this.stateOptions.put("t", possibleForT);
		
		boolean[] possibleForN = {true, true, true, true};
		this.stateOptions.put("N", possibleForN);
		this.stateOptions.put("n", possibleForN);
		
		boolean[] possibleForDash = {true, true, true, true};
		this.stateOptions.put("-", possibleForDash);
		
		boolean[] possibleForR = {true, false, true, false};
		this.stateOptions.put("R", possibleForR);
		this.stateOptions.put("r", possibleForR);
		
		boolean[] possibleForY = {false, true, false, true};
		this.stateOptions.put("Y", possibleForY);
		this.stateOptions.put("y", possibleForY);
		
		// Define the index of each nucleotide
		this.stateIndices = new Hashtable<String, Integer>();
		this.stateIndices.put("A", 0);
		this.stateIndices.put("a", 0);
		this.stateIndices.put("C", 1);
		this.stateIndices.put("c", 1);
		this.stateIndices.put("G", 2);
		this.stateIndices.put("g", 2);
		this.stateIndices.put("T", 3);
		this.stateIndices.put("t", 3);
		
		this.states = new String[4];
		this.states[0] = "A";
		this.states[1] = "C";
		this.states[2] = "G";
		this.states[3] = "T";
	}
	
	private void readInFASTA(String fileName) throws IOException {
		/**
		 * FASTA file structure:
		 * 		220 3927
		 *		>WB98_S53_93.vcf
		 *		GGGCCTCTNNNCTTCAATACCCCCGATACAC
		 *		>WB99_S59_94.vcf
		 *		GGGCCTCTNNNNTTCAATACCCCCGATACAC
		 *		... 
		 */
		
		// Print progress if requested
		if(this.verbose == true){
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

		// Initialise a state table to store the sequences
		this.statesTable = new String[this.tipIndices.size()][0];
		
    	// Initialise Variables to store the Sequence Information
    	String isolateName = "";
    	StringBuilder sequence = new StringBuilder();
    	
    	// Begin Reading the Fasta File
    	String line = null;
    	while(( line = reader.readLine()) != null){
    		
    		// Skip the Header Information if present
    		if(line.matches("(^[0-9])(.*)")){
    			continue;
    			
    		// Deal with the Isolate Sequences
    		}else if(line.matches("(^>)(.*)")){
    			
    			// Store the previous Sequence
    			if(isolateName != ""){
    				
    				this.statesTable[this.tipIndices.get(isolateName)] = sequence.toString().split("");
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
		this.statesTable[this.tipIndices.get(isolateName)] = sequence.toString().split("");
		
		// Store the number of sites
		this.nSites = this.statesTable[0].length;
		
		// Check a sequence was found for each tip
		for(String tipName : Methods.getKeysString(this.tipIndices)) {
			
			// Check if tip in sequences table
			if(this.statesTable[this.tipIndices.get(tipName)][0] == null) {
				System.err.println((char)27 + "[31mERROR!! Nucleotide sequence for tip (" + tipName + "), not found in fasta file" + (char)27 + "[0m");
				System.exit(0);
			}
		}
		
		// Note the positions of the fasta
		this.sites = new String[this.statesTable[0].length];
		for(int position = 0; position < this.statesTable[0].length; position++) {
			this.sites[position] = Integer.toString(position);
		}
		
		// Print progress if requested
		if(this.verbose == true){
			System.out.println("Finished encoding fasta file as states table.");
		}
	}
	
	private void checkFileType(String type) {
		
		// Check the file type is either "fasta" or "traits"
		if(type.matches("fasta") || type.matches("traits")) {
			this.fileType = type;
		}else {
			System.err.println((char)27 + "[31mERROR!! Unrecognised file type (" + type + "), should be either \"fasta\" or \"traits\"" + (char)27 + "[0m");
			System.exit(0);
		}
	}
	
	private void indexTerminalNodes(ArrayList<Node> terminalNodes) {
		
		// Initialise hashtable to stroe the index of each tip in the phylogeny
		this.tipIndices = new Hashtable<String, Integer>();
		
		// Examine each of the terminal nodes
		for(int i = 0; i < terminalNodes.size(); i++) {
			this.tipIndices.put(terminalNodes.get(i).getName(), i);
		}
	}
}
