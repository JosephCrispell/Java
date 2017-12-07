package contactNetworks;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;

import methods.ArrayMethods;
import methods.MatrixMethods;
import methods.WriteToFile;
import filterSensitivity.DistanceMatrixMethods;

public class SpatialDiffusion {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		// Parameters
		int popSize = 200;
		int noGroups = 20;
		int groupSize = popSize / noGroups;
		double[] infectiousness = 	{ 0, 0.001,  0};
		double[] transitions = 		{ 0, 0.0015, 0};
		double[] mutationRates =	{ 0, 0.5,  0};
		int[] gridSize = 			{ 250, 250};
		double recordThreshold = 0.4;
				
		int simulationLength = 200000;
		double samplingEfficiency = 1;
				
		int distanceCap = 9999;
		int trace = 0;
		
		// Output File
		String outFile = "C:/Users/Joseph Crisp/Desktop/spatialDiffusionEstimates.txt";
		BufferedWriter bWriter = WriteToFile.openFile(outFile, false);
						
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
			
			System.out.println(results.getSampledIndividuals().length);
			
			// R0
			double averageDistance = DistanceMatrixMethods.findAverageDistance(groupDistanceMatrix);
			double r0 = EpidemiologicalModelFitSimulation.calculateBasicReproductionNumber(infectiousness, transitions, averageDistance, popSize, groupSize);
			System.out.println(r0);
			
