package badgerPopulation;

public class Grid {

    public int[] groups;
    public int[][] grid;
    //public int[][] edges;
    public double[][] euclideanDistanceMatrix;
    public double[] edgeDistances;
    
    public Grid(int[][] popGrid, int[] groupIds, double[][] euclidDist, double[] groupEdgeDistances){
	   	this.grid = popGrid;
    	this.groups = groupIds;
    	//this.edges = connections;
    	this.euclideanDistanceMatrix = euclidDist;
    	this.edgeDistances = groupEdgeDistances; 
	}

    // Methods for Getting Grid Information

    public int[][] getGrid(){
   	 	return grid;
    }
    public int[] getGroups(){
   	 	return groups;
    }
    //public int[][] getEdges(){
   	 	//return edges;
    //}
    public double[][] getDistanceMatrix(){
   	 	return euclideanDistanceMatrix;
    }
    public double[] getEdgeDistances(){
   	 	return edgeDistances;
    }
}
