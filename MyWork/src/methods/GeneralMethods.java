package methods;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;


import org.uncommons.maths.random.MersenneTwisterRNG;

public class GeneralMethods {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
	}
	
	public static String[] getAllFilesInDirectory(String directory, String extension){
		File folder = new File(directory);
		File[] listOfFiles = folder.listFiles();
		
		String[] files = new String[listOfFiles.length];
		String fileName;
		
		int index = -1;
		for(int i = 0; i < listOfFiles.length; i++){
			
			fileName = listOfFiles[i].getName();
			
			if(extension != null && fileName.matches("(.*)" + extension) == true){
				index++;
				files[index] = fileName;
			}			
		}
		
		return ArrayMethods.subset(files, 0, index);
	}

	public static String removeDelimiter(String value, String sep){
		
		return ArrayMethods.toString(value.split(sep), "");
	}
	
	public static String replaceDelimiter(String value, String currentSep, String newSep){
		
		return ArrayMethods.toString(value.split(currentSep, -1), newSep);
	}
	
	public static Random startRandomNumberGenerator(int seed){
		byte[] seedAsBytes = ByteBuffer.allocate(16).putInt(seed).array();
		int x = java.nio.ByteBuffer.wrap(seedAsBytes).getInt(); // Get int back from byte[]
		Random random = new MersenneTwisterRNG(seedAsBytes);
		
		return random;
	}
	
	public static void makeDirectory(String directoryPath){
		File dir = new File(directoryPath);
		
		boolean success = dir.mkdir();
		
		if(success == false){
			System.out.println("Failed to make the directory: " + directoryPath);
		}
	}

	public static double round(double x, double decimalPlaces){
		
		double value = Math.pow(10, decimalPlaces);
		
		return Math.round(x * value)/value;
	}

	public static double getRandomDoubleInRange(double min, double max, Random random){
		
		/**
		 * Method taken directly from StackOverflow:
		 * http://stackoverflow.com/questions/3680637/generate-a-random-double-in-a-range
		 */
		
		return min + (random.nextDouble() * (max - min));
	}

	public static double calculateEuclideanDistance(double[] a, double[] b){
			
		return Math.sqrt( Math.pow((a[0] - b[0]), 2.0) + Math.pow((a[1] - b[1]), 2.0) );
		
	}
	public static double calculateEuclideanDistance(int[] a, int[] b){
		
		return Math.sqrt( Math.pow(((double) a[0] - (double) b[0]), 2.0) + Math.pow(((double) a[1] - (double) b[1]), 2.0) );
		
	}
}
