package molecularTyping;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;

import geneticDistances.GeneticDistances;
import geneticDistances.Sequence;
import methods.ArrayMethods;
import methods.CalendarMethods;
import methods.GeneralMethods;
import methods.HashtableMethods;
import methods.MatrixMethods;
import methods.WriteToFile;

public class Spoligotyping {

	public static void main(String[] args) throws IOException {
		
		// Get the date
		String date = CalendarMethods.getCurrentDate("dd-MM-yyyy");
		
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/NewAnalyses_13-07-17/Mislabelling/Spoligotyping/";
		
		// Get the reference spacer sequences to search for
		String spacerSequencesFile = path + "Xia2016-43SpacerSequences-25bp.fasta";
		Sequence[] sequences = GeneticDistances.readFastaFile(spacerSequencesFile);
		char[][] spacerReverseCompliments = getReverseComplimentOfSpacers(sequences);
		
		// Read in the spoligotype conversion table
		String spoligotypeConversionTable = path + "SpoligotypeConversionTable_17-01-17.txt";
		Hashtable<String, int[]> spoligotypeBinaryCodes = readSpoligotypeConversionTable(spoligotypeConversionTable);
				
		// Read in the spoligotyping region information
		String annotationFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Reference/TransferAnnotations/" + 
		"LT708304.1_AF2122-97_Malone2017_rmPrefixToLocusTag_Inc-repeats-mobile_DamiensComments_04-09-17.gff";
		int[][] spacerStartEnds = getSpacerRegionStartEnds(annotationFile,
				"feature	CDS	3079186	307999",
				"feature	gene	3084599	3084940");
		
		// Find the VCF files in the directory
		String vcfFileDirectory = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/NewAnalyses_13-07-17/vcfFiles/";
		String[] filesInDirectory = GeneralMethods.getAllFilesInDirectory(vcfFileDirectory, ".vcf.gz");
		
		// Get the previous spoligotype information if available
		String isolateDataPath = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester_CattleAndBadgers/NewAnalyses_13-07-17/IsolateData/";
		String badgerFile = isolateDataPath + "BadgerInfo_08-04-15_LatLongs_XY_Centroids.csv";
		String cattleFile = isolateDataPath + "CattleIsolateInfo_LatLongs_plusID_outbreakSize_Coverage_AddedStrainIDs.csv";
		Hashtable<String, String> isolateTypes = getBadgerIsolateSpoligotypes(badgerFile);
		getCattleIsolateSpoligotypes(cattleFile, isolateTypes);
		
		// Note the start and end of the Spoligotyping region - taken directly from annotation file
		int[] regionOfInterest = {3080147, 3084550};
		
		// Open an output file
		String outputFile = path + "SpoligotypeMatches_" + date + ".txt";
		BufferedWriter bWriter = WriteToFile.openFile(outputFile, false);
		
		// Add a header
		String[] spoligotypes = HashtableMethods.getKeysString(spoligotypeBinaryCodes);
		WriteToFile.writeLn(bWriter, "File\tAssignedSpoligotype\tAverageDepth\tProportionNs\t" + ArrayMethods.toString(spoligotypes, "\t"));
				
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
			
			char[] sequence = getNucleotideSequence(vcfInfo, 
					mappingQualityThreshold, highQualityBaseDepthThreshold, readDepthThreshold,
					alleleSupportThreshold, qualityScoreThreshold, fqThreshold, regionOfInterest);
			
			// Get the nucleotide sequences of the spacers
			char[][] isolateSpacerSequences = getSpacerSequences(sequence, spacerStartEnds, regionOfInterest[0]);
			
			// Find which spacer sequences are present
			int mismatchThreshold = 0;
			int[] foundSpacer = searchForReferenceSpacersInIsolateSpacerRegions(spacerReverseCompliments, 
					isolateSpacerSequences, mismatchThreshold);
			
			// Note which spoligotype was present
			int[][] spacerInfo = searchSpoligotypeBinaryCodes(spoligotypeBinaryCodes, foundSpacer);
						
			// Note the spoligotype provided if found
			String[] parts = vcfFileName.split("/");
			String isolateID = parts[parts.length - 1].split("_")[0];
			if(isolateTypes.get(isolateID) != null){
				WriteToFile.write(bWriter, parts[parts.length - 1] + "\t" + isolateTypes.get(isolateID) + 
						"\t" + vcfInfo.getAverageDepth() + "\t" + vcfInfo.getRegionCoverage());
			}else{
				WriteToFile.write(bWriter, parts[parts.length - 1] + "\tNA\t" + vcfInfo.getAverageDepth() + 
						"\t" + vcfInfo.getRegionCoverage());
			}			
			
			// Print the match information
			for(int i = 0; i < spoligotypes.length; i++){

				WriteToFile.write(bWriter, "\t" + spacerInfo[i][0] + ":" + spacerInfo[i][1]);
			}
			WriteToFile.write(bWriter, "\n");
		}
		
