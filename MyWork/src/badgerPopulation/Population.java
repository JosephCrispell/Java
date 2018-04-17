package badgerPopulation;

import org.uncommons.maths.random.PoissonGenerator;

import methods.ArrayMethods;

import java.util.Random;

public class Population {

	public Badger[][] population;
	public Grid grid;
	public int badgerId;
	public int popSize;
	public int[][] groupAge; // {Cf, Jf, Af, Cm, Jm, Am}
	public int[][] groupInfection; // Record the number of individuals in group: {S, E, I, G}
	
	// Mutation Events
	public int mutationEventNo;
	
	public Population(int nGroups, PoissonGenerator agePoisson, PoissonGenerator grpSizePoisson, 
			double probMale, Random random, int[] dimensions){
	   	 
		createInitialPopulation(nGroups, agePoisson, grpSizePoisson, probMale, random);
		
		generatePopulationGrid(dimensions, nGroups);
	}

	// Setting methods
	public void setBadgersInfectionStatus(int groupIndex, int badgerIndex, int infectionStatus){
		
		/**
		 * 	GroupInfection Structure:
		 * 	S	E	I	G
		 * 	0	1	2	3
		 */
		
		// Get the previous status for the badger
		int status = this.population[groupIndex][badgerIndex].getInfectionStatus();
		this.groupInfection[groupIndex][status]--;
		
		// Set the badgers infection status
		this.population[groupIndex][badgerIndex].setInfectionStatus(infectionStatus);
		
		// Update the badger group info
		status = this.population[groupIndex][badgerIndex].getInfectionStatus();
		this.groupInfection[groupIndex][status]++;
	}
	
	// Getting methods
	public int getPopSize(){
		return this.popSize;
	}
	
	// Infection
	public void seedInfection(int nSeeds, int status, Random random){
		
		// Initialise a variable to record how many seeds have been added
		int nSeedsAdded = 0;
		
		// Initialise variables to note the badger and group chosen
		int groupIndex;
		int badgerIndex;
		
		while(nSeedsAdded < nSeeds){
			
			// Randomly pick badger group
			groupIndex = ArrayMethods.randomIndex(this.population.length, random);
			
			// Check that the group has susceptible badgers in it
			if(this.groupInfection[groupIndex][0] > 0){
				
				// Choose a seed
				badgerIndex = ArrayMethods.randomIndex(this.population[groupIndex].length, random);
				
				// Infect the seed
				setBadgersInfectionStatus(groupIndex, badgerIndex, status);
				
				// Record that we infected a seed badger
				nSeedsAdded++;
			}
		}		
	}
	
	public void progression(double[] progressionRates, Random random){
		
		/**
		 * Progression rates is an array with the probability of leaving the state e.g.:
		 * 		S	E		I		G
		 * 		0	0.1		0.05	0
		 * 
		 * Progression can only be: E -> I -> G
		 */
		
		// Examine every individual in the population
		for(int groupIndex = 0 ; groupIndex < this.population.length; groupIndex++){
			for(int badgerIndex = 0; badgerIndex < this.population[groupIndex].length; badgerIndex++){
				
				// Skip susceptible and generalised infected individuals
				if(this.population[groupIndex][badgerIndex].getInfectionStatus() == 0 ||
						this.population[groupIndex][badgerIndex].getInfectionStatus() == 3){
					continue;
				}
				
				// Did the infection in the current individual progress?
				if(random.nextDouble() < progressionRates[this.population[groupIndex][badgerIndex].getInfectionStatus()]){
					
					// Note that the infection in the current individual has progressed
					this.groupInfection[groupIndex][this.population[groupIndex][badgerIndex].getInfectionStatus()]--;
					this.population[groupIndex][badgerIndex].setInfectionStatus(this.population[groupIndex][badgerIndex].getInfectionStatus() + 1);
					this.groupInfection[groupIndex][this.population[groupIndex][badgerIndex].getInfectionStatus()]++;
					
					System.out.println("here!");
				}				
			}
		}
	}
	
