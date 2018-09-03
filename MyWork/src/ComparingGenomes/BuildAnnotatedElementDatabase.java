package ComparingGenomes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.format.ResolverStyle;
import java.util.Hashtable;

import javax.swing.plaf.synth.SynthScrollBarUI;

import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.GeneralMethods;
import methods.GeneticMethods;
import methods.HashtableMethods;
import methods.WriteToFile;
import smithWatermanAlignment.Alignment;
import smithWatermanAlignment.SmithWaterman;

public class BuildAnnotatedElementDatabase {

	public static void main(String[] args) throws IOException{
		
		// Set the path
//		String path = "/home/josephcrispell/Desktop/Research/ComparingReferenceGenomes_10-04-18/ncbi-genomes-2018-04-10/Test/";
		String path = "/home/josephcrispell/Desktop/Research/Reference_Casali2012/";
		
		String[] sequences = getSequencesFromGenbankFile(path, "Casali2012_UpdatedH37Rv.embl");
		
		System.out.println("Found " + sequences.length + " sequences");
		
		BufferedWriter bWriter = WriteToFile.openFile(path + "Casali2012_UpdatedH37Rv.fasta", false);
		bWriter.write(">Casali2012_UpdatedH37Rv Mycobacterium tuberculosis H37Rv, complete genome\n");
		
		for(int i = 0; i < sequences[0].length(); i += 80) {
			
			int end = i + 80;
			if(end > sequences[0].length() - 1) {
				end = sequences[0].length(); // Don't add -1 as last index isn't included - I THINK!
			}
			
			bWriter.write(sequences[0].substring(i, end) + "\n");
		}	
		
		bWriter.close();
		
		// Get the current date
		String date = CalendarMethods.getCurrentDate("dd-MM-yy");
		
//		// Set size range for the annotated elements of interest
//		int[] sizeRange = {50, 100000};
//		
//		// Get an array of all the genbank files in the current directory
//		String[] genbankFiles = GeneralMethods.getAllFilesInDirectory(path, ".gbff");
		
//		// Examine the annotations and their sequences present in each genbank file - store the unique ones
//		Hashtable<String, String[]> uniqueAnnotations = readGenbankFilesAndRecordUniqueAnnotations(genbankFiles, path, sizeRange, true, false);
//		
//		System.out.println("Found " + uniqueAnnotations.size() + " unique annotations");
		
		// Print the information for each annotation found
		//writeUniqueAnnotationsToFile(uniqueAnnotations, path, date);
		
//		Hashtable<String, Annotation> annotations = readGenbankFiles(genbankFiles, path, sizeRange, true, false);
//		System.out.println("Found " + annotations.size() + " unique annotations");
	}
	
	public static void addOrUpdateAnnotations(Hashtable<String, Annotation> annotations, String annotationSequence, String file, int set, String type,
			boolean firstFile, boolean verbose) {
		
		// NOT CURRENTLY CONSIDERING WHEN ANNOTATIONS FOUND IN DIFFERENT DIRECTION...
		
		System.out.print("Examining annotation: ");
		
		// Check if already encountered exact sequence
		if(annotations.containsKey(annotationSequence)){
			
			System.out.print("Already exists!\n");
			
			// Add the current file and set to the annotation information
			annotations.get(annotationSequence).addFileAndSet(file, set);
		
		// Check if similar sequence available
		}else if(firstFile == false){
			
			// Initialise a variable to store the length ratio
			double lengthRatio;
		
			// Initialise a variable to store the alignment
			Alignment alignment;
		
			// Initialise a variable to record whether found similar sequence
			boolean found = false;
					
			// Search for similar sequence
			for(String sequence : HashtableMethods.getKeysString(annotations)) {
			
				// Calculate the length ratio between the current annotations
				lengthRatio = (double) sequence.length() / (double) annotationSequence.length();
			
				// Check if of similar lengths
				if(lengthRatio > 0.9 && lengthRatio < 1.1) {
				
					// Align the forward sequence
					alignment = SmithWaterman.align(sequence.toCharArray(), annotationSequence.toCharArray(),
						1, -1, -1, false, false)[0];
				
					// Check if found high scoring alignment
					if((double) alignment.getScore() / (double) annotationSequence.length() > 0.9) {
						System.out.print("Found similar sequence!\n");
						alignment.print();
						
						// Record that found similar sequence
						found = true;
					
						// Add in current annotation sequence but link to similar sequences annotation
						annotations.put(annotationSequence, annotations.get(sequence));
					
						// Add in file and set for current annotation
						annotations.get(sequence).addFileAndSet(file, set);
					
						// Finish search
						break;
					}
				}
			}
		
			// If never found similar sequence - add in annotation as new annotation
			if(found == false) {
				System.out.print("New sequence added\n");
				annotations.put(annotationSequence, new Annotation(annotationSequence, type));
			}
		
		// If in first file - add all annotations ancountered
		}else {
			System.out.print("New sequence added\n");
			annotations.put(annotationSequence, new Annotation(annotationSequence, type));
		}
	}
	
	public static int[] parseCoords(String coords, boolean verbose){
		
		// Remove ">" or "<" if present
		coords = coords.replace(">", "");
		coords = coords.replace("<", "");
		
		// Check if compliment required
		if(coords.matches("complement(.*)") == true){			
			coords = coords.substring(11, coords.length() - 1);
		}
		
		// Convert the coordinates into numbers
		int[] coordinates = null;
		if(coords.matches("join(.*)") == false){
			coordinates = ArrayMethods.range(ArrayMethods.convertToInteger(coords.split("\\.\\.")));
		}else if(verbose) {
			System.out.println("Ignoring annotation with \"join(...)\": " + coords);
		}
		
		return(coordinates);
	}
	
