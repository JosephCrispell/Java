package workForMarian;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;

import newProcessingSequencingData.FilterSet;

import methods.ArrayMethods;
import processingSequenceData.Combine;
import processingSequenceData.VcfFileBufferedReader;
import processingSequenceData.MergedFileBufferedReader;

public class SnpCoverage {

	public static void main(String[] args) throws IOException {
		
		/**
		 * SNP Coverage information
		 * 	How many of the isolates have coverage at any given SNP?
		 * 
		 * Note that Marian's VCF files only show variant sites.
		 */
		
		String path = "/Users/josephcrisp1/Desktop/vcfFiles/";
		
		// Find the VCF Files
		String[] vcfFiles = Combine.findVcfFilesInDirectory(path);	
		
		// Open all the VCF Files
		VcfFileBufferedReader[] readers = openAllVcfFiles(vcfFiles, path);
		
		// Set Filters that we want to use (will be in one of the python scripts)
		//FilterSet filters = new FilterSet(13, 2, 20, 0.95, 0, 0);
		
		// Examine each SNP across the VCF files
		
		
	}
	
//	public static int checkSufficientCoverage(String snpInfo, FilterSet filters){
//		
//		/**
//		 *  #CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	fileName
//		 *  gi|31791177|ref|NC_002945.3|	1057	.	A	G	222	.	DP=147;VDB=0.0182;AF1=1;AC1=2;DP4=0,0,67,73;MQ=60;FQ=-282	GT:PL:GQ	1/1:255,255,0:99
//		 */
//		
//		
//	}
	
	public static int[] getPositions(String[] fileLines){
		
		// Get the Position for which each of the lines (taken from the VCF file) are associated with
		
		// Initialise an array to store all the Positions
		int[] snps = new int[fileLines.length];
		
		// Initialise a variable to use if reach the end of any of the VCF files - Position not present
		int value = -99;
		
		// Examine each of the merged.txt file lines
		for(int i = 0; i < fileLines.length; i++){
			
			// As long as their is Position info present
			if(fileLines[i] != null){
				
				// Insert the Position from the current merged.txt file line
				value = Integer.parseInt(fileLines[i].split("\t")[1]); // Note that also record the value - this can be inserted into any empty positions
				snps[i] = value;
			}
		}
		
		// For any merged.txt file lines where no Info present - insert a value
		for(int i = 0; i < snps.length; i++){
			if(fileLines[i] == null){
				
				// Since using a actual Position found then won't impact the finding the min or check if they are different
				snps[i] = value;
			}
		}
		
		// Return the Positions
		return snps;
	}
	
 	public static void examineNextSnp(MergedFileBufferedReader[] readers, String[] fileLines){
		
		/**
		 * Here every SNP position that has coverage in at least 1 isolate must be examined in each of the isolates.
		 * 	VCF files are available for each isolate listing the number of sites with coverage in that particular isolate.
		 * 	We need to move from the minimum position in the isolates all the way to the maximum, examining the coverage
		 *  of each SNP in each isolate.
		 *  SNPs here are the sites that vary against the reference in at least one isolate (FIRST FILTER)
		 *  Coverage refers to which isolates have sufficient quality of coverage according to some filtering criteria
		 *  
		 * For each SNP encountered this method should return the percentage of isolates that have sufficient coverage at that
		 * position.
		 * 
		 * 	The problem is that not all of the VCF files will have all the same SNPs. But they are all ordered by the SNP. 
		 * 	Therefore as we investigate each VCF file in parallel - we record the number of isolates that have sufficient coverage, 
		 *  if the SNP is absent in some VCF files then that means for these isolates there was no coverage at the current SNP.
		 */
		
		// Get the Position that each of the lines (from the VCF files) is associated with
		int[] positions = getPositions(fileLines);
		
		// Initialize variable in case some Positions differ - only deal with minimum, then re-evaluate each merged.txt (move to next line if necessary)
		int min;
		
		// If all the Positions are the same across the files then
		if(Combine.allSame(positions) == 1){
			
			// Is there a SNP at the current Position?????
			
			// Then examine each of the Positions in each VCF file
			for(int i = 0; i < readers.length; i++){
				
				// Check the VCF file line isn't null (i.e. reached end of the that file - none of the remaining SNPs are present)
				if(fileLines[i] != null){
				
					// FILTER site and determine if has sufficient coverage
					
					
					// Set the shift for the current VCF file - ready to move to the next line
					readers[i].setShift(1);
				}else{
					
					// Set the shift for the current VCF file - reached end no point in shifting
					readers[i].setShift(0);
				}
			}			
		}else{ // If the positions aren't the same
			
			// Find the minimum position with information in one of the VCF files
			min = ArrayMethods.min(positions);
			
			// Is there Variation at this current position?????
			
			// Examine each of the lines from the VCF files
			for(int i = 0; i < readers.length; i++){
				
				// If the Position in the info from the current VCF file is equal to the minimum then look at its information
				if(positions[i] == min && fileLines[i] != null){
					
					// FILTER site and determine if has sufficient coverage
					
					
					// Set the shift for the current VCF file - used the information in this line, move to next
					readers[i].setShift(1);
				}else{
					
					// Set the shift for the current VCF file - didn't use the information in this line don't move until this information has been used
					readers[i].setShift(0);
				}
			}	
		}
	}
	
