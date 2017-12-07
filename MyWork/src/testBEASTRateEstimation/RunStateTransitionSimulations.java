package testBEASTRateEstimation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Random;

import org.uncommons.maths.random.MersenneTwisterRNG;
import org.uncommons.maths.random.PoissonGenerator;

import methods.ArrayMethods;
import methods.GeneralMethods;
import methods.MatrixMethods;
import methods.WriteToFile;

public class RunStateTransitionSimulations {
	
	public static void main(String[] args) throws IOException {
		
		//***** Set up Simulation *****
		
		// Set the path
		String homeDirectory = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/TestingBEAST/StateTransitions/";
		
		// Set an array size limit
		Global.arraySizeLimit = 99;
		
		// Get the current date and time
		String date = GeneralMethods.getCurrentDate("dd-MM-yyyy");
		
		// Set the simulation parameters
		int simulationLength = 120;
		int popSize = 1000;
		
		// SIR infection parameters
		double[][] stateTransitionProbs = {{0.8, 0.2},	// 0 -> 0, 0 -> 1
										   {0.2, 0.8}}; // 1 -> 0, 1 -> 1
		double[] infectiousness = {0, 0.1, 0};
		double[] transitionRates = {0, 0.02, 0};
		int seedStatus = 1;
	
		// Define the state settings
		int[] states = {0, 1};
		double[] initialStateProportions = {0.5, 0.5};
		String[] temporalBiases = {"early", "late"};
		//double[] samplingProportionsPerState = {0.5, 0.5};		
		
		// Sampling settings
		int[] startEnd = {20, 120};
		double samplingProportion = 0.001;
		//double[][] stateProportionsPerTimestep = StateEstimationMethods.calculateSamplingProportionsForEachStateInEachTimestepSampled(states.length, startEnd, temporalBiases, samplingProportionsPerState);	
		int timeStepPos = -1;		
		int[] stateToSamplePerTimeStep;
		
		// Set the number of replicates
		int nReplicates = 100;
		
		// Provide a list of random numbers to act as seeds
		int[] seeds = generateSeeds(nReplicates);
		int seed;
		
		// Initialise variables for the rate estimation
		//double[] ratesEstimatedOnFullTree;
		//double[] ratesEstimatedOnSampledTree;
		double[][] limitsFull;
		double[][] limitsSamp;
		double percentage = 0.01;
		double[] bounds = {0, 1};
		int n = 10000;
		int nStates = states.length;
		
		// Open an output file
		String resultsFile = homeDirectory + "simulationResults_TEMPORALLYBIASED_" + date + ".txt";
		BufferedWriter bWriter = WriteToFile.openFile(resultsFile, false);
		WriteToFile.writeLn(bWriter, "AB\tBA\tSampAB\tSampBA");
		String output;
		
		for(int runId = 1; runId <= nReplicates; runId++){
			
			System.out.println("\n***************************************************************");
			System.out.println("\t\tBeginning Simulation: " + runId + " of " + nReplicates);
			System.out.println("***************************************************************\n");
			
			// Reset the sampling timestep counter
			timeStepPos = -1;
			
			// Initialise a random number generator with a defined seed
			seed = seeds[runId - 1];
			System.out.println("Seed = " + seed);
			Random random = GeneralMethods.startRandomNumberGenerator(seed);
			
			// Assign the states to be sample in each timestep of the sampling window
			//stateToSamplePerTimeStep = StateEstimationMethods.noteStateToSamplePerTimestep((startEnd[1] - startEnd[0]) + 1, samplingProportionsPerState, random);
			stateToSamplePerTimeStep = StateEstimationMethods.noteStateToSamplePerTimeStepTwoStateOnly((startEnd[1] - startEnd[0]) + 1, random, temporalBiases);
			
			// Initialise the population
			Population population = new Population(popSize, infectiousness.length, 99999);
			
			// Assign states to all the individuals in the population
			StateEstimationMethods.assignStates(population, states.length, initialStateProportions, random);
			
			// Seed the infection
			Methods.seedInfection(population, seedStatus, random, 0);
						
			// Open an output file to store the population status at each time step
			String surveyFile = homeDirectory + "simulation_" + runId + "_" + date + "_survey.txt";
			BufferedWriter survey = WriteToFile.openFile(surveyFile, false);
			StateEstimationMethods.printHeaderForSurveyFile(survey, infectiousness.length, states.length);

			// Reset the Global variables
			Global.stateTransitionTimes = new double[states.length][states.length];
			Global.stateTransitionCounts = new int[states.length][states.length];
			Global.nSampledFromEachState = new int[states.length];
			
			// Print the current population status
			StateEstimationMethods.printPopulationStatus(0, population, survey, states.length);
			
			//***** Begin the simulation *****
			for(int timeStep = 1; timeStep < simulationLength; timeStep++){
				
				// INFECTION
				StateEstimationMethods.infection(population, infectiousness, random, timeStep, stateTransitionProbs);
				
				// SAMPLING
				if(timeStep >= startEnd[0] && timeStep <= startEnd[1]){
					
					// Get the sampling proportions for the current timestep
					timeStepPos++;
					//samplingProportionsPerState = StateEstimationMethods.getStateSamplingProportionsForCurrentTimestep(stateProportionsPerTimestep, timeStepPos);
					
					// Sample the population
					//StateEstimationMethods.sample(population, timeStep, samplingProportion, infectiousness, samplingProportionsPerState, random);
					StateEstimationMethods.sampleNew(population, timeStep, samplingProportion, infectiousness, stateToSamplePerTimeStep[timeStepPos], random);
				}
						
				// RECOVERY
				StateEstimationMethods.recovery(population, transitionRates, timeStep, random);
						
				// BIRTH
				StateEstimationMethods.birth(population, timeStep, popSize, states.length, initialStateProportions);
				
				// SURVEY
				StateEstimationMethods.printPopulationStatus(timeStep, population, survey, states.length);
						
				// STOPPING CONDITION
				if(population.getNumberOfIndividualsInCompartment(1) == 0){
					System.out.println("No infectious individuals left in the population. Time step = " + timeStep + ".");
					break;
				}
				
				// Print some progress information
				if(timeStep  % 10 == 0){
					System.out.print(".");
				}
			}
			System.out.println();
			
			// Close the Survey File
			WriteToFile.close(survey);
			
			System.out.println("Finished Simulation.\n");
			System.out.println("Final Population Size = " + population.getSize());
			System.out.println("Sampled: " + population.getNumberSampled());
			
			// Check that some individuals were sampled
			if(population.getNumberSampled() > 2){
						
				//***** Analyse the simulation results *****

				System.out.println("\nBuilding transmission tree...");
				// Build the full transmission tree as an adjacency matrix
				int[][] adjacencyMatrix = Methods.buildAdjacencyMatrix(population, "none");
				
				System.out.println("Estimating State Transition Rates Using Pagel's Method...");
				//ratesEstimatedOnFullTree = StateEstimationMethods.estimateStateTransitionRatesUsingPagelsMethod(adjacencyMatrix, population, nStates, bounds, n, random, infectiousness, simulationLength);
				limitsFull = StateEstimationMethods.estimateStateTransitionRatesUsingPagelsMethod(adjacencyMatrix, population, nStates, bounds, n, random, infectiousness, simulationLength, percentage);
				
				System.out.println("Building sampled transmission tree...");				
				// Build the sampled transmission tree as an adjacency matrix
				SequenceMethods.removeUnSampledIndividualsWhoInfectedNoOne(population, adjacencyMatrix);
				SequenceMethods.iterativelyRemoveUnsampledLeaves(adjacencyMatrix, population, null);
				SequenceMethods.removeUnsampledIndividualsOnPathToSampledIndividuals(adjacencyMatrix, population);
				SequenceMethods.removeRootIfNotInvolved(adjacencyMatrix, population);
				
				System.out.println("Estimating State Transition Rates Using Pagel's Method...");
				//ratesEstimatedOnSampledTree = StateEstimationMethods.estimateStateTransitionRatesUsingPagelsMethod(adjacencyMatrix, population, nStates, bounds, n, random, infectiousness, simulationLength);
				limitsSamp = StateEstimationMethods.estimateStateTransitionRatesUsingPagelsMethod(adjacencyMatrix, population, nStates, bounds, n, random, infectiousness, simulationLength, percentage);
				
				// Print the estimated rates out to file
				//output = ArrayMethods.toString(ratesEstimatedOnFullTree, "\t");
				//output += "\t" + ArrayMethods.toString(ratesEstimatedOnSampledTree, "\t");
				output = ArrayMethods.toString(limitsFull[0], ";") + "\t";
				output += ArrayMethods.toString(limitsFull[1], ";") + "\t";
				output += ArrayMethods.toString(limitsSamp[0], ";") + "\t";
				output += ArrayMethods.toString(limitsSamp[1], ";");
				WriteToFile.writeLn(bWriter, output);
				
			}else{
				System.out.println("ERROR: Not enough individuals were sampled!");
			}
		}
		
		// Close the results file
		WriteToFile.close(bWriter);
	}
	
	public static int[] generateSeeds(int n){
		int[] array = new int[n];
		
		Random random = new MersenneTwisterRNG();
		
		for(int i = 0; i < n; i++){
			array[i] = random.nextInt();
		}
		
		return array;
	}
}
