package geneticDistances;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;

import testBEASTRateEstimation.Global;

import methods.ArrayMethods;
import methods.GeneralMethods;
import methods.GeneticMethods;
import methods.HashtableMethods;
import methods.WriteToFile;

public class GeneticDistances {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		// Read the FASTA file
		String fasta = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/NewAnalyses_02-06-16/InitialTree/sequences_plusREF_minCov-0.9_20-06-16.fasta"; 
		Sequence[] sequences = readFastaFile(fasta);
		
		// Calculate the genetic distances
		int[][] distances = createGeneticDistanceMatrix(sequences);
		
		// Print out the Genetic Distances
		String distancesFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/NewAnalyses_02-06-16/InitialTree/distances_plusREF_21-06-16_pDistance.txt";
		printGeneticDistances(distances, sequences, distancesFile, "\t");
		
		// Read the NEXUS file
		//String nexus = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/AnalysesForPaper/CombinedEastAndWest/Clade1_SouthIsland/sequences_26-10-15_Clade1_paired.nexus";
		//Sequence[] sequences = readNexusFile(nexus);
		
		// Read the isolate Traits file
		//String isolateTraits = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/AnalysesForPaper/CombinedEastAndWest/Clade1_SouthIsland/isolateTraits_26-10-15_Clade1_paired.txt";
		//Hashtable<String, String> isolateSpecies = readIsolateTraitsFileRecordSpecies(isolateTraits);
		
		// Make the sequence IDs match those in the hashtable
		//changeSequenceNamesToMatchIsolateIds(sequences);
		
