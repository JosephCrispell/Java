package testBEASTRateEstimation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Random;

import org.uncommons.maths.random.MersenneTwisterRNG;
import org.uncommons.maths.random.PoissonGenerator;
import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

import methods.ArrayMethods;
import methods.GeneralMethods;
import methods.MatrixMethods;
import methods.WriteToFile;

public class StateEstimationMethods {

	public static void main(String[] args){
		
		for(int i = 0; i < 1000; i++){
			
			if((i + 1) % 10 == 0){
				System.out.println("Ah!");
			}
		}
		
	}
	
	
	//********** POPULATION DYNAMICS METHODS **********
	public static void calculateStateForceOfInfection(Population population, double[] infectiousness, 
			double[][] stateTransitionProbs){
		
		// Note the number of states
		int nStates = stateTransitionProbs.length;
		
		// Initialise an array to store the probability of avoidance of susceptible individuals in a given state
		double[] probAvoidance = ArrayMethods.repeat(1.0, nStates);
		double infectionProb;
		
		/**
		 *  Initialise a matrix to store source weights
		 *  Sources are weighted by their infectiousness and the transition probability from
		 */
		double[][] sourceWeightsForStates = new double[nStates][population.getSize()];
		
		// Examine each infectious individual in the population
		for(int id = 0; id < population.getSize(); id++){
			
			// Skip non-infectious individuals: Susceptible, Recovered (in SIR), and sampled
			if(infectiousness[population.getIndividualsInfectionStatus(id)] == 0 || population.checkIfIndividualSampled(id) == true){
				continue;
			}
			
			/**
			 * Calculate the probability of avoidance of being infected by the current infectious individual
			 * for susceptible individuals in different states
			 */
			for(int stateIndex = 0; stateIndex < nStates; stateIndex++){
				
				infectionProb = infectiousness[population.getIndividualsInfectionStatus(id)] *
						stateTransitionProbs[population.getIndividualsState(id)][stateIndex];
				
				probAvoidance[stateIndex] *= (1 - infectionProb);
				sourceWeightsForStates[stateIndex][id] = infectionProb;
			}
		}
		
		// Convert the probability of avoidance to an infection probability
		double[] infectionProbsForStates = new double[nStates];
		for(int stateIndex = 0; stateIndex < nStates; stateIndex++){
			
			infectionProbsForStates[stateIndex] = 1 - probAvoidance[stateIndex];
		}
		
		// Store the calculated forces of infection
		population.setStateForceOfInfection(infectionProbsForStates);
		
		// Store the source weights, specific to the sink's state
		population.setStateSourceWeights(sourceWeightsForStates);
	}	
	
	public static void infection(Population population, double[] infectiousness, Random random,
			int timeStep, double[][] stateTransitionProbs) throws IOException{
		
		// Get the ids of the susceptible individuals in the population
		int[] susceptibles = population.getIndicesOfIndividualsInCompartment(0);
		
		// Calculate the force of infection for all susceptible individuals in the previous timestep
		calculateStateForceOfInfection(population, infectiousness, stateTransitionProbs);
		
		// Get a list of all individuals IDs in the population
		int[] ids = ArrayMethods.seq(0, population.getSize() - 1, 1);
				
		// Initialise variable to store source information
		int sourceIndex;

		// Examine each susceptible individual in turn
		for(int id : susceptibles){
			
			// Did the current susceptible individual get infected in the previous timestep?
			if(random.nextDouble() < population.getStateForceOfInfection(population.getIndividualsState(id))){

				// Choose a random source for the current individual
				sourceIndex = ArrayMethods.randomWeightedChoice(ids,
						population.getStateSourceWeights(population.getIndividualsState(id)), random);

				// Infect the current susceptible individual
				population.setIndividualsInfectionStatus(id, population.getIndividualsInfectionStatus(id) + 1, timeStep);
				
				// Record the transmission event
				population.addInfecteeToSourcesList(sourceIndex, id);
				
				// Note the state transition event
				// REMOVED A + 1 AT THE END OF BELOW
				//Global.stateTransitionTimes[population.getIndividualsState(sourceIndex)][population.getIndividualsState(id)] += 1.0 / (((double) timeStep - (double) population.getIndividualsStatusChanges(sourceIndex)[1]) + 1);
				Global.stateTransitionTimes[population.getIndividualsState(sourceIndex)][population.getIndividualsState(id)] += 1.0 / (((double) timeStep - (double) population.getIndividualsStatusChanges(sourceIndex)[1]));
				Global.stateTransitionCounts[population.getIndividualsState(sourceIndex)][population.getIndividualsState(id)]++;
			}
		}
	}
	
