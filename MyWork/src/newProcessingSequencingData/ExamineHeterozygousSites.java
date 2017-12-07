package newProcessingSequencingData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Hashtable;
import java.util.zip.GZIPInputStream;

import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.WriteToFile;

public class ExamineHeterozygousSites {
	
	public static void main(String[] args) throws IOException {
		
		// Get the current date
		String date = CalendarMethods.getCurrentDate("dd-MM-yyyy");
		
		// Set the path variable
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/TestingPipeline/FASTQs/vcfFiles/";
		
		// Find the VCF Files
		String[] vcfFileNames = MergeVcfFiles.findVcfFilesInDirectory(path);
		
		// Look for Heterozygous sites in each VCF file
		for(String file : vcfFileNames){
			
			System.out.println("Examining " + file + "...");
			
			// Open the VCF file
			VcfFile reader = openVcfFile(file, path);
			
			// Assume that we'll never observe multiple alternates - that would require a SNP occurred on same site
			String[] parts = file.split("_");
			String outputFile = path + "/" + parts[0] + "_" + parts[1] + "_HeterozygousSiteInfo_" + date + ".txt";
			lookForHeterozygousSites(reader, outputFile);
		}	
	}
	
	public static void lookForHeterozygousSites(VcfFile reader, String outputFile) throws IOException{
		/**
		 * Each SNP Information Line in the VCF file is structured:
		 * 
		 *  #CHROM	POS		ID	REF	ALT	QUAL	FILTER	INFO								FORMAT(fields)	(values)	fileName
		 *  runID	1057	.	A	G	222		.		DP=147;VDB=0.0182;DP4=0,0,67,73;...	GT:PL:GQ		1/1:255,255,0:99
		 *  0		1		2	3	4	5		6		7									8				9
		 *  
		 *  
		 *  
		 * 	"\t"	#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT(fields)	FORMAT(values)	
		 * 			0		1	2	3	4	5		6		7		8				9
		 *													|		|				|
		 *	";"						INFO Column Fields <-----	":"	--> Fields		-->	Values
		 *							DP	AF1	AC1	DP4	MQ	FQ				GT	PL	GQ		"/"	","	"|" ""
		 *							0	1	2	3	4	5				0	1	2
		 *										|					   
		 * 	","			DP4 Column Fields <------					   
		 * 				ref-forward	ref-Reverse	alt-forward	alt-reverse
		 * 				0			1			2			3
		 *
		 *-----------------------------------------------------------------------------------------------------------
		 * Rarely more than one alternate allele can be present at a given position. If an alternate allele is called
		 * at a given position then we need to know which of the alternate alleles it was.
		 * The GT (Genotype) variable in the format info tells which allele was called at a given position.
		 * 		0	REF
		 * 		1	1st ALT
		 * 		2	2nd ALT
		 * 		...
		 * 
		 * 	GT	0/0	homozygous REF
		 * 	GT	0/1 heterozygous REF and ALT (diploid)
		 * 	GT 	1/1 homozygous ALT
		 * 
		 * We are not dealing with a diploid organism so only a single allele can be called at a given position.
		 * 
		 * Note that GT can be separated by | if genotype is phased - this is the process by which a haplotype
		 * has been estimated (tried to reconstruct from multiple sequence sources)
		 * 
		 */
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(outputFile, false);
		WriteToFile.writeLn(bWriter, "Position\tGenotype\tAlternateAlleleSupport\tReadDepth");
		
		// Initialise variables to store information about each site
		Hashtable<String, double[]> info;
		Hashtable<String, double[]> format;
		int[] genotype;
		double[] highQualityBaseCounts;
		double altSupport;
		double sum;
		
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
				
		// Begin reading the file
		while(( line = reader.getBfReader().readLine()) != null){
		
			// Split the current line into it's columns
			cols = line.split("\t");
			
			// Check if genotype information exists
			if(line.matches("(.*)\tGT:(.*)") == true){
				
				// Get the format column
				format = SnpInfo.getFormatColInfo(cols[8], cols[9]);
				
				genotype = ArrayMethods.convertDouble2Int(format.get("GT"));
				
				if(genotype[0] != genotype[1]){
					
					// Get the info column
					info = SnpInfo.getInfoColInfo(cols[7]);
					
					// Get the DP4 information from the info column
					altSupport = 0;
					sum = 0;
					if(info.get("DP4") != null){
						highQualityBaseCounts = info.get("DP4");
						
						// Calculate the proportion of High quality bases supporting the alternate allele
						sum = ArrayMethods.sum(highQualityBaseCounts);
						altSupport = (highQualityBaseCounts[2] + highQualityBaseCounts[3]) / sum;
					}
										
					WriteToFile.writeLn(bWriter, cols[1] + "\t" + ArrayMethods.toString(genotype, "/") + "\t" + altSupport + 
							"\t" + sum);
				}
			}
		}
		
		// Close the open VCF file
		reader.getBfReader().close();
		
		// Close the output file
		WriteToFile.close(bWriter);
	}
	
 	public static VcfFile openVcfFile(String fileName, String path) throws IOException{
		
		// Create an VcfFile object
		VcfFile reader;
				
		// Initialise objects for opening and reading files
		InputStream input;
		BufferedReader bfReader = null;
		GZIPInputStream gZipFile;
		
		// Check if file is zipped
		if(fileName.matches("(.*).gz") == true){
			
			// Open the current vcf file
			input = new FileInputStream(path + fileName);
			gZipFile = new GZIPInputStream(input);
			bfReader = new BufferedReader(new InputStreamReader(gZipFile));
			
		}else{
			// Open the current vcf file
			input = new FileInputStream(path + fileName);
			bfReader = new BufferedReader(new InputStreamReader(input));
		}
		
		// Store the VCF file
		reader = new VcfFile(path + fileName, fileName.split("_")[0], bfReader, 1);
		
		return reader;		
	}
}
