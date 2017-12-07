package badgerPopulation;

import java.util.Arrays;

import methods.ArrayMethods;
import Jama.*;

public class NGMMethods {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	public static double generateNextGenerationMatrix(Badger[][] population, double exposed2Infected,
			double infected2Generalised, double[] dispersalProbs, double seasonsEffect, int minAge,
			double[] deathProbs, double[] avgAges, double infectionEffect, char seedState, double[] infectionProbs){
		
		/**
		 * Computing the Next Generation Matrix
		 * 	
		 * 	Mij = the probability that i, if infected, would infect j over the course of its life
		 * 
		 * 	The Next Generation Matrix can be used as a means of estimating an Basic Reproduction Number value for the system. Each individual in the population is 
		 * 	compared to one another and Mij = the probability that i, if infected, would infect j over the course of its life. The lead eigenvalue of this matrix has
		 *  been shown to equivalent to the R0.
		 *  
		 *  Currently the R0 is only estimated taking into account WITHIN group spread.
		 */
		
		int popSize = BadgerMethods.calculatePopulationSize(population);
		
		double[][] ngm = new double[popSize][popSize];
		
		int i = -1;
		for( Badger[] group : population ){
			for( Badger seed : group ){
				i++;
				
				int j = -1;
				for( Badger[] grp : population ){
					for( Badger sink : grp ){
						j++;
					
						if(i == j){
							continue;
						}
						
						// Calculate the probability that the seed badger would infect the sink badger over the course of its lifetime
						ngm[i][j] = calculateProbInfection(seed, sink, exposed2Infected, infected2Generalised, dispersalProbs, seasonsEffect, minAge, deathProbs, avgAges, infectionEffect, seedState, infectionProbs);
					}
				}
			}
		}
		
		// Calculate the Lead Eigenvalue - equivalent to the BASIC REPRODUCTION NUMBER
		Matrix ngmMatrix = new Matrix(ngm);
		EigenvalueDecomposition E = new EigenvalueDecomposition(ngmMatrix); 
		double[] lambdaRe = E.getRealEigenvalues();
		
		return ArrayMethods.max(lambdaRe);

	}
	
	public static double calculateProbInfection(Badger seed, Badger sink, double exposed2Infected,
			double infected2Generalised, double[] dispersalProbs, double seasonsEffect, int minAge,
			double[] deathProbs, double[] avgAges, double infectionEffect, char seedState, double[] infectionProbs){
		
		/**
		 * Calculating the Probability that if the Seed badger was infected, what is the probability that, over the
		 * remainder of its life, it will infect the Sink badger.
		 * 		- Time spent together in the same group
		 * 			* Death
		 * 				-> Age
		 * 				-> Infection Status
		 * 			* Dispersal
		 * 		- How long seed stays in each of the infection states
		 */
		
		double infectionProb = 0;
		
		// Are these badgers in the same group? - Only interested in within Group Infection at the moment
		if(seed.getGroupId() == sink.getGroupId()){
			
			// How long, on average, will the seed badger stay in each of the Infection States?
			double[] infectionStatePeriods = calculateInfectionStatePeriods(seedState, exposed2Infected, infected2Generalised);
			
			// How long, on average, will each badger stay in the group?
			double periodSeedInGroup = calculatePeriodInGroup(seed, dispersalProbs, seasonsEffect, minAge);
			double periodSinkInGroup = calculatePeriodInGroup(sink, dispersalProbs, seasonsEffect, minAge);
			
			// How long, on average, will each badger remain alive?
			double seedLifespan = calculateLifespan(seed, deathProbs, avgAges, 1, infectionStatePeriods, infectionEffect);
			double sinkLifespan = calculateLifespan(sink, deathProbs, avgAges, 0, infectionStatePeriods, infectionEffect);
			
			// How long, on average, are the two badgers going to be in contact in the same group?
			double[] periods = {periodSeedInGroup, periodSinkInGroup, seedLifespan, sinkLifespan};
			double contactPeriod = ArrayMethods.min(periods);
			
			// During the In-Contact period what states will the seed badger be in, and for how long?
			double[] inContactInfectionPeriods = calculateInContactInfectionStatePeriods(infectionStatePeriods, contactPeriod);
		
			// What is the probability that the Seed badger will infect the Sink badger?
			infectionProb = 1 - ( Math.pow(1 - infectionProbs[0], inContactInfectionPeriods[1]) * Math.pow(1 - infectionProbs[1], inContactInfectionPeriods[2]) );
		}
		
		return infectionProb;
	}
	
