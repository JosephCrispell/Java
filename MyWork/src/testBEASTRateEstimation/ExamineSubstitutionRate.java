package testBEASTRateEstimation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Random;

import org.uncommons.maths.random.PoissonGenerator;

import methods.ArrayMethods;
import methods.GeneralMethods;
import methods.MatrixMethods;
import methods.WriteToFile;

public class ExamineSubstitutionRate {
	public static void main(String[] args) throws IOException {
		
		//***** Set up Simulation *****
		
		// Set the path
		String path = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/TestingBEAST/ExamineSubstitutionRate/";
		
		// Set an array size limit
		Global.arraySizeLimit = 99;
		
		// Get the current date and time
		String date = GeneralMethods.getCurrentDate("dd-MM-yyyy");
		
		// Set the simulation parameters
		int simulationLength = 45;
		int popSize = 100;
		
		// SIR infection parameters
		double[] infectiousness = {0, 0.002, 0};
		double[] transitionRates = {0, 0.004, 0};
		int seedStatus = 1;
	
		// Sampling settings
		int[] startEnd = {20, 120};
		double samplingProportion = 0.05;
		
		// Set the mutation parameters
		int genomeSize = 4500000;
		double mutationRate = 0.01;
		double[] mutationRates = {0, mutationRate, 0};
		double nucleotideTransitionRate = 0.5;
		double[] nucleotideWeights = {0.25, 0.25, 0.25, 0.25};
		
		// Set the seed
		int seed = RunStateTransitionSimulations.generateSeeds(1)[0];
		
		// Run simulation
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
		
		// Create a file to store the simulation settings and result summary
		String settingsFile = path + "simulation_"+ date + "_settings.txt";
		
		// Open an output file to store the population status at each time step
		String surveyFile = path + "simulation_" + date + "_survey.txt";
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
	
			// Create the Reference Sequence
			Global.reference = SequenceMethods.generateRandomNucleotideSequence(nucleotideWeights, genomeSize, random);
			
			// Define the mutation events
			SequenceMethods.defineMutationEvents(nucleotideTransitionRate, random, 99);
			
			// Calculate the sampled substitution rate
			System.out.println("Estimating Substitution Rate");
			double[] substitutionRates = estimateSubstitutionRateForSampledPopulation(population, simulationLength, genomeSize);
			System.out.println("Rate (full) = " + substitutionRates[0]);
			System.out.println("Rate (sampled) = " + substitutionRates[1]);
			
			// Print out the simulation settings
			printSettings(settingsFile, simulationLength, popSize, infectiousness, genomeSize, mutationRates, transitionRates, seedStatus, startEnd, samplingProportion, nucleotideTransitionRate, population, substitutionRates, seed);
			
			printMutationEventInfo();
		}else{
			System.out.println("Not enough individuals were sampled: " + population.getNumberSampled());
		}
	}
	
	public static void printMutationEventInfo(){
		
		for(int i = 0; i < Global.mutationEventNo; i++){
			
			System.out.println(i + "\t" + Global.who[i] + "\t" + Global.when[i]);
		}
	}
	
	public static double[] estimateSubstitutionRateForSampledPopulation(Population population,
			int simulationLength, int genomeSize) throws IOException{

		// Build the entire adjacency matrix
		int[][] adjacencyMatrix = Methods.buildAdjacencyMatrix(population, "none");
		
		// Estimate the substitution rate on the full transmission tree
		double[] rates = new double[2];
		rates[0] = SequenceMethods.estimateSubstitutionRateOnSampledTransmissionTree(adjacencyMatrix, adjacencyMatrix, population, simulationLength, genomeSize);
		
		// Copy the entire adjacency matrix
		int[][] sampledAdjacencyMatrix = MatrixMethods.copy(adjacencyMatrix);
		
		// Build the sampled adjacency matrix
		SequenceMethods.removeUnSampledIndividualsWhoInfectedNoOne(population, sampledAdjacencyMatrix);
		SequenceMethods.iterativelyRemoveUnsampledLeaves(sampledAdjacencyMatrix, population, null);
		SequenceMethods.removeUnsampledIndividualsOnPathToSampledIndividuals(sampledAdjacencyMatrix, population);
		SequenceMethods.removeRootIfNotInvolved(sampledAdjacencyMatrix, population);
		
		// Estimate the substitution rate for the sampled population
		rates[1] = SequenceMethods.estimateSubstitutionRateOnSampledTransmissionTree(sampledAdjacencyMatrix, adjacencyMatrix, population, simulationLength, genomeSize);
		return rates;
	}
	
	public static void printSettings(String fileName, int simulationLength, int popSize, 
			double[] infectiousness, int genomeSize, double[] mutationRates, double[] transitionRates, int seedStatus,
			int[] startEnd, double prop, double nucleotideTransitionRate, Population population,
			double[] substitutionRateEstimates, int seed) throws IOException{
		
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
		WriteToFile.write(bWriter, "Substitution rate estimated on full transmission tree = " + substitutionRateEstimates[0] + "\t(" + (substitutionRateEstimates[0] / genomeSize) + ")\n");
		WriteToFile.write(bWriter, "Substitution rate estimated on sampled transmission tree = " + substitutionRateEstimates[1] + "\t(" + (substitutionRateEstimates[1] / genomeSize) + ")\n");
		WriteToFile.write(bWriter, population.getNumberSampled() + " individuals were sampled.\n");
		WriteToFile.write(bWriter, "Constant Site Counts: A, C, G, T = " + ArrayMethods.toString(Global.constantSiteCounts, ", ") + "\n");
			
		// Close the output file
		WriteToFile.close(bWriter);		
	}
}
