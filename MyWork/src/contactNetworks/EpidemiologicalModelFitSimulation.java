package contactNetworks;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.MatrixMethods;
import methods.WriteToFile;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import badgerPopulation.GridMethods;
import filterSensitivity.DistanceMatrix;
import filterSensitivity.DistanceMatrixMethods;
import filterSensitivity.NJTreeMethods;
import filterSensitivity.Node;

public class EpidemiologicalModelFitSimulation {

	public static void main(String[] args) throws IOException {

		
		/** 
		 * Run an SIR Model 
		 * 
		 * 	Simple infection ran on a simple network. Mutation events are recorded in each individual's pathogen. All 
		 *  individuals (that have come into contact with the disease) are sampled at the end of the simulation and Genetic Distance Matrix is generated according
		 *  to the mutation events. Spatial Distance is calculated using the Network represented as an adjacency matrix.
		 *  
		 *   - No birth, death, migration
		 *   - Contact Network is static
		 *   - Infection can spread through direct contact on network or spatial spread between groups
		 *   
		 *  Aim is to establish a correlation between the genetic distance between individuals and their spatial
		 *  association. Pearson's Correlation coefficient is used to provide a correlation metric for genetic vs. 
		 *  spatial distance.
		 */
		
		// Parameters
		int popSize = 100;
		int noGroups = 25;
		int groupSize = popSize / noGroups;
		double[] infectiousness = 	{ 0, 0.00015,  0};
		double[] transitions = 		{ 0, 0.00015, 0};
		double[] mutationRates =	{ 0, 0.002,  0};
		int[] gridSize = 			{250, 250};
		double recordThreshold = 0.5;
		
		// Take in command line arguments
		mutationRates[1] = Double.parseDouble(args[0]);
		infectiousness[1] = Double.parseDouble(args[1]);
		transitions[1] = Double.parseDouble(args[2]);
		popSize = Integer.parseInt(args[3]);
		noGroups = Integer.parseInt(args[4]);
		recordThreshold = Double.parseDouble(args[5]);
		String simulationOutputFile = args[6];
		BufferedWriter r0File = WriteToFile.openFile(args[7], true);
				
		int simulationLength = 200000;
		double samplingEfficiency = 1;
		
		int distanceCap = 9999;
		int trace = 0;
			
		// Run Simulations
		int noSimulations = 1;
		int x = 0;
		while(x < noSimulations){
			
			// Create Initial Population
			Individual[] population = createInitialPopulation(popSize, transitions);
			
			// Assign Individuals to Groups
			Individual[][] groups = createGroupedPopulation(population, noGroups);
			
			// Randomly Place Groups on Grid
			double[][] groupDistanceMatrix = createGroupDistanceMatrix(noGroups, gridSize[0], gridSize[1]);
			
			// Run the Simulation
			Results results = runSimulationModelGroupSpatial(transitions, infectiousness, simulationLength, 1, mutationRates, simulationLength, samplingEfficiency, groups, groupDistanceMatrix, distanceCap, trace, popSize);
			
			System.out.print(".");
			
			// Record the Results of the Simulation
			if(results.getSampledIndividuals().length >= recordThreshold*popSize){
				
				// Record Simulation Variables: Genetic, Spatial, Temporal, Network Distances
				printSimulationVariablesTable(results, simulationOutputFile);
				
				// Calculate the R0 for the Simulation
				double averageDistance = DistanceMatrixMethods.findAverageDistance(groupDistanceMatrix);
				double r0 = calculateBasicReproductionNumber(infectiousness, transitions, averageDistance, popSize, groupSize);
				WriteToFile.writeLn(r0File, r0);
				
				x++;
				System.out.print("\n");
			}
		}
		
		WriteToFile.close(r0File);
	}
	
