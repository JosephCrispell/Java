package filterSensitivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import methods.*;


public class DistanceMatrixMethods {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		
		// Method Testing Area
		
		String fastaFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/origTest/originalFastaJava.txt";
		
		Sequence[] sequences = DistanceMatrixMethods.readFastaFile(fastaFile);
		
		DistanceMatrix distanceMatrixInfo = DistanceMatrixMethods.buildDistanceMatrix(sequences, "pDistance");
		
				
		String file = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/origTest/originalDistanceMatrix.txt";
	    
		
		print(distanceMatrixInfo, file);
		
		
		// Compare two different Fasta Files
//		String file1 = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/testFasta1.txt";
//		String file2 = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/testFasta2.txt";
//		String tree1 = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/testTree1.txt";
//		String tree2 = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/testTree2.txt";
//		WriteFile treeFile1 = new WriteFile(tree1, false);
//    	treeFile1.writeToFile("");
//    	treeFile1 = new WriteFile(tree1, true);
//    	WriteFile treeFile2 = new WriteFile(tree2, false);
//    	treeFile2.writeToFile("");
//    	treeFile2 = new WriteFile(tree2, true);
//    	
//    	// Build the Distance Matrices   	
//		Sequence[] sequences1 = readFastaFile(file1);
//		DistanceMatrix distanceMatrixInfo1 = buildDistanceMatrix(sequences1, "pDistance");
//		
//		MatrixMethods.print(distanceMatrixInfo1.getDistanceMatrix());
//		
//		Sequence[] sequences2 = readFastaFile(file2);
//		DistanceMatrix distanceMatrixInfo2 = buildDistanceMatrix(sequences2, "pDistance");
//		
//		// Build the NJ Trees
//		Node newickTree1 = NJTreeMethods.BuildNJTree(distanceMatrixInfo1);
//		NJTreeMethods.printNode(newickTree1, treeFile1);
//		
//		Node newickTree2 = NJTreeMethods.BuildNJTree(distanceMatrixInfo2);
//		NJTreeMethods.printNode(newickTree2, treeFile2);
		
	}
	
	// Method
	public static Sequence[] readFastaFile(String file) throws NumberFormatException, IOException{
		
		/**
		 * Fasta File Format:
		 * 	noSamples noNucleotides
		 * 	SampleA-- GGTCAGTGCGTAGC
		 * 	9chars--- Nucleotides...
		 * 
		 * C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/allRawReads/vcfFiles/sequenceFasta.txt
		 */
		
		// Open the Sequence Fasta File for Reading
    	InputStream input = new FileInputStream(file);
    	BufferedReader reader = new BufferedReader(new InputStreamReader(input));
    	
    	// Initialise Variables to Store the Sequence Information
    	Sequence[] sequences = {};
    	char[] nucleotides;
    	int noSamples;
    	int noNucleotides = -1;
    	
    	// Begin Reading the Fasta File
    	int lineNo = -1;
    	String line = null;
    	while(( line = reader.readLine()) != null){
    	   	lineNo++;
    	   	
    	   	// Split the File Lines by " " into a String Array
    	   	String[] columns = line.split(" ");
    		
    	   	// Find the No. of Samples and Sequence Length Information
    	   	if(lineNo == 0){
    	   		
    	   		noSamples = Integer.parseInt(columns[0]);
    	   		noNucleotides = Integer.parseInt(columns[1]);
    	   		
    	   		// Set the Size of the Array of Sequence Objects and Sequence Size within these
    	   		sequences = new Sequence[noSamples];
    	   		nucleotides = new char[noNucleotides];
   	   		
    	   	}else{
    	   		
    	   		// Convert the Sequence String to a Character Array
    	   		nucleotides = columns[1].toCharArray();
    	   		
    	   		// Create a New Sequence Object Containing the Sequence Array and Sample Name
    	   		sequences[lineNo - 1] = new Sequence(columns[0], nucleotides);

    	   	} 	
       	}
    	
    	reader.close();
    	
    	return sequences;    	
	}

	public static DistanceMatrix buildDistanceMatrix(Sequence[] sequences, String model){
		
		/**
		 * Build a Distance Matrix Comparing Nucleotide Sequences According to a Specified Mutation Model
		 * Models:
		 * 	pDistance - Proportion of sites which are different between the two sequences
		 * 	JukesCantor - Assumes all substitution events are equally likely and occur at an equal rate
		 */
		
		// Create an empty Distance Matrix of the Correct Size
		double[][] d = new double[sequences.length][sequences.length];
		String[] sampleNames = new String[sequences.length];
		
		// Compare Each Sample i to Every Other Sample j
		for(int i = 0; i < sequences.length; i++){
			
			sampleNames[i] = sequences[i].getSampleName(); // Store Each of the Samples Names
			// Get the Current Sample's (I) Sequence
			char[] seqI = sequences[i].getSequence(); 
			
			for(int j = 0; j < sequences.length; j++){
				
				// Don't Compare the Same Sequence and Only Compare Sequence Pairs Once
				if(i == j || d[i][j] != 0){
					continue;
				}
				
				// Get the Sequence of Sample J
				char[] seqJ = sequences[j].getSequence();
				
				// Compare the Sequences - Count the Number of Differences
				double count = 0;
				for(int index = 0; index < seqI.length; index++){
					
					// If the Positions are Different and Neither are N then Count
					if(seqI[index] !=seqJ[index] && seqI[index] != 'N' && seqJ[index] != 'N'){
						count++;
					}
				}
				
				// Fill in the Distance Matrix element
				d[i][j] = 0;
				if(count > 0 && model == "pDistance"){
					
					/**
					 * P-Distance:
					 * 	Proportion of sites which are different between the two sequences
					 */
					
					d[i][j] = count/seqI.length;
					d[j][i] = count/seqI.length;
					
				}else if(count > 0 && model == "JukesCantor"){

					/**
					 * Jukes and Cantor Distance:
					 * 	Assumes all substitution events are equally likely and occur at an equal rate		
					 * 
					 * 		dij = -3/4 ln(1 - 4/3D)
					 * 	
					 * 	dij = Distance between sequence i and j expressed as the no. of changes/site
					 * 	D = p-distance 
					 * 	ln = Natural logarithm to correct for superimposed substitutions
					 * 	
					 * 	The 3/4 and 4/3 terms reflect that there are four types of nucleotides and three ways in which
					 * 	a second nucleotide may not match the first, with all types of change being equally likely
					 * 	(i.e. unrelated sequences should be 25% identical by chance alone).
					 */
					
					d[i][j] = (-3.0 / 4.0) * Math.log( 1.0 - ((4.0/3.0) * (count/seqI.length)) );
					d[j][i] = (-3.0 / 4.0) * Math.log( 1.0 - ((4.0/3.0) * (count/seqI.length)) );
				}
			}
		}
		
		// Return the Distance Matrix Information
		return new DistanceMatrix(sampleNames, d);
	}
	
	public static DistanceMatrix readInDistanceMatrix(String file) throws IOException{
    	
		/**
		 * NOTE:
		 * This Method reads in Formatted Distance Matrix - Formatting is done using Perl:
		 * 		perl FormatDistanceMatrix.pl distanceMatrix.txt perlDistanceMatrix.txt
		 * 
		 * C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/allRawReads/vcfFiles/perlDistanceMatrix.txt
		 * 
		 * 		A	B	C	D
		 * 		A	0	-	-	-
		 * 		B	-	0	-	-
		 * 		C	-	-	0	-
		 * 		D	-	-	-	0
		 * 
		 */
		
    	// Open the Distance Matrix File for Reading
    	InputStream input = new FileInputStream(file);
    	BufferedReader reader = new BufferedReader(new InputStreamReader(input));
    			
    	// Distance Matrix Information to Store
    	String[] sampleNames = {};
    	double[][] distanceMatrix = {};
    	
    	// Begin Reading the Distance Matrix File
    	int lineNo = -1;
    	String line = null;
    	while(( line = reader.readLine()) != null){
    	   	lineNo++;
    	   	
    	   	String[] columns = line.split("\t");
    		
    	   	// Find the Sample names
    	   	if(lineNo == 0){
    	   		
    	   		// Sample Names are given in Tab separated List in First Line of File
    	   		sampleNames = columns;
    	   		
    	   		// Initialise Distance Matrix at the Correct Size
    	   		distanceMatrix = new double[sampleNames.length][sampleNames.length];
    	   	}else{
    	   		
    	   		// Build the Distance Matrix
    	   		int row = lineNo - 1;
    	   		
    	   		// Note line starts with sample name (begin at index 1)
    	   		for(int index = 1; index < columns.length; index++){
    	   			distanceMatrix[row][index - 1] = Double.parseDouble(columns[index]);
    	   		}
    	   	} 	
       	}
    	
    	// Store the Distance Matrix Information
    	DistanceMatrix distanceMatrixInfo = new DistanceMatrix(sampleNames, distanceMatrix);
    	
    	reader.close();
    	
    	return distanceMatrixInfo;
    }

	public static void print(DistanceMatrix distanceMatrixInfo, String fileName) throws IOException{
		/**
		 *  Print the Distance Matrix out to File:
		 *  	A	B	C	D
		 * 		A	0	-	-	-
		 * 		B	-	0	-	-
		 * 		C	-	-	0	-
		 * 		D	-	-	-	0
		 *  
		 *  To Avoid writing to file on multiple occasions a String is built
		 *  
		 */
		
		// Open and Wipe File
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Extract Distance Matrix Information
		double[][] d = distanceMatrixInfo.getDistanceMatrix();
		String[] sampleNames = distanceMatrixInfo.getSampleNames();
		String line = "";
		
		// Add Sample Names
		for(int i = 0; i < sampleNames.length; i++){
			line = line + sampleNames[i] + "\t";
		}
		WriteToFile.writeLn(bWriter, line);
		
		// Add Distance Matrix Elements
		for(int i = 0; i < sampleNames.length; i++){
			
			line = sampleNames[i] + "\t";
			
			for(int j = 0; j < sampleNames.length; j++){
				
				line = line + d[i][j] + "\t";
				
			}
			
			WriteToFile.writeLn(bWriter, line);
		}
	}
	
	public static double findAverageDistance(double[][] distanceMatrix){
		double average = 0;
		int[][] record = new int[distanceMatrix.length][distanceMatrix[0].length];
		
		int noDistances = 0;
		for(int i = 0; i < distanceMatrix.length; i++){
			for(int j = 0; j < distanceMatrix[0].length; j++){
				
				if(i != j && record[i][j] == 0){
					average += distanceMatrix[i][j];
					noDistances++;
					record[i][j] = 1;
					record[j][i] = 1;
				}
			}
		}
		
		average = average / noDistances;
		
		return average;
	}
	
	// Methods for Examining Nearest Neighbours
	public static int[] findNearestNeighbours(double[] row, int id){
		int[] minPositions = new int[0];
		
		// Find the Minimum Value avoid the d[i][i] which will equal 0
		double min = 99999999; // Arbitrary High Number
		for(int index = 0; index < row.length; index++){
			if(row[index] < min && index != id){
				min = row[index];
			}
		}
		
		// Find all occurrences of the Min Value above - These are the Nearest Neighbours
		for(int index = 0; index < row.length; index++){
			if(row[index] == min && index != id){
				minPositions = ArrayMethods.append(minPositions, index);
			}
		}
		
		// Return the indexes of the Nearest Neighbours
		return minPositions;
	}

	public static int[][] findNeighboursInDistanceMatrix(double[][] d, int noSamples){
		
		// For each Sample in the Distance Matrix - Find its Nearest Neighbours
		int[][] neighbourInfo = new int[noSamples][0];
		
		for(int index = 0; index < noSamples; index++){
			neighbourInfo[index] = findNearestNeighbours(d[index], index);
		}
		
		return neighbourInfo;
	}

	public static double[] compareNeighbourDistributions(int[][] originalNeighbours, int[][] currentNeighbours){
		
		/**
		 *  Compare the Nearest Neighbour Distributions between Two Trees
		 *  
		 *  original: 	A -> {B,C,D,E,F}
		 *  current: 	A -> {B,D,G,H}
		 *  
		 *  number the Same = 2
		 *  proportion the Same = 2 / 5 (Length of Longest Neighbour List)
		 *  
		 */
		
		
		int noSamples = originalNeighbours.length;
		
		// Initialise the Variables to Store the Results - Note Results Available as Count and Proportion
		double noSame;
		double propSame;
		double[] counts = new double[noSamples];
		double[] proportions = new double[noSamples];;
		
		// Examine Each Sample's Nearest Neighbours within the two trees
		for(int index = 0; index < noSamples; index++){
			
			// How many of the Current Samples Original Neighbours Remain?
			noSame = 0;
			for(int id : originalNeighbours[index]){
				if(ArrayMethods.found(currentNeighbours[index], id) == 1){
					noSame++;
				}
			}
			
			// What Proportion is this?
			if(originalNeighbours[index].length > currentNeighbours[index].length){
				propSame = noSame / originalNeighbours[index].length;
			}else{
				propSame = noSame / currentNeighbours[index].length;
			}
			
			// Store the Results
			counts[index] = noSame;
			proportions[index] = propSame;
		}
		
		// Return the Proportions
		return proportions;
	}

	public static double[] compareNearestNeighbours(DistanceMatrix original, DistanceMatrix current){
		
		/**
		 * Compare the Nearest Neighbour Distributions between Two Trees
		 *  
		 *  original: 	A -> {B,C,D,E,F}
		 *  current: 	A -> {B,D,G,H}
		 *  
		 *  number the Same = 2
		 *  proportion the Same = 2 / 5 (Length of Longest Neighbour List)
		 */
		
		// Prepare Original Tree Information
		double[][] origD = original.getDistanceMatrix();
		String[] origSampleNames = original.getSampleNames();
		int[][] origNearestNeighbours = findNeighboursInDistanceMatrix(origD, origSampleNames.length);
		
		// Prepare Current Distance Matrix Information
		double[][] currD = current.getDistanceMatrix();
		String[] currSampleNames = current.getSampleNames();
		int[][] currNearestNeighbours = findNeighboursInDistanceMatrix(currD, currSampleNames.length);
		
		// Compare the Nearest Neighbour Distributions
		return compareNeighbourDistributions(origNearestNeighbours, currNearestNeighbours);
		
	}
	// Methods For Examining Which Groups the Nearest Neighbours are From (Group is either Herd or Episode)
	// Methods for Examining Groups of Nearest Neighbours - Plan 1
	public static double[] compareNearestNeighbourGroupProportions(DistanceMatrix origDistanceMatrixInfo, DistanceMatrix currDistanceMatrixInfo, char group){
		
		/**
		 * Which Herds are the Nearest Neighbours From?
		 * Here we investigate the proportion of Nearest Neighbours which came from each of the Cattle Herds
		 * 
		 * Example:
		 * 	Herds {1,2,3,4,5}
		 * 	original:	A1 -> {B1,C1,D2,E3} -> {0.5,		0.25,		0.25,	0,		0}
		 *  current:	A1 -> {G2,D2,B1,F4} -> {0.25,		0.5,		0,		0.25,	0}
		 *  								   {0.25/0.5,  	0.25/0.5,	0,		0,		1} ->Average-> 2/no Herds = 0.4
		 */
		
		// Get the System Information
		String[] sampleNames = origDistanceMatrixInfo.getSampleNames();
		String[] groupIds = findGroupIds(sampleNames, group);	
		double[][] origD = origDistanceMatrixInfo.getDistanceMatrix();
		double[][] currD = currDistanceMatrixInfo.getDistanceMatrix();
		
		// Initialise an Array to Store the Results
		double[] proportionConsistency = new double[sampleNames.length];
		
		// Compare the Neighbour Herd Proportions for each of the Samples across the two Distance Matrices
		for(int index = 0; index < sampleNames.length; index++){
			
			// Find the Nearest Neighbours of the Current Sample in Each Tree
			int[] origNeighbours = findNearestNeighbours(origD[index], index);
			int[] currNeighbours = findNearestNeighbours(currD[index], index);
			
			// Find the Herds from which these Neighbours Came
			String[] origNeighboursGroups = findGroupsOfNeighbours(origNeighbours, sampleNames, group);
			String[] currNeighboursGroups = findGroupsOfNeighbours(currNeighbours, sampleNames, group);
			
			// Find the Proportion of Nearest Neighbours which came from each Herd
			double[] origNeighboursGroupProp = findNeighboursGroupProportions(origNeighboursGroups, groupIds);
			double[] currNeighboursGroupProp = findNeighboursGroupProportions(currNeighboursGroups, groupIds);
			
			// Compare the Nearest Neighbour Herd Proportion Distributions
			double mean = 0;
			for(int pos = 0; pos < groupIds.length; pos++){
				
				// Divide Smaller Proportion by Larger - Only Interested in Consistency not direction
				if(origNeighboursGroupProp[pos] > currNeighboursGroupProp[pos] && origNeighboursGroupProp[pos] != 0){
					
					mean += currNeighboursGroupProp[pos] / origNeighboursGroupProp[pos];
				}else if(currNeighboursGroupProp[pos] > origNeighboursGroupProp[pos] && currNeighboursGroupProp[pos] != 0){
					
					mean += origNeighboursGroupProp[pos] / currNeighboursGroupProp[pos];
				}else if(origNeighboursGroupProp[pos] == currNeighboursGroupProp[pos]){
					
					mean += 1;
				}// Note that if only one is 0 then consistency is 0 -> nothing done
			}
			
			// Store the Result
			proportionConsistency[index] = mean / groupIds.length;
		}
		
		return proportionConsistency;
	}
	
	public static String[] findGroupsOfNeighbours(int[] neighbours, String[] sampleNames, char group){
		
		// Initialise an Array to Store the Herd IDs of the Neighbours
		String[] sampleHerds = new String[neighbours.length];
		
		// Investigate each of the Nearest Neighbours
		for(int index = 0; index < neighbours.length; index++){
			
			// Find Sample's Herd
			String herd = findSamplesGroup(sampleNames[neighbours[index]], group);
			
			// Store the Herd ID
			sampleHerds[index] = herd;
		}
		
		// Return the Herd IDs for each of the Nearest Neighbours
		return sampleHerds;
	}
	
	public static String findSamplesGroup(String sampleName, char group){
		
		// Get the Sample Info: NSampleNo_AnimalID_HerdID_EpisodeID_Year_Badger_SampleID
		String[] sampleInfo = sampleName.split("_");
		
		// Select the correct part of the Smaple's name depending on whether looking at Herd or Episode
		int pos = -1;
		if(group == 'H'){
			pos = 2;
		}else if (group == 'E'){
			pos = 3;
		}else{
			System.out.println("ERROR: Incorrect Group Specification in method: findSamplesGroup");
		}
		
		// Return the Herd ID
		return sampleInfo[pos];		
	}

	public static double[] findNeighboursGroupProportions(String[] neighboursGroups, String[] GroupIds){
		
		/**
		 * Calculating the Proportion of a Sample's Nearest Neighbours which came from Each Herd
		 * 
		 * Example:
		 * 	Herds {1,2,3,4,5}
		 * 	original:	A1 -> {B1,C1,D2,E3} -> {0.5, 0.25, 0.25, 0, 0}
		 */
		
		// Initialise an Array to Store the Group Proportions
		double[] groupProportions = new double[GroupIds.length];
		
		// Count the Number of Nearest Neighbours which came from each Group
		for(String group : neighboursGroups){
			
			// Look for the Group of the Current Nearest Neighbour in the Array of HerdIDs
			for(int index = 0; index < GroupIds.length; index++){
				if(group.equals(GroupIds[index])){
					// Create a Tally at the Index of each Group
					groupProportions[index]++;
					break;
				}
			}
		}
		
		// Convert the Tallies into a Proportion of the Number of Neighbours
		for(int index = 0; index < groupProportions.length; index++){
			groupProportions[index] = groupProportions[index] / neighboursGroups.length;
		}
		
		// Return the Group Proportions Array
		return groupProportions;
	}
	
	// PROBLEM - HERD IDS FOR BADGERS ARE NOT SIMPLE INTEGERS - Created methods to compare Strings instead
	public static String[] findGroupIds(String[] sampleNames, char group){
		/**
		 * Find the Herd IDs of all the Herds involved
		 * 
		 * Sample Name Structure:
		 * 		NSampleNo_AnimalID_HerdID_EpisodeID_Year_Badger_SampleID
		 */
		
		// Initialise Array to Store Group IDs
		String[] groups = new String[sampleNames.length];
		
		// Select the correct part of the Smaple's name depending on whether looking at Herd or Episode
		int pos = -1;
		if(group == 'H'){
			pos = 2;
		}else if (group == 'E'){
			pos = 3;
		}else{
			System.out.println("ERROR: Incorrect Group Specification in method: findSamplesGroups");
		}
		
		// Investigate each of the Sample Names
		for(int index = 0; index < sampleNames.length; index++){
			String[] parts = sampleNames[index].split("_");
			// Store the Herd ID from the Current Sample
			groups[index] = parts[pos];
		}
		
		// Find the Unique Herd IDs
		groups = ArrayMethods.unique(groups);
		
		return groups;
	}

	// Methods for Examining the GROUP'S Nearest Neighbours 
	// Methods for Examining Groups Nearest Neighbours - Plan 2
	public static double[] compareGroupNearestNeighbourDistributions(DistanceMatrix origInfo, DistanceMatrix currentInfo, char group){
		
		/**
		 * The Sample Distance Matrix is compressed into a Herd Distance Matrix where:
		 * 		Mij = Average Distance from each Sample in Herd i to each Sample in Herd j
		 * 
		 * The Nearest Herd Distributions are calculated using the Herd Distance Matrices of two different Trees and Compared
		 * 
		 * Samples are group by either Herd (H) or Episode (E)
		 */
		
		// Create the Herd Distance Matrices
		double[][] origGroupDistanceMatrix = createGroupDistanceMatrix(origInfo, group);
		double[][] currentGroupDistanceMatrix = createGroupDistanceMatrix(currentInfo, group);
	
		// Generate the Nearest Herd Distributions for each Herd Distance Matrix
		int[][] origNearestGroups = findNeighboursInDistanceMatrix(origGroupDistanceMatrix, origGroupDistanceMatrix.length);
		int[][] currentNearestGroups = findNeighboursInDistanceMatrix(currentGroupDistanceMatrix, currentGroupDistanceMatrix.length);
		
		// Compare the Nearest Herd Distributions amongst the Trees
		double[] consistency = compareNeighbourDistributions(origNearestGroups, currentNearestGroups);
		
		// Return the Consistency Levels of each Herd's Nearest Herds between the Trees
		return consistency;	
	}

	public static double[][] createGroupDistanceMatrix(DistanceMatrix distanceMatrixInfo, char group){
		
		/**
		 * The Sample Distance Matrix is compressed into a Herd Distance Matrix where:
		 * 	Mij = Average Distance from each Sample in Herd i to each Sample in Herd j
		 * 
		 * 
		 * Samples can either be grouped by Herd of Episode (H or E)
		 */
		
		// Extract the Distance Matrix Information
		String[] sampleNames = distanceMatrixInfo.getSampleNames();
		double[][] d = distanceMatrixInfo.getDistanceMatrix();

		// Find the Group IDs in the Distance Matrix - Note that Samples can either be grouped by herd or episode
		String[] groupIds = findGroupIds(sampleNames, group);
		String[] sampleGroups = findSamplesGroups(sampleNames, group);
				
		// Initialise the Herd Distance Matrix in the Correct Size
		double[][] groupDistanceMatrix = new double[groupIds.length][groupIds.length];
		
		// Investigate each of the Herds - Comparing all the samples in the given herd to the samples in each of the other herds
		for(int i = 0; i < groupIds.length; i++){
			for(int j = 0; j < groupIds.length; j++){
				
				// Avoid comparing the Same Herd or Comparing Herds more than once
				if(i == j || groupDistanceMatrix[i][j] != 0){ continue;}
				
				// Store the Herd IDs of the Herds being investigated
				String groupI = groupIds[i];
				String groupJ = groupIds[j];
				
				// Reset the Distance Sum and No. of Samples in Each Herd Counts
				double sum = 0;
				double noGroupI = 0;
				double noGroupJ = 0;
				
				// Investigate the Samples in Herd I
				for(int samplei = 0; samplei < sampleGroups.length; samplei++){
					
					// Only Interested in Samples From the Current Herd I
					if(!sampleGroups[samplei].equals(groupI)){continue;}
					
					// Count the Number of Samples in Herd I and Herd J
					noGroupI++;
					noGroupJ = 0; // Note need to know how many are in HerdJ only once (in this case take last count) (otherwise would keep adding for each I)
					
					// Investigate the Samples in Herd J
					for(int samplej = 0; samplej < sampleGroups.length; samplej++){
						
						// Only Interested in Samples in Herd J
						if(!sampleGroups[samplej].equals(groupJ)){continue;}
						
						// Count the Number of Samples in Herd J
						noGroupJ++;
						
						// Add the Distance from Sample i (Herd I) to Sample j (Herd J) to the Distance Sum
						sum += d[samplei][samplej];
					}
				}
				
				// Calculate the Average Distance by Taking the Sum of the Distances over the Number Distances
				// Fill in the Appropriate Elements in the Herd Distance Matrix
				groupDistanceMatrix[i][j] = sum / (noGroupI * noGroupJ);
				groupDistanceMatrix[j][i] = sum / (noGroupI * noGroupJ);
				
			}
		}
		
		// Return the Herd Distance Matrix
		return groupDistanceMatrix;
	}
	
	public static String[] findSamplesGroups(String[] sampleNames, char group){
		String[] herds = new String[sampleNames.length];
		
		// NSampleNo_AnimalID_HerdID_EpisodeID_Year_Badger_SampleID
		
		// Select the correct part of the Smaple's name depending on whether looking at Herd or Episode
		int pos = -1;
		if(group == 'H'){
			pos = 2;
		}else if (group == 'E'){
			pos = 3;
		}else{
			System.out.println("ERROR: Incorrect Group Specification in method: findSamplesGroups");
		}
		
		for(int index = 0; index < sampleNames.length; index++){
			String[] parts = sampleNames[index].split("_");
			herds[index] = parts[pos];
		}
		
		return herds;
	}
}
