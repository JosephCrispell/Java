package newProcessingSequencingData;

import java.io.IOException;

import methods.ArrayMethods;
import methods.CalendarMethods;

public class ExamineINDELS {
	
	public static void main(String[] args) throws IOException {
		
		// Get the current date
		String date = CalendarMethods.getCurrentDate("dd-MM-yyyy");
		
		// Set the path variable
		String path = "/home/josephcrispell/Desktop/Research/RepublicOfIreland/Fastqs_MAP/Testing/vcfFiles/";
		
		// Find the VCF Files
		String[] vcfFileNames = MergeVcfFiles.findVcfFilesInDirectory(path);
		
		// Examine each of the VCF files
		for(String file : vcfFileNames){
			
			System.out.println("Examining " + file + "...");
			
			// Open the VCF file
			VcfFile reader = ExamineHeterozygousSites.openVcfFile(file, path);
			
			// Read each line of the VCF file and search for INDELs
			searchForINDELs(reader);
		}
		
	}
	
	public static void searchForINDELs(VcfFile reader) throws IOException {
		
		// Initialise variables necessary for parsing the file
		String line = null;
		String[] cols;
		
		// Begin reading the file
		while(( line = reader.getBfReader().readLine()) != null){
		
			// Split the current line into it's columns
			cols = line.split("\t");
			
			// Skip line if no alternate present
			if(cols[4].matches("\\.")) {
				continue;
			}
			
			// Check for Insertion - Alternate allele will be multiple characters
			// Note that there can be multiple alleles - need to check each one (they are separated by ",")
			if(cols[7].matches("INDEL(.*)")) {
				for(String alt : cols[4].split(",")) {
					
					System.out.print("INDEL\t");
					
					if(cols[3].length() > alt.length()) {
						System.out.print("Deletion\t" + 
								"Position: " + cols[1] + "\t" + cols[3] + "\t" + alt + "\n");
					}else if(alt.length() > cols[3].length()){
						System.out.print("Insertion\t" + 
								"Position: " + cols[1] + "\t" + cols[3] + "\t" + alt + "\n");
					}
				}
			}

		}

	}
}