	public static Results runSimulationModelNetwork(double[] transitions, double[] infectiousness, int length,
			int seedStateIndex, double[] mutationRates, int sampleTime, double samplingEfficacy, Individual[] population,
			double[][] distanceMatrix){
	
		// Create Instance of a Random Number Generator
		MersenneTwister random = new MersenneTwister();
	
		// Initialise the Output Distance Matrices
		double[][] geneticDistanceMatrix = new double[0][0];
		double[][] spatialDistanceMatrix = new double[0][0];
		Individual[] sampled = new Individual[0];
		
		// Seed the Infection
		Individual seed = IndividualMethods.randomChoice(population);
		int[] mutationEvents = new int[1];
		mutationEvents[0] = Global.mutationEventNo;
		seed.setStatusIndex(seedStateIndex);
		seed.setMutationEvents(mutationEvents);
		
		int[] timeEnteredStates = seed.getTimeEnteredStates();
		timeEnteredStates[seedStateIndex - 1] = 1;
		seed.setTimeEnteredStates(timeEnteredStates);
		
		// Record the number of individuals in each population state
		int[] summary = new int[transitions.length];
		summary[0] = population.length - 1;
		summary[seed.getStatusIndex()]++;
				
		// Run a Simulation
		for(int timeStep = 1; timeStep <= length; timeStep++){
			
			// Find Infectious Individuals
			for(Individual infectedIndividual : IndividualMethods.findInfectiousIndividuals(population, infectiousness)){

				/**
				 *  Did the Source's pathogen mutate in the current Time Step
				 *  In this way the sequences of infectious individuals is updated every timeStep
				 */
				mutationEvents = IndividualMethods.mutateSequence(mutationRates[infectedIndividual.getStatusIndex()], timeStep, infectedIndividual.getMutationEvents(), infectedIndividual.getMutationsLastChecked());
			
				// Update Sources Mutation Events
				infectedIndividual.setMutationEvents(mutationEvents);
				infectedIndividual.setMutationsLastChecked(timeStep);
				
				// Did this individual infect anyone else during the current time step?
				for(Individual contact : infectedIndividual.getContacts()){
					
					if(random.nextDouble() < infectiousness[infectedIndividual.getStatusIndex()] && contact.getStatusIndex() == 0){
						
						// Record in Population Summary
						summary[contact.getStatusIndex()]--;
						
						// Infect the Current Individual
						contact.setStatusIndex(contact.getStatusIndex() + 1);
						contact.setMutationEvents(mutationEvents);
						contact.setMutationsLastChecked(timeStep);
						summary[contact.getStatusIndex()]++;
						
						// Append this contact to the list of Individuals the current Infected individual has infected
						Individual[] infected = infectedIndividual.getIndividualsInfected();
						infected = IndividualMethods.append(infected, contact);
						infectedIndividual.setIndividualsInfected(infected);
						
						// Record When Contact Individual Moved into new State
						timeEnteredStates = contact.getTimeEnteredStates();
						timeEnteredStates[contact.getStatusIndex() - 1] = timeStep;
						contact.setTimeEnteredStates(timeEnteredStates);
						
					}
				}
				
				/**
				 * Did the Infected Individual's infection Progress
				 * 
				 * states = { S I R }
				 * transitions = { 0 0.1 0 }
				 */
				if(random.nextDouble() < transitions[infectedIndividual.getStatusIndex()]){
					
					// Record in Population Summary
					summary[infectedIndividual.getStatusIndex()]--;
					
					// Update Individual's infection Status
					infectedIndividual.setStatusIndex(infectedIndividual.getStatusIndex() + 1);
					summary[infectedIndividual.getStatusIndex()]++;
					
					// Record When Individual Moved into new State
					timeEnteredStates = infectedIndividual.getTimeEnteredStates();
					timeEnteredStates[infectedIndividual.getStatusIndex() - 1] = timeStep;
					infectedIndividual.setTimeEnteredStates(timeEnteredStates);
					
				}
				
			}
			
			// Examine Population Summary
			//System.out.println(Arrays.toString(summary));
			
			
			// Sample Now?
			if(timeStep == sampleTime || ArrayMethods.multiply(summary, infectiousness) == 0 || summary[0] == 0){
							
				sampled = new Individual[population.length];
				int posUsed = -1;
				
				// Attempt to Sample each individual in the population
				for(int i = 0; i < population.length; i++){
					if(random.nextDouble() < samplingEfficacy && population[i].getStatusIndex() != 0){
						posUsed++;
						sampled[posUsed] = population[i];
					}
				}
				
				// Subset to remove empty positions in sampled list
				sampled = IndividualMethods.subset(sampled, 0, posUsed);
				
				// Check if any mutations have occurred in any of the individuals since last checked
				updateIndividualsSequences(sampled, timeStep, mutationRates);
				
				// Build the Genetic Distance Matrix
				geneticDistanceMatrix = IndividualMethods.buildGeneticDistanceMatrix(sampled);
				
				// Build the Spatial Distance Matrix
				spatialDistanceMatrix = IndividualMethods.buildSpatialDistanceMatrix(sampled, distanceMatrix);
				
				// Print TimeStep to record when simulation ended
				System.out.println("Simulation ended " + (length - timeStep) + " step(s) early.\n" + Arrays.toString(summary));
				
				// Finish Simulation
				break;
			}
		}
		
		// Simulation Finished
		return new Results(population, sampled, geneticDistanceMatrix, spatialDistanceMatrix);
	}

