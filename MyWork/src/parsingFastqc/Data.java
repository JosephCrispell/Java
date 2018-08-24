package parsingFastqc;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;

import org.uncommons.maths.random.MersenneTwisterRNG;

import methods.ArrayMethods;
import methods.GeneralMethods;
import methods.HashtableMethods;
import methods.MatrixMethods;

public class Data {

	private String fastqcDataFile;
	private boolean verbose;
	
	// Basic statistics
	private String basicStatisticsPass;
	private String file;
	private String encoding;
	private int nReads;
	private int[] readLength;
	private double gcContent;
	
	// Per base sequence quality
	private String perBaseSequenceQualityPass;
	private Table perBaseSequenceQualityTable;
	
	// Per tile sequence quality
	private String perTileSequenceQualityPass;
	private Table perTileSequenceQualityTable;
	
	// Per sequence quality scores
	private String perSequenceQualityScoresPass;
	private Table perSequenceQualityScoresTable;
	
	// Per base sequence content
	private String perBaseSequenceContentPass;
	private Table perBaseSequenceContentTable;
	
	// Per sequence GC content
	private String perSequenceGCContentPass;
	private Table perSequenceGCContentTable;
	private Random random;
	
	// Per base N content
	private String perBaseNContentPass;
	private Table perBaseNContentTable;
	
	// Sequence Length Distribution
	private String sequenceLengthDistributionPass;
	private Table sequenceLengthDistributionTable;
	
	// Sequence Duplication Levels
	// First column: Duplication level
	private String sequenceDuplicationLevelsPass;
	private Table sequenceDuplicationLevelsTable;
	private double totalDeDuplicatedPercentage;
	
	// Overrepresented sequences
	private String overrepresentedSequencesPass;
	private Table overrepresentedSequencesTable;
	
	// Adapter content
	private String adapterContentPass;
	private Table adapterContentTable;
	
	// Kmer content
	private String kmerContentPass;
	private Table kmerContentTable;
	
	public Data(String fastqcResultTableFile, boolean verbose, Random random) throws IOException {
		
		this.verbose = verbose;
		this.random = random;
		
		// Store the file name
		this.fastqcDataFile = fastqcResultTableFile;
		
		// Read the data file and store the information
		readFile();	
	}
	
	// Getting methods
	public String getName() {
		return this.file;
	}
	public String getEncoding() {
		return this.encoding;
	}
	public int getnReads() {
		return this.nReads;
	}
	public int[] getReadLength() {
		return this.readLength;
	}
	public double getGcContent() {
		return this.gcContent;
	}
	
	// Method to build output line
	public String buildOutputLineSummarisingFastQCData(int qualityThreshold, double deviationThreshold,	int nSitesThreshold, int minLength) {
		
		/**
		 *  Print summary line
		 *  
		 *  FileName	Encoding	NumberReads	ReadLength	GC	PropBelowQualityThreshold	LeftTrim	RightTrim
		 *  0			1			2			3			4	5							6			7
		 *  
		 *  PropBelowLengthThreshold	
		 *  10
		 *  
		 *  BasicStatsFlag	PerBaseSeqQualFlag	PerTileSeqQualFlag	PerBaseSeqContentFlag	PerSeqGCContentFlag	PerBaseNContentFlag	SeqLengthDistFlag
		 *  11				12					13					14						15					16					17
		 *  
		 *  SeqDupFlag	OverrepresentedSeqsFlag	AdapterContentFlag	KmerContentFlag
		 *  18			19						20					21
		 *  
		 *  OLD:
		 *  FileName	Encoding	NumberReads	ReadLength	GC	LeftQualTrim	RightQualTrim	PropBelowQualityThreshold	LeftTrim	RightTrim
		 *  0			1			2			3			4	5				6				7							8			9
		 *  
		 *  DiffFromExpectedGC	NPeaksInGCDist	PropBelowLengthThreshold	AdaptersPresent	NKmersAboveConsecutiveBaseThreshold
		 *  10					11				12							13				14
		 *  
		 *  BasicStatsFlag	PerBaseSeqQualFlag	PerTileSeqQualFlag	PerBaseSeqContentFlag	PerSeqGCContentFlag	PerBaseNContentFlag	SeqLengthDistFlag
		 *  15				16					17					18						19					20					21
		 *  
		 *  SeqDupFlag	OverrepresentedSeqsFlag	AdapterContentFlag	KmerContentFlag
		 *  22			23						24					25
		 */
		
		// Initialise an output string
		String output = "";
		
		// Add Basic Statistics info
		output += this.file + "\t" + this.encoding + "\t" + this.nReads + "\t" + ArrayMethods.toString(this.readLength, "-") + "\t" + 
				this.gcContent + "\t";
		if(this.verbose) {
			System.out.println("Name: " + this.file);
			System.out.println("Encoding: " + this.encoding);
			System.out.println("Number of reads: " + this.nReads);
			System.out.println("GC content: " + this.gcContent + "\n\n");
		}
		
		// Add summary information for trimming
//		output += ArrayMethods.toString(checkPerBaseSequenceQuality(qualityThreshold), "\t") + "\t";
		output += checkPerSequenceQualityScores(qualityThreshold) + "\t";
		output += ArrayMethods.toString(checkPerBaseSequenceContent(deviationThreshold, nSitesThreshold), "\t") + "\t";
//		output += ArrayMethods.toString(checkPerSequenceGCContent(expected, diffFromExpectedThreshold, nSearchesForPeaks, mergeDistanceForPeaks, 
//				propSearchesPeakFoundInThreshold), "\t") + "\t";
		output += checkSequenceLengthDistribution(minLength) + "\t";
//		output += checkAdapterContent(meanPerSiteAdapterContentThreshold) + "\t";
//		output += checkKmerContent(nConsecutiveBaseThreshold) + "\t";
		
		// Add FASTQC flags
		output += reportFASTQCFlags();
		
		return output;
	}
	
