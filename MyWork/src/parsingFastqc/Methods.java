package parsingFastqc;

import java.io.IOException;

import methods.CalendarMethods;

public class Methods {

	public static void main(String[] args) throws IOException{
		
		// Get the current date
		String date = CalendarMethods.getCurrentDate("dd-MM-yy");
		
		// Set the path
		String path = "/home/josephcrispell/Desktop/Testing/";
		
		// Note the name of the fastqc output directories (paired end)
		String forwardFolder = "ERS519053_1_fastqc";
		String reverseFolder = "ERS519053_2_fastqc";
		
		// Read in the data files
		Data forward = new Data(path + forwardFolder + "/fastqc_data.txt");
		//Data reverse = new Data(path + reverseFolder + "/fastqc_data.txt");
		
	}
	
}