	public static Results runSimulationModelSpatial(double[] transitions, double[] infectiousness, int length,
			int seedStateIndex, double[] mutationRates, int sampleTime, double samplingEfficacy, Individual[] population,
			double[][] distanceMatrix, int distanceCap, int trace){
		
		// Index Individuals in Population
		for(int i = 0; i < population.length; i++){
			population[i].setIndex(i);
		}
		
		// Create Instance of a Random Number Generator
		MersenneTwister random = new MersenneTwister();
			
		// Initialise the Output Distance Matrices
		double[][] geneticDistanceMatrix = new double[0][0];
		double[][] spatialDistanceMatrix = new double[0][0];
		Individual[] sampled = new Individual[0];
				
		// Seed the Infection
		Individual seed = IndividualMethods.randomChoice(population);
		int[] mutationEvents = new int[1];
		mutationEvents[0] = Global.mutationEventNo;
		seed.setStatusIndex(seedStateIndex);
		seed.setMutationEvents(mutationEvents);
		
		int[] timeEnteredStates = seed.getTimeEnteredStates();
		timeEnteredStates[seedStateIndex - 1] = 1;
		seed.setTimeEnteredStates(timeEnteredStates);
				
		// Record the number of individuals in each population state
		int[] summary = new int[transitions.length];
		summary[0] = population.length - 1;
		summary[seed.getStatusIndex()]++;
				
		// Run a Simulation
		for(int timeStep = 1; timeStep <= length; timeStep++){
			
			/**
			 * Initiate Hashtable to record all those infected during the current timestep
			 * Individuals infected in the current timestep are not considered infectious until the next timestep. 
			 */
			Hashtable<Individual, Integer> justInfected = new Hashtable<Individual, Integer>();
			
			// Find Infectious Individuals
			for(int inf = 0; inf < population.length; inf++){
				
				// Select only infectious Individuals (and those that have not just been infected)
				if(infectiousness[population[inf].getStatusIndex()] == 0 && justInfected.get(population[inf]) == null){
					continue;
				}
				
				/**
				 *  Did the Source's pathogen mutate in the current Time Step
				 *  In this way the sequences of infectious individuals is updated every timeStep
				 */
				mutationEvents = IndividualMethods.mutateSequence(mutationRates[population[inf].getStatusIndex()], timeStep, population[inf].getMutationEvents(), population[inf].getMutationsLastChecked());
					
				// Update Sources Mutation Events
				population[inf].setMutationEvents(mutationEvents);
				population[inf].setMutationsLastChecked(timeStep);
				
				// Did this individual infect anyone else during the current time step? Look at Susceptibles
				for(int con = 0; con < population.length; con++){
					
					// Probability influenced by the Distance Susceptible is away from Current infectious individual
					if(distanceMatrix[inf][con] <= distanceCap && random.nextDouble() < (infectiousness[population[inf].getStatusIndex()] / distanceMatrix[inf][con]) && population[con].getStatusIndex() == 0){
						
						// Record in Population Summary
						summary[population[con].getStatusIndex()]--;
							
						// Infect the Current Individual
						population[con].setStatusIndex(population[con].getStatusIndex() + 1);
						population[con].setMutationEvents(mutationEvents);
						population[con].setMutationsLastChecked(timeStep);
						summary[population[con].getStatusIndex()]++;
						justInfected.put(population[con], 1);
							
						// Append this contact to the list of Individuals the current Infected individual has infected
						Individual[] infected = population[inf].getIndividualsInfected();
						infected = IndividualMethods.append(infected, population[con]);
						population[inf].setIndividualsInfected(infected);
						
						// Record When Contact Individual Moved into new State
						timeEnteredStates = population[con].getTimeEnteredStates();
						timeEnteredStates[population[con].getStatusIndex() - 1] = timeStep;
						population[con].setTimeEnteredStates(timeEnteredStates);
					}					
				}
				
				
				/**
				 * Did the Infected Individual's infection Progress
				 * 
				 * states = { S I R }
				 * transitions = { 0 0.1 0 }
				 */
				if(random.nextDouble() < transitions[population[inf].getStatusIndex()]){
					
					// Record in Population Summary
					summary[population[inf].getStatusIndex()]--;
							
					// Update Individual's infection Status
					population[inf].setStatusIndex(population[inf].getStatusIndex() + 1);
					summary[population[inf].getStatusIndex()]++;
					
					// Record When Individual Moved into new State
					timeEnteredStates = population[inf].getTimeEnteredStates();
					timeEnteredStates[population[inf].getStatusIndex() - 1] = timeStep;
					population[inf].setTimeEnteredStates(timeEnteredStates);						
				}
			}
					
			// Examine Population Summary
			if(trace == 1){
				System.out.println(Arrays.toString(summary));
			}
					
					
			// Sample Now?
			if(timeStep == sampleTime || ArrayMethods.multiply(summary, infectiousness) == 0 || summary[0] == 0){
								
				sampled = new Individual[population.length];
				int posUsed = -1;
						
				// Attempt to Sample each individual in the population
				for(int i = 0; i < population.length; i++){
					if(random.nextDouble() < samplingEfficacy && population[i].getStatusIndex() != 0){
						posUsed++;
						sampled[posUsed] = population[i];
					}
				}
				
				// Subset to remove empty positions in sampled list
				sampled = IndividualMethods.subset(sampled, 0, posUsed);
				
				// Check if any mutations have occurred in any of the individuals since last checked
				updateIndividualsSequences(sampled, timeStep, mutationRates);
				
				// Build the Genetic Distance Matrix
				geneticDistanceMatrix = IndividualMethods.buildGeneticDistanceMatrix(sampled);
				
				// Build the Spatial Distance Matrix
				spatialDistanceMatrix = IndividualMethods.buildSpatialDistanceMatrix(sampled, distanceMatrix);
				
				// Print TimeStep to record when simulation ended
				System.out.println("Simulation ended " + (length - timeStep) + " step(s) early.\n" + Arrays.toString(summary));
						
				// Finish Simulation
				break;
			}
		}
				
		// Simulation Finished
		return new Results(population, sampled, geneticDistanceMatrix, spatialDistanceMatrix);
	}

