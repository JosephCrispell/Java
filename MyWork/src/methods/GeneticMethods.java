package methods;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import geneticDistances.GeneticDistances;
import geneticDistances.Sequence;
import woodchesterGeneticVsEpi.CompareIsolates;

public class GeneticMethods {

	public static void main(String[] args) throws IOException{
		
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/NewAnalyses_02-06-16/";
				
		// Get the genetic distances to the Reference
		String fastaFile = path + "InitialTree/";
		fastaFile += "sequences_plusRef_Prox-10_22-08-16.fasta";
		Sequence[] sequences = GeneticDistances.readFastaFile(fastaFile);
		CompareIsolates.setIsolateIds(sequences);
		
		// Calculate the proportion of Ns
		int badger = 0;
		int cow = 0;
		for(int i = 0; i < sequences.length; i++){
			
			if(calculatePropNs(sequences[i].getSequence()) > 0.1){
				if(sequences[i].getName().matches("TB(.*)") == true){
					cow++;
				}else{
					badger++;
				}
				System.out.println(sequences[i].getName() + "\t" + calculatePropNs(sequences[i].getSequence()));
			}			
		}
		
		System.out.println("Found " + (cow + badger) + " crap isolates!\n" + "N. Badgers = " + badger +
				"\nN. Cows = " + cow);
	}
	
	public static double calculatePropNs(char[] sequence){
		
		double value = 0;
		
		for(int i = 0; i < sequence.length; i++){
			if(sequence[i] == 'N'){
				value++;
			}
		}
		
		return value/ (double)sequence.length;
	}
	
	public static int calculateNumberDifferencesBetweenSequences(char[] a, char[] b){
		
		// Assumes the sequences are the same length
		if(a.length != b.length){
			System.out.println("ERROR!: GeneralMethods:calculateGeneticPDistance: Sequences are not the same Length");
		}
		
		int count = 0;
		for(int i = 0; i < a.length; i++){
			if(a[i] != b[i] && a[i] != 'N' && b[i] != 'N'){
				count++;
			}
		}
		
		return count;
	}
	
	public static Sequence[] readFastaFile(String fileName) throws IOException{
		
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
		
		// Open the Sequence Fasta File for Reading
    	InputStream input = new FileInputStream(fileName);
    	BufferedReader reader = new BufferedReader(new InputStreamReader(input));
    	
    	// Initialise Variables to Store the Sequence Information
    	String isolateName = "";
    	String sequence = "";
    	Sequence[] sequences = new Sequence[0];
    	int pos = -1;
    	
    	int noSamples;
    	int noNucleotides = -1;
    	
    	// Begin Reading the Fasta File
    	String line = null;
    	String[] parts;
    	while(( line = reader.readLine()) != null){
    		
    		// Read the Header Information
    		if(line.matches("(^[0-9])(.*)")){
    			parts = line.split(" ");
    			
    			noSamples = Integer.parseInt(parts[0]);
    			noNucleotides = Integer.parseInt(parts[1]);
    			
    			sequences = new Sequence[noSamples];
    		
    		// Deal with the Isolate Sequences
    		}else if(line.matches("(^>)(.*)")){
    			
    			// Store the previous Sequence
    			if(isolateName != ""){
    				
    				pos++;
    				sequences[pos] = new Sequence(isolateName, sequence.toCharArray());
    			}
    			
    			// Get the current isolates Information
    			isolateName = line.substring(1);
    			sequence = "";
    		
    		// Store the isolates sequence
    		}else{
    			
    			sequence = sequence + "" + line;
       		}  		
    	}
		reader.close();
		
		// Store the last isolate
		pos++;
		sequences[pos] = new Sequence(isolateName, sequence.toCharArray()); 
		
		return Sequence.subset(sequences, 0, pos);
	}
	
	public static int calculateNumberDifferencesBetweenSequences(char[] a, char[] b, boolean[] informative){
		
		// Assumes the sequences are the same length
		if(a.length != b.length){
			System.out.println("ERROR!: GeneralMethods:calculateGeneticPDistance: Sequences are not the same Length");
		}
		
		int count = 0;
		for(int i = 0; i < a.length; i++){
			if(a[i] != b[i] && a[i] != 'N' && b[i] != 'N' && informative[i] == true){
				count++;
			}
		}
		
		return count;		
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

}
