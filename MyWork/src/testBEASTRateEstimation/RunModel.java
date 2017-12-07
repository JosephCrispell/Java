package testBEASTRateEstimation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Random;

import methods.ArrayMethods;
import methods.GeneralMethods;
import methods.MatrixMethods;
import methods.WriteToFile;

import org.uncommons.maths.random.PoissonGenerator;

public class RunModel {

	/**
	 * @param args
	 * @throws IOException 
	 */
public static void main(String[] args) throws IOException {
		
		//***** Set up Simulation *****
		
		// Set the path
		String homeDirectory = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/TestingBEAST/SamplingWindow/Window_110-120/";
		
		// Set an array size limit
		Global.arraySizeLimit = 99;
		
		// Get the current date and time
		String date = GeneralMethods.getCurrentDate("dd-MM-yyyy");
		
		// Set the simulation parameters
		int simulationLength = 120;
		int popSize = 1000;
		
		// SIR infection parameters
		double[] infectiousness = {0, 0.1, 0};
		double[] transitionRates = {0, 0.2, 0};
		int seedStatus = 1;
		//double forceOfInfection;
	
		// Sampling settings
		int[] startEnd = {110, 120};
		double samplingProportion = 0.01;
		
		// BEAST settings
		int chainLength = 10000000;
		
		// Set the mutation parameters
		int genomeSize = 4500000;
		double mutationRate = 0.5;
		double[] mutationRates = {0, mutationRate, 0};
		double nucleotideTransitionRate = 0.5;
		double[] nucleotideWeights = {0.25, 0.25, 0.25, 0.25};
		
		// Set the number of replicates
		int nReplicates = 10;
		
		// Set the seeds for each replicate
		//int[] seeds = RunStateTransitionSimulations.generateSeeds(nReplicates);
		int[] seeds = {3736647, 8472032, 8926317, 5318945, 7687231, 9996354, 4571885, 3881955, 231447, 4357242};
		int seed;
		
		// Run the replicates of the simulation model
		for(int runId = 1; runId <= nReplicates; runId++){
			
			System.out.println("\n***************************************************************");
			System.out.println("\t\tBeginning Simulation: " + runId + " of " + nReplicates);
			System.out.println("***************************************************************\n");
			
			// Initialise a random number generator with a defined seed
			seed = seeds[runId - 1];
			System.out.println("Seed = " + seed);
			Random random = GeneralMethods.startRandomNumberGenerator(seed);
			
			// Initialise a poisson distribution for the mutation process
			PoissonGenerator[] mutationPoissons = Methods.generatePoissonDistributionsAroundMutationRates(mutationRates, random);
			
			// Reset the Global variables
			Global.mutationEventNo = -1;
						
			// Initialise the population
			Population population = new Population(popSize, infectiousness.length, 99999);
			
			// Seed the infection
			Methods.seedInfection(population, seedStatus, random, 0);
						
			// BEAST settings
			String filePrefix = "run_" + runId + "_" + date + "_JC_Strict_Constant_" + ArrayMethods.toString(startEnd, "-");
			
			// Set the path
			String path = homeDirectory + filePrefix;
			
			// Make a new directory
			GeneralMethods.makeDirectory(path);			
			path = path + "/";
			
			// Create a file to store the simulation settings and result summary
			String settingsFile = path + "simulation_" + runId + "_"+ date + "_settings.txt";
			
			// Open an output file to store the population status at each time step
			String surveyFile = path + "simulation_" + runId + "_" + date + "_survey.txt";
			BufferedWriter survey = WriteToFile.openFile(surveyFile, false);
			Methods.printHeaderForSurveyFile(survey, infectiousness.length);

			// Print the current population status
			Methods.printPopulationStatus(0, population, survey);
			
			
			//***** Begin the simulation *****
			for(int timeStep = 1; timeStep < simulationLength; timeStep++){
				
				// INFECTION
				Methods.infection(population, infectiousness, random, timeStep, mutationPoissons);

				// SAMPLING
				if(timeStep >= startEnd[0] && timeStep <= startEnd[1]){
					Methods.sample(population, timeStep, samplingProportion, infectiousness, random, mutationRates, mutationPoissons);
				}
						
				// RECOVERY
				Methods.recovery(population, transitionRates, timeStep, random, mutationPoissons);
						
				// BIRTH
				Methods.birth(population, 2, timeStep, popSize);

				// SURVEY
				Methods.printPopulationStatus(timeStep, population, survey);
						
				// STOPPING CONDITION
				if(population.getNumberOfIndividualsInCompartment(1) == 0){
					System.out.println("No infectious individuals left in the population. Time step = " + timeStep + ".");
					break;
				}
			}
			
			// Check for any additional mutation events
			Methods.updateMutationEvents(population, mutationRates, mutationPoissons, simulationLength);
			
			// Close the Survey File
			WriteToFile.close(survey);
			
			System.out.println("Finished Simulation.\n");
			System.out.println(Global.mutationEventNo + " Mutation Events Occurred.");
			System.out.println("Final Population Size = " + population.getSize());
			System.out.println("Sampled: " + population.getNumberSampled());
			
			// Check that some individuals were sampled
			if(population.getNumberSampled() > 2){
			
			
				//***** Analyse the simulation results *****

				// Get a list of the sampled individuals
				int[] sampledIds = population.getIdsOfSampledIndividuals();
				
				// Create the Reference Sequence
				Global.reference = SequenceMethods.generateRandomNucleotideSequence(nucleotideWeights, genomeSize, random);
				
				// Define the mutation events
				SequenceMethods.defineMutationEvents(nucleotideTransitionRate, random, 99);
				
				// Calculate the sampled substitution rate
				System.out.println("Estimating Substitution Rate");
				double substitutionRateForSampled = SequenceMethods.estimateSubstitutionRateForSampledPopulation(population, simulationLength, genomeSize);
				System.out.println("Rate = " + substitutionRateForSampled);
				
				// Find the informative sites on the genome
				System.out.println("Finding Informative Sites");
				SequenceMethods.findInformativeMutationEventSites(population, sampledIds);
				int[] sortedInformativeSites = SequenceMethods.getOrderedListOfInformativeSites();
				System.out.println("Found " + sortedInformativeSites.length + " sites.");
				
				// Write the BEAST XML file
				int[] constantSiteCounts = SequenceMethods.writeBeastXML(filePrefix, population, sortedInformativeSites, chainLength, path, genomeSize);
				
				// Print out the simulation settings
				SequenceMethods.printSettings(settingsFile, simulationLength, popSize, infectiousness, genomeSize, mutationRates, transitionRates, startEnd, samplingProportion, nucleotideTransitionRate, population, sortedInformativeSites.length, substitutionRateForSampled, seed, constantSiteCounts);
				
				// Examine the distribution of mutation window sizes
				String windowSizeFile = path + "simulation_" + runId + "_"+ date + "_windowSizes.txt";
				SequenceMethods.calculateMutationWindowSizes(population, windowSizeFile);
								
			}else{
				System.out.println("Not enough individuals were sampled: " + population.getNumberSampled());
			}		

		
		}
	}
}