	public static Results runSimulationModelGroupSpatial(double[] transitions, double[] infectiousness, int length,
			int seedStateIndex, double[] mutationRates, int sampleTime, double samplingEfficacy, Individual[][] groups,
			double[][] groupDistanceMatrix, int distanceCap, int trace, int popSize){
		
		// Create Instance of a Random Number Generator
		MersenneTwister random = new MersenneTwister();
			
		// Initialise the Output Distance Matrices
		double[][] geneticDistanceMatrix = new double[0][0];
		double[][] spatialDistanceMatrix = new double[0][0];
		double[][] temporalDistanceMatrix = new double[0][0];
		int[][] networkDistanceMatrix = new int[0][0];
		Individual[] sampled = new Individual[0];
		
		// Initialise Group Adjacency Matrix to Record Transmission Events
		int[][] groupAdjacencyMatrix = new int[groups.length][groups.length];
		
		// Initialise List to store all Individuals in population (flattened version of groups)
		Individual[] population = new Individual[popSize];
		int indexPop = -1;
		
		// Initialise table to store the Mutation Per Generation information
		int[][] mutationsPerGenerationInfo = new int[popSize][4]; // No.Mutations	GenerationTime	Timestep	Within?
		int indexMut = -1;
				
		// Seed the Infection
		int randomGroupIndex = ArrayMethods.randomChoice(ArrayMethods.range(0, groups.length - 1, 1));
		Individual seed = IndividualMethods.randomChoice(groups[randomGroupIndex]);
		int[] mutationEvents = new int[1];
		mutationEvents[0] = Global.mutationEventNo;
		seed.setStatusIndex(seedStateIndex);
		seed.setMutationEvents(mutationEvents);

		int[] timeEnteredStates = seed.getTimeEnteredStates();
		timeEnteredStates[seedStateIndex] = 1;
		seed.setTimeEnteredStates(timeEnteredStates);
				
		// Record the number of individuals in each population state
		int[] summary = new int[transitions.length];
		summary[0] = popSize - 1;
		summary[seed.getStatusIndex()]++;
				
		// Run a Simulation
		for(int timestep = 1; timestep <= length; timestep++){
			
			/**
			 * Initiate Hashtable to record all those infected during the current timestep
			 * Individuals infected in the current timestep are not considered infectious until the next timestep. 
			 */
			Hashtable<Individual, Integer> justInfected = new Hashtable<Individual, Integer>();
			Hashtable<Individual, Integer> infectedAnIndividual = new Hashtable<Individual, Integer>();
			
			for(int groupIndex = 0; groupIndex < groups.length; groupIndex++){
				
				int[] groupSummary = new int[transitions.length];
				double[] individualWeights = new double[groups[groupIndex].length];
				
				/**
				 *  WITHIN GROUP SPREAD
				 *  Find Infectious Individuals in Current Group
				 */
				for(int inf = 0; inf < groups[groupIndex].length; inf++){
					
					// Store Individual
					if(timestep == 1){
						indexPop++;
						population[indexPop] = groups[groupIndex][inf];
					}
										
					// Record the Infection Status of the Group
					if(justInfected.get(groups[groupIndex][inf]) == null){
						individualWeights[inf] = infectiousness[groups[groupIndex][inf].getStatusIndex()];
						groupSummary[groups[groupIndex][inf].getStatusIndex()]++;
					}else{
						// If just become infected - not infectious until next timeStep
						individualWeights[inf] = 0;
						groupSummary[groups[groupIndex][inf].getStatusIndex() - 1]++;
					}
					
					
					// Select only infectious Individuals
					if(infectiousness[groups[groupIndex][inf].getStatusIndex()] == 0){
						individualWeights[inf] = 0;
						continue;
					}
					
					/**
					 *  Did the Source's pathogen mutate in the current Time Step
					 *  In this way the sequences of infectious individuals is updated every timeStep
					 */
					mutationEvents = IndividualMethods.mutateSequenceNew(mutationRates, timestep, groups[groupIndex][inf]);
						
					// Update Sources Mutation Events
					groups[groupIndex][inf].setMutationEvents(mutationEvents);
					groups[groupIndex][inf].setMutationsLastChecked(timestep);
					
					// Did this individual infect anyone else (IN OWN GROUP) during the current time step? Look at Susceptibles
					for(int con = 0; con < groups[groupIndex].length; con++){
						
						/**
						 *  Individuals in the same Group are all connected to one another - infection depends 
						 *  on infectiousness alone.
						 */
						if(random.nextDouble() < infectiousness[groups[groupIndex][inf].getStatusIndex()] && justInfected.get(groups[groupIndex][inf]) == null && groups[groupIndex][con].getStatusIndex() == 0){
							
							// How many Mutations occurred between the current and previous transmission event?
							indexMut++;
							mutationsPerGenerationInfo[indexMut][0] = groups[groupIndex][inf].getNoMutationsSinceTransmission();
							mutationsPerGenerationInfo[indexMut][1] = timestep - groups[groupIndex][inf].getTimeOfLastTransmission();
							mutationsPerGenerationInfo[indexMut][2] = timestep;
							mutationsPerGenerationInfo[indexMut][3] = 1; // Was it within group?
							
							// Record in Population Summary
							summary[groups[groupIndex][con].getStatusIndex()]--;
								
							// Infect the Current Individual
							groups[groupIndex][con].setStatusIndex(groups[groupIndex][con].getStatusIndex() + 1);
							groups[groupIndex][con].setMutationEvents(mutationEvents);
							groups[groupIndex][con].setMutationsLastChecked(timestep);
							groups[groupIndex][con].setTimeOfLastTransmission(timestep);
							groups[groupIndex][con].setSource(groups[groupIndex][inf]);
							
							summary[groups[groupIndex][con].getStatusIndex()]++;
							
							justInfected.put(groups[groupIndex][con], 1);
															
							// Append this contact to the list of Individuals the current Infected individual has infected
							Individual[] infected = groups[groupIndex][inf].getIndividualsInfected();
							infected = IndividualMethods.append(infected, groups[groupIndex][con]);
							groups[groupIndex][inf].setIndividualsInfected(infected);
							
							// Record When Contact Individual Moved into new State
							timeEnteredStates = groups[groupIndex][con].getTimeEnteredStates();
							timeEnteredStates[groups[groupIndex][con].getStatusIndex()] = timestep;
							groups[groupIndex][con].setTimeEnteredStates(timeEnteredStates);
							
							// Record the WithinGroup Transmgission Event
							groupAdjacencyMatrix[groupIndex][groupIndex]++;
							
							// Record that the current Infectious individual infected an individual
							if(infectedAnIndividual.get(groups[groupIndex][inf]) == null){
								infectedAnIndividual.put(groups[groupIndex][inf], 1);
							}
						}					
					}
					
					
					/**
					 * Did the Infected Individual's infection Progress
					 * 
					 * states = { S I R }
					 * transitions = { 0 0.1 0 }
					 */
					if(random.nextDouble() < transitions[groups[groupIndex][inf].getStatusIndex()] && justInfected.get(groups[groupIndex][inf]) == null){
						
						// Record in Group Summary
						groupSummary[groups[groupIndex][inf].getStatusIndex()]--;
												
						// Record in Population Summary
						summary[groups[groupIndex][inf].getStatusIndex()]--;
								
						// Update Individual's infection Status
						groups[groupIndex][inf].setStatusIndex(groups[groupIndex][inf].getStatusIndex() + 1);
						summary[groups[groupIndex][inf].getStatusIndex()]++;
						
						individualWeights[inf] = infectiousness[groups[groupIndex][inf].getStatusIndex()];
						groupSummary[groups[groupIndex][inf].getStatusIndex()]--;
						
						// Record When Individual Moved into new State
						timeEnteredStates = groups[groupIndex][inf].getTimeEnteredStates();
						timeEnteredStates[groups[groupIndex][inf].getStatusIndex()] = timestep;
						groups[groupIndex][inf].setTimeEnteredStates(timeEnteredStates);
							
					}
				}
				
				/**
				 * BETWEEN GROUP SPREAD
				 * Calculate the Force of Infection for the Current Group
				 * This Force of Infection will decay with distance ---> Force of Infection / Distance
				 */
				
				
				// Force of Infection
				double probEvade = 1;
				for(int i = 0; i < groupSummary.length; i++){
					probEvade *= Math.pow(1 - infectiousness[i], groupSummary[i]);
				}
				if(probEvade == 1){
					continue;
				}
				
				// Assess each of the other Groups
				double groupSpecificForce = 0;
				for(int otherGroupIndex = 0; otherGroupIndex < groups.length; otherGroupIndex++){
					if(groupIndex == otherGroupIndex){
						continue;
					}
					
					// Check if susceptibles are present
					Individual[] susceptibles = IndividualMethods.selectStatus(groups[otherGroupIndex], 0);
					if(susceptibles.length == 0){
						continue;
					}
					
					// Calculate Force of Infection for Current Other Group
					groupSpecificForce = (1 - probEvade) / groupDistanceMatrix[groupIndex][otherGroupIndex];
					
					// Did the infection spread between these two groups?
					if(random.nextDouble() < groupSpecificForce){
						
						// Record the Inter-group Transmission Event
						groupAdjacencyMatrix[groupIndex][otherGroupIndex]++;
						
						// Pick Source - Weighted Choice
						Individual source = IndividualMethods.randomWeightedChoice(groups[groupIndex], individualWeights);
												
						// Extract Sequence - Note doesn't need updating in this timestep
						int[] mutations = source.getMutationEvents();
						
						// Pick Sink - Random Choice of Susceptibles
						Individual sink = IndividualMethods.randomChoice(susceptibles);
						summary[sink.getStatusIndex()]--;
						
						justInfected.put(sink, 1);
						
						// Infect sink
						sink.setStatusIndex(sink.getStatusIndex() + 1);
						sink.setMutationEvents(mutations);
						sink.setMutationsLastChecked(timestep);
						sink.setTimeOfLastTransmission(timestep);
						sink.setSource(source);
						
						summary[sink.getStatusIndex()]++;
						
						// Append Sink to Source's list of individuals it has infected
						Individual[] infected = source.getIndividualsInfected();
						infected = IndividualMethods.append(infected, sink);
						source.setIndividualsInfected(infected);
						
						// How many Mutations occurred between the current and previous transmission event?
						indexMut++;
						mutationsPerGenerationInfo[indexMut][0] = source.getNoMutationsSinceTransmission();
						mutationsPerGenerationInfo[indexMut][1] = timestep - source.getTimeOfLastTransmission();
						mutationsPerGenerationInfo[indexMut][2] = timestep;
						mutationsPerGenerationInfo[indexMut][3] = 0; // Was it within group?
						
						// Record When Sink Individual Moved into new State
						timeEnteredStates = sink.getTimeEnteredStates();
						timeEnteredStates[sink.getStatusIndex()] = timestep;
						sink.setTimeEnteredStates(timeEnteredStates);
						
						// Record that the source infected an individual
						if(infectedAnIndividual.get(source) == null){
							infectedAnIndividual.put(source, 1);
						}
					}
					
					// Reset the Mutation Counter for each of the infectious individuals that infected an individual
					Enumeration<Individual> keys = infectedAnIndividual.keys();
					while(keys.hasMoreElements()){
						
						Individual individual = keys.nextElement();
						individual.setNoMutationsSinceTransmission(0);
						individual.setTimeOfLastTransmission(timestep);
						
					}
				}
			}
			
					
			// Examine Population Summary
			if(trace == 1){
				System.out.println(Arrays.toString(summary));
			}
					
					
			// Sample Now?
			if(timestep == sampleTime || ArrayMethods.multiply(summary, infectiousness) == 0 || summary[0] == 0){
								
				sampled = new Individual[popSize];
				int posUsed = -1;
						
				// Attempt to Sample each individual in the population
				for(int i = 0; i < groups.length; i++){
					for(int j = 0; j < groups[i].length; j++){
						
						if(random.nextDouble() < samplingEfficacy && groups[i][j].getStatusIndex() != 0){
							posUsed++;
							sampled[posUsed] = groups[i][j];
						}
					}
				}
				
				// Subset to remove empty positions in sampled list
				sampled = IndividualMethods.subset(sampled, 0, posUsed);
				
				// Check if any mutations have occurred in any of the individuals since last checked
				updateIndividualsSequences(sampled, timestep, mutationRates);
				
				// Build the Genetic Distance Matrix
				geneticDistanceMatrix = IndividualMethods.buildGeneticDistanceMatrix(sampled);
				
				// Build the Spatial Distance Matrix
				spatialDistanceMatrix = createIndividualDistanceMatrix(sampled, groupDistanceMatrix);
				
				// Build the Temporal Distance Matrix
				temporalDistanceMatrix = createTemporalDistanceMatrix(sampled);
				
				// Build the Network Distance Matrix
				networkDistanceMatrix = createNetworkDistanceMatrix(sampled, groupAdjacencyMatrix);
				
				// Print TimeStep to record when simulation ended
				//System.out.println("Simulation ended " + (length - timeStep) + " step(s) early.\n" + Arrays.toString(summary));
						
				// Finish Simulation
				break;
			}
		}
				
		// Simulation Finished
		Results results = new Results(population, sampled, geneticDistanceMatrix, spatialDistanceMatrix);
		results.setTemporalDistanceMatrix(temporalDistanceMatrix);
		results.setNetworkDistanceMatrix(networkDistanceMatrix);
		results.setMutationsVsGenerationTime(mutationsPerGenerationInfo);
		
		return results;
	}

