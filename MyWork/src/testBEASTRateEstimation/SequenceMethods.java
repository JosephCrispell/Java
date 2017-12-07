package testBEASTRateEstimation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Random;

import methods.ArrayMethods;
import methods.GeneralMethods;
import methods.HashtableMethods;
import methods.MatrixMethods;
import methods.WriteToFile;

public class SequenceMethods {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	public static int[] generateRandomNucleotideSequence(double[] nucleotideWeights, int length, Random random){
		
		// Nucleotide sequence represented as integers: A = 0, C = 1, G = 2, T = 3		
		
		// Initialise an array to store the nucleotide sequence
		int[] sequence = new int[length];
		
		// Build the random nucleotide sequence
		for(int i = 0; i < length; i++){
			
			// Randomly choose a nucleotide for the current position
			sequence[i] = ArrayMethods.randomWeightedIndex(nucleotideWeights, random);
		}
		
		return sequence;		
	}
	
	public static double[][] createHKYSubstitutionMatrix(double tsProb){
		
		/**
		 *  Build a HKY substitution matrix - single parameter for the transition 
		 *  probability - purine (A, G) to purine or pyrimidine(C, T) to pyrimidine
		 */
		double tvProb = 1 - tsProb;			// A		C	G		T
		double[][] matrix = { 	{0, 		tsProb, tvProb, tvProb},
								{tsProb,	0,		tvProb,	tvProb},
								{tvProb,	tvProb,	0,		tsProb},
								{tvProb,	tvProb,	tsProb,	0     }	};
		
		return matrix;
	}
	
	public static void defineMutationEvents(double tsProb, Random random, int sizeLimit){
		
		// Define the substitution matrix
		double[][] substitutionMatrix = createHKYSubstitutionMatrix(tsProb);
		
		// Initialise an array to store the mutation event info
		Global.mutationEventInfoNew = new int[Global.mutationEventNo + 1][2];
		
		// Initialise an array to store a list of mutation events found at a genomes site
		IntArray events;
		
		// Define each of the mutation events
		for(int i = 0; i < Global.mutationEventNo + 1; i++){
					
			// Randomly choose a position on the reference genome
			Global.mutationEventInfoNew[i][0] = ArrayMethods.randomIndex(Global.reference.length, random);
					
			// Create an alternate allele - random weighted choice using the substitution matrix
			Global.mutationEventInfoNew[i][1] = ArrayMethods.randomWeightedIndex(substitutionMatrix[Global.reference[Global.mutationEventInfoNew[i][0]]], random);
		}
	}
	
	public static void findInformativeMutationEventSites(Population population, int[] sampledIds){
		
		// Initialise an array the count the number of sampled individuals that carry a mutation event
		int[] nIndividualsWithEvent = new int[Global.mutationEventNo + 1];
		
		// Examine the mutation events of every sampled individual
		for(int id : sampledIds){
			
			// Record which events the current sampled individual has
			for(int event : population.getIndividualsMutationEvents(id)){
				nIndividualsWithEvent[event]++;
			}
		}
		
		// Initialise a hashtable to record the sites on the genome that are informative
		Global.informativeGenomeEventSites = new Hashtable<Integer, Integer>();
		
		// Examine each mutation event, which are informative?
		for(int i = 0; i < Global.mutationEventNo + 1; i++){
			
			// Skip events that are found in none or all of the sampled individuals
			if(nIndividualsWithEvent[i] == 0 || nIndividualsWithEvent[i] == sampledIds.length){
				continue;
			}
			
			// Record the position that the current informative event fell on
			Global.informativeGenomeEventSites.put(Global.mutationEventInfoNew[i][0], i);
		}		
	}
	