	public static double calculatePeriodInGroup(Badger badger, double[] dispersalProbs, double seasonsEffect, double minAge){
		
		/**
		 * Calculating the length of time the Badger is likely to spend in its current Group
		 * 		- Calculate Dispersal Probability for year taking into account the increase rate in Spring
		 * 		- Take into account the differences in dispersal between Sexes
		 * 		- Take into account the min Age for dispersal
		 */
	
		double periodInGroup = 0;
		double dispersalProb = 0;
		if(badger.getSex() == 'M'){
			
			// Calculate the cumulative probability of Dispersal for a single year
			dispersalProb = 1 - ( Math.pow(1 - dispersalProbs[0], 3) * ( 1 - dispersalProbs[0] * seasonsEffect) );
			
			// Calculate the average number of years the current individual is going to stay in its current group (1 / probability)
			periodInGroup = 1 / dispersalProb;
		}else{
			dispersalProb = 1 - ( Math.pow(1 - dispersalProbs[1], 3) * ( 1 - dispersalProbs[1] * seasonsEffect) );
			periodInGroup = 1 / dispersalProb;
		}
		
		// Account for years before Min age for dispersal if necessary
		if(badger.getAge() < minAge){
			periodInGroup += minAge - badger.getAge();
		}
		
		return periodInGroup;
	}

	public static double[] calculateInfectionStatePeriods(char seedState, double exposed2Infected, double infected2Generalised){
		
		/**
		 * Estimating the period of time the seed badger is likely to spend in each of infection states
		 * 		Taking average period that is equal 1 / probability -> divide by four to convert from seasons to years
		 * 		Note these values do not take into account death rates - they assume an infinite lifespan (death dealth with later)
		 */
		
		double[] statePeriods = new double[2];
		statePeriods[0] = (1 / exposed2Infected) / 4;
		statePeriods[1] = (1 / infected2Generalised) / 4;
		
		return statePeriods;
	}

	public static int calculateLifespan(Badger badger, double[] deathProbs, double[] avgAges, int seed,
			double[] infectionStatePeriods, double infectionEffect){
		
		/**
		 * Here we are asking given the age, sex, and infection status of an individual, on average, how much longer
		 * is this individual going to be alive?
		 * 
		 * 		Create a hypothetical population and ask when will half of this population have disappeared?
		 * 			Needs to be done in this fashion because the probability of death changes as a function of infection
		 * 			status and age.
		 */
		
		int age = badger.getAge();
		
		// Is the badger Female?
		double deathRate = deathProbs[0];
		double avgAge = avgAges[0];
		if(badger.getSex() == 'F'){
			deathRate = deathProbs[0];
			avgAge = avgAges[0];
		}
		
		// Create a hypothetical population, when will half be removed?
		double percentRemaining = 100;
		int years = 0;
		double deathProb = 0;
		while(percentRemaining > 50){
			
			/**
			 *  Note with each iteration investigating shifting along by Years - if badger aged 4 then will examine 4,5,6,7,8,9,10... 
			 *  until find when half would have died.
			 */
			
			if(years + age < 1){
				// Probability of death is much higher for cubs
				deathProb = 1 - Math.pow((1 - deathProbs[2]), 4);
				
			}else if(years < infectionStatePeriods[0] + infectionStatePeriods[1] || seed == 0){ 
				// Probability of death is unaffected by the badger being in the exposed or infected states OR if not seed (i.e. not infected)
				deathProb = 1 - Math.pow(1 - deathRate, (age + years)/avgAge);
				
			}else{
				// In the Generalised state the probability of death increases by some factor
				deathProb = 1 - Math.pow(1 - deathRate, ((age + years)/avgAge) * infectionEffect);
			}
			
			// Remove from the hypothetical population the number of individuals likely to die at the current age
			percentRemaining *= (1 - deathProb);
			
			// Move to the Next Age bracket
			years++;
		}

		return years;
	}
	
	public static double[] calculateInContactInfectionStatePeriods(double[] infectionStatePeriods, double contactPeriod){
		
		/**
		 * What states is the seed badger likely to pass through whilst in contact with the sink badger?
		 * 		
		 * 		  E	  I 			   G
		 * 		|--|-----|--------------------------|
		 * 		  2   5	  			   25	
		 * 		|~~!~~~~~!~~|
		 * 		  2   5   2
		 * 		IN CONTACT PERIOD
		 */
		
		double[] infectionPeriods = new double[3];
		infectionPeriods[0] = infectionStatePeriods[0];
		
		// Does the Exposed Period span the Contact Period?
		if(infectionPeriods[0] >= contactPeriod){ 
			infectionPeriods[0] = contactPeriod;
						
		}else{ // Contact Period must contain Exposed Period and at least part of the Infected Period
			infectionPeriods[1] = infectionStatePeriods[1];
			
			// Does the Infected Period + the Exposed Period span the contact period?
			if(infectionPeriods[1] >= contactPeriod - infectionPeriods[0]){
				infectionPeriods[1] = contactPeriod - infectionPeriods[0];
								
			}else{// Contact Period must contain Exposed, Infected and at least part of the Generalised Period
				infectionPeriods[2] = contactPeriod - (infectionPeriods[1] + infectionPeriods[0]);
				// Note that the Generalised state is the final Infection state
			}
		}
		
		return infectionPeriods;
	}
}