 	public void betweenGroupInfection(double[] infectiousnessBetweenGroup, int season, int seasonCount,
			PoissonGenerator[] mutationPoissons, Random random){
		
		// Calculate the force of infection for each group
		double[] forceOfInfectionForEachGroup = calculateForceOfInfection(infectiousnessBetweenGroup);
		double[] groupWeights;
		double forceOfInfection;
		
		// Initialise variables for finding the source of infection
		int sourceGroup;
		double[] sourceWeights;
		int sourceIndex;
		
		// Get the group distance matrix
		double[][] distances = grid.getDistanceMatrix();
		
		// Examine each group in the population
		for(int groupIndex = 0; groupIndex < this.population[groupIndex].length; groupIndex++){
			
			// Skip groups where no susceptible individuals are present
			if(this.groupInfection[groupIndex][0] == 0){
				continue;
			}
			
			// Scale each group's force of infection by it's distance to the current group
			groupWeights = ArrayMethods.divide(forceOfInfectionForEachGroup, distances[groupIndex]);
			
			// Calculate the between-group force of infection for the current group
			forceOfInfection = calculateOutsideForceOfInfection(groupWeights);
			
			// Did any of the badgers in the current group get infected?
			for(int badgerIndex = 0; badgerIndex < this.population[groupIndex].length; badgerIndex++){
				
				// Skip non-susceptible individuals or cubs in their first 3 months
				if(this.population[groupIndex][badgerIndex].getInfectionStatus() != 0 || 
						(this.population[groupIndex][badgerIndex].getAge() == 0 && season == 0)){
					continue;
				}
				
				// Did the current individual get infected?
				if(random.nextDouble() < forceOfInfection){
					
					// Find a source for the current individual
					sourceGroup = ArrayMethods.randomWeightedIndex(groupWeights, random);
					sourceWeights = calculateSourceWeights(groupIndex, infectiousnessBetweenGroup);
					sourceIndex = ArrayMethods.randomWeightedIndex(sourceWeights, random);
					
					// Record the transmission event
					recordTransmissionEvent(groupIndex, badgerIndex, sourceGroup, sourceIndex, seasonCount, mutationPoissons);
				}
			}
		}		
	}
	
	public void withinGroupInfection(int season, int seasonCount, double[] infectiousness, 
			PoissonGenerator[] mutationPoissons, Random random){
		
		/**
		 * Within Group Infection:
		 * 	Using the MASS ACTION PRINCIPLE the force of infection is calculated as:
		 * 		1 - (1 - ir)^i * (1 - gr)^g
		 * 
		 * 	It is important to have source of infection. The source is randomly chosen from the group's infected badgers.
		 * 	This random choice is weighted (by infection rate) towards badgers carrying a generalised infection.
		 * 	
		 * 	Notes:
		 * 		Cubs cannot get infected in their first three months of life - the are still confined to their burrow and 
		 * 		likely to only be in contact with their mother.
		 * 		Infected Badgers will move directly into the Exposed State
		 * 		Not allowing for multiple infections or Recovery
		 */
		
		// Calculate the force of infection in each group
		double[] forceOfInfection = calculateForceOfInfection(infectiousness);
		double[] sourceWeights;
		int source;
		
		// Examine the badgers in each group
		for(int groupIndex = 0; groupIndex < this.population.length; groupIndex++){
			
			// Skip groups with no infectious individuals OR no susceptible individuals
			if((this.groupInfection[groupIndex][2] == 0 && this.groupInfection[groupIndex][3] == 0) ||
					this.groupInfection[groupIndex][0] == 0){
				continue;
			}
			
			System.out.println("here!");
			
			// Calculate the weights for potential sources of infection
			sourceWeights = calculateSourceWeights(groupIndex, infectiousness);
			
			// Examine the badgers in this group
			for(int badgerIndex = 0; badgerIndex < this.population[groupIndex].length; badgerIndex++){
				
				// Skip non-susceptible individuals or cubs in their first 3 months
				if(this.population[groupIndex][badgerIndex].getInfectionStatus() != 0 || 
						(this.population[groupIndex][badgerIndex].getAge() == 0 && season == 0)){
					continue;
				}
				
				// Did the current susceptible badger get infected?
				if(random.nextDouble() < forceOfInfection[groupIndex]){
					
					// Who infected the current badger?
					source = ArrayMethods.randomWeightedIndex(sourceWeights, random);
					
					// Record the transmission event
					recordTransmissionEvent(groupIndex, badgerIndex, groupIndex, source, seasonCount, mutationPoissons);
				}
			}			
		}
	}
	