	// Method to report FASTQC flags
	public String reportFASTQCFlags() {
		
		// Build FASTQC flag string
		String output = this.basicStatisticsPass + "\t" + this.perBaseSequenceQualityPass + "\t" + this.perTileSequenceQualityPass + "\t" + 
				this.perSequenceQualityScoresPass + "\t" + this.perSequenceGCContentPass + "\t" + this.perBaseNContentPass + "\t" + 
				this.sequenceLengthDistributionPass + "\t" + this.sequenceDuplicationLevelsPass + "\t" + this.overrepresentedSequencesPass + "\t" + 
				this.adapterContentPass + "\t" + this.kmerContentPass;
		
		// Print verbose information
		if(this.verbose) {
			System.out.println("FASTQC flags found:");
			if(this.basicStatisticsPass.matches("pass") == false) {
				System.out.println("\tBasic Statistics: " + this.basicStatisticsPass);
			}
			if(this.perBaseSequenceQualityPass.matches("pass") == false) {
				System.out.println("\tPer base sequence quality: " + this.perBaseSequenceQualityPass);
			}
			if(this.perTileSequenceQualityPass.matches("pass") == false) {
				System.out.println("\tPer tile sequence quality: " + this.perTileSequenceQualityPass);
			}
			if(this.perSequenceQualityScoresPass.matches("pass") == false) {
				System.out.println("\tPer sequence quality scores: " + this.perSequenceQualityScoresPass);
			}
			if(this.perBaseSequenceContentPass.matches("pass") == false) {
				System.out.println("\tPer base sequence content: " + this.perBaseSequenceContentPass);
			}
			if(this.perSequenceGCContentPass.matches("pass") == false) {
				System.out.println("\tPer sequence GC content: " + this.perSequenceGCContentPass);
			}
			if(this.perBaseNContentPass.matches("pass") == false) {
				System.out.println("\tPer base N content: " + this.perBaseNContentPass);
			}
			if(this.sequenceLengthDistributionPass.matches("pass") == false) {
				System.out.println("\tSequence Length Distribution: " + this.sequenceLengthDistributionPass);
			}
			if(this.sequenceDuplicationLevelsPass.matches("pass") == false) {
				System.out.println("\tSequence Duplication Levels: " + this.sequenceDuplicationLevelsPass);
			}
			if(this.overrepresentedSequencesPass.matches("pass") == false) {
				System.out.println("\tOverrepresented sequences: " + this.overrepresentedSequencesPass);
			}
			if(this.adapterContentPass.matches("pass") == false) {
				System.out.println("\tAdapter Content: " + this.adapterContentPass);
			}
			if(this.kmerContentPass.matches("pass") == false) {
				System.out.println("\tKmer Content: " + this.kmerContentPass);
			}
		}
		
		return output;
	}
	
