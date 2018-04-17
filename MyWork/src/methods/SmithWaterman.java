package methods;

import java.io.IOException;

public class SmithWaterman {

	/**
	 * An implementation of the Smith-Waterman local alignment algorithm
	 * - Took all instructions from: https://en.wikipedia.org/wiki/Smith%E2%80%93Waterman_algorithm#Example
	 */
	
	public static void main(String[] args) throws IOException{
		
		
		
		char[] a = "GTCGTCGCGTCGCTGCTCGCGGAGAGAGTCGATAGATGCGATAGCTAGCGAGCAGCGAGCAGCAGCTACGATCAGCGACTAGCATCGAGCAGCGATCAGCTAGCATCAGCTACGATCAGCTAGCATCGA".toCharArray();
		char[] b = "GTCGTCGCGTCGCTGCTCGCGGAGATCGATAGATGCGATAGCTAGCGAGCAGCGAGCAGCAGCTACGATCAGCGACTGGCATCGAGCAGCGATCAGCTAGCATCAGCTACGATCTTGCTAGCATCGA".toCharArray();
		
		int match = 3;
		int misMatch = -3;
		int gapPenalty = 1;
		
		int[][] scoringMatrix = new int[a.length + 1][b.length + 1];

		int[][][] scoreSources = fillInScoringMatrix(scoringMatrix, a, b, match, misMatch, gapPenalty);
		
		constructAlignment(scoringMatrix, a, b, scoreSources);
	}
	
	public static void constructAlignment(int[][] scoringMatrix, char[] a, char[] b, int[][][] sources){
		
		// Find the max indices
		int[][] maxIndices = findIndicesOfMaximums(scoringMatrix);
		
		// Reconstruct the alignment associated with each maximum found
		for(int pos = 0; pos < maxIndices[0].length; pos++){
			
			int i = maxIndices[0][pos];
			int j = maxIndices[1][pos];
			
			System.out.println("Alignment with maximum alignment score of " + scoringMatrix[i][j]);
			
			StringBuffer alignedA = new StringBuffer();
			StringBuffer alignedB = new StringBuffer();
			addNextAlignedPair(scoringMatrix, sources, i, j, a, b, alignedA, alignedB);
			
			System.out.println(alignedA.reverse().toString());
			System.out.println(alignedB.reverse().toString());
		}
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
	
	public static int[][][] fillInScoringMatrix(int[][] scoringMatrix, char[] a, char[] b, int match, int misMatch, int gapPenalty){
		
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
				scores[1] = scoringMatrix[i][j-1] - gapPenalty;
				
				// Calculate the vertical score
				scores[2] = scoringMatrix[i-1][j] - gapPenalty;
				
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

}