	public static int calculateGeneticDistance(Individual a, Individual b, int sizeLimit){
		
		// Get a unique list of the sites where mutations occur in either individual
		int[] sites = ArrayMethods.combine(HashtableMethods.getKeysInt(a.getGenomeSiteInfo(Global.mutationEventInfoNew, sizeLimit)), HashtableMethods.getKeysInt(b.getGenomeSiteInfo(Global.mutationEventInfoNew, sizeLimit)));
		sites = ArrayMethods.unique(sites);
		
		// Initialise two arrays to store the mutation events found at a shared site
		int[] aEvents = new int[0];
		int[] bEvents = new int[0];
		
		// Initialise a variable to record the number of differences between the individuals
		int nDiff = 0;
		
		// Examine each site and compare the individuals
		for(int site : sites){
			
			// Do both the individuals have a mutation at the current site?
			if(a.getGenomeSiteInfo(Global.mutationEventInfoNew, sizeLimit).get(site) != null && b.getGenomeSiteInfo(Global.mutationEventInfoNew, sizeLimit).get(site) != null){
				
				// Get the mutation event(s) at the current site for both individuals
				aEvents = a.getGenomeSiteInfo(Global.mutationEventInfoNew, sizeLimit).get(site).getUsedPositions();
				bEvents = b.getGenomeSiteInfo(Global.mutationEventInfoNew, sizeLimit).get(site).getUsedPositions();
				
				// Do the individuals have a different event at the current site?
				// 	Note that we are only interested in the last event at the site for each individual
				if(aEvents[aEvents.length - 1] != bEvents[bEvents.length - 1]){
					nDiff++;
				}
				
			// Only one individual has event at the current site - found a difference
			}else{
				nDiff++;
			}
		}
		
		return nDiff;		
	}

	public static int[] getOrderedListOfInformativeSites(){
		
		// Return an ordered list of the informative genome sites
		return ArrayMethods.sort(HashtableMethods.getKeysInt(Global.informativeGenomeEventSites));
	}
	
	public static int[] getConstantSiteCounts(int[] sortedInformativeSites){
		
		// Initialise an array to count the alleles present at non-informative sites
		int[] constantSiteCounts = new int[4];
		
		// Index the informative sites
		Hashtable<Integer, Integer> indexedInformativeSites = ArrayMethods.indexArray(sortedInformativeSites);
		
		// Evaluate every position on the reference sequence
		for(int i = 0; i < Global.reference.length; i++){
			
			// Skip informative sites
			if(indexedInformativeSites.get(i) != null){
				continue;
			}
			
			// Have found a non-informative site - record allele in counts
			constantSiteCounts[Global.reference[i]]++;
		}
		
		return constantSiteCounts;
	}
	
	public static char[] createConcatenatedSequence(Individual a, int[] sortedInformativeSites){
		
		// Initialise an array of the nucleotides
		char[] nucleotides = {'A', 'C', 'G', 'T'};
		
		// Initialise a string to store the sequence
		char[] sequence = new char[sortedInformativeSites.length];
		int pos = -1;
		
		// Get the site information for the individual's mutation events
		Hashtable<Integer, IntArray> genomeSiteInfo = a.getGenomeSiteInfo(Global.mutationEventInfoNew, Global.arraySizeLimit);
		
		// Examine each of the informative sites
		for(int site : sortedInformativeSites){
			pos++;
			
			// Does the individual have the current site?
			if(genomeSiteInfo.get(site) != null){
			
				// Take the last event to occur at the current site
				sequence[pos] = nucleotides[Global.mutationEventInfoNew[genomeSiteInfo.get(site).getLastValue()][1]];
								
			// Individual doesn't have the current informative site - insert reference allele
			}else{
				sequence[pos] = nucleotides[Global.reference[site]];
			}
		}
		
		return sequence;
	}

	public static void writeAlignmentBlock(BufferedWriter bWriter, int[] sampledIds, Population population,
			int nInformativeSites, int genomeSize) throws IOException{
		
		// Find the informative Sites
		findInformativeMutationEventSites(population, sampledIds);
		int[] informativeSites = getOrderedListOfInformativeSites();
		
		WriteToFile.write(bWriter, "\t<!-- The sequence alignment (each sequence refers to a taxon above).         -->\n");
		WriteToFile.write(bWriter, "\t<!-- ntax=" + sampledIds.length + " nchar=" + informativeSites.length + "                                                     -->\n");
		WriteToFile.write(bWriter, "\t<alignment id=\"alignment\" dataType=\"nucleotide\">\n");
		
		// Initialise a variable to store each sampled individual's sequence
		char[] sequence;
		String outputSequence = "\t\t\t";
		
		for(int id : sampledIds){
			
			outputSequence = "\t\t\t";
			
			WriteToFile.write(bWriter, "\t\t<sequence>\n");
			WriteToFile.write(bWriter, "\t\t\t<taxon idref=\"Individual-" + id + "_" + population.getTimeStepIndividualSampledIn(id) + "\"/>\n");
			
			// Get the individual's sequence
			sequence = createConcatenatedSequence(population.getIndividual(id), informativeSites);			
			
			// Print the individual's sequence - 2000 sites at a time
			for(int i = 0; i < sequence.length; i++){
				
				if(i % 2000 != 0 || i == 0){
					outputSequence = outputSequence + sequence[i];
				}else{
					outputSequence = outputSequence + "\n\t\t\t" + sequence[i];
				}
			}
			WriteToFile.write(bWriter, outputSequence + "\n\t\t</sequence>\n");			
		}
		
		WriteToFile.write(bWriter, "\t</alignment>\n\n");
	}