	// Methods for calculating trimming parameters
	public static void compareBasicStatistics(Data forward, Data reverse) {
		
		System.out.println("Forward: " + forward.getName() + "\nReverse: " + reverse.getName());
		
		// Check have same encoding
		if(forward.getEncoding().matches(reverse.getEncoding()) == false) {
			System.out.println("ERROR! Forward and reverse reads do not have same encoding." + 
					"\nForward: " + forward.getEncoding() + 
					"\nReverse: " + reverse.getEncoding());
		}else {
			System.out.println("Encoding: " + forward.getEncoding());
		}
		
		// Check have equal number of reads
		if(forward.getnReads() != reverse.getnReads()) {
			System.out.println("Warning! Forward and reverse reads do not have the same number of reads." + 
					"\nForward: " + forward.getnReads() + 
					"\nReverse: " + reverse.getnReads());
		}else {
			System.out.println("Number of reads: " + forward.getnReads());
		}
	}

	public boolean[] checkPerBaseSequenceQuality(int qualityThreshold) {
		
		// Get the max read length
		int maxReadLength = this.readLength[this.readLength.length - 1];
		
		// Initialise an array to store the quality thresholds required on the left and right
		boolean[] output = new boolean[2];
		
		// Get the table values and types
		Value[][] values = this.perBaseSequenceQualityTable.getValues();
		
		// Check positions left of middle
		for(int row = 0; row < values.length; row++) {
			
			// Get the positions (1st column) associated with the current row
			int[] positions = values[row][0].getIntValues();
			
			// Check whether 10th Percentile (6th column) has dropped below 25
			if(values[row][5].getDoubleValue() < qualityThreshold) {
				
				// Check whether passed middle of read
				if(positions[positions.length - 1] > 0.5*maxReadLength) {
					output[1] = true;
				}else {
					output[0] = true;
				}
			}
			
			// Check whether mean (2nd column) drops below threshold
			if(this.verbose && values[row][1].getDoubleValue() < qualityThreshold) {
				System.out.println("Warning! Per base sequence quality: Mean drops below threshold.");
			}
		}
		
		// Report findings
		if(this.verbose && output[0]) {
			System.out.println("Left quality trimming necessary (threshold = " + qualityThreshold + ")");
		}
		if(this.verbose && output[1]) {
			System.out.println("Right quality trimming necessary (threshold = " + qualityThreshold + ")");
		}
		
		return output;
	}

	public double checkPerSequenceQualityScores(int qualityThreshold) {
		
		// Initialise a variable to store proportion of reads below threshold
		double propReadsBelowThreshold = 0;
		
		// Initialise a variable to store the count of reads with quality below threshold
		int sum = 0;
		
		// Get the values in the table
		Value[][] values = this.perSequenceQualityScoresTable.getValues();
		
		// Examine each of the counts for each quality score
		for(int row = 0; row < values.length; row++) {
			
			// Check whether we have reached the quality threshold (quality in 1st column)
			if(values[row][0].getIntValue() >= qualityThreshold) {
				break;
			}
			
			// Keep calculating the sum of read counts (2nd column)
			sum += values[row][1].getDoubleValue();
		}
		
		// Report findings
		if(sum > 0) {
			// Calculate proportion of reads with quality below threshold
			propReadsBelowThreshold = (double) sum / (double) this.nReads;
			
			if(verbose) {
				System.out.println("Apply quality threshold (" + qualityThreshold + "): " + sum + " reads failed (proportion total reads = " + 
					GeneralMethods.round(propReadsBelowThreshold, 2) + ")");
			}			
		}
		
		return propReadsBelowThreshold;
	}
	
