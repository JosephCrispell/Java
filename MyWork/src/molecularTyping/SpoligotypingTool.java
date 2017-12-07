package molecularTyping;

import java.io.IOException;
import java.util.Hashtable;

import geneticDistances.GeneticDistances;
import geneticDistances.Sequence;
import methods.ArrayMethods;
import methods.GeneralMethods;
import methods.HashtableMethods;
import methods.MatrixMethods;

public class SpoligotypingTool {

	public static void main(String[] args) throws IOException{
		
		
		/**
		 * A tool to provide an M. bovis isolate's spoligotype.
		 * Can take the following files as input:
		 * 	- VCF file (aligned to AF2122-97)	X
		 *  - FASTA (aligned to AF2122-97)
		 *  - FASTQ file (unaligned)
		 *  
		 *  Note: 	Quality filtering currently turned off
		 *  		Looks for identical spacer primer match
		 *  		Doesn't account for Ns
		 */
		
		// ########## PREPARATION ##########
		
		// Set the path
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/";
		path += "NewAnalyses_02-06-16/InvestigatingCattleMislabelling/Spoligotyping/";
		
		// Get the reference spacer sequences to search for
		String spacerSequencesFile = path + "Xia2016-43SpacerSequences-25bp.fasta";
		Sequence[] sequences = GeneticDistances.readFastaFile(spacerSequencesFile);
		char[][] spacerReverseCompliments = Spoligotyping.getReverseComplimentOfSpacers(sequences);
				
		// Read in the spoligotype conversion table
		String spoligotypeConversionTable = path + "SpoligotypeConversionTable_17-01-17.txt";
		Hashtable<String, int[]> spoligotypeBinaryCodes = Spoligotyping.readSpoligotypeConversionTable(spoligotypeConversionTable);
						
		// ########## INPUT ##########
		
		// Note the input file name --- VCF
//		String fileName = path + "Test/TB1488_S24_80.vcf";
		
		// Note the input file name --- FASTA
//		String fileName = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Reference/" + 
//		"NC_002945.3_AF2122-97.fasta";
		
		// Note the name of the M. bovis AF2122-97 reference annotation file
		String annotationFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Reference/" + 
		"NC_002945_annotation.gff.txt";
		
		// ########## SPOLIGOTYPING ##########
		
		// Set print and mismatch info
		int nToPrint = 1;
		int misMatchCost = 10;
		
//		// Get the spoligotype information for the current isolate
//		spacerCounts = examineSpoligotypeOfVCF(fileName, spacerReverseCompliments, spoligotypeBinaryCodes,
//				annotationFile, nToPrint, misMatchCost, spacerCounts);
//		
//		System.out.println(ArrayMethods.toString(spacerCounts, ", "));
		
		// Find the VCF files in the directory
//		String vcfFileDirectory = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/";
//		vcfFileDirectory += "NewAnalyses_02-06-16/InvestigatingCattleMislabelling/Spoligotyping/Test/";
		String vcfFileDirectory = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/";
		vcfFileDirectory += "NewAnalyses_02-06-16/allVCFs-IncludingPoor/vcfFiles/";
		String[] filesInDirectory = GeneralMethods.getAllFilesInDirectory(vcfFileDirectory, ".vcf");
		
		System.out.println(ArrayMethods.toString(filesInDirectory, "\n"));
		System.out.println();
		
		// Initialise an array to count the number of times each spacer is encountered
		int[] spacerCounts = new int[43];
		
		for(String fileName : filesInDirectory){
			
			spacerCounts = examineSpoligotypeOfVCF(vcfFileDirectory + fileName, spacerReverseCompliments,
					spoligotypeBinaryCodes,	annotationFile, nToPrint, misMatchCost, spacerCounts);
			
			System.out.print(".");
		}
		System.out.println();
		System.out.println("############################################################################################");
		System.out.println();
		System.out.println();
		System.out.println(ArrayMethods.toString(spacerCounts, ", "));
		
	}
	
	public static void printSpoligotypeMatchingInfo(String[] spoligotypes, int[][] spacerInfo, int n, int factor){
		
		// Get the order of the match information - best to worst
		int[] order = ArrayMethods.getOrder(calculateScore(spacerInfo, factor));
		
		// Print the match information
		System.out.println("Printing information for top " + n + " matches...");
		System.out.println("Spoligotype\tNumber Spacers Missing\tNumber Mismatched Spacers");
		for(int i = 0; i < n; i++){
			
			System.out.println(spoligotypes[order[i]] + "\t" + spacerInfo[order[i]][0] + "\t" + spacerInfo[order[i]][1]);
		}
	}
	
