package badgerPopulation;

import java.io.IOException;
import java.util.Random;

import org.uncommons.maths.random.PoissonGenerator;

import methods.GeneralMethods;
import testRateEstimation.Methods;

public class Simulation {

	public static void main(String[] args) throws IOException {
		
		/**
		 * Badger Population Model
		 */
		
		// Initialise a random number generator
		int seed = 132425366;
		System.out.println("Seed = " + seed);
		Random random = GeneralMethods.startRandomNumberGenerator(seed);
				
		// Initialise the parameters
		Parameters parameters = new Parameters();

		// Initialise a poisson distributions
		PoissonGenerator[] mutationPoissons = Testing.generatePoissonDistributionsAroundMutationRates(parameters.getMutationRatesPerSeason(), random);
		PoissonGenerator litterSizePoisson = new PoissonGenerator(parameters.getAvgLitterSize(), random);
		PoissonGenerator agePoisson = new PoissonGenerator(parameters.getMeanAge(), random);
		PoissonGenerator nImmigrantsPoisson = new PoissonGenerator(parameters.getAvgNoImmigrants(), random);
		PoissonGenerator grpSizePoisson = new PoissonGenerator(parameters.getMeanGroupSize(), random);
				
		// Create Initial Population
		Population population = new Population(parameters.getNoGroups(), agePoisson, grpSizePoisson,
				parameters.getProbMale(), random, parameters.getGridDimensions());
		
		// Initialise Variables
		int seasonCount = 0;
		
		// YEARS
		for( int year = 0; year < parameters.getSimulationLength(); year++){
			
			System.out.println(year + "\t" + population.getPopSize());
			
			// Stopping condition
			if(population.getPopSize() == 0){
				System.out.println("Simulation stopped. Population size = 0.");
				break;
			}
			
			// SEED INFECTION
			if(year == parameters.getSeedYear()){
				population.seedInfection(parameters.getNoSeeds(), parameters.getSeedStatus(), random);
			}
			
			/**
			 *  Spring -> Summer -> Autumn -> Winter
			 *  0		  1			2		  3
			 */
			
			// SEASONS 									Spring -> Summer -> Autumn -> Winter
			for( int season = 0; season < 4; season++){
				
				// Keep count of the number of seasons that have passed
				seasonCount++;
				
				// SPRING
				if(season == 0){
						
					// BIRTH
					population.birth(seasonCount, parameters.getProbMale(), parameters.getPseudoVerticalTransmission(),
							litterSizePoisson, mutationPoissons, random);
						
					// BREEDING
					population.breeding(parameters.getMinBreedingAge());
				}
					
				// INFECTION
				population.withinGroupInfection(season, seasonCount, parameters.getInfectionProbs(),
						mutationPoissons, random);
					
				// PROGRESSION
				population.progression(parameters.getProgressionProbs(), random);
							
				// DISPERSAL
				population.dispersal(parameters.getDispersalProbs(), parameters.getMinDispersalAge(), season,
						parameters.getSeasonalEffects(), parameters.getClosed(), random);
					
				// DEATH
				population.death(parameters.getDeathProbs(), parameters.getCarryingCapacity(), random,
						parameters.getInfectionEffects());
					
				// IMMIGRATION
				population.immigration(parameters.getMinDispersalAge(), parameters.getProbMale(),
						nImmigrantsPoisson, agePoisson, parameters.getClosed(), random);

			}
			
			// AGING
			population.aging();
			
		}
	}
}
