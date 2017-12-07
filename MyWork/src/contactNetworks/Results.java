package contactNetworks;

public class Results {

	public Individual[] individuals;
	public Individual[] sampledIndividuals;
	public double[][] geneticDistanceMatrix;
	public double[][] spatialDistanceMatrix;
	public double[][] temporalDistanceMatrix;
	public int[][] networkDistanceMatrix;
	public int[][] mutationsVsGenerationTime; // No.Mutations	GenerationTime	Timestep
	
	public Results(Individual[] population, Individual[] sampled, double[][] genetic, double[][] spatial) {
		this.individuals = population;
		this.sampledIndividuals = sampled;
		this.geneticDistanceMatrix = genetic;
		this.spatialDistanceMatrix = spatial;
	}
	
	// Methods for Setting
	public void setIndividuals(Individual[] population){
		this.individuals = population;
	}
	public void setSampledIndividuals(Individual[] sampled){
		this.sampledIndividuals = sampled;
	}
	public void setGeneticDistanceMatrix(double[][] genetic){
		this.geneticDistanceMatrix = genetic;
	}
	public void setSpatialDistanceMatrix(double[][] spatial){
		this.spatialDistanceMatrix = spatial;
	}
	public void setTemporalDistanceMatrix(double[][] temporal){
		this.temporalDistanceMatrix = temporal;
	}
	public void setNetworkDistanceMatrix(int[][] network){
		this.networkDistanceMatrix = network;
	}
	public void setMutationsVsGenerationTime(int[][] mutationsPerGeneration){
		this.mutationsVsGenerationTime = mutationsPerGeneration;
	}
	
	
	// Methods for Getting
	public Individual[] getIndividuals(){
		return individuals;
	}
	public Individual[] getSampledIndividuals(){
		return sampledIndividuals;
	}
	public double[][] getGeneticDistanceMatrix(){
		return geneticDistanceMatrix;
	}
	public double[][] getSpatialDistanceMatrix(){
		return spatialDistanceMatrix;
	}
	public double[][] getTemporalDistanceMatrix(){
		return temporalDistanceMatrix;
	}
	public int[][] getNetworkDistanceMatrix(){
		return networkDistanceMatrix;
	}
	public int[][] getMutationsVsGenerationTime(){
		return mutationsVsGenerationTime;
	}
}
