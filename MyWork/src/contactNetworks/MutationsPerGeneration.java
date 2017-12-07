package contactNetworks;

import java.io.BufferedWriter;
import java.io.IOException;

import methods.WriteToFile;

import filterSensitivity.DistanceMatrixMethods;

public class MutationsPerGeneration {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		/** 
		 * Run an SIR Model 
		 * 
		 * 	Simple infection ran on a simple network. Mutation events are recorded in each individual's pathogen. All 
		 *  individuals (that have come into contact with the disease) are sampled at the end of the simulation and Genetic Distance Matrix is generated according
		 *  to the mutation events. Spatial Distance is calculated using the Network represented as an adjacency matrix.
		 *  
		 *   - No birth, death, migration
		 *   - Contact Network is static
		 *   - Infection spreads only through contact
		 *   
		 *  Aim is to establish a correlation between the genetic distance between individuals and their spatial
		 *  association. Pearson's Correlation coefficient is used to provide a correlation metric for genetic vs. 
		 *  spatial distance.
		 */
		
		// Parameters
		int popSize = 400;
		int noGroups = 20;
		int groupSize = popSize / noGroups;
		double[] infectiousness = 	{ 0, 0.005,  0};
		double[] transitions = 		{ 0, 0.02, 0};
		double[] mutationRates =	{ 0, 0.05,  0};
		int[] gridSize = 			{ 500, 500};
		double recordThreshold = 0.5;
					
		int simulationLength = 200000;
		double samplingEfficiency = 1;
						
		int distanceCap = 9999;
		int trace = 0;
		
		// Open Output Files
		String mutationsPerGenerationFileName = "C:/Users/Joseph Crisp/Desktop/mutationsPerGeneration.txt";
		
		// Run Simulations
		int noSimulations = 1;
		int x = 0;
		while(x < noSimulations){
			
			// Create Initial Population
			Individual[] population = EpidemiologicalModelFitSimulation.createInitialPopulation(popSize, transitions);
			
			// Assign Individuals to Groups
			Individual[][] groups = EpidemiologicalModelFitSimulation.createGroupedPopulation(population, noGroups);
			
			// Randomly Place Groups on Grid
			double[][] groupDistanceMatrix = EpidemiologicalModelFitSimulation.createGroupDistanceMatrix(noGroups, gridSize[0], gridSize[1]);
			
			// Run the Simulation
			Results results = EpidemiologicalModelFitSimulation.runSimulationModelGroupSpatial(transitions, infectiousness, simulationLength, 1, mutationRates, simulationLength, samplingEfficiency, groups, groupDistanceMatrix, distanceCap, trace, popSize);
			
			double averageDistance = DistanceMatrixMethods.findAverageDistance(groupDistanceMatrix);
			double r0 = EpidemiologicalModelFitSimulation.calculateBasicReproductionNumber(infectiousness, transitions, averageDistance, popSize, groupSize);
			System.out.println(results.getSampledIndividuals().length + "\t" + r0);
			
			// Record the Results of the Simulation
			if(results.getSampledIndividuals().length >= recordThreshold*popSize){
				
				// Store the Number of Mutations per Generation Distribution
				printMutationsPerGenerationInfo(results.getMutationsVsGenerationTime(), mutationsPerGenerationFileName);
				x++;
			}
		}
	}
	
	public static void printMutationsPerGenerationInfo(int[][] table, String fileName) throws IOException{
		/**
		 *  Table structure: No.Mutations	GenerationTime	Timestep	Within?
		 *  
		 *  For recording the output from multiple simulations will store only the number of mutations per generation
		 *  	No. mutations per Generation = No. Mutations / GenerationTime
		 */
		
		// Open File for Writing to
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Print Header
		WriteToFile.writeLn(bWriter, "No.Mutations\tGenerationTime\tTimestep\tMutationsPerGeneration\tWithinGroup");
				
		for(int i = 0; i < table.length; i++){
			
			// Avoid empty rows in table
			if(table[i][2] != 0){
				
				WriteToFile.writeLn(bWriter, table[i][0] + "\t" + table[i][1] + "\t" + table[i][2] + "\t" + ((double) table[i][0] / (double) table[i][1]) + "\t" + table[i][3]);
				
			}
		}
		WriteToFile.write(bWriter, "\n");
		WriteToFile.close(bWriter);
	}

}
