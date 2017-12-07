package interSpeciesTransmission;

import java.io.BufferedWriter;
import java.io.IOException;

import methods.WriteToFile;

public class Test {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		// Initialise Simulation parameters
		int popSize = 100;
		int nSimulations = 1;
		
		// Sampling parameters
		double proportion = 0.97;
		double[] samplingStateBiases = {1,1};
		String[] samplingTemporalBias = {"early", "late"}; // "early", "late", "none"
		
		// Initialise Infection parameters
		int nInfectionStatuses = 2;
		double[] infectiousness = {0, 0.01};
		
		// Initialise state parameters
		int[] types = {0, 1};
		double[] typeWeights = {1, 1};
		
		// Open an output file
		String outputFile = "C:/Users/Joseph Crisp/Desktop/testModel.txt";
		BufferedWriter output = WriteToFile.openFile(outputFile, false);
		
		// Print output header
		Simulation.writeHeaderIntoOutput(output, "Infectiousness", types);
		
		// Run a simulation
		Simulation.runSimulations(output, nSimulations, popSize, types, typeWeights, nInfectionStatuses, infectiousness, proportion, samplingStateBiases, 0, samplingTemporalBias);
		
		WriteToFile.close(output);
	}
	
}
