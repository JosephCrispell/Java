package genericSimulationParts;

public class Sample {

	public int[] mutationEventSequence;
	public Individual sampledIndividual;
	public int timestepSampled;
	public double[] latLongs;
		
	public Sample(int[] mutationEvents, Individual individual, int timestep, double[] latsAndLongs) {
		
		this.mutationEventSequence = mutationEvents;
		this.sampledIndividual = individual;
		this.timestepSampled = timestep;
		this.latLongs = latsAndLongs;
	}
	
	// Setting Methods
	public void setMutationEventSequence(int[] mutationEvents){
		this.mutationEventSequence = mutationEvents;
	}
	public void setSampledIndividuals(Individual individual){
		this.sampledIndividual = individual;
	}
	public void setTimestepSampled(int timestep){
		this.timestepSampled = timestep;
	}
	public void setLatLongs(double[] latsAndLongs){
		this.latLongs = latsAndLongs;
	}
	
	// Getting Methods
	public int[] getMutationEventSequence(){
		return this.mutationEventSequence;
	}
	public Individual getSampledIndividual(){
		return this.sampledIndividual;
	}
	public int getTimestepSampled(){
		return this.timestepSampled;
	}
	public double[] getLatLongs(){
		return this.latLongs;
	}
	
}
