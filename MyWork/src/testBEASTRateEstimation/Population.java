package testBEASTRateEstimation;

import java.util.Hashtable;
import java.util.Set;

import org.apache.commons.math3.random.MersenneTwister;
import org.uncommons.maths.random.PoissonGenerator;

import methods.ArrayMethods;
import methods.HashtableMethods;

public class Population {

	public Individual[] individuals;
	public int lastIndividualsIndexUsed;
	public Hashtable<Integer, Integer>[] compartments;
	public Hashtable<Integer, Integer> sampled;
	public Hashtable<Integer, Integer>[] individualsInStates;
	public double[] stateForceOfInfection;
	public double[][] stateSourceWeights;
	public double forceOfInfection;
	public double[] sourceWeights;
	
	public Population(int popSize, int nStatuses, int limit){
		
		// Initialise an array to store the individuals in the population
		this.individuals = new Individual[limit];
				
		// Initialise each of the population compartments i.e. S I R
		this.compartments = new Hashtable[nStatuses];
		for(int i = 0; i < nStatuses; i++){
			this.compartments[i] = new Hashtable<Integer, Integer>();
		}
		
		// Initialise a hashtable to store the sampled individuals
		this.sampled = new Hashtable<Integer, Integer>();
			
		// Create all of the current individuals in the population
		for(int i = 0; i < popSize; i++){
			
			this.individuals[i] = new Individual(i, nStatuses, 0);
			
			// Add the current susceptible individual into its compartment
			this.compartments[0].put(i, 1);
		}
				
		// Note the last ID used
		this.lastIndividualsIndexUsed = popSize - 1;
	}

	// Changing/Setting Methods
	public void setSourceWeights(double[] array){
		this.sourceWeights = array;
	}
	
	public void setForceOfInfection(double value){
		this.forceOfInfection = value;
	}
	
	public void setStateSourceWeights(double[][] matrix){
		this.stateSourceWeights = matrix;
	}
	
	public void setStateForceOfInfection(double[] array){
		this.stateForceOfInfection = array;
	}
	
	public void addIndividualToPopulation(int timeStep){
		
		// Increment the index
    	this.lastIndividualsIndexUsed++;
    	
    	// Check that we are still within the bounds of the array length
    	if(this.lastIndividualsIndexUsed < this.individuals.length){
    		
    		// If we are, add the new individual to the next available position
    		this.individuals[this.lastIndividualsIndexUsed] = new Individual(this.lastIndividualsIndexUsed, compartments.length, timeStep);
    	}else{
    		
    		// Otherwise increase the size of the list
    		Individual[] newList = new Individual[individuals.length * 2];
    		
    		// Insert all of the individuals already in the list
    		for(int i = 0; i < this.lastIndividualsIndexUsed; i++){
    			newList[i] = this.individuals[i];
    		}
    		
    		// Add the new individual
    		newList[this.lastIndividualsIndexUsed] = new Individual(this.lastIndividualsIndexUsed, compartments.length, timeStep);
    		
    		// Use this new bigger list from now on
    		this.individuals = newList;
    	}
		
    	// Add the individuals ID into its associated compartment
    	this.compartments[0].put(this.lastIndividualsIndexUsed, 1);
	}
	
	public void addIndividualToPopulation(int timeStep, int state){
		
		// Increment the index
    	this.lastIndividualsIndexUsed++;
    	
    	// Check that we are still within the bounds of the array length
    	if(this.lastIndividualsIndexUsed < this.individuals.length){
    		
    		// If we are, add the new individual to the next available position
    		this.individuals[this.lastIndividualsIndexUsed] = new Individual(this.lastIndividualsIndexUsed, compartments.length, timeStep);
    	}else{
    		
    		// Otherwise increase the size of the list
    		Individual[] newList = new Individual[individuals.length * 2];
    		
    		// Insert all of the individuals already in the list
    		for(int i = 0; i < this.lastIndividualsIndexUsed; i++){
    			newList[i] = this.individuals[i];
    		}
    		
    		// Add the new individual
    		newList[this.lastIndividualsIndexUsed] = new Individual(this.lastIndividualsIndexUsed, compartments.length, timeStep);
    		
    		// Use this new bigger list from now on
    		this.individuals = newList;
    	}
		
    	// Add the individuals ID into its associated compartment
    	this.compartments[0].put(this.lastIndividualsIndexUsed, 1);
    	
    	// Set the state of the individual
    	setIndividualsState(lastIndividualsIndexUsed, state);
	}
	
