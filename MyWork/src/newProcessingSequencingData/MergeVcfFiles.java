package newProcessingSequencingData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.zip.GZIPInputStream;

import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.MatrixMethods;
import methods.WriteToFile;
import processingSequenceData.Combine;
import processingSequenceData.MergedFileBufferedReader;
import processingSequenceData.VcfFileBufferedReader;

public class MergeVcfFiles {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		if(args[0].equals("-help") || args[0].equals("")){
			System.out.println("Java Tool to summarise and combine a set of VCF files into a single Merged file.");
			System.out.println("\nCommand Line Structure:");
			System.out.println("\tjava -jar mergingTool.jar pathToDirectory genomeAnnotation.gff\n");
			System.out.println("\t\tpath2Directory\t\tProvide path to directory containing vcf files");
			System.out.println("\t\tgenomeAnnotation.gff\tProvide path to M. bovis genome annotation file if you would like to ignore repeat/PE/PPE regions, otherwise enter false");
			System.out.println("\nNotes:");
			System.out.println("Only sites that show variation against the Reference sequence are kept - Variant Positions.");
			System.out.println("In addition any sites falling within Repeat regions or PPE/PE coding regions can be removed.");
			System.out.println("The following information is stored for a given variant position in a VCF:");
			System.out.println("\tRead Depth - DP\n\tHigh Quality Base Depth (DP4) - HQDP\n\tMapping Quality - MQ\n\tQuality Score - QUAL");
			System.out.println("\tFQ\n\tReference and Alternate Alleles - RefAlt");
			System.out.println("\nThe above Information is stored in the following Format:");
			System.out.println("\tDP;HQDP;MQ;QUAL;FQ;RefAlt");
			System.out.println("\n*** Currently skipping any INDELS observed in the VCF files");
		}else{
			
			// Get the current date
			String date = CalendarMethods.getCurrentDate("dd-MM-yyyy");
			
			// Print check information
			System.out.println("Input information provided:");
			System.out.println("\tPath to directory:\t" + args[0]);
			System.out.println("\tGenome annotation file:\t" + args[1]);
			System.out.println();
			System.out.println("Output files produced:");
			System.out.println("merged_" + date + ".txt\t\t\tVCF-like file containing variant site quality information for all isolates.");
			System.out.println("genomeCoverage_" + date + ".txt\t\tRead depth of each isolate at each site on reference genome.");
			System.out.println("constantSiteCounts_" + date + ".txt\tAlelle counts for each site on the genome that was constant among the isolates or ignored.");
			System.out.println("heterozygousSiteCount_" + date + ".txt\tThe number of heterozygous sites (where high quality bases support both the reference and alternate alleles) for each isolate.");
			System.out.println();
			System.out.println("Beginning to merge VCF files. May take several minutes!");
			
			// Get the path to the directory
//			String path = "/home/josephcrispell/Desktop/Research/RepublicOfIreland/Fastqs_MAP/Testing/vcfFiles/";
			String path = args[0];
			
			// Open the genome annotation file
//			String annotationFile = "/home/josephcrispell/Desktop/Research/Reference_MAP/GCF_000007865.1_ASM786v1_genomic.gff";
			String annotationFile = args[1];
			
			// Note the regions of the genome that we want to ignore
			int[][] regionsToIgnore = new int[0][0];
			if(annotationFile.matches("false") == false){
				findRegionsToIgnore(annotationFile);
			}			
			
			// Find the VCF Files
			String[] vcfFileNames = findVcfFilesInDirectory(path);
		
			// Open the VCF Files
			VcfFile[] vcfFiles = openAllVcfFiles(vcfFileNames, path);
		
			/**
			 *  Combine all the VCF Files into a single Merged VCF File
			 *  	Only Variant sites are included in the Merged file
			 *  	Variant sites are sites where at least one of the isolates shows variation		 *  
			 */
		
//			String mergedVCFsFile = "/home/josephcrispell/Desktop/Research/RepublicOfIreland/Fastqs_MAP/Testing/vcfFiles/mergedVCFs_16-07-18.txt";
			String mergedVCFsFile = "merged_" + date + ".txt";
			
//			String coverageFile = "/home/josephcrispell/Desktop/Research/RepublicOfIreland/Fastqs_MAP/Testing/vcfFiles/coverageVCFs_16-07-18.txt";
//			String constantSiteCounts = "/home/josephcrispell/Desktop/Research/RepublicOfIreland/Fastqs_MAP/Testing/vcfFiles/constantSiteCounts_16-07-18.txt";
			String coverageFile = "genomeCoverage_" + date + ".txt";
			String constantSiteCounts = "constantSiteCounts_" + date + ".txt";
			boolean skipIndels = true;
			combineVCFFiles(vcfFiles, mergedVCFsFile, regionsToIgnore, coverageFile, constantSiteCounts, skipIndels);
			
			/**
			 * Note the heterozygous site count of each VCF file
			 */
			String heterozygousSiteCountFile = "heterozygousSiteCount_" + date + ".txt";
			printHeterozygousSiteCountsOfVCF(heterozygousSiteCountFile, vcfFiles);
			
			/**
			 * Close all of the opened VCF files
			 */
			
			closeAllVcfFiles(vcfFiles);
		}
	}

	public static void closeAllVcfFiles(VcfFile[] vcfFiles) throws IOException{
		
		for(VcfFile file : vcfFiles){
			file.getBfReader().close();
		}
	}
	
	public static void printHeterozygousSiteCountsOfVCF(String fileName, VcfFile[] vcfFiles) throws IOException{
		
		// Open the output file and print header
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		WriteToFile.writeLn(bWriter, "FileName\tHeterozygousSiteCount");
		
		// Print the heterozygous site count for each VCF file unless it is zero
		for(int i = 0; i < vcfFiles.length; i++){
			WriteToFile.writeLn(bWriter, vcfFiles[i].getFileName() + "\t" + vcfFiles[i].getHeterozygousSiteCount());

		}
		
		// Close the output file
		WriteToFile.close(bWriter);
	}
	
	public static int[][] findRegionsToIgnore(String annotationFile) throws IOException{
		
		/**
		 * 	##gff-version 3
		 *	#!gff-spec-version 1.20
		 *	#!processor NCBI annotwriter
		 *	##sequence-region NC_002945.3 1 4345492
		 *	##species http://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=233413
		 *	NC_002945.3	RefSeq	region	1	4345492	.	+	.	ID=id0;Name=ANONYMOUS;Dbxref=taxon:233413;Is_circular=true;gbkey=Src;genome=chromosome;mol_type=genomic DNA;old-name=Mycobacterium bovis subsp. bovis AF2122%2F97;strain=AF2122%2F97
		 *	NC_002945.3	RefSeq	gene	1	1524	.	+	.	ID=gene0;Name=dnaA;Dbxref=GeneID:1090743;gbkey=Gene;gene=dnaA;locus_tag=Mb0001
		 */
		
		// Open the genome annotation file
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(annotationFile)));
		
		// Initialise an array to store the start and end coordinates of each region to ignore
		int[][] regionCoords = new int[99999999][2];
		int pos = -1;
		
		// Initialise the necessary variables for parsing the file
		String[] cols;
		
		// Initialise a hashtable to keep track of whether regions have already been added
		Hashtable<String, Integer> present = new Hashtable<String, Integer>();
		
		// Examine each line of the file
		String line = null;
		while(( line = reader.readLine()) != null){
			
			// Skip the header lines
			if(line.matches("#(.*)")){
				continue;
			}
			
			// Split the line into its columns
			cols = line.split("\t", -1);
			
			// Skip lines that don't contain any columns
			if(cols.length == 1){
				continue;
			}
			
			// Search for repeat/PPE/PE regions and note their start and end coordinates
			if(cols[2].matches("repeat_region") || cols[8].matches("(.*)gene=PPE(.*)") || cols[8].matches("(.*)gene=PE(.*)")){
				
				if(present.get(cols[3] + ":" + cols[4]) == null){
				
					pos++;
					regionCoords[pos][0] = Integer.parseInt(cols[3]);
					regionCoords[pos][1] = Integer.parseInt(cols[4]);

					// Record the current region
					present.put(cols[3] + ":" + cols[4], 1);
				}
			}
		}
		
		// Close the annotation file
		reader.close();
	
		return MatrixMethods.removeEmptyRows(regionCoords, pos);
	}
	
	public static String[] findVcfFilesInDirectory(String path){
		
		// Open Current directory
		File folder = new File(path);
		
		// Extract Files from Directory
		File[] listOfFiles = folder.listFiles();
		
		// Initialise an array to store the file names
		String[] files = new String[listOfFiles.length];
		int posUsed = -1;
		String file;
		
		// Examine each file in Directory
		for(int i = 0; i < listOfFiles.length; i++){
			
			// Convert the File object to a String
			file = listOfFiles[i].getName();
			
			// Is the file a VCF file?
			if(file.matches("(.*).vcf(.*)")){
				posUsed++;
				files[posUsed] = file; // Store only the VCF files
			}
		}
		
		// Return list of VCF file names
		return ArrayMethods.subset(files, 0, posUsed);
	}

	public static VcfFile[] openAllVcfFiles(String[] fileNames, String path2Directory) throws IOException{
		
		// Initialise an Array to store the BufferedReaders
		VcfFile[] readers = new VcfFile[fileNames.length];
				
		// Initialise objects for opening and reading files
		InputStream input;
		BufferedReader bfReader = null;
		GZIPInputStream gZipFile;
		
		// Initialise String to store WBID for each VCF file
		String wbId;
				
		// Open all the files and store them
		for(int i = 0; i < fileNames.length; i++){
			
			// Extract the WBID from the VCf file name: WB10_S9_1
			wbId = fileNames[i].split("_")[0];
			
			// Check if file is zipped
			if(fileNames[i].matches("(.*).gz") == true){
				
				// Open the current vcf file
				input = new FileInputStream(path2Directory + "/" + fileNames[i]);
				gZipFile = new GZIPInputStream(input);
				bfReader = new BufferedReader(new InputStreamReader(gZipFile));
				
			}else{
				// Open the current vcf file
				input = new FileInputStream(path2Directory + "/" + fileNames[i]);
				bfReader = new BufferedReader(new InputStreamReader(input));
			}			
					
			// Store the BufferedReader for the current merged.txt file
			readers[i] = new VcfFile(fileNames[i], wbId, bfReader, 1); // Note that shift is set to 1		
		}
				
		return readers;	
	}

	public static String summariseSNPInfoLine(String snpInfoLine, VcfFile vcfFile){
		
		// Extract all the available information out of the SNP information line
		SnpInfo info = new SnpInfo(snpInfoLine, vcfFile);
		
		/**
		 *  Summarise the SNP with only information to be used in filtering
		 * 		Read Depth							DP		int
		 * 		High Quality Base Depth (DP4)		DP4		int[]
		 * 		Mapping Quality						MQ		int
		 * 		Quality Score								int
		 * 		FQ									FQ		int
		 * 		Reference and Alternate alleles				char char
		 * 
		 * 		DP;HQDP;MQ;QUAL;FQ;RefAlt
		 */
		
		// Initialise Default values for the above metrics
		int depth = 0;
		int[] hqDepth = {0,0,0,0};
		int mq = 0;
		int qual = 0;
		int fq = 0;
		char ref = 'N';
		char alt = 'N';
		
		// Get the Info column Hashtable
		Hashtable<String, double[]> infoCol = info.getInfoCol();
		
		// Build the summarise SNP info string
		String infoSummary = info.getChrom() + "\t" + info.getPos();
		
		// Add Read Depth
		if(infoCol.get("DP") != null){
			depth = (int) infoCol.get("DP")[0];
		}
		infoSummary = infoSummary + "\t" + depth;
		
		// Add the High Quality Base Depth
		if(infoCol.get("DP4") != null){
			hqDepth = ArrayMethods.convertDouble2Int(infoCol.get("DP4"));
		}
		infoSummary = infoSummary + ";" + ArrayMethods.toString(hqDepth, ",");
		
		// Add the Mapping Quality
		if(infoCol.get("MQ") != null){
			mq = (int) infoCol.get("MQ")[0];
		}
		infoSummary = infoSummary + ";" + mq;
		
		// Add the Quality Score
		infoSummary = infoSummary + ";" + info.getQualityScore();
		
		// Add the FQ Value
		if(infoCol.get("FQ") != null){
			fq = (int) infoCol.get("FQ")[0];
		}
		infoSummary = infoSummary + ";" + fq;
		
		// Add the Reference and Alternate alleles
		infoSummary = infoSummary + ";" + info.getRef() + info.getAlt();
		
		return infoSummary;
	}
	
	public static String[] returnNextLineFromEachFile(VcfFile[] readers, String[] previousLinesInfo, boolean skipIndels) throws IOException{
		
		/**
		 * Getting the next set of lines from the VCF files
		 * 	Because not all the VCF files will contain the same SNP positions, some of the inputs are paused until others catch up in the parallel
		 * 	reading - this is done using the shift variable assigned to each of the BufferedReaders
		 */
		
		// Initialise an Array to store the next set of file lines
		String[] linesInfo = new String[readers.length];
		String line = null;
		boolean indelFound;
		
		// Examine each of the BufferedReaders (within in their object) 
		for(int i = 0; i < readers.length; i++){
			
			// If shift = 1 mean that we are meant to move to the next line for the current BufferedReader
			if(readers[i].getShift() == 1){
				
				// Store the next file line from the current BufferedReader (VCF file) - skip INDELS
				if(skipIndels) {
					indelFound = true;
					while(indelFound) {
						line = readers[i].getBfReader().readLine();
						if(line != null) {
							indelFound = line.matches("(.*)INDEL(.*)");
						}else {
							indelFound = false;
						}						
					}
					
				}else {
					line = readers[i].getBfReader().readLine();
				}							
				
				// Check that haven't just reach end of the VCF file
				if(line != null){
					linesInfo[i] = summariseSNPInfoLine(line, readers[i]);
				}else{
					linesInfo[i] = null;
				}
				
			// If shift = 0, then this stream is temporally paused to allow others to catch up
			}else{
				
				// Store the previous line - e.g. stays the same
				linesInfo[i] = previousLinesInfo[i];
			}
			
		}
		
		// Return the set of file lines
		return linesInfo;
	}

	public static String buildHeaderAndFields(VcfFile[] vcfFiles){
		// Here creating the Header and Fields part of VCF
		
		// Add the Header Information and the start of the Fields line
		String output = vcfFiles[0].getHeader() + "\n" + "#CHROM\tPOS\t";
				
		// Add the file names from each of the VCF files
		for(int i = 0; i < vcfFiles.length; i++){
					
			output = output + vcfFiles[i].getFileName();
			
			// Add separator (":") if necessary
			if(i < vcfFiles.length - 1){
				output = output + ":";
			}
		}
				
		// Return the Header and Fields as a string 
		return output;	
	}
	
	public static int checkIfFinished(String[] lines){
		// Combining the VCF files will finish when we have reached the end of all the VCF files
		
		// Create a boolean variable to indicate whether we are finished
		int finished = 1;
				
		// Look at each of lines just taken from the VCF files
		for(String line : lines){
					
			// If any of them aren't null (null if finished VCF file) then haven't finished
			if(line != null){
				finished = 0;
			}
		}
				
		// Return boolean result
		return finished;
	}

	public static int[] getSNP(String[] fileLines){
		
		// Get the SNP for which each of the lines (taken from the merged.txt file) are associated with
		
		// Initialise an array to store all the SNPs
		int[] snps = new int[fileLines.length];
		
		// Initialise a variable to use if reach the end of any of the merged.txt files - SNP not present
		int value = -99;
		
		// Examine each of the merged.txt file lines
		for(int i = 0; i < fileLines.length; i++){
			
			// As long as their is SNP info present
			if(fileLines[i] != null){
				
				// Insert the SNP from the current merged.txt file line
				value = Integer.parseInt(fileLines[i].split("\t")[1]); // Note that also record the value - this can be inserted into any empty positions
				snps[i] = value;
			}
		}
		
		// For any merged.txt file lines where no Info present - insert a value
		for(int i = 0; i < snps.length; i++){
			if(fileLines[i] == null){
				
				// Since using a actual SNP found then won't impact the finding the min or check if they are different
				snps[i] = value;
			}
		}
		
		// Return the SNPS
		return snps;
	}
	
	public static int allSame(int[] snps){
		
		// Check and see if all the SNPs are the same
		
		// Initialise boolean variable for result
		int allSame = 1;
		
		// Look at all SNPs 
		for(int i = 1; i < snps.length; i++){
			
			// If any of SNPs isn't equal to the previous SNP - then SNPs aren't all the same
			if(snps[i] != snps[i-1]){
				
				// Change result and finish
				allSame = 0;
				break;
			}
		}		
		
		// Return the result
		return allSame;
	}
	
	public static String[] buildOutputLine(VcfFile[] readers, String[] fileLines, String chrom) throws IOException{
		
		/**
		 * Here the SNP information from each of the VCF file needs to be combined into a single SNP information line in the output
		 * 	The problem is that not all of the VCF files will have all the same SNPs. But they are all ordered by the SNP. 
		 * 	Therefore as we investigate each VCF file in parallel - if the is a SNP present in some but absent in others then for those where
		 * 	it is absent a blank must be put in that set of Samples place.
		 * 
		 */
		
		// Get the SNP that each of the lines is associated with
		int[] snps = getSNP(fileLines);
		
		// Initialise variable in case some SNPs differ - only deal with minimum, then re-evaluate each VCF (move to next line if necessary)
		int min;
		
		// Initialise blank to insert when SNP coverage Information is not present in a given sample's VCF file
		String blank = "-------------------";
		
		// Initialise the output line
		String[] outputLines = new String[2];
		String snpInfo = "";
		
		// If all the SNPs are the same across the files
		if(allSame(snps) == 1){
			
			// Print the genome position out to file
			outputLines[1] = snps[0] + "\t";
			
			// Create the start of the SNP information line
			outputLines[0] = chrom + "\t" + snps[0] + "\t";
			
			// Then add the SNP coverage information from each sample's VCF file
			for(int i = 0; i < readers.length; i++){
				
				// Check the VCF file line isn't null (i.e. reached end of the that file - none of the remaining SNPs are present)
				if(fileLines[i] != null){
					
					// Get the SNP info for the current isolate
					snpInfo = fileLines[i].split("\t")[2];
					
					// Print the read depth for the current isolate into the coverage file
					outputLines[1] = outputLines[1] + snpInfo.split(";")[0];
					
					// Add the SNP coverage info from the current VCF file line
					outputLines[0] = outputLines[0] + snpInfo;
				
					// If not reading the last VCF file add separator
					if(i < readers.length - 1){
						outputLines[0] = outputLines[0] + ":";
						outputLines[1] = outputLines[1] + "\t";
					}
				
					// Set the shift for the current VCF file - ready to move to the next line
					readers[i].setShift(1);
				}else{
					
					// Read Depth for current isolate is unavailable, print NA
					outputLines[1] = outputLines[1] + "0";
					
					// If reached end of VCF file then insert blank info for the samples
					outputLines[0] = outputLines[0] + blank;
					
					// If not reading the last VCF file add separator
					if(i < readers.length - 1){
						outputLines[0] = outputLines[0] + ":";
						outputLines[1] = outputLines[1] + "\t";
					}
					
					// Set the shift for the current VCF file - reached end no point in shifting
					readers[i].setShift(0);
				}
			}			
		}else{ // If the SNPs aren't the same
			
			// Find the min SNP with information in one of the VCF files
			min = ArrayMethods.min(snps);
			
			// Print the genome position out to file
			outputLines[1] = min + "\t";
			
			// Create the start of the SNP Information line
			outputLines[0] = chrom + "\t" + min + "\t";
			
			// Examine each of the lines from the VCF file
			for(int i = 0; i < readers.length; i++){
				
				// If the SNP in the info from the current VCF file is equal to the min then add its information in
				if(snps[i] == min && fileLines[i] != null){
					
					// Get the SNP info for the current isolate
					snpInfo = fileLines[i].split("\t")[2];
					
					// Print the read depth for the current isolate into the coverage file
					outputLines[1] = outputLines[1] + snpInfo.split(";")[0];
					
					// Add the SNP coverage information from the current VCF file in
					outputLines[0] = outputLines[0] + snpInfo;
				
					// If not reading the last VCF file add separator
					if(i < readers.length - 1){
						outputLines[0] = outputLines[0] + ":";
						outputLines[1] = outputLines[1] + "\t";
					}
					
					// Set the shift for the current VCF file - used the information in this line, move to next
					readers[i].setShift(1);
				}else{
					
					// Read Depth for current isolate is unavailable, print NA
					outputLines[1] = outputLines[1] + "0";
					
					// SNP for current VCF file isn't equal to the min (means min SNP didn't have coverage in the current sample) - add in blank insert
					outputLines[0] = outputLines[0] + blank;
					
					// If not reading the last VCF file add separator
					if(i < readers.length - 1){
						outputLines[0] = outputLines[0] + ":";
						outputLines[1] = outputLines[1] + "\t";
					}
					
					// Set the shift for the current VCF file - didn't use the information in this line don't move until this information has been used
					readers[i].setShift(0);
				}
			}	
		}
		
		// Return the SNP Information line
		return outputLines;
	}
	
	public static void closeVcfFiles(VcfFile[] readers) throws IOException{
		
		for(VcfFile reader : readers){
			reader.getBfReader().close();
		}
	}
	
	public static int checkIfSNPFallsInRegionToIgnore(int snp, int[][] regionsToIgnore){
		
		int result = 0;
		
		// Check each region in turn to see if SNP falls within it
		for(int[] coords : regionsToIgnore){
			
			if(snp >= coords[0] && snp <= coords[1]){
				result = 1;
				break;				
			}
		}
		
		return result;
	}
	
	public static void combineVCFFiles(VcfFile[] vcfFiles, String mergedVCFsFile, int[][] coordsOfRegionsToIgnore,
			String coverageFile, String constantSiteCountsFile, boolean skipIndels) throws IOException{
		
		// Open the output merged VCFs File
		BufferedWriter outputMerged = WriteToFile.openFile(mergedVCFsFile, false);
		
		// Open the output coverage file
		BufferedWriter outputCoverage = WriteToFile.openFile(coverageFile, false);
		
		// Get the first set of SNP Information lines
		String[] lines = returnNextLineFromEachFile(vcfFiles, new String[vcfFiles.length], skipIndels);
		String[] outputLines = new String[2];
		int snp;
		int result;
		
		// Initialise an array to store a count of constant sites: A C G T
		int[] constantSiteCounts = new int[4];
		
		// Get the CHROM id for the SNPs
		String chrom = lines[0].split("\t")[0];
		
		// Print out the Header and Fields Lines
		String header = buildHeaderAndFields(vcfFiles);
		WriteToFile.writeLn(outputMerged, header);
		
		// Find the fields section of the header
		String[] parts = header.split("\n");
		int indexOfHeader = -1;
		for(int i = 0; i < parts.length; i++){
			if(parts[i].matches("#CHROM(.*)")){
				indexOfHeader = i;
				break;
			}
		}
		parts = parts[indexOfHeader].split("\t");
		
		header = parts[1] + "\t" + parts[2];
		WriteToFile.writeLn(outputCoverage, header.replace(":", "\t"));
		
		// Read the rest of the SNP information lines
		int lineNo = 0;
		while(checkIfFinished(lines) == 0){ // Check that haven't reached the end of all the files
			
			// Combine the SNP Information lines from each VCF file into a single output line for each SNP
			outputLines = buildOutputLine(vcfFiles, lines, chrom);
		
			// Print the isolate coverage info for the current position out to file
			WriteToFile.writeLn(outputCoverage, outputLines[1]);
			
			// Does the the SNP fall within a region we want to ignore?
			snp = Integer.parseInt(outputLines[0].split("\t")[1]);
			result = 0;
			if(coordsOfRegionsToIgnore.length > 0){
				result = checkIfSNPFallsInRegionToIgnore(snp, coordsOfRegionsToIgnore);
			}			
			
			// Only print SNP Information for those SNPs where an Alternate allele has been found in at least 1 sample
			if(outputLines[0].matches("(.*);[A-Z][A-Z](.*)") && result == 0){
				WriteToFile.writeLn(outputMerged, outputLines[0]);
			
			// Record the number of constant sites as a Count: A C G T
			}else if(result == 0){
				
				// Found out which allele is present at the current position
				constantSiteCounts = findAllelePresent(outputLines[0], constantSiteCounts);
			}
			
			// Get the next set of lines from the merged.txt lines
			lines = returnNextLineFromEachFile(vcfFiles, lines, skipIndels);
			
			// Print a progress dot every x lines
			if(lineNo % 10000 == 0){
				System.out.print(".");
			}
		
			lineNo++;
		}
		System.out.println();
		
		// Open the constant site counts file
		BufferedWriter constantSiteCountsOutput = WriteToFile.openFile(constantSiteCountsFile, false);
			
		// Print out the Constant Site Counts
		WriteToFile.writeLn(constantSiteCountsOutput, "\n Constant Site Counts: A, C, G, T\n" + ArrayMethods.toString(constantSiteCounts, "\t"));
		
		WriteToFile.close(outputMerged);
		WriteToFile.close(outputCoverage);
		WriteToFile.close(constantSiteCountsOutput);
		closeVcfFiles(vcfFiles);
	}
	
	public static int[] findAllelePresent(String line, int[] constantSiteCounts){
		
		/**
		 *  Output Line Structure:
		 *   #CHROM						POS		IsolateInfoA:IsolateInfoB:...
		 *   ENA|BX248333|BX248333.1	1057	264;0,0,129,114;50;222.0;-282;AG:83;0,0,39,33;49;222.0;-244;AG:122;0,0,57,59;50;222.0;-282;AG:
		 *   0							1		2
		 */
		
		// Get the Reference allele at the current position
		//					 isolateInfo    isolateInfoA  Alleles		ReferenceAllele
		String[] isolatesInfo = line.split("\t")[2].split(":");
		
		// Find the first isolate for which there is information
		String allele = "";
		for(String info : isolatesInfo){
			
			if(info.matches("(.*)-----(.*)") == false){
				allele = info.split(";")[5].substring(0,1);
				break;
			}
		}
		
		// Increment the correct count A C G T
		if(allele.equals("A")){
			constantSiteCounts[0]++;
		}else if(allele.equals("C")){
			constantSiteCounts[1]++;
		}else if(allele.equals("G")){
			constantSiteCounts[2]++;
		}else if(allele.equals("T")){
			constantSiteCounts[3]++;
		}else{
			System.out.println("Unknown Allele Found: " + allele);
		}
		
		return constantSiteCounts;
	}
}