	public int[] checkPerBaseSequenceContent(double deviationThreshold, int nSitesThreshold) {
		
		// Initialise an array to store the number of sites to remove from the left and right of the reads
		int[] nSitesToTrim = new int[2];
		
		// Calculate the number of sites to remove from left
		nSitesToTrim[0] = calculateNSitesToTrim(deviationThreshold, nSitesThreshold, false);
		
		// Calculate the number of sites to remove from right
		nSitesToTrim[1] = calculateNSitesToTrim(deviationThreshold, nSitesThreshold, true);
		
		// Report findings
		if(this.verbose && nSitesToTrim[0] > 0) {
			System.out.println("Trim " + nSitesToTrim[0] + " nucleotide(s) from the LEFT of each read.");
		}else if(this.verbose && nSitesToTrim[0] == -1){
			System.out.println("ERROR! Didn't find " + nSitesThreshold + " consecutive sites with deviation < " + deviationThreshold + " on LEFT side.");
		}
		if(this.verbose && nSitesToTrim[1] > 0) {
			System.out.println("Trim " + nSitesToTrim[1] + " nucleotide(s) from the RIGHT of each read.");
		}else if(this.verbose && nSitesToTrim[1] == -1){
			System.out.println("ERROR! Didn't find " + nSitesThreshold + " consecutive sites with deviation < " + deviationThreshold + " on RIGHT side.");
		}
		
		return nSitesToTrim;		
	}
	private int calculateNSitesToTrim(double deviationThreshold, int nSitesThreshold, boolean right) {
		
		// Initialise an array to record how many sites passed from on left or right
		int nSitesWithoutDeviation = 0;
		
		// Initialise an array to store the number of sites to trim off left and right
		int nSitesToTrim = -1;
		
		// Get the table of values
		Value[][] values = this.perBaseSequenceContentTable.getValues();
		
		// Initialise two arrays to store the previous and current positions examined
		int[] positions;
		int[] previousPositions = new int[1];
		if(right) {
			previousPositions[0] = this.readLength[this.readLength.length -1];
		}
		
		// Work in from the left or right
		for(int i = 0; i < values.length; i++) {
			
			// Get current row
			int row = i;
			if(right) {
				row = (values.length - 1) - row;
			}
			
			// Get the positions (1st column) associated with the current row
			positions = values[row][0].getIntValues();
						
			// Get the proportion of each allele from the left side #Base: G:1	A:2	T:3	C:4
			double propA = values[row][2].getDoubleValue();
			double propC = values[row][4].getDoubleValue();
			double propG = values[row][1].getDoubleValue();
			double propT = values[row][3].getDoubleValue();
			
			// Check whether the left A-T proportion difference AND G-C proportion difference are within threshold of deviation
			if(Math.abs(propA - propT) < deviationThreshold && Math.abs(propC - propG) < deviationThreshold) {
			
				// Calculate the number of positions between the current and previous position
				if(right) {
					nSitesWithoutDeviation += previousPositions[0] - positions[0];
				}else {
					nSitesWithoutDeviation += positions[positions.length - 1] - previousPositions[previousPositions.length - 1];
				}
			// Else reset the site counter
			}else {
				nSitesWithoutDeviation = 0;
			}
			
			// Check whether reached nSitesWithoutDeviation threshold for LEFT
			if(nSitesWithoutDeviation >= nSitesThreshold) {
				
				// Calculate n sites to remove for left
				if(right == false && positions[positions.length - 1] - nSitesWithoutDeviation != 0) {
					nSitesToTrim = positions[positions.length - 1] - nSitesWithoutDeviation;
				// For right
				}else if(right && this.readLength[this.readLength.length - 1] - (positions[0] + nSitesWithoutDeviation) != 0){
					nSitesToTrim = this.readLength[this.readLength.length - 1] - (positions[0] + nSitesWithoutDeviation);
				// If none to remove
				}else {
					nSitesToTrim = 0;
				}
				break;
			}
			
			// Make the current positions the previous
			previousPositions = ArrayMethods.copy(positions);
		}
		
		return nSitesToTrim;
	}
	
	public double[] checkPerSequenceGCContent(double expected, double differenceFromExpectedThreshold,
			int nSearchesForPeaks, int mergeDistanceForPeaks, double propSearchesPeakFoundInThreshold) {
		
		// Checked whether observed mean GC content is close to expected
		double diffFromExpected = this.gcContent - expected;
		if(this.verbose && Math.abs(diffFromExpected) > differenceFromExpectedThreshold) {
			System.out.println("Warning! GC content is not within " + differenceFromExpectedThreshold + " of expected value.");
		}
		
		// Get the count values for each GC percentage (2nd column)
		double[] counts = this.perSequenceGCContentTable.getColumnDoubleValues(1);
		
		// Estimate the number of peaks in this vector
		int nPeaks = ArrayMethods.estimateNumberOfPeaks(nSearchesForPeaks, counts, mergeDistanceForPeaks, propSearchesPeakFoundInThreshold, this.random);
		
		// Check if found multiple peaks
		if(this.verbose && nPeaks > 1) {
			System.out.println("Warning! Found " + nPeaks + " peaks in GC content distribution");
		}
		
		// Create output
		double[] output = {diffFromExpected, nPeaks};
		
		return output;
	}
	
