package smithWatermanAlignment;

import java.io.IOException;

import javax.swing.plaf.synth.SynthSeparatorUI;

import methods.ArrayMethods;

public class SmithWaterman {

	/**
	 * An implementation of the Smith-Waterman local alignment algorithm
	 * - Took all instructions from: https://en.wikipedia.org/wiki/Smith%E2%80%93Waterman_algorithm#Example
	 * 
	 * Extend to include gap extension
	 */
	
	public static void main(String[] args) throws IOException{
		
		char[] a = "ACTGCTCG".toCharArray();
		char[] b = "ACTGCCTCT".toCharArray();
		
		int match = 2;
		int misMatch = -1;
		int gap = -1;
		
		int[][] scoringMatrix = new int[a.length + 1][b.length + 1];
		printScoringMatrix(scoringMatrix, a, b);
		System.out.println("\n\n");

		int[][][] scoreSources = fillInScoringMatrix(scoringMatrix, a, b, match, misMatch, gap);
		printScoringMatrix(scoringMatrix, a, b);
		System.out.println("\n\n");
		printSourceMatrix(scoreSources, a, b);
		System.out.println("\n\n");
		
		Alignment[] alignments = constructAlignments(scoringMatrix, a, b, scoreSources, true, true);

	}
	
	public static Alignment[] align(char[] a, char[] b, int match, int misMatch, int gap, boolean returnAll, boolean print){
		
		// Initialise the scoring matrix
		int[][] scoringMatrix = new int[a.length + 1][b.length + 1];

		// Fill the scoring matrix
		int[][][] scoreSources = fillInScoringMatrix(scoringMatrix, a, b, match, misMatch, gap);
		
		// Find the best alignment(s)
		Alignment[] alignments = constructAlignments(scoringMatrix, a, b, scoreSources, print, returnAll);
		
		return alignments;
	}
	
	public static Alignment[] constructAlignments(int[][] scoringMatrix, char[] a, char[] b, int[][][] sources, boolean print, boolean all){
		
		// Find the max indices
		int[][] maxIndices = findIndicesOfMaximums(scoringMatrix);
		
		// Initialise an array to store the alignments
		Alignment[] alignments = new Alignment[maxIndices[0].length];
		
		// Reconstruct the alignment associated with each maximum found
		int nAlignmentsToRecord = maxIndices[0].length;
		if(all == false){
			nAlignmentsToRecord = 1;
		}
		for(int pos = 0; pos < nAlignmentsToRecord; pos++){
			
			// Get the current max's indices
			int i = maxIndices[0][pos];
			int j = maxIndices[1][pos];
			
			// Build the alignment		
			StringBuffer alignedA = new StringBuffer();
			StringBuffer alignedB = new StringBuffer();
			addNextAlignedPair(scoringMatrix, sources, i, j, a, b, alignedA, alignedB);

			// Store the alignment
			alignments[pos] = new Alignment(alignedA.reverse().toString().toCharArray(), alignedB.reverse().toString().toCharArray(), scoringMatrix[i][j]);
			
			// Print alignment if requested
			if(print){
				alignments[pos].print();
			}
		}
		
		return alignments;
	}
	
	public static void addNextAlignedPair(int[][] scoringMatrix, int[][][] sources, int i, int j, char[] a, char[] b, StringBuffer alignedA, StringBuffer alignedB){
		
		// Check that not finished - score != 0
		if(scoringMatrix[i][j] != 0){
			
			// Diagonal move
			if(i-1 == sources[i][j][0] && j-1 == sources[i][j][1]){
				alignedA.append(a[i-1]);
				alignedB.append(b[j-1]);
				
			
			// Horizontal
			}else if(i == sources[i][j][0] && j-1 == sources[i][j][1]){
				alignedA.append("-");
				alignedB.append(b[j-1]);
				
			// Vertical
			}else if(i-1 == sources[i][j][0] && j == sources[i][j][1]){
				alignedA.append(a[i-1]);
				alignedB.append("-");
			}
			
			// Move to next position in scoring matrix
			addNextAlignedPair(scoringMatrix, sources, sources[i][j][0], sources[i][j][1], a, b, alignedA, alignedB);
		}
	}
	
	public static int[][] findIndicesOfMaximums(int[][] scoringMatrix){
		
		// Initialise two arrays to store the i and j indices of the maximum values
		int[] iIndices = new int[0];
		int[] jIndices = new int[0];
		
		// Initialise a variable to record the max - values in scoring matrix can't be less than 1
		int max = -1;
		
		// Examine each score in the scoring matrix
		for(int i = 0; i < scoringMatrix.length; i++){
			for(int j = 0; j < scoringMatrix[0].length; j++){
				
				// Check if found new max
				if(scoringMatrix[i][j] > max){
					
					max = scoringMatrix[i][j];
					iIndices = new int[1];
					iIndices[0] = i;
					jIndices = new int[1];
					jIndices[0] = j;
					
				// Check if found new occurrence of current max
				}else if(scoringMatrix[i][j] == max){
					
					iIndices = ArrayMethods.append(iIndices, i);
					jIndices = ArrayMethods.append(jIndices, j);
				}
			}
		}
		
		int[][] output = {iIndices, jIndices};
		
		return output;
	}
	
