package woodchesterCattle;

import java.util.Hashtable;

public class MovementNetwork {

	public Hashtable<String, Location> locations = new Hashtable<String, Location>();
	public int[][] adjacencyMatrix;
	public double[][] spatialDistanceMatrix;
	public Hashtable<Integer, int[][]> shortestPathsFull;
	public Hashtable<Integer, int[][]> shortestPathsWithoutSelectedPremises;
	public int[][] nSharedBetweenLocationsMatrix;

	
	public MovementNetwork(Hashtable<String, Location> locationInfo, int[][] matrix){
		
		this.adjacencyMatrix = matrix;	
		this.locations = locationInfo;
	}
	
	// Setting Methods
	public void setLocations(Hashtable<String, Location> locationInfo){
		this.locations = locationInfo;
	}
	public void setAdjacencyMatrix(int[][] matrix){
		this.adjacencyMatrix = matrix;
	}
	public void setSpatialDistanceMatrix(double[][] matrix){
		this.spatialDistanceMatrix = matrix;
	}
	public void setShortestPathsFull(Hashtable<Integer, int[][]> paths){
		this.shortestPathsFull = paths;
	}
	public void setShortestPathsWithoutSelectedPremises(Hashtable<Integer, int[][]> paths){
		this.shortestPathsWithoutSelectedPremises = paths;
	}
	public void setNSharedBetweenLocationsMatrix(int[][] matrix){
		this.nSharedBetweenLocationsMatrix = matrix;
	}
	
	// Getting Methods
	public Hashtable<String, Location> getLocations(){
		return this.locations;
	}
	public int[][] getAdjacencyMatrix(){
		return this.adjacencyMatrix;
	}
	public double[][] getSpatialDistanceMatrix(){
		return this.spatialDistanceMatrix;
	}
	public int getNLocations(){
		return this.locations.size();
	}
	public int getNMovementsBetweenHerds(String herdA, String herdB){
		
		return this.adjacencyMatrix[this.locations.get(herdA).getPosInAdjacencyMatrix()][this.locations.get(herdB).getPosInAdjacencyMatrix()];
	}	
	public double getSpatialDistanceBetweenHerds(String herdA, String herdB, MovementNetwork network){
		
		return this.spatialDistanceMatrix[this.locations.get(herdA).getPosInAdjacencyMatrix()][this.locations.get(herdB).getPosInAdjacencyMatrix()];
	}
	public Hashtable<Integer, int[][]> getShortestPathsFull(){
		return this.shortestPathsFull;
	}
	public Hashtable<Integer, int[][]> getShortestPathsWithoutSelectedPremises(){
		return this.shortestPathsWithoutSelectedPremises;
	}
	public int[][] getNSharedBetweenLocationsMatrix(){
		return this.nSharedBetweenLocationsMatrix;
	}
	
}