	public static Individual[] createInitialPopulation(int popSize, double[] transitions){
		// Initialise Population
		Individual[] population = new Individual[popSize];
		
		// Create each individual
		for(int i = 0; i < popSize; i++){
			Global.individualId++;
			population[i] = new Individual(Global.individualId);
		}
		
		return population;
	}

	public static double[][] createComparisonLists(double[][] geneticDistance, double[][] spatialDistance){
		
		// How many comparisons need to be made?
		double n = geneticDistance[0].length;
		double noComparisons = (n * (n - 1.0) ) / 2.0;
		
		// Initialise table of correct size to store the Genetic and Spatial Distances for each Sample to Sample comparison
		double[][] table = new double[2][(int) noComparisons];
		
		// Only make comparisons once
		int[][] record = new int[(int) n][(int) n];
		
		// Begin to make Sample to Sample Comparisons
		int col = -1;
		for(int i = 0; i < n; i++){
			for(int j = 0; j < n; j++){
				
				// Ignore Diagonal and comparisons that have already been made
				if(i != j && record[i][j] == 0){
					col++;
					
					// Store the Genetic and Spatial Distances between Sample i and Sample j
					table[0][col] = geneticDistance[i][j];
					table[1][col] = spatialDistance[i][j];
					
					// Record the Comparison
					record[i][j] = 1;
					record[j][i] = 1;
				}
				
			}
		}
		
		return table;
		
	}
	
