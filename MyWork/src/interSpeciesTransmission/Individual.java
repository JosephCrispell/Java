package interSpeciesTransmission;

public class Individual {

	public int id;
	public int type;
	public int infectionStatus = 0;
	public int[] infectionStatusChanges;
	
	public Individual(int uniqueId, int typeOfIndividual, int nInfectionStates) {
		
		this.id = uniqueId;
		this.type = typeOfIndividual;
		
		this.infectionStatusChanges = new int[nInfectionStates];

	}
	
	// Setting Methods
	public void setInfectionStatus(int status, int timeStep){
		this.infectionStatus = status;
		this.infectionStatusChanges[status] = timeStep;
	}
	
	// Getting Methods
	public int getId(){
		return this.id;
	}
	public int getType(){
		return this.type;
	}
	public int getInfectionStatus(){
		return this.infectionStatus;
	}
	public int[] getInfectionStatusChanges(){
		return this.infectionStatusChanges;
	}

	// General Methods
	public static Individual[] append(Individual[] individuals, Individual individual){
		Individual[] newArray = new Individual[individuals.length + 1];
		
		for(int i = 0; i < individuals.length; i++){
			newArray[i] = individuals[i];
		}
		newArray[individuals.length] = individual;
		
		return newArray;
	}
}
