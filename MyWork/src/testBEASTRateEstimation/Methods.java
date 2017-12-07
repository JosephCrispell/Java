package testBEASTRateEstimation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Random;

import org.uncommons.maths.random.MersenneTwisterRNG;
import org.uncommons.maths.random.PoissonGenerator;

import methods.ArrayMethods;
import methods.GeneralMethods;
import methods.HashtableMethods;
import methods.MatrixMethods;
import methods.WriteToFile;

public class Methods {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		String fileName = "C:/Users/Joseph Crisp/Desktop/test.txt";
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		writeMCMCBlock(bWriter, 100000000, "run_1_JC_Strict_Constant");
		
		WriteToFile.close(bWriter);
	}
	
	// Population Methods
	public static void calculateForceOfInfection(Population population, double[] infectiousness){
		
		// Initialise a variable to store the probability of avoidance of susceptible individuals
		double probAvoidance = 1;
		double infectionProb;
		
		/**
		 *  Initialise an array to store source weights
		 *  Sources are weighted by their infectiousness
		 */
		double[] sourceWeights = new double[population.getSize()];
		
		// Examine each infectious individual in the population
		for(int id = 0; id < population.getSize(); id++){
			
			// Skip non-infectious individuals: Susceptible, Recovered (in SIR), and sampled
			if(infectiousness[population.getIndividualsInfectionStatus(id)] == 0  || 
					population.checkIfIndividualSampled(id) == true){
				continue;
			}
			
			/**
			 * Calculate the probability of avoidance of being infected by the current infectious individual
			 * for susceptible individuals
			 */
			infectionProb = infectiousness[population.getIndividualsInfectionStatus(id)];
				
			probAvoidance *= (1 - infectionProb);
			sourceWeights[id] = infectionProb;

		}
		
		// Store the calculated force of infection
		population.setForceOfInfection(1 - probAvoidance);
		
		// Store the source weights, specific to the sink's state
		population.setSourceWeights(sourceWeights);
	}

	public static void seedInfection(Population population, int seedStatus, Random random, int timeStep){
		
		// Randomly select an individual in the population
		int seedIndex = random.nextInt(population.getNumberOfIndividualsInCompartment(0));
		Global.seedIndex = seedIndex;
		
		// Infect that individual
		population.setIndividualsInfectionStatus(seedIndex, seedStatus, timeStep);
		
		// Begin the mutation process
		Global.mutationEventNo++;
		
		int[] events = {Global.mutationEventNo}; // First mutation event is zero
		population.addIndividualsMutationEvents(seedIndex, events, timeStep);
	}
	
	public static double calculateInfectionForce(double[] infectiousness, Population population){
		
		// Initialise a variable to store the probability of avoidance - note that we calculate avoidance rather than infection (1 - avoidance)
		double probabilityOfAvoidance = 1.0;
		
		// Initialise a variable to record how many individuals are in each particular compartment
		int nIndividualsInCompartment;
		
		// Examine each compartment and its associated infectiousness i.e. S I R, only I has an infectiousness != 0
		for(int i = 0; i < infectiousness.length; i++){
			
			// Get the number of individuals in the current compartment
			nIndividualsInCompartment = population.getNumberOfIndividualsInCompartment(i);
			
			// Check that the current compartment isn't empty and has an infectious != 0
			if(infectiousness[i] == 0 || nIndividualsInCompartment == 0){
				continue;
			}
			
			// Add the force of infection for the current compartment - note probabilities are combined by multiplying the probabilities of avoidance
			probabilityOfAvoidance = probabilityOfAvoidance * Math.pow((1.0 - infectiousness[i]), (double) nIndividualsInCompartment);
		}
		
		// Return the overall force of infection
		return 1.0 - probabilityOfAvoidance;
	}
	
	public static double[] generateWeightsForCompartments(double[] infectiousness, Population population){
		
		// Compartments are weighted by the infectiousness of their individuals
		double[] weights = new double[infectiousness.length];
		
		// Examine each compartment. Weight = infectiousness * nIndividuals
		for(int i = 0; i < infectiousness.length; i++){
			
			weights[i] = infectiousness[i] * population.getNumberOfIndividualsInCompartment(i);
		}
		
		return weights;
	}
	
	public static void infection(Population population, double[] infectiousness, Random random,
			int timeStep, PoissonGenerator[] randomPoissons) throws IOException{
		
		// Get the ids of the susceptible individuals in the population
		int[] susceptibles = population.getIndicesOfIndividualsInCompartment(0);
		
		// Initialise an array to store the IDs of the potential sources
		//int[] potentialSources;

		// Calculate the force of infection for all susceptible individuals in the previous timestep
		//double forceOfInfection = calculateInfectionForce(infectiousness, population);
		calculateForceOfInfection(population, infectiousness);
		
		// Calculate the weights (for choosing a source) for each compartment
		//double[] compartmentWeights = generateWeightsForCompartments(infectiousness, population);
		//int compartment;
		
		// Initialise variable to store source information
		int sourceIndex;
		
		// Get a list of all individuals IDs in the population
		int[] ids = ArrayMethods.range(0, population.getSize() - 1, 1);

		// Examine each susceptible individual in turn
		for(int id : susceptibles){
			
			// Did the current susceptible individual get infected in the previous timestep?
			if(random.nextDouble() < population.getForceOfInfection()){

				// Make a random weighted choice of the infection compartment
				//compartment = ArrayMethods.randomWeightedIndex(compartmentWeights, random);
				//potentialSources = population.getIndicesOfIndividualsInCompartment(compartment);
				
				// Choose a random source for the current individual
				//sourceIndex = ArrayMethods.randomChoice(potentialSources, random);
				sourceIndex = ArrayMethods.randomWeightedChoice(ids, population.getSourceWeights(), random);

				// Have any mutations occurred in the source's pathogen?
				population.mutateIndividualsSequence(sourceIndex, timeStep, randomPoissons);

				// Infect the current susceptible individual
				population.setIndividualsInfectionStatus(id, population.getIndividualsInfectionStatus(id) + 1, timeStep);
				population.addIndividualsMutationEvents(id, population.getIndividualsMutationEvents(sourceIndex), timeStep);
				
				// Record the transmission event
				population.addInfecteeToSourcesList(sourceIndex, id);
			}
		}
	}
	
	public static void recovery(Population population, double[] transitionRates, int timeStep,
			Random random, PoissonGenerator[] randomPoissons){
		
		/**
		 *  Get a list of the IDs of all individuals that could change state - they must be
		 *  in a compartment with an associated transition rate != 0
		 */
		int[] ids = new int[0];
		for(int i = 0; i < transitionRates.length; i++){
			
			if(transitionRates[i] != 0){
				ids = ArrayMethods.combine(ids, population.getIndicesOfIndividualsInCompartment(i));
			}			
		}
		
		// Examine each of these individuals in turn
		for(int id : ids){

			// Did the current individual change infection state?
			if(random.nextDouble() < transitionRates[population.getIndividualsInfectionStatus(id)]){

				// Update the current individuals mutation events
				population.mutateIndividualsSequence(id, timeStep, randomPoissons);
				
				// Change the current individuals infection status
				population.setIndividualsInfectionStatus(id, population.getIndividualsInfectionStatus(id) + 1, timeStep);				
			}
		}
	}
	
	public static void birth(Population population, int nStatesToConsider, int timeStep, int popLimit){
		
		/**
		 *  Calculate the number of births necessary to keep the population at a constant size.
		 *  	For example if we were interested in keeping the numbers of susceptible (S) and 
		 *  	infectious (I) individuals at a constant size (N) then:
		 *  		nBorn = N - (S + I)
		 */
		
		// Calculate the number of individuals in compartments of interest
		int sum = 0;
		for(int i = 0; i < nStatesToConsider; i++){
			sum += population.getNumberOfIndividualsInCompartment(i);
		}
		
		// Calculate the number of individuals to be born to keep the population at a constant level
		int nBorn = popLimit - sum;
				
		// Check that there are some individuals to add
		if(nBorn != 0){
			
			// Add each of the new individuals
			for(int i = 0; i < nBorn; i++){
				population.addIndividualToPopulation(timeStep);
			}			
		}
	}
	
	// Sampling Methods
	public static int calculateNumberInfectiousInPop(double[] infectiousness, Population population){
		
		// Initialise a variable to store the number of infectious individuals in the population
		int nIndividuals = 0;
		
		// Examine each of the compartments and their associated infectiousness
		for(int i = 0; i < infectiousness.length; i++){
		
			// If individuals in the current compartment are infectious, record the number in the compartment
			if(infectiousness[i] != 0){
				nIndividuals += population.getNumberOfIndividualsInCompartment(i); 
			}
		}
		
		return nIndividuals;
	}
	
	public static void sample(Population population, int timeStep, double prop,
			double[] infectiousness, Random random, double[] mutationRates, PoissonGenerator[] randomPoissons){
		
		/**
		 *  When an individual is sampled it is removed from the simulation. 
		 *  	No more infection or mutation
		 */
		
		// Calculate number of samples - only sampling infectious individuals
		int nSamples = (int) (prop * calculateNumberInfectiousInPop(infectiousness, population));

		/**
		 *  Calculate the weights (for choosing an individual to sample) for each compartment.
		 *  Weight is calculated as a function of the infectiousness of the compartment and 
		 *  the number of individuals in that compartment.
		 */
		double[] compartmentWeights = generateWeightsForCompartments(infectiousness, population);
		int compartment;
		int[] potentialSamplees;
		
		// Initialise a variable to store the index of each chosen individual
		int chosen;
		
		// Sample from the population
		for(int i = 0; i < nSamples; i++){
			
			// Make a random weighted choice of the infection compartment from which to sample
			compartment = ArrayMethods.randomWeightedIndex(compartmentWeights, random);
			potentialSamplees = population.getIndicesOfIndividualsInCompartment(compartment);
			
			// Choose a random individual to sample
			chosen = ArrayMethods.randomChoice(potentialSamplees, random);
			
			// Note when the current individual was sampled
			population.recordSamplingEvent(chosen, timeStep);
			
			// Update the sampled individual's mutation events sequence
			population.mutateIndividualsSequence(chosen, timeStep, randomPoissons);
		}
	}
	
	public static int findSource(int infectee, int[][] adjacencyMatrix){
		
		// Initialise a variable to store the ID of the source
		int source = -1;
		
		// Loop through every row in the adjacency matrix
		for(int i = 0; i < adjacencyMatrix.length; i++){
			
			// Search the infectee's column to find the source
			if(adjacencyMatrix[i][infectee] == 1){
				source = i;
				break;
			}
		}
		
		return source;
	}
	
	public static int[] returnPathToSource(int id, int[][] adjacencyMatrix, int source){
		
		// Initialise an array to store the individuals on the path from then sampled infectee to the seed
		int[] path = new int[9999];
		int index = -1;		
		int parent;
		
		// Begin tracing back through the path of transmission to the seed
		while(id != source){
			
			// Find the source to the current infectee
			parent = findSource(id, adjacencyMatrix);
			
			// Store the source found
			index++;
			path[index] = parent;
			
			// Move to the source - note will stop if reached seed
			id = parent;
		}
		
		// Only return the used positions of the array of sources
		return ArrayMethods.subset(path, 0, index);
	}
	
	public static int findAncestor(Population population, int[][] adjacencyMatrix){
		
		// Initialise a matrix to store the IDs of individuals on the path from each sampled individual to the seed
		int[][] pathsToSeed = new int[population.getNumberSampled()][0];
		
		// Get the IDs of the sampled individuals
		Hashtable<Integer, Integer> sampled = population.getSampled();
		int[] sampledIds = HashtableMethods.getKeysInt(sampled);
		
		// Initialise a variable to store the ancestors ID
		int ancestor = -1;
		
		// Follow the path from each sampled individual to the seed
		for(int i = 0; i < sampledIds.length; i++){
			
			// Get the transmission path to the seed for the current individual
			pathsToSeed[i] = returnPathToSource(sampledIds[i], adjacencyMatrix, Global.seedIndex);
			
			/**
			 *   If one of the sampled individuals has a path of length 1 or 0, it was either infected directly
			 *   by the seed or is the seed. Therefore the ancestor is the seed.
			 */
			if(pathsToSeed[i].length < 2){
				ancestor = Global.seedIndex;
			}
		}
		
		// Check we haven't already found the ancestor
		if(ancestor == -1){
			
			// Initialise variables for comparing the parents along each path to the seed
			int same;
			int parent;
			
			// Compare the paths for each of the sampled individuals - working from seed to tips
			for(int i = 1; i <= pathsToSeed[0].length; i++){
				
				// Check if the current parent on the path is same for all sampled
				parent = pathsToSeed[0][pathsToSeed[0].length - i];
				same = 1;
				for(int x = 1; x < sampledIds.length; x++){
					
					/**
					 *   If we have reached the end of the path to a sampled individual (root to tip) or the
					 *   current parent differs to that of the first sampled individual. Then we have just 
					 *   passed the ancestor (the last shared source, on the path from root to tip). 
					 *   Stop searching.
					 */
					if( i > pathsToSeed[x].length || pathsToSeed[x][pathsToSeed[x].length - i] != parent){
						same = 0;
						break;
					}				
				}
				
				/**
				 *  Once found a difference, the ancestor is the last shared source (i.e. the source at the
				 *  previous index on the path from root to tip.
				 */
				if(same == 0){
					ancestor = pathsToSeed[0][pathsToSeed[0].length - (i - 1)];
					break;
				}
			}
			
			// If we never found an ancestor then the seed must be the last shared source
			if(ancestor == -1){
				ancestor = Global.seedIndex;
			}
		}
			
		return ancestor;
	}
	
	// Estimating the substitution rate on the Transmission Tree
	public static double estimateSubstitutionRateForTransmissionTree(int[][] adjacencyMatrix, Population population,
			int simulationLength, int genomeSize){
		
		/**
		 * Examine each branch of the transmission tree.
		 * For each branch calculate:
		 * 	- The genetic distance between the source and sink
		 *  - Temporal distance - time over which variation could accrue
		 *  		PeriodInSourceAfterInfection = SourceRemovalTime - SinkInfectionTime
		 *  		PeriodInSink = SinkRemovalTime - SinkInfectionTime
		 *  - Substitution rate = geneticDistance / temporalDistance
		 */
		
		// Initialise variables to calculate the temporal distance
		double sourceRemovalTime;
		double sinkInfectionTime;
		double sinkRemovalTime;
		double periodInSource;
		double periodInSink;
		
		// Create an array to store the substitution rate estimations for each branch
		int nDifferences;
		double sumTemporalDistance = 0;
		double sumGeneticDistance = 0;

		// Examine each of the branches in the sampled transmission tree
		for(int i = 0; i < adjacencyMatrix.length; i++){
			
			// Check that the source's sequences have been created
			checkIndividualsSequence(i, population);
			
			for(int j = 0; j < adjacencyMatrix.length; j++){
				
				// Skip empty elements
				if(adjacencyMatrix[i][j] == 0){
					continue;
				}
				
				// Check that the sink's sequences have been created
				checkIndividualsSequence(i, population);
				checkIndividualsSequence(j, population);

				// Get the Removal Times for the source and sink
				sourceRemovalTime = getRemovalTime(i, population, simulationLength);
				sinkRemovalTime = getRemovalTime(j, population, simulationLength);
				
				// Get the Infection Time for the sink
				sinkInfectionTime = population.getIndividualsStatusChanges(j)[1];
				
				// Calculate the period spent in source after infection
				periodInSource = sourceRemovalTime - sinkInfectionTime;
				
				// Calculate the period spent in sink
				periodInSink = sinkRemovalTime - sinkInfectionTime;
				
				// Calculate the genetic distance
				nDifferences = calculateDistance(population.getIndividualsSequence(i), population.getIndividualsSequence(j));
				sumGeneticDistance += calculateJukesCantorDistance(nDifferences, genomeSize);
				
				// Calculate the temporal distance
				sumTemporalDistance += periodInSource + periodInSink;
				
				// Remove the sink's sequence to save RAM
				population.setIndividualsSequence(j, null);
				
//				System.out.println("\n##### TRANSMISSION EVENT #####");
//				System.out.println("Infection Event Time = " + sinkInfectionTime);
//				System.out.println("Source Removal Time = " + sourceRemovalTime + "\t" + ArrayMethods.toString(population.getIndividualsStatusChanges(i), ", ") + "\t\t" + population.getTimeStepIndividualSampledIn(i));
//				System.out.println("\tTime Spent in Source = " + periodInSource);
//				System.out.println("Sink Removal Time =   " + sinkRemovalTime  + "\t" + ArrayMethods.toString(population.getIndividualsStatusChanges(j), ", ") + "\t\t" + population.getTimeStepIndividualSampledIn(j));
//				System.out.println("\tTime Spent in Sink = " + periodInSink);
//				System.out.println("-----");
//				System.out.println("Source Sequence: " + ArrayMethods.toString(population.getIndividualsSequence(i), ""));
//				System.out.println("Sink Sequence:   " + ArrayMethods.toString(population.getIndividualsSequence(j), ""));
//				System.out.println("-----");
//				System.out.println("Temporal Distance = " + (periodInSource + periodInSink));
//				System.out.println("Genetic Distance = " + calculateDistance(population.getIndividualsSequence(i), population.getIndividualsSequence(j)));
			}
			
			// Remove the source's sequence to save RAM
			population.setIndividualsSequence(i, null);
		}
		
		return sumGeneticDistance / sumTemporalDistance;
	}
	
	public static double estimateSubstitutionRateForSampledTransmissionTree(int[][] sampledAdjacencyMatrix, 
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
			
			// Check that the source's sequences have been created
			checkIndividualsSequence(i, population);
						
			for(int j = 0; j < sampledAdjacencyMatrix.length; j++){
				
				// Skip empty elements
				if(sampledAdjacencyMatrix[i][j] == 0){
					continue;
				}
				
//				System.out.println("\nTransmission Event: " + i + " -> " + j);
				
				// Check that the source's and sink's sequences have been created
				checkIndividualsSequence(i, population);
				checkIndividualsSequence(j, population);

				// Get the Removal Times for the source and sink
				sourceRemovalTime = getRemovalTime(i, population, simulationLength);
				sinkRemovalTime = getRemovalTime(j, population, simulationLength);
				
				// Calculate the Time Left Source
				timeLeftSource = findTimeLeftSource(i, j, adjacencyMatrix, population);
				
				// Calculate the period spent in source after infection
				periodInSource = sourceRemovalTime - timeLeftSource;
				
				// Calculate the period spent in sink
				periodInSink = sinkRemovalTime - timeLeftSource;
				
				// Calculate the genetic distance
				nDifferences = calculateDistance(population.getIndividualsSequence(i), population.getIndividualsSequence(j));
				sumGeneticDistance += calculateJukesCantorDistance(nDifferences, genomeSize);
								
				// Calculate the temporal distance
				sumTemporalDistance += periodInSource + periodInSink;
				
				
				
//				System.out.println("Source Information: " + i);
//				System.out.println("State changes: " + ArrayMethods.toString(population.getIndividualsStatusChanges(i), ", "));
//				System.out.println("Sampling Time = " + population.getTimeStepIndividualSampledIn(i));
//				System.out.println("Removal Time = " + sourceRemovalTime);
//				System.out.println("-------------------------");
//				System.out.println("Sink Information: " + j);
//				System.out.println("State changes: " + ArrayMethods.toString(population.getIndividualsStatusChanges(j), ", "));
//				System.out.println("Sampling Time = " + population.getTimeStepIndividualSampledIn(j));
//				System.out.println("Removal Time = " + sinkRemovalTime);
//				System.out.println("-------------------------");
//				System.out.println("Time Left Source = " + timeLeftSource);
//				System.out.println("Period in Source = " + periodInSource);
//				System.out.println("Period in Sink = " + periodInSink);
//				System.out.println("Temporal Distance = " + (periodInSource + periodInSink));
//				System.out.println("-------------------------");
//				System.out.println("Source Sequence = " + ArrayMethods.toString(population.getIndividualsSequence(i), ""));
//				System.out.println("Source Sequence = " + ArrayMethods.toString(population.getIndividualsSequence(j), ""));
//				System.out.println("Genetic Distance = " + nDifferences);
//				
//				System.out.println("\n\nSum Genetic Distance = " + sumGeneticDistance);
//				System.out.println("Sum Temporal Distance = " + sumTemporalDistance + "\n\n");
				
				
				// Remove the sink's sequence to save RAM
				population.setIndividualsSequence(j, null);
				
				
			}
			
			// Remove the source's sequence to save RAM
			population.setIndividualsSequence(i, null);
		}
		
		return sumGeneticDistance / sumTemporalDistance;
	}
	
	public static int getRemovalTime(int index, Population population, int simulationLength){
		
		int removalTime = -1;
		
		// Check if the source was sampled
		if(population.getTimeStepIndividualSampledIn(index) != -1){
			removalTime = population.getTimeStepIndividualSampledIn(index);
		
		// Check if the source ever recovered
		}else if(population.getIndividualsStatusChanges(index)[2] != 0){
			removalTime = population.getIndividualsStatusChanges(index)[2];
		
		// Set removal to end of simulation
		}else{
			removalTime = simulationLength;
		}
		
		return removalTime;
	}
	
	public static void checkIndividualsSequence(int index, Population population){
		
		// Initialise an array to store the individuals sequence (as integers)
		int[] sequenceAsIntegers;
		
		// Check the individual's sequence has been created
		if(population.getIndividualsSequence(index) == null){
			
			// Create the sequence as a series of Integers - these encode specific nucleotides. A = 0; C = 1; G = 2; T = 3
			sequenceAsIntegers = createIntegerSequence(Global.reference, population.getIndividualsMutationEvents(index), Global.mutationEventInfo);
			
			// Convert the sequence of Integers into a sequence of nucleotides
			population.setIndividualsSequence(index, createSequence(sequenceAsIntegers));
		}
	}
	
	public static int findTimeLeftSource(int source, int sink, int[][] adjacencyMatrix, Population population){
		
		/**
		 * The time left source will be the infection time of the first infected individual on the
		 * transmission path from the source to the sink.
		 * We must follow the transmission path from the sink to the source until we find this individual.
		 */

		// Get the IDs of the individuals on the transmission path from the sink to the source
		int[] pathToSource = returnPathToSource(sink, adjacencyMatrix, source);
		
		//System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		//System.out.println(source + "\t" + sink);
		//System.out.println("\tPath To Source: " + ArrayMethods.toString(pathToSource, ","));
		
		// Check whether the sink was infected directly by the source
		int firstInfectedIndividualOnPath = -1;
		if(pathToSource.length == 1){
			
			firstInfectedIndividualOnPath = sink;
			//System.out.println("DIRECT TRANSMISSION - " + firstInfectedIndividualOnPath + "\t" + ArrayMethods.toString(population.getIndividualsStatusChanges(firstInfectedIndividualOnPath), ", "));
		
		// Get the ID of the first individual infected on the path from the source to the sink
		}else{
			firstInfectedIndividualOnPath = pathToSource[pathToSource.length - 2];
			//System.out.println("INDIRECT TRANSMISSION - " + firstInfectedIndividualOnPath + "\t" + ArrayMethods.toString(population.getIndividualsStatusChanges(firstInfectedIndividualOnPath), ", "));
		}
		
		return population.getIndividualsStatusChanges(firstInfectedIndividualOnPath)[1];
	}
	
	public static double calculateJukesCantorDistance(int nDifferences, int genomeSize){
		
		/**
		 * Given a proportion (p) of sites that differ between two sequences the Jukes-Cantor estimate
		 * of the evolutionary distance - in terms of the expected number of changes is given by
		 * 		-3/4 * ln(1 - (4/3) * p)
		 * 		a	 * ln(1 - b)
		 */
		
		double proportion = (double) nDifferences / (double) genomeSize;
		
		double distance = 0;
		if(nDifferences != 0){
			
			double a = -3.0/4.0;
			double b = (4.0/3.0) * proportion;
			distance = a * Math.log(1.0 - b);
		}
		
		return distance * (double) genomeSize;
	}
	
	public static int countNodesOfSampledTransmissionTree(int[][] sampledAdjacencyMatrix, Population population) throws IOException{
		
		return MatrixMethods.sum(sampledAdjacencyMatrix) + 1;
	}
	
	// Surveying Methods
 	public static void printPopulationStatus(int timeStep, Population population, BufferedWriter bWriter) throws IOException{
		
 		// Write the current time step to file
		WriteToFile.write(bWriter, timeStep);
		
		// Print the number of individuals in each of the compartments
		for(int i = 0; i < population.getNumberInfectionStatuses(); i++){
			
			WriteToFile.write(bWriter, "\t" + population.getNumberOfIndividualsInCompartment(i));			
		}
		
		// Print out the number of individuals sampled
		WriteToFile.write(bWriter, "\t" + population.getNumberSampled() + "\n");
	}
	
	public static void printHeaderForSurveyFile(BufferedWriter bWriter, int nInfectionStates) throws IOException{
		
		// Write the first column header to file
		WriteToFile.write(bWriter, "TimeStep");
		
		// Write the index of each of the infection states to file
		for(int i = 0; i < nInfectionStates; i++){
			WriteToFile.write(bWriter, "\t" + i);
		}
		
		// Write the last column header to file
		WriteToFile.write(bWriter, "\tnSampled\n");
	}
	
	// New Methods for converting mutation events into sequences
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
	
	public static int[][] defineMutationEvents(double[][] substitutionMatrix, Random random,
			int[] referenceSequence){
		
		// Initialise a Hashtable to record the positions where mutations fall
		Global.sitesUsed = new Hashtable<Integer, Integer>();
		
		// Initialise a table to store the Mutation Event information. {position, refIndex, altIndex}
		int[][] eventInfo = new int[Global.mutationEventNo + 1][3];
				
		// Define each of the mutation events
		for(int i = 0; i < Global.mutationEventNo + 1; i++){
					
			// Randomly choose a position on the reference genome
			eventInfo[i][0] = ArrayMethods.randomIndex(referenceSequence.length, random);
					
			// Get the Reference allele
			eventInfo[i][1] = referenceSequence[eventInfo[i][0]];
					
			// Create an alternate allele - random weighted choice using the substitution matrix
			eventInfo[i][2] = ArrayMethods.randomWeightedIndex(substitutionMatrix[eventInfo[i][1]], random);
			
			// Did the mutation fall on a site where a previous mutation had occurred?
			if(Global.sitesUsed.get(eventInfo[i][0]) != null){
				Global.noMutationsFellOnUsedSite++;
			}else{
				// Record where the mutation fell
				Global.sitesUsed.put(eventInfo[i][0], 1);
			}			
		}
		
		return eventInfo;
	}
	
	public static int[] createIntegerSequence(int[] referenceSequence, int[] events, int[][] eventInfo){
		
		// Copy the reference sequence - represented as integers. A = 0; C = 1; G = 2; T = 3
		int[] sequence = ArrayMethods.copy(referenceSequence);
		
		// Insert each of the mutations defined by the mutation event information
		for(int event : events){
			sequence[eventInfo[event][0]] = eventInfo[event][2];
		}
		
		return sequence;
	}
	
	public static char[] createSequence(int[] sequenceAsIntegers){
		
		// Create an array storing the different nucleotides
		char[] nucleotides = {'A', 'C', 'G', 'T'};
		
		// Initialise an array to store the nucleotide sequence
		char[] sequence = new char[sequenceAsIntegers.length];
		
		// Examine each position on the sequence
		for(int i = 0; i < sequenceAsIntegers.length; i++){
			
			// Convert the Integer code into its associated nucleotide. A = 0; C = 1; G = 2; T = 3
			sequence[i] = nucleotides[sequenceAsIntegers[i]];			
		}
		
		return sequence;
	}

	public static char[][] getSampledSequences(Population population, int genomeSize){
		
		// Get the IDs of the sampled individuals
		int[] sampledIds = population.getIdsOfSampledIndividuals();
		
		// Initialise an matrix to store each of the sampled individuals sequences
		char[][] sequences = new char[population.getNumberSampled()][genomeSize];
		
		// Insert each of the sampled individual's sequences into the matrix
		for(int i = 0; i < sampledIds.length; i++){
			
			// Create the individual's sequence
			checkIndividualsSequence(sampledIds[i], population);
			
			sequences[i] = population.getIndividualsSequence(sampledIds[i]);
			
			// Remove the individual's sequence to save RAM
			population.setIndividualsSequence(sampledIds[i], null);
		}
		
		return sequences;
	}
	
	public static int[] findInformativeSites(Population population, int genomeSize){
		
		// Initialise an array to record whether each site in the "alignment" is informative
		int[] informative = new int[genomeSize];
				
		// Get all the sequences from the sampled individuals
		char[][] sequences = getSampledSequences(population, genomeSize);
		char compareTo;
		
		// Compare the sequences of the sampled individuals to one another
		for(int i = 0; i < genomeSize; i++){
			
			// Get the Integer code for the nucleotide at the current position
			compareTo = sequences[1][i];
			
			// Check if the current position in the sequence ever had a mutation event
			if(Global.sitesUsed.get(i) == null){
				
				/**
				 *  Found a non-informative site - no mutation fell on this site - Record 
				 *  the nucleotide at the current position as a constant site.
				 */
				if(compareTo == 'A'){
					Global.constantSiteCounts[0]++;
				}else if(compareTo == 'C'){
					Global.constantSiteCounts[1]++;
				}else if(compareTo == 'G'){
					Global.constantSiteCounts[2]++;
				}else if(compareTo == 'T'){
					Global.constantSiteCounts[3]++;
				}else{
					System.out.println("Unknown Allele Found: " + compareTo);
				}
				
				// Skip to the next site
				continue;
			}
			
			/**
			 *  If the current position did mutate, check each individual in the population to
			 *  see if there is variation at this site in the sampled population.
			 */
			for(int x = 1; x < population.getNumberSampled(); x++){
				
				// If the current individual's nucleotide different to that of the first individual?
				if(sequences[x][i] != compareTo){
					informative[i] = 1;
					break;
				}
				
				// Have we reached the last individual without finding any variation?
				if(x == population.getNumberSampled() - 1){
					
					// Found a non-informative site. Record the nucleotide at the current position as a constant site.
					if(compareTo == 'A'){
						Global.constantSiteCounts[0]++;
					}else if(compareTo == 'C'){
						Global.constantSiteCounts[1]++;
					}else if(compareTo == 'G'){
						Global.constantSiteCounts[2]++;
					}else if(compareTo == 'T'){
						Global.constantSiteCounts[3]++;
					}else{
						System.out.println("Unknown Allele Found: " + compareTo);
					}
				}
				
				
			}
		}
		
		return informative;
	}
	
	public static void createSequencesForSampledIndividuals(Population population, double tsProb, Random random,
			int genomeSize, double[] nucleotideWeights){
		
		// Create a Substitution Matrix
		double[][] substitutionMatrix = createHKYSubstitutionMatrix(tsProb);
		
		// Create Reference Sequence
		Global.reference = generateRandomNucleotideSequence(nucleotideWeights, genomeSize, random);
		
		// Define the mutation events
		Global.mutationEventInfo = defineMutationEvents(substitutionMatrix, random, Global.reference);
		
		// Initialise variables to store each sampled individual's sequence
		int[] sequenceAsIntegers = new int[genomeSize];
		
		// Create each of the sampled individuals sequences
		int[] sampledIds = population.getIdsOfSampledIndividuals();
		for(int id : sampledIds){
			
			// Create the sequence as a series of Integers - these encode specific nucleotides. A = 0; C = 1; G = 2; T = 3
			sequenceAsIntegers = createIntegerSequence(Global.reference, population.getIndividualsMutationEvents(id), Global.mutationEventInfo);
			
			// Convert the sequence of Integers into a sequence of nucleotides
			population.setIndividualsSequence(id, createSequence(sequenceAsIntegers));
		}		
	}
	
	public static int printSampledSequences(Population population, int genomeSize, String format, String fileName) throws IOException{
		
		// Find the informative sites
		int[] informativeSites = findInformativeSites(population, genomeSize);
				
		/**
		 *  Initialise a string to store the information to go into the output file - building
		 *  the whole file string before writing it to file.
		 */
		String output = "";
				
		// Get the IDs of the sampled individuals
		int[] sampledIds = population.getIdsOfSampledIndividuals();
		
		// Print out the header for the NEXUS file - if this is the format wanted
		if(format.matches("NEXUS")){
			
			output = output + "#NEXUS\n\n\nBEGIN DATA;\nDIMENSIONS NTAX=" + sampledIds.length + " NCHAR=" + ArrayMethods.sum(informativeSites) + ";\nFORMAT MISSING=N GAP=- DATATYPE=DNA;\nMATRIX\n";
		}		
				
		// Print out the informative sites for each of the isolates into the output file
		char[] sequence;
		String sequenceToPrint = "";
		for(int i = 0; i < sampledIds.length; i++){
				
			// Wipe the nucleotide sequence to print
			sequenceToPrint = "";
			
			// Create the individual's sequence
			checkIndividualsSequence(sampledIds[i], population);
			
			// Extract the nucleotide sequence for the current individual
			sequence = population.getIndividualsSequence(sampledIds[i]);
					
			// Print out the isolate id - add an ">" if FASTA format
			if(format.matches("FASTA")){
				
				output = output + ">" + sampledIds[i] + "\t";
			
			// If the output is in NEXUS format - add in the timestep sampled
			}else if(format.matches("NEXUS")){
				
				output = output + "Individual-" + sampledIds[i] + "_" + population.getTimeStepIndividualSampledIn(sampledIds[i]) + "\t";
			}
		
			// Create the sequence of informative sites for the current individual
			for(int x = 0; x < sequence.length; x++){
				
				// Only add informative sites
				if(informativeSites[x] == 1){
					sequenceToPrint = sequenceToPrint + sequence[x];
				}
			}
			
			// Print the sequence of informative sites into the output file
			output = output + sequenceToPrint + "\n";
			
			// Remove the individual's sequence to save RAM
			population.setIndividualsSequence(sampledIds[i], null);
		}
		
		// Print out the end of the NEXUS file
		if(format.matches("NEXUS")){
			output = output + ";\nEND;\n";
		}
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		WriteToFile.write(bWriter, output);		
		
		// Close the output file
		WriteToFile.close(bWriter);
		
		// Return the number of informative sites
		return ArrayMethods.sum(informativeSites);		
	}
	
	public static int calculateDistance(char[] a, char[] b){
		
		// Initialise a variable to count the differences between the sequences
		int diff = 0;
		
		/**
		 *  Get a list of the positions on the genome that changed - don't need to look for
		 *  differences at any other site
		 */
		int[] mutationPositions = HashtableMethods.getKeysInt(Global.sitesUsed);
		
		// Examine each mutation position in the sequence
		for(int i = 0; i < mutationPositions.length; i++){
			
			// Count the number of different sites observed
			if(a[mutationPositions[i]] != b[mutationPositions[i]]){
				diff++;
			}
		}
		
		return diff;
	}
	
	public static void printGeneticDistancesForSampledIndividuals(Population population, String fileName) throws IOException{
		
		// Get the IDs of the sampled individuals
		int[] sampledIds = population.getIdsOfSampledIndividuals();
		
		// Initialise a matrix to store the calculated genetic distances
    	int[][] distances = new int[sampledIds.length][sampledIds.length];
    	
    	// Initialise a variable to store the genetic distance for each comparison
    	int distance;
    	
    	// Initialise a string to store the genetic distances to go into the output file
    	String header = "";
    	String output = "";
    	
    	// Compare every sampled individual to every other
    	for(int i = 0; i < sampledIds.length; i++){
    		
    		// Build the header for the output file
    		header = header + sampledIds[i] + "\t";
    		
    		// Begin building the current row of the output table
    		output = output + sampledIds[i];
    		
    		for(int j = 0; j < sampledIds.length; j++){
    			
    			// Avoid making the same comparison twice and ignore the diagonal
    			if(i < j){
    				
    				// Calculate the genetic distance between the current individuals
            		distance = calculateDistance(population.getIndividualsSequence(sampledIds[i]), population.getIndividualsSequence(sampledIds[j]));
        			
            		// Record the genetic distance calculated. Note the genetic distance matrix is symmetric
            		distances[i][j] = distance;
        			distances[j][i] = distance;
    			}
    			
    			// Print the genetic distance of every comparison out to file
    			output = output + "\t" + distances[i][j];
    		}
    		
    		// Finish the current row of the genetic distance table in the output
    		output = output + "\n";
    	}
    	
    	// Remove trailing tab from header
	    header = header.substring(0, header.length() - 1);
		
	    // Print the output information into the genetic distances file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		WriteToFile.write(bWriter, header + "\n" + output);
		
		// Close the output file
		WriteToFile.close(bWriter);
	}
	
	// Recording Mutation Events Methods
 	public static void updateMutationEvents(Population population, double[] mutationRates,
			PoissonGenerator[] randomPoissons, int timeStep){
		
		/**
		 *  Get a list of all individuals with an active infection - i.e. in an infection
		 *  state with a mutation rate != 0
		 */
		int[] ids = new int[0];
		for(int i = 0; i < mutationRates.length; i++){
					
			if(mutationRates[i] != 0){
				ids = ArrayMethods.combine(ids, population.getIndicesOfIndividualsInCompartment(i));
			}			
		}
		
		// Examine each of the those individuals in turn
		for(int id : ids){
			
			// Mutate their sequence
			population.mutateIndividualsSequence(id, timeStep, randomPoissons);
		}
	}
 		
	public static void createConstantSiteCountInsert(String fileName) throws IOException{
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Begin build the output string
		String output = "\t<!-- The unique patterns from 1 to end                                       -->\n\t<!-- npatterns=1204                                                          -->\n\t<mergePatterns id=\"patterns\">\n\t\t<patterns from=\"1\" strip=\"false\">\n\t\t\t<alignment idref=\"alignment\"/>\n\t\t</patterns>\n\n\t\t<constantPatterns>\n\t\t\t<alignment idref=\"alignment\"/>\n\t\t\t<counts>\n\t\t\t\t";
		output = output + "<parameter value=\"" + ArrayMethods.toString(Global.constantSiteCounts, " ") + "\"/>\n";
		output = output + "\t\t\t</counts>\n\t\t</constantPatterns>\n\t</mergePatterns>\n";
		
		// Write the output string to file
		WriteToFile.write(bWriter, output);
		
		// Close the output file
		WriteToFile.close(bWriter);
	}
	
	public static PoissonGenerator[] generatePoissonDistributionsAroundMutationRates(double[] rates, Random random){
		PoissonGenerator[] randomPoissons = new PoissonGenerator[rates.length];
		
		// Examine each of the compartments and their associated mutation rate
		for(int i = 0; i < rates.length; i++){
			
			// If the rate is none-zero create a poisson distribution around it
			if(rates[i] != 0){
				randomPoissons[i] = new PoissonGenerator(rates[i], random);
			}
		}
		
		return randomPoissons;
	}
	
	// Print Settings Method
	public static void printSettings(String fileName, int simulationLength, int popSize, 
			double[] infectiousness, int genomeSize, double[] mutationRates, double[] transitionRates, int seedStatus,
			int[] startEnd, double prop, double nucleotideTransitionRate, Population population,
			int nInformativeSites, double sampledTransmissionTreeRate, int seed) throws IOException{
		
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
		WriteToFile.write(bWriter, "Seed Status: " + seedStatus + "\n");
		WriteToFile.write(bWriter, "Seed ID: " + Global.seedIndex + "\n");
		WriteToFile.write(bWriter, "Nucleotide Transition Rate: " + nucleotideTransitionRate + "\n");
		WriteToFile.write(bWriter, "Sampling Window: " + startEnd[0] + ", " + startEnd[1] + "\n");
		WriteToFile.write(bWriter, "Sampling Proportion: " + prop + "\n");
		WriteToFile.write(bWriter, "Nucleotide Transition Rate: " + nucleotideTransitionRate + "\n");
		
		// Print out the simulation results
		WriteToFile.write(bWriter, "\nSimulation Results:\n");
		WriteToFile.write(bWriter, population.getSize() + " individuals were involved in this simulation.\n");
		WriteToFile.write(bWriter, Global.mutationEventNo + 1 + " mutations occured over the course of the simulation.\n");
		WriteToFile.write(bWriter, "Actual mutation rate = " + ArrayMethods.mean(Global.mutations) + "\t(" + (ArrayMethods.mean(Global.mutations) / genomeSize) + ")\n");
		WriteToFile.write(bWriter, "Substitution rate estimated on sampled transmission tree = " + sampledTransmissionTreeRate + "\t(" + (sampledTransmissionTreeRate / genomeSize) + ")\n");
		WriteToFile.write(bWriter, population.getNumberSampled() + " individuals were sampled.\n");
		WriteToFile.write(bWriter, nInformativeSites + " informative sites were found in their sequences.\n");
		WriteToFile.write(bWriter, "Constant Site Counts: A, C, G, T = " + ArrayMethods.toString(Global.constantSiteCounts, ", ") + "\n");
			
		// Close the output file
		WriteToFile.close(bWriter);		
	}

	// Transmission/Phylogenetic Tree Methods
	public static int[][] buildAdjacencyMatrix(Population population, String fileName) throws IOException{
		
		// Get a list of the individuals in the population
		Individual[] individuals = population.getIndividuals();
		
		// Initialise an adjacency matrix to record the transmission events that occurred during the simulation
		int[][] adjacencyMatrix = new int[population.getSize()][population.getSize()];
		
		// Initialise a temporary array to store the infectees of each individual
		int[] infectees;
		
		// Initialise a string to store the transmission events
		String output = "";
		
		// Examine who each of the individuals in the population infected
		for(int i = 0; i < individuals.length; i++){
			
			// Skip those that never infected anyone
			if(individuals[i].getLastInfecteeIndex() == -1){
				continue;
			}
			
			// Get the infectees of the current individual
			infectees = individuals[i].getInfectees();
			
			// Record each transmission event. Note the adjacency matrix is non-symmetric
			for(int j = 0; j < infectees.length; j++){
				adjacencyMatrix[i][infectees[j]] = 1;
				
				if(fileName.matches("none") == false){
					output = output + i + "," + infectees[j] + "\n";
				}
			}
		}
		
		// Print the Transmission event information out to file
		if(fileName.matches("none") == false){
			BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
			WriteToFile.write(bWriter, output);
			WriteToFile.close(bWriter);
		}						
		
		return adjacencyMatrix;
	}

	public static void printTransmissionTree(int[][] adjacencyMatrix, String fileName) throws IOException{
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Print each branch of the adjacency matrix
		for(int i = 0; i < adjacencyMatrix.length; i++){
			for(int j = 0; j < adjacencyMatrix[0].length; j++){
				
				if(adjacencyMatrix[i][j] == 1){
					WriteToFile.writeLn(bWriter, i + "," + j);
				}
			}
		}
		
		// Close the output file
		WriteToFile.close(bWriter);
	}
	
	public static void removeUnsampledIndividualsOnPathToSampledIndividuals(int[][] adjacency,
			Hashtable<Integer, Integer> sampled){
		
		// Initialise a variable to store the calculated degree
		int[] degree;
		
		// Initialise variables to store source and infectee indices
		int sourceIndex;
		int infecteeIndex;
			
		for(int row = 0; row < adjacency.length; row++){
			
			// Skip sampled individuals
			if(sampled.get(row) != null){
				continue;
			}
			
			// Calculate the degree for the current individual
			degree = calculateInAndOutDegreeOfIndividual(row, adjacency);
			
			/**
			 *  Remove individuals with degree = 2 - they are un-sampled individuals on the path
			 *  to sampled individuals.
			 */
			
			if(degree[0] == 1 && degree[1] == 1){
				
				// Find the source of infection for the current individual
				sourceIndex = findSourceAndRemoveConnection(row, adjacency);
				
				// Find the infectee of the current individual
				infecteeIndex = findInfecteeAndRemoveConnection(row, adjacency);
				
				// Connect the source and the infectee
				adjacency[sourceIndex][infecteeIndex] = 1;				
			}		
		}		
	}
	
	public static int findSourceAndRemoveConnection(int infectee, int[][] adjacencyMatrix){
		
		// Initialise a variable to store the ID of the source
		int source = -1;
		
		// Loop through every row in the adjacency matrix
		for(int i = 0; i < adjacencyMatrix.length; i++){
			
			// Search the infectee's column to find the source
			if(adjacencyMatrix[i][infectee] == 1){
				source = i;
				
				adjacencyMatrix[i][infectee] = 0;
				break;
			}
		}
		
		return source;
	}
	
	public static int findInfecteeAndRemoveConnection(int source, int[][] adjacency){
		
		int infectee = -1;
		
		for(int col = 0; col < adjacency[source].length; col++){
			
			if(adjacency[source][col] == 1){
				infectee = col;
				
				adjacency[source][col] = 0;
				break;
			}
			
		}
		
		return infectee;
	}
	
	public static void removeUnsampledLeaves(int[][] adjacency, Hashtable<Integer, Integer> sampled){
		
		/**
		 *  Examine each row of the adjacency matrix - remove those that aren't sampled and didn't infect
		 *  anyone - un-sampled leaves.
		 *  
		 *  These individuals will have a degree (IN + OUT) of 0.
		 */
		
		// Initialise a variable to store the calculated degree
		int[] degree;
		
		// Initialise a variable to keep count of how many un-sampled leaves were found
		int nFound = 0;
		
		for(int row = 0; row < adjacency.length; row++){
			
			// Skip sampled individuals
			if(sampled.get(row) != null){
				continue;
			}
			
			// Calculate the degree (IN and OUT) for the current individual
			degree = calculateInAndOutDegreeOfIndividual(row, adjacency);
			
			// Remove any individuals with degree = 1, infected but never infected anyone
			if(degree[0] == 1 && degree[1] == 0){
				nFound++;
				removeLeaf(adjacency, row);
			}
		}
		
		// If we removed some un-sampled leaves - check matrix again for more
		if(nFound > 0){
			removeUnsampledLeaves(adjacency, sampled);
		}
	}
	
	public static int[] calculateInAndOutDegreeOfIndividual(int index, int[][] adjacency){
		
		// Get the Out degree for the current individual - number of individuals it infected
		int[] degree = new int[2];
		degree[1] = ArrayMethods.sum(adjacency[index]);
		
		/**
		 *  Get the In degree for the current individual - should be 1 unless individual has been removed
		 *  from the transmission tree.
		 */
		for(int row = 0; row < adjacency.length; row++){
			
			// Find connection to check individual still on tree
			if(adjacency[row][index] == 1){
				degree[0] += adjacency[row][index];
				break;
			}
		}
		
		return degree;
	}
	
	public static void removeLeaf(int[][] adjacency, int index){
		
		// Transmission tree is recorded in adjacency matrix - and edge is from row to col (non-symmetric)
		for(int row = 0; row < adjacency[index].length; row++){
			
			// Find the edge to the current individual
			if(adjacency[row][index] == 1){
				adjacency[row][index] = 0;
				break;
			}
		}		
	}
	
	public static void removeRootIfNotInvolved(int[][] adjacency, Hashtable<Integer, Integer> sampled){
		
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
			if(sampled.get(row) != null){
				continue;
			}
			
			// Calculate the IN and OUT degree of the current individual
			degree = calculateInAndOutDegreeOfIndividual(row, adjacency);
			
			// Is the current individual a root, that we want to remove?
			if(degree[0] == 0 && degree[1] == 1){
				
				// Remove the root
				findInfecteeAndRemoveConnection(row, adjacency);
				removed = 1;
				
				break;
			}
		}
		
		// If we found an uninformative root, we may have created another
		if(removed == 1){
			removeRootIfNotInvolved(adjacency, sampled);
		}
	}
	
	// Create BEAST XML file
	public static void createBeastXML(String filePrefix, Population population, int nInformativeSites,
			int chainLength, String path, int[] constantSiteCounts, int genomeSize) throws IOException{
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(path + filePrefix + ".xml", false);
		
		// Get the IDs of the sampled individuals
		int[] sampledIds = population.getIdsOfSampledIndividuals();
		
		// Write to top of the XML file
		writeStartOfXML(bWriter);
		
		// Write the taxa block
		int[] minMax = writeTaxaBlock(bWriter, sampledIds, population);
	
		// Write the alignment block
		writeAlignmentBlock(bWriter, sampledIds, population, nInformativeSites, genomeSize);
		
		// Write the Constant Site Count Block
		writeConstantSiteCountBlock(bWriter, constantSiteCounts);
		
		// Write the blocks associated with the Population Dynamics
		writeTreeModelBlocks(bWriter, minMax);
		
		// Write the blocks describing the substitution process
		writeSubstitutionProcessBlocks(bWriter);
		
		// Write the Operators block
		writeOperatorsBlock(bWriter, minMax);
		
		// Write the MCMC settings block
		writeMCMCBlock(bWriter, chainLength, filePrefix);
		
		// Close the output file
		WriteToFile.close(bWriter);		
	}
	
	public static void writeStartOfXML(BufferedWriter bWriter) throws IOException{
		
		WriteToFile.write(bWriter, "<?xml version=\"1.0\" standalone=\"yes\"?>\n\n");
		WriteToFile.write(bWriter, "<!-- Generated by BEAUTi v1.8.2                                              -->\n");
		WriteToFile.write(bWriter, "<!--       by Alexei J. Drummond, Andrew Rambaut and Marc A. Suchard         -->\n");
		WriteToFile.write(bWriter, "<!--       Department of Computer Science, University of Auckland and        -->\n");
		WriteToFile.write(bWriter, "<!--       Institute of Evolutionary Biology, University of Edinburgh        -->\n");
		WriteToFile.write(bWriter, "<!--       David Geffen School of Medicine, University of California, Los Angeles-->\n");
		WriteToFile.write(bWriter, "<!--       http://beast.bio.ed.ac.uk/                                        -->\n");
		WriteToFile.write(bWriter, "<beast>\n\n");
	}
	
	public static int[] writeTaxaBlock(BufferedWriter bWriter, int[] sampledIds, Population population) throws IOException{
		
		WriteToFile.write(bWriter, "\t<!-- The list of taxa to be analysed (can also include dates/ages).          -->\n");
		WriteToFile.write(bWriter, "\t<!-- ntax=" + sampledIds.length + "                                          -->\n");
		
		WriteToFile.write(bWriter, "\t<taxa id=\"taxa\">\n");
		
		// Initialise an array to store the min and max time steps sampled
		int[] minMax = new int[2];
		minMax[0] = 99999999;
		
		for(int id : sampledIds){
			
			WriteToFile.write(bWriter, "\t\t<taxon id=\"Individual-" + id + "_" + population.getTimeStepIndividualSampledIn(id) + "\">\n");
			WriteToFile.write(bWriter, "\t\t\t<date value=\"" + population.getTimeStepIndividualSampledIn(id) + ".0\" direction=\"forwards\" units=\"years\"/>\n");
			WriteToFile.write(bWriter, "\t\t</taxon>\n");
			
			// Have we found a new minimum timestep sampled?
			if(population.getTimeStepIndividualSampledIn(id) < minMax[0]){
				minMax[0] = population.getTimeStepIndividualSampledIn(id);
			}
			
			// Have we found a new maximum timestep sampled?
			if(population.getTimeStepIndividualSampledIn(id) > minMax[1]){
				minMax[1] = population.getTimeStepIndividualSampledIn(id);
			}
		}
		
		WriteToFile.write(bWriter, "\t</taxa>\n\n");
		
		return minMax;
	}
	
	public static void writeAlignmentBlock(BufferedWriter bWriter, int[] sampledIds, Population population,
			int nInformativeSites, int genomeSize) throws IOException{
		
		// Find the informative Sites
		int[] informativeSites = findInformativeSites(population, genomeSize);
		
		WriteToFile.write(bWriter, "\t<!-- The sequence alignment (each sequence refers to a taxon above).         -->\n");
		WriteToFile.write(bWriter, "\t<!-- ntax=" + sampledIds.length + " nchar=" + ArrayMethods.sum(informativeSites) + "                                                     -->\n");
		WriteToFile.write(bWriter, "\t<alignment id=\"alignment\" dataType=\"nucleotide\">\n");
				
		// Initialise a variable to store each sampled individual's sequence
		char[] sequence;
		String outputSequence = "\t\t\t";
		
		for(int id : sampledIds){
			
			outputSequence = "\t\t\t";
			
			WriteToFile.write(bWriter, "\t\t<sequence>\n");
			WriteToFile.write(bWriter, "\t\t\t<taxon idref=\"Individual-" + id + "_" + population.getTimeStepIndividualSampledIn(id) + "\"/>\n");
			
			// Get the individual's sequence
			checkIndividualsSequence(id, population);
			sequence = returnSequenceWithOnlyInformativeSites(informativeSites, population.getIndividualsSequence(id), genomeSize);			
			
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
	
	public static char[] returnSequenceWithOnlyInformativeSites(int[] informativeSites, char[] sequence,
			int genomeSize){
		
		char[] newSequence = new char[genomeSize];
		int pos = -1;
		
		for(int i = 0; i < informativeSites.length; i++){
			
			if(informativeSites[i] == 1){
				pos++;
				newSequence[pos] = sequence[i];
			}
		}
		
		return ArrayMethods.subset(newSequence, 0, pos);
	}
	
	public static void writeConstantSiteCountBlock(BufferedWriter bWriter, int[] constantSiteCounts) throws IOException{
		
		WriteToFile.write(bWriter, "\t<!-- The unique patterns from 1 to end                                       -->\n");
		WriteToFile.write(bWriter, "\t<!-- npatterns=1150                                                          -->\n");
		WriteToFile.write(bWriter, "\t<mergePatterns id=\"patterns\">\n");
		WriteToFile.write(bWriter, "\t\t<patterns from=\"1\" strip=\"false\">\n");
		WriteToFile.write(bWriter, "\t\t\t<alignment idref=\"alignment\"/>\n");
		WriteToFile.write(bWriter, "\t\t</patterns>\n\n");
		WriteToFile.write(bWriter, "\t\t<constantPatterns>\n");
		WriteToFile.write(bWriter, "\t\t\t<alignment idref=\"alignment\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t<counts>\n");
		WriteToFile.write(bWriter, "\t\t\t\t<parameter value=\"" + ArrayMethods.toString(constantSiteCounts, " ") + "\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t</counts>\n");
		WriteToFile.write(bWriter, "\t\t</constantPatterns>\n");		
		WriteToFile.write(bWriter, "\t</mergePatterns>\n\n");
		
	}
	
	public static void writeTreeModelBlocks(BufferedWriter bWriter, int[] minMax) throws IOException{
		
		// Calculate the number of timesteps between the first and last sampled individual
		int nTimeSteps = minMax[1] - minMax[0];
		
		WriteToFile.write(bWriter, "\t<!-- A prior assumption that the population size has remained constant       -->\n");
		WriteToFile.write(bWriter, "\t<!-- throughout the time spanned by the genealogy.                           -->\n");
		WriteToFile.write(bWriter, "\t<constantSize id=\"constant\" units=\"years\">\n");
		WriteToFile.write(bWriter, "\t\t<populationSize>\n");
		WriteToFile.write(bWriter, "\t\t\t<parameter id=\"constant.popSize\" value=\"" + nTimeSteps + "0.0\" lower=\"0.0\"/>\n");
		WriteToFile.write(bWriter, "\t\t</populationSize>\n");
		WriteToFile.write(bWriter, "\t</constantSize>\n\n");
		
		WriteToFile.write(bWriter, "\t<!-- Generate a random starting tree under the coalescent process            -->\n");
		WriteToFile.write(bWriter, "\t<coalescentSimulator id=\"startingTree\">\n");
		WriteToFile.write(bWriter, "\t\t<taxa idref=\"taxa\"/>\n");
		WriteToFile.write(bWriter, "\t\t<constantSize idref=\"constant\"/>\n");
		WriteToFile.write(bWriter, "\t</coalescentSimulator>\n\n");
		
		WriteToFile.write(bWriter, "\t<!-- Generate a tree model                                                   -->\n");
		WriteToFile.write(bWriter, "\t<treeModel id=\"treeModel\">\n");
		WriteToFile.write(bWriter, "\t\t<coalescentTree idref=\"startingTree\"/>\n");
		WriteToFile.write(bWriter, "\t\t<rootHeight>\n");
		WriteToFile.write(bWriter, "\t\t\t<parameter id=\"treeModel.rootHeight\"/>\n");
		WriteToFile.write(bWriter, "\t\t</rootHeight>\n");
		WriteToFile.write(bWriter, "\t\t<nodeHeights internalNodes=\"true\">\n");
		WriteToFile.write(bWriter, "\t\t\t<parameter id=\"treeModel.internalNodeHeights\"/>\n");
		WriteToFile.write(bWriter, "\t\t</nodeHeights>\n");
		WriteToFile.write(bWriter, "\t\t<nodeHeights internalNodes=\"true\" rootNode=\"true\">\n");
		WriteToFile.write(bWriter, "\t\t\t<parameter id=\"treeModel.allInternalNodeHeights\"/>\n");
		WriteToFile.write(bWriter, "\t\t</nodeHeights>\n");
		WriteToFile.write(bWriter, "\t</treeModel>\n\n");
		
		WriteToFile.write(bWriter, "\t<!-- Generate a coalescent likelihood                                        -->\n");
		WriteToFile.write(bWriter, "\t<coalescentLikelihood id=\"coalescent\">\n");
		WriteToFile.write(bWriter, "\t\t<model>\n");
		WriteToFile.write(bWriter, "\t\t\t<constantSize idref=\"constant\"/>\n");
		WriteToFile.write(bWriter, "\t\t</model>\n");
		WriteToFile.write(bWriter, "\t\t<populationTree>\n");
		WriteToFile.write(bWriter, "\t\t\t<treeModel idref=\"treeModel\"/>\n");
		WriteToFile.write(bWriter, "\t\t</populationTree>\n");
		WriteToFile.write(bWriter, "\t</coalescentLikelihood>\n\n");		
		
	}
	
	public static void writeSubstitutionProcessBlocks(BufferedWriter bWriter) throws IOException{
		
		WriteToFile.write(bWriter, "\t<!-- The strict clock (Uniform rates across branches)                        -->\n");
		WriteToFile.write(bWriter, "\t<strictClockBranchRates id=\"branchRates\">\n");
		WriteToFile.write(bWriter, "\t\t<rate>\n");
		WriteToFile.write(bWriter, "\t\t\t<parameter id=\"clock.rate\" value=\"1.0\" lower=\"0.0\"/>\n");
		WriteToFile.write(bWriter, "\t\t</rate>\n");
		WriteToFile.write(bWriter, "\t</strictClockBranchRates>\n\n");
		
		WriteToFile.write(bWriter, "\t<!-- The general time reversible (GTR) substitution model                    -->\n");
		WriteToFile.write(bWriter, "\t<gtrModel id=\"gtr\">\n");
		WriteToFile.write(bWriter, "\t\t<frequencies>\n");
		WriteToFile.write(bWriter, "\t\t\t<frequencyModel dataType=\"nucleotide\">\n");
		WriteToFile.write(bWriter, "\t\t\t\t<frequencies>\n");
		WriteToFile.write(bWriter, "\t\t\t\t\t<parameter id=\"frequencies\" value=\"0.25 0.25 0.25 0.25\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t\t</frequencies>\n");
		WriteToFile.write(bWriter, "\t\t\t</frequencyModel>\n");
		WriteToFile.write(bWriter, "\t\t</frequencies>\n");
		WriteToFile.write(bWriter, "\t\t<rateAC>\n");
		WriteToFile.write(bWriter, "\t\t\t<parameter id=\"ac\" value=\"1.0\" lower=\"0.0\"/>\n");
		WriteToFile.write(bWriter, "\t\t</rateAC>\n");
		WriteToFile.write(bWriter, "\t\t<rateAG>\n");
		WriteToFile.write(bWriter, "\t\t\t<parameter id=\"ag\" value=\"1.0\" lower=\"0.0\"/>\n");
		WriteToFile.write(bWriter, "\t\t</rateAG>\n");
		WriteToFile.write(bWriter, "\t\t<rateAT>\n");
		WriteToFile.write(bWriter, "\t\t\t<parameter id=\"at\" value=\"1.0\" lower=\"0.0\"/>\n");
		WriteToFile.write(bWriter, "\t\t</rateAT>\n");
		WriteToFile.write(bWriter, "\t\t<rateCG>\n");
		WriteToFile.write(bWriter, "\t\t\t<parameter id=\"cg\" value=\"1.0\" lower=\"0.0\"/>\n");
		WriteToFile.write(bWriter, "\t\t</rateCG>\n");
		WriteToFile.write(bWriter, "\t\t<rateGT>\n");
		WriteToFile.write(bWriter, "\t\t\t<parameter id=\"gt\" value=\"1.0\" lower=\"0.0\"/>\n");
		WriteToFile.write(bWriter, "\t\t</rateGT>\n");
		WriteToFile.write(bWriter, "\t</gtrModel>\n\n");
		
		WriteToFile.write(bWriter, "\t<!-- site model                                                              -->\n");
		WriteToFile.write(bWriter, "\t<siteModel id=\"siteModel\">\n");
		WriteToFile.write(bWriter, "\t\t<substitutionModel>\n");
		WriteToFile.write(bWriter, "\t\t\t<gtrModel idref=\"gtr\"/>\n");
		WriteToFile.write(bWriter, "\t\t</substitutionModel>\n");
		WriteToFile.write(bWriter, "\t</siteModel>\n\n");
		
		WriteToFile.write(bWriter, "\t<!-- Likelihood for tree given sequence data                                 -->\n");
		WriteToFile.write(bWriter, "\t<treeLikelihood id=\"treeLikelihood\" useAmbiguities=\"false\">\n");
		WriteToFile.write(bWriter, "\t\t<patterns idref=\"patterns\"/>\n");
		WriteToFile.write(bWriter, "\t\t<treeModel idref=\"treeModel\"/>\n");
		WriteToFile.write(bWriter, "\t\t<siteModel idref=\"siteModel\"/>\n");
		WriteToFile.write(bWriter, "\t\t<strictClockBranchRates idref=\"branchRates\"/>\n");
		WriteToFile.write(bWriter, "\t</treeLikelihood>\n\n");
		
	}
	
	public static void writeOperatorsBlock(BufferedWriter bWriter, int[] minMax) throws IOException{
		
		// Calculate the number of timesteps between the first and last sampled individual
		int nTimeSteps = minMax[1] - minMax[0];
		
		WriteToFile.write(bWriter, "\t<!-- Define operators                                                        -->\n");
		WriteToFile.write(bWriter, "\t<operators id=\"operators\" optimizationSchedule=\"default\">\n");
		WriteToFile.write(bWriter, "\t\t<scaleOperator scaleFactor=\"0.75\" weight=\"3\">\n");
		WriteToFile.write(bWriter, "\t\t\t<parameter idref=\"clock.rate\"/>\n");
		WriteToFile.write(bWriter, "\t\t</scaleOperator>\n");
		WriteToFile.write(bWriter, "\t\t<subtreeSlide size=\"" + nTimeSteps + ".0\" gaussian=\"true\" weight=\"15\">\n");
		WriteToFile.write(bWriter, "\t\t\t<treeModel idref=\"treeModel\"/>\n");
		WriteToFile.write(bWriter, "\t\t</subtreeSlide>\n");
		WriteToFile.write(bWriter, "\t\t<narrowExchange weight=\"15\">\n");
		WriteToFile.write(bWriter, "\t\t\t<treeModel idref=\"treeModel\"/>\n");
		WriteToFile.write(bWriter, "\t\t</narrowExchange>\n");
		WriteToFile.write(bWriter, "\t\t<wideExchange weight=\"3\">\n");
		WriteToFile.write(bWriter, "\t\t\t<treeModel idref=\"treeModel\"/>\n");
		WriteToFile.write(bWriter, "\t\t</wideExchange>\n");
		WriteToFile.write(bWriter, "\t\t<wilsonBalding weight=\"3\">\n");
		WriteToFile.write(bWriter, "\t\t\t<treeModel idref=\"treeModel\"/>\n");
		WriteToFile.write(bWriter, "\t\t</wilsonBalding>\n");
		WriteToFile.write(bWriter, "\t\t<scaleOperator scaleFactor=\"0.75\" weight=\"3\">\n");
		WriteToFile.write(bWriter, "\t\t\t<parameter idref=\"treeModel.rootHeight\"/>\n");
		WriteToFile.write(bWriter, "\t\t</scaleOperator>\n");
		WriteToFile.write(bWriter, "\t\t<uniformOperator weight=\"30\">\n");
		WriteToFile.write(bWriter, "\t\t\t<parameter idref=\"treeModel.internalNodeHeights\"/>\n");
		WriteToFile.write(bWriter, "\t\t</uniformOperator>\n");
		WriteToFile.write(bWriter, "\t\t<scaleOperator scaleFactor=\"0.75\" weight=\"3\">\n");
		WriteToFile.write(bWriter, "\t\t\t<parameter idref=\"constant.popSize\"/>\n");
		WriteToFile.write(bWriter, "\t\t</scaleOperator>\n");
		WriteToFile.write(bWriter, "\t\t<upDownOperator scaleFactor=\"0.75\" weight=\"3\">\n");
		WriteToFile.write(bWriter, "\t\t\t<up>\n");
		WriteToFile.write(bWriter, "\t\t\t\t<parameter idref=\"clock.rate\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t</up>\n");
		WriteToFile.write(bWriter, "\t\t\t<down>\n");
		WriteToFile.write(bWriter, "\t\t\t\t<parameter idref=\"treeModel.allInternalNodeHeights\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t</down>\n");
		WriteToFile.write(bWriter, "\t\t</upDownOperator>\n");
		WriteToFile.write(bWriter, "\t</operators>\n\n");
	}
	
	public static void writeMCMCBlock(BufferedWriter bWriter, int chainLength, String filePrefix) throws IOException{
		
		WriteToFile.write(bWriter, "\t<!-- Define MCMC                                                             -->\n");
		WriteToFile.write(bWriter, "\t<mcmc id=\"mcmc\" chainLength=\"" + chainLength + "\" autoOptimize=\"true\" operatorAnalysis=\"" + filePrefix + ".ops.txt\">\n");
		WriteToFile.write(bWriter, "\t\t<posterior id=\"posterior\">\n");
		WriteToFile.write(bWriter, "\t\t\t<prior id=\"prior\">\n");
		WriteToFile.write(bWriter, "\t\t\t\t<gammaPrior shape=\"0.05\" scale=\"10.0\" offset=\"0.0\">\n");
		WriteToFile.write(bWriter, "\t\t\t\t\t<parameter idref=\"ac\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t\t</gammaPrior>\n");
		WriteToFile.write(bWriter, "\t\t\t\t<gammaPrior shape=\"0.05\" scale=\"20.0\" offset=\"0.0\">\n");
		WriteToFile.write(bWriter, "\t\t\t\t\t<parameter idref=\"ag\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t\t</gammaPrior>\n");
		WriteToFile.write(bWriter, "\t\t\t\t<gammaPrior shape=\"0.05\" scale=\"10.0\" offset=\"0.0\">\n");
		WriteToFile.write(bWriter, "\t\t\t\t\t<parameter idref=\"at\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t\t</gammaPrior>\n");
		WriteToFile.write(bWriter, "\t\t\t\t<gammaPrior shape=\"0.05\" scale=\"10.0\" offset=\"0.0\">\n");
		WriteToFile.write(bWriter, "\t\t\t\t\t<parameter idref=\"cg\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t\t</gammaPrior>\n");
		WriteToFile.write(bWriter, "\t\t\t\t<gammaPrior shape=\"0.05\" scale=\"10.0\" offset=\"0.0\">\n");
		WriteToFile.write(bWriter, "\t\t\t\t\t<parameter idref=\"gt\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t\t</gammaPrior>\n");
		WriteToFile.write(bWriter, "\t\t\t\t<uniformPrior lower=\"0.0\" upper=\"1.0E100\">\n");
		WriteToFile.write(bWriter, "\t\t\t\t\t<parameter idref=\"clock.rate\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t\t</uniformPrior>\n");
		WriteToFile.write(bWriter, "\t\t\t\t<oneOnXPrior>\n");
		WriteToFile.write(bWriter, "\t\t\t\t\t<parameter idref=\"constant.popSize\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t\t</oneOnXPrior>\n");
		WriteToFile.write(bWriter, "\t\t\t\t<coalescentLikelihood idref=\"coalescent\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t</prior>\n");
		WriteToFile.write(bWriter, "\t\t\t<likelihood id=\"likelihood\">\n");
		WriteToFile.write(bWriter, "\t\t\t\t<treeLikelihood idref=\"treeLikelihood\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t</likelihood>\n");
		WriteToFile.write(bWriter, "\t\t</posterior>\n");
		WriteToFile.write(bWriter, "\t\t<operators idref=\"operators\"/>\n\n");
		
		WriteToFile.write(bWriter, "\t\t<!-- write log to screen                                                     -->\n");
		WriteToFile.write(bWriter, "\t\t<log id=\"screenLog\" logEvery=\"" + (chainLength / 10000) + "\">\n");
		WriteToFile.write(bWriter, "\t\t\t<column label=\"Posterior\" dp=\"4\" width=\"12\">\n");
		WriteToFile.write(bWriter, "\t\t\t\t<posterior idref=\"posterior\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t</column>\n");
		WriteToFile.write(bWriter, "\t\t\t<column label=\"Prior\" dp=\"4\" width=\"12\">\n");
		WriteToFile.write(bWriter, "\t\t\t\t<prior idref=\"prior\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t</column>\n");
		WriteToFile.write(bWriter, "\t\t\t<column label=\"Likelihood\" dp=\"4\" width=\"12\">\n");
		WriteToFile.write(bWriter, "\t\t\t\t<likelihood idref=\"likelihood\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t</column>\n");
		WriteToFile.write(bWriter, "\t\t\t<column label=\"rootHeight\" sf=\"6\" width=\"12\">\n");
		WriteToFile.write(bWriter, "\t\t\t\t<parameter idref=\"treeModel.rootHeight\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t</column>\n");
		WriteToFile.write(bWriter, "\t\t\t<column label=\"clock.rate\" sf=\"6\" width=\"12\">\n");
		WriteToFile.write(bWriter, "\t\t\t\t<parameter idref=\"clock.rate\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t</column>\n");
		WriteToFile.write(bWriter, "\t\t</log>\n\n");
		
		
		WriteToFile.write(bWriter, "\t\t<!-- write log to file                                                       -->\n");
		WriteToFile.write(bWriter, "\t\t<log id=\"fileLog\" logEvery=\"" + (chainLength / 10000) + "\" fileName=\"" + filePrefix + ".log.txt\" overwrite=\"false\">\n");
		WriteToFile.write(bWriter, "\t\t\t<posterior idref=\"posterior\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t<prior idref=\"prior\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t<likelihood idref=\"likelihood\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t<parameter idref=\"treeModel.rootHeight\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t<parameter idref=\"constant.popSize\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t<parameter idref=\"ac\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t<parameter idref=\"ag\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t<parameter idref=\"at\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t<parameter idref=\"cg\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t<parameter idref=\"gt\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t<parameter idref=\"clock.rate\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t<treeLikelihood idref=\"treeLikelihood\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t<coalescentLikelihood idref=\"coalescent\"/>\n");
		WriteToFile.write(bWriter, "\t\t</log>\n\n");
		
		WriteToFile.write(bWriter, "\t\t<!-- write tree log to file                                                  -->\n");
		WriteToFile.write(bWriter, "\t\t<logTree id=\"treeFileLog\" logEvery=\"" + (chainLength / 10000) + "\" nexusFormat=\"true\" fileName=\"" + filePrefix + ".trees.txt\" sortTranslationTable=\"true\">\n");
		WriteToFile.write(bWriter, "\t\t\t<treeModel idref=\"treeModel\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t<trait name=\"rate\" tag=\"rate\">\n");
		WriteToFile.write(bWriter, "\t\t\t\t<strictClockBranchRates idref=\"branchRates\"/>\n");
		WriteToFile.write(bWriter, "\t\t\t</trait>\n");
		WriteToFile.write(bWriter, "\t\t\t<posterior idref=\"posterior\"/>\n");
		WriteToFile.write(bWriter, "\t\t</logTree>\n");
		
		
		WriteToFile.write(bWriter, "\t</mcmc>\n");
		WriteToFile.write(bWriter, "\t<report>\n");
		WriteToFile.write(bWriter, "\t\t<property name=\"timer\">\n");
		WriteToFile.write(bWriter, "\t\t\t<mcmc idref=\"mcmc\"/>\n");
		WriteToFile.write(bWriter, "\t\t</property>\n");
		WriteToFile.write(bWriter, "\t</report>\n");
		
		WriteToFile.write(bWriter, "</beast>\n");
		
	}
	
	// OLD Genetic Distance Method
    public static int calculateDistanceOLD(Hashtable<Integer, Integer> a, Hashtable<Integer, Integer> b){
    	/**
    	 * Method to Compare two Sequences of Mutation Events
    	 * 	Mutation Event Sequences Record which of the Mutations (which occurred over the course
    	 * 	of the simulation occurred in the pathogen carried by the current individual.
    	 * 
    	 * 	Example:
    	 * 		SequenceA = {1,	2,	3,	5,	7,	8,		13}
    	 * 		SequenceB = {1,	2,			7,		9};
    	 * 
    	 * 	noShared = 3
    	 * 	noDifferences = (A.length - noShared) + (B.length - noShared)
    	 * 	noDifferences = (7 - 3) + (4 - 3)
    	 * 
    	 *	noDifferences = 5
    	 *
    	 *	Number of Differences is used to define the genetic distance between two samples.
    	 */
    	
    	// Initialise a variable to count the number of shared events
    	int  noShared = 0;
    	
    	// Get a list of the mutation events for a
    	int[] mutationEventsA = HashtableMethods.getKeysInt(a);
    	
    	// For each of a's mutation events, check if b has the same one
    	for(int event : mutationEventsA){
    		
    		if(b.get(event) != null){

    			noShared++;
    		}    		
    	}

    	// Return the number of Differences
    	return (a.size() - noShared) + (b.size() - noShared);
    }
}
