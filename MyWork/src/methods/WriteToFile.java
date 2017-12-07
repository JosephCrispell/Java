package methods;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class WriteToFile {

	public static BufferedWriter openFile(String fileName, boolean append) throws IOException{
		
		// Find the File
		File file = new File(fileName);
		
		// Check exists
		if(!file.exists()){
			file.createNewFile();
		}
		
		// Open the File for Writing
		FileWriter writer = new FileWriter(file.getAbsoluteFile(), append);
		
		return new BufferedWriter(writer);
	}

	public static void write(BufferedWriter bWriter, String string) throws IOException{
		bWriter.write(string);
	}
	public static void writeLn(BufferedWriter bWriter, String string) throws IOException{
		bWriter.write(string + "\n");
	}
	public static void write(BufferedWriter bWriter, int number) throws IOException{
		bWriter.write(Integer.toString(number));
	}
	public static void writeLn(BufferedWriter bWriter, int number) throws IOException{
		bWriter.write(Integer.toString(number) + "\n");
	}
	public static void write(BufferedWriter bWriter, double number) throws IOException{
		bWriter.write(String.valueOf(number));
	}
	public static void writeLn(BufferedWriter bWriter, double number) throws IOException{
		bWriter.write(String.valueOf(number) + "\n");
	}

	public static void close(BufferedWriter bWriter) throws IOException{
		bWriter.close();
	}

	public static String[] findFilesInDirectory(String directory, String regex){

		// Method to List files in a Directory which match a pattern
		
		// Open Directory
		File folder = new File(directory);
		File[] files = folder.listFiles();
		
		// Empty Array to Store File Names
		String[] fileNames = new String[0];
		String file;
		
		// Investigate the files in the Current Directory
		for(int index = 0; index < files.length; index++){

			// Get File Name
			file = files[index].getName();
			
			// Check if it matches pattern "(.*).txt"
			if(file.matches(regex)){

				fileNames = ArrayMethods.append(fileNames, file);
			}
		}
		
		return fileNames;
	}
}
