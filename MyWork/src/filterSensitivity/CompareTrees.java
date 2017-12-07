package filterSensitivity;

import java.io.IOException;

import methods.ArrayMethods;
import methods.WriteToFile;

public class CompareTrees {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		// Command Line Structure: java -jar jarFile treeDirectory NNfile NNHPfile HNNfile
		
		// Find the Distance Matrix Files
		String[] files = WriteToFile.findFilesInDirectory(args[0], "distanceMatrix(.*)");
		
		// Compare the Distance Matrices
		ConsistencyMeasures consistency = compareDistanceMatrices(files);
		
		// Store the Resulting Consistency Matrices
		DistanceMatrixMethods.print(consistency.getNearestNeighbourConsistency(), args[1]);
		DistanceMatrixMethods.print(consistency.getHerdProportionsConsistency(), args[2]);
		DistanceMatrixMethods.print(consistency.getHerdNearestNeighbourConsistency(), args[3]);
		
	}
	public static ConsistencyMeasures compareDistanceMatrices(String[] files) throws IOException{

		/**
		 *  Comparing each of the output Distance Matrices to one another
		 *  
		 *  All of Three Consistency Measures are used:
		 *  	- Nearest Neighbour
		 *  	- Nearest Neighbour Herd Proportions
		 *  	- Herd Nearest Neighbour
		 */
		
		// Initialise Consistency Matrix Information
		double[][] NNconsistencyMatrix = new double[files.length][files.length];
		double[][] NNHPconsistencyMatrix = new double[files.length][files.length];
		double[][] HNNconsistencyMatrix = new double[files.length][files.length];
		String[] filterCombinations = new String[files.length];
		String[] parts = new String[6];
		
		
		// Build the Consistency Matrix
		for(int i = 0; i < files.length; i++){
			
			/**
			 *  Create the Sample Names from File Names - These contain the Filter Combination
			 *  	distanceMatrix_Depth_HQDepth_MQ_HZGTY_.txt
			 */
			parts = files[i].split("_");
			filterCombinations[i] = parts[1] + "_" + parts[2] + "_" + parts[3] + "_" + parts[4];
			
			for(int j = 0; j < files.length; j++){
				
				if(i != j && NNconsistencyMatrix[i][j] == 0){ // Note only need to check one of the distance Matrices
					
					// Retrieve the Distance Matrix Information
					DistanceMatrix distanceMatrixInfo1 = DistanceMatrixMethods.readInDistanceMatrix(files[i]);
					DistanceMatrix distanceMatrixInfo2 = DistanceMatrixMethods.readInDistanceMatrix(files[j]);
					
					double[] NNconsistency = DistanceMatrixMethods.compareNearestNeighbours(distanceMatrixInfo1, distanceMatrixInfo2);
					double meanNNConsistency = ArrayMethods.mean(NNconsistency);
					
					double[] NNHPconsistency = DistanceMatrixMethods.compareNearestNeighbourGroupProportions(distanceMatrixInfo1, distanceMatrixInfo2, 'H');
					double meanNNHPConsistency = ArrayMethods.mean(NNHPconsistency);
					
					double[] HNNconsistency = DistanceMatrixMethods.compareGroupNearestNeighbourDistributions(distanceMatrixInfo1, distanceMatrixInfo2, 'H');
					double meanHNNConsistency = ArrayMethods.mean(HNNconsistency);
					
					// Store the Results
					NNconsistencyMatrix[i][j] = meanNNConsistency;
					NNconsistencyMatrix[j][i] = meanNNConsistency;
					
					NNHPconsistencyMatrix[i][j] = meanNNHPConsistency;
					NNHPconsistencyMatrix[j][i] = meanNNHPConsistency;
					
					HNNconsistencyMatrix[i][j] = meanHNNConsistency;
					HNNconsistencyMatrix[j][i] = meanHNNConsistency;
					
					
				}
			}
		}
		
		// Store each Distance Matrix
		DistanceMatrix NNinfo = new DistanceMatrix(filterCombinations, NNconsistencyMatrix);
		DistanceMatrix NNHPinfo = new DistanceMatrix(filterCombinations, NNHPconsistencyMatrix);
		DistanceMatrix HNNinfo = new DistanceMatrix(filterCombinations, HNNconsistencyMatrix);
		
		return new ConsistencyMeasures(NNinfo, NNHPinfo, HNNinfo);
		
	}
}
