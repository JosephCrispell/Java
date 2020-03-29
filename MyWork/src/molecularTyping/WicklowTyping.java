package molecularTyping;

import java.io.IOException;
import java.util.Hashtable;

import geneticDistances.GeneticDistances;
import geneticDistances.Sequence;
import methods.CalendarMethods;
import methods.GeneralMethods;
import methods.HashtableMethods;

public class WicklowTyping {

	public static void main(String[] args) throws IOException {
		
		// Get the date
		String date = CalendarMethods.getCurrentDate("dd-MM-yyyy");
		
		// Set the path
		String path = "/home/josephcrispell/storage/Research/";
				
		// Get the reference spacer sequences to search for
		String spacerSequencesFile = path + "RepublicOfIreland/Mbovis/Wicklow/Spoligotyping/Xia2016-43SpacerSequences-25bp.fasta";
		Sequence[] sequences = GeneticDistances.readFastaFile(spacerSequencesFile);
		char[][] spacerReverseCompliments = Spoligotyping.getReverseComplimentOfSpacers(sequences);
				
		// Read in the spoligotype conversion table
		String spoligotypeConversionTable = path + "RepublicOfIreland/Mbovis/Wicklow/Spoligotyping/SpoligotypeConversionTable_17-01-17.txt";
		Hashtable<String, int[]> spoligotypeBinaryCodes = Spoligotyping.readSpoligotypeConversionTable(spoligotypeConversionTable);
		String[] spoligotypes = HashtableMethods.getKeysString(spoligotypeBinaryCodes);
		
		// Read in the spoligotyping region information
		String annotationFile = path + "Reference/TransferAnnotations_23-05-18/UpdatedMaloneAnnotations_FINAL_25-05-18.gff";
		int[][] spacerStartEnds = Spoligotyping.getSpacerRegionStartEnds(annotationFile,
				"feature	CDS	3079186	307999",
				"feature	gene	3084599	3084940");
				
		// Find the VCF files in the directory
		String vcfFileDirectory = path + "RepublicOfIreland/Mbovis/Wicklow/vcfFiles/";
		String[] filesInDirectory = GeneralMethods.getAllFilesInDirectory(vcfFileDirectory, ".vcf.gz");
		
		// Note the start and end of the Spoligotyping region - taken directly from annotation file
		int[] regionOfInterest = {3080147, 3084550};
		
		// Examine each vcf
		for(int fileIndex = 0; fileIndex < filesInDirectory.length; fileIndex++){
		
			System.out.println("Spoligotyping: " + filesInDirectory[fileIndex] + ". File " + (fileIndex + 1) +
				" of " + filesInDirectory.length);
		
			// Read in the vcf file and store information for region of interest
			String vcfFileName = vcfFileDirectory + filesInDirectory[fileIndex];
			VcfFile vcfInfo = new VcfFile(vcfFileName, regionOfInterest);
		
			// Get the nucleotide sequence for the region of interest
			double mappingQualityThreshold = 0;
			double highQualityBaseDepthThreshold = 0;
			double readDepthThreshold = 0;
			double alleleSupportThreshold = 0;
			double qualityScoreThreshold = 0;
			double fqThreshold = 0;
		
			char[] sequence = Spoligotyping.getNucleotideSequence(vcfInfo, 
					mappingQualityThreshold, highQualityBaseDepthThreshold, readDepthThreshold,
					alleleSupportThreshold, qualityScoreThreshold, fqThreshold, regionOfInterest);
		
			// Get the nucleotide sequences of the spacers
			char[][] isolateSpacerSequences = Spoligotyping.getSpacerSequences(sequence, spacerStartEnds, regionOfInterest[0]);
		
			// Find which spacer sequences are present
			int mismatchThreshold = 0;
			int[] foundSpacer = Spoligotyping.searchForReferenceSpacersInIsolateSpacerRegions(spacerReverseCompliments, 
					isolateSpacerSequences, mismatchThreshold);
		
			// Note which spoligotype was present
			int[][] spacerInfo = Spoligotyping.searchSpoligotypeBinaryCodes(spoligotypeBinaryCodes, foundSpacer);
			
			// Print the match information
			for(int i = 0; i < spoligotypes.length; i++){

				System.out.print("\t" + spacerInfo[i][0] + ":" + spacerInfo[i][1]);
			}
			System.out.println();
		}

	}
	
}