	public static void readAnnotationsInGenbankFile(String path, String fileName, String[] sequences, Hashtable<String, Integer> annotationTypes, 
			Hashtable<String, Annotation> annotations, boolean firstFile, boolean verbose) throws IOException {
		
		// Open the animals table file
		InputStream input = new FileInputStream(path + fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise a variables to parse the file
		String line = null;
		String[] parts;
		
		// Initialise variables for parsing the annotations
		int setIndex = 0;
		boolean foundSequence = false;
		int[] coordinates = new int[2];
		String annotationSequence;
												
		// Begin reading the file
		while(( line = reader.readLine()) != null){			
			
			// Split the current line into parts
			parts = line.split("( +)");
			
			// Check if found new annotation
			if(parts.length == 3 && parts[2].matches("(.*)\\.\\.(.*)") == true && annotationTypes.containsKey(parts[1])){
				
				// Get the annotation coordinates
				coordinates = parseCoords(parts[2], verbose);
				
				// Check that coordinates were retrieved
				if(coordinates != null) {
					
					// Get the annotation sequence
					annotationSequence = sequences[setIndex].substring(coordinates[0]-1, coordinates[1]);
					
					// Add or update annotation information
					addOrUpdateAnnotations(annotations, annotationSequence, fileName, setIndex, parts[1], firstFile, verbose);
				}
				continue;
			}
			
			// Check if found sequence
			if(foundSequence == false && line.matches("ORIGIN(.*)")){
				foundSequence = true;
				continue;
			}
			
			// Check if reached end of sequence
			if(foundSequence == true && line.matches("//")){
				foundSequence = false;
				setIndex++;
				continue;
			}
		}
		
		// Close the input file
		input.close();
		reader.close();		
	}
	
	public static String[] getSequencesFromGenbankFile(String path, String fileName) throws IOException{
		
		// Open the animals table file
		InputStream input = new FileInputStream(path + fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise a variables to parse the file
		String line = null;
		String[] parts;
		
		// Initialise variables to store the sequences found in the current annotation files (genome (and plasmid(s)))
		StringBuilder sequence = new StringBuilder();
		boolean foundSequence = false;
		int setIndex = -1;
		String[] sequences = new String[10];
												
		// Begin reading the file
		while(( line = reader.readLine()) != null){			
			
			// Split the current line into parts
			parts = line.split("( +)");
			
			// Check if found sequence
			if(foundSequence == false && (line.matches("ORIGIN(.*)") || line.matches("SQ(.*)"))){
				foundSequence = true;
				sequence = new StringBuilder();
				continue;
			}
			
			// Check if reached end of sequence
			if(foundSequence == true && line.matches("//")){
				foundSequence = false;
				
				// Store the sequence
				setIndex++;
				if(setIndex < sequences.length) {
					sequences[setIndex] = sequence.toString();
				}else {
					String[] copy = new String[sequences.length + 10];
					for(int i = 0; i < sequences.length; i++) {
						copy[i] = sequences[i];
					}
					copy[setIndex] = sequence.toString();
					sequences = copy;
				}				
				continue;
			}
			
			// Build sequence until end if reached
			if(foundSequence == true){
				for(int i = 1; i < parts.length - 1; i++){
					sequence.append(parts[i].toUpperCase());
				}
			}						
		}
		
		// Close the input file
		input.close();
		reader.close();
		
		// Get the subset of the sequences
		String[] output = sequences;
		if(setIndex < sequences.length - 1) {
			output = new String[setIndex + 1];
			for( int i = 0; i < setIndex + 1; i++) {
				output[i] = sequences[i];
			}
		}		
		
		return(output);
	}

	public static Hashtable<String, Annotation> readGenbankFiles(String[] genbankFiles, String path, int[] sizeRange,
			boolean verbose, boolean printSequence) throws IOException{
		
		// Make a note of the annotation types we're interested in
		Hashtable<String, Integer> annotationTypes = new Hashtable<String, Integer>();
//		annotationTypes.put("gene", 1);
		annotationTypes.put("CDS", 1);
//		annotationTypes.put("rRNA", 1);
//		annotationTypes.put("tRNA", 1);
//		annotationTypes.put("ncRNA", 1);
//		annotationTypes.put("repeat_region", 1);	
		
		// Initialise a hashtable to store the unique annotated sequences found across all the annotation files
		// Note that the same key (sequence) could reference the same Annotation information - hopefully :-|
		Hashtable<String, Annotation> annotations = new Hashtable<String, Annotation>();
		
		// Initialise a variable to store the organism sequence(s)
		String[] sequences;
		
		// Examine each genbank file
		for(int i = 0; i < genbankFiles.length; i++){
			
			if(verbose){
				System.out.println("Reading " + genbankFiles[i] + " (" + (i + 1) + " of " + genbankFiles.length + ")");
			}
			
			// Get the sequence(s) from the current annotation file
			sequences = getSequencesFromGenbankFile(path, genbankFiles[i]);
			if(verbose) {
				System.out.println("Found " + sequences.length + " sequence(s) in " + genbankFiles[i]);
			}
			
			// Read the annotations present in the current file
			readAnnotationsInGenbankFile(path, genbankFiles[i], sequences, annotationTypes, annotations, i == 0, verbose);
		}
		
		return(annotations);
	}	
}