	// Population Dynamics
	public void immigration(int minAge, double probMale, PoissonGenerator nImmigrantsPoisson, PoissonGenerator agePoisson, boolean closed,
			Random random){
		
		/**
		 * Estimate from the data the number of badgers moving into the population per season
		 * 		No. immigrants = (trapping Efficiency per Year * No. of Adults/Juveniles appearing in Year) / 4
		 * 
		 * Immigration is simply modelled using the Average Number of Immigrants - as taken from the data.
		 * A certain number of individuals (taken from Poisson distribution around the mean no. immigrants) will be 
		 * preferentially added to groups within the population. Groups which are closest to the edge of the grid 
		 * will be favoured.
		 * Immigrants are Susceptible.
		 * Could Have a grid nested within another grid
		 * 
		 * Groups were additionally weighted by the sex ratio dependent on the randomly assigned sex of the immigrant 
		 * 
		 * ASSUMES NO INTRODUCTION OF DISEASE FROM OUTSIDE
		 */
		
		// How many immigrants were there?
		int nImmigrants = 0;
		if(closed == false){
			nImmigrants = nImmigrantsPoisson.nextValue();
		}
		
		// Initialise some variables describing the immigrants
		int age;
		int sex;
				
		// Initialise a variable to store the immigration weights for each group
		int[][] nFemalesMales = getGroupSexRatios(minAge - 1, true);
		double[][] weights = calculateDispersalWeights(-1, this.grid.getEdgeDistances(), nFemalesMales); // -1 for current group
		int destinationGroup;
		
		// Add each of the new immigrants into the population
		for(int i = 0; i < nImmigrants; i++){
			
			// Create an ID for the badger
			this.badgerId++;
			
			// Get the age
			age = agePoisson.nextValue();
			
			// Get the sex
			sex = 0; // Female
			if(random.nextDouble() < probMale){
				sex = 1; // Male
			}
			
			// Choose a group for the current badger
			destinationGroup = ArrayMethods.randomWeightedIndex(weights[sex], random);
			
			// Add the new badger into the population
			this.population[destinationGroup] = BadgerMethods.addBadger(this.population[destinationGroup],
					new Badger(sex, age, 0, destinationGroup, badgerId));
			recordAddedBadger(destinationGroup, sex, 0, age);
		}		
	}

	public void dispersal(double[] dispersalRates, int minAge, int season, double[] seasonalEffects, boolean closed,
			Random random){
		/**
		 * Dispersal:
		 * 	Adults and Juveniles are able to migrate to any of the groups in the population via weighted dispersal.
		 *  Weighted Dispersal:
		 *  	- Euclidean Distance
		 *  	- Sex Ratio (i.e. Males attracted to Females and vice versa)
		 *  
		 *  Weight = (1 / Distance) * Sex Ratio
		 *  
		 *  
		 * Dispersal Out of Population:
		 * 	Edge is given weight = (1 / distanceToEdge) * 0.5
		 * 	Note that Sex Ratio for outside Population is assumed to 50:50
		 * 	Edge is added to list of groups to be randomly chosen from according to a set of weights
		 */
		
		// Extract the Group Distance Matrix from the Population Grid Object
		double[][] distances = this.grid.getDistanceMatrix();
		double[] groupDistancesToEdge = grid.getEdgeDistances();
		int destinationGroup;
		
		// Count the number of males and females above the minAge in each group
		int[][] nFemalesMales = getGroupSexRatios(minAge - 1, true);

		// Initialise arrays to store the weights assigned to each group for males and females
		double[][] weights = new double[2][this.population.length];
		
		// Examine each group
		for(int groupIndex = 0; groupIndex < this.population.length; groupIndex++){
			
			// Skip group if there are no males and females to migrate
			if(nFemalesMales[groupIndex][0] == 0 && nFemalesMales[groupIndex][1] == 0){
				continue;
			}
			
			// Get the dispersal weights for the males and females
			weights = calculateDispersalWeights(groupIndex, distances[groupIndex], nFemalesMales);
			
			// Assign an emigration probability
			if(closed == false){
				
				weights[groupIndex][0] = 0.5;
				weights[groupIndex][1] = 0.5;
				if(groupDistancesToEdge[groupIndex] != 0){
					weights[groupIndex][0] = (1 / groupDistancesToEdge[groupIndex]) * 0.5;
					weights[groupIndex][1] = (1 / groupDistancesToEdge[groupIndex]) * 0.5;
				}
			}
			
			//System.out.println("");
			//System.out.println("Group Index = " + groupIndex);
			//System.out.println("Dispersal Weights: ");
			//System.out.println("\t\t" + ArrayMethods.toString(weights[0], ", "));
			//System.out.println("\t\t" + ArrayMethods.toString(weights[1], ", "));
			
			// Did any badgers in the current group disperse?
			for(int badgerIndex = 0; badgerIndex < this.population[groupIndex].length; badgerIndex++){
				
				// Skip badgers that aren't old enough
				if(this.population[groupIndex][badgerIndex].getAge() < minAge){
					continue;
				}
				
				// Did a dispersal event occur?
				if(random.nextDouble() < dispersalRates[this.population[groupIndex][badgerIndex].getSex()] * seasonalEffects[season]){
				
					// Where did the badger go?
					destinationGroup = ArrayMethods.randomWeightedIndex(weights[this.population[groupIndex][badgerIndex].getSex()], random);
					
					// Add the badger into the new group
					if(destinationGroup != groupIndex){
						this.population[destinationGroup] = BadgerMethods.addBadger(this.population[destinationGroup], this.population[groupIndex][badgerIndex]);
						recordAddedBadger(destinationGroup, this.population[destinationGroup][this.population[destinationGroup].length - 1].getSex(),
								this.population[destinationGroup][this.population[destinationGroup].length - 1].getInfectionStatus(),
								this.population[destinationGroup][this.population[destinationGroup].length - 1].getAge());
					
					} // Otherwise do nothing and remove the badger
					
					// Remove the badger from the previous group
					removeBadger(groupIndex, badgerIndex);
				}
			}			
		}
	}
	