	public static double[] getSourceWeights(Population population, double[] infectiousness, int[] sources){
		
		double[] weights = new double[sources.length];
		
		for(int i = 0; i < sources.length; i++){
			weights[i] = infectiousness[population.getIndividualsInfectionStatus(sources[i])];
		}
		
		return weights;
	}
	
	public static void recovery(Population population, double[] transitionRates, int timeStep,
			Random random){
		
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

				// Change the current individuals infection status
				population.setIndividualsInfectionStatus(id, population.getIndividualsInfectionStatus(id) + 1, timeStep);
				
				// Remove the current individual from the record of individuals in each state
				population.recordThatIndividualOfGivenStateRemoved(id);
			}
		}
	}
	
	public static void birth(Population population, int timeStep, int popLimit,
			int nStates, double[] stateProportions){
		
		/**
		 *  Calculate the number of births necessary to keep the population at a constant size.
		 *  	For example if we were interested in keeping the numbers of susceptible (S) and 
		 *  	infectious (I) individuals at a constant size (N) then:
		 *  		nBorn = N - (S + I)
		 */
		
		// Calculate the number of individuals of each state to be added into the population
		int nIndividualsToAdd;
		for(int i = 0; i < nStates; i++){
			nIndividualsToAdd = (int) (((double) popLimit * stateProportions[i]) - (double) population.getNumberOfIndividualsWithState(i));
			
			// Add that number of individuals with the current state
			for(int x = 0; x < nIndividualsToAdd; x++){
				population.addIndividualToPopulation(timeStep, i);
			}
		}
	}
	
	//********** SUVERY METHODS ***********
 	public static void printPopulationStatus(int timeStep, Population population, BufferedWriter bWriter,
 			int nStates) throws IOException{
		
 		// Write the current time step to file
		WriteToFile.write(bWriter, timeStep);
		
		// Print the number of individuals in each of the compartments
		for(int i = 0; i < population.getNumberInfectionStatuses(); i++){
			
			WriteToFile.write(bWriter, "\t" + population.getNumberOfIndividualsInCompartment(i));			
		}
		
		// Print out the number of individuals sampled from each state - cumulative
		for(int i = 0; i < nStates; i++){
			WriteToFile.write(bWriter, "\t" + Global.nSampledFromEachState[i]);
		}
		
		// Print the number of individuals in each state
		for(int i = 0; i < nStates; i++){
			WriteToFile.write(bWriter, "\t" + population.getNumberOfIndividualsWithState(i));
		}
		
		// Finish the line
		WriteToFile.write(bWriter, "\n");
		
	}
	
	public static void printHeaderForSurveyFile(BufferedWriter bWriter, int nInfectionStatuses, int nStates) throws IOException{
		
		// Write the first column header to file
		WriteToFile.write(bWriter, "TimeStep");
		
		// Write the index of each of the infection statuses to file
		for(int i = 0; i < nInfectionStatuses; i++){
			WriteToFile.write(bWriter, "\tNumberWithStatus:" + i);
		}
		
		// Write the columns to record how many of each state are sampled
		for(int i = 0; i < nStates; i++){
			WriteToFile.write(bWriter, "\tNSampledIn:" + i);
		}
		
		// Write the index of each of the states
		for(int i = 0; i < nStates; i++){
			WriteToFile.write(bWriter, "\tNumberInState:" + i);
		}
		
		// Finish the line
		WriteToFile.write(bWriter, "\n");
	}
	
	//********** STATE METHODS *********
	public static void assignStates(Population population, int nStates, double[] stateProportions, Random random){
		
		// Initialise hashtable to store the individuals in the population in each state
		population.initialiseListsForIndividualsInEachState(nStates);
		
		// Get the Ids of all the individuals in the population
		int[] ids = ArrayMethods.seq(0, population.getSize() - 1, 1);
		
		// Randomly choose the individuals to be in each state
		int nIndividualsInState;
		int[] chosen;
		for(int i = 0; i < nStates; i++){
			
			// Check we haven't reached the last state
			if(i < nStates - 1){
				// Calculate the number of individuals to be in the current state
				nIndividualsInState = (int) (stateProportions[i] * (double) population.getSize());
				
				// Choose that number of individuals
				chosen = ArrayMethods.randomChoices(ids, nIndividualsInState, random, false);
				
				// Assign the current state to all the chosen individuals
				for(int id : chosen){
					
					// Assign the chosen state to the current individual
					population.setIndividualsState(id, i);
					
					// Remove the current individual from the list
					ids = ArrayMethods.deleteElement(ids, id);
				}
			
			// Assign the remaining individuals to the last state
			}else{
				
				// Assign the current state to all the remaining individuals
				for(int id : ids){
					
					// Assign the chosen state to the current individual
					population.setIndividualsState(id, i);
					
					// Remove the current individual from the list
					ids = ArrayMethods.deleteElement(ids, id);
				}
			}			
		}	
	}
	
	//********** NEW SAMPLING METHODS **********
	
	public static int[] noteStateToSamplePerTimeStepTwoStateOnly(int nTimeSteps, Random random,
			String[] temporalBiases){
		
		// Bias can either be early (first half), or late (second half) and this method will
		// only work for a two-state system
		
		// Initialise an array to store a list of timesteps
		int[] timeSteps = new int[0];
		
		// Initialise an array to store the state to be sampled in each tiemstep
		int[] stateToSampleInTimeStep = new int[nTimeSteps];
		
		for(int i = 0; i < temporalBiases.length; i++){
			
			// Early or late sampling?
			if(temporalBiases[i].matches("early")){
				timeSteps = ArrayMethods.seq(0, (nTimeSteps / 2) - 1, 1);
				
			}else if(temporalBiases[i].matches("late")){
				timeSteps = ArrayMethods.seq(nTimeSteps / 2, nTimeSteps - 1, 1);
			}else{
				System.out.println("ERROR!: Incorrect option for temporal bias " + temporalBiases[i]);
			}
			
			for(int index : timeSteps){
				stateToSampleInTimeStep[index] = i;
			}
		}
		
		return stateToSampleInTimeStep;
	}
	
 	public static int[] noteStateToSamplePerTimestep(int nTimeSteps,
			double[] proportionsForStateSampling, Random random){
		
		// Initialise an array to store the state to be sampled in each tiemstep
		int[] stateToSampleInTimeStep = new int[nTimeSteps];
		
		// Calculate the number of timesteps to sample for each state
		int[] nTimeStepsPerState = new int[proportionsForStateSampling.length];
		for(int i = 0; i < proportionsForStateSampling.length; i++){
			nTimeStepsPerState[i] = (int) (proportionsForStateSampling[i] * nTimeSteps);
		}
		
		// Randomly choose the timesteps to sample for each state
		int[] timeSteps = ArrayMethods.seq(0, nTimeSteps - 1, 1);
		int[] chosen;
		for(int i = 0; i < proportionsForStateSampling.length; i++){
			
			if(i < proportionsForStateSampling.length - 1){
				
				chosen = ArrayMethods.randomChoices(timeSteps, nTimeStepsPerState[i], random, false);
				
				for(int index : chosen){
					stateToSampleInTimeStep[index] = i;
					
					// Remove the current chosen timestep
					timeSteps = ArrayMethods.deleteElement(timeSteps, index);
				}
			
			// Assign any remaining timeSteps to the last state
			}else{

				for(int index : timeSteps){
					stateToSampleInTimeStep[index] = i;
				}
			}			
		}
		
		return stateToSampleInTimeStep;
	}
	
	public static void sampleNew(Population population, int timeStep, double prop,
			double[] infectiousness, int state, Random random){
		
		// Calculate number of samples - only sampling infectious individuals
		int nSamples = (int) (prop * Methods.calculateNumberInfectiousInPop(infectiousness, population));
		
		/**
		 *  Calculate the weights (for choosing an individual to sample) for each compartment.
		 *  Weight is calculated as a function of the infectiousness of the compartment and 
		 *  the number of individuals in that compartment.
		 */
		double[] compartmentWeights;
		int compartment;
		int[] potentialSamplees;
		
		// Initialise a variable to store the indices of each chosen individual
		int[] chosen;
		
		// Sample from the population
		// Make a random weighted choice of the infection compartment from which to sample
		compartmentWeights  = Methods.generateWeightsForCompartments(infectiousness, population);
		compartment = ArrayMethods.randomWeightedIndex(compartmentWeights, random);
		potentialSamplees = population.getIndicesOfIndividualsInCompartment(compartment, state);
			
		// Randomly choose the individuals
		chosen = ArrayMethods.randomChoices(potentialSamplees, nSamples, random, false);
			
		// Note which individuals were sampled
		for(int x = 0; x < chosen.length; x++){
			population.recordSamplingEvent(chosen[x], timeStep);
				
			// Remove the current individual from the record of individuals in each state
			population.recordThatIndividualOfGivenStateRemoved(chosen[x]);
				
			// Keep count of how many from each state are sampled
			Global.nSampledFromEachState[population.getIndividualsState(chosen[x])]++;
		}		
	}
	
	//********** SAMPLING METHODS **********
	public static double[] getStateSamplingProportionsForCurrentTimestep(double[][] proportionsPerTimestep, int pos){
		
		// Initialise an array to store the sampling proportions per state
		double[] proportions = new double[proportionsPerTimestep.length];
		
		// Get the sampling proportion for each state at the current timestep
		for(int i = 0; i < proportionsPerTimestep.length; i++){
			
			proportions[i] = proportionsPerTimestep[i][pos];
		}
		
		return proportions;
	}
	
 	public static double[][] calculateSamplingProportionsForEachStateInEachTimestepSampled(int nStates,
			int[] startEnd, String[] biases, double[] proportionsIfNoBias){
		
		/**
		 *  Initialise an array (of arrays) to store the proportion of individuals to be sampled from each
		 *  state in each timestep sampled
		 */
		double[][] proportionsPerTimestep = new double[nStates][startEnd[1] - startEnd[0]];
		
		// Calculate for each state the sampling proportion per timestep
		for(int i = 0; i < nStates; i++){
			
			// Are we using a temporal bias?
			if(biases[i].matches("none")){
				proportionsPerTimestep[i] = ArrayMethods.repeat(proportionsIfNoBias[i], startEnd[1] - startEnd[0]);
			
			// If there is a bias in the current individuals sampling apply it
			}else{
				proportionsPerTimestep[i] = calculateSamplingProportionsWithTemporalBias(startEnd[1] - startEnd[0], biases[i]);
			}
		}
		
		return proportionsPerTimestep;
	}
	
	public static double[] calculateSamplingProportionsWithTemporalBias(int nTimesteps,
			String bias){
		
		// Initialise an array to store the calculated sampling proportions for each state
		double[] proportions = new double[nTimesteps];
		
		// Calculate m
		double m = 1 / (double) nTimesteps;
		
		// Calculate the proportion (of the total number begin sampled) to be of individuals with each state
		for(int i = 0; i < nTimesteps; i++){
			
			// What direction is the bias
			if(bias.matches("early")){
				
				proportions[i] = (-m * (double) i) + (m * (double) nTimesteps);
							
			}else if(bias.matches("late")){
				
				proportions[i] = m * (double) i;
			}else{
				System.out.println("ERROR!: Unknown temporal bias encountered: " + bias);
			}
		}
		
		return proportions;
	}
		
	public static void sample(Population population, int timeStep, double prop,
			double[] infectiousness, double[] stateProportions, Random random){
		
		// Get a list of all the infectiousness individuals in the population
		
		// Calculate number of samples - only sampling infectious individuals
		int nSamples = (int) (prop * Methods.calculateNumberInfectiousInPop(infectiousness, population));

		// Calculate the number of infectious individuals of each state to be sampled
		int[] nSamplesPerState = calculateNSamplesPerState(nSamples, stateProportions);
		
		/**
		 *  Calculate the weights (for choosing an individual to sample) for each compartment.
		 *  Weight is calculated as a function of the infectiousness of the compartment and 
		 *  the number of individuals in that compartment.
		 */
		double[] compartmentWeights;
		int compartment;
		int[] potentialSamplees;
		
		// Initialise a variable to store the indices of each chosen individual
		int[] chosen;
		
		// Sample from the population
		for(int i = 0; i < nSamplesPerState.length; i++){
			
			// Make a random weighted choice of the infection compartment from which to sample
			compartmentWeights  = Methods.generateWeightsForCompartments(infectiousness, population);
			compartment = ArrayMethods.randomWeightedIndex(compartmentWeights, random);
			potentialSamplees = population.getIndicesOfIndividualsInCompartment(compartment, i);
			
			// Randomly choose an individual
			chosen = ArrayMethods.randomChoices(potentialSamplees, nSamplesPerState[i], random, false);
			
			// Note when all the individuals were sampled
			for(int x = 0; x < chosen.length; x++){
				population.recordSamplingEvent(chosen[x], timeStep);
				
				// Remove the current individual from the record of individuals in each state
				population.recordThatIndividualOfGivenStateRemoved(chosen[x]);
				
				// Keep count of how many from each state are sampled
				Global.nSampledFromEachState[population.getIndividualsState(chosen[x])]++;
			}			
		}
	}
	
	public static int[] calculateNSamplesPerState(int nSamples, double[] stateProportions){
		
		int[] nSamplesPerState = new int[stateProportions.length];
		
		for(int i = 0; i < stateProportions.length; i++){
			nSamplesPerState[i] = (int) ((double) nSamples * stateProportions[i]); 
		}
		
		return nSamplesPerState;
	}
	
	//********** PAGEL'S METHOD STATE TRANSITION RATE ESTIMATION *********
	public static double[][] orderRandomRatesByLikelihood(double[][] logLikelihoodScores, int nRates){
		
		/**
		 * This Method Uses the Bubble Sort Algorithm
		 * 		Described here: http://en.wikipedia.org/wiki/Bubble_sort
		 * 
		 * 	For each element, compare it to the next element. If it is smaller than the next element, swap the elements.
		 * 	Do this for each element of the list (except the last). Continue to iterate through the list elements and
		 *  make swaps until no swaps can be made.
		 *  
		 *  Large --> Small
		 */
		
		double[] a;
		double[] b;
		
		int swappedHappened = 1;
		while(swappedHappened == 1){ // Continue to compare the List elements until no swaps are made
		
			int swapped = 0;
			for(int index = 0; index < logLikelihoodScores.length - 1; index++){
				
				// Compare Current Element to Next Element
				if(logLikelihoodScores[index][nRates] < logLikelihoodScores[index + 1][nRates]){
					
					// Swap Current Element is Larger
					a = logLikelihoodScores[index];
					b = logLikelihoodScores[index + 1];
					
					logLikelihoodScores[index] = b;
					logLikelihoodScores[index + 1] = a;
					
					// Record that a Swap occurred
					swapped++;
				}
			}
			
			// Check if any swaps happened during the last iteration - if none then finished
			if(swapped == 0){
				swappedHappened = 0;
			}
		}
		
		return logLikelihoodScores;
		
	}
	
	public static double[][] findLowerUpperLimitsAndMeanRateForTopNRates(double[][] logLikelihoodScores, int nRates, int n){
		
		// RECORD THE MAXIMUM LOG LIKELIHOOD VALUES INSTEAD OF MEAN??
		
		// Order the random rates by their logLikelihood score
		logLikelihoodScores = orderRandomRatesByLikelihood(logLikelihoodScores, nRates);
		
		// Initialise an array to store the upper and lower limits for each rate
		double[][] limits = new double[nRates][3]; // min, max, mean
		
		// Find the min and max for each rate for the top n
		for(int i = 0; i < n; i++){
			
			for(int rateIndex = 0; rateIndex < nRates; rateIndex++){
				
				// Check if found new minimum
				if(logLikelihoodScores[i][rateIndex] < limits[rateIndex][0] || limits[rateIndex][0] == 0){
					limits[rateIndex][0] = logLikelihoodScores[i][rateIndex];
				
				// Check if found new maximum
				}
				
				if(logLikelihoodScores[i][rateIndex] > limits[rateIndex][1] || limits[rateIndex][1] == 0){
					limits[rateIndex][1] = logLikelihoodScores[i][rateIndex];
				}
				
				// Calculations for mean
				limits[rateIndex][2] += logLikelihoodScores[i][rateIndex];
			}			
		}
		
		// Finish the mean calculations
		for(int rateIndex = 0; rateIndex < nRates; rateIndex++){
			limits[rateIndex][2] = limits[rateIndex][2] / (double) n;
		}
		
		return limits;
	}
	
	public static double[] findMaximumLikelihoodRates(double[][] logLikelihoodScores, int nRates){
		
		double max = logLikelihoodScores[0][nRates];
		int index = 0;
		
		for(int i = 1; i < logLikelihoodScores.length; i++){
			
			if(logLikelihoodScores[i][nRates] > max){
				max = logLikelihoodScores[i][nRates];
				index = i;
			}			
		}
		
		return logLikelihoodScores[index];
	}
		
	public static double calculateLogLikelihood(int[][] adjacencyMatrix, Population population,
			double[] infectiousness, double[][][] pMatrices){
		
		// Initialise a variable to store the logLikelihood of the tree
		double logLikelihood = 0;
		double prob;
		
		// Find the first infectious state
		int firstInfectiousState = returnFirstInfectiousStatus(infectiousness);
		
		// Initialise a variable to store the branch length
		double branchLength;
		
		// Initialise variables to record the state of individuals
		int stateI;
		int stateJ;
		
		// Examine each edge in the adjacency matrix
		for(int row = 0; row < adjacencyMatrix.length; row++){
			
			// Get the state of the current individual
			stateI = population.getIndividualsState(row);
			
			for(int col = 0; col < adjacencyMatrix[0].length; col++){
				
				// Skip if no connection
				if(adjacencyMatrix[row][col] == 0){
					continue;
				}
				
				// Get the state of the current individual
				stateJ = population.getIndividualsState(col);
				
				// Calculate the period of time in which this transmission event occurred 
				branchLength = calculateHowLongTransmissionEventTook(row, col, population, firstInfectiousState);
				
				// Get the probability of observing the current branch beginning in state x and ending in state y
				prob = pMatrices[(int) (branchLength - 1)][stateI][stateJ];
				
				// Add the log(P(t)) to the logLikelihood sum
				logLikelihood += Math.log(prob);
			}
		}
		
		return logLikelihood;
	}
	
	public static double[][][] generatePForRangeOfBranchLengths(double[][] Q, int maxBranchLength, int nStates){
		
		/**
		 * Method taken from Pagel et al. 2004:
		 * Bayesian Estimation of Ancestral Character States on Phylogenies
		 * 
		 * P(t) = e^(Qt)
		 * 
		 * t = a time interval, branch length
		 * 
		 * NOTE: Negative sign removed as this seems to be an error
		 */
		
		// Initialise an array to store the p matrices
		double[][][] pMatrices = new double[maxBranchLength][nStates][nStates];
		
		// Initialise a matrix
		DoubleMatrix p;
		
		// Create the P matrices for a range of branch lengths
		for(int index = 0; index < maxBranchLength; index++){
			
			p = MatrixFunctions.expm(new DoubleMatrix(MatrixMethods.timesBy(Q, index + 1)));
			
			// Store the values of the p matrix
			for(int i = 0; i < nStates; i++){
				for(int j = 0; j < nStates; j++){
					
					pMatrices[index][i][j] = p.get(i, j);
				}
			}
		}
		
		return pMatrices;		
	}
	
	public static void getRatesFromQ(double[][] Q, int nRates, double[][] logLikelihoodScores, int index){
		
		// Get off diagonal elements and store them
		int pos = -1;
		
		for(int i = 0; i < Q.length; i++){
			for(int j = 0; j < Q.length; j++){
				
				// Skip the diagonal
				if(i == j){
					continue;
				}
				
				pos++;
				logLikelihoodScores[index][pos] = Q[i][j];				
			}
		}
	}
	
	public static double[][] estimateStateTransitionRatesUsingPagelsMethod(int[][] adjacencyMatrix, Population population,
			int nStates, double[] bounds, int n, Random random, double[] infectiousness, int simulationLength,
			double top){
		
		// Calculate the number of rates to be calculated
		int nRates = (int) (Math.pow(nStates, 2) - nStates);
		
		// Initialise an array to store the random rate pairs and their logLikelihood score
		double[][] logLikelihoodScores = new double[n][nRates + 1];
		
		// Initialise the variables necessary to calculate the likelihood
		double[][] Q;
		
		// Initialise an array to store P matrices
		double[][][] pMatrices;
		
		// Conduct n estimations - note both rates chosen (randomly) simultaneously
		for(int pos = 0; pos < n; pos++){
			
			// Randomly generate transition rates to fill Q
			Q = generateQ(nStates, bounds, random);
			getRatesFromQ(Q, nRates, logLikelihoodScores, pos);
			
			// Calculate P for a range of branch lengths
			pMatrices = generatePForRangeOfBranchLengths(Q, simulationLength, nStates);
			
			// Calculate the logLikelihood
			logLikelihoodScores[pos][nRates] = calculateLogLikelihood(adjacencyMatrix, population, infectiousness, pMatrices);
			
			// Print progress information
			if((pos + 1)  % 100 == 0){
				System.out.print(".");
			}
			if((pos + 1)  % 1000 == 0){
				System.out.print("\n");
			}
		}
		System.out.println();
		
		// Find the maximum likelihood rate pair
		//double[] rates = findMaximumLikelihoodRates(logLikelihoodScores, nRates);
		//return ArrayMethods.subset(rates, 0, rates.length - 2);
		
		// Find the upper and lower limits for the top N rates
		int topN = (int) (top * (double) n);
		return findLowerUpperLimitsAndMeanRateForTopNRates(logLikelihoodScores, nRates, topN);
	}
	
	public static void estimateStateTransitionRatesUsingPagelsMethod(int[][] adjacencyMatrix, Population population,
			int nStates, double[] bounds, int n, Random random, double[] infectiousness, String fileName, String anotherFile) throws IOException{
		
		// Open the output file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		WriteToFile.writeLn(bWriter, buildHeader(nStates));
		
		// Initialise the variables necessary to calculate the likelihood
		double[][] Q;
		double logLikelihood;
		
		// Conduct n estimations - note both rates chosen (randomly) simultaneously
		for(int pos = 0; pos < n; pos++){
			
			// Randomly generate transition rates to fill Q
			Q = generateQ(nStates, bounds, random);
			
			// Calculate the logLikelihood
			logLikelihood = calculateLogLikelihood(adjacencyMatrix, population, infectiousness, Q, anotherFile);
			
			// Store the result
			WriteToFile.writeLn(bWriter, createOutputString(Q, logLikelihood));
			
			// Print progress information
			if((pos + 1)  % 10 == 0){
				System.out.print(".");
			}
		}
		System.out.println();
		
		// Close the output file
		WriteToFile.close(bWriter);
	}
	
	public static String buildHeader(int nStates){
		String header = "";
		
		for(int i = 0; i < nStates; i++){
			for(int j = 0; j < nStates; j++){
				
				if(i != j){
					header += "State_" + i + "-" + j + "\t";
				}
			}
		}
		
		header += "LogLikelihood";
		
		return header;
	}
	
	public static String createOutputString(double[][] Q, double logLikelihood){
		String output = "";
		int pos = -1;
		
		// Store the randomly chosen state transition rates
		for(int i = 0; i < Q.length; i++){
			for(int j = 0; j < Q[0].length; j++){
				
				if(i != j){
					pos++;
					output += Q[i][j] + "\t";
				}
			}
		}
		
		// Store the likelihood
		output += logLikelihood;
		
		return output;
	}
	
	public static double[][] generateQ(int nStates, double[] bounds, Random random){
		
		double[][] Q = new double[nStates][nStates];
		
		for(int i = 0; i < nStates; i++){
			for(int j = 0; j < nStates; j++){
				
				if(i != j){
					Q[i][j] = GeneralMethods.getRandomDoubleInRange(bounds[0], bounds[1], random);
				}
			}
		}
		
		for(int i = 0; i < nStates; i++){
			Q[i][i] = -1 * ArrayMethods.sum(Q[i]);
		}
		
		return Q;
	}
	
	public static double calculateLogLikelihood(int[][] adjacencyMatrix, Population population,
			double[] infectiousness, double[][] Q, String fileName) throws IOException{
		
		// Initialise a variable to store the logLikelihood of the tree
		double logLikelihood = 0;
		double prob;
		
		// Find the first infectious state
		int firstInfectiousState = returnFirstInfectiousStatus(infectiousness);
		
		// Initialise a variable to store the branch length
		double branchLength;
		
		// Initialise variables to record the state of individuals
		int stateI;
		int stateJ;
		
		// Open an output file
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		WriteToFile.writeLn(bWriter, "StateI\tStateJ\tBranchLength");
		
		// Examine each edge in the adjacency matrix
		for(int row = 0; row < adjacencyMatrix.length; row++){
			
			// Get the state of the current individual
			stateI = population.getIndividualsState(row);
			
			for(int col = 0; col < adjacencyMatrix[0].length; col++){
				
				// Skip if no connection
				if(adjacencyMatrix[row][col] == 0){
					continue;
				}
				
				// Get the state of the current individual
				stateJ = population.getIndividualsState(col);
				
				// Calculate the period of time in which this transmission event occurred 
				branchLength = calculateHowLongTransmissionEventTook(row, col, population, firstInfectiousState);
				
				// Calculate the probability of observing the current branch beginning in state x and ending in state y
				prob = calculatePt(Q, branchLength, stateI, stateJ);

				WriteToFile.writeLn(bWriter, stateI + "\t" + stateJ + "\t" + branchLength);
				
				// Add the log(P(t)) to the logLikelihood sum
				logLikelihood += Math.log(prob);
			}
		}
		
		// Close the file
		WriteToFile.close(bWriter);
		
		return logLikelihood;
	}
	
	public static double calculatePt(double[][] q, double branchLength, int i, int j){
		
		/**
		 * Method taken from Pagel et al. 2004:
		 * Bayesian Estimation of Ancestral Character States on Phylogenies
		 * 
		 * P(t) = e^(Qt)
		 * 
		 * t = a time interval, branch length
		 * 
		 * NOTE: Negative sign removed as this seems to be an error
		 */
		
		return MatrixFunctions.expm(new DoubleMatrix(MatrixMethods.timesBy(q, branchLength))).get(i, j);
	}

	public static double[][] convertDoubleMatrixToMatrix(DoubleMatrix matrix){
		double[][] m = new double[matrix.rows][matrix.columns];
		
		for(int i = 0; i < matrix.rows; i++){
			for(int j = 0; j < matrix.columns; j++){
				
				m[i][j] = matrix.get(i, j);
			}
		}
		
		return m;
	}
	
	//********** ESTIMATE STATE TRANSITION RATES METHODS *********
	
	public static int calculateHowLongTransmissionEventTook(int i, int j, Population population, int firstInfectiousState){
		
		// Calculated the time period over which a transmission event occurred
		int timePeriod = (population.getIndividualsStatusChanges(j)[1] - population.getIndividualsStatusChanges(i)[firstInfectiousState]);
		
		/**
		 *  NOTE: It is possible for an individual to infect a susceptible individual in the same timestep it was
		 *  infected in.
		 *  timePeriod = 0;
		 *  
		 *  Here we add 1 to the period length
		 *  
		 *  TURNED THIS OFF!
		 */
		
		//return timePeriod + 1;
		return timePeriod;		
	}
	
	public static double[][] estimateStateTransitionRates(int[][] adjacencyMatrix, Population population,
			int nTypes, double[] infectiousness){
		
		// Find the index of the first infectious state
		int firstInfectiousState = returnFirstInfectiousStatus(infectiousness);

		// Initialise a matrix to store the type transition rates
		double[][] typeTransitionRates = new double[nTypes][nTypes];
		int[][] typeTransitionCounts = new int[nTypes][nTypes];
		double rate = 0;
		
		// Initialise a variable to store the calculated time period over which a transmission event occured
		int timePeriod;
		
		// Examine each edge in the adjacency matrix
		for(int row = 0; row < adjacencyMatrix.length; row++){
			
			for(int col = 0; col < adjacencyMatrix[0].length; col++){
				
				// Skip if no connection
				if(adjacencyMatrix[row][col] == 0){
					continue;
				}
				
				// Calculate the period of time in which this transmission event occurred 
				timePeriod = calculateHowLongTransmissionEventTook(row, col, population, firstInfectiousState);
				
				// Found connection
				rate = 1.0 / (double) timePeriod;
				typeTransitionRates[population.getIndividualsState(row)][population.getIndividualsState(col)] += rate;
				typeTransitionCounts[population.getIndividualsState(row)][population.getIndividualsState(col)]++;
			}
		}
		
		// Finish - calculate mean type transition rates
		for(int i = 0; i < nTypes; i++){
			
			for(int j = 0; j < nTypes; j++){
				
				if(typeTransitionCounts[i][j] == 0){
					continue;
				}
				
				typeTransitionRates[i][j] = typeTransitionRates[i][j] / typeTransitionCounts[i][j];
			}
		}
		
		return typeTransitionRates;		
	}
	
	public static int returnFirstInfectiousStatus(double[] infectiousness){
		
		int status = -1;
		
		for(int i = 0; i < infectiousness.length; i++){
			if(infectiousness[i] > 0){
				status = i;
				break;
			}
		}
		
		return status;
	}
}