	public double checkSequenceLengthDistribution(int minLength) {
		
		// Get the values
		Value[][] values = this.sequenceLengthDistributionTable.getValues();
		
		// Initialise a variable to count how many reads found to be less than threshold length
		int count = 0;
		
		// Examine the read lengths (1st column) observed
		for(int row = 0; row < values.length; row++) {
			
			// Check whether current length is less than threshold
			int[] lengths = values[row][0].getIntValues();
			if(lengths[lengths.length - 1] < minLength) {
				count += values[row][1].getDoubleValue();
			}
		}
		
		// Check if found reads with length less than threshold length
		if(this.verbose && count > 0) {
			System.out.println("Apply sequence length threshold (" + minLength + "): " + count + " shorter reads");
		}
		
		return (double) count / (double) this.nReads;
	}
	
	public boolean checkAdapterContent(double meanPerSiteAdapterContentThreshold) {
		
		// Get the values
		Value[][] values = this.adapterContentTable.getValues();
		
		// Get the column names
		String[] colNames = this.adapterContentTable.getColNames();
		
		// Initialise an array to store mean values
		double[] means = new double[5];
		
		// Calculate mean adapter content for each site for each adapter type reported
		for(int row = 0; row < values.length; row++) {
			
			means[0] += values[row][1].getDoubleValue();
			means[1] += values[row][2].getDoubleValue();
			means[2] += values[row][3].getDoubleValue();
			means[3] += values[row][4].getDoubleValue();
			means[4] += values[row][5].getDoubleValue();
		}
		
		// Finish calculating means
		means = ArrayMethods.divide(means, values.length);
		
		// Check whether any adapters have too higher content
		boolean adaptersPresent = false;
		for(int i = 0; i < 5; i++) {
			
			if(means[i] > meanPerSiteAdapterContentThreshold) {
				
				adaptersPresent = true;
				if(this.verbose) {
					System.out.println("Warning! Adapter (" + colNames[i + 1] + ") has mean per site content (" + 
						means[i] + ") above threshold (" + meanPerSiteAdapterContentThreshold + ")");
				}				
			}
		}
		
		return adaptersPresent;
	}
	
	public int checkKmerContent(int nConsecutiveBaseThreshold) {
		
		// Get the Kmer table
		Value[][] values = this.kmerContentTable.getValues();
		
		// Initialise a variable to count number of kmers flagged with too many consecutive bases
		int nKmersFlagged = 0;
		
		// Check values exist
		if(values != null) {
			
			// Examine each of the kmers found
			for(int row = 0; row < values.length; row++) {
				
				// Get the kmer as a set of nucleotides
				char[] kmer = values[row][0].getStringValue().toCharArray();
				
				// Count the largest number of consecutive nucleotides
				int max = 1;
				int count = 1;
				for(int i = 1; i < kmer.length; i++) {
					if(kmer[i] == kmer[i-1]) {
						count++;
					}else {
						count = 1;
					}
					if(count > max) {
						max = count;
					}
				}
				
				// Check whether identified kmer with > threshold consecutive identical nucleotides
				if(max > nConsecutiveBaseThreshold) {
					nKmersFlagged++;
					if(this.verbose) {
						System.out.println("Warning! Found kmer (" + values[row][0].getStringValue() + ") with > " + nConsecutiveBaseThreshold + 
							" identical consecutive nucleotides. Found in " + (int) values[row][1].getDoubleValue() + " reads.");
					}				
				}			
			}
		}
		
		return nKmersFlagged;
	}
		