	public void birth(int seasonCount, double probMale, Boolean pseudoVertical, PoissonGenerator litterSize,
			PoissonGenerator[] mutationPoissons, Random random){
		
		/**
		 * Females which were successfully fertilised in the PREVIOUS year will give birth
		 * 		There litter Size will be randomly drawn from a Poisson Distribution around the Average Litter Size
		 * 		If PseudoVerticalTransission is enabled the cub will begin life in the Exposed state 
		 * 		if its Mother has a	generalised infection
		 */

		// Initialise a variable to store the litter size
		int nCubs;
		Badger[] cubs;
		int sex;
		int cubInfectionStatus;
		int[] mutationEvents = null;
		
		// Examine each of the badgers in the population
		for(int groupIndex = 0; groupIndex < this.population.length; groupIndex++){
			for(int badgerIndex = 0; badgerIndex < this.population[groupIndex].length; badgerIndex++){
				
				// Only interested in fertilised females
				if(this.population[groupIndex][badgerIndex].getFertilisationStatus() != 'F'){
					continue;
				}
				
				// How many cubs did the current female have?
				nCubs = litterSize.nextValue();
				cubs = new Badger[nCubs];
				
				// Get the infection status of the cubs
				cubInfectionStatus = 0;
				if(this.population[groupIndex][badgerIndex].getInfectionStatus() == 3 && pseudoVertical == true){
					cubInfectionStatus = 1;
					
					// Update the mothers mutation events list
					mutateBadgerSequence(groupIndex, badgerIndex, seasonCount, mutationPoissons);
					mutationEvents = this.population[groupIndex][badgerIndex].getMutationEvents();
					
					// Note which badgers the mother infected
					this.population[groupIndex][badgerIndex].setInfectees(ArrayMethods.combine(
							this.population[groupIndex][badgerIndex].getInfectees(), 
							ArrayMethods.seq(this.badgerId + 1, this.badgerId + nCubs, 1)));
				}
				
				// Create each of the cubs
				for(int i = 0; i < nCubs; i++){
					
					// Find the sex of the current cub
					sex = 0; // Female
					if(random.nextDouble() < probMale){
						sex = 1; // Male
					}
					
					// Generate a unique badger id
					this.badgerId++;
					
					// Create the cub
					cubs[i] = new Badger(sex, 0, cubInfectionStatus, groupIndex, this.badgerId);
					recordAddedBadger(groupIndex, sex, cubInfectionStatus, 0);
					
					// Pass the mutation events to the cub if mother has generalised infection and pseudovertical is on
					if(cubInfectionStatus == 1){
						cubs[i].setMutationEvents(mutationEvents, seasonCount);
					}
				}
				
				// Add the cubs into the population
				this.population[groupIndex] = BadgerMethods.addBadgers(this.population[groupIndex], cubs);
				
				// Reset fertilisation status of the mother
				this.population[groupIndex][badgerIndex].setFertilisationStatus('N');
			}
		}
	}
	
	public void breeding(int minBreedingAge){
		
		/**
		 * Breeding will occur only if both males and females (above minAge) are present in the group.
		 * All females (above minAge) will be fertilised.
		 */
		
		int[] nFemalesMales = new int[2];
		
		// Examine the badgers in each of the groups
		for(int groupIndex = 0; groupIndex < this.population.length; groupIndex++){
			
			// How many males and females above minAge are there?
			nFemalesMales = getNumberBadgers(groupIndex, minBreedingAge - 1, true);
			
			// Skip groups where males and females aren't both present
			if(nFemalesMales[0] == 0 && nFemalesMales[1] == 0){
				continue;
			}
			
			// Examine each of the badgers in the group
			for(int badgerIndex = 0; badgerIndex < this.population[groupIndex].length; badgerIndex++){
				
				// Skip males and under age females
				if(this.population[groupIndex][badgerIndex].getSex() == 1 || this.population[groupIndex][badgerIndex].getAge() < minBreedingAge){
					continue;
				}
				
				// Fertilise current female badger
				this.population[groupIndex][badgerIndex].setFertilisationStatus('F');
			}
		}
		
	}
	