	public static void printSimulationVariablesTable(Results results, String fileName) throws IOException{
		
		// Calculate how many comparisons need to be made
		int n = ( results.getSampledIndividuals().length * (results.getSampledIndividuals().length - 1) ) / 2;
		
		// Retrieve the Necessary Data
		double[][] geneticDistanceMatrix = results.getGeneticDistanceMatrix();
		double[][] spatialDistanceMatrix = results.getSpatialDistanceMatrix();
		double[][] temporalDistanceMatrix = results.getTemporalDistanceMatrix();
		int[][] networkDistanceMatrix = results.getNetworkDistanceMatrix();
		
		// Record Comparisons Made
		int[][] record = new int[results.getSampledIndividuals().length][results.getSampledIndividuals().length];
		
		// Open File for Writing to
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		String line = "Genetic" + "\t" + "Spatial" + "\t" + "Temporal" + "\t" + "Network";
		WriteToFile.writeLn(bWriter, line);
		
		// Make Comparisons and Print to File
		for(int i = 0; i < results.getSampledIndividuals().length; i++){
			for(int j = 0; j < results.getSampledIndividuals().length; j++){
				
				
				if( i == j || record[i][j] == 1){
					continue;
				}
				
				// Genetic	Spatial	Temporal	Network
				line = String.valueOf(geneticDistanceMatrix[i][j]) + "\t" + String.valueOf(spatialDistanceMatrix[i][j]) + "\t" + String.valueOf(temporalDistanceMatrix[i][j]) + "\t" + String.valueOf(networkDistanceMatrix[i][j]);
				WriteToFile.writeLn(bWriter, line);
				
				// Record the Comparison
				record[i][j] = 1;
				record[j][i] = 1;
			}
		}
		
		// Close the File
		WriteToFile.close(bWriter);
	}

	public static void printMutationsPerGenerationDistribution(int[][] table, String fileName) throws IOException{
		   
		/**
		 *  Table structure: No.Mutations	GenerationTime	Timestep
		 *  
		 *  For recording the output from multiple simulations will store only the number of mutations per generation
		 *  	No. mutations per Generation = No. Mutations / GenerationTime
		 */
		
		// Open File for Writing to
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
				
		for(int i = 0; i < table.length; i++){
			
			if(table[i][2] != 0){
				WriteToFile.write(bWriter, ((double) table[i][0] / (double) table[i][1]) + ",");
			}
		}
		WriteToFile.write(bWriter, "\n");
		WriteToFile.close(bWriter);
		
	}
	
