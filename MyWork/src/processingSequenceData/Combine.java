package processingSequenceData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.HashtableMethods;
import methods.WriteToFile;

public class Combine {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

//		String[] files = {	"C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/raw_reads_B1/vcfFiles/merged.txt",
//							"C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/raw_reads_seq2/vcfFiles/merged.txt",
//							"C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/raw_reads_seq3/vcfFiles/merged.txt"};
		
//		combineMergedFiles(args);
		
		
//		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/test";
//		String file = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/sampleInformation.csv";
		//combineVCFFiles(args[1], args[0], args[2]);
		
		String nZSampleInfo = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/SampleInformation/sample_information.csv";
		String TarSampleInfo = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/Taranaki/SampleInfoTaranaki.csv";
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/AllVCFs/";
		String alts = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/NewZealand/AllVCFs/alternatePositions.txt";
		
//		if(args[0].equals("-help")){
//			
//			System.out.println("JAR file to merge VCF files into a single file.");
//			System.out.println("Command Line Arguments:");
//			System.out.println("NewZealandSampleInformationFile\tTaranakiSampleInformation\tPathToCurrentDirectory\tAlternatePositionsFile");
//			System.out.println("Note: VCF files must be unzipped.");
//			
//		}else{
//			combineVCFFilesNewZealand(args[0], args[1], args[2], args[3]);
//		}