	public static int[] writeBeastXML(String filePrefix, Population population, int[] sortedInformativeSites,
			int chainLength, String path, int genomeSize) throws IOException{
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(path + filePrefix + ".xml", false);
		
		// Get the constant site counts
		int[] constantSiteCounts = getConstantSiteCounts(sortedInformativeSites);
		
		// Get the IDs of the sampled individuals
		int[] sampledIds = population.getIdsOfSampledIndividuals();
		
		// Write to top of the XML file
		Methods.writeStartOfXML(bWriter);
		
		// Write the taxa block
		int[] minMax = Methods.writeTaxaBlock(bWriter, sampledIds, population);
	
		// Write the alignment block
		writeAlignmentBlock(bWriter, sampledIds, population, sortedInformativeSites.length, genomeSize);
		
		// Write the Constant Site Count Block
		Methods.writeConstantSiteCountBlock(bWriter, constantSiteCounts);
		
		// Write the blocks associated with the Population Dynamics
		Methods.writeTreeModelBlocks(bWriter, minMax);
		
		// Write the blocks describing the substitution process
		Methods.writeSubstitutionProcessBlocks(bWriter);
		
		// Write the Operators block
		Methods.writeOperatorsBlock(bWriter, minMax);
		
		// Write the MCMC settings block
		Methods.writeMCMCBlock(bWriter, chainLength, filePrefix);
		
		// Close the output file
		WriteToFile.close(bWriter);
		
		return constantSiteCounts;
	}

	public static double estimateSubstitutionRateOnSampledTransmissionTree(int[][] sampledAdjacencyMatrix, 
			int[][] adjacencyMatrix, Population population, int simulationLength, int genomeSize){
		
		/**
		 * Examine each branch of the sampled transmission tree
		 * For each branch calculate:
		 * 	- The genetic distance between the source and sink
		 *  - Temporal distance - time over which variation could accrue
		 *  		PeriodInSourceAfterInfection = SourceRemovalTime - TimeLeftSource
		 *  		PeriodInSink = SinkRemovalTime - TimeLeftSource
		 *  - Substitution rate = geneticDistance / temporalDistance
		 *  
		 *  TimeLeftSource must be calculated since a branch could span un-sampled individuals and the
		 *  evolution that may have occurred in these individuals must also be considered.
		 */
		
		// Initialise variables to calculate the temporal distance
		double sourceRemovalTime;
		double timeLeftSource;
		double sinkRemovalTime;
		double periodInSource;
		double periodInSink;
		
		// Create an array to store the substitution rate estimations for each branch
		int nDifferences;
		double sumTemporalDistance = 0;
		double sumGeneticDistance = 0;

		// Examine each of the branches in the sampled transmission tree
		for(int i = 0; i < sampledAdjacencyMatrix.length; i++){
						
			for(int j = 0; j < sampledAdjacencyMatrix.length; j++){
				
				// Skip empty elements
				if(sampledAdjacencyMatrix[i][j] == 0){
					continue;
				}

				// Get the Removal Times for the source and sink
				sourceRemovalTime = Methods.getRemovalTime(i, population, simulationLength);
				sinkRemovalTime = Methods.getRemovalTime(j, population, simulationLength);
				
				// Calculate the Time Left Source
				timeLeftSource = Methods.findTimeLeftSource(i, j, adjacencyMatrix, population);
				
				// Calculate the period spent in source after infection
				periodInSource = sourceRemovalTime - timeLeftSource;
				
				// Calculate the period spent in sink
				periodInSink = sinkRemovalTime - timeLeftSource;
				
				// Calculate the genetic distance
				nDifferences = calculateGeneticDistance(population.getIndividual(i), population.getIndividual(j), Global.arraySizeLimit);
				sumGeneticDistance += Methods.calculateJukesCantorDistance(nDifferences, genomeSize);
								
				// Calculate the temporal distance
				sumTemporalDistance += periodInSource + periodInSink;
				
//				System.out.println("--------------------------------------------------------------------------------");
//				System.out.println(i + "\t" + j);
//				System.out.println("Source status changes: " + ArrayMethods.toString(population.getIndividualsStatusChanges(i), ", "));
//				System.out.println("Sink status changes:   " + ArrayMethods.toString(population.getIndividualsStatusChanges(j), ", "));
//				System.out.println("Source sampled = " + population.getTimeStepIndividualSampledIn(i));
//				System.out.println("Sink sampled = " + population.getTimeStepIndividualSampledIn(j));
//				System.out.println(ArrayMethods.toString(population.getIndividualsInfectees(i), ", "));
//				System.out.println("Removal Times: " + sourceRemovalTime + "\t" + sinkRemovalTime);
//				System.out.println("Time left source = " + timeLeftSource);
//				System.out.println("Period in Source = " + periodInSource);
//				System.out.println("Period in Sink = " + periodInSink);
//				System.out.println("Genetic Distance = " + nDifferences);
			}
		}
		
		return sumGeneticDistance / sumTemporalDistance;
	}

