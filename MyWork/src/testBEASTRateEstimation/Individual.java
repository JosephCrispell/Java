package testBEASTRateEstimation;

import java.util.Hashtable;

import genericSimulationParts.Global;
import methods.ArrayMethods;

import org.apache.commons.math3.random.MersenneTwister;

public class Individual {

	// Status information
	public int id;
	public int infectionStatus = 0;
	public int[] infectionStatusChanges;
	public int state;
	
	// Mutation event info
	public int[] mutationEvents = new int[9999];
	public int lastMutationIndex = -1;
    public int mutationsLastChecked = 0;
    public Hashtable<Integer, Integer> mutationEventsHashtable;
    public char[] sequence;
    public Hashtable<Integer, IntArray> genomeSiteInfo;
    
    // Sampling info
    public int timeStepSampled = -1;
    
    // Transmission event info
    public int[] infectees = new int[9999];
    public int lastInfecteeIndex = -1;
	
	public Individual(int uniqueId, int nInfectionStates, int timeStep) {
		
		this.id = uniqueId;
		this.infectionStatusChanges = new int[nInfectionStates];
		this.infectionStatusChanges[0] = timeStep;
		this.mutationsLastChecked = timeStep;

	}
	
	// Setting Methods
	public void setInfectionStatus(int status, int timeStep){
		this.infectionStatus = status;
		this.infectionStatusChanges[status] = timeStep;
	}
    public void setMutationsLastChecked(int count){
      	this.mutationsLastChecked = count;
    }
    public void setSequence(char[] nucleotides){
    	this.sequence = nucleotides;
    }
    public void setTimeStepSampled(int timeStep){
    	this.timeStepSampled = timeStep;
    }
    public void addInfectee(int id){
    	
    	// Increment the index
    	this.lastInfecteeIndex++;
    	
    	// Check that we are still within the bounds of the array length
    	if(this.lastInfecteeIndex < this.infectees.length){
    		this.infectees[this.lastInfecteeIndex] = id;
    	}else{
    		
    		// Increase the size of the infectees list
    		int[] newList = new int[infectees.length * 2];
    		
    		for(int i = 0; i < this.lastInfecteeIndex; i++){
    			newList[i] = this.infectees[i];
    		}
    		newList[this.lastInfecteeIndex] = id;
    		
    		this.infectees = newList;
    	}    	
    }
    public void setGenomeSiteInfo(Hashtable<Integer, IntArray> info){
    	this.genomeSiteInfo = info;
    }
    public void setState(int type){
    	this.state = type;
    }
	
	// Getting Methods
	public int getId(){
		return this.id;
	}
	public int getInfectionStatus(){
		return this.infectionStatus;
	}
	public int[] getInfectionStatusChanges(){
		return this.infectionStatusChanges;
	}
	public int[] getMutationEvents(){
	  	
		// Create an array to store the mutation events - used positions
		int[] events = new int[this.lastMutationIndex + 1];
		
		// Get each of the mutation events - used positions - in the larger array
		for(int i = 0; i <= this.lastMutationIndex; i++){
			events[i] = this.mutationEvents[i];
		}
		
		return events;
	}
	public int getMutationsLastChecked(){
	  	return this.mutationsLastChecked;
	}
	public char[] getSequence(){
		return this.sequence;
	}
	public int getTimeStepSampled(){
		return this.timeStepSampled;
	}
	public int[] getInfectees(){
		
		// Initialise an array to store the IDs of the infectees of the current individual - used positions
		int[] fullPositions = new int[this.lastInfecteeIndex + 1];
		
		// Insert each of the infectees - in used positions
		for(int i = 0; i < this.lastInfecteeIndex + 1; i++){
			fullPositions[i] = this.infectees[i];
		}
		
		return fullPositions;
	}
	public int getLastInfecteeIndex(){
		return this.lastInfecteeIndex;
	}
	public Hashtable<Integer, Integer> getMutationEventsHashtable(){
		return this.mutationEventsHashtable;
	}
	public Hashtable<Integer, IntArray> getGenomeSiteInfo(int[][] mutationEventInfo, int sizeLimit){
		
		// Check if Genome Site Info exists
		if(this.genomeSiteInfo == null){
			
			noteMutationEventSites(mutationEventInfo, sizeLimit);			
		}
		
		return this.genomeSiteInfo;
	}
	public int getState(){
		return this.state;
	}
	
	// General Methods
	public void addMutationEvents(int[] events, int timeStep){

		// Check that we are still within the bounds of the array length
    	if(this.lastMutationIndex + events.length < this.mutationEvents.length){
    		
    		// Add each of the new events
    		for(int i = 0; i < events.length; i++){
    			this.mutationEvents[i + this.lastMutationIndex + 1] = events[i];
    		}
    		
    		// Record the last position used
    		this.lastMutationIndex = this.lastMutationIndex + events.length;
    		
    	}else{
    		
    		// Increase the size of the mutation events list
    		int[] newList = new int[this.mutationEvents.length * 2];
    		
    		// Add in the previous mutation events
    		for(int i = 0; i <= this.lastMutationIndex; i++){
    			newList[i] = this.mutationEvents[i];
    		}
    		
    		// Add each of the new events
    		for(int i = 0; i < events.length; i++){
    			this.mutationEvents[i + this.lastMutationIndex + 1] = events[i];
    		}
    		
    		// Record the last position used
    		this.lastMutationIndex = this.lastMutationIndex + events.length;
    	}
    	
    	// Update when the mutation events were last checked
    	this.mutationsLastChecked = timeStep;		
	}
	
	public void noteMutationEventSites(int[][] mutationEventInfo, int sizeLimit){
		
		// Initialise a hashtable to record genome sites of this individual's mutation events
		this.genomeSiteInfo = new Hashtable<Integer, IntArray>();
		
		// Initialise an array to note the mutation events present at a particular site
		IntArray events;
		
		// Examine each of the individual's mutation events
		for(int event : this.mutationEvents){
			
			// Has the site been used previously?
			if(this.genomeSiteInfo.get(mutationEventInfo[event][0]) != null){
							
				// Add the event to the array of events at the current site
				this.genomeSiteInfo.get(mutationEventInfo[event][0]).append(event);
				
			// If site not used note event present at site
			}else{
				
				// Create an array to record the events at the current site
				events = new IntArray(sizeLimit);
				events.append(event);
				this.genomeSiteInfo.put(mutationEventInfo[event][0], events);
			}
		}
	}
	
	public void createMutationEventsHashtable(){
		
		// Initialise a Hashtable to Store the 
		this.mutationEventsHashtable = new Hashtable<Integer, Integer>();
		
		// Index each of the mutation events
		for(int i = 0; i < this.mutationEvents.length; i++){
			this.mutationEventsHashtable.put(this.mutationEvents[i], i);
		}
	}

	public int isMutationEventPresent(int mutationEvent){
		
		// Check mutation events hashtable has been created
		if(this.mutationEventsHashtable == null){
			createMutationEventsHashtable();
		}
		
		// Check if mutation event is present in the hashtable
		int result = 0;
		if(this.mutationEventsHashtable.get(mutationEvent) != null){
			result = 1;
		}
		
		return result;
	}
}