	public static int[][][] fillInScoringMatrix(int[][] scoringMatrix, char[] a, char[] b, int match, int misMatch, int gap){
		
		// Initialise a matrix to store the coordinates of the source
		int[][][] sources = new int[scoringMatrix.length][scoringMatrix[0].length][2];
		
		// Initialise an array to store the scores of movements through the scoring matrix
		int[] scores = new int[4];
		int maxIndex;
		
		// Calculate the score for each cell in the scoring matrix
		for(int i = 1; i <= a.length; i++){
			
			for(int j = 1; j <= b.length; j++){
				
				// Reset the scoring array - note last value is zero - stops negative scores
				scores = new int[4];
				int[][] sourceIndices = {{i-1, j-1}, {i, j-1}, {i-1, j}};
				
				// Calculate the diagonal score
				scores[0] = scoringMatrix[i-1][j-1] + compareNucleotides(a[i-1], b[j-1], match, misMatch);
				
				// Calculate the horizontal score
				scores[1] = scoringMatrix[i][j-1] + gap;
				
				// Calculate the vertical score
				scores[2] = scoringMatrix[i-1][j] + gap;
				
				// Find the maximum index in the scores
				maxIndex = ArrayMethods.findMaxs(scores)[0];
				
				// Store the indices of the chosen move
				if(scores[maxIndex] != 0){
					sources[i][j][0] = sourceIndices[maxIndex][0];
					sources[i][j][1] = sourceIndices[maxIndex][1];
				}				
				
				// Store the calculated score
				scoringMatrix[i][j] = scores[maxIndex];				
			}
		}
		
		return sources;
	}
	
	public static int score(char[] a, char[]b, int i, int j, int[][] scoringMatrix, int match, int misMatch, int gapPenalty){
		
		// Initialise an array to score the scores for each type of movement through the scoring matrix
		int[] scores = new int[4];
		
		// Calculate the diagonal score
		scores[0] = scoringMatrix[i-1][j-1] + compareNucleotides(a[i-1], b[j-1], match, misMatch);
		
		// Calculate the horizontal score
		scores[1] = scoringMatrix[i][j-1] - gapPenalty;
		
		// Calculate the vertical score
		scores[2] = scoringMatrix[i-1][j] - gapPenalty;
		
		// Choose the max score from diagonal, horizontal, and vertical - note 0 also included
		return ArrayMethods.max(scores);
	}
	
	public static int compareNucleotides(char a, char b, int match, int misMatch){
		
		int output = misMatch;
		if(a == b){
			output = match;
		}
		
		return output;
	}
	
	public static void printScoringMatrix(int[][] scoringMatrix, char[] a, char[] b){
		
		System.out.println("    " + ArrayMethods.toString(b, " "));
		for(int i = 0; i < scoringMatrix.length; i++){
			
			if(i != 0){
				System.out.print(a[i-1] + " ");
			}else{
				System.out.print("  ");
			}
			for(int j = 0; j < scoringMatrix[0].length; j++){
				
				System.out.print(scoringMatrix[i][j] + " ");
			}
			System.out.println();
		}		
	}

	public static void printSourceMatrix(int[][][] sourceMatrix, char[] a, char[] b){
		
		System.out.println("    " + ArrayMethods.toString(b, " "));
		for(int i = 0; i < sourceMatrix.length; i++){
			
			if(i != 0){
				System.out.print(a[i-1] + " ");
			}else{
				System.out.print("  ");
			}
			for(int j = 0; j < sourceMatrix[0].length; j++){
				
				// Check direction - either diagonal up or down
				// {{i-1, j-1}, {i, j-1}, {i-1, j}}
				if(i == 0 || j == 0 || (sourceMatrix[i][j][0] == 0 && sourceMatrix[i][j][1] == 0)) {
					System.out.print("  ");
				}else if(sourceMatrix[i][j][0] == i-1 && sourceMatrix[i][j][1] == j-1) {
					System.out.print("\\ ");
				}else if(sourceMatrix[i][j][0] == i-1 && sourceMatrix[i][j][1] == j) {
					System.out.print("^ ");
				}else if(sourceMatrix[i][j][0] == i && sourceMatrix[i][j][1] == j-1) {
					System.out.print("< ");
				}else {
					System.out.print("  ");
				}
			}
			System.out.println();
		}		
	}
}