		// Calculate Genetic Distances, noting whether they are within or between wildlife and cattle
		//String outputFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/AnalysesForPaper/CombinedEastAndWest/Clade1_SouthIsland/GeneticDistances/geneticDistances_Clade1_paired_26-10-15.txt";
		//printGeneticDistancesNotingWithinBetweenTransmission(outputFile, sequences, isolateSpecies);
		
	}

	public static void printGeneticDistances(int[][] distances, Sequence[] sequences, String fileName, String sep) throws IOException{
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Write the column names (sequence names) into the first row
		String row = "Name";
		for(int i = 0; i < sequences.length; i++){
			row = row + sep + sequences[i].getName();
		}
		WriteToFile.writeLn(bWriter, row);
		
		// Print the genetic distances
		for(int i = 0; i < distances.length; i++){
			
			WriteToFile.writeLn(bWriter, sequences[i].getName() + sep + ArrayMethods.toString(distances[i], sep));
		}
		
		// Close the output file
		WriteToFile.close(bWriter);
	}
	
	public static void printGeneticDistancesNotingWithinBetweenTransmission(String outputFile, Sequence[] sequences,
			Hashtable<String, String> isolateSpecies) throws IOException{
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(outputFile, false);
		
		// Print a Header into the output file
		WriteToFile.writeLn(bWriter, "Distance\tComparison");
		
		// Initialise necessary variables for comparison
		String iSpecies;
		String jSpecies;
		char[] iSequence;
		char[] jSequence;
		double geneticDistance;
		String comparison = "";
		
		int nBovineComparisons = 0;
		int nWildlifeComparisons = 0;
		int nBetweenComparisons = 0;
		
		int[] nIsolates = new int[2];
		
		// Calculate the genetic distances between the isolates
		for(int i = 0; i < sequences.length; i++){
			
			// Get the information for isolate i
			iSequence = sequences[i].getSequence();
			iSpecies = isolateSpecies.get(sequences[i].getName());
			
			if(iSpecies.matches("BOVINE") == true){
				nIsolates[0]++;
			}else{
				nIsolates[1]++;
			}
			
			for(int j = 0; j < sequences.length; j++){
				
				// Only make the comparison once and avoid comparing the same isolate
				if(i >= j){
					continue;
				}
				
				// Get the information for isolate j
				jSequence = sequences[j].getSequence();
				jSpecies = isolateSpecies.get(sequences[j].getName());
				
				// Calculate the Genetic Distance
				geneticDistance = GeneticMethods.calculateNumberDifferencesBetweenSequences(iSequence, jSequence);
				
				// Note whether comparison was within or between wildlife and cattle
				comparison = "";
				if(iSpecies.matches("BOVINE") == true && jSpecies.matches("BOVINE") == true){
					comparison = "BOVINE";
					nBovineComparisons++;
				}else if(iSpecies.matches("BOVINE") == false && jSpecies.matches("BOVINE") == false){
					comparison = "WILDLIFE";
					nWildlifeComparisons++;
				}else if(iSpecies.matches("BOVINE") == true || jSpecies.matches("BOVINE") == true){
					comparison = "BETWEEN";
					nBetweenComparisons++;
				}else{
					System.out.println("Wasn't able to assign a label to the current comparison:");
					System.out.println("Species i = " + iSpecies);
					System.out.println("Species j = " + jSpecies);
				}
				
				// Print the genetic distance out to file
				WriteToFile.writeLn(bWriter, geneticDistance + "\t" + comparison);
			}
		}
		System.out.println();
		
		System.out.println("Made " + (nBovineComparisons + nWildlifeComparisons + nBetweenComparisons) + " comparisons.");
		System.out.println("\t" + nBovineComparisons + " BOVINE comparisons.");
		System.out.println("\t" + nWildlifeComparisons + " WILDLIFE comparisons.");
		System.out.println("\t" + nBetweenComparisons + " BETWEEN comparisons.");
		System.out.println("\nNumber of Cattle Isolates = " + nIsolates[0]);
		System.out.println("Number of Non-Cattle Isolates = " + nIsolates[1]);
		
		// Close the output file
		WriteToFile.close(bWriter);
		
	}
	
	public static int[][] createGeneticDistanceMatrix(Sequence[] sequences){
		
		// Initialise the genetic distance matrix - NOTE using just count of differences
		int[][] distances = new int[sequences.length][sequences.length];
		int distance;
				
		// Compare each isolate to one another
		for(int i = 0; i < sequences.length; i++){
			
			for(int j = 0; j < sequences.length; j++){
				
				// Only make comparison once and skip self comparisons
				if(i >= j){
					continue;
				}
				
				// Calculate the genetic distance
				distance = GeneticMethods.calculateNumberDifferencesBetweenSequences(sequences[i].getSequence(), sequences[j].getSequence());
				
				// Store the calculate genetic distance
				distances[i][j] = distance;
				distances[j][i] = distance;
			}
		}
		
		return distances;
	}
	
	public static void changeSequenceNamesToMatchIsolateIds(Sequence[] sequences){
		
		/**
		 * name structure:
		 * 		AgR136_S18_8.vcf
		 * 		N42_S11_105.vcf
		 * 		12754_8#29_99.vcf
		 * 
		 * Need to use only the first part, and where this begins with N need to replace it with AgR
		 */
		
		// Initialise the necessary variables
		String name = "";
		String[] parts;
		
		// Loop through each of the Isolates
		for(Sequence sequence : sequences){
			
			// Get the isolates ID
			parts = sequence.getName().split("_");
			name = parts[0];
			
			// Check whether it begins with N
			if(name.matches("N(.*)")){
				name = "AgR" + name.split("N")[1];
			}
			
			// Check if ID spans multiple columns
			if(name.matches("127(.*)")){
				name = parts[0] + "_" + parts[1];
			}
			
			// Change the current sequence name
			sequence.setName(name);
		}
	}
	
	public static Hashtable<String, String> readIsolateTraitsFileRecordSpecies(String fileName) throws IOException{
		/**
		 * Isolate Traits File:
		 * 	traits	Species	Location	Area	REA	Latitude	Longitude	CATTLE
		 *	AgR136_1991	BOVINE	TWIZEL	MACKENZIECOUNTRY	11	-44.15434355253987	170.5380525017088	BOVINE
		 */
		
		// Open the input file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise a Hashtable to store the species of each isolate
		Hashtable<String, String> table = new Hashtable<String, String>();
		
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
		
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			
			// Ignore the header
			if(line.matches("traits(.*)") == false){
				
				cols = line.split("\t");
				
				// Store the species of the current isolate
				table.put(cols[0], cols[1]);
			}
		}
		
		// Close the input file
		input.close();
		reader.close();
		
		return table;
	}
	
	public static Hashtable<String, String[]> readIsolateTraitsFile(String fileName) throws IOException{
		
		/**
		 * Isolate Traits File Structure:
		 * 		traits		Species	Latitude	Longitude	Location	Area				Source	CATTLE
		 *		AgR136_1991	BOVINE	-44.266		170.09		TWIZEL		MACKENZIECOUNTRY	Farmed	BOVINE
		 *		0			1		2			3			4			5					6		7
		 */
		
		// Open the input file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
				
		// Initialise a Hashtable to store the species of each isolate
		Hashtable<String, String[]> table = new Hashtable<String, String[]>();
				
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
				
		// Begin reading the file
		while(( line = reader.readLine()) != null){
					
			// Ignore the header
			if(line.matches("traits(.*)") == false){
				
				cols = line.split("\t");
						
				// Store the species of the current isolate
				table.put(cols[0], cols);
			}
		}
				
		// Close the input file
		input.close();
		reader.close();
				
		return table;
	}
	
	public static Sequence[] readFastaFile(String fileName) throws IOException{
		
		/**
		 * FASTA file structure:
		 * 	2 20
		 *  >AgR136_S18_8.vcf
		 *  GGGGCTCCGACAGCCCCGTG
		 *  >AgR145_S24_9.vcf
		 *  GGGGCTCCGACAGCCCCGTG
		 */
		
		// Open the input File
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise variables to process the fasta file
		String line = null;
		String[] cols;
		int lineNo = 0;		
				
		// Initialise an array to store the sequences
		Sequence[] sequences = new Sequence[99999999];
		String name = "";
		String sequence = "";
		int pos = -1;
		
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			lineNo++;
			
			// Is there count information present?
			if(lineNo == 1 && line.matches(">(.*)") == false){
				cols = line.split(" ");
				
				// Initialise the sequence array to the correct size
				sequences = new Sequence[Integer.parseInt(cols[0])];
				
			// Have we found a new isolate?
			}else if(line.matches(">(.*)") == true){
				
				// Store the previous isolates sequence if there was one
				if(name.equals("") == false){
					pos++;
					sequences[pos] = new Sequence(name, sequence.toCharArray());
				}
				
				// Reset the isolate Information
				name = line.substring(1);
				sequence = "";
				
			}else{
				
				// Build the isolates sequence
				sequence = sequence + line;
			}			
		}
		
		// Store the last isolate's sequence
		pos++;
		sequences[pos] = new Sequence(name, sequence.toCharArray());
		
		// Subset the sequences if necessary
		if(sequences.length == 99999999){
			sequences = Sequence.subset(sequences, 0, pos);
		}
		
		// Close the FASTA file
		input.close();
		reader.close();
		
		return sequences;
	}

	public static Sequence[] readNexusFile(String fileName) throws IOException{
		/**
		 * NEXUS file structure:
		 * 	#NEXUS
		 *	
		 *	
		 *	BEGIN DATA;
		 *	DIMENSIONS NTAX=2 NCHAR=20;
		 *	FORMAT MISSING=N GAP=- DATATYPE=DNA;
		 *	MATRIX
		 *	AgR136_1991	ATGGAGGGGCCTCCAGCGGC
		 *	12754_8#29_1992	ATGGAGGGGCCTCCAGCGGC
		 *	;
		 *	END;
		 */
		
		// Open the input File
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise variables to process the fasta file
		String line = null;
		String[] cols;	
						
		// Initialise an array to store the sequences
		Sequence[] sequences = new Sequence[0];
		int pos = -1;
		int nSequences;
		
		// Initialise a variable to note whether we have found the sequence block
		int foundSequences = 0;
		
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			
			// Have we found the dimensions info?
			if(line.matches("DIMENSIONS(.*)") == true){
				
				// Get the number of sequences
				nSequences = Integer.parseInt(line.split(" ")[1].split("=")[1]);
				sequences = new Sequence[nSequences];
				
			// Have we found the sequence block?
			}else if(line.matches("MATRIX(.*)") == true){
				foundSequences = 1;
				
			// Process the isolate sequences
			}else if(foundSequences == 1 && line.matches(";(.*)") == false){
				
				// Get the isolate name and sequence
				cols = line.split("\t");
				
				// Store the isolate sequence
				pos++;
				sequences[pos] = new Sequence(cols[0], cols[1].toCharArray());
				
			// Have we finished with the sequences yet?
			}else if(foundSequences == 1 && line.matches(";(.*)") == true){
				foundSequences = 0;
			}
		}
		
		// Close the input file
		input.close();
		reader.close();
		
		return sequences;
	}

	public static String[] getSequenceNames(Sequence[] sequences){
		String[] names = new String[sequences.length];
		
		for(int i = 0; i < sequences.length; i++){
			names[i] = sequences[i].getName();
		}
		return names;
	}
}