	public void death(double[] deathRates, double carryingCapacity, Random random, double[] infectionEffects){
		
		/**
		 * Death Rates structure: { Female, Male, Cub }
		 * Infection Effects: {1, 1, 1, 2} e.g. Generalised Infection state increase death rate by 2
		 * 		
		 * 		Adult mortality can be affected by the Infection Status of the Badger
		 * 		Dr = Dr * InfectionEffect 
		 * 			
		 * 		Cub mortality is affected by Density Dependence
		 * 			Survival of a Cub is dependent on its group Size (no. of adults and juveniles) in relation to the 
		 * 			carrying capacity
		 * 			
		 * 
		 * 			LOGISTIC:
		 * 			Death Rate 1|               * * * *
		 * 			 		 0.9|             * 
		 * 					 0.8|            * 
		 * 					 0.7|           *
		 * 					 0.6|		   *				Death Rate is dependent on the difference between the group
		 * 				 Dr	 0.5|----------*				size and carrying capacity
		 * 					 0.4|     	   *|				y = 1 / (1 + e^(K - x))
		 * 					 0.3|  		  * |				
		 * 					 0  | * * *     |
		 * 						|______________________
		 * 			   			  0 1 2 3 4 5 6 7 8 9 10 Group Size
		 *                      		  K
		 * 			
		 * 			LINEAR:
		 * 			Death Rate 1|                   *
		 * 			 		 0.9|                 *
		 * 					 0.8|               *
		 * 					 0.7|             *
		 * 					 0.6|		    *
		 * 				 Dr	 0.5|---------*				Linear Relationship such that the Death Rate is lower
		 * 					 0.4|       * |				when the Group Size (N) is below K and higher when it is above.
		 * 					 0.3|     *   |				y = mx + c 	---> y = mx
		 * 					 0.2|   *     |				Dr = mN 	---> m = Dr/K
		 * 					 0.1| *       |				aDr = (Dr/K) * N
		 * 					 0  *___________________
		 * 			   			0 1 2 3 4 5 6 7 8 9 10 Group Size
		 *                      		  K
		 *                      
		 *        An Exponential relationship would be: y = e^x + c
		 *                      
		 */
		
		/**
		 * 	GroupInfection Structure:
		 * 	S	E	I	G
		 * 	0	1	2	3
		 * 
		 * 	GroupAge Structure:
		 * 	Cf	Jf	Af	Cm	Jm	Am
		 * 	0	1	2	3	4	5
		 */
		
		// Initialise variables to calculate the cub mortality rate
		double groupSize;
		double cubDeathRate;
		int[] ageCategoriesForGroupSize = {0, 1, 2}; // Cubs, Juveniles, Adults
		
		// Initialise a variable to calculate the adult death rate
		double adultDeathRate;
		
		// Examine the badgers in each of the groups
		for(int groupIndex = 0; groupIndex < this.population.length; groupIndex++){
			
			
			// Skip empty groups
			if(ArrayMethods.sum(this.groupAge[groupIndex]) == 0){
				continue;
			}
			
			// Calculate the cub death rate for the current group
			//groupSize = ArrayMethods.sum(getNumberBadgers(groupIndex, 0, true));
			groupSize = getGroupSize(groupIndex, ageCategoriesForGroupSize);
			//cubDeathRate = (deathRates[2] / carryingCapacity) * groupSize;
			cubDeathRate = 1 / (1 + Math.exp(carryingCapacity - groupSize));
			
			// Check if any of the badgers in the current group died
			for(int badgerIndex = 0; badgerIndex < this.population[groupIndex].length; badgerIndex++){
				
				// Is the current badger a cub?
				if(this.population[groupIndex][badgerIndex].getAge() == 0){
					
					// Did the current cub die?
					if(random.nextDouble() < cubDeathRate){
						
						// Remove the badger from the population
						removeBadger(groupIndex, badgerIndex);
					}
					
				// The current badger is an adult
				}else{
					
					// Calculate the death rate
					adultDeathRate = deathRates[this.population[groupIndex][badgerIndex].getSex()] * 
							infectionEffects[this.population[groupIndex][badgerIndex].getInfectionStatus()];
					
					// OLD Calculate the effect of both Age and Infection Status on this individual's survival probability
					//double deathRate = 1 - Math.pow(1 - deathRates[0], ((double) badger.getAge() / avgAges[0]) * infection);
					
					// Did the current badger die?
					if(random.nextDouble() < adultDeathRate){
						
						// Remove the badger from the population
						removeBadger(groupIndex, badgerIndex);
					}
				}
			}			
		}		
	}
	
