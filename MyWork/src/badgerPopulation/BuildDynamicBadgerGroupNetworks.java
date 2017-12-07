package badgerPopulation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Hashtable;

import methods.ArrayMethods;
import methods.HashtableMethods;
import methods.MatrixMethods;
import methods.WriteToFile;

import woodchesterBadgers.CaptureData;
import woodchesterBadgers.CreateDescriptiveEpidemiologicalStats;
import woodchesterBadgers.DynamicGroupContactNetwork;
import woodchesterBadgers.StepwiseMatching;

public class BuildDynamicBadgerGroupNetworks {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		// Read in the consolidated badger capture data
		String consolidatedCaptureData = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/TrappingData/consolidatedWPData.txt";
		Hashtable<String, CaptureData> captureData = CreateDescriptiveEpidemiologicalStats.readConsolidatedBadgerCaptureInfo(consolidatedCaptureData);
	
		// Read in the Badger Group Location Information
		String territoryCentroidsFile = "C:/Users/Joseph Crisp/Desktop/UbuntuSharedFolder/Woodchester/TerritoryCentroids.csv";
		Hashtable<String, double[]> territoryCentroids = StepwiseMatching.getTerritoryCentroids(territoryCentroidsFile);

		// Define a window, during which we are interested in the group contact network
		int[] window = {1982, 2011};
		
		// Get yearly group adjacency matrices
		DynamicGroupContactNetwork adjacencyMatrices = getGroupAdajacencyMatrices(captureData, window, territoryCentroids);
		
		// Open an output file
		String outputFile = "C:/Users/Joseph Crisp/Desktop/DynamicAdjacencyMatrices.txt";
		printAdjacencyMatrices(adjacencyMatrices, outputFile, "\t");
	}

	public static void printAdjacencyMatrices(DynamicGroupContactNetwork adjacencyMatrixInfo, String fileName, String sep) throws IOException{
		
		// Open and Wipe File
		BufferedWriter bWriter = WriteToFile.openFile(fileName, false);
		
		// Extract the adjacency matrix information
		int[][][] adjacencyMatrices = adjacencyMatrixInfo.getAdjacencyMatrices();
		String[] orderedGroupNames = adjacencyMatrixInfo.getOrderedGroups();
		int[] window = adjacencyMatrixInfo.getWindow();
		
		// Convert the ordered group names into a string
		String names = ArrayMethods.toString(orderedGroupNames, sep);
		
		// Print out each adjacency matrix for the group network for each year of our window
		for(int x = 0; x < adjacencyMatrices.length; x++){
			WriteToFile.writeLn(bWriter, "Weighted Adjacency Matrix for Dispersal Events that occured in: " + (window[0] + x ));
			
			// Print out the group names
			WriteToFile.writeLn(bWriter, "groups" + sep + names);
			
			for(int i = 0; i < adjacencyMatrices[0].length; i++){
				
				// Print out the current group
				WriteToFile.write(bWriter, orderedGroupNames[i]);
				
				for(int j = 0; j < adjacencyMatrices[0][0].length; j++){
					
					// Print out the current element of the current adjacency matrix
					WriteToFile.write(bWriter, sep + adjacencyMatrices[x][i][j]);
				}
				
				// Finish the line
				WriteToFile.write(bWriter, "\n");
			}
		}
		
		// Close the output file
		WriteToFile.close(bWriter);
	}
	
	public static DynamicGroupContactNetwork getGroupAdajacencyMatrices(Hashtable<String, CaptureData> captureData, int[] window,
			Hashtable<String, double[]> territoryCentroids){
		
		// RECORDING MOVEMENT BY THE YEAR THAT THE BADGER WAS FOUND TO BE IN A DIFFERENT GROUP
		
		// Get the badger tattoos
		String[] tattoos = HashtableMethods.getKeysString(captureData);
		CaptureData history;
		
		// Initialise a hashtable to store the groups and their indices in the adjacency matrix
		Hashtable<String, Integer> groupIndices = new Hashtable<String, Integer>();
		int groupIndex = -1;
		
		// Initialise an adjacency matrix to record the inter-group movements for each year
		int[][][] adjacencyMatrices = new int[(window[1] - window[0]) + 1][100][100]; // Large size - cut down later
		
		// Initialise variables to keep track of badgers group
		String[] groups;
		Calendar[] dates;
		
		// Examine the movement history of each badger
		for(String tattoo : tattoos){
			
			history = captureData.get(tattoo);
			groups = history.getGroupsInhabited();
			dates = history.getCaptureDates();
			
			// Have we seen the current badger's group before? Is it a group we're interested in?
			if(groupIndices.get(groups[0]) == null && groups[0].equals("NA") == false && territoryCentroids.get(groups[0]) != null){
				groupIndex++;
				groupIndices.put(groups[0], groupIndex);
			}
			
			// Skip badgers for which there was only one capture event
			if(history.getCaptureDates().length == 1){
				continue;
			}
			
			// Examine the dispersal history of the current badger
			for(int i = 1; i < groups.length; i++){
				
				// Have we seen the current group before?  Is it a group we're interested in?
				if(groupIndices.get(groups[i]) == null && groups[i].equals("NA") == false && territoryCentroids.get(groups[i]) != null){
					groupIndex++;
					groupIndices.put(groups[i], groupIndex);
				}
				
				/**
				 *  Has the badger changed group?
				 *  Is the movement between two known groups?
				 *  Does the movement fall within the window that we are interested in?
				 *  Are both the previous and current groups, ones that we are interested in?
				 *  
				 *  RECORDING MOVEMENT BY THE YEAR THAT THE BADGER WAS FOUND TO BE IN A DIFFERENT GROUP
				 */
				if(groups[i-1].equals(groups[i]) == false && groups[i].equals("NA") == false && groups[i-1].equals("NA") == false && dates[i].get(Calendar.YEAR) >= window[0] && dates[i].get(Calendar.YEAR) <= window[1] && territoryCentroids.get(groups[i]) != null && territoryCentroids.get(groups[i - 1]) != null){
					
					// Record the movement
					adjacencyMatrices[dates[i].get(Calendar.YEAR) - window[0]][groupIndices.get(groups[i-1])][groupIndices.get(groups[i])]++;
				}
			}			
		}
		
		// Remove the unused rows and columns from adjacency
		adjacencyMatrices = removeUnusedRowsAndCols(adjacencyMatrices, groupIndex);
		
		return new DynamicGroupContactNetwork(adjacencyMatrices, window, groupIndices);
	}
	
	public static int[][][] removeUnusedRowsAndCols(int[][][] adjacencyMatrices, int lastGroupIndex){
		
		// Initialise new empty adjacency matrix
		int[][][] newRecord = new int[adjacencyMatrices.length][lastGroupIndex + 1][lastGroupIndex + 1];
		
		// Fill the empty matrix
		for(int x = 0; x < adjacencyMatrices.length; x++){
			for(int i = 0; i < lastGroupIndex + 1; i++){
				for(int j = 0; j < lastGroupIndex + 1; j++){
					newRecord[x][i][j] = adjacencyMatrices[x][i][j];
				}
			}
		}
		
		return newRecord;
	}
}