			// Record the Results of the Simulation
			if(results.getSampledIndividuals().length >= recordThreshold*popSize){
						
				double[][] diffusionEstimates = estimateSpatialDiffusion(results, mutationRates);
				
				WriteToFile.writeLn(bWriter, "SimulationEstimate\tGeneticEstimate");
				
				for(int j = 0; j < diffusionEstimates[1].length; j++){
					WriteToFile.writeLn(bWriter, diffusionEstimates[0][j] + "\t" + diffusionEstimates[1][j]);
				}
				
				x++;
			}
		}
		
		WriteToFile.close(bWriter);
	}
	
	public static double[][] estimateSpatialDiffusion(Results results, double[] mutationRates) throws IOException{
		
		// NOTE written assuming S I R model used !!!!!!!!!!!!!
		
		String outFile = "C:/Users/Joseph Crisp/Desktop/spatialDiffusionInconsistencies.txt";
		BufferedWriter bWriter = WriteToFile.openFile(outFile, false);
		
		// Retrieve the Sample Individuals 
		Individual[] sampled = results.getSampledIndividuals();
		
		// Initialise Table to store the diffusion rate Estimates: SimulationEstimate	GeneticEstimate
		double noComparisons = (sampled.length * (sampled.length - 1)) / 2;
		double[][] diffusionEstimates = new double[2][(int) noComparisons];
		
		double[][] geneticDistances = results.getGeneticDistanceMatrix();
		double[][] spatialDistances = results.getSpatialDistanceMatrix();
		double[][] temporalDistances = results.getTemporalDistanceMatrix();
		
		int pos = -1;
		for(int i = 0; i < sampled.length; i++){
			for(int j = 0; j < sampled.length; j++){
				
				// Only make the comparison once and avoid diagonal
				if(i >= j){
					continue;
				}
				pos++;
				
				// Estimate the Spatial Diffusion Rate from the Time of Infection for each Individual
				double[] transmissionRouteDistances = calculateTransmissionRouteSpatialAndTemporalDistances(sampled[i], sampled[j], spatialDistances, temporalDistances);
				diffusionEstimates[0][pos] = transmissionRouteDistances[0] / transmissionRouteDistances[1];
				
				// Check no Infinities were inserted
				if(diffusionEstimates[0][pos] > 999999){
					System.out.println("ERROR" + "\t" + i + "\t" + j + "\t" + transmissionRouteDistances[0] + "\t" + transmissionRouteDistances[1] + "\t" + sampled[i].getIndex() + "\t" + sampled[j].getIndex());
				}
				
				// Estimate the Spatial Diffusion Rate from the Genetic Data
				double evolutionaryTime = geneticDistances[sampled[i].getIndex()][sampled[j].getIndex()] / mutationRates[1];
				// Check evolutionary time isn't 0
				if(evolutionaryTime == 0){
					diffusionEstimates[1][pos] = spatialDistances[sampled[i].getIndex()][sampled[j].getIndex()];
				}else{
					diffusionEstimates[1][pos] = spatialDistances[sampled[i].getIndex()][sampled[j].getIndex()] / evolutionaryTime;
				}
				
				// EXAMINE WHERE GENETIC ESTIMATES ARE MILES OFF
				if(diffusionEstimates[1][pos] > 0.5){
					
					// Mutation Events?
					WriteToFile.writeLn(bWriter, "Start");
					WriteToFile.writeLn(bWriter, "Comparing: \t" + sampled[i].getId() + "\t" + sampled[j].getId());
					WriteToFile.writeLn(bWriter, "Comparing: \t" + sampled[i].getGroupIndex() + "\t" + sampled[j].getGroupIndex());
					if(sampled[i].getSource() != null && sampled[j].getSource() != null){
						WriteToFile.writeLn(bWriter, "i Source: \t" + sampled[i].getSource().getId() + "\t" + sampled[i].getSource().getGroupIndex());
						WriteToFile.writeLn(bWriter, "j Source: \t" + sampled[i].getSource().getId() + "\t" + sampled[j].getSource().getGroupIndex());
					}else{
						WriteToFile.writeLn(bWriter, "i Source: \t" + sampled[i].getSource());
						WriteToFile.writeLn(bWriter, "j Source: \t" + sampled[i].getSource());
					}
					WriteToFile.writeLn(bWriter, "i: Mutation Events \t" + Arrays.toString(sampled[i].getMutationEvents()));	
					WriteToFile.writeLn(bWriter, "j: Mutation Events \t" + Arrays.toString(sampled[j].getMutationEvents()));
					WriteToFile.writeLn(bWriter, "Genetic Distance: \t" + geneticDistances[sampled[i].getIndex()][sampled[j].getIndex()]);
					WriteToFile.writeLn(bWriter, "Spatial Distance: \t" + spatialDistances[sampled[i].getIndex()][sampled[j].getIndex()]);
					WriteToFile.writeLn(bWriter, "Transmission Route Distances: \t" + Arrays.toString(transmissionRouteDistances));
					WriteToFile.writeLn(bWriter, "Spatial Diffusion Estimates Simulation: \t" + diffusionEstimates[0][pos]);
					WriteToFile.writeLn(bWriter, "Spatial Diffusion Estimates Genetic: \t" + diffusionEstimates[1][pos]);
					
					WriteToFile.writeLn(bWriter, "End\n\n");
				}
				
			}
		}
		
		WriteToFile.close(bWriter);
		
		return diffusionEstimates;
	}
	
	public static double[] calculateTransmissionRouteSpatialAndTemporalDistances(Individual a, Individual b, double[][] spatialDistances,
			double[][] temporalDistances){
		/**
		 * In order to estimate the spatial diffusion rate between two individuals you need to trace the
		 * path from individual a to b back through the transmission tree summing the distances between individuals
		 * as you go.
		 * 			
		 * 		  B---D---I
		 * 		 /	  
		 * 		A ---E---F---H
		 * 			  	  \
		 * 			   	   G---J
		 * 							  
		 * Transmission Route Distance between I and J:
		 * 		[ d(I,D) + d(D,B) + d(B,A) ] + [ d(J,G) + d(G,F) + d(F,E) + d(E,A) ]
		 * 
		 * Transmission Route Distance between H and J:
		 * 		[ d(H,F) ] + [ d(J,G) + d(G,F) ]
		 * 
		 * Transmission Route Distance between H and E:
		 * 	Here E is involved in the transmission chain of H, therefore the distance is:
		 * 		[ d(H,F) + d(F,E) ]
		 * 	not:
		 * 		[ d(H,F) + d(F,E) + d(E,A) ] + [ d(E,A) ]
		 * 		
		 */
		
		// Initialise variable to store Transmission route spatial and temporal distances
		double[] distances = new double[2];
		
		// Follow path through transmission tree back to initial seed for each Individual
		Individual[] route4A = returnTransmissionRoute(a, new Individual[0]);
		Individual[] route4B = returnTransmissionRoute(b, new Individual[0]);
				
		// Is individual B involved in the Transmission chain to A?
		if(IndividualMethods.found(route4A, b) == 1){
			distances = calculateDistanceToSource(a, route4A, b, spatialDistances, temporalDistances);
			
		// Is individual A involved in the Transmission chain to B?
		}else if(IndividualMethods.found(route4B, a) == 1){
			distances = calculateDistanceToSource(b, route4B, a, spatialDistances, temporalDistances);
			
		// Otherwise find their common source
		}else{
			
			// Identify first common source
			Individual source = findFirstCommonSource(route4A, route4B);
					
			/**
			 *  Calculate the Distance back to the Common source for each Individual and combine to calculate the
			 *  spatial distance covered via the transmission route between Individual A and B.
			 */
			distances = calculateDistanceToSource(a, route4A, source, spatialDistances, temporalDistances);
			distances = ArrayMethods.add(distances, calculateDistanceToSource(b, route4B, source, spatialDistances, temporalDistances));
		}
				
		return distances;		
	}
	
	public static double[] calculateDistanceToSource(Individual individual, Individual[] route, Individual source,
			double[][] spatialDistances, double[][] temporalDistances){
		
		/**
		 * The passage of Transmission to any given individual is recorded as a list of internal sources. The 
		 * spatial distance covered in the transmission to anyone of these sources is the sum of the distances for 
		 * the transmission events that are within the path to that source.
		 * 
		 * A: {B,E,F,C}
		 * 
		 * A -> F = d(A,B) + d(B,E) + d(E,F) + d(F,C)
		 */
		
		double[] distances = new double[2];
		for(int i = 0; i < route.length; i++){
			
			if(route[i] != source){
				
				// Is it the first in the list? If so then take distance from the Individual
				if(i == 0){
					distances[0] += spatialDistances[individual.getIndex()][route[i].getIndex()];
					distances[1] += temporalDistances[individual.getIndex()][route[i].getIndex()];
				}else{
					distances[0] += spatialDistances[route[i - 1].getIndex()][route[i].getIndex()];
					distances[1] += temporalDistances[route[i - 1].getIndex()][route[i].getIndex()];
				}
				
			}else{
				
				// Is it the first in the list? If so then take distance from the Individual
				if(i == 0){
					distances[0] += spatialDistances[individual.getIndex()][route[i].getIndex()];
					distances[1] += temporalDistances[individual.getIndex()][route[i].getIndex()];
				}else{
					distances[0] += spatialDistances[route[i - 1].getIndex()][route[i].getIndex()];
					distances[1] += temporalDistances[route[i - 1].getIndex()][route[i].getIndex()];
				}
				
				// We have found the source so stop calculating distances
				break;
			}
		}
	
		return distances;
	}	
	
	public static Individual findFirstCommonSource(Individual[] route4A, Individual[] route4B){
		
		// Initialise Source
		Individual source = new Individual(-99);
		
		// Which is shortest path?
		int[] lengths = {route4A.length, route4B.length};
		int length = ArrayMethods.min(lengths);
		
		// If the Parent Node of the Terminal Nodes is the same then it is the MRCA
		if(route4A[0] == route4B[0]){
			source = route4A[0];
							
		}else{
		
			// Compare each internal source working back from the Seed (last added) to each Individual (first)
			for(int x = 1; x < length + 1; x++){

				/**
				 *  If the internal sources aren't the same then the previous internal source (back towards Seed)
				 *  is the First common Source.
				 */
				if(route4A[route4A.length - x] != route4B[route4B.length - x]){
					source = route4A[route4A.length - x + 1];
					break;
				}			
			}
		
			/**
			 *  If the no Common Source was identified then it is the Parent of the Terminal Node with the least
			 *  Internal Nodes. 
			 */
			if(source.getId() == -99){
				source = route4A[route4A.length - length];
			}
			
		}
		
		
		
		return source;
	}
	
	public static Individual[] returnTransmissionRoute(Individual individual, Individual[] route){
		/**
		 * Trace the path of an epidemic back from the current individual to the original seed
		 * 	The source of each infected individual is recorded
		 * 	The original seed will have no source -> null
		 */
		
		if(individual.getSource() != null){
			route = IndividualMethods.append(route, individual.getSource());
			route = returnTransmissionRoute(individual.getSource(), route);
		}
		
		return route;
	}
	
	public static double calculateMutationRateOverInfection(Individual a, double[] rates){
		
		/**
		 * Need to estimate the average mutation rate of the course of an individuals infection,
		 * but an individual can spend different times in different states that may have different 
		 * mutation rates associated with them.
		 * 					  S  	I  		G  		R
		 * timeInStates = 	{14, 	2, 		3, 		12};
		 * mutationRates =  {0,		0.1, 	0.3,	0 };
		 * 
		 * ratesExperience = {0.1, 0.1, 0.3, 0.3, 0.3}; -> average rate = 0.22 
		 */
		
		double[] mutationRates = new double[0]; 
		int[] timeEnteredStates = a.getTimeEnteredStates();
		
		for(int i = 0; i < rates.length; i++){
			
			// Skip states that have no associated mutation rate
			if(rates[i] == 0){
				continue;
			}
			
			// How long did the Individual spend in the current state?
			int timesteps = timeEnteredStates[i] - timeEnteredStates[i - 1]; // First State isn't infectious
			for(int x = 0; x < timesteps; x++){
				mutationRates = ArrayMethods.append(mutationRates, rates[i]);
			}
		}
		
		return ArrayMethods.mean(mutationRates);
		
	}
	

}