	public void aging(){
		
		/**
		 * 	GroupAge Structure:
		 * 	Cf	Jf	Af	Cm	Jm	Am
		 * 	0	1	2	3	4	5
		 */
		
		int age;
		int sex;
		
		for(int groupIndex = 0; groupIndex < this.population.length; groupIndex++){
			
			for(int badgerIndex = 0; badgerIndex < this.population[groupIndex].length; badgerIndex++){
				
				// Get the current badger's age
				age = this.population[groupIndex][badgerIndex].getAge();
				sex = this.population[groupIndex][badgerIndex].getSex();
				
				// Record the change in age category if it occurred
				if(sex == 0 && age < 2){
					if(age == 0){
						this.groupAge[groupIndex][0]--;
						this.groupAge[groupIndex][1]++;
					}else if(age == 1){
						this.groupAge[groupIndex][1]--;
						this.groupAge[groupIndex][2]++;
					}
				}else if(sex == 1 && age < 2){
					if(age == 0){
						this.groupAge[groupIndex][3]--;
						this.groupAge[groupIndex][4]++;
					}else if(age == 1){
						this.groupAge[groupIndex][4]--;
						this.groupAge[groupIndex][5]++;
					}
				}				
				
				// Change the current badger's age
				this.population[groupIndex][badgerIndex].setAge(age + 1);				
			}
		}
	}
	
	// Initialising Population
	public void createInitialPopulation(int nGroups, PoissonGenerator agePoisson, PoissonGenerator grpSizePoisson,
			double probMale, Random random){
		
		/**
		 * Starting Situation:
		 * 		Group Size is taken from a Poisson Distribution around the Mean group Size
		 * 		Badger Age taken from Poisson Distribution around the Mean Badger Age
		 * 		No infection Present
		 * 
		 * Could set up to run from an Initial stable population starting point instead...
		 * 
		 *  GroupAge Structure:
		 * 	Cf	Jf	Af	Cm	Jm	Am
		 * 	0	1	2	3	4	5
		 * 
		 * GroupInfection Structure:
		 * 	S	E	I	G
		 * 	0	1	2	3
		 */
		
		// Initialise an empty population
		this.population = new Badger[nGroups][0];
		
		// Initialise the record for each badger group
		this.groupAge = new int[nGroups][6]; // {Cf, Jf, Af, Cm, Jm, Am}
		this.groupInfection = new int[nGroups][4]; // {S, E, I, G}
		
		// Initialise the badger id
		this.badgerId = -1;
		this.popSize = 0;
		int sex;
		int age;
		
		// Create the badger groups
		for(int groupIndex = 0; groupIndex < nGroups; groupIndex++){
			
			// Randomly generate an empty badger group of a given size
			this.population[groupIndex] = new Badger[grpSizePoisson.nextValue()];
			
			// Create each of the badgers in the group
			for(int badgerIndex = 0; badgerIndex < this.population[groupIndex].length; badgerIndex++){
				
				// Find the sex of the current badger
				sex = 0; // Female
				if(random.nextDouble() < probMale){
					sex = 1; // Male
				}
				
				// Generate a unique badger id
				this.badgerId++;
				this.popSize++;
				
				// Find the age for the current badger
				age = agePoisson.nextValue();
				
				// Create the badger
				this.population[groupIndex][badgerIndex] = new Badger(sex, age, 0, groupIndex,
						this.badgerId);
				
				// Record that the badger was added into the population
				recordAddedBadger(groupIndex, sex, 0, age);
			}
		}
	}

	public void generatePopulationGrid(int[] dimensions, int nGroups){
		
		/**
		 * Badger Groups are randomly assigned a cell on a Grid
		 * 		Directly connected groups are noted
		 * 		A Group Distance Matrix is generated
		 * 		Group Edge Distances are Calculated
		 * 
		 * Population Grid is stored as an object
		 */
		
		// dimensions = { noRows, noCols }
		
		// Place the Groups on the Grid
		int[] groupIds = ArrayMethods.seq(0, nGroups - 1, 1);
		int[][] grid = GridMethods.createRandomPopulationGrid(dimensions[0], dimensions[1], groupIds);
		
		// Generate the Distance Matrix
		double[][] distanceMatrix = GridMethods.generateEuclideanDistanceMatrix(grid, groupIds);
		
		// Store the Distance From Edge for each Group
		double[] groupEdgeDistances = GridMethods.calculateGroupEdgeDistances(grid, groupIds.length);
		
		// Store the Population Grid Information
		this.grid = new Grid(grid, groupIds, distanceMatrix, groupEdgeDistances);
	}
	