	public void setIndividualsInfectionStatus(int id, int newStatus, int timeStep){
		
		// Record the current individual's compartment change
		this.compartments[individuals[id].getInfectionStatus()].remove(id); // Previous
		this.compartments[newStatus].put(id, 1); // New
		
		// Change the individuals infection status
		this.individuals[id].setInfectionStatus(newStatus, timeStep);		
	}
	
	public void addIndividualsMutationEvents(int id, int[] mutationEvents, int timeStep){
		
		this.individuals[id].addMutationEvents(mutationEvents, timeStep);		
	}
	
	public void mutateIndividualsSequence(int id, int timeStep, PoissonGenerator[] randomPoissons){
		
		// When was the infected Individual last checked?
		int nTimeSteps = timeStep - this.individuals[id].getMutationsLastChecked();

		/**
		 *  Calculate the number of mutations that occurred for the current individual.
		 *  Drawing from a poisson distribution around the mutation rate associated
		 *  with the current individual's infection status. Do this for each timeStep
		 *  since the mutation events of the current individual were last checked.
		 */
		int nMutations = 0;
		int nMutationsToAdd = 0;
		for(int i = 0; i < nTimeSteps; i++){
			nMutationsToAdd = randomPoissons[this.individuals[id].getInfectionStatus()].nextValue();
			
			// If a mutation occurred record the individual and time-step it occurred in
			for(int x = 0; x < nMutationsToAdd; x++){
				Global.who[Global.mutationEventNo + (x + 1)] = id;
				Global.when[Global.mutationEventNo + (x + 1)] = this.individuals[id].getMutationsLastChecked() + i;
			}
			
			nMutations += nMutationsToAdd;
		}
		
		Global.geneticDistance += nMutations;
		Global.temporalDistance += nTimeSteps;
		
		// Keep a note of the actual number of mutations that occurred
		double rate = 0;
		if(nMutations != 0){
			rate = (double) nMutations / (double) nTimeSteps;
		}
		if(nTimeSteps != 0){
			Global.mutations = ArrayMethods.append(Global.mutations, rate);
		}
		
		// Check that mutations have occurred 
		if(nMutations > 0){
			
			// Generate a list of the these mutations
			int[] newEvents = ArrayMethods.range(Global.mutationEventNo + 1, Global.mutationEventNo + nMutations, 1);

			// Combine the new mutation events with the individuals current list
			this.individuals[id].addMutationEvents(newEvents, timeStep);
			
			// Update the mutation event counter
			Global.mutationEventNo =  Global.mutationEventNo + nMutations;
		}
	}
	
	public void recordSamplingEvent(int id, int timeStep){
		
		// Set the individuals sampling time
		this.individuals[id].setTimeStepSampled(timeStep);
		
		// Move the individuals id into the sampled hashtable
		this.compartments[individuals[id].getInfectionStatus()].remove(id); // Remove it from its infection compartment
		
		if(this.sampled.get(id) == null){
			this.sampled.put(id, 1); // add to sampled
		}else{
			
			System.out.println("ERROR!: The individual has already been sampled!");
		}
		
	}
	
	public void sampleAllIndividuals(int timeStep){
		
		// Examine each of the individuals created during the simulation
		for(int i = 0; i <= this.lastIndividualsIndexUsed; i++){
			
			// Don't sample susceptible individuals
			if(this.individuals[i].getInfectionStatus() == 0){
				continue;
			}
			
			// Record that the current individual was sampled
			this.compartments[individuals[i].getInfectionStatus()].remove(i);
			this.sampled.put(i, 1);
			
			// Note when the current individual was sampled
			this.individuals[i].setTimeStepSampled(timeStep);
		}
	}
	
	public void addInfecteeToSourcesList(int sourceId, int infecteeId){
		this.individuals[sourceId].addInfectee(infecteeId);
	}
	
	public void createIndividualsMutationEventHashtable(int id){
		this.individuals[id].createMutationEventsHashtable();
	}
	
	public void setIndividualsSequence(int id, char[] sequence){
		individuals[id].setSequence(sequence);
	}
	
	public void setIndividualsMutationEventSites(int id, int[][] mutationEventInfo, int sizeLimit){
		
		individuals[id].noteMutationEventSites(mutationEventInfo, sizeLimit);
	}
	
	public void setIndividualsState(int id, int state){
		this.individuals[id].setState(state);
		
		// Add the current individual to the list of individuals in a given state
		this.individualsInStates[state].put(id, state);
	}
	
	public void recordThatIndividualOfGivenStateRemoved(int id){
		this.individualsInStates[getIndividualsState(id)].remove(id);
	}
	
