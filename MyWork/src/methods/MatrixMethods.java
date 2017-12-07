package methods;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Hashtable;

import woodchesterCattle.Location;

public class MatrixMethods {

	public static void main(String[] args) {
		// Method Testing Area
		int[][] matrix = {{ 1, 2, 3, 4, 5, 6, 7},
						  { 8, 9,10,11,12,13,14},
						  {15,16,17,18,19,20,21},
						  {22,23,24,25,26,27,28}};
		
		int[] rows = {2,3};
		int[] cols = {4,7,9};

		
	}

	
	// Methods
	public static int[][] addEmptyRowsAndColumns(int[][] squareMatrix, int n){
		
		int[][] newMatrix = new int[squareMatrix.length + n][squareMatrix[0].length + n];
		
		// Put current values into matrix - note that we leave the rest empty
		for(int i = 0; i < squareMatrix.length; i++){
			for(int j = 0; j < squareMatrix[0].length; j++){

				newMatrix[i][j] = squareMatrix[i][j];
			}
		}
		
		return newMatrix;
	}
	
	public static double[][] scaleToOne(double[][] matrix){
		
		double total = sum(matrix);
		
		double[][] newMatrix = new double[matrix.length][matrix[0].length];
		
		for( int i = 0; i < matrix.length; i++){
			for( int j = 0; j < matrix[0].length; j++){
				
				newMatrix[i][j] = matrix[i][j] / total;			
			}
		}
		
		return newMatrix;
	}
	
	public static int[][] fill(int[][] matrix, int value){
		
		for(int i = 0; i < matrix.length; i++){
			
			for(int j = 0; j < matrix[0].length; j++){
				
				matrix[i][j] = value;
			}
		}
		
		return matrix;
	}

	public static int[][] copy(int[][] matrix){
		
		int[][] copy = new int[matrix.length][matrix[0].length];
		
		for(int i = 0; i < matrix.length; i++){
			
			for(int j = 0; j < matrix[0].length; j++){
				
				copy[i][j] = matrix[i][j];
			}
		}
		
		return copy;
	}
	public static double[][] copy(double[][] matrix){
		
		double[][] copy = new double[matrix.length][matrix[0].length];
		
		for(int i = 0; i < matrix.length; i++){
			
			for(int j = 0; j < matrix[0].length; j++){
				
				copy[i][j] = matrix[i][j];
			}
		}
		
		return copy;
	}
	public static double[][] copyConvert2Double(int[][] matrix){
		
		double[][] copy = new double[matrix.length][matrix[0].length];
		
		for(int i = 0; i < matrix.length; i++){
			
			for(int j = 0; j < matrix[0].length; j++){
				
				copy[i][j] = matrix[i][j];
			}
		}
		
		return copy;
	}
	
	public static void print(int[][] matrix){
		
		for(int i = 0; i < matrix.length; i++){
			
			for(int j = 0; j < matrix[0].length; j++){
				
				System.out.print(matrix[i][j] + "\t");
			}
			
			System.out.println("\n\n");
		}
		
	}
	public static void print(long[][] matrix){
		
		for(int i = 0; i < matrix.length; i++){
			
			for(int j = 0; j < matrix[0].length; j++){
				
				System.out.print(matrix[i][j] + "\t");
			}
			
			System.out.println("\n\n");
		}
		
	}
	public static void print(double[][] matrix, double decimalPlaces){
		
		double value = Math.pow(10, decimalPlaces);
		
		for(int i = 0; i < matrix.length; i++){
			
			for(int j = 0; j < matrix[0].length; j++){
				
				System.out.print(Math.round(matrix[i][j] * value)/value + "\t");
			}
			
			System.out.println("\n\n");
		}
		
	}
	public static void print(double[][] matrix, String fileName, String sep) throws IOException{
		
		// Open and Wipe File
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Build an array to store each file line
		String line = "";
				
		// Store the Matrix in a String
		for(int i = 0; i < matrix.length; i++){
			line = "";
			for(int j = 0; j < matrix[0].length; j++){
				line = line + matrix[i][j];
				
				if(j != matrix[0].length - 1){
					line = line + sep;
				}
			}
			
			WriteToFile.writeLn(bWriter, line);
		}
		
		WriteToFile.close(bWriter);
	}
	public static void print(int[][] matrix, String fileName, String sep) throws IOException{
		
		// Open and Wipe File
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Build an array to store each file line
		String line = "";
				
		// Store the Matrix in a String
		for(int i = 0; i < matrix.length; i++){
			line = "";
			for(int j = 0; j < matrix[0].length; j++){
				line = line + matrix[i][j];
				
				if(j != matrix[0].length - 1){
					line = line + sep;
				}
			}
			
			WriteToFile.writeLn(bWriter, line);
		}
		
		WriteToFile.close(bWriter);
	}	
	public static double[] flatten(double[][] matrix){
		double[] array = new double[matrix.length * matrix[0].length];
		
		int pos = -1;
		for(int i = 0; i < matrix.length; i++){
			for(int j = 0; j < matrix[0].length; j++){
				pos++;
				array[pos] = matrix[i][j];
			}
		}
		
		return array;
	}
	
