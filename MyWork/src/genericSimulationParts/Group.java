package genericSimulationParts;

public class Group {

	public Individual[] individuals;
	public int groupId;
	public double[] latLongs;
	
	public Group(Individual[] individualsInGroup, int id) {

		this.individuals = individualsInGroup;
		this.groupId = id;

	}
	
	// Setting Methods
	public void setIndividuals(Individual[] individualsInGroup){
		this.individuals = individualsInGroup;
	}
	public void setGroupId(int id){
		this.groupId = id;
	}
	public void setLatLongs(double[] latsAndLongs){
		this.latLongs = latsAndLongs;
	}
	
	// Getting Methods
	public Individual[] getIndividuals(){
		return this.individuals;
	}
	public int getGroupId(){
		return this.groupId;
	}
	public double[] getLatLongs(){
		return this.latLongs;
	}
	
	// Other Methods
	public void removeIndividual(Individual individual){
		
		Individual[] group = new Individual[this.individuals.length - 1];
		
		int pos = -1;
		for(Individual x : this.individuals){
			if(x != individual){
				pos++;
				group[pos] = x;
			}
		}
		
		this.individuals = group;
	}
	
	public void addIndividual(Individual individual){
		Individual[] group = new Individual[this.individuals.length + 1];
		
		for(int i = 0; i < this.individuals.length; i++){
			group[i] = individuals[i];
		}
		group[this.individuals.length] = individual;
		
		this.individuals = group;
	}

}