		combineVCFFilesNewZealand(nZSampleInfo, TarSampleInfo, path, alts);
	}
	
	// Combining VCF file methods
	
	public static void combineVCFFilesNewZealand(String sampleInformationFileNZ, String sampleInformationFileTar, String path2Directory, String altPosFileName) throws IOException{
		
		// Open the output File
		BufferedWriter bWriter = WriteToFile.openFile(altPosFileName, false);
		
		// Find the VCF Files
		String[] vcfFiles = findVcfFilesInDirectory(path2Directory);
		
		// Extract the Sample Information and store in Hashtable
		Hashtable<String, String[]> sampleInfo = getSampleInformationNewZealand(sampleInformationFileNZ);
		sampleInfo = getSampleInformationTaranaki(sampleInfo, sampleInformationFileTar);
		
		// Open the Merged.txt files
		VcfFileBufferedReader[] readers = openAllVcfFiles(vcfFiles, sampleInfo, path2Directory);
		
		// Get the first set of SNP Information lines
		String[] lines = returnNextLineFromEachFile(readers, new String[readers.length]);
		String line;
		
		// Get the CHROM id for the SNPs
		String chrom = lines[0].split("\t")[0];
		
		// Print out the Header and Fields Lines
		WriteToFile.writeLn(bWriter, buildHeaderAndFieldsNewZealandAndTaranaki(readers));
		
		// Read the rest of the SNP information lines
		int lineNo = 0;
		while(checkIfFinished(lines) == 0){ // Check that haven't reached the end of all the files
			
			// Combine the SNP Information lines from each VCF file into a single output line for each SNP
			line = buildOutputLine(readers, lines, chrom);
			
			// Only print SNP Information for those SNPs where an Alternate allele has been found in at least 1 sample
			if(line.matches("(.*);[A-Z][A-Z](.*)")){
				WriteToFile.writeLn(bWriter, line);
			}
			
			// Get the next set of lines from the merged.txt lines
			lines = returnNextLineFromEachFile(readers, lines);
			
			// Print a progress dot every x lines
			lineNo++;
			if(lineNo % 10000 == 0){
				System.out.print(".");
				
				if(lineNo % 1000000 == 0 && lineNo != 0){
					
					System.out.println();
				}				
			}
			
		}
		System.out.println();
		
		WriteToFile.close(bWriter);
		closeVcfFiles(readers);
	}
	
	public static void combineVCFFiles(String sampleInformationFile, String path2Directory, String altPosFileName) throws IOException{
		
		// Open the output File
		BufferedWriter bWriter = WriteToFile.openFile(altPosFileName, false);
		
		// Find the VCF Files
		String[] vcfFiles = findVcfFilesInDirectory(path2Directory);
		
		// Extract the Sample Information and store in Hashtable
		Hashtable<String, String[]> sampleInfo = getSampleInformationWoodchester(sampleInformationFile);
		
		// Open the Merged.txt files
		VcfFileBufferedReader[] readers = openAllVcfFiles(vcfFiles, sampleInfo, path2Directory);
		
		// Get the first set of SNP Information lines
		String[] lines = returnNextLineFromEachFile(readers, new String[readers.length]);
		String line;
		
		// Get the CHROM id for the SNPs
		String chrom = lines[0].split("\t")[0];
		
		// Print out the Header and Fields Lines
		WriteToFile.writeLn(bWriter, buildHeaderAndFieldsWoodchester(readers));
		
		// Read the rest of the SNP information lines
		int lineNo = 0;
		while(checkIfFinished(lines) == 0){ // Check that haven't reached the end of all the files
			
			// Combine the SNP Information lines from each VCF file into a single output line for each SNP
			line = buildOutputLine(readers, lines, chrom);
			
			// Only print SNP Information for those SNPs where an Alternate allele has been found in at least 1 sample
			if(line.matches("(.*);[A-Z][A-Z](.*)")){
				WriteToFile.writeLn(bWriter, line);
			}
			
			// Get the next set of lines from the merged.txt lines
			lines = returnNextLineFromEachFile(readers, lines);
			
			// Print a progress dot every x lines
			if(lineNo % 10000 == 0){
				System.out.print(".");
			}
			lineNo++;
		}
		System.out.println();
		
		WriteToFile.close(bWriter);
		closeVcfFiles(readers);
	}
	
	public static VcfFileBufferedReader[] openAllVcfFiles(String[] fileNames, Hashtable<String, String[]> sampleInfo, String path2Directory) throws IOException{
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
			readers[i] = new VcfFileBufferedReader(fileNames[i], sampleInfo.get(wbId), wbId, bfReader, 1);		
		}
				
		return readers;	
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
			if(file.matches("(.*).vcf")){
				posUsed++;
				files[posUsed] = file; // Store only the VCF files
			}
		}
		
		// Return list of VCF file names
		return ArrayMethods.subset(files, 0, posUsed);
	}

	public static Hashtable<String, String[]> getSampleInformationNewZealand(String sampleInfoFile) throws IOException{
		// Initialise a Hashtable to store the Sample Information

		/**
		 * 	Samplename,	year,	HOST,	Latitude,	Longitude,	LOCATION,	AREA,		REATYPE
		 *	AgR197,		1983,	POSSUM,	-42.467,	171.211,	GREYMOUTH,	WESTCOAST,	1
		 *	0			1		2		3			4			5			6			7
		 * 
		 * Necessary Information:
		 * SampleID	Location	Lat	Long	Region	Year	Species	REA
		 * 0		1			2	3		4		5		6		7	
		 */
				
		// Open the current merged.txt file
		InputStream input = new FileInputStream(sampleInfoFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise a Hashtable to store the Sample Information
		Hashtable<String, String[]> sampleInfo = new Hashtable<String, String[]>();
		
		// Initialise variables for dealing with the sampleInfo
		String[] parts;
		String[] info = new String[8];
				
		// Begin reading the sample information file
		String line = null;
	    while(( line = reader.readLine()) != null){
			    	
	    	// Skip the File Header Lines
	    	if(line.matches("Samplename(.*)") == true){
	    		continue;
	    	}
		    	
	    	// Split the File line by "," - csv
	    	parts = line.split(",");
	    	
	    	// Extract the relevant information
	    	info[0] = parts[0]; // Sample ID
	    	info[1] = parts[5]; // Location
	    	info[2] = parts[3]; // Latitude
	    	info[3] = parts[4]; // Longitude
	    	info[4] = parts[6]; // Area
	    	info[5] = parts[1]; // Year
	    	info[6] = parts[2]; // Species
	    	info[7] = parts[7]; // REA
	    	
	    	// Store the Sample Information by the Sample ID
	    	sampleInfo.put(parts[0], info);	 
	    	
	    	// Wipe the variables
	    	parts = new String[0];
	    	info = new String[8];
	    	
	    }
		
	    return sampleInfo;
	}
	
	public static Hashtable<String, String[]> getSampleInformationTaranaki(Hashtable<String, String[]> sampleInfo, String sampleInfoFile) throws IOException{
		// Initialise a Hashtable to store the Sample Information

		/**
		 * 	ID,							Case No,	Sample ID,	Host,	Owner,	Location,	Area,		
		 * 	Taranaki- Opunake Hawera,	W08/0843,	27,			BOVINE,	Farm A,	Opunake,	Taranaki,
		 * 	0							1			2			3		4		5			6	
		 * 
		 * 	Lat,		Long,				REA TYPE,	YEAR,	M40,	ETRD,	ETRC,	ETRE,	NZ2,	Q18,	Q11a,	Q26,	DR2,	DR1,	Q3232,
		 * 	-39.4083, 	174.0843,			20,			2008,	2,		4,		4,		3,		5,		2,		9,		4,		15,		5,		8,
		 * 	7			8					9			10		11		12		13		14		15		16		17		18
		 * 
		 * Necessary Information:
		 * SampleID	Location	Lat	Long	Region	Year	Species	REA
		 * 0		1			2	3		4		5		6		7	
		 */
				
		// Open the current merged.txt file
		InputStream input = new FileInputStream(sampleInfoFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise variables for dealing with the sampleInfo
		String[] parts;
		String[] info = new String[8];
				
		// Begin reading the sample information file
		String line = null;
	    while(( line = reader.readLine()) != null){
			
	    	// Skip the File Header Lines
	    	if(line.matches("ID(.*)") == true){
	    		continue;
	    	}
		    	
	    	// Split the File line by "," - csv
	    	parts = line.split(",");
	    	
	    	// Extract the relevant information
	    	info[0] = "N" + parts[2]; // Sample ID
	    	info[1] = parts[5]; // Location
	    	info[2] = parts[7]; // Latitude
	    	info[3] = parts[8]; // Longitude
	    	info[4] = parts[6]; // Area
	    	info[5] = parts[10]; // Year
	    	info[6] = parts[3]; // Species
	    	info[7] = parts[9]; // REA			Note: -1 means not known
	    	
	    	// Store the Sample Information by the Sample ID
	    	sampleInfo.put("N" + parts[2], info);	 
	    	
	    	// Wipe the variables
	    	parts = new String[0];
	    	info = new String[8];
	    	
	    }
		
	    return sampleInfo;
	}
	
	public static Hashtable<String, String[]> getSampleInformationWoodchester(String sampleInfoFile) throws IOException{
		
		// Initialise a Hashtable to store the Sample Information
		
		/**
		 * 
		 *  Sample Information file Structure:
		 *  
		 *	WB_id	CB_id	SequencingBatch	tattoo	date	pm	sample	afno	result	lesions	abscesses	Spoligo	SpolPrime	Social_Group_Trapped_At
		 *	0		1		2				3		4		5	6		7		8		9		10			11		12			13
		 *
		 *	String	Genotype	Spoligotype	Gen+Spol	comment,,,,,,,
		 *	14		15			16			17			18...
		 *
		 *	Want to store the following Information: WBID:	CBID	SeqBatch	tattoo	date	socialGroup	
		 *											 0		1		2			3		4		13
		 */
		
		// Open the current merged.txt file
		InputStream input = new FileInputStream(sampleInfoFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise a Hashtable to store the Sampel Information
		Hashtable<String, String[]> sampleInfo = new Hashtable<String, String[]>();
		
		// Initialise variables for dealing with the sampleInfo
		String[] parts;
		String[] info = new String[6];
		
		// Begin reading the sample information file
		String line = null;
	    while(( line = reader.readLine()) != null){
	    	
	    	// Skip the File Header Lines
	    	if(line.matches("WB(.*)") == false || line.matches("WB_(.*)") == true){
	    		continue;
	    	}
	    	
	    	// Split the File line by "," - csv
	    	parts = line.split(",");
	    	
	    	// Extract the relevant information
	    	info[0] = parts[1];
	    	info[1] = parts[2];
	    	info[2] = parts[3];
	    	info[3] = parts[4];
	    	info[4] = parts[13];
	    	
	    	// Store the Sample Information by the WBID
	    	sampleInfo.put(parts[0], info);	 
	    	
	    	// Wipe the variables
	    	parts = new String[0];
	    	info = new String[6];
	    	
	    }
		
	    return sampleInfo;
	}
	
	public static String buildHeaderAndFieldsWoodchester(VcfFileBufferedReader[] readers){
		
		// Here creating the Header and Fields part of VCF
		
		// Add the Header Information and the start of the Fields line
		String output = readers[0].getHeader() + "\n" + "#CHROM\tPOS\t";
		
		// Initialise variables or sample information
		String[] sampleInfo;
		String runId;
		String sampleName;
		
		// Add the sample Names from each of the merged.txt files
		for(int i = 0; i < readers.length; i++){
			
			// Sample Name Structure: WBID_RunID_CBID_SeqBatch_Tattoo_Date_SocialGroup
			
			// Get the RunID from the VCF file name: WB10_S9_1
			runId = readers[i].getFileName().split("_")[1];
			
			// Create the sample name
			sampleInfo = readers[i].getSampleInfo();
			sampleName = readers[i].getId() + "_" + runId + "_" + sampleInfo[0] + "_" + sampleInfo[1] + "_" + sampleInfo[2] + "_" + sampleInfo[3] + "_" + sampleInfo[4];
			
			// Add the sample name into the fields
			output = output + sampleName;
			
			// Add separator (":") if necessary
			if(i < readers.length - 1){
				output = output + ":";
			}
		}
		
		// Return the Header and Fields as a string 
		return output;		
	}

	public static String buildHeaderAndFieldsNewZealandAndTaranaki(VcfFileBufferedReader[] readers){
		
		// Here creating the Header and Fields part of VCF
		
		// Add the Header Information and the start of the Fields line
		String output = readers[0].getHeader() + "\n" + "#CHROM\tPOS\t";
		
		// Initialise variables or sample information
		String[] sampleInfo;
		String runId;
		String sampleName;
		String sampleNo;
		
		// Add the sample Names from each of the merged.txt files
		for(int i = 0; i < readers.length; i++){
			
			/**
			 * 	SampleID	Location	Lat	Long	Region	Year	Species	REA
			 *	 0			1			2	3		4		5		6		7	
			 *
			 *	SampleID_RunID_Location_Lat_Long_Region_Year_Species_REA_SampleNo
			 */
			
			// Get the RunID from the VCF file name: N10_S9_1
			runId = readers[i].getFileName().split("_")[1];
			sampleNo = readers[i].getFileName().split("_")[2];
			
			// Create the sample name
			sampleInfo = readers[i].getSampleInfo();
			sampleName = readers[i].getId() + "_" + runId + "_" + sampleInfo[1] + "_" + sampleInfo[2] + "_" + sampleInfo[3] + "_" + sampleInfo[4] + "_" + sampleInfo[5] + "_" + sampleInfo[6] + "_" + sampleInfo[7] + "_" + sampleNo;
			
			// Add the sample name into the fields
			output = output + sampleName;
			
			// Add separator (":") if necessary
			if(i < readers.length - 1){
				output = output + ":";
			}
		}
		
		// Return the Header and Fields as a string 
		return output;		
	}
	
	public static String[] returnNextLineFromEachFile(VcfFileBufferedReader[] readers, String[] previousLines) throws IOException{
		
		/**
		 * Getting the next set of lines from the VCF files
		 * 	Because not all the VCF files will contain the same SNP positions, some of the inputs are paused until others catch up in the parallel
		 * 	reading - this is done using the shift variable assigned to each of the BufferedReaders
		 */
		
		// Initialise an Array to store the next set of file lines
		String[] lines = new String[readers.length];
		String line;
		
		// Examine each of the BufferedReaders (within in their object) 
		for(int i = 0; i < readers.length; i++){
			
			// If shift = 1 mean that we are meant to move to the next line for the current BufferedReader
			if(readers[i].getShift() == 1){
				
				// Store the next file line from the current BufferedReader (VCF file)
				line = readers[i].getBfReader().readLine();
				
				// Check that haven't just reach end of the VCF file
				if(line != null){
					lines[i] = summariseSNPInfoLine(line);
				}else{
					lines[i] = line;
				}
				
			// If shift = 0, then this stream is temporally paused to allow others to catch up
			}else{
				
				// Store the previous line - e.g. stays the same
				lines[i] = previousLines[i];
			}
			
		}
		
		// Return the set of file lines
		return lines;
	}
	
	public static String summariseSNPInfoLine(String snpInfoLine){
		/**		 
		 * To save drive space, we try to reduce the merged file size to a minimum so the SNP Information in each VCF at each SNP is
		 * summarised:
		 * 		MQ;HQb;DP;RefAlt
		 * 
		 * Each SNP Information Line in the VCF file is structured:
		 * 
		 * 			#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT			"\t"
		 * 			0		1	2	3	4	5		6		7		8
		 *													|
		 *							INFO Column Fields <-----						";"
		 *							DP	AF1	AC1	DP4	MQ	FQ
		 *							0	1	2	3	4	5
		 *										|
		 * 				DP4 Column Fields <------									","
		 * 				ref-forward	ref-Reverse	alt-forward	alt-reverse
		 * 				0			1			2			3
		 */
		
		// Split the SNP Information Line by "\t"
		String[] parts = snpInfoLine.split("\t");
		
		// Select the Info Column
		String[] infoColumn = parts[7].split(";");
		
		// Initialise the Relevant SNP coverage metrics - with defaults
		String depth = "0";
		String hqDepth = "0,0,0,0";
		String mq = "0";
		
		// Initialise variable to store metric information
		String[] varInfo;
		
		// Examine the Info column and check if any of the metrics are present
		for(String col : infoColumn){
			varInfo = col.split("=");
			
			// Skip Variables with incomplete information
			if(varInfo.length < 2){
				continue;
			}
			
			// Search for specific tags corresponding to the metrics of interest
			if(varInfo[0].equals("DP")){
				depth = varInfo[1];
				
			}else if(varInfo[0].equals("DP4")){
				hqDepth = varInfo[1];
				
			}else if(varInfo[0].equals("MQ")){
				mq = varInfo[1];
			}
			
			// Reset Variable Info parts
			varInfo = new String[0];	
		}
		
		// Build the summarised SNP information: CHROM	SNP	MQ;HQb;DP;RefAlt
		return parts[0] + "\t" + parts[1] + "\t" + mq + ";" + hqDepth + ";" + depth + ";" + parts[3] + parts[4];
		
	}
	
	public static String buildOutputLine(VcfFileBufferedReader[] readers, String[] fileLines, String chrom){
		
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
		String outputLine = "";
		
		// If all the SNPs are the same across the files then
		if(allSame(snps) == 1){
			
			// Create the start of the SNP information line
			outputLine = chrom + "\t" + snps[0] + "\t";
			
			// Then add the SNP coverage information from each sample's VCF file
			for(int i = 0; i < readers.length; i++){
				
				// Check the VCF file line isn't null (i.e. reached end of the that file - none of the remaining SNPs are present)
				if(fileLines[i] != null){
					
					// Add the SNP coverage info from the current VCF file line
					outputLine = outputLine + fileLines[i].split("\t")[2];
				
					// If not reading the last VCF file add separator
					if(i < readers.length - 1){
						outputLine = outputLine + ":";
					}
				
					// Set the shift for the current VCF file - ready to move to the next line
					readers[i].setShift(1);
				}else{
					
					// If reached end of VCF file then insert blank info for the samples
					outputLine = outputLine + blank;
					
					// If not reading the last VCF file add separator
					if(i < readers.length - 1){
						outputLine = outputLine + ":";
					}
					
					// Set the shift for the current VCF file - reached end no point in shifting
					readers[i].setShift(0);
				}
			}			
		}else{ // If the SNPs aren't the same
			
			// Find the min SNP with information in one of the VCF files
			min = ArrayMethods.min(snps);
			
			// Create the start of the SNP Information line
			outputLine = chrom + "\t" + min + "\t";
			
			// Examine each of the lines from the VCF file
			for(int i = 0; i < readers.length; i++){
				
				// If the SNP in the info from the current VCF file is equal to the min then add its information in
				if(snps[i] == min && fileLines[i] != null){
					
					// Add the SNP coverage information from the current VCF file in
					outputLine = outputLine + fileLines[i].split("\t")[2];
				
					// If not reading the last VCF file add separator
					if(i < readers.length - 1){
						outputLine = outputLine + ":";
					}
					
					// Set the shift for the current VCF file - used the information in this line, move to next
					readers[i].setShift(1);
				}else{
					
					// SNP for current VCF file isn't equal to the min (means min SNP didn't have coverage in the current sample) - add in blank insert
					outputLine = outputLine + blank;
					
					// If not reading the last VCF file add separator
					if(i < readers.length - 1){
						outputLine = outputLine + ":";
					}
					
					// Set the shift for the current VCF file - didn't use the information in this line don't move until this information has been used
					readers[i].setShift(0);
				}
			}	
		}
		
		// Return the SNP Information line
		return outputLine;
	}
	
	public static void closeVcfFiles(VcfFileBufferedReader[] readers) throws IOException{
		
		for(VcfFileBufferedReader reader : readers){
			reader.getBfReader().close();
		}
	}
	
	// Combining Merged.txt files methods
	
	public static void combineMergedFiles(String[] fileNames) throws IOException{
		
		// Open the Merged.txt files
		MergedFileBufferedReader[] readers = openAllMergedFiles(fileNames);
		
		// Get the first set of SNP Information lines
		String[] lines = returnNextLineFromEachFile(readers, new String[readers.length]);
		
		// Get the CHROM id for the SNPs
		String chrom = lines[0].split("\t")[0];
		
		// Print out the Header and Fields Lines
		System.out.println(buildHeaderAndFields(readers));
		
		// Read the rest of the SNP information lines
		while(checkIfFinished(lines) == 0){ // Check that haven't reached the end of all the files
			
			// Combine the SNP Information lines from each merged.txt file into a single output line for each SNP
			System.out.println(buildOutputLine(readers, lines, chrom));
			
			// Get the next set of lines from the merged.txt lines
			lines = returnNextLineFromEachFile(readers, lines);
			
		}
	}
	
	public static String buildHeaderAndFields(MergedFileBufferedReader[] readers){
		
		// Here creating the Header and Fields part of VCF
		
		// Add the Header Information and the start of the Fields line
		String output = readers[0].getHeader() + "\n" + "#CHROM\tPOS\t";
		
		// Add the sample Names from each of the merged.txt files
		for(int i = 0; i < readers.length; i++){
			output = output + combineSampleNames(readers[i].getSampleNames());
			
			if(i < readers.length - 1){
				output = output + ":";
			}
		}
		
		// Return the Header and Fields as a string 
		return output;		
	}
	
	public static MergedFileBufferedReader[] openAllMergedFiles(String[] fileNames) throws IOException{
		
		// Open each of the merged.txt files and store them as accessible BufferedReaders
		
		// Initialise an Array to store the BufferedReaders
		MergedFileBufferedReader[] readers = new MergedFileBufferedReader[fileNames.length];
		
		// Initialise objects for opening and reading files
		InputStream input;
		BufferedReader bfReader;
		
		// Open all the files and store them
		for(int i = 0; i < fileNames.length; i++){
			
			// Open the current merged.txt file
			input = new FileInputStream(fileNames[i]);
			bfReader = new BufferedReader(new InputStreamReader(input));
			
			// Store the BufferedReader for the current merged.txt file
			readers[i] = new MergedFileBufferedReader(bfReader, 1);			
		}
		
		return readers;		
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
	
	public static String buildOutputLine(MergedFileBufferedReader[] readers, String[] fileLines, String chrom){
		
		/**
		 * Here the SNP information from each of the merged.txt needs to be combine into a single SNP information in the output
		 * 	The problem is that not all of the merged.txt files will have all the same SNPs. But they are all ordered by the SNP. 
		 * 	Therefore as we investigate each merged.txt file in parallel - if the is a SNP present in some but absent in others then for those where
		 * 	it is absent a blank must be put in that set of Samples place.
		 */
		
		// Get the SNP that each of the lines (from the merged.txt file) is associated with
		int[] snps = getSNP(fileLines);
		
		// Initialise variable in case some SNPs differ - only deal with minimum, then re-evaluate each merged.txt (move to next line if necessary)
		int min;
		
		// Initialise the output line
		String outputLine = "";
		
		// If all the SNPs are the same across the files then
		if(allSame(snps) == 1){
			
			// Create the start of the SNP information line
			outputLine = chrom + "\t" + snps[0] + "\t";
			
			// Then add the sample Information for each of the sets within the merged.txt files
			for(int i = 0; i < readers.length; i++){
				
				// Check the merged.txt line isn't null (i.e. reached end of the that file - none of the remaining SNPs are present)
				if(fileLines[i] != null){
					
					// Add the sample info from the current merged.txt file line
					outputLine = outputLine + fileLines[i].split("\t")[2];
				
					// If not reading the last merged.txt file add separator
					if(i < readers.length - 1){
						outputLine = outputLine + ":";
					}
				
					// Set the shift for the current merged.txt file - ready to move to the next line
					readers[i].setShift(1);
				}else{
					
					// If reached end of merged file then insert blank info for the samples
					outputLine = outputLine + readers[i].getBlankInfo4Samples();
					
					// If not reading the last merged.txt file add separator
					if(i < readers.length - 1){
						outputLine = outputLine + ":";
					}
					
					// Set the shift for the current merged.txt file - reached end no point in shifting
					readers[i].setShift(0);
				}
			}			
		}else{ // If the SNPs aren't the same
			
			// Find the min SNP with information in one of the merged.txt files
			min = ArrayMethods.min(snps);
			
			// Create the start of the SNP Information line
			outputLine = chrom + "\t" + min + "\t";
			
			// Examine each of the lines from the merged.txt file
			for(int i = 0; i < readers.length; i++){
				
				// If the SNP in the info from the current merged.txt file is equal to the min then add its information in
				if(snps[i] == min && fileLines[i] != null){
					
					// Add the sample information from the current merged.txt file in
					outputLine = outputLine + fileLines[i].split("\t")[2];
				
					// If not reading the last merged.txt file add separator
					if(i < readers.length - 1){
						outputLine = outputLine + ":";
					}
					
					// Set the shift for the current merged.txt file - used the information in this line, move to next
					readers[i].setShift(1);
				}else{
					
					// SNP for current merged.txt file isn't equal to the min (means min isn't present in the current merged.txt file) - add in blank insert
					outputLine = outputLine + readers[i].getBlankInfo4Samples();
					
					// If not reading the last merged.txt file add separator
					if(i < readers.length - 1){
						outputLine = outputLine + ":";
					}
					
					// Set the shift for the current merged.txt file - didn't use the information in this line don't move until this information has been used
					readers[i].setShift(0);
				}
			}	
		}
		
		// Return the SNP Information line
		return outputLine;
	}
	
	public static String combineSampleNames(String[] sampleNames){
		
		// Combine the array of sample names found in each of the merged.txt files into a single sample names string
		
		// Initialise a string to store the concatenated sample names
		String output = sampleNames[0];
		
		// Combine each of the sample names with a ":" separator
		for(int i = 1; i < sampleNames.length; i++){
			output = output + ":" + sampleNames[i];
		}
		
		// Return the concatenated sample names string
		return output;
	}

	
	// General Methods
	
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
}
