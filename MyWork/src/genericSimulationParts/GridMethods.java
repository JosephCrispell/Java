package genericSimulationParts;
import java.util.Arrays;
import java.util.Random;

import methods.*;

import org.apache.commons.math3.random.MersenneTwister;


public class GridMethods {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Methods Testing Area
		
		int[] groupIds = {0,1,2,3,4,5,6,7,8,9,10};
		
		int[][] grid = createRandomPopulationGrid(10, 10, groupIds);
		
		MatrixMethods.print(grid);
		
		System.out.println(Arrays.toString(calculateGroupEdgeDistances(grid, groupIds.length)));

		
	}
	
	// Methods
	public static int[][] createRandomPopulationGrid(int noRows, int noCols, int[] groupIds){
		
		// Create Instance of a Random Number Generator
		MersenneTwister random = new MersenneTwister();
		
		// Create an Empty Grid
		int[][] grid = new int[noRows][noCols];
		grid = MatrixMethods.fill(grid, -1);
		
		// Randomly Position the Groups on the Grid
		int row = 0;
		int col = 0;
		for(int id : groupIds){
			
			// Avoid placing Groups in Used spots
			int used = 1;
			while( used == 1 ){
				row = random.nextInt(noRows);
				col = random.nextInt(noCols);
				
				if(grid[row][col] == -1){
					used = 0;
				}
			}
			
			// Place Group
			grid[row][col] = id;
			
		}
		
		return grid;		
	}

	public static int[] findNeighbours(int groupId, int[][] grid, int neighbourhoodSize){
		
		// Find position of current Group
		int[] coordinates = {-1,-1};
		for( int row = 0; row < grid.length; row++){
			for( int col = 0; col < grid[0].length; col++){
				
				if( groupId == grid[row][col] ){
					coordinates[0] = row;
					coordinates[1] = col;
				}
			}
		}
		
		// Find the Neighbours of this Group
		int row = coordinates[0];
		int col = coordinates[1];
		
		int[] neighbours = {};
		
		// Search the Columns in the:
		for( int index = col - neighbourhoodSize; index <= col + neighbourhoodSize; index++){
				
			// Check searching within the confines of the grid
			if(index >= grid[0].length || index < 0){
				continue;
			}
				
			// Check all the rows involved
			for(int i = 1; i <= neighbourhoodSize; i++ ){
			
				// Rows above
				if(row - i < grid.length && row - i >= 0 && grid[row - i][index] != -1){
					neighbours = ArrayMethods.append(neighbours, grid[row - i][index]);
				}
				
				// Rows below
				if(row + i < grid.length && row + i >= 0 && grid[row + i][index] != -1){
					neighbours = ArrayMethods.append(neighbours, grid[row + i][index]);
				}
			
			}
			
			// Containing Row
			if(row < grid.length && row >= 0 && grid[row][index] != -1 && grid[row][index] != groupId){
				neighbours = ArrayMethods.append(neighbours, grid[row][index]);
			}

		}
		
		return neighbours;
	}

	public static int[][] deriveConnections(int[][] grid, int neighbourhoodSize, int[] groupIds){
		
		// Create a Large Empty Array to Store the Connections
		int[][] connections = new int[0][0];
		
		for(int id : groupIds){
			
			int[] neighbours = findNeighbours(id, grid, neighbourhoodSize);
			
			// Add a Connection for each of the Current Groups Neighbours
			for(int neighbour : neighbours){
				int[] connection = {id, neighbour};
				
				// Check if Connection hasn't already been added
				int found = 0;
				for(int[] edge : connections){
					if(edge[0] == connection[0] && edge[1] == connection[1] || edge[0] == connection[1] && edge[1] == connection[0]){
						found++;
					}
				}
				
				// Add Connection if not already Present
				if(found == 0){
					connections = appendEdge(connections, connection);
				}
				
			}
		}
		
		return connections;
	}
	
	public static int[] findEdgeGroups(int[][] grid){
		int[] edgeGroups = {};
		
		// Search Top Row
		for(int element : grid[0]){ 
			if(element != -1){
				edgeGroups = ArrayMethods.append(edgeGroups, element);
			}
		}
		
		// Search Bottom Row
		for(int element : grid[grid.length - 1]){ 
			if(element != -1){
				edgeGroups = ArrayMethods.append(edgeGroups, element);
			}
		}
		
		// Search Left Side
		for(int row = 1; row < grid.length - 1; row++){
			if(grid[row][0] != -1){
				edgeGroups = ArrayMethods.append(edgeGroups, grid[row][0]);
			}
		}
		
		// Search Right Side
		for(int row = 1; row < grid.length - 1; row++){
			if(grid[row][grid[0].length - 1] != -1){
				edgeGroups = ArrayMethods.append(edgeGroups, grid[row][grid[0].length - 1]);
			}
		}
		
		return edgeGroups;
	}

	public static int[][] appendEdge(int[][] matrix, int[] edge){
		int[][] newMatrix = new int[matrix.length + 1][2];
				
		for(int i = 0; i < matrix.length; i++){
			newMatrix[i] = matrix[i];
		}
		
		newMatrix[matrix.length] = edge;
		
		return newMatrix;
	}

	public static int[] findConnectedGroups(int[][] connections, int groupId){
		int[] groups = new int[0];
		
		// Investigate each Edge
		for(int[] edge : connections){
			
			if(edge[0] == groupId){
				groups = ArrayMethods.append(groups, edge[1]);
			}
		}
		
		return groups;
	}

	public static double euclidianDistance(int[] point1, int[] point2){
		// Calculate the Euclidian Distance between two points on a 2D grid
		
		double[] a = {(double) point1[0], (double) point1[1]};
		double[] b = {(double) point2[0], (double) point2[1]};
		
		return Math.sqrt( Math.pow((a[0] - b[0]), 2.0) + Math.pow((a[1] - b[1]), 2.0) );
	}

	public static double[][] generateEuclideanDistanceMatrix(int[][] grid, int[] groupIds){
		double[][] distanceMatrix = new double[groupIds.length][groupIds.length];
		
		// Find the Coordinates of all the Groups
		int[][] groupCoordinates = findAllGroupCoordinates(grid, groupIds.length);
		
		for(int groupA : groupIds){
			int[] a = groupCoordinates[groupA];
			
			for(int groupB : groupIds){
				if(groupA != groupB && distanceMatrix[groupA][groupB] == 0){
					int[] b = groupCoordinates[groupB];
					
					double distance = euclidianDistance(a, b);
					distanceMatrix[groupA][groupB] = distance;
					distanceMatrix[groupB][groupA] = distance;
				}
			}
		}
		
		return distanceMatrix;
	}
	
	public static int[] findGroup(int[][] grid, int groupId){
		
		int[] coordinates = {-1, -1};
		
		for(int i = 0; i < grid.length; i++){
			for(int j = 0; j < grid[0].length; j++){
				
				if(grid[i][j] == groupId){
					coordinates[0] = i;
					coordinates[1] = j;
				}
			}
		}
		
		return coordinates;
	}

	public static int[][] findAllGroupCoordinates(int[][] grid,  int noGroups){
		
		int[][] groupCoordinates = new int[noGroups][2];
		
		for(int i = 0; i < grid.length; i++){
			for(int j = 0; j < grid[0].length; j++){
				
				if(grid[i][j] != -1){
					groupCoordinates[grid[i][j]][0] = i;
					groupCoordinates[grid[i][j]][1] = j;
				}
			}
		}
		
		return groupCoordinates;
	}

	public static double[] calculateGroupEdgeDistances(int[][] grid, int noGroups){
		double[] edgeDistances = new double[noGroups];
		int[] distanceToSides = new int[4];
		
		for(int i = 0; i < grid.length; i++){
			for(int j = 0; j < grid[0].length; j++){
				
				if(grid[i][j] != -1){

					// Find which side 
					distanceToSides[0] = i; // Top
					distanceToSides[1] = (grid.length - 1) - i; // Bottom
					distanceToSides[2] = j; // Left
					distanceToSides[3] = (grid[0].length - 1) - j; // Right
					
					edgeDistances[grid[i][j]] = ArrayMethods.min(distanceToSides);
				}
			}
		}
		return edgeDistances;
	}
}
