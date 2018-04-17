package genericSimulationParts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Random;

import methods.ArrayMethods;
import methods.LatLongMethods;
import methods.WriteToFile;

import org.apache.commons.math3.random.MersenneTwister;
import org.uncommons.maths.random.PoissonGenerator;

import badgerPopulation.GridMethods;

public class SimulationMethods {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int count = 0;
		for(int i = 0; i < 10; i++){
			count++;
			System.out.println(i);
			if(i == 5 && count < 10){
				i = -1;
			}
			
			
		}
	}
	
	/** 
	 * Note that the simulation speed could be improved by designing the methods to be assess each individual
	 * Since during the simulation each individual is assessed should design so that only looping through the individuals in a given group only once
	 * 	Nested:
	 * 		Is this individual female? Is it Spring? Is she Fertile? Did she give birth? Did she get fertilised?
	 * 		Is this individual Infected? Did they infect anyone else? Did their disease progress?
	 * 		Did this individual disperse?
	 * 		Did this individual die?
	 * 
	 * The methods can be slotted into a simulation as needed...
	 * 		
	 */
	
	// Initialise Population
	public static Population createInitialPopulation(int popSize, int noGroups, int meanGroupSize, double meanAge,
			double[][] latLongs, int[] gridDimensions, int sexRatio, int useLatLongs, char unit, int noInfectionStates){
		
		// Initialise Vector to Store the Groups
		Group[] groups = new Group[noGroups];
		int groupSize = popSize / noGroups;
		int age = 0;
		
		// Is the size of the groups varying?
		PoissonGenerator randPoisGrpSize = new PoissonGenerator(meanGroupSize, new Random());
		
		// Is Age included in this model?
		PoissonGenerator randPoisAge = new PoissonGenerator(meanAge, new Random());
		
		// Is sex included in this model?
		MersenneTwister random = new MersenneTwister();
		int sex = 0;
						
		// Create the Individuals in the Group
		for(int groupId = 0; groupId < noGroups; groupId++){
			
			// Is the group size varying around the mean?
			if(meanGroupSize != 0){
				groupSize = randPoisGrpSize.nextValue();
			}
			
			// Create a vector to store the Individuals in the current group
			Individual[] individuals = new Individual[groupSize];
			int individualPos = -1;
			
			for(int i = 0; i < groupSize; i++){
				Global.individualId++;
				
				// Is the age varying around a mean?
				if(meanAge != 0){
					age = randPoisAge.nextValue();
				}
				
				// Is sex needed to be taken into account?
				if(sexRatio != 0 && random.nextDouble() < sexRatio){
					sex = 1;
				}
				
				// Create the individual
				individualPos++;
				Global.individualId++;
				individuals[individualPos] = new Individual(Global.individualId, noInfectionStates);
				individuals[individualPos].setAge(age);
				individuals[individualPos].setGroupIndex(groupId);
				individuals[individualPos].setSex(sex);		
				
			}
			
			// Create the Group
			groups[groupId] = new Group(individuals, groupId);
			if(useLatLongs == 1){
				groups[groupId].setLatLongs(latLongs[groupId]);
			}
		}
		
		// Store the Information about the Population
		Population population = new Population(groups);
		
		/** 
		 * Calculate the Group Distance Matrix, either:
		 * 	Place the groups on a Grid of given dimensions
		 * 	Use Latitude and Longitude points associated with each group
		 */
		double[][] groupDistances = new double[groups.length][groups.length];
		if(useLatLongs == 0){
			groupDistances = generateGroupDistanceMatrix(groups.length, gridDimensions);
		}else{
			groupDistances = generateGroupDistanceMatrix(groups, unit);
		}
		
		population.setGroupDistanceMatrix(groupDistances);
				
		return population;
	}
	
	// Breeding
		
	// Birth
	public static Group birth(Individual female, double avgLitterSize, Group group, int noInfectionStates, double sexRatio, int pseudoVerticalTransFactor, 
			double[] infectiousness){
		
		/**
		 * Females, if fertilised, will give birth to a number of offspring
		 * 	The litter size is drawn from a Poisson distribution around the average litter size
		 */
		
		// Generate a Poisson Distribution around the mean Litter Size 
		PoissonGenerator randomPoisson = new PoissonGenerator(avgLitterSize, new Random());
		
		// Initialise a random number generator
		MersenneTwister random = new MersenneTwister();
		
		for(int i = 0; i < randomPoisson.nextValue(); i++){
			
			// Create each of the offspring
			Global.individualId++;
			Individual individual = new Individual(Global.individualId, noInfectionStates);
			
			individual.setSex(decideSex(random, sexRatio));
			
			// Did pseudo-vertical transmission occur?
			if(pseudoVerticalTransFactor != 0 && infectiousness[individual.getStatusIndex()] > 0){
				
				double infectionProb = 1 - Math.pow(1 - infectiousness[individual.getStatusIndex()], pseudoVerticalTransFactor);
				
				if(random.nextDouble() < infectionProb){
					individual.setStatusIndex(1);
				}
			}
			
			// Add the new individual into the Group
			group.addIndividual(individual);
			
		}
		
		return group;
	}
	
	// Death
	public static Group death(Individual individual, double[][] deathProbs, int[] ageMinBounds, Group group){
		
		/**
		 * Individuals will die at a given probability defined by the death probability matrix:
		 * 		sex\age cat	0	1	2
		 * 				F	0.6	0.2	0.2
		 * 				M	0.7	0.3	0.3
		 * 
		 * 	Allows for differences between age categories and sex
		 */
		
		// Initialise a random number generator
		MersenneTwister random = new MersenneTwister();
		
		// Find the age category for the current individual
		int ageCategoryIndex = findAgeCategoryIndex(ageMinBounds, individual.getAge());
		
		// Find the probability of death for the current individual taking into account its age category and sex
		double deathProb = deathProbs[individual.getSex()][ageCategoryIndex];
		
		// Did the current individual die?
		if(random.nextDouble() < deathProb){
			
			// Remove the current individual from its group
			group.removeIndividual(individual);
		}
		
		return group;		
	}
	
	// Dispersal
	public static Population dispersal(Population population, int[] ageMinBounds, double[][] dispersalProbs){
		/**
		 * Individuals will disperse at a given probability each timestep. This could depend on sex and or age
		 * 			Sex\AgeMinBounds	0	1	2		
		 * 			F					0	0.1	0.2
		 * 			M					0	0.1	0.15		
		 * 
		 * Where the individual disperses to will be randomly decided weighted by the inter-group distances and the group Sex Ratios
		 * 		
		 * 		sexRatio = No. Males / No. Females
		 * 		maleWeight = sexRatio * distance
		 * 		femaleWeight = (1 - sexRatio) * distance
		 */
		
		// Initialise a random number generator
		MersenneTwister random = new MersenneTwister();
		
		// Extract the Group Information
		Group[] groups = population.getGroups();
		double[][] groupDistanceMatrix = population.getGroupDistanceMatrix();
		
		// Find min age for dispersal - first age for which probability isn't 0
		int minAge = findMinAge4Dispersal(dispersalProbs, ageMinBounds);
		
		// Calculate the Sex Ratios for each of the Groups
		double[] sexRatios = getGroupSexRatios(groups, minAge);
		
		// Create an array of group Ids
		int[] groupIds = ArrayMethods.seq(0, groups.length - 1, 1);
		
		// Need to assess every individual in the population
		for(Individual individual : population.getIndividuals()){
			
			// What age category does this individual fall into?
			int ageCategoryIndex = findAgeCategoryIndex(ageMinBounds, individual.getAge()); // Note that if no age default is 0
			
			// Find the dispersal Probability for the current individual
			double dispersalProb = dispersalProbs[individual.getSex()][ageCategoryIndex]; // Default sex is female = 0
			
			// Did the Current individual disperse?
			if(dispersalProb != 0 && random.nextDouble() < dispersalProb){
				
				// Generate the Weights for the Groups
				double[] weights = generateGroupDispersalWeights(groupDistanceMatrix[individual.getGroupIndex()], sexRatios, individual.getSex());
				
				// Where did this individual disperse to?
				int destinationIndex = ArrayMethods.randomWeightedChoice(groupIds, weights);
				
				// Remove the individual from the previous group
				groups[individual.getGroupIndex()].removeIndividual(individual);
				
				// Add the individual to the destination group
				individual.setGroupIndex(destinationIndex);
				groups[individual.getGroupIndex()].addIndividual(individual);
			}
		}
		
		return population;
	}
	
	// Immigration
	public static Population immigration(Population population, double meanImmigrationProb, double meanAge, double sexRatio, double meanNoImmigrants,
			int noInfectionStates, int minAge, int sex){
		
		/**
		 * Individuals migrate into a population at a given probability and the number that migrate at any one time is drawn from a Poisson
		 * distribution around the mean number of immigrants. At the moment infection cannot be brought in externally.
		 * 
		 * A sink group is chosen at random - can be weighted by the sex ratio of that group (sex == 1). Haven't included distance from edge.
		 */
		
		// Initialise Poisson Generator to chose number of immigrants
		PoissonGenerator randPoisNoImm = new PoissonGenerator(meanNoImmigrants, new Random());
		Individual individual;
		
		// Is Age included in this model? - If so then Generate a Poisson Distribution to draw immigrants age from
		PoissonGenerator randPoisAge = new PoissonGenerator(meanAge, new Random());
		
		// Initialise a random number generator
		MersenneTwister random = new MersenneTwister();
		
		// Extract the groups in the population
		Group[] groups = population.getGroups();
		int[] groupIDs = ArrayMethods.seq(0,  groups.length, 1);
		int sinkGrpIndex = -99;
		
		// Get the Sex Ratios for the groups
		double[] sexRatios4Females = new double[groups.length];
		double[] sexRatios4Males = new double[groups.length];
		if(sex == 1){
			sexRatios4Females = getGroupSexRatios(groups, minAge);
			sexRatios4Males = ArrayMethods.oneMinusElements(sexRatios4Females);
		}
		
		
		for(int i = 0; i < randPoisNoImm.nextValue(); i++){
			
			// Create the Immigrant
			Global.individualId++;
			individual = new Individual(Global.individualId, noInfectionStates);
						
			// Is the age varying around a mean?
			if(meanAge != 0){
				individual.setAge(randPoisAge.nextValue());
			}
			
			// Is sex needed to be taken into account?
			if(sexRatio != 0 && random.nextDouble() < sexRatio){
				individual.setSex(1);
			}
			
			// Find the sink group
			if(sex == 0){
				sinkGrpIndex = ArrayMethods.randomChoice(groupIDs);
			}else if(individual.getSex() == 0){
				sinkGrpIndex = ArrayMethods.randomWeightedChoice(groupIDs, sexRatios4Females);
			}else{
				sinkGrpIndex = ArrayMethods.randomWeightedChoice(groupIDs, sexRatios4Males);
			}
			
			// Add the immigrant to the sink group
			groups[sinkGrpIndex].addIndividual(individual);
			individual.setGroupIndex(sinkGrpIndex);
			
		}
		
		return population;
	}
	
	// Seed Infection
	public static Individual[] seedInfection(Individual[] individuals, int noSeeds, int statusIndex, int noStates){
		
		// Initialise a random number generator
		MersenneTwister random = new MersenneTwister();	
		
		// Initialise status variable
		int status;
		int[] states = ArrayMethods.seq(0, noStates - 1, 1);
		
		for(int i = 0; i < noSeeds; i++){
		
			// Chose a Seed
			Individual seed = IndividualMethods.randomChoice(individuals);
			
			// Choose infection status
			status = statusIndex;
			if(statusIndex == -1){
				
				// Choose a random Status
				status = ArrayMethods.randomChoice(states);
			}
			
			// Infect the seed
			seed.setStatusIndex(status);			
		}
		
		return individuals;
	}
	
	// Infection
	
	// Progression
	public static Individual diseaseProgression(Individual individual, double[][] progressionProbs, int noInfectionStates){
		
		/**
		 * Individuals move between disease states at a given rate defined by the progression probabilities:
		 * 		states: 			{	0, 	1, 		2} for {S, I, R}
		 * 		progressionProbs: 	{	0, 	0,	0
		 * 								0,	0,	0.1
		 * 								0,	0,	0	}
		 * 			Mij = the probability of progressing from state i to state j
		 */
		
		// Initialise a random number generator
		MersenneTwister random = new MersenneTwister();
		
		// Create an array of the potential infection states
		int[] potentialStates = ArrayMethods.seq(0, noInfectionStates - 1, 1);
		
		// Extract the probabilities of the current individual progression into a different disease state given it's current state
		double[] stateWeights = progressionProbs[individual.getStatusIndex()];
			
		// Calculate the overall probability of progression
		double progressionProb = 1;
		for(double prob : stateWeights){
			progressionProb = progressionProb * (1 - prob);
		}
			
		// Did the individual's disease progress?
		if(progressionProb != 1 && random.nextDouble() < (1 - progressionProb)){
			
			// Which state did the individual progress into?
			int newStateIndex = ArrayMethods.randomWeightedChoice(potentialStates, stateWeights);
			
			// Update the individual's infection state
			individual.setStatusIndex(newStateIndex);
		}
		
		return individual;
	}
	
	// Aging
	public static Population aging(Population population){
		for(Individual individual : population.getIndividuals()){
			individual.setAge(individual.getAge() + 1);
		}
		
		return population;
	}
	
	// Output
	public static void survey(Population population, int noInfectionStates, int[] ageMinBounds, int timestep, BufferedWriter bWriter) throws IOException{
		
		// What information do you want to record?
		int[] noInStates = new int[noInfectionStates];
		int[] noInAgeCategories = new int[ageMinBounds.length];
		int[] noSexes = new int[2];
		
		// Examine each individual in the population and record the relevant information
		for(Individual individual : population.getIndividuals()){
			noInStates[individual.getStatusIndex()]++;
			noInAgeCategories[findAgeCategoryIndex(ageMinBounds, individual.getAge())]++;
			noSexes[individual.getSex()]++;
		}
		
		// Print out the Survey to file
		String line = combineSurveyInfo(noInStates, noInAgeCategories, noSexes, timestep);
		
		WriteToFile.writeLn(bWriter, line);
	}

	public static Population surveyAndAging(Population population, int noInfectionStates, int[] ageMinBounds, int timestep, BufferedWriter bWriter) throws IOException{
		// What information do you want to record?
		int[] noInStates = new int[noInfectionStates];
		int[] noInAgeCategories = new int[ageMinBounds.length];
		int[] noSexes = new int[2];
				
		// Examine each individual in the population and record the relevant information
		for(Individual individual : population.getIndividuals()){
			noInStates[individual.getStatusIndex()]++;
			noInAgeCategories[findAgeCategoryIndex(ageMinBounds, individual.getAge())]++;
			noSexes[individual.getSex()]++;
			
			// Aging
			individual.setAge(individual.getAge() + 1);
		}
				
		// Print out the Survey to file
		String line = combineSurveyInfo(noInStates, noInAgeCategories, noSexes, timestep);
				
		WriteToFile.writeLn(bWriter, line);
		
		return population;
	}
	
	// General Methods
	public static int decideSex(MersenneTwister random, double sexRatio){
		int sex = 0; // Female

		if(random.nextDouble() < sexRatio){
			sex = 1;
		}
		
		return sex;
	}
	
	public static String combineSurveyInfo(int[] noInStates, int[] noInAgeCats, int[] noSexes, int timestep){
		String line = "" + timestep;
		
		for(int x : noInStates){
			line = line + "\t" + x;
		}
		
		for(int x : noInAgeCats){
			line = line + "\t" + x;
		}
		
		return line + "\t" + noSexes[0] + "\t" + noSexes[1];	
		
	}
	
	public static int findAgeCategoryIndex(int[] ageMinBounds, int age){
		
		// Age categories defined by their minimum entry age: {0, 1, 2} might be {baby, juvenile, adult}
		int index = -99;
		for(int i = 0; i < ageMinBounds.length; i++){
			if(age < ageMinBounds[i]){
				index = i - 1;
				break;
			}
		}
		
		// If older than all minBounds then assign individual to last category
		if(index == -99){
			index = ageMinBounds.length - 1;
		}
		
		return index;
		
	}
	
	public static double[][] generateGroupDistanceMatrix(int noGroups, int[] gridDimensions){
		
		// Generate array of Group Indexes (IDs)
		int[] groupIds = ArrayMethods.seq(0, noGroups - 1, 1);
		
		// Randomly place those groups on a grid of the right dimensions
		int[][] grid = GridMethods.createRandomPopulationGrid(gridDimensions[0], gridDimensions[1], groupIds);
		
		// Calculate the inter-group distances
		return GridMethods.generateEuclideanDistanceMatrix(grid, groupIds);
	}
	
	public static double[][] generateGroupDistanceMatrix(Group[] groups, char unit){
		
		double[][] groupDistances = new double[groups.length][groups.length];

		for(Group groupI : groups){
			
			int i = groupI.getGroupId();
			double[] latLongsI = groupI.getLatLongs();
			
			for(Group groupJ : groups){
				
				int j = groupJ.getGroupId();
				double[] latLongsJ = groupJ.getLatLongs();
				
				// Skip the diagonal and making the same comparison twice
				if(i <= j){
					continue;
				}
				
				// Calculate the Great circle distance between the groups
				double distance = LatLongMethods.distance(latLongsI[0], latLongsI[1], latLongsJ[0], latLongsJ[1], unit);
				
				// Store the distance
				groupDistances[i][j] = distance;
				groupDistances[j][j] = distance;
				
			}
		}
		
		return groupDistances;
		
	}
	
	public static double[] generateGroupDispersalWeights(double[] distances, double[] sexRatios, int sex){
		
		/**
		 *	Where the individual disperses to will be randomly decided weighted by the inter-group distances and the group Sex Ratios
		 * 		
		 * 		sexRatio = No. Males / No. Females
		 * 		maleWeight = (1 - sexRatio) * distance
		 * 		femaleWeight = sexRatio * distance
		*/
		
		double[] weights = new double[distances.length];
		
		for(int i = 0; i < distances.length; i++){
			
			if( sex == 1 ){ // Male
				
				weights[i] = (1 - sexRatios[i]) * distances[i];
				
			}else{ // Female
				
				weights[i] = sexRatios[i] * distances[i];
			}
		}
		
		return weights;
	}
	
	public static double[] getGroupSexRatios(Group[] groups, int minAge){
		double[] sexRatios = new double[groups.length];
		int[] sexRatio = new int[2];
		
		// Find the Sex Ratio for each of the groups
		for(int i = 0; i < groups.length; i++){
			
			sexRatio = getSexRatio(groups[i].getIndividuals(), minAge);
			
			if(sexRatio[0] != 0 && sexRatio[1] != 0){
				sexRatios[i] = (double) sexRatio[1] / (double) sexRatio[0]; // No. Males / No. Females
			}else if(sexRatio[0] == 0 && sexRatio[1] != 0){
				sexRatios[i] = 1;
			}else{
				sexRatios[i] = 0;
			}
		}
		
		return sexRatios;
	}
	
	public static int[] getSexRatio(Individual[] individuals, int minAge){
		
		// Initialise array to store the no of adult females and males in the group
		int[] sexRatio = new int[2];
		
		// Investigate each of the Individuals
		for(Individual individual : individuals){
			if(individual.getAge() >= minAge){
				sexRatio[individual.getSex()]++;
			}
		}
		
		return sexRatio;
	}

	public static int findMinAge4Dispersal(double[][] dispersalProbs, int[] ageMinBounds){
		/**
		 * Individuals will disperse at a given probability each timestep. This could depend on sex and or age
		 * 			Sex\AgeMinBounds	0	1	2		
		 * 			F					0	0.1	0.2
		 * 			M					0	0.1	0.15	
		*/
		
		int minAge = -1;
		for(int i = 0; i < ageMinBounds.length; i++){
			
			if(dispersalProbs[0][i] + dispersalProbs[1][i] > 0){
				minAge = ageMinBounds[i];
				break;
			}
		}
		
		return minAge;
	}
}
