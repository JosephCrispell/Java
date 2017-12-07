package newProcessingSequencingData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.WriteToFile;

public class ExamineHeterozygousSitesInMergedVCFFile {

	public static void main(String[] args) throws IOException {
		
		// Get the current date
		String date = CalendarMethods.getCurrentDate("dd-MM-yyyy");
	
		// Set the path variable
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/NewAnalyses_13-07-17/vcfFiles/";
		
		// Note the merged file name
		String mergedFile = "merged_26-09-2017.txt";
		lookForHeterozygousSitesInMergedVcfsFile(path, mergedFile);
	}
	
	public static void lookForHeterozygousSitesInMergedVcfsFile(String path, String mergedFile) throws IOException{
		// Open an output file
		String coverageFile = path + "heterozygousSiteInfo_Coverage_" + mergedFile.split("_")[1];
		BufferedWriter bWriterCoverage = WriteToFile.openFile(coverageFile, false);
		String altSupportFile = path + "heterozygousSiteInfo_AltSupport_" + mergedFile.split("_")[1];
		BufferedWriter bWriterAltSupport = WriteToFile.openFile(altSupportFile, false);
				
		// Open the merged file
		InputStream input = new FileInputStream(path + mergedFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
				
		// Initialise an array to store the VCF file names
		String[] vcfFileNames;
		
		// Initialise variables to store the site information
		String[] isolateInfo;
		double[] hqBaseDepth;
		double sum;
		double altSupport;
				
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
		
		// Begin reading the file
		while(( line = reader.readLine()) != null){
							
			// Skip the VCF header lines
			if(line.matches("##(.*)") == true){
				continue;
			}
			
			// Get the isolate VCF file names from field line
			if(line.matches("#CHROM(.*)") == true){
				vcfFileNames = line.split("\t")[2].split(":");
				
				// Print a header into the output file
				WriteToFile.writeLn(bWriterCoverage, "Position\t" + ArrayMethods.toString(vcfFileNames, "\t"));
				WriteToFile.writeLn(bWriterAltSupport, "Position\t" + ArrayMethods.toString(vcfFileNames, "\t"));
				continue;
			}
			
			// Split the current line into its columns
			cols = line.split("\t");
					
			// Get the isolate quality information for the current position
			isolateInfo = cols[2].split(":");
			
			// Print the current position
			WriteToFile.write(bWriterCoverage, cols[1]);
			WriteToFile.write(bWriterAltSupport, cols[1]);
			
			// Examine each isolate in turn
			for(int i = 0; i < isolateInfo.length; i++){
				
				// Skip isolates which don't have alternate allele
				if(isolateInfo[i].matches("(.*)----(.*)") == false){
					
					// Get the high quality base depth at the current position
					hqBaseDepth = ArrayMethods.convert2Double(isolateInfo[i].split(";")[1].split(","));
					
					// Calculate the proportion of high quality bases supporting the alternate - ASSUMES only one!
					sum = ArrayMethods.sum(hqBaseDepth);
					altSupport = (hqBaseDepth[2] + hqBaseDepth[3]) / sum;
					
					WriteToFile.write(bWriterCoverage, "\t" + (int) sum);
					WriteToFile.write(bWriterAltSupport, "\t" + altSupport);
					
				}else{
					WriteToFile.write(bWriterCoverage, "\t" + "NA");
					WriteToFile.write(bWriterAltSupport, "\t" + "NA");
				}
			}
			WriteToFile.write(bWriterCoverage, "\n");
			WriteToFile.write(bWriterAltSupport, "\n");
		}
		System.out.println();
					
		// Close the input file
		input.close();
		reader.close();
		
		// Close the output file
		WriteToFile.close(bWriterCoverage);
		WriteToFile.close(bWriterAltSupport);

	}
}