	public static double estimateSubstitutionRateForSampledPopulation(Population population,
			int simulationLength, int genomeSize) throws IOException{

		// Build the entire adjacency matrix
		int[][] adjacencyMatrix = Methods.buildAdjacencyMatrix(population, "none");
		
		// Copy the entire adjacency matrix
		int[][] sampledAdjacencyMatrix = MatrixMethods.copy(adjacencyMatrix);
		
		// Build the sampled adjacency matrix
		removeUnSampledIndividualsWhoInfectedNoOne(population, sampledAdjacencyMatrix);
		iterativelyRemoveUnsampledLeaves(sampledAdjacencyMatrix, population, null);
		removeUnsampledIndividualsOnPathToSampledIndividuals(sampledAdjacencyMatrix, population);
		removeRootIfNotInvolved(sampledAdjacencyMatrix, population);
		
		// Estimate the substitution rate for the sampled population
		return estimateSubstitutionRateOnSampledTransmissionTree(sampledAdjacencyMatrix, adjacencyMatrix, population, simulationLength, genomeSize);
	}
	
	public static int[][] buildSampledTransmissionTree(int[][] adjacencyMatrix, Population population){
		
		// Copy the entire adjacency matrix
		int[][] sampledAdjacencyMatrix = MatrixMethods.copy(adjacencyMatrix);
				
		// Build the sampled adjacency matrix
		removeUnSampledIndividualsWhoInfectedNoOne(population, sampledAdjacencyMatrix);
		iterativelyRemoveUnsampledLeaves(sampledAdjacencyMatrix, population, null);
		removeUnsampledIndividualsOnPathToSampledIndividuals(sampledAdjacencyMatrix, population);
		removeRootIfNotInvolved(sampledAdjacencyMatrix, population);
		
		return sampledAdjacencyMatrix;
	}

 	public static void removeUnSampledIndividualsWhoInfectedNoOne(Population population,
			int[][] adjacencyMatrix){
		
		// Examine each individual in the population
		for(int i = 0; i < population.getSize(); i++){
			
			// Find individuals who weren't sampled and never infected anyone
			if(population.getTimeStepIndividualSampledIn(i) == -1 && population.getIndividualsLastInfecteeIndex(i) == -1){
				
				// Remove the current individual from the adjacency matrix
				Methods.removeLeaf(adjacencyMatrix, i);
			}
		}
		
	}
	
