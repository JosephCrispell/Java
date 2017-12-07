package hungarianAlgorithm;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Set;

import methods.ArrayMethods;
import methods.HashtableMethods;
import methods.MatrixMethods;

public class HungarianAlgorithm {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

//		double[][] matrix = { 	{ 	82,	83,	69,	92 	},
//				 		   		{	77, 37,	49,	92 	},
//				 		   		{	11, 69,	5,	86	},
//				 		   		{	8,	9,	98,	23	}	};
		
//		double[][] matrix = { 	{ 	10,	19,	8,	15 	},
// 		   						{	10, 18,	7,	17 	},
// 		   						{	13, 16,	9,	14	},
// 		   						{	12,	19,	8,	18	},
// 		   						{	14,	17,	10,	19	}	};
		
//		double[][] matrix = { 	{ 	90,		75,		75,	80 	},
// 		   						{	35, 	85,		55,	65 	},
// 		   						{	125, 	95,		90,	105	},
// 		   						{	45,		110,	95,	115	}	};
		
//		double[][] matrix = { 	{ 	250,	400,	350	},
//								{	400,	600,	350	},
//								{	200,	400,	250	}	};
		
		double[][] matrix = { 	{ 	14,	5,	8,	7 	},
								{	2,	12,	6,	5 	},
								{	7,	8,	3,	9	},
								{	2,	4,	6,	10	}	};
		
		
		int[] assignedJobs = implementHungarianMatchingAlgorithm(matrix);
		