	// Methods for reading in the data
	private void readFile() throws IOException {
		
		// Open the input file
		InputStream input = new FileInputStream(this.fastqcDataFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		// Initialise variables to record which block we're in
		boolean basicStatistics = false;
		boolean perBaseSequenceQuality = false;
		boolean perTileSequenceQuality = false;
		boolean perSequenceQualityScores = false;
		boolean perBaseSequenceContent = false;
		boolean perSequenceGCContent = false;
		boolean perBaseNContent = false;
		boolean sequenceLengthDistribution = false;
		boolean sequenceDuplicationLevels = false;
		boolean sequenceDupilicationLevelsReachedTable = false;
		boolean overrepresentedSequences = false;
		boolean adapterContent = false;
		boolean kmerContent = false;
		
		// Initialise variables necessary for parsing the file
		String line = null;
							
		// Begin reading the file
		while(( line = reader.readLine()) != null){
		
			// Initialise an array to store the table column types
			String[] colTypes; 
			
			/**
			 * Basic Statistics block
			 */
			
			// Check if found
			if(line.matches(">>Basic Statistics(.*)")) {
				basicStatistics = true;
				this.basicStatisticsPass = line.split("\t")[1];
				continue;
			}
			
			// Check if reached end
			if(basicStatistics == true && line.matches(">>END_MODULE")) {
				basicStatistics = false;
			}
			
			// Record Basic Statistics
			if(basicStatistics == true) {
				recordBasicStatistics(line);
			}
			
			/**
			 * Per base sequence quality
			 */
			
			// Check if found
			if(line.matches(">>Per base sequence quality(.*)")) {
				
				// Note the class of each column
				colTypes = ArrayMethods.repeat("double", 7);
				colTypes[0] = "int[]";
				
				// Note that found block
				perBaseSequenceQuality = true;
				
				// Check for pass flag
				this.perBaseSequenceQualityPass = line.split("\t")[1];
				
				// Initialise table
				this.perBaseSequenceQualityTable = new Table("Per base sequence quality", colTypes);
				continue;
			}
			
			// Check if reached end
			if(perBaseSequenceQuality == true && line.matches(">>END_MODULE")) {
				perBaseSequenceQuality = false;
				
				// Finish building the table
				this.perBaseSequenceQualityTable.finishedWithTable();
			}
			
			// Record each row of the Per base sequencing quality table
			if(perBaseSequenceQuality == true) {
				this.perBaseSequenceQualityTable.addRow(line);
			}
			
			/**
			 * Per tile sequence quality
			 */
			
			// Check if found
			if(line.matches(">>Per tile sequence quality(.*)")) {
				
				// Note the class of each column
				colTypes = ArrayMethods.repeat("double", 3);
				colTypes[0] = "int";
				colTypes[1] = "int[]";
				
				// Note that found block
				perTileSequenceQuality = true;
				
				// Check for pass flag
				this.perTileSequenceQualityPass = line.split("\t")[1];
				
				// Initialise table
				this.perTileSequenceQualityTable = new Table("Per tile sequence quality", colTypes);
				continue;
			}
			
			// Check if reached end
			if(perTileSequenceQuality == true && line.matches(">>END_MODULE")) {
				perTileSequenceQuality = false;
				
				// Finish building the table
				this.perTileSequenceQualityTable.finishedWithTable();
			}
			
			// Record each row of the Per base sequencing quality table
			if(perTileSequenceQuality == true) {
				this.perTileSequenceQualityTable.addRow(line);
			}

			
			/**
			 * Per sequence quality scores
			 */
			
			// Check if found
			if(line.matches(">>Per sequence quality scores(.*)")) {
				
				// Note the class of each column
				colTypes = ArrayMethods.repeat("double", 2);
				colTypes[0] = "int";
				
				// Note that found block
				perSequenceQualityScores = true;
				
				// Check for pass flag
				this.perSequenceQualityScoresPass = line.split("\t")[1];
				
				// Initialise table
				this.perSequenceQualityScoresTable = new Table("Per sequence quality scores", colTypes);
				continue;
			}
			
			// Check if reached end
			if(perSequenceQualityScores == true && line.matches(">>END_MODULE")) {
				perSequenceQualityScores = false;
				
				// Finish building the table
				this.perSequenceQualityScoresTable.finishedWithTable();
			}
			
			// Record each row of the Per base sequencing quality table
			if(perSequenceQualityScores == true) {
				this.perSequenceQualityScoresTable.addRow(line);
			}
			
			/**
			 * Per base sequence content
			 */
			
			// Check if found
			if(line.matches(">>Per base sequence content(.*)")) {
				
				// Note the class of each column
				colTypes = ArrayMethods.repeat("double", 5);
				colTypes[0] = "int[]";
				
				// Note that found block
				perBaseSequenceContent = true;
				
				// Check for pass flag
				this.perBaseSequenceContentPass = line.split("\t")[1];
				
				// Initialise table
				this.perBaseSequenceContentTable = new Table("Per base sequence content", colTypes);
				continue;
			}
			
			// Check if reached end
			if(perBaseSequenceContent == true && line.matches(">>END_MODULE")) {
				perBaseSequenceContent = false;
				
				// Finish building the table
				this.perBaseSequenceContentTable.finishedWithTable();
			}
			
			// Record each row of the Per base sequencing quality table
			if(perBaseSequenceContent == true) {
				this.perBaseSequenceContentTable.addRow(line);
			}
			
			/**
			 * Per sequence GC content
			 */
			
			// Check if found
			if(line.matches(">>Per sequence GC content(.*)")) {
				
				// Note the class of each column
				colTypes = ArrayMethods.repeat("double", 2);
				colTypes[0] = "int";
				
				// Note that found block
				perSequenceGCContent = true;
				
				// Check for pass flag
				this.perSequenceGCContentPass = line.split("\t")[1];
				
				// Initialise table
				this.perSequenceGCContentTable = new Table("Per sequence GC content", colTypes);
				continue;
			}
			
			// Check if reached end
			if(perSequenceGCContent == true && line.matches(">>END_MODULE")) {
				perSequenceGCContent = false;
				
				// Finish building the table
				this.perSequenceGCContentTable.finishedWithTable();
			}
			
			// Record each row of the Per base sequencing quality table
			if(perSequenceGCContent == true) {
				this.perSequenceGCContentTable.addRow(line);
			}
			
			/**
			 * Per base N content
			 */
			
			// Check if found
			if(line.matches(">>Per base N content(.*)")) {
				
				// Note the class of each column
				colTypes = ArrayMethods.repeat("double", 2);
				colTypes[0] = "int[]";
				
				// Note that found block
				perBaseNContent = true;
				
				// Check for pass flag
				this.perBaseNContentPass = line.split("\t")[1];
				
				// Initialise table				
				this.perBaseNContentTable = new Table("Per base N content", colTypes);
				continue;
			}
			
			// Check if reached end
			if(perBaseNContent == true && line.matches(">>END_MODULE")) {
				perBaseNContent = false;
				
				// Finish building the table
				this.perBaseNContentTable.finishedWithTable();
			}
			
			// Record each row of the Per base sequencing quality table
			if(perBaseNContent == true) {
				this.perBaseNContentTable.addRow(line);
			}
			
			/**
			 * Sequence Length Distribution
			 */
			
			// Check if found
			if(line.matches(">>Sequence Length Distribution(.*)")) {
				
				// Note the class of each column
				colTypes = ArrayMethods.repeat("double", 2);
				colTypes[0] = "int[]";
				
				// Note that found block
				sequenceLengthDistribution = true;
				
				// Check for pass flag
				this.sequenceLengthDistributionPass = line.split("\t")[1];
				
				// Initialise table				
				this.sequenceLengthDistributionTable = new Table("Sequence Length Distribution", colTypes);
				continue;
			}
			
			// Check if reached end
			if(sequenceLengthDistribution == true && line.matches(">>END_MODULE")) {
				sequenceLengthDistribution = false;
				
				// Finish building the table
				this.sequenceLengthDistributionTable.finishedWithTable();
			}
			
			// Record each row of the Per base sequencing quality table
			if(sequenceLengthDistribution == true) {
				this.sequenceLengthDistributionTable.addRow(line);
			}
			
			/**
			 * Sequence Duplication Levels
			 */
			
			// Check if found
			if(line.matches(">>Sequence Duplication Levels(.*)")) {
				
				// Note the class of each column
				colTypes = ArrayMethods.repeat("double", 3);
				colTypes[0] = "String";
				
				// Note that found block
				sequenceDuplicationLevels = true;
				
				// Check for pass flag
				this.sequenceDuplicationLevelsPass = line.split("\t")[1];
				
				// Initialise table				
				this.sequenceDuplicationLevelsTable = new Table("Sequence Duplication Levels", colTypes);
				continue;
			}
			
			// Check if reached table
			if(sequenceDuplicationLevels == true && line.matches("#Total Deduplicated Percentage(.*)")){
				
				// Get the total deduplicated percentage
				this.totalDeDuplicatedPercentage = Double.parseDouble(line.split("\t")[1]);
				
				// Note that reached table
				sequenceDupilicationLevelsReachedTable = true;
				continue;
			}
			
			// Check if reached end
			if(sequenceDuplicationLevels == true && line.matches(">>END_MODULE")) {
				sequenceDuplicationLevels = false;
				
				// Finish building the table
				this.sequenceDuplicationLevelsTable.finishedWithTable();
			}
			
			// Record each row of the Per base sequencing quality table
			if(sequenceDuplicationLevels == true && sequenceDupilicationLevelsReachedTable == true) {
				this.sequenceDuplicationLevelsTable.addRow(line);
			}
			
			/**
			 * Overrepresented sequences
			 */
			
			// Check if found
			if(line.matches(">>Overrepresented sequences(.*)")) {
				
				// Note the class of each column
				colTypes = ArrayMethods.repeat("double", 4);
				colTypes[0] = "String";
				colTypes[1] = "int";
				colTypes[3] = "String";
				
				// Note that found block
				overrepresentedSequences = true;
				
				// Check for pass flag
				this.overrepresentedSequencesPass = line.split("\t")[1];
				
				// Initialise table				
				this.overrepresentedSequencesTable = new Table("Overrepresented sequences", colTypes);
				continue;
			}
			
			// Check if reached end
			if(overrepresentedSequences == true && line.matches(">>END_MODULE")) {
				overrepresentedSequences = false;
				
				// Finish building the table
				this.overrepresentedSequencesTable.finishedWithTable();
			}
			
			// Record each row of the Per base sequencing quality table
			if(overrepresentedSequences == true) {
				this.overrepresentedSequencesTable.addRow(line);
			}

			
			/**
			 * Adapter Content
			 */
			
			// Check if found
			if(line.matches(">>Adapter Content(.*)")) {
				
				// Note the class of each column
				colTypes = ArrayMethods.repeat("double", 6);
				colTypes[0] = "int[]";
				
				// Note that found block
				adapterContent = true;
				
				// Check for pass flag
				this.adapterContentPass = line.split("\t")[1];
				
				// Initialise table				
				this.adapterContentTable = new Table("Adapter Content", colTypes);
				continue;
			}
			
			// Check if reached end
			if(adapterContent == true && line.matches(">>END_MODULE")) {
				adapterContent = false;
				
				// Finish building the table
				this.adapterContentTable.finishedWithTable();
			}
			
			// Record each row of the Per base sequencing quality table
			if(adapterContent == true) {
				this.adapterContentTable.addRow(line);
			}

			/**
			 * Kmer Content
			 */
			
			// Check if found
			if(line.matches(">>Kmer Content(.*)")) {
				
				// Note the class of each column
				colTypes = ArrayMethods.repeat("double", 5);
				colTypes[0] = "String";
				colTypes[4] = "int[]";
				
				// Note that found block
				kmerContent = true;
				
				// Check for pass flag
				this.kmerContentPass = line.split("\t")[1];
				
				// Initialise table				
				this.kmerContentTable = new Table("Kmer Content", colTypes);
				continue;
			}
			
			// Check if reached end
			if(kmerContent == true && line.matches(">>END_MODULE")) {
				kmerContent = false;
				
				// Finish building the table
				this.kmerContentTable.finishedWithTable();
			}
			
			// Record each row of the Per base sequencing quality table
			if(kmerContent == true) {
				this.kmerContentTable.addRow(line);
			}
		}
		
		// Close the input file
		input.close();
		reader.close();
	}
	
	private void recordBasicStatistics(String line) {
		
		// Filename
		if(line.matches("Filename(.*)")) {
			this.file = line.split("\t")[1];
		}
		
		// Encoding
		if(line.matches("Encoding(.*)")) {
			this.encoding = line.split("\t")[1];
		}
		
		// Total Sequences
		if(line.matches("Total Sequences(.*)")) {
			this.nReads = Integer.parseInt(line.split("\t")[1]);
		}
		
		// Sequence length
		if(line.matches("Sequence length(.*)")) {
			this.readLength = ArrayMethods.convertToInteger(line.split("\t")[1].split("-"));
		}
		
		// %GC
		if(line.matches("%GC(.*)")) {
			this.gcContent = Double.parseDouble(line.split("\t")[1]);
		}
	}
}
