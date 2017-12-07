package contactNetworks;

public class Individual {

	public int[] mutationEvents = new int[0];
    public int mutationsLastChecked = 0;
    public int statusIndex = 0;
    public int id;
    public int age;
    public Individual[] contacts = new Individual[0];
    public Individual[] individualsInfected = new Individual[0];
    public Individual source;
    public int groupIndex;
    public int index;
    public int[] timeEnteredStates = new int[3];
    public int timeOfLastTransmission = 0;
    public int noMutationsSinceTransmission = 0;
    
	public Individual(int uniqueId) {
		this.id = uniqueId;
	}

	// Methods for Setting Information
	public void setStatusIndex(int infectionStatusIndex){
   	 	this.statusIndex = infectionStatusIndex;
    }
	public void setMutationEvents(int[] mutations){
      	this.mutationEvents = mutations;
    }
    public void setMutationsLastChecked(int count){
      	this.mutationsLastChecked = count;
    }
    public void setContacts(Individual[] contactReferences){
      	this.contacts = contactReferences;
    }
    public void setIndividualsInfected(Individual[] infected){
      	this.individualsInfected = infected;
    }
    public void setGroupIndex(int individualsGroupIndex){
      	this.groupIndex = individualsGroupIndex;
    }
    public void setIndex(int pos){
      	this.index = pos;
    }
    public void setTimeEnteredStates(int[] timesteps){
      	this.timeEnteredStates = timesteps;
    }
    public void setTimeOfLastTransmission(int timestep){
    	this.timeOfLastTransmission = timestep;
    }
    public void setNoMutationsSinceTransmission(int number){
    	this.noMutationsSinceTransmission = number;
    }
    public void setSource(Individual individual){
    	this.source = individual;
    }
    public void setAge(int timestep){
    	this.age = timestep;
    }
  
    
    // Methods for Getting Information
    public int getId(){
    	return id;
    }
    public int getStatusIndex(){
   	 	return statusIndex;
    }    
    public int[] getMutationEvents(){
      	return mutationEvents;
    }
    public int getMutationsLastChecked(){
    	return mutationsLastChecked;
    }
    public Individual[] getContacts(){
    	return contacts;
    }
    public Individual[] getIndividualsInfected(){
    	return individualsInfected;
    }
    public int getGroupIndex(){
    	return groupIndex;
    }
    public int getIndex(){
    	return index;
    }
    public int[] getTimeEnteredStates(){
    	return timeEnteredStates;
    }
    public int getTimeOfLastTransmission(){
    	return timeOfLastTransmission;
    }
    public int getNoMutationsSinceTransmission(){
    	return noMutationsSinceTransmission;
    }
    public Individual getSource(){
    	return source;
    }
    public int getAge(){
    	return age;
    }
}