		System.out.println("\n" + Arrays.toString(assignedJobs));
	}
	
	public static int[][] markColumns(int row, int[] markedRows, int[] markedCols, double[][] matrix, int[][] assignedZeros){
		
		int[][] markedRowsAndCols = new int[2][0];
		markedRowsAndCols[0] = markedRows;
		markedRowsAndCols[1] = markedCols;
		
		// Mark all columns with zeros in the unassigned rows
		for(int col = 0; col < matrix[0].length; col++){
						
			if(matrix[row][col] == 0 && markedCols[col] == 0){
							
				markedCols[col] = 1;
							
				// Mark all rows with assigned zeros in marked column
				for(int i = 0; i < matrix.length; i++){
					if(assignedZeros[i][col] == 1){
									
						markedRows[i] = 1;
						
						markedRowsAndCols = markColumns(i, markedRows, markedCols, matrix, assignedZeros);
						markedRows = markedRowsAndCols[0];
						markedCols = markedRowsAndCols[1];									
					}
				}
			}
		}
		
		markedRowsAndCols[0] = markedRows;
		markedRowsAndCols[1] = markedCols;
		
		return markedRowsAndCols;		
	}
	
	public static int pickWorkerToAssign(int[] workers, Hashtable<Integer, Integer> assignedWorkers, double[][] matrix, int job,
			Hashtable<Integer, int[]> possibleAssignments){
		
		int[] unAssigned = new int[99];
		int usedPos = -1;
		
		for(int worker : workers){
			
			if(assignedWorkers.get(worker) == null){
				usedPos++;
				unAssigned[usedPos] = worker;
			}			
		}
		
		unAssigned = ArrayMethods.subset(unAssigned, 0, usedPos);
		
		int pickedWorker = unAssigned[0];
		
		if(unAssigned.length > 1){
			
			for(int worker : unAssigned){
				
				if(matrix[worker][job] < matrix[pickedWorker][job] && possibleAssignments.get(pickedWorker).length > 1){
					pickedWorker = worker;
				}
			}
		}
		
		return pickedWorker;
		
	}
	
	public static int[] sortPossibleAssignments(Hashtable<Integer, int[]> possibleAssignments){
		
		int[] keys = HashtableMethods.getKeysInt(possibleAssignments);
		
		/**
		 * This Method Uses the Bubble Sort Algorithm
		 * 		Described here: http://en.wikipedia.org/wiki/Bubble_sort
		 * 
		 * 	For each element, compare it to the next element. If it is larger than the next element, swap the elements.
		 * 	Do this for each element of the list (except the last). Continue to iterate through the list elements and
		 *  make swaps until no swaps can be made.
		 */
		
		int swappedHappened = 1;
		while(swappedHappened == 1){ // Continue to compare the List elements until no swaps are made
		
			int swapped = 0;
			for(int index = 0; index < keys.length - 1; index++){
				
				// Compare Current Element to Next Element
				if(possibleAssignments.get(keys[index]).length > possibleAssignments.get(keys[index + 1]).length){
					
					// Swap Current Element is Larger
					int a = keys[index];
					int b = keys[index + 1];
					
					keys[index] = b;
					keys[index + 1] = a;
					
					// Record that a Swap occurred
					swapped++;
				}
			}
			
			// Check if any swaps happened during the last iteration - if none then finished
			if(swapped == 0){
				swappedHappened = 0;
			}
		}
		
		return keys;
	}

	public static int[] implementHungarianMatchingAlgorithm(double[][] originalMatrix){
		
		/**
		 * The Hungarian Matching Algorithm - Harold Kuhn (1955)
		 * 
		 * 	The assignment problem deals with assigning machines to task, workers to jobs, soccer players to positions, and so on. The goal is to 
		 * 	determine the optimum assignment that, for example, minimises the total cost or maximises the team effectiveness.
		 * 	The Hungarian algorithm is an easy to understand and easy to use algorithm that solves the assignment problem.
		 * 
		 *  Example - Four Jobs need to given to Four Workers. Taken from http://www.hungarianalgorithm.com/examplehungarianalgorithm.php
		 *  The matrix for the cost of assigning a certain worker to a certain job:
		 *  	Workers\Jobs	A	B	C	D
		 *  				I	82	83	69	92
		 *  				II	77	37	49	92
		 *  				III	11	69	5	86
		 *  				IV	8	9	98	23
		 *  
		 *  STEP 1: Find the Lowest value in each row and subtract it from all value in its row
		 *  	Workers\Jobs	A	B	C	D
		 *  				I	13	14	0	23
		 *  				II	40	0	12	55
		 *  				III	6	64	0	81
		 *  				IV	0	1	90	15
		 *  
		 *  STEP 2: Repeat STEP 1 but for Columns
		 *  	Workers\Jobs	A	B	C	D
		 *  				I	13	14	0	8
		 *  				II	40	0	12	40
		 *  				III	6	64	0	66
		 *  				IV	0	1	90	0
		 *  
		 *  STEP 3: Cover All ZEROS with the minimum no of rows/columns
		 *  	
		 *								*  
		 *  	Workers\Jobs	A	B	C	D
		 *  				I	13	14	0	8
		 *  				II	40	0	12	40	*
		 *  				III	6	64	0	66
		 *  				IV	0	1	90	0	*
		 *  
		 *  STEP 4: Find the Smallest Uncovered Value and
		 *  	- Subtract that value from all uncovered values
		 *  	- Add that value to all values covered twice
		 *  
		 *  	Workers\Jobs	A	B	C	D
		 *  				I	7	8	0	2	*			I: 		C
		 *  				II	40	0	18	40	*			II:		B
		 *  				III	6	58	0	60	*			III:	A
		 *  				IV	0	1	96	0	*			IV:		D
		 *  
		 *  
		 *  Algorithm stops when the no of rows/columns needed to be covered is >= to the number of jobs
		 *  	
		 */
		
		double[][] matrix = MatrixMethods.copy(originalMatrix);
		
		// STEP 1: Find the Lowest value in each row and subtract it from all value in its row
		matrix = subtractMinRowValue(matrix);
		
		// STEP 2: Repeat STEP 1 but for Columns
		matrix = subtractMinColValue(matrix);
		
		// STEP 3: Cover All ZEROS with the minimum no of rows/columns
		CoveredRowsAndCols rowsAndCols2Cover = findRowsAndCols2CoverAllZeros(matrix);
		double min;
		
		// Steps 3 & 4 are repeated in series until the number of rows/columns covered is >= number of jobs
		while(rowsAndCols2Cover.getRows().length + rowsAndCols2Cover.getCols().length < matrix[0].length){
			
			// STEP 4: Create more Zeros
			min = findMinUncoveredNumber(matrix, rowsAndCols2Cover);
			matrix = applyUncoveredMin2Matrix(matrix, rowsAndCols2Cover, min);		
			
			// STEP 3: Cover All ZEROS with the minimum no of rows/columns
			rowsAndCols2Cover = findRowsAndCols2CoverAllZeros(matrix);
			
		}
		
		// FINISH: Return the Optimal Assignment
		return findAssignment(matrix, originalMatrix);
		
	}
	
	public static Hashtable<Integer, int[]> findPossibleWorkers4Assignments(double[][] matrix){
		
		Hashtable<Integer, int[]> possibleWorkers = new Hashtable<Integer, int[]>();
		
		int[] workers = new int[matrix.length];
		int posUsed = -1;
		
		for(int col = 0; col < matrix[0].length; col++){
			
			for(int row = 0; row < matrix.length; row++){
				
				if(matrix[row][col] == 0){
					posUsed++;
					workers[posUsed] = row;
				}
			}
			
			possibleWorkers.put(col, ArrayMethods.subset(workers, 0, posUsed));
			
			workers = new int[matrix.length];
			posUsed = -1;
		}
		
		return possibleWorkers;
	}
	
	public static Hashtable<Integer, int[]> findPossibleAssignments4Workers(double[][] matrix){
		
		Hashtable<Integer, int[]> possibleAssignments = new Hashtable<Integer, int[]>();
		
		int[] jobs = new int[matrix.length];
		int posUsed = -1;
		
		for(int row = 0; row < matrix.length; row++){
			
			for(int col = 0; col < matrix[0].length; col++){
				
				if(matrix[row][col] == 0){
					posUsed++;
					jobs[posUsed] = col;
				}
			}
			
			possibleAssignments.put(row, ArrayMethods.subset(jobs, 0, posUsed));
			
			jobs = new int[matrix.length];
			posUsed = -1;
		}
		
		return possibleAssignments;		
	}
	
	public static int[] findAssignment(double[][] matrix, double[][] originalMatrix){
		
		Hashtable<Integer, int[]> possibleworkers = findPossibleWorkers4Assignments(matrix);
		Hashtable<Integer, int[]> possibleAssignments = findPossibleAssignments4Workers(matrix);
		int[] keys = HashtableMethods.getKeysInt(possibleworkers);
		
		keys = sortPossibleAssignments(possibleworkers);
		
		int[] assignedJobs = ArrayMethods.repeat(-1, matrix[0].length);
		Hashtable<Integer, Integer> assignedWorkers = new Hashtable<Integer, Integer>();
		
		for(int key : keys){
			
			if(possibleworkers.get(key).length == 1){
				
				assignedJobs[key] = possibleworkers.get(key)[0];
				
				assignedWorkers.put(possibleworkers.get(key)[0], 1);
				
			}else{
				
				assignedJobs[key] = pickWorkerToAssign(possibleworkers.get(key), assignedWorkers, originalMatrix, key, possibleAssignments);
				
				assignedWorkers.put(assignedJobs[key], 1);
				
			}
		}
		
		return assignedJobs;
	}
	
	public static double[][] subtractMinColValue(double[][] matrix){
		
		double[][] newMatrix = new double[matrix.length][matrix[0].length];
		double min;
		
		for(int col = 0; col < matrix[0].length; col++){
			
			// Find the column minimum
			min = ArrayMethods.min(MatrixMethods.selectColumn(matrix, col));
			
			for(int row = 0; row < matrix.length; row++){
				
				newMatrix[row][col] = matrix[row][col] - min;
			}
		}
		
		return newMatrix;
	}
	
	public static double[][] subtractMinRowValue(double[][] matrix){
		
		double[][] newMatrix = new double[matrix.length][matrix[0].length];
		double min = -99;
		
		for(int row = 0; row < matrix.length; row++){
			
			// Find the min of the current row
			min = ArrayMethods.min(matrix[row]);
			
			for(int col = 0; col < matrix[0].length; col++){
				
				// Subtract the min for the row away from the current value
				newMatrix[row][col] = matrix[row][col] - min;
				
			}	
		}
		
		return newMatrix;		
	}
	
	public static CoveredRowsAndCols findRowsAndCols2CoverAllZeros(double[][] matrix){
		
		/**
		 * Find the least number of rows/columns to cover in order to cover all zeros. Taken from: http://en.wikipedia.org/wiki/Hungarian_algorithm 
		 * 
		 * Assigning Zeros in Matrix Rules:
		 * 		Rows with single zeros - Assign zero and cross out any zeros in the same column
		 * 		Rows with multiple zeros - Assign one (that isn't crossed out), cross out others and those in column of assigned zero
		 * 		Zeros that are crossed out are unassigned
		 * 
		 * Marking Rows and columns Rules:
		 * 		Mark all rows with no assigned zeros
		 * 		Mark all columns with zeros in newly marked row(s)
		 * 		Mark all rows with assigned zeros in newly marked columns
		 * 		Mark all columns with zeros in newly marked row(s)
		 * 
		 * Selecting Rows and columns to cover:
		 * 		Select all marked columns and unmarked rows
		 */
		
		int[] markedRows = new int[matrix.length];
		int[] markedCols = new int[matrix[0].length];
		
		int[] rowsWithAssignedZeros = new int[matrix.length];
		
		int[][] assignedZeros = new int[matrix.length][matrix[0].length];
		
			
		for(int row = 0; row < matrix.length; row++){
			
			for(int col = 0; col < matrix[0].length; col++){
				
				if(matrix[row][col] == 0 && markedRows[row] == 0 && markedCols[col] == 0){
					
					assignedZeros[row][col] = 1;
					
					rowsWithAssignedZeros[row] = 1;
					markedRows[row] = 1;
					markedCols[col] = 1;
					
				}				
			}			
		}
		
		markedRows = new int[matrix.length];
		markedCols = new int[matrix[0].length];
		
		int[][] markedRowsAndCols = new int[2][0];
		markedRowsAndCols[0] = markedRows;
		markedRowsAndCols[1] = markedCols;
		
		for(int row = 0; row < rowsWithAssignedZeros.length; row++){
			
			// Skip assigned rows
			if(rowsWithAssignedZeros[row] == 1){
				continue;
			}
			
			// Mark all unassigned rows
			markedRows[row] = 1;
			
			// Mark all Columns with zeros in this unassigned row
			markedRowsAndCols = markColumns(row, markedRows, markedCols, matrix, assignedZeros);
			
			markedRows = markedRowsAndCols[0];
			markedCols = markedRowsAndCols[1];
			
		}
		
		markedRowsAndCols[0] = markedRows;
		markedRowsAndCols[1] = markedCols;
		
		Hashtable<Integer, Integer> rows2Cover = new Hashtable<Integer, Integer>();
		Hashtable<Integer, Integer> cols2Cover = new Hashtable<Integer, Integer>();
		
		// Cover all unmarked rows
		for(int row = 0; row < matrix.length; row++){
			if(markedRows[row] == 0){
				rows2Cover.put(row, 1);
			}
		}
		
		// Cover all marked columns
		for(int col = 0; col < matrix[0].length; col++){
			if(markedCols[col] == 1){
				cols2Cover.put(col, 1);
			}
		}
		
		CoveredRowsAndCols rowsAndCols2Cover = new CoveredRowsAndCols(rows2Cover, cols2Cover);
		
		return rowsAndCols2Cover;
	}
	
	public static double[][] applyUncoveredMin2Matrix(double[][] matrix, CoveredRowsAndCols rowsAndCols2Cover, double min){
		
		// Extract the Rows and Columns being Covered
		Hashtable<Integer, Integer> rows2Cover = rowsAndCols2Cover.getCoveredRows();
		Hashtable<Integer, Integer> cols2Cover = rowsAndCols2Cover.getCoveredCols();
		
		double[][] newMatrix = new double[matrix.length][matrix[0].length];
		
		for(int i = 0; i < matrix.length; i++){
			
			for(int j = 0; j < matrix[0].length; j++){
				
				// Take the Uncovered Min value away from all uncovered elements
				if(rows2Cover.get(i) == null && cols2Cover.get(j) == null){
					newMatrix[i][j] = matrix[i][j] - min;
				
				// Add the Uncovered Min value to those elements covered twice
				}else if(rows2Cover.get(i) != null && cols2Cover.get(j) != null){
					newMatrix[i][j] = matrix[i][j] + min;
					
				// Copy all other elements
				}else{
					
					newMatrix[i][j] = matrix[i][j];					
				}
			}
		}
		
		return newMatrix;
	}
	
	public static double findMinUncoveredNumber(double[][] matrix, CoveredRowsAndCols rowsAndCols2Cover){
		
		// Extract the Rows and Columns being Covered
		Hashtable<Integer, Integer> rows2Cover = rowsAndCols2Cover.getCoveredRows();
		Hashtable<Integer, Integer> cols2Cover = rowsAndCols2Cover.getCoveredCols();
		
		double value = 99999999;
		
		for(int i = 0; i < matrix.length; i++){
			
			// Skip Covered Rows
			if(rows2Cover.get(i) != null){
				continue;
			}
			
			for(int j = 0; j < matrix[0].length; j++){
				
				// Skip Covered Columns
				if(cols2Cover.get(j) != null){
					continue;
				}
				
				if(matrix[i][j] < value){
					value = matrix[i][j];
				}
			}
		}
		
		return value;
	}

}
