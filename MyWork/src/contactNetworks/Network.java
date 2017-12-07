package contactNetworks;
import methods.GraphMethods;

public class Network {

	public Individual[] individuals;
	public int[][] adjacencyMatrix;
	public double[][] distanceMatrix;
	
	public Network(Individual[] ids, int[][] adjacency) {
		this.individuals = ids;
		this.adjacencyMatrix = adjacency;
		this.distanceMatrix = GraphMethods.buildDistanceMatrixDouble(adjacency);
	}
	
	// Methods for Setting Information
	public void setIndividuals(Individual[] ids){
		this.individuals = ids;
	}
	public void setAdjacencyMatrix(int[][] adjacency){
		this.adjacencyMatrix = adjacency;
		this.distanceMatrix = GraphMethods.buildDistanceMatrixDouble(adjacency);
	}
	
	// Methods for Getting Information
	public Individual[] getIndividuals(){
		return individuals;
	}
	public int[][] getAdjacencyMatrix(){
		return adjacencyMatrix;
	}
	public double[][] getDistanceMatrix(){
		return distanceMatrix;
	}
}