	public void initialiseListsForIndividualsInEachState(int nStates){
		this.individualsInStates = new Hashtable[nStates];
		
		for(int i = 0; i < nStates; i++){
			this.individualsInStates[i] = new Hashtable<Integer, Integer>();
		}
	}
	
	
	// Getting Methods
	public boolean checkIfIndividualSampled(int id){
		boolean result = false;
		
		if(this.individuals[id].getTimeStepSampled() != -1){
			result = true;
		}
		
		return result;
	}
	
	public double[] getSourceWeights(){
		return this.sourceWeights;
	}
	
	public double getForceOfInfection(){
		return this.forceOfInfection;
	}
	
	public double[] getStateSourceWeights(int stateIndex){
		return this.stateSourceWeights[stateIndex];
	}
	
	public double getStateForceOfInfection(int stateIndex){
		return this.stateForceOfInfection[stateIndex];
	}
	
 	public Individual[] getIndividuals(){
		
 		// Initialise an array to store the used positions of the individuals array
		Individual[] fullPositions = new Individual[this.lastIndividualsIndexUsed + 1];
		
		// Store each of the individuals in a used position of the individuals array
		for(int i = 0; i < this.lastIndividualsIndexUsed + 1; i++){
			fullPositions[i] = this.individuals[i];
		}
		
		return fullPositions;
	}

	public Individual getIndividual(int index){
		return this.individuals[index];
	}
	
	public int[] getIndicesOfIndividualsInCompartment(int index){
		
		return HashtableMethods.getKeysInt(this.compartments[index]);
	}
	
	public int getNumberOfIndividualsWithState(int state){
		return this.individualsInStates[state].size();
	}
	
 	public int[] getIndicesOfIndividualsInCompartment(int status, int state){
		
		// Get all the Ids of the individuals with the infection status of interest
		Set<Integer> keys = this.compartments[status].keySet();
		int[] ids = new int[this.compartments[status].size()];
		
		int pos = -1;
		for(int key : keys){
			
			// Check if the current individual has the state of interest
			if(getIndividualsState(key) == state){
				pos++;
				ids[pos] = key;
			}			
		}		
		
		return ArrayMethods.subset(ids, 0, pos);
	}
	
	public int getNumberOfIndividualsInCompartment(int index){
		return this.compartments[index].size();
	}
	
	public int getSize(){
		return this.lastIndividualsIndexUsed + 1;
	}
	
	public int[] getIndividualsMutationEvents(int id){
		return this.individuals[id].getMutationEvents();
	}

	public int getIndividualsInfectionStatus(int id){
		return this.individuals[id].getInfectionStatus();
	}

	public int getNumberSampled(){
		return sampled.size();
	}
	
	public int[] getIdsOfSampledIndividuals(){
		return HashtableMethods.getKeysInt(this.sampled);
	}
	
	public char[] getIndividualsSequence(int id){
		return this.individuals[id].getSequence();
	}
	
	public int getNumberInfectionStatuses(){
		return this.compartments.length;
	}
	
	public int getTimeStepIndividualSampledIn(int id){
		return this.individuals[id].getTimeStepSampled();
	}
	
	public Hashtable<Integer, Integer> getSampled(){
		return this.sampled;
	}
	
	public int[] getIndividualsStatusChanges(int id){
		return this.individuals[id].getInfectionStatusChanges();
	}

	public Hashtable<Integer, Integer> getIndividualsMutationEventHashtable(int id){
		return this.individuals[id].getMutationEventsHashtable();
	}
	
	public Hashtable<Integer, IntArray> getIndividualsGenomeSiteInfo(int id, int[][] mutationEventInfo, int sizeLimit){
		return this.individuals[id].getGenomeSiteInfo(mutationEventInfo, sizeLimit);
	}
	
	public int getIndividualsLastInfecteeIndex(int id){
		return this.individuals[id].getLastInfecteeIndex();
	}
	
	public int[] getIndividualsInfectees(int id){
		return this.individuals[id].getInfectees();
	}
	
 	public int getIndividualsState(int id){
		return this.individuals[id].getState();
	}
	
	public int checkIfIndividualHasMutationEvent(int id, int mutationEvent){
		return this.individuals[id].isMutationEventPresent(mutationEvent);
	}

	public int[] getInfectiousIndividualsInPopulation(double[] infectiousness){
		
		int[] idsOfInfectious = new int[0];
		
		for(int i = 0; i < infectiousness.length; i++){
			
			if(infectiousness[i] > 0){
				idsOfInfectious = ArrayMethods.combine(idsOfInfectious, getIndicesOfIndividualsInCompartment(i));
			}
		}
		
		return idsOfInfectious;
	}
}