	public static int[][] readInt(String fileName, String sep, int size) throws IOException{
		
		// Open the input file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise a matrix
		int[][] matrix = new int[size][size];
		
		// Initialise variables necessary for parsing the file
		String line = null;
		int lineNo = 0;
		String[] cols;
				
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			lineNo++;
			
			cols = line.split(sep);
			matrix[lineNo - 1] = ArrayMethods.convertToInteger(cols);
		}
		
		// Close the current movements file
		input.close();
		reader.close();
		
		return matrix;
	}
	public static double[][] readDouble(String fileName, String sep, int size) throws IOException{
		
		// Open the input file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise a matrix
		double[][] matrix = new double[size][size];
		
		// Initialise variables necessary for parsing the file
		String line = null;
		int lineNo = 0;
		String[] cols;
				
		// Begin reading the file
		while(( line = reader.readLine()) != null){
			lineNo++;
			
			cols = line.split(sep);
			matrix[lineNo - 1] = ArrayMethods.convert2Double(cols);
		}
		
		// Close the current movements file
		input.close();
		reader.close();
		
		return matrix;
	}
	
 	public static int sum(int[][] matrix){
		
		int total = 0;
		
		for(int i = 0; i < matrix.length; i++){
			for(int j = 0; j < matrix[0].length; j++){
				
				total += matrix[i][j];
			}
		}
		
		return total;
	}
	
	public static double sum(double[][] matrix){
		
		double total = 0;
		
		for(int i = 0; i < matrix.length; i++){
			for(int j = 0; j < matrix[0].length; j++){
				
				total += matrix[i][j];
			}
		}
		
		return total;
	}
	
	public static double[][] addRow(double[][] matrix, double[] row){
		
		double[][] newMatrix = new double[matrix.length + 1][row.length];
		
		for(int i = 0; i < matrix.length; i++){
			newMatrix[i] = matrix[i];
		}
		newMatrix[matrix.length] = row;
		
		return newMatrix;		
	}
	public static long[][] addRow(long[][] matrix, long[] row){
		
		long[][] newMatrix = new long[matrix.length + 1][row.length];
		
		for(int i = 0; i < matrix.length; i++){
			newMatrix[i] = matrix[i];
		}
		newMatrix[matrix.length] = row;
		
		return newMatrix;		
	}
	public static int[][] addRow(int[][] matrix, int[] row){
		
		int[][] newMatrix = new int[matrix.length + 1][row.length];
		
		for(int i = 0; i < matrix.length; i++){
			newMatrix[i] = matrix[i];
			
			System.out.println(ArrayMethods.toString(matrix[i], "\t"));
		}
		newMatrix[matrix.length] = row;
		
		return newMatrix;		
	}

	public static int[][] removeEmptyRows(int[][] matrix, int lastRow){
		
		int[][] part = new int[lastRow + 1][matrix[0].length];
		
		for(int i = 0; i <= lastRow; i++){
			
			part[i] = matrix[i];
		}
		
		return part;		
	}
	
	public static double[][] addCol(double[][] matrix, double[] colValues){
		
		double[][] newMatrix = new double[matrix.length][matrix[0].length + 1];
		
		for(int row = 0; row < matrix.length; row++){
			
			for(int col = 0; col < matrix[0].length; col++){
				
				newMatrix[row][col] = matrix[row][col];
			}
			
			newMatrix[row][matrix[0].length] = colValues[row];
		}
		
		return newMatrix;
		
	}
	
	public static double[] selectColumn(double[][] matrix, int column){
		
		double[] colValues = new double[matrix.length];
		
		for(int row = 0; row < matrix.length; row++){
			
			colValues[row] = matrix[row][column];
		}
		
		return colValues;
	}

	public static double[][] timesBy(double[][] matrix, double value){
		
		double[][] m = new double[matrix.length][matrix[0].length];
		
		for(int i = 0; i < matrix.length; i++){
			for(int j = 0; j < matrix[0].length; j++){
				m[i][j] = matrix[i][j] * value;
			}
		}
		
		return m;
	}
	
}
