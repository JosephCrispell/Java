package ExamineWPInterspeciesTransmission;

import java.util.Hashtable;

import methods.ArrayMethods;
import methods.HashtableMethods;
import methods.MatrixMethods;
import woodchesterGeneticVsEpi.CompareIsolates;

public class AdjacencyMatrix {

	public Hashtable<String, Integer> groupIndices;
	public int[][] adjacencyMatrix;
	public int[][] shortestPaths;
	
	public String[] sampledGroups;
	public int[] sampledGroupIndices;
	public double meanShortestPathLength;
	public double proportionPathsPresent;
	
	public AdjacencyMatrix(String[] groups){
		
		this.groupIndices = HashtableMethods.indexArray(groups);
		this.adjacencyMatrix = new int[groups.length][groups.length];
		this.shortestPaths = new int[groups.length][groups.length];
	}
	
	// Setting methods
	public void setSampledGroups(String[] groups){
		this.sampledGroups = groups;
		
		// Note the indices of the sampled herds
		this.sampledGroupIndices = new int[this.sampledGroups.length];
		for(int i = 0; i < this.sampledGroups.length; i++){
			this.sampledGroupIndices[i] = this.groupIndices.get(this.sampledGroups[i]);
		}
	}
	
	// Getting methods
	public double getMeanShortestPathLength(){
		return this.meanShortestPathLength;
	}
	public double getProportionsPathsPresent(){
		return this.proportionPathsPresent;
	}
	
	// Recording movements
	public void addMovement(String from, String to, boolean directional){
		
		if(groupIndices.get(from) != null && groupIndices.get(to) != null){
			this.adjacencyMatrix[groupIndices.get(from)][groupIndices.get(to)]++;
			if(directional == false){
				this.adjacencyMatrix[groupIndices.get(to)][groupIndices.get(from)]++;
			}
		}
	}

	// Shortest path
	public void noteShortestPathForSampledNodes(){

		// Calculate the shortest paths between all the herds
		int[][] shortestPathsForGroup;
		
		for(int i : this.sampledGroupIndices){
			
			shortestPathsForGroup = CompareIsolates.findShortestPathsFromNode(i, this.adjacencyMatrix);
			
			for(int j = 0; j < this.adjacencyMatrix.length; j++){
				
				this.shortestPaths[i][j] = shortestPathsForGroup[j].length;
			}
		}
		
		// Calculate the mean shortest path length between the sampled herds
		calculateMeanShortestPathLengthBetweenHerds(this.sampledGroupIndices);
	}

	public void calculateMeanShortestPathLengthBetweenHerds(int[] herdIndices){
		
		// Initialise variables to store the calculate mean shortest path length and proportion paths present
		this.meanShortestPathLength = 0;
		this.proportionPathsPresent = 0;
		
		// Compare the selected herds to one another
		for(int i : herdIndices){
			for(int j : herdIndices){
				
				// Ignore self comparisons
				if(i == j){
					continue;
				}

				// Check that a shortest path exists between the two herds
				if(this.shortestPaths[i][j] != 0){
					
					// Add to running shortest path total
					this.meanShortestPathLength += this.shortestPaths[i][j];
					
					// Note that a path is present
					this.proportionPathsPresent++;
				}				
			}
		}
		
		if(this.proportionPathsPresent != 0){
			
			// Calculate the mean
			this.meanShortestPathLength = this.meanShortestPathLength / this.proportionPathsPresent;
			
			// Calculate the proportion of paths that existed;
			this.proportionPathsPresent = this.proportionPathsPresent / ((double) herdIndices.length * ((double) herdIndices.length - 1.0));
		}		
	}
}
