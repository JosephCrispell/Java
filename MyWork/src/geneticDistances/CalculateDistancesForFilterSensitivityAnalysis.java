package geneticDistances;

import java.io.BufferedWriter;
import java.io.IOException;

import methods.ArrayMethods;
import methods.GeneralMethods;
import methods.GeneticMethods;
import methods.WriteToFile;

public class CalculateDistancesForFilterSensitivityAnalysis {

	public static void main(String[] args) throws IOException {
		
		// Read in the fasta file
		String fasta = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/VNTR10/NewAnalyses_18-05-16/FilterSensitivity/sequences_1_23-08-16.fasta"; 
		//String fasta = args[0];
		Sequence[] sequences = GeneticDistances.readFastaFile(fasta);
		
		// Calculate the genetic distances
		String geneticDistancesFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/VNTR10/NewAnalyses_18-05-16/FilterSensitivity/distances.txt";
		//String geneticDistancesFile = args[1];
		calculateGeneticDistances(sequences, geneticDistancesFile);
	}
	
	public static void calculateGeneticDistances(Sequence[] sequences, String fileName) throws IOException{
		
		// Initialise variables to record some statistics
		int sequenceLength = sequences[0].getSequence().length;
		double[] proportionNs = new double[sequences.length];
		char[] sequence;
		int distance;
		
		// Initialise a string to store the output
		String output = "Genetic\n";
				
		// Compare each isolate to one another
		for(int i = 0; i < sequences.length; i++){
			
			// Examine the i sequence
			sequence = sequences[i].getSequence();
			
			for(int pos = 0; pos < sequenceLength; pos++){
				if(sequence[pos] == 'N'){
					proportionNs[i]++;
				}
			}
			proportionNs[i] = proportionNs[i] / sequenceLength;
			
			for(int j = 0; j < sequences.length; j++){
				
				// Only make comparison once and skip self comparisons
				if(i >= j){
					continue;
				}
				
				// Calculate the genetic distance
				distance = GeneticMethods.calculateNumberDifferencesBetweenSequences(sequences[i].getSequence(), sequences[j].getSequence());
				
				// Out the distance to the output
				output += distance + "\n";
			}
		}
		
		// Write the output out to file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		WriteToFile.write(bWriter, output);
		WriteToFile.close(bWriter);
		
		// Print out the sequence statistics
		System.out.println(sequenceLength + "\t" + ArrayMethods.mean(proportionNs));
	}
	
	
	
}
