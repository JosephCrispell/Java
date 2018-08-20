package parsingFastqc;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.HashtableMethods;
import methods.MatrixMethods;

public class Data {

	private String directory;
	
	// Basic statistics
	private boolean basicStatisticsPass;
	private String file;
	private String encoding;
	private int nReads;
	private int[] readLength;
	private double gcContent;
	
	// Per base sequence quality
	private boolean perBaseSequenceQualityPass;
	private Table perBaseSequenceQualityTable;
	
	// Per tile sequence quality
	private boolean perTileSequenceQualityPass;
	private Table perTileSequenceQualityTable;
	
	// Per sequence quality scores
	private boolean perSequenceQualityScoresPass;
	private Table perSequenceQualityScoresTable;
	
	// Per base sequence content
	private boolean perBaseSequenceContentPass;
	private Table perBaseSequenceContentTable;
	
	// Per sequence GC content
	private boolean perSequenceGCContentPass;
	private Table perSequenceGCContentTable;
	
	// Per base N content
	private boolean perBaseNContentPass;
	private Table perBaseNContentTable;
	
	// Sequence Length Distribution
	private boolean sequenceLengthDistributionPass;
	private Table sequenceLengthDistributionTable;
	
	// Sequence Duplication Levels
	// First column: Duplication level
	private boolean sequenceDuplicationLevelsPass;
	private Table sequenceDuplicationLevelsTable;
	private double totalDeDuplicatedPercentage;
	
	// Overrepresented sequences
	private boolean overrepresentedSequencesPass;
	private Table overrepresentedSequencesTable;
	
	// Adapter content
	private boolean adapterContentPass;
	private Table adapterContentTable;
	
	// Kmer content
	private boolean kmerContentPass;
	private Table kmerContentTable;
	
	public Data(String fastqcResultDirectory) throws IOException {
		
		// Store the file name
		this.directory = fastqcResultDirectory;
		
		// Read the data file and store the information
		readFile();

	}
	
	private void readFile() throws IOException {
		
		// Open the input file
		InputStream input = new FileInputStream(directory);
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
				this.basicStatisticsPass = line.matches("(.*)pass");
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
				this.perBaseSequenceQualityPass = line.matches("(.*)pass");
				
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
				this.perTileSequenceQualityPass = line.matches("(.*)pass");
				
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
				this.perSequenceQualityScoresPass = line.matches("(.*)pass");
				
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
				this.perBaseSequenceContentPass = line.matches("(.*)pass");
				
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
				this.perSequenceGCContentPass = line.matches("(.*)pass");
				
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
				this.perBaseNContentPass = line.matches("(.*)pass");
				
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
				colTypes[0] = "int";
				
				// Note that found block
				sequenceLengthDistribution = true;
				
				// Check for pass flag
				this.sequenceLengthDistributionPass = line.matches("(.*)pass");
				
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
				this.sequenceDuplicationLevelsPass = line.matches("(.*)pass");
				
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
				this.overrepresentedSequencesPass = line.matches("(.*)pass");
				
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
				this.adapterContentPass = line.matches("(.*)pass");
				
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
				this.kmerContentPass = line.matches("(.*)pass");
				
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
