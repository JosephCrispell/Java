package ComparingGenomes;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Hashtable;

import javax.swing.plaf.synth.SynthScrollBarUI;

import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.GeneralMethods;
import methods.HashtableMethods;
import methods.WriteToFile;
import smithWatermanAlignment.Alignment;
import smithWatermanAlignment.SmithWaterman;

public class BuildAnnotatedElementDatabase {

	public static void main(String[] args) throws IOException{
		
		// Set the path
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/ComparingReferenceGenomes_10-04-18/ncbi-genomes-2018-04-10/Test/";
				
		// Get the current date
		String date = CalendarMethods.getCurrentDate("dd-MM-yy");
		
		// Set size range for the annotated elements of interest
		int[] sizeRange = {50, 100000};
		
		// Get an array of all the genbank files in the current directory
		String[] genbankFiles = GeneralMethods.getAllFilesInDirectory(path, ".gbff");
		
		// Examine the annotations and their sequences present in each genbank file - store the unique ones
		Hashtable<String, String[]> uniqueAnnotations = readGenbankFilesAndRecordUniqueAnnotations(genbankFiles, path, sizeRange, true);
		
		// TESTING
		System.out.println("############################################################################################################");
		System.out.println("############################################################################################################");
		String[] sequences = HashtableMethods.getKeysString(uniqueAnnotations);
		for(int i = 0; i < sequences.length; i++){
			
			System.out.print(".");
			
			for(int j = 0; j < sequences.length; j++){
				
				if(i >= j){
					continue;
				}

				if(sequences[i].length() == sequences[j].length()){
					
					char[] a = sequences[i].toCharArray();
					char[] b = sequences[j].toCharArray();
					
					Alignment alignment = SmithWaterman.align(a, b, 2, -2, -2, false, false)[0];
					
					if(alignment.getScore() / (double) a.length > 1){
						System.out.println("--------------------------------------------------------------------------------------------------------------------------");
						alignment.print();
					}
					
				}
			}
		}
		//TESTING
		
		System.out.println("Found " + uniqueAnnotations.size() + " unique annotations");
		
		// Print the information for each annotation found
		//writeUniqueAnnotationsToFile(uniqueAnnotations, path, date);
	}
	
	public static void writeUniqueAnnotationsToFile(Hashtable<String, String[]> uniqueAnnotations, String path, String date) throws IOException{
		
		BufferedWriter bWriter = WriteToFile.openFile(path + "UniqueAnnotations_" + date + ".txt", false);
		bWriter.write("Sequence\tSource\tFiles\n");
		for(String key : HashtableMethods.getKeysString(uniqueAnnotations)){
			
			bWriter.write(key.split(":")[0] + "\t" + summariseFilesFoundIn(uniqueAnnotations.get(key)) + "\n");
		}
		bWriter.close();		
	}
	
	public static String summariseFilesFoundIn(String[] fileInfo){
		
		// Initialise a hashtable to store the coords from each file
		Hashtable<String, String> fileCoords = new Hashtable<String, String>();
		
		// Initialise necessary variables for parsing the file information
		String[] parts;
		
		// Initialise a flag to recognise whether annotated element is found in genome, plasmid, or both
		String flag;
		int nGenome = 0;
		int nPlasmid = 0;
		
		// Examine each annotation file record
		for(String info : fileInfo){
			
			// Split the information into its parts
			parts = info.split(":");
			
			// Check annotation index, 0 = genome and >0 = plasmids
			if(parts[1].matches("0") == true){
				nGenome++;
			}else{
				nPlasmid++;
			}
			
			// Check if seen file before
			if(fileCoords.get(parts[0]) != null){
				fileCoords.put(parts[0], fileCoords.get(parts[0]) + "," + parts[2]);
			}else{
				fileCoords.put(parts[0], parts[2]);
			}			
		}
		
		// Check the number of times annotated element was annotated in genome or plasmid
		if(nGenome > 0 && nPlasmid == 0){
			flag = "genome";
		}else if(nPlasmid > 0 && nGenome == 0){
			flag = "plasmid";
		}else{
			flag = "both";
		}
		
		// Build an output string
		String output = flag + "\t";
		String[] keys = HashtableMethods.getKeysString(fileCoords);
		for(int i = 0; i < keys.length; i++){
			
			if(i == 0){
				output += keys[i] + ":" + fileCoords.get(keys[i]);
			}else{
				output += ";" + keys[i] + ":" + fileCoords.get(keys[i]);
			}
		}
		
		return output;
	}
	