		// Close the output file
		WriteToFile.close(bWriter);
	}
	
	public static void getCattleIsolateSpoligotypes(String fileName, Hashtable<String, String> isolateSpoligotypes) throws IOException{
		
		/**
		 * Cattle Isolate Sample Information File Structure:
		 * 	CPH_10km	SampleRef	CultureResult	DateCultured	ReasonForSlaughter	SkinTestType
		 * 	0			1			2				3				4					5
		 * 
		 * 	Species	DeerType	Mapx	Mapy	MapRef	LesionsFound	Eartag	Database	CPH	CPHH
		 * 	6		7			8		9		10		11				12		13			14	15
		 * 
		 * 	BreakdownID	County	BadgerInvestigationNo	Year	Spoligotype	Genotype	ComplVNTR	VNTR
		 * 	16			17		18						19		20			21			22			23
		 * 
		 * 	Profile2	VNStatus	VNPtA	VNPtB	VNPtC	VNPtD	VNPtE	VNPtF	VNPtG	VNPtH	VNPtI
		 * 	24			25			26		27		28		29		30		31		32		33		34
		 * 
		 * 	Rawtag	BreakYr	VLA.Genotype	GenotypeFirst	GenotypeSecond	GenotypeThird	TypingYear
		 * 	35		36		37				38				39				40				41
		 * 
		 * 	Name	Location	Expected_Genotype1	Expected_Genotype2	Expected_Genotype3
		 * 	42		43			44					45					46
		 * 
		 * 	Expected_Genotype4	Expected_Genotype5	Latitude	Longitude	StrainId	OutbreakSize
		 * 	47					48					49			50			51			52
		 * 
		 * 	Coverage
		 * 	53
		 */
		
		// Open the input file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise the necessary variables for parsing the file
		String line;
		String[] cols;
				
		// Begin reading the input file
		while(( line = reader.readLine()) != null){
					
			// Skip the header line
			if(line.matches("CPH_10km(.*)") == true){
				continue;
			}
					
			// Split the current line into its columns
			cols = line.split(",", -1);
			
			// Store Spoligotype for the current isolate
			isolateSpoligotypes.put(cols[51], cols[21]);
		}

		// Close the input file
		input.close();
		reader.close();
	}
	
	public static Hashtable<String, String> getBadgerIsolateSpoligotypes(String fileName) throws IOException{
		
		/**
		 * Badger Isolate Sample Information File Structure:
		 * 	WB_id	CB_id	Batch	tattoo	date	pm	sample	AHVLA_afno	lesions	abscesses	
		 * 	0		1		2		3		4		5	6		7			8		9
		 * 	
		 * 	AHVLASpoligo	Social.Group.Trapped.At	AFBI_VNTRNo	AFBI_String	AFBI_Genotype	
		 * 	10				11						12			13			14
		 * 	
		 * 	AFBI_Spoligotype	AFBI_GenSpol	notes	SampledGrpLat	SampledGrpLong	SampledGrpX	
		 * 	15					16				17		18				19				20
		 * 	
		 * 	SampledGrpY
		 * 	21
		 */
		
		// Open the input file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));

		// Initialise a hashtable to store the badger isolate spoligotypes
		Hashtable<String, String> isolateSpoligotypes = new Hashtable<String, String>();
		
		// Initialise the necessary variables for parsing the file
		String line;
		String[] cols;
				
		// Begin reading the input file
		while(( line = reader.readLine()) != null){
					
			// Skip the header line
			if(line.matches("WB_id(.*)") == true){
				continue;
			}
					
			// Split the current line into its columns
			cols = line.split(",", -1);
			
			// Store Spoligotype for the current isolate
			String type = "SB0" + cols[15];
			if(cols[15].matches("0")){
				type = "NA";
			}
			isolateSpoligotypes.put(cols[0], type);
		}

		// Close the input file
		input.close();
		reader.close();
		
		return isolateSpoligotypes;				
	}
	
	public static int[][] searchSpoligotypeBinaryCodes(Hashtable<String, int[]> spoligotypeBinaryCodes, 
			int[] binaryCode){
		
		// Get an array of all the spoligotype names
		String[] spoligotypes = HashtableMethods.getKeysString(spoligotypeBinaryCodes);
		
		// Create an array to record how different the current binary code is from those for the different spoligotypes
		int[][] matchingInfo = new int[spoligotypes.length][2];

		// Examine each of the spoligotypes by comparing the binary codes
		for(int i = 0; i < spoligotypes.length; i++){
			
			matchingInfo[i] = compareBinaryCodes(spoligotypeBinaryCodes.get(spoligotypes[i]), binaryCode);
		}
		
		return matchingInfo;
	}
	
	public static int[] compareBinaryCodes(int[] reference, int[] a){
		
		/**
		 * A binary code is used to record which of the 43 spacers is present:
		 * 		   0100011100000010111010000111100101010100110
		 * 		   0100011000000010101010010101100101010000110		4 missing 1 mismatch
		 * 
		 * If the you have: 1 there is a mismatch. 0 Means the VCF doesn't match the current spoligotype
		 * 				    0					   1
		 */
		
		// Create a variable to record the number of mismatches
		int nSpacersMissing = 0;
		int nSpacerMismatches = 0;
		
		// Examine each of the 43 spacers
		for(int i = 0; i < reference.length; i++){
			
			// Check the presence of the current spacer in the VCF and current spoligotype
			if(reference[i] != a[i] && reference[i] == 1){
				nSpacersMissing++;
			
			// Break out if spacer present in VCF and not in current spoligotype
			}else if(reference[i] != a[i] && reference[i] == 0){
				nSpacerMismatches++;
			}
		}
		
		// Create an output array
		int[] output = new int[2];
		output[0] = nSpacersMissing;
		output[1] = nSpacerMismatches;
		
		return output;
	}
	
	public static Hashtable<String, int[]> readSpoligotypeConversionTable(String fileName) throws IOException{
		
		// Open the input file
		InputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));

		// Initialise the necessary variables for parsing the VCF file
		String line;
		String[] cols;
		
		// Initialise a hashtable to store the spoligotype
		Hashtable<String, int[]> spoligotypeBinaryCodes = new Hashtable<String, int[]>();
		
		// Begin reading the input file
		while(( line = reader.readLine()) != null){
			
			// Skip the header line
			if(line.matches("SB Number(.*)") == true){
				continue;
			}
			
			// Split the current line into its columns
			cols = line.split("\t");
			
			// Store Binary code for the spoligotype along with its number
			spoligotypeBinaryCodes.put(cols[0], ArrayMethods.convertToInteger(cols[1].split("")));
		}

		// Close the input file
		input.close();
		reader.close();
		
		return spoligotypeBinaryCodes;
	}
	
	public static int[] searchForReferenceSpacersInIsolateSpacerRegions(char[][] spacerReverseCompliments, 
			char[][] isolateSpacerSequences, int mismatchThreshold){
		
		/**
		 * Spoligotype region is a region where a 36bp sequence is repeated. Spacer sequences
		 * between the repeats are different and used to define spoligotypes.
		 * 43 known spacer sequences are used to define spoligotypes.
		 * 		Repeat	Spacer
		 * 		AGTCGTC--------AGTCGTC-------AGTCGTC--------AGTCGTC-------AGTCGTC-------AGTCGTC
		 * This method takes the spacer regions present in a VCF file and compares them each to the
		 * 43 known spacers sequences to create a binary code:
		 * 		0000101110110011111010011011001010101011001
		 */
		
		// Initialise an array to note the results of the comparison between a VCF spacer region and 1 known sequence
		int[] searchResult;
		
		// Initialise an array to record which of the 43 known spacer sequences were found
		int[] foundSpacer = new int[spacerReverseCompliments.length];

		// For each spacer sequence in the VCF, compare it to all the known 43 spacer sequences
		for(int isolateSpacerIndex = 0; isolateSpacerIndex < isolateSpacerSequences.length; isolateSpacerIndex++){
			for(int spacerReverseComplimentIndex = 0; spacerReverseComplimentIndex < spacerReverseCompliments.length; spacerReverseComplimentIndex++){
				
				// Make the comparison between the current VCF region and the current known spacer
				searchResult = searchSequenceForSpacer(isolateSpacerSequences[isolateSpacerIndex], 
						spacerReverseCompliments[spacerReverseComplimentIndex], mismatchThreshold);
				
				// Record whether the current VCF region matches to the current knwon spacer sequence
				if(searchResult[0] != -1){
					foundSpacer[spacerReverseComplimentIndex] = 1;
					break;
				}
			}
		}
		
		return foundSpacer;
	}
	
	public static void printAlignment(char[] sequence, char[] spacer, int start){
		
		// Method to produce a nucleotide alignment
		
		// Print the reference sequence
		System.out.println(ArrayMethods.toString(sequence, ""));
		
		// Pad left
		for(int i = 0; i < start; i++){
			System.out.print("-");
		}
		
		// Print spacer
		System.out.print(ArrayMethods.toString(spacer, ""));
		
		// Pad right
		for(int i = start + spacer.length; i < sequence.length; i++){
			System.out.print("-");
		}
		System.out.println();
	}
	
	public static int[] searchSequenceForSpacer(char[] sequence, char[] spacer, int mismatchThreshold){
		
		/**
		 * Method to check whether a known spacer sequence is present within a VCF region
		 * 
		 * sequence = AGGGTCCTAGTTGGGCTTT
		 * spacer = CCTAGT
		 * 1:
		 * 		AGGGTCCTAGTTGGGCTTT
		 * 		CCTAGT					number mismatches = 6
		 * 2:
		 * 		AGGGTCCTAGTTGGGCTTT
		 * 		 CCTAGT					number mismatches = 6
		 * 3:
		 * 		AGGGTCCTAGTTGGGCTTT
		 * 		  CCTAGT				number mismatches = 4
		 * 4:
		 * 		AGGGTCCTAGTTGGGCTTT
		 * 		   CCTAGT				number mismatches = 6
		 * 5:
		 * 		AGGGTCCTAGTTGGGCTTT
		 * 		    CCTAGT				number mismatches = 5
		 * 6:
		 * 		AGGGTCCTAGTTGGGCTTT
		 * 		     CCTAGT				number mismatches = 0 ---> Break if less than threshold
		 */
		
		// Initialise a variable to count the number of mismatches in an alignment
		int nMismatches;
		
		// Initialise an array to store where the alignment begins and the number of mismatches
		int[] result = {-1, 0}; // Start index, nMismatches
		
		// Start the alignment at each position of the input sequence - VCF region
		for(int i = 0; i < sequence.length; i++){
			
			// Count the mismatches for the current alignment
			nMismatches = countMismatches(sequence, spacer, i);
			
			// Finish if mismatches is below a certain threshold
			if(nMismatches <= mismatchThreshold){
				result[0] = i;
				result[1] = nMismatches;
				break;
			}			
		}
		
		return result;
	}
	
	public static int countMismatches(char[] sequence, char[] pattern, int start){
		
		/**
		 * Count the number of mismatches in an alignment:
		 * 		TGTTTGTGGTTACGTGT
		 * 		           ACTGGTGGT -> 5 mismatches
		 */
		
		// Initialise a variable to record the number of mismatches
		int nMismatches = 0;
		
		// Each each position of the alignment
		for(int i = start; i < start + pattern.length; i++){
			
			// Add mismatches if the pattern overlaps the end of the alignment
			if(i == sequence.length){
				nMismatches = nMismatches + (pattern.length - (sequence.length - start));
				break;
			}
			
			// Note whether a mismatch is present at the current site
			if(sequence[i] != pattern[i-start]){
				nMismatches++;
			}
		}
		
		return nMismatches;
	}
	
	public static char[][] getReverseComplimentOfSpacers(Sequence[] sequences){
		
		// Initialise an array to store the reverse complimented spacer sequences
		char[][] reverseCompliments = new char[sequences.length][];
		
		// Convert each sequence
		for(int i = 0; i < sequences.length; i++){
						
			// Get the reverse
			reverseCompliments[i] = ArrayMethods.reverse(sequences[i].getSequence());
			
			// Get the compliment
			reverseCompliments[i] = getSequenceComplement(reverseCompliments[i]);
		}
		
		return reverseCompliments;
	}
	
	public static char[] getSequenceComplement(char[] sequence){
		
		// Define the nucleotide compliments
		Hashtable<Character, Character> nucleotidePairs = new Hashtable<Character, Character>();
		nucleotidePairs.put('A', 'T');
		nucleotidePairs.put('T', 'A');
		nucleotidePairs.put('C', 'G');
		nucleotidePairs.put('G', 'C');
		
		// Initialise an array to store the sequence compliment
		char[] compliment = new char[sequence.length];
		
		// Create the sequence compliment
		for(int i = 0; i < sequence.length; i++){
			compliment[i] = nucleotidePairs.get(sequence[i]);
		}
		
		return compliment;
	}
	
	public static char[][] getSpacerSequences(char[] sequence, int[][] spacerStartEnd, int regionStart){
		
		// Initialise an array to store the spacer sequences
		char[][] spacerSequences = new char[spacerStartEnd.length][0];
		
		// Get each spacer sequence - based upon its start and end position within a region
		for(int i = 0; i < spacerStartEnd.length; i++){
			spacerSequences[i] = ArrayMethods.subset(sequence, spacerStartEnd[i][0] - regionStart,
					spacerStartEnd[i][1] - regionStart);
		}
		
		return spacerSequences;
	}
	
	public static int[][] getSpacerRegionStartEnds(String annotationFile,
			String lineBeforeSpacerBlock,
			String lineAfterSpacerBlock) throws IOException{
		
		// Open the M. bovis annotations file
		InputStream input = new FileInputStream(annotationFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));

		// Initialise an matrix to store the start and end positions of each spacer region
		int[][] spacerStartEnds = new int[41][2];
		int spacerIndex = -1;
		
		// Initialise the necessary variables for parsing the file
		String line;
		String[] cols;
		boolean foundSpoligotypingAnnotations = false;
		boolean inIS6110 = false;
		
		// Initialise a variable to store the end of the previous repeat region
		int endOfPreviousRepeat = -1;
		int start;
		int end;
		
		while(( line = reader.readLine()) != null){
			
			// Look for the start of the spoligotype repeat region annotations
			if(line.matches("(.*)" + lineBeforeSpacerBlock + "(.*)") == true){
				foundSpoligotypingAnnotations = true;
				continue;
			}
			
			// Have we reached the end of the annotations?
			if(line.matches("(.*)" + lineAfterSpacerBlock + "(.*)") == true){
				break;
			}
			
			// Are we in the right part of the file?
			if(foundSpoligotypingAnnotations == true){
				
				// Split the line into its columns
				cols = line.split("\t");
				
				// Get the start and end of the current repeat region
				start = Integer.parseInt(cols[3]);
				end = Integer.parseInt(cols[4]);
				
				// If a previous repeat region exists, note the start and end of the spacer between
				if(endOfPreviousRepeat != -1 && inIS6110 == false){
					spacerIndex++;
					spacerStartEnds[spacerIndex][0] = endOfPreviousRepeat + 1;
					spacerStartEnds[spacerIndex][1] = start - 1;
				}
				
				// Note the end of the current repeat region
				endOfPreviousRepeat = end;
			}
			
			// Check if in the IS6110 element
			if(inIS6110 == false && line.matches("(.*)part direct repeat(.*)") == true){
				inIS6110 = true;
			}else if(inIS6110 == true && line.matches("(.*)part direct repeat(.*)") == true){
				inIS6110 = false;
			}
			
		}

		// Close the VCF file
		input.close();
		reader.close();
		
		return spacerStartEnds;
	}
	
	public static double[] calculateProportionHighQualityBasesSupportingRefAndAltAlleles(double[] highQualityBaseDepth){
		
		// Initialise an array to store the calculate proprotions
		double[] proportions = new double[2];
		double sum = ArrayMethods.sum(highQualityBaseDepth);
		
		// Calculate the proportion supporting the reference allele
		if(highQualityBaseDepth[0] + highQualityBaseDepth[1] != 0){
			proportions[0] = highQualityBaseDepth[0] + highQualityBaseDepth[1] / sum;
		}
		
		// Calculate the proportion supporting the alternate allele
		if(highQualityBaseDepth[2] + highQualityBaseDepth[3] != 0){
			proportions[0] = highQualityBaseDepth[2] + highQualityBaseDepth[3] / sum;
		}
		
		return proportions;
	}
	
	public static boolean checkSufficientSequencingDataAvailable(SnpInfo positionInfo){
		
		boolean result = true;
		
		if(positionInfo.getInfoCol().get("DP4") == null){
			result = false;
		}
		
		if(positionInfo.getInfoCol().get("DP") == null){
			result = false;
		}
		
		if(positionInfo.getInfoCol().get("MQ") == null){
			result = false;
		}
		
		if(positionInfo.getInfoCol().get("FQ") == null){
			result = false;
		}

		return result;
	}
	
 	public static char[] getNucleotideSequence(VcfFile vcfInfo,
			double mq, double hqdp, double dp, double sup, double qual, double fq, int[] regionOfInterest){
		
 		Hashtable<Integer, SnpInfo> infoForEachPosition = vcfInfo.getInfoForEachPosition();
 		
 		// Create an initial sequence of Ns
		char[] sequence = ArrayMethods.repeat('N', (regionOfInterest[1] - regionOfInterest[0]) + 1);
		
		// Initialise variables to store the quality information
		SnpInfo positionInfo;
		double[] highQualityBaseDepth;
		double[] alleleSupport;
		String alleleCalled;
		double[] alleleHighQualityBaseDepth = new double[2];
		
		// Initialise a variable to record the average depth at each position
		double depthTotal = 0;
		
		for(int i = regionOfInterest[0]; i <= regionOfInterest[1]; i++){
			
			// Does the position have sequencing quality information available?
			if(infoForEachPosition.get(i) != null){
				
				// Get the quality information for the current position
				positionInfo = infoForEachPosition.get(i);
				
				// Check sufficient quality information exists at the current position
				if(checkSufficientSequencingDataAvailable(positionInfo) == false){
					continue;
				}
				
				// Get the necessary quality information
				highQualityBaseDepth = positionInfo.getInfoCol().get("DP4");
				
				// Calculate proportion high quality bases supporting the reference and alternate allele
				alleleSupport = calculateProportionHighQualityBasesSupportingRefAndAltAlleles(highQualityBaseDepth);
				
				// Note whether reference or alternate allele called
				if(alleleSupport[0] >= alleleSupport[1]){
					alleleCalled = "REF";
					alleleHighQualityBaseDepth[0] = highQualityBaseDepth[0];
					alleleHighQualityBaseDepth[1] = highQualityBaseDepth[1];
				}else{
					alleleCalled = "ALT";
					alleleHighQualityBaseDepth[0] = highQualityBaseDepth[2];
					alleleHighQualityBaseDepth[1] = highQualityBaseDepth[3];
				}
				
				// Add depth at current position to running total
				depthTotal += positionInfo.getInfoCol().get("DP")[0];
				
				// Is the data available, of sufficient quality?
								
				// READ DEPTH FILTER
				if(positionInfo.getInfoCol().get("DP")[0] >= dp){
					
					// HIGH QUALITY BASE DEPTH
					if(alleleHighQualityBaseDepth[0] >= hqdp && alleleHighQualityBaseDepth[1] >= hqdp){
						
						// MAPPING QUALITY
						if(positionInfo.getInfoCol().get("MQ")[0] >= mq){
							
							// ALLELE SUPPORT
							if((alleleCalled.matches("REF") == true && alleleSupport[0] >= sup) ||
									(alleleCalled.matches("ALT") == true && alleleSupport[1] >= sup)){
								
								// QUALITY SCORE
								if(positionInfo.getQualityScore() >= qual){
									
									// FQ VALUE - negative
									if(positionInfo.getInfoCol().get("FQ")[0] <= fq){
										
										// PASSED
										if(alleleCalled.matches("REF") == true){
											sequence[i - regionOfInterest[0]] = positionInfo.getRef();
										}else if(positionInfo.getAlt() != '.'){
											sequence[i - regionOfInterest[0]] = positionInfo.getAlt();
										}
									}
								}
							}
						}
					}
				}
			}
		}
		
		vcfInfo.setAverageDepth((depthTotal / (double) sequence.length));
		vcfInfo.setRegionCoverage(getProportionCoverage(sequence));
		
		return sequence;
	}

 	public static double getProportionCoverage(char[] sequence){
 		
 		// Note nucleotides
 		Hashtable<Character, Integer> nucleotides = new Hashtable<Character, Integer>();
 		nucleotides.put('A', 1);
 		nucleotides.put('C', 1);
 		nucleotides.put('T', 1);
 		nucleotides.put('G', 1);
 		
 		// Examine each position in the sequence
 		int count = 0;
 		for(int i = 0; i < sequence.length; i++){
 			if(nucleotides.get(sequence[i]) == null){
 				count++;
 			}
 		}
 		
 		// Convert count to proportion
 		double proportion = 0;
 		if(count != 0){
 			proportion = (double) count / (double) sequence.length;
 		}
 		
 		return proportion;
 	}
 	
}