	// General Methods - Population Dynamics
	public double[][] calculateDispersalWeights(int currentGroupIndex, double[] distances, int[][] nFemalesMales){
		
		// Initialise an array to store the weights for males and females
		double[][] weights = new double[2][this.population.length];
		
		// Initialise variables to store the number of males and females
		double nFemales;
		double nMales;
		
		// Calculate the dispersal weights from the current group to every other group
		for(int groupIndex = 0; groupIndex < this.population.length; groupIndex++){
			
			// Skip the current group
			if(groupIndex == currentGroupIndex){
				continue;
			}
			
			// Get the number of males and females in the group
			nFemales = nFemalesMales[groupIndex][0];
			nMales = nFemalesMales[groupIndex][1];
			if(nFemales == 0){
				nFemales = 1;
			}
			if(nMales == 0){
				nMales = 1;
			}
			
			// Females: nMales / nFemales
			weights[0][groupIndex] = (1 / distances[groupIndex]) * (nMales / nFemales);
			
			// Males: nFemales / nMales
			weights[1][groupIndex] = (1 / distances[groupIndex]) * (nFemales / nFemales);
		}
		
		return weights;
	}
	
	public int[] getNumberBadgers(int groupIndex, int ageCategory, boolean above){
		
		 /**
		 * 	GroupAge Structure:
		 * 	Cf	Jf	Af	Cm	Jm	Am
		 * 	0	1	2	3	4	5
		 */
		
		int[] nFemalesMales = new int[2];
		
		if(above == true){
			
			// Females
			for(int i = ageCategory + 1; i < 4; i++){
				nFemalesMales[0] += this.groupAge[groupIndex][i];
			}
			
			// Males
			for(int i = ageCategory + 1 + 3; i < 6; i++){
				nFemalesMales[1] += this.groupAge[groupIndex][i];
			}
			
		}else{
			// Females
			for(int i = 0; i < ageCategory; i++){
				nFemalesMales[0] += this.groupAge[groupIndex][i];
			}
			
			// Males
			for(int i = 3; i < ageCategory + 3; i++){
				nFemalesMales[1] += this.groupAge[groupIndex][i];
			}
		}
		
		return nFemalesMales;
	}
	
	public int getGroupSize(int groupIndex, int[] categories){
		
		/**
		 * 	GroupAge Structure:
		 * 	Cf	Jf	Af	Cm	Jm	Am
		 * 	0	1	2	3	4	5
		 */
		
		// Initialise a variable to store the group size
		int groupSize = 0;
		
		// Count the number of badgers in each category
		for(int i : categories){
			
			// Females
			groupSize += this.groupAge[groupIndex][i];
			// Males
			groupSize += this.groupAge[groupIndex][i + 3];
		}
		
		return groupSize;
	}
	
	public int[][] getGroupSexRatios(int ageCategory, boolean above){
		
		// Initialise a matrix to store the numbers of females and males in each group
		int[][] nFemalesMales = new int[this.population.length][2];
		
		// Find the number of males and females (above/below minAge) for each group
		for(int groupIndex = 0; groupIndex < this.population.length; groupIndex++){
			
			nFemalesMales[groupIndex] = getNumberBadgers(groupIndex, ageCategory, above);
		}
		
		return nFemalesMales;
	}
	
	public void removeBadger(int groupIndex, int badgerIndex){
		
		/**
		 *  GroupAge Structure:
		 * 	Cf	Jf	Af	Cm	Jm	Am
		 * 	0	1	2	3	4	5
		 * 
		 * GroupInfection Structure:
		 * 	S	E	I	G
		 * 	0	1	2	3
		 */
		
		// *** Record the removal of the badger from it's group
		// Age
		int age = this.population[groupIndex][badgerIndex].getAge();
		int sex = this.population[groupIndex][badgerIndex].getSex();
		// Age: Females
		if(sex == 0){
			if(age < 2){
				this.groupAge[groupIndex][age]--;
			}else{
				this.groupAge[groupIndex][2]--;
			}
		// Age: Males
		}else if(sex == 1){
			if(age < 2){
				this.groupAge[groupIndex][age + 3]--;
			}else{
				this.groupAge[groupIndex][5]--;
			}
		}
		
		// Infection status
		this.groupInfection[groupIndex][this.population[groupIndex][badgerIndex].getInfectionStatus()]--;
		
		// *** Remove the badger ***
		this.population[groupIndex] = BadgerMethods.deleteBadger(this.population[groupIndex], badgerIndex);
		this.popSize--;
	}
	