	public static String[] returnNextLineFromEachFile(MergedFileBufferedReader[] readers, String[] previousLines) throws IOException{
		
		/**
		 * Getting the next set of lines from the merged.txt files
		 * 	Because not all the merged.txt files will contain the same SNP positions, some of the inputs are paused until others catch in the parallel
		 * 	reading - this is done using the shift variable assigned to each of the BufferedReaders
		 */
		
		// Initialise an Array to store the next set of file lines
		String[] lines = new String[readers.length];
		
		// Examine each of the BufferedReaders (within in their object) 
		for(int i = 0; i < readers.length; i++){
			
			// If shift = 1 mean that we are meant to move to the next line for the current BufferedReader
			if(readers[i].getShift() == 1){
				
				// Store the next file line from the current BufferedReader (merged.txt file)
				lines[i] = readers[i].getBfReader().readLine(); 
				
			// If shift = 0, then this stream is temporally paused to allow ohers to catch up
			}else{
				
				// Store the previous line - e.g. stays the same
				lines[i] = previousLines[i];
			}
			
		}
		
		// Return the set of file lines
		return lines;
	}
	
	public static int checkIfFinished(String[] lines){
		
		// Combining the merged.txt files will finish when we have reached the end of all the merged.txt files
		
		// Create a boolean variable to indicate whether we are finished
		int finished = 1;
		
		// Look at each of lines just taken from the merged.txt
		for(String line : lines){
			
			// If any of them aren't null (null if finished merged.txt file) then haven't finished
			if(line != null){
				finished = 0;
			}
		}
		
		// Return boolean result
		return finished;
	}
	
	public static VcfFileBufferedReader[] openAllVcfFiles(String[] fileNames, String path2Directory) throws IOException{
		// Open each of the merged.txt files and store them as accessible BufferedReaders
		
		// Initialise an Array to store the BufferedReaders
		VcfFileBufferedReader[] readers = new VcfFileBufferedReader[fileNames.length];
				
		// Initialise objects for opening and reading files
		InputStream input;
		BufferedReader bfReader;
		
		// Initialise String to store WBID for each VCF file
		String wbId;
				
		// Open all the files and store them
		for(int i = 0; i < fileNames.length; i++){
			
			// Extract the WBID from the VCf file name: WB10_S9_1
			wbId = fileNames[i].split("_")[0];
					
			// Open the current merged.txt file
			input = new FileInputStream(path2Directory + "/" + fileNames[i]);
			bfReader = new BufferedReader(new InputStreamReader(input));
					
			// Store the BufferedReader for the current merged.txt file
			readers[i] = new VcfFileBufferedReader(fileNames[i], new String[0], wbId, bfReader, 1);		
		}
				
		return readers;	
	}
}