	public static double calculateCorrelation(double[][] table){
		/**
		 * Calculate the Correlation using Pearson's Correlation Metric
		 * 
		 * 		x	y	x*y	x^2	y^2
		 * 		2	3	6	4	9
		 * 		5	9	45	25	81
		 * 		3	1	3	9	1
		 * 		4	5	20	16	25
		 * 	SUM	14	18	74	54	116
		 * 	
		 * 							  n(SUM(x*y)) - SUM(x) * SUM(y)
		 * 		corr 	= -------------------------------------------------------
		 * 			 	  SQRT[ n(SUM(x^2) - SUM(x)^2) * n(SUM(y^2) - SUM(y)^2) ]
		 * 			
		 * 	
		 */
		
		// Initialise each of the Pearson's Correlation terms
		double sumX = 0;
		double sumY = 0;
		double sumXY = 0;
		double sumXSquared = 0;
		double sumYSquared = 0;
		double n = table.length;
		
		// Initialise variables to store each x and y variable
		double x = 0;
		double y = 0;
		
		// Calculate the sums for each of the Pearson's Correlation terms
		for(int i = 0; i < n; i++){
			
			// Extract the variables for the current comparison
			x = table[i][0];
			y = table[i][1];
			
			// Add the current values to the sums
			sumX += x;
			sumY += y;
			sumXY += x * y;
			sumXSquared += Math.pow(x, 2);
			sumYSquared += Math.pow(y, 2);
		}
		
		// Calculate the correlation
		double top = (n * sumXY) - (sumX * sumY);
		double bottom = (n * (sumXSquared - Math.pow(sumX, 2))) * (n * (sumYSquared - Math.pow(sumY, 2)));
		bottom = Math.sqrt(bottom);
		
		return top / bottom;
	}

	public static int checkComparisonTable(double[][] table, int noTransmissionEvents){
		// Method to check the output data from the simulation
		int fine = 0;
		
		// Did any transmission Events occur?
		if(noTransmissionEvents > 1){
			
			// If their enough genetic differences to differentiate samples?
			if(columnTotal(table, 0) > 0){
				fine = 1;
			}else{
				System.out.println("Not enough genetic differences to differentiate between Samples.");
			}
		}else{
			System.out.println("Not enough Transmission Events occurred over the course of the Simulation " + "(" + noTransmissionEvents + ").");
		}
		
		return fine;
	}
	
	public static double columnTotal(double[][] table, int column){
		double total = 0;
		for( double[] row : table){
			total += row[column];
		}
		
		return total;
	}

	public static void printGeneticVsSpatial(double[][] table, String fileName) throws IOException{
		
		/**
		 * Table contains two paired lists:
		 * Genetic vs. Spatial distances
		 */
		
		// Open and Wipe File
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		String line = "Genetic" + "\t" + "Spatial";
		WriteToFile.writeLn(bWriter, line);
				
		// Write eahc Line to file
		for(int col = 0; col < table[0].length; col++){
			line = table[0][col] + "\t" + table[1][col] + "\n";
			WriteToFile.writeLn(bWriter, line);
		}
	}

	public static void updateIndividualsSequences(Individual[] sampled, int timeStep, double[] mutationRates){
		
		// Initialise an array to store each of the Mutation Event sequences
		int[] mutationEvents = new int[0];
		
		for(Individual individual : sampled){
			// Have any mutations occurred in the Current Individual?
			mutationEvents = IndividualMethods.mutateSequence(mutationRates[individual.getStatusIndex()], timeStep, individual.getMutationEvents(), individual.getMutationsLastChecked());
			
			// Update the current Individual's Mutation Event Sequence
			individual.setMutationEvents(mutationEvents);
			individual.setMutationsLastChecked(timeStep);
		}
		
	}

	public static Individual[][] createGroupedPopulation(Individual[] population, int noGroups){
		
		// Initialise Groups
		int groupSize = population.length / noGroups;
		Individual[][] groups = new Individual[noGroups][groupSize];
		
		// Fill the Groups
		int pos = -1;
		for(int i = 0; i < noGroups; i++){
			for(int j = 0; j < groupSize; j++){
				pos++;
				// Store Individual
				groups[i][j] = population[pos];
				
				// Record Group Individual is in
				population[pos].setGroupIndex(i);
			}
		}
		
		return groups;
		
	}

	public static double[][] createIndividualDistanceMatrix(Individual[] population, double[][] groupDistanceMatrix){
		
		// Initialise Individual Distance Matrix
		double[][] individualDistanceMatrix = new double[population.length][population.length];
		
		for(int i = 0; i < population.length; i++){
			
			population[i].setIndex(i);
			
			for(int j = 0; j < population.length; j++){
				
				// Skip comparing the same individuals
				if(i == j || individualDistanceMatrix[i][j] != 0){
					continue;
				}
				
				// Are these individuals in the same group?
				if(population[i].getGroupIndex() == population[j].getGroupIndex()){
					individualDistanceMatrix[i][j] = 1;
					individualDistanceMatrix[j][i] = 1;
				}else{
					// How far are their respective groups apart?
					individualDistanceMatrix[i][j] = groupDistanceMatrix[population[i].getGroupIndex()][population[j].getGroupIndex()];
					individualDistanceMatrix[j][i] = groupDistanceMatrix[population[j].getGroupIndex()][population[i].getGroupIndex()];
				}
				
			}
		}
		
		return individualDistanceMatrix;
	}

