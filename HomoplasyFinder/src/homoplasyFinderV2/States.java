package homoplasyFinderV2;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;

public class States {

	
	private String[][] stateTable; // State for each tip at each site
	private Hashtable<String, Integer> tipIndices; // Tip index - used to order state information
	private Hashtable<String, boolean[]> stateOptions; // State options for each state
	
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
			
			// I am here!!!! :-)
			
		}
	}
	
	// Methods
	public void noteNucleotideStateOptions() {
		
		// Initialise the state options hashtable - records the possible state for each nucleotide code
		this.stateOptions = new Hashtable<String, boolean[]>();
		
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
	}
	
	public void readInFASTA(String fileName) throws IOException {
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
		this.stateTable = new String[this.tipIndices.size()][0];
		
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
    				
    				this.stateTable[this.tipIndices.get(isolateName)] = sequence.toString().split("");
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
		this.stateTable[this.tipIndices.get(isolateName)] = sequence.toString().split("");
		
		// Check a sequence was found for each tip
		for(String tipName : Methods.getKeysString(this.tipIndices)) {
			
			// Check if tip in sequences table
			if(this.stateTable[this.tipIndices.get(tipName)][0] == null) {
				System.err.println((char)27 + "[31mERROR!! Nucleotide sequence for tip (" + tipName + "), not found in fasta file" + (char)27 + "[0m");
				System.exit(0);
			}
		}
		
		// Print progress if requested
		if(this.verbose == true){
			System.out.println("Finished encoding fasta file as states table.");
		}
	}
	
	public void checkFileType(String type) {
		
		// Check the file type is either "fasta" or "traits"
		if(type.matches("fasta") || type.matches("traits")) {
			this.fileType = type;
		}else {
			System.err.println((char)27 + "[31mERROR!! Unrecognised file type (" + type + "), should be either \"fasta\" or \"traits\"" + (char)27 + "[0m");
			System.exit(0);
		}
	}
	
	public void indexTerminalNodes(ArrayList<Node> terminalNodes) {
		
		// Initialise hashtable to stroe the index of each tip in the phylogeny
		this.tipIndices = new Hashtable<String, Integer>();
		
		// Examine each of the terminal nodes
		for(int i = 0; i < terminalNodes.size(); i++) {
			this.tipIndices.put(terminalNodes.get(i).getName(), i);
		}
	}
}