	public static void iterativelyRemoveUnsampledLeaves(int[][] adjacency, Population population,
			Hashtable<Integer, Integer> rowsToExamine){
		
		/**
		 *  Examine each row of the adjacency matrix - remove those that aren't sampled and didn't infect
		 *  anyone - un-sampled leaves.
		 *  
		 *  These individuals will have an OUT degree of 0.
		 */
		
		// Initialise a hashtable to record the rows that we removed an connection
		Hashtable<Integer, Integer> rowsWithEdgeRemoved = new Hashtable<Integer, Integer>();
		
		for(int row = 0; row < adjacency.length; row++){
			
			// Skip sampled individuals
			if(population.getTimeStepIndividualSampledIn(row) != -1){
				continue;
			}
			
			// Skip rows we don't want examine - ones that we didn't remove an edge in the previous iteration
			if(rowsToExamine != null && rowsToExamine.get(row) == null){
				continue;
			}
			
			// Remove any individuals with out degree = 1, infected but never infected anyone
			if(ArrayMethods.sum(adjacency[row]) == 0){
				rowsWithEdgeRemoved.put(removeLeaf(adjacency, row), 1);
			}
		}
		
		// If we removed some un-sampled leaves - check matrix again for more
		if(rowsWithEdgeRemoved.size() > 0){
			iterativelyRemoveUnsampledLeaves(adjacency, population, rowsWithEdgeRemoved);
		}
	}
	
	public static int removeLeaf(int[][] adjacency, int index){
		
		// Create a variable to note the row
		int sourceIndex = -1;
		
		// Transmission tree is recorded in adjacency matrix - and edge is from row to col (non-symmetric)
		for(int row = 0; row < adjacency[index].length; row++){
			
			// Find the edge to the current individual
			if(adjacency[row][index] == 1){
				adjacency[row][index] = 0;
				
				// Note the index of the source to the leaf that was just removed
				sourceIndex = row;
				break;
			}
		}
		
		return sourceIndex;
	}

	public static void removeUnsampledIndividualsOnPathToSampledIndividuals(int[][] adjacency, Population population){
		
		// Initialise a variable to store the calculated degree
		int[] degree;
		
		// Initialise variables to store source and infectee indices
		int sourceIndex;
		int infecteeIndex;
			
		for(int row = 0; row < adjacency.length; row++){
			
			// Skip sampled individuals
			if(population.getTimeStepIndividualSampledIn(row) != -1){
				continue;
			}
			
			// Calculate the degree for the current individual
			degree = Methods.calculateInAndOutDegreeOfIndividual(row, adjacency);
			
			/**
			 *  Remove individuals with degree = 2 - they are un-sampled individuals on the path
			 *  to sampled individuals.
			 */
			
			if(degree[0] == 1 && degree[1] == 1){
				
				// Find the source of infection for the current individual
				sourceIndex = Methods.findSourceAndRemoveConnection(row, adjacency);
				
				// Find the infectee of the current individual
				infecteeIndex = Methods.findInfecteeAndRemoveConnection(row, adjacency);
				
				// Connect the source and the infectee
				adjacency[sourceIndex][infecteeIndex] = 1;				
			}		
		}		
	}

	public static void removeRootIfNotInvolved(int[][] adjacency, Population population){
		
		/**
		 *  Remove the root if it isn't involved in the transmission path to sampled individuals.
		 *  If after:
		 *  	- The removal of un-sampled leaves and 
		 *  	- The removal of un-sampled individuals on the path to sampled individuals.
		 *  A root with an out degree = 1 remains, this individual can be removed.
		 */
		
		// Initialise a variable to record whether a root individual was removed
		int removed = 0;
		
		// Initialise a variable to store the IN and OUT degree of each individual
		int[] degree;
		
		// Loop through every individual on the adjacency matrix
		for(int row = 0; row < adjacency.length; row++){
			
			// Skip sampled individuals
			if(population.getTimeStepIndividualSampledIn(row) != -1){
				continue;
			}
			
			// Calculate the IN and OUT degree of the current individual
			degree = Methods.calculateInAndOutDegreeOfIndividual(row, adjacency);
			
			// Is the current individual a root, that we want to remove?
			if(degree[0] == 0 && degree[1] == 1){
				
				// Remove the root
				Methods.findInfecteeAndRemoveConnection(row, adjacency);
				removed = 1;
				
				break;
			}
		}
		
		// If we found an uninformative root, we may have created another
		if(removed == 1){
			removeRootIfNotInvolved(adjacency, population);
		}
	}