	public static double[][] createGroupedSpatialEnvironment(Individual[] population, int noGroups, int x, int y, int buffer){
		
		// Create Group Distance Matrix
		int[][] grid = GridMethods.createRandomPopulationGrid(x, y, ArrayMethods.range(0, noGroups - 1, 1));
		double[][] groupDistanceMatrix = GridMethods.generateEuclideanDistanceMatrix(grid, ArrayMethods.range(0, noGroups - 1, 1));
		
		// Allocate Individuals to Groups and Record as each Individuals groupId
		int groupSize = population.length / noGroups;
		
		int pos = -1;
		for(int i = 0; i < noGroups; i++){
			for(int j = 0; j < groupSize; j++){
				pos++;
				population[pos].setGroupIndex(i);
			}
		}
		
		/**
		 *  Calculate the Distance between each individual in the Population
		 *  Individuals in the same group are part of a complete network. Distances between individuals of different
		 *  groups is defined in the group distance Matrix
		 */
		double[][] individualDistanceMatrix = new double[population.length][population.length];
		
		for(int i = 0; i < population.length; i++){
			
			int iGroup = population[i].getGroupIndex();
			
			for(int j = 0; j < population.length; j++){
				
				int jGroup = population[j].getGroupIndex();
				
				// Avoid repeat comparisons
				if(individualDistanceMatrix[i][j] != 0 || i == j){
					continue;
				}
				
				if(iGroup == jGroup){ // SAME Group
					individualDistanceMatrix[i][j] = 1;
					individualDistanceMatrix[j][i] = 1;
				}else{ // DIFFERENT Group
					individualDistanceMatrix[i][j] = groupDistanceMatrix[iGroup][jGroup] + buffer;
					individualDistanceMatrix[j][i] = groupDistanceMatrix[jGroup][iGroup] + buffer;
				}
			}
		}
		
		return individualDistanceMatrix;
	}

	public static double[][] createGroupDistanceMatrix(int noGroups, int x, int y){
		// Create Group Distance Matrix
		int[][] grid = GridMethods.createRandomPopulationGrid(x, y, ArrayMethods.range(0, noGroups - 1, 1));
		double[][] groupDistanceMatrix = GridMethods.generateEuclideanDistanceMatrix(grid, ArrayMethods.range(0, noGroups - 1, 1));
	
		return groupDistanceMatrix;
	}

	public static double[][] createTemporalDistanceMatrix(Individual[] sampled){
		
		double[][] temporal = new double[sampled.length][sampled.length];
		
		for(int i = 0; i < sampled.length; i++){
			for(int j = 0; j < sampled.length; j++){
				
				// Skip comparisons between the same individual and repeated comparisons
				if(i == j || temporal[i][j] != 0){
					continue;
				}
				
				if(sampled[i].getTimeEnteredStates()[1] > sampled[j].getTimeEnteredStates()[1]){
					temporal[i][j] = sampled[i].getTimeEnteredStates()[1] - sampled[j].getTimeEnteredStates()[1];
					temporal[j][i] = sampled[i].getTimeEnteredStates()[1] - sampled[j].getTimeEnteredStates()[1];
				}else{
					temporal[i][j] = sampled[j].getTimeEnteredStates()[1] - sampled[i].getTimeEnteredStates()[1];
					temporal[j][i] = sampled[j].getTimeEnteredStates()[1] - sampled[i].getTimeEnteredStates()[1];
				}
			}
		}
		
		return temporal;
	}

	public static int[][] createNetworkDistanceMatrix(Individual[] sampled, int[][] groupAdjacencyMatrix){

		int[][] network = new int[sampled.length][sampled.length];
		
		for(int i = 0; i < sampled.length; i++){
			for(int j = 0; j < sampled.length; j++){
				
				// Skip comparisons between the same individual and repeated comparisons
				if(i == j || network[i][j] != 0){
					continue;
				}
				
				int distance = groupAdjacencyMatrix[sampled[i].getGroupIndex()][sampled[j].getGroupIndex()] + groupAdjacencyMatrix[sampled[j].getGroupIndex()][sampled[i].getGroupIndex()];
				
				network[i][j] = distance;
				network[j][i] = distance;
			}
		}
		
		return network;
	}

	public static double calculateBasicReproductionNumber(double[] infectiousness, double[] transitions, double distance,
			int popSize, int groupSize){
		
		/**
		 * Calculate the probability that an infected individual will infect any of the individuals
		 * it comes into contact with. this included both within and between group contacts:
		 * 											 			
		 * 			Beta * (n - 1)	   Beta/distance * (N - n)
		 * 			--------------  +  -----------------------	
		 * 				Gamma					Gamma
		 * 	
		 * 	n = average group size
		 * 	N = population size
		 *  Beta = infectiousness
		 *  Gamma = recovery rate
		 *  d = average group-to-group distance
		 */
		
		double N = (double) popSize;
		double n = (double) groupSize;
		
		// Calculate R0 for Within Group and Between Group Transmission Separately		
		double withinGroupR0 = 0;
		double betweenGroupR0 = 0;
		for(int i = 0; i < infectiousness.length; i++){
			if(infectiousness[i] != 0){
				withinGroupR0 += (infectiousness[i] * (n - 1)) / transitions[i];
				betweenGroupR0 += ((infectiousness[i] / distance) * (N - n)) / transitions[i];
			}
		}
		
		return withinGroupR0 + betweenGroupR0;
	}
}
