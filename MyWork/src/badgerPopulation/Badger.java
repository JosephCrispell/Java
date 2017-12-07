package badgerPopulation;

import java.util.Hashtable;

import testRateEstimation.IntArray;

public class Badger {

    public int sex = -1;
    public int age = 0;
    public int infectionStatus = 0;
    public int groupId = -1;
    public int badgerId = -1;
    public char fertilisationStatus = 'N';

    // Transmission inforation
    public int[] infectees = new int[0];
    
    // Mutation event info
	public int[] mutationEvents = new int[9999];
	public int lastMutationIndex = -1;
    public int mutationsLastChecked = 0;
    public Hashtable<Integer, IntArray> genomeSiteInfo;
    
    public Badger(int badgerSex, int badgerAge, int badgerInfectionStatus, int badgerGroupId, int currentBadgerId){
	   	 this.sex = badgerSex;
	   	 this.age = badgerAge;
	   	 this.infectionStatus = badgerInfectionStatus;
	   	 this.groupId = badgerGroupId;
	   	 this.badgerId = currentBadgerId;
	}

    // Methods for Setting Badger Information
    public void setInfectees(int[] array){
    	this.infectees = array;
    }
    public void setMutationsLastChecked(int count){
      	this.mutationsLastChecked = count;
    }
    public void setSex(char badgerSex){
   	 	this.sex = badgerSex;
    }
    public void setAge(int badgerAge){
   	 	this.age = badgerAge;
    }
    public void setInfectionStatus(int badgerInfectionStatus){
   	 	this.infectionStatus = badgerInfectionStatus;
    }
    public void setGroupId(int badgerGroupId){
   	 	this.groupId = badgerGroupId;
    }
    public void setBadgerId(int currentBadgerId){
   	 	this.badgerId = currentBadgerId;
    }
    public void setFertilisationStatus(char badgerFertilisationStatus){
      	this.fertilisationStatus = badgerFertilisationStatus;
    }
    public void setMutationEvents(int[] mutations, int seasonCount){
      	this.mutationEvents = mutations;
      	this.mutationsLastChecked = seasonCount;
    }

    
    // Methods for Getting Badger Information
    public int[] getInfectees(){
    	return this.infectees;
    }
    public int getSex(){
   	 	return sex;
    }
    public int getAge(){
   	 	return age;
    }
    public int getInfectionStatus(){
   	 	return infectionStatus;
    }
    public int getGroupId(){
   	 	return groupId;
    }
    public int getBadgerId(){
   	 	return badgerId;
    }
    public int getFertilisationStatus(){
      	return fertilisationStatus;
    }
    public int getMutationsLastChecked(){
    	return mutationsLastChecked;
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
	public Hashtable<Integer, IntArray> getGenomeSiteInfo(int[][] mutationEventInfo, int sizeLimit){
		
		// Check if Genome Site Info exists
		if(this.genomeSiteInfo == null){
			
			noteMutationEventSites(mutationEventInfo, sizeLimit);			
		}
		
		return this.genomeSiteInfo;
	}
	
	
	// General Mutation Event Methods
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

}