	public void recordAddedBadger(int groupIndex, int sex, int infectionStatus, int age){
		
		/**
		 *  Update the associated group with the information from the current badger
		 *  
		 *  GroupAge Structure:
		 * 	Cf	Jf	Af	Cm	Jm	Am
		 * 	0	1	2	3	4	5
		 * 
		 * GroupInfection Structure:
		 * 	S	E	I	G
		 * 	0	1	2	3
		 */
		
		// Age: Females
		if(sex == 0){
			if(age == 0){
				this.groupAge[groupIndex][0]++;
			}else if(age == 1){
				this.groupAge[groupIndex][1]++;
			}else{
				this.groupAge[groupIndex][2]++;
			}
		// Age: Males
		}else if(sex == 1){
			if(age == 0){
				this.groupAge[groupIndex][3]++;
			}else if(age == 1){
				this.groupAge[groupIndex][4]++;
			}else{
				this.groupAge[groupIndex][5]++;
			}
		}
		
		// Infection Status
		this.groupInfection[groupIndex][infectionStatus]++;
		
		// Population
		this.popSize++;
	}
	
	// General Methods - Infection
	public double calculateOutsideForceOfInfection(double[] forceOfInfectionFromEachGroup){
		
		// Initialise a variable to store the calculated outside force of infection for the current group
		double force = 1;
		for(int i = 0; i < forceOfInfectionFromEachGroup.length; i++){
			force *= (1 - forceOfInfectionFromEachGroup[i]);
		}
		
		return 1 - force;
	}

	public void mutateBadgerSequence(int groupIndex, int badgerIndex, int seasonCount, PoissonGenerator[] randomPoissons){
		
		// When was the infected Individual last checked?
		int nTimeSteps = seasonCount - this.population[groupIndex][badgerIndex].getMutationsLastChecked();

		/**
		 *  Calculate the number of mutations that occurred for the current individual.
		 *  Drawing from a poisson distribution around the mutation rate associated
		 *  with the current individual's infection status. Do this for each timeStep
		 *  since the mutation events of the current individual were last checked.
		 */
		int nMutations = 0;
		for(int i = 0; i < nTimeSteps; i++){
			nMutations += randomPoissons[this.population[groupIndex][badgerIndex].getInfectionStatus()].nextValue();
		}
		
		// Check that mutations have occurred 
		if(nMutations > 0){
			
			// Generate a list of the these mutations
			int[] newEvents = ArrayMethods.seq(this.mutationEventNo + 1, this.mutationEventNo + nMutations, 1);

			// Combine the new mutation events with the individuals current list
			this.population[groupIndex][badgerIndex].addMutationEvents(newEvents, seasonCount);
			
			// Update the mutation event counter
			this.mutationEventNo =  this.mutationEventNo + nMutations;
		}
	}
	
	public double[] calculateForceOfInfection(double[] infectiousness){
		/**
		 * 	Using the MASS ACTION PRINCIPLE the force of infection is calculated as:
		 * 		1 - (1 - ir)^i * (1 - gr)^g
		 */
		
		// Initialise an array to store the force of infection for each group
		double[] forceOfInfection = new double[this.population.length];
		
		// Examine each group
		for(int groupIndex = 0; groupIndex < this.population.length; groupIndex++){
			forceOfInfection[groupIndex] =  1 - (Math.pow(1 - infectiousness[0], this.groupInfection[groupIndex][2]) *
					Math.pow(1 - infectiousness[1], this.groupInfection[groupIndex][3]));
		}
		
		return forceOfInfection;
	}
	
	public void recordTransmissionEvent(int infecteeGroup, int infecteeIndex, int sourceGroup, int sourceIndex,
			int seasonCount, PoissonGenerator[] mutationPoissons){
		
		// Change the susceptible badgers infection status
		this.population[infecteeGroup][infecteeIndex].setInfectionStatus(1);
		
		// Pass on the sources mutation events sequence
		mutateBadgerSequence(sourceGroup, sourceIndex, seasonCount, mutationPoissons);
		this.population[infecteeGroup][infecteeIndex].setMutationEvents(this.population[sourceGroup][sourceIndex].getMutationEvents(), seasonCount);
		
		// Record the change in infection status
		this.groupInfection[infecteeGroup][0]--;
		this.groupInfection[infecteeGroup][1]++;
		
		// Record the transmission event
		this.population[sourceGroup][sourceIndex].setInfectees(
				ArrayMethods.append(this.population[sourceGroup][sourceIndex].getInfectees(), 
						this.population[infecteeGroup][infecteeIndex].getBadgerId()));
	}
	
	public double[] calculateSourceWeights(int groupIndex, double[] infectiousness){
		
		// Initialise an array to store the weights
		double[] weights = new double[this.population[groupIndex].length];
		
		// Assign a weight for each individual in the group - according to the infectiousness of their infection state
		for(int badgerIndex = 0; badgerIndex < this.population[groupIndex].length; badgerIndex++){
			weights[badgerIndex] = infectiousness[this.population[groupIndex][badgerIndex].getInfectionStatus()];
		}
		
		return weights;
	}


}