	public static int[] calculateScore(int[][] spacerInfo, int factor){
		
		int[] scores = new int[spacerInfo.length];
		
		for(int i = 0; i < spacerInfo.length; i++){
			
			scores[i] = spacerInfo[i][0] + (factor * spacerInfo[i][1]);
		}
		
		return scores;
	}
	
	public static int[] examineSpoligotypeOfVCF(String vcfFileName, char[][] spacerReverseCompliments,
			Hashtable<String, int[]> spoligotypeBinaryCodes, String mBovisReferenceAnnotationFile,
			int nToPrint, int misMatchCost, int[] spacerCounts) throws IOException{
	
		// Note the regions where the spacer sequences will be found - using M. bovis AF2122-97
		int[][] spacerStartEnds = Spoligotyping.getSpacerRegionStartEnds(mBovisReferenceAnnotationFile);
		
		// Note the spoligotyping region
		int[] regionOfInterest = {3075735, 3080138}; // Taken from the annotation file
		
		// Read in the vcf file and store information for region of interest
		VcfFile vcfInfo = new VcfFile(vcfFileName, regionOfInterest);
		
		// Get the nucleotide sequence for the region of interest
		char[] sequence = Spoligotyping.getNucleotideSequence(vcfInfo, 0, 0, 0, 0, 0, 0, regionOfInterest);
		
		// Get the nucleotide sequences of the spacers
		char[][] isolateSpacerSequences = Spoligotyping.getSpacerSequences(sequence, spacerStartEnds, regionOfInterest[0]);
				
		// Find which spacer sequences are present
		int mismatchThreshold = 0;
		int[] foundSpacer = Spoligotyping.searchForReferenceSpacersInIsolateSpacerRegions(spacerReverseCompliments, 
				isolateSpacerSequences, mismatchThreshold);
				
//		// Note which spoligotype was present
//		int[][] spacerInfo = Spoligotyping.searchSpoligotypeBinaryCodes(spoligotypeBinaryCodes, foundSpacer);
//		
//		// Print out the information
//		printSpoligotypeMatchingInfo(HashtableMethods.getKeysString(spoligotypeBinaryCodes), spacerInfo, nToPrint,
//				misMatchCost);
		
		return ArrayMethods.add(spacerCounts, foundSpacer);
	}
	
 	public static void examineSpoligotypeOfFASTA(String fastaFileName, char[][] spacerReverseCompliments,
			Hashtable<String, int[]> spoligotypeBinaryCodes, String mBovisReferenceAnnotationFile,
			int nToPrint, int misMatchCost) throws IOException{
 		
		// Note the regions where the spacer sequences will be found - using M. bovis AF2122-97
		int[][] spacerStartEnds = Spoligotyping.getSpacerRegionStartEnds(mBovisReferenceAnnotationFile);
		
		// Note the spoligotyping region
		int[] regionOfInterest = {3075735, 3080138}; // Taken from the annotation file
		
		// Read in the fasta file 
		Sequence[] sequences = GeneticDistances.readFastaFile(fastaFileName);
		
		// Examine each sequence in the fasta file in turn
		for(int i = 0; i < sequences.length; i++){
			
			// Get the nucleotide sequence for the region of interest
			char[] sequence = ArrayMethods.subset(sequences[i].getSequence(), regionOfInterest[0], regionOfInterest[1]);
			
			// Get the nucleotide sequences of the spacers
			char[][] isolateSpacerSequences = Spoligotyping.getSpacerSequences(sequence, spacerStartEnds, regionOfInterest[0]);
			
			// Find which spacer sequences are present
			int mismatchThreshold = 0;
			int[] foundSpacer = Spoligotyping.searchForReferenceSpacersInIsolateSpacerRegions(spacerReverseCompliments, 
					isolateSpacerSequences, mismatchThreshold);
			// Note which spoligotype was present
			int[][] spacerInfo = Spoligotyping.searchSpoligotypeBinaryCodes(spoligotypeBinaryCodes, foundSpacer);
			
			// Print out the information
			System.out.println("############################################################");
			System.out.println(ArrayMethods.toString(foundSpacer, ""));
			System.out.println("Sequence: " + sequences[i].getName());
			printSpoligotypeMatchingInfo(HashtableMethods.getKeysString(spoligotypeBinaryCodes), spacerInfo, nToPrint,
					misMatchCost);
		}
	}
}
