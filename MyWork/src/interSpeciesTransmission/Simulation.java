package interSpeciesTransmission;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Random;

import methods.ArrayMethods;
import methods.MatrixMethods;
import methods.WriteToFile;

import org.apache.commons.math3.random.MersenneTwister;
import org.uncommons.maths.random.MersenneTwisterRNG;
import org.uncommons.maths.random.PoissonGenerator;

public class Simulation {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		// BROKEN - not building sampled transmission tree correctly!!!!!
		// See testRateEstimation for fixed methods!!!
		
		// Initialise Simulation parameters
		int popSize = 1000;
		int nSimulations = 1000;
		
		// Sampling parameters
		double proportion = 0.2;
		double[] samplingTypeProportions = {0.5,0.5}; // Must sum to 1
		String[] samplingTemporalBias = {"none", "none"}; // "early", "late", "none"
		
		// Initialise Infection parameters
		int nInfectionStatuses = 2;
		double[] infectiousness = {0, 0.001};
		
		// Initialise state parameters
		int[] types = {0, 1};
		double[] typeWeights = {0.7, 0.3};
		
		// Open an output file
		String outputFile = "C:/Users/Joseph Crisp/Desktop/simulationSet_SamplingProportion-InitialStateBias_1000-1000-0.001_0.7-0.3.txt";
		BufferedWriter output = WriteToFile.openFile(outputFile, false);
		
		// Print output header
		writeHeaderIntoOutput(output, "SamplingProportion\tnKept", types);
		
		// Explore a parameter set
		for(double parameter = 1; parameter > 0; parameter = parameter - 0.01){
		
			// Round the bias parameter to 2 decimal places
			parameter = Math.round(parameter * 10000000.0)/10000000.0;

			//samplingTypeProportions[0] = parameter;
			//samplingTypeProportions[1] = 1 - parameter;
			
			//typeWeights[0] = parameter;
			//typeWeights[1] = 1 - parameter;
			
			proportion = parameter;
			
			System.out.println("Running simulations for a sampling proportion of: " + parameter);
			
			runSimulations(output, nSimulations, popSize, types, typeWeights, nInfectionStatuses, infectiousness, proportion, samplingTypeProportions, parameter, samplingTemporalBias);
		}
		
		// Close the output file
		WriteToFile.close(output);

	}
	
	public static void writeHeaderIntoOutput(BufferedWriter output, String parameterChanging, int[] states) throws IOException{
		
		WriteToFile.write(output, parameterChanging);
		for(int i = 0; i < states.length; i++){
			for(int j = 0; j < states.length; j++){
				
				WriteToFile.write(output, "\t" + "Real_" + i + ":" + j + "\t" + "Estimated_" + i + ":" + j);
			}
		}
		WriteToFile.write(output, "\n");
		
	}
	
	public static void runSimulations(BufferedWriter output,int nSimulations, int popSize, int[] types,
			double[] typeWeights, int nInfectionStatuses, double[] infectiousness, double proportion,
			double[] samplingStateBiases, double parameterChanging, String[] samplingTemporalBias) throws IOException{
		
		// Initialise a random number generator
		Random random = new MersenneTwisterRNG();
		
		// Find the first infectious state
		int firstInfectiousState = Methods.findFirstInfectiousStatus(infectiousness);
		
		// Run n simulations
		for(int x = 1; x <= nSimulations; x++){
			
			// Initialise the population
			Global.populationState = new int[nInfectionStatuses];
			Global.populationState[0] = popSize;
			Global.adjacencyMatrix = new int[popSize][popSize];
			Individual[] population = Methods.intialisePopulation(popSize, types, typeWeights, nInfectionStatuses, random);
			
			// Seed the infection
			Methods.seedInfection(population, firstInfectiousState, random, 0);
			
			// Begin simulation
			int timeStep = 0;
			while(Global.populationState[0] > 0){
				timeStep++;
			
				// Infection
				Methods.infection(population, infectiousness, random, timeStep);
			}
			
			// Calculate state transition rate matrix
			double[][] typeTransitionRateMatrix = Methods.calculateTypeTransitionMatrix(Global.adjacencyMatrix, population, types.length, firstInfectiousState);
					
			// Sample the infected individuals in the population
			Hashtable<Integer, Integer> sampled = Methods.sampleInfectedIndividuals(population, proportion, samplingStateBiases, samplingTemporalBias, timeStep, firstInfectiousState, random);
			
			// Sample the transmission tree
			int[][] sampledAdjacencyMatrix = MatrixMethods.copy(Global.adjacencyMatrix);
			sampledAdjacencyMatrix = Methods.removeUnsampledLeaves(sampledAdjacencyMatrix, sampled);
			//sampledAdjacencyMatrix = Methods.removeUnsampledIndividualsWithDegreeOne(sampledAdjacencyMatrix, sampled);
			
			// Count how many individuals remain in the tree
			int nRemaining = MatrixMethods.sum(sampledAdjacencyMatrix) + 1;
			
			// Calculate the sampled state transition matrix
			double[][] sampledTypeTransitionRateMatrix = Methods.calculateTypeTransitionMatrix(sampledAdjacencyMatrix, population, types.length, firstInfectiousState);
		
			// Print out the real vs. estimate state transitions
			printTypeTransitionRateEstimations(typeTransitionRateMatrix, sampledTypeTransitionRateMatrix, output, parameterChanging, nRemaining);
			
			// Keep track of how many simulations have been completed
			if(x%100 == 0){
				System.out.print(".");
			}	
		}
		System.out.println();
	}
	
	public static void printTypeTransitionRateEstimations(double[][] real, double[][] estimated,
			BufferedWriter file, double parameterChanging, int n) throws IOException{
		
		WriteToFile.write(file, parameterChanging + "\t" + n);
		for(int i = 0; i < real.length; i++){
			for(int j = 0; j < real[0].length; j++){
				
				WriteToFile.write(file, "\t" + real[i][j] + "\t" + estimated[i][j]);
			}
		}
		WriteToFile.write(file, "\n");
	}
}