	public static void printSettings(String fileName, int simulationLength, int popSize, 
			double[] infectiousness, int genomeSize, double[] mutationRates, double[] transitionRates,
			int[] startEnd, double samplingProp, double nucleotideTransitionRate, Population population,
			int nInformativeSites, double sampledTransmissionTreeRate, int seed, int[] constantSiteCounts) throws IOException{
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Print the current date and time into the Patameter settings file
		String date = GeneralMethods.getCurrentDate("dd/MM/yyyy HH:mm");
		WriteToFile.write(bWriter, "Parameter Settings for Simulation: " + date + "\n");
		WriteToFile.write(bWriter, "Seed = " + seed + "\n");
		
		// Print out the  simulation parameters
		WriteToFile.write(bWriter, "Simulation Length: " + simulationLength + "\n");
		WriteToFile.write(bWriter, "Population Size: " + popSize + "\n");
		WriteToFile.write(bWriter, "Genome Size: " + genomeSize + "\n");
		for(int i = 0; i < infectiousness.length; i++){
			WriteToFile.write(bWriter, "State: " + i + "\tInfectiousness = " + infectiousness[i] + ", Mutation Rate = " + mutationRates[i] + ", LeavingRate = " + transitionRates[i] + "\n");
		}
		WriteToFile.write(bWriter, "Seed ID: " + Global.seedIndex + "\n");
		WriteToFile.write(bWriter, "Nucleotide Transition Rate: " + nucleotideTransitionRate + "\n");
		WriteToFile.write(bWriter, "Sampling Window: " + startEnd[0] + ", " + startEnd[1] + "\n");
		WriteToFile.write(bWriter, "Sampling Proportion: " + samplingProp + "\n");
		WriteToFile.write(bWriter, "Nucleotide Transition Rate: " + nucleotideTransitionRate + "\n");
		
		// Print out the simulation results
		WriteToFile.write(bWriter, "\nSimulation Results:\n");
		WriteToFile.write(bWriter, population.getSize() + " individuals were involved in this simulation.\n");
		WriteToFile.write(bWriter, Global.mutationEventNo + 1 + " mutations occured over the course of the simulation.\n");
		WriteToFile.write(bWriter, "Actual mutation rate = " + ArrayMethods.mean(Global.mutations) + "\t(" + (ArrayMethods.mean(Global.mutations) / genomeSize) + ")\n");
		WriteToFile.write(bWriter, "Substitution rate estimated on sampled transmission tree = " + sampledTransmissionTreeRate + "\t(" + (sampledTransmissionTreeRate / genomeSize) + ")\n");
		WriteToFile.write(bWriter, population.getNumberSampled() + " individuals were sampled.\n");
		WriteToFile.write(bWriter, nInformativeSites + " informative sites were found in their sequences.\n");
		WriteToFile.write(bWriter, "Constant Site Counts: A, C, G, T = " + ArrayMethods.toString(constantSiteCounts, ", ") + "\n");
			
		// Close the output file
		WriteToFile.close(bWriter);		
	}

	public static int[] calculateMutationWindowSize(Individual individual){
		
		// The mutation window if the period within an individual that the pathogen could mutate in
		int[] size = new int[2];
		size[0] = -1;
		
		// Check if the current individual was sampled
		if(individual.getTimeStepSampled() != -1){
			size[0] = individual.getTimeStepSampled() - individual.getInfectionStatusChanges()[1];
			size[1] = 1;
		
		//	Check that the individual recovered
		}else if(individual.getInfectionStatus() == 2){
			size[0] = individual.getInfectionStatusChanges()[2] - individual.getInfectionStatusChanges()[1];
		}
		
		return size;
	}
	
	public static void calculateMutationWindowSizes(Population population, String fileName) throws IOException{
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		WriteToFile.writeLn(bWriter, "WindowSize\tSampled");
		
		// Get a list of the individuals in the population
		Individual[] individuals = population.getIndividuals();
		
		// Initialise an array to store the calculated window sizes
		int[] windowSizes = new int[individuals.length];
		int pos = -1;
		int[] size;
		
		// Examine each individual in the population
		for(Individual individual : individuals){
			
			size = calculateMutationWindowSize(individual);
			
			if(size[0] != -1){
				pos++;
				windowSizes[pos] = size[0];
				
				WriteToFile.writeLn(bWriter, size[0] + "\t" + size[1]);
			}
		}
		
		// Close the output file
		WriteToFile.close(bWriter);
	}
}