	public static Hashtable<String, String[]> readGenbankFilesAndRecordUniqueAnnotations(String[] genbankFiles, String path, int[] sizeRange, boolean verbose) throws IOException{
		
		// Make a note of the annotation types we're interested in
		Hashtable<String, Integer> annotationTypes = new Hashtable<String, Integer>();
		annotationTypes.put("gene", 1);
		annotationTypes.put("CDS", 1);
		annotationTypes.put("rRNA", 1);
		annotationTypes.put("tRNA", 1);
		annotationTypes.put("ncRNA", 1);
		annotationTypes.put("repeat_region", 1);	
		
		// Initialise a hashtable to store the unique annotated sequences found across all the annotation files
		Hashtable<String, String[]> uniqueAnnotations = new Hashtable<String, String[]>();
		
		// Examine each genbank file
		for(int i = 0; i < genbankFiles.length; i++){
			
			if(verbose){
				System.out.println("Reading " + genbankFiles[i] + " (" + (i + 1) + " of " + genbankFiles.length + ")");
			}
			
			// Read and store the genbank file
			GenbankFile genbank = new GenbankFile(path + genbankFiles[i], annotationTypes, verbose);
			
			// Print sequence to file
//			String outputFile = path + genbankFiles[i].replaceAll("gbff", "fasta");
//			BufferedWriter bWriter = WriteToFile.openFile(outputFile, false);
//			bWriter.write(">" + genbankFiles[i] + "\n");
//			bWriter.write(genbank.getAnnotationSet(0).getSequence() + "\n");
//			bWriter.close();
			
			// Check each of the annotated sequences - have they been found before?
			addAnyAdditionalAnnotatedSequencesFound(genbank, uniqueAnnotations, genbankFiles[i], sizeRange, verbose);
		}
		
		return uniqueAnnotations;
	}
	
	public static void addAnyAdditionalAnnotatedSequencesFound(GenbankFile genbank, Hashtable<String, String[]> uniqueAnnotations, String file, int[] sizeRange, boolean verbose){
		
		// Examine each of the annotation sets from the current file
		for(int setIndex = 0; setIndex < genbank.getNumberAnnotationSets(); setIndex++){
			
			// Examine each of its annotations - store any ones we haven't encountered before
			for(String coords : HashtableMethods.getKeysString(genbank.getAnnotationSet(setIndex).getAnnotations())){
				
				// Get the start and end coordinates
				int start = genbank.getAnnotationSet(setIndex).getAnnotations().get(coords).getStart();
				int end = genbank.getAnnotationSet(setIndex).getAnnotations().get(coords).getEnd();
				
				// Skip annotated elements that are too small or too big
				if(end - start < sizeRange[0] || end - start > sizeRange[1]){
					
					if(verbose){
						System.out.println("Ignoring annotation in " + file + " as its size (" + (end - start) + ") is outside of range.");
					}
					continue;
				}
				
				// Get the sequences for the current annotation
				String sequence = genbank.getAnnotationSet(setIndex).getAnnotations().get(coords).getSequence();
				String reverseCompliment = genbank.getAnnotationSet(setIndex).getAnnotations().get(coords).getReverseCompliment();
				
				// Build an annotation set identifier
				String id = file + ":" + setIndex + ":" + start + "-" + end;
				
				// Check if encountered key before
				if(uniqueAnnotations.get(sequence) != null){
					
					// Append the annotation information
					uniqueAnnotations.put(sequence, ArrayMethods.append(uniqueAnnotations.get(sequence), id));
					
				}else if(uniqueAnnotations.get(reverseCompliment) != null){
					
					// Append the annotation information
					uniqueAnnotations.put(reverseCompliment, ArrayMethods.append(uniqueAnnotations.get(reverseCompliment), id));
					
				}else{
					
					// Create new unique annotation with information from current
					String[] annotationIDs = {id};
					uniqueAnnotations.put(sequence, annotationIDs);
				}
			}
		}
	}
}
