package interSpeciesTransmission;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Random;

import org.uncommons.maths.random.PoissonGenerator;

import methods.ArrayMethods;
import methods.HashtableMethods;
import methods.MatrixMethods;
import methods.WriteToFile;

public class Methods {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		
	}
	
	
	// Simulation methods
	public static int findFirstInfectiousStatus(double[] infectiousness){
		
		int index = -1;
		for(int i = 0; i < infectiousness.length; i++){
			
			if(infectiousness[i] != 0){
				index = i;
				break;
			}
		}
		
		return index;
	}
	
	public static Individual[] intialisePopulation(int popSize, int[] types, double[] typeWeights,
			int nStatuses, Random random) throws IOException{
		
		Individual[] population = new Individual[popSize];
		Global.numbersInTypes = new int[types.length];
		
		int type = -1;
		
		for(int i = 0; i < popSize; i++){
			
			type = ArrayMethods.randomWeightedChoice(types, typeWeights, random);
			
			Global.numbersInTypes[type]++;
			
			population[i] = new Individual(i, type, nStatuses);			
		}
		
		return population;
	}

	public static void seedInfection(Individual[] population, int seedStatus, Random random, int timeStep){
		
		// Randomly select an individual in the population
		int seedIndex = random.nextInt(population.length);
		Global.seedIndex = seedIndex;
		
		// Infect that individual
		population[seedIndex].setInfectionStatus(seedStatus, timeStep);
		
		// Record the population state
		Global.populationState[0]--;
		Global.populationState[seedStatus]++;
	}
	
	public static double[] returnWeightsAssignedToEachIndividualInPopulation(double[] infectiousness,
			Individual[] population){
		
		/**
		 *  Noting the infectiousness of every individual in the population
		 *  	This will be used for weighted selection of infection sources
		 *  	Updating population status at the same time
		 */
		
		double[] weights = new double[population.length];
		
		for(int i = 0; i < population.length; i++){
			
			// Add weight for individual
			weights[population[i].getId()] = infectiousness[population[i].getInfectionStatus()];
		}
		
		return weights;
	}

	public static double calculateInfectionForce(double[] infectiousness){
		
		double probabilityOfAvoidance = 1.0;
		
		for(int i = 0; i < infectiousness.length; i++){
			
			if(infectiousness[i] == 0 || Global.populationState[i] == 0){
				continue;
			}
			
			probabilityOfAvoidance = probabilityOfAvoidance * Math.pow((1.0 - infectiousness[i]), (double) Global.populationState[i]);
		}
		
		return 1.0 - probabilityOfAvoidance;
	}
	
	public static int randomlyChooseSource(double[] weights, Random random){
		
		int[] ids = ArrayMethods.seq(0, weights.length - 1, 1);
				
		return ArrayMethods.randomWeightedChoice(ids, weights, random);
	}
	
	public static void infection(Individual[] population, double[] infectiousness, Random random,
			int timeStep) throws IOException{
		
		// Initialise an array for the ids of the individuals in the population
		int[] ids = ArrayMethods.seq(0, population.length - 1, 1);
		
		// Calculate the infectious of each individual in the population - for weighted source selection
		double[] weights = returnWeightsAssignedToEachIndividualInPopulation(infectiousness, population);
		
		// Calculate the force of infection for all susceptible individuals in the previous timestep
		double forceOfInfection = calculateInfectionForce(infectiousness);
		//double forceOfInfection = infectiousness[1];
		
		// Initialise variable to store source information
		int sourceIndex;
		
		for(int i = 0; i < population.length; i++){
			
			// Skip non-susceptible individuals
			if(population[i].getInfectionStatus() != 0){
				continue;
			}
			
			// Did the current susceptible individual get infected in the previous timestep?
			if(random.nextDouble() < forceOfInfection){
				
				// Infect the current susceptible individual
				population[i].setInfectionStatus(population[i].getInfectionStatus() + 1, timeStep);
				
				// Choose a random source for the current individual
				sourceIndex = ArrayMethods.randomWeightedChoice(ids, weights, random);
				
				// Record this infection event
				Global.adjacencyMatrix[sourceIndex][i] = 1;

				// Update population status
				Global.populationState[population[i].getInfectionStatus() - 1]--;
				Global.populationState[population[i].getInfectionStatus()]++;
				
				int timeBetweenInfection = population[i].getInfectionStatusChanges()[1] - population[sourceIndex].getInfectionStatusChanges()[1];
				
				//WriteToFile.writeLn(Global.bWriter, timeStep + "," + timeBetweenInfection);
			}
		}
	}

	public static void printPopulationStatus(int timeStep){
		System.out.print(timeStep + "\t");
		for(int i = 0; i < Global.populationState.length; i++){
			System.out.print(i + ": " + Global.populationState[i] + "\t");
		}
		System.out.println();
	}
	
	public static void calculateR0Distribution(Individual[] population, int[][] adjacencyMatrix) throws IOException{
		
		String outputFile = "C:/Users/Joseph Crisp/Desktop/r0Dist.txt";
		BufferedWriter output = WriteToFile.openFile(outputFile, false);
		
		for(Individual individual : population){
			
			WriteToFile.writeLn(output, individual.getInfectionStatusChanges()[1] + "\t" + ArrayMethods.sum(adjacencyMatrix[individual.getId()]));
			
		}
		
		WriteToFile.close(output);
	}
	
	// Transmission tree methods - BROKEN!!!!!!!!!!!!!!!!!!!!!! See testRateEstimation Methods
	
	public static void printTreeIntoFile(BufferedWriter bWriter, int[][] adjacencyMatrix) throws IOException{
		
		for(int i = 0; i < adjacencyMatrix.length; i++){
			for(int j = 0; j < adjacencyMatrix[0].length; j++){
								
				if(adjacencyMatrix[i][j] == 1){
					WriteToFile.write(bWriter, i + "," + j + "\n");
				}
			}
		}
	}
	
	public static void printTransmissionEvents(int[][] adjacencyMatrix){
		
		for(int i = 0; i < adjacencyMatrix.length; i++){
			for(int j = 0; j < adjacencyMatrix[0].length; j++){
								
				if(adjacencyMatrix[i][j] == 1){
					System.out.println(i + " -> " + j);
				}
			}
		}		
	}
	
	public static int[][] removeUnsampledLeaves(int[][] adjacency,
			Hashtable<Integer, Integer> sampled){
		
		// Copy the adjacency matrix
		int[][] sampledAdjacency = MatrixMethods.copy(adjacency);
		
		// Initialise variable to keep track of whether there are any unsampled leaves remaining
		int noUnsampledLeafFound = 0;
		int found = 0;
		
		// Initialise a variable to store the sum of each row of the adjacency matrix (degree)
		int degree = 0;
		
		// Initialise a Hashtable to record leaves already assessed
		Hashtable<Integer, Integer> assessedLeaves = new Hashtable<Integer, Integer>();
		
		// Examine each row of the adjacency
		while(noUnsampledLeafFound == 0){
			
			found = 0;
			
			for(int i = 0; i < sampledAdjacency.length; i++){
				
				// Skip sampled individuals or individuals we have already examined
				if(sampled.get(i) != null  || assessedLeaves.get(i) != null){
					continue;
				}
				
				// Calculate the degree for the current individual
				degree = ArrayMethods.sum(adjacency[i]);
				
				// Skip non-leaves and sampled leaves, i.e. have a degree >= 1
				if(degree != 0){
					continue;
				}
				
				// Found a non-sampled leaf
				found = 1;
				
				System.out.println("Found an unsampled leaf!");
				
				// Remove the edge to this un-sampled leaf - loop through rows to find and remove connection (1)
				for(int row = 0; row < sampledAdjacency[0].length; row++){
					
					if(sampledAdjacency[row][i] == 1){
						
						sampledAdjacency[row][i] = 0;
						break;
					}
				}
				
				// Record that we have dealt with this un-sampled leaf
				assessedLeaves.put(i, 1);
			}
			
			// Check to see if any un-sampled leaves were found
			if(found == 0){
				noUnsampledLeafFound = 1;
			}
		}
		
		return sampledAdjacency;		
	}

	public static int[][] removeUnsampledIndividualsWithDegreeOne(int[][] adjacency,
			Hashtable<Integer, Integer> sampled){
		
		// Copy the adjacency matrix
		int[][] sampledAdjacency = MatrixMethods.copy(adjacency);
				
		// Initialise a variable to store the sum of each row of the adjacency matrix (degree)
		int degree = 0;
		
		// Create an array of zeros - needed to remove connection from un-sampled individual to it infectee
		int[] blank = new int[sampledAdjacency[0].length];
		
		// Initialise variables to store source and infectees
		int sourceIndex;
		int infecteeIndex;
			
		for(int row = 0; row < sampledAdjacency.length; row++){
			
			// Skip sampled individuals
			if(sampled.get(row) != null){
				continue;
			}
			
			// Calculate the degree for the current individual
			degree = ArrayMethods.sum(sampledAdjacency[row]);
			
			// Skip individuals with degree != 1
			if(degree != 1){
				continue;
			}
			
			/*
			 *  Found an unsampled individual with degree 1 i.e. this individual is on a path to a sampled
			 *  individual - but since not sampled we aren't interested in it
			 *  We want to remove the connection to it and connect its source to its infectee
			 */
			
			System.out.println("Found an un-sampled individual on the path to a sampled individual!");
			
			// Find the index of this current un-sampled individual's source and who it infected
			sourceIndex = -1;
			infecteeIndex = -1;
			for(int x = 0; x < sampledAdjacency.length; x++){
				
				// Search column for Source
				if(sampledAdjacency[x][row] == 1){
					sourceIndex = x;
				}
				
				// Search row for Infectee
				if(sampledAdjacency[row][x] == 1){
					infecteeIndex = x;
				}
				
				// Finish if found both
				if(infecteeIndex != -1 && sourceIndex != -1){
					break;
				}
			}
							
			sampledAdjacency[row] = blank; // Removes connection from unsampled individual
							
			// Check unsampled individual isn't source
			if(sourceIndex != -1){
				sampledAdjacency[sourceIndex][row] = 0; // Removes connection to unsampled individual
				sampledAdjacency[sourceIndex][infecteeIndex] = 1; // Add connection between source and infectee
			}		
		}
					
		return sampledAdjacency;		
	}

	public static double[][] calculateTypeTransitionMatrix(int[][] adjacencyMatrix, Individual[] population,
			int nTypes, int firstInfectiousState){
		
		// Initialise a matrix to store the type transition rates
		double[][] typeTransitionRates = new double[nTypes][nTypes];
		int[][] typeTransitionCounts = new int[nTypes][nTypes];
		double rate = 0;
		
		// Examine each edge in the adjacency matrix
		for(int row = 0; row < adjacencyMatrix.length; row++){
			
			for(int col = 0; col < adjacencyMatrix[0].length; col++){
				
				// Skip if no connection
				if(adjacencyMatrix[row][col] == 0){
					continue;
				}
				
				// Found connection
				rate = 1.0 / (double) (population[col].getInfectionStatusChanges()[1] - population[row].getInfectionStatusChanges()[firstInfectiousState]);
				typeTransitionRates[population[row].getType()][population[col].getType()] += rate;
				typeTransitionCounts[population[row].getType()][population[col].getType()]++;
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

	// Sampling Strategy methods
	
	public static Hashtable<Integer, Integer> sampleInfectedIndividuals(Individual[] population, double proportion,
			double[] typeProportions, String[] temporalBias, int timeStep, int firstInfectiousState, 
			Random random){
		
		// Initialise lists of weights for individuals by their type
		int[][] idsOfIndividuals = new int[typeProportions.length][0];
		double[][] weightsForIndividuals = new double[typeProportions.length][0];
		for(int i = 0; i < typeProportions.length; i++){
			idsOfIndividuals[i] = new int[Global.numbersInTypes[i]];
			weightsForIndividuals[i] = new double[Global.numbersInTypes[i]];			
		}
		int[] indices = new int[typeProportions.length];
		
		// Assign a weight (for sample selection) to each individual in the population
		for(int i = 0; i < population.length; i++){
		
			// Skip non-infected individuals
			if(population[i].getInfectionStatus() == 0){
				continue;
			}
			
			// Calculate the weight for the current individual - temporal weighting
			double weight = 0;
			if(temporalBias[population[i].getType()] == "early"){
				
				weight = 1 + timeStep - population[i].getInfectionStatusChanges()[firstInfectiousState];
				
			}else if(temporalBias[population[i].getType()] == "late"){
				
				weight = 1 + population[i].getInfectionStatusChanges()[firstInfectiousState];
				
			}else if(temporalBias[population[i].getType()] == "none"){
				weight = 1;
			}else{
				System.out.println("Error: temporal bias setting not recognised. " + temporalBias[population[i].getType()]);
			}
			
			if(weight == 0){
				System.out.println("A weight of zero was calculated! " + temporalBias[population[i].getType()] + "\t" + population[i].getInfectionStatusChanges()[firstInfectiousState]);
			}
			
			// Store the individuals id and its assigned weight
			idsOfIndividuals[population[i].getType()][indices[population[i].getType()]] = i;
			weightsForIndividuals[population[i].getType()][indices[population[i].getType()]] = weight;
			indices[population[i].getType()]++;			
		}
		
		//  Calculate the number of samples to be taken
		int[] nSamplesFromTypes = new int[typeProportions.length];
		for(int i = 0; i < typeProportions.length; i++){
			nSamplesFromTypes[i] = (int) (proportion * typeProportions[i] * (double) population.length);
		}
		
		// Initialise a Hashtable to store the sampled individuals
		Hashtable<Integer, Integer> sampled = new Hashtable<Integer, Integer>();
		Hashtable<String, Integer> test = new Hashtable<String, Integer>();
		int index;
		int count = 0;
		
		// Randomly select individuals from the population according to their type and weight
		for(int i = 0; i < typeProportions.length; i++){
			
			double sum = ArrayMethods.sum(weightsForIndividuals[i]);
			
			// Select from within the current type
			for(int x = 0; x < nSamplesFromTypes[i]; x++){
				
				if(sum == 0){
					break;
				}
				
				// Randomly select individual
				index = ArrayMethods.randomWeightedIndex(weightsForIndividuals[i], random);
				
				// Store individuals id
				sampled.put(idsOfIndividuals[i][index],  1);
				test.put(idsOfIndividuals[i][index] + "", 1);
				
				// Set the weight of the sampled individual to 0 - no replacement
				sum = sum - weightsForIndividuals[i][index];
				weightsForIndividuals[i][index] = 0;
				
				count++;
			}
		}
		
		return sampled;
	}
}
