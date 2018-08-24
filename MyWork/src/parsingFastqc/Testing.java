package parsingFastqc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.uncommons.maths.random.MersenneTwisterRNG;

import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.WriteToFile;

public class Testing {

	public static void main(String[] args) throws IOException{
		
		// Check if help requested
		if(args.length == 0 || args[0].equals("-help") || args[0].equals("") || args[0].equals("-h") || args[0].equals("help") || 
				args[0].equals("--help")){
			System.out.println("A tool to summarise the output created by FASTQC");
			System.out.println("\nCommand Line Structure:");
			System.out.println("\tjava -jar SummariseFASTQC_DATE.jar fastqcDirectory qualityThreshold deviationThreshold minLength\n");
			System.out.println("\t\tfastqcDirectory\tDirectory containing all the unzipped FASTQC output directories");
			System.out.println("\t\tqualityThreshold\tQuality threshold for identify poor reads and bases within them");
			System.out.println("\t\tdeviationThreshold\tThreshold (%) that G-C or A-T are allowed to deviate from one another. Used to calculate left and right trimming");
			System.out.println("\t\tminLength\tThe minimum length of read expected");
			System.out.println("\nNotes:");
			System.out.println("For LEFT and RIGHT trimming 10 consecutive bases are required to have < deviationThreshold for search to stop.");

			System.exit(0);
		}

		
		// Build a random number generator
		Random random = new MersenneTwisterRNG();
		
		// Get the current date
		String date = CalendarMethods.getCurrentDate("dd-MM-yy");
		
		// Set the path
//		String path = "/home/josephcrispell/Desktop/Research/Granjean2017ConvergentEvolution/FASTQs/FASTQC/";
		String path = args[0];
		
		// Check if path ends with "/"
		if(path.matches("(.*)/") == false) {
			path += "/";
		}
		
		// Get the FASTQC directory
//		String fastqcDirectory = args[0];
		
		// Note parameters
		int qualityThreshold = Integer.parseInt(args[1]); // Quality threshold for reads/bases
		double deviationThreshold = Double.parseDouble(args[2]); // How many % can G deviate from C at each position?
		int nSitesThreshold = 10; // How many sites must be below deviation threshold before stop search?
//		double expected = 65; // Expected GC content
//		int diffFromExpectedThreshold = 5; // How different can observed GC be from expected before flagged? Verbose only
//		int nSearchesForPeaks = 100; // NUmber of hill climbs to conduct to identify peaks
//		int mergeDistanceForPeaks = 20; // Number of indices identified peaks must be away from each other to be defined as separate peak
//		double propSearchesPeakFoundInThreshold = 0.2; // Proportion searches peak must be found in to be recognised as peak
		int minLength = Integer.parseInt(args[3]); // Minimum length for reads
//		double meanPerSiteAdapterContentThreshold = 0.05; // Mean proportion sites matching adapter threshold
//		int nConsecutiveBaseThreshold = 4; // Number of consecutive bases allowed in kmer before flagged
		
		// Get the FASTQC directories
		ArrayList<String> fastqcDirectories = getFASTQCDirectories(path);
		
		// Open an output file
		BufferedWriter bWriter = WriteToFile.openFile(path + "Fastqc_summary_" + date + ".txt", false);
		bWriter.write("FileName\tEncoding\tNumberReads\tReadLength\tGC\tPropBelowQualityThreshold\tLeftTrim\tRightTrim\tPropBelowLengthThreshold\t" + 
				"BasicStatsFlag\tPerBaseSeqQualFlag\tPerTileSeqQualFlag\tPerBaseSeqContentFlag\tPerSeqGCContentFlag\tPerBaseNContentFlag\t" + 
				"SeqLengthDistFlag\tSeqDupFlag\tOverrepresentedSeqsFlag\tAdapterContentFlag\tKmerContentFlag\n");
		
		// Examine each of the fastqc directories
		for(int i = 0; i < fastqcDirectories.size(); i++) {
			
			System.out.println("Reading FASTQC output: " + fastqcDirectories.get(i) + "\t(" + (i+1) + " of " + fastqcDirectories.size());
			
			// Read the FASTQC data file in
			Data fastqcInfo = new Data(path + fastqcDirectories.get(i) + "/fastqc_data.txt", false, random);
				
			// Print a single line summary
			String output = fastqcInfo.buildOutputLineSummarisingFastQCData(qualityThreshold, deviationThreshold, nSitesThreshold, minLength);
			bWriter.write(output + "\n");
		}
		
		// Close the output file
		bWriter.close();

	}
	
	public static ArrayList<String> getFASTQCDirectories(String path) {
		
		// Open the directory at the end of the path
		File directory = new File(path);
		
		// Get a list of all the files and directories in the directory
		String[] filesAndDirectories = directory.list();
		
		// Initialise an ArrayList to store the FASTQC directories
		ArrayList<String> fastqcDirectories = new ArrayList<String>();
		
		// Examine each file and directory
		for(String element : filesAndDirectories) {
			
			// Open the file
			File file = new File(directory, element);
			
			// Skip files
			if(file.isDirectory() == false) {
				continue;
			}
			
			// Skip directories that don't end with "_fastqc"
			if(element.matches("(.*)_fastqc") == false) {
				continue;
			}
			
			// Add the current FASTQC directory into growing array
			fastqcDirectories.add(element);
		}
		
		
		return fastqcDirectories;
	}
	
}
