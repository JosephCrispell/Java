package badgerPopulation;
import java.util.Random;

import methods.ArrayMethods;

import org.apache.commons.math3.random.MersenneTwister;

import filterSensitivity.DistanceMatrix;


public class BadgerMethods {

	public static void main(String[] args) {
		// Method Testing Area
		
		Badger[] population = new Badger[0];
		int noFemales = countSex(population, 'F', 1);
	}
    
	// Methods 
	public static Badger randomChoice( Badger[] array, Random random){
		
		int randomIndex = random.nextInt(array.length);
	
		return array[randomIndex];
	}

	public static int countSex(Badger[] array, char sex, int minAge){
		int count = 0;
		
		for(Badger badger : array){
			
			if(badger.getSex() == sex && badger.getAge() >= minAge){
				
				count++;
			}
		}
		
		return count;
	}
	public static int countFertilised(Badger[] array){
		int count = 0;
		for(Badger badger : array){
			if(badger.getSex() == 'F' && badger.getFertilisationStatus() == 'F'){
				
				count++;
			}
		}
		
		return count;
	}

	public static Badger[] addBadger(Badger[] array, Badger badger){
		
		Badger[] newArray = new Badger[array.length + 1];
			
		for(int index = 0; index < array.length; index++){
			newArray[index] = array[index];
		}
		newArray[newArray.length - 1] = badger;
		
		return newArray;
	}
	public static Badger[] addBadgers(Badger[] array, Badger[] badgers){
		
		Badger[] newArray = new Badger[array.length + badgers.length];
			
		for(int index = 0; index < array.length; index++){
			newArray[index] = array[index];
		}
		for(int index = 0; index < badgers.length; index++){
			newArray[index + array.length] = badgers[index];
		}
		
		return newArray;
	}

	public static int countAge(Badger[] array, int age, String condition){
		int count = 0;
		for(Badger badger : array){
			
			if(condition.equals("Older") && badger.getAge() > age){
				count++;
			}else if(condition.equals("Younger") && badger.getAge() < age){
				count++;
			}
		}
		
		return count;
	}

	public static Badger[] deleteBadger(Badger[] array, Badger badger){
		
		Badger[] newArray = new Badger[array.length - 1];
		
		int pos = -1;
		for(Badger x : array){
			if(x != badger){
				pos++;
				newArray[pos] = x;
			}
		}
		
		return newArray;
	}
	public static Badger[] deleteBadger(Badger[] array, int badgerIndex){
		
		Badger[] newArray = new Badger[array.length - 1];
		
		int pos = -1;
		for(int i = 0; i < array.length; i++){
			if(i != badgerIndex){
				pos++;
				newArray[pos] = array[i];
			}
		}
		
		return newArray;
	}

	public static Badger[] copy(Badger[] array){
		Badger[] copy = new Badger[array.length];
		
		for(int index = 0; index < array.length; index++){
			copy[index] = array[index];
		}
		
		return copy;
	}

	public static Badger[] flatten(Badger[][] population){
		Badger[] badgers = new Badger[9999];
		
		// Put all badgers in large Array
		int pos = -1;
		for(Badger[] group : population){
			for(Badger badger : group){
				pos++;
				badgers[pos] = badger;
			}
		}
		
		// Subset large array to select used positions
		return subset(badgers, 0, pos);
	}
	
	public static Badger[] subset(Badger[] array, int start, int end){
		Badger[] part = new Badger[end - start + 1];
		
		int pos = -1;
		for(int index = 0; index < array.length; index++){
			
			if(index >= start && index <= end){
				pos++;
				part[pos] = array[index];
			}
		}
		
		return part;
	}

	// Sequence Methods
    public static int[] mutateSequence(double mutationRate, int nSeasons, int[] mutationEvents, Random random){
    	
    	// Have any mutations occurred?
    	for(int i = 0; i < nSeasons; i++){
    		
    		// Did a mutation occur this time?
    		if(random.nextDouble() < mutationRate){
    			
    			// Store this new Mutation Event - Recorded as when it came
    			Global.mutationEventNo++;
    			mutationEvents = ArrayMethods.append(mutationEvents, Global.mutationEventNo);
    		}
    	}
    	
    	return mutationEvents;
    }

    public static DistanceMatrix buildDistanceMatrix(Badger[] sampledBadgers){
    	
    	String[] sampleNames = new String[sampledBadgers.length];
    	double[][] d = new double[sampledBadgers.length][sampledBadgers.length];
    	
    	for(int i = 0; i < sampledBadgers.length; i++){
    		
    		// Build the Sample Names
    		sampleNames[i] = Integer.toString(sampledBadgers[i].getBadgerId());
    		
    		for(int j = 0; j < sampledBadgers.length; j++){
    			
    			if(d[i][j] != 0 && i != j){
    			
    				// Compare the two Sequences of Mutation Events
    				double pDistance = calculateDistance(sampledBadgers[i].getMutationEvents(), sampledBadgers[i].getMutationEvents());
    			    			
    				// Store the Distance
    				d[i][j] = pDistance;
    				d[j][i] = pDistance;
    			}
    		}
    	}
    	
    	return new DistanceMatrix(sampleNames, d);
    }
    
    public static double calculateDistance(int[] a, int[] b){

    	/**
    	 * Method to Compare two Sequences of Mutation Events
    	 * 	Mutation Event Sequences Record which of the Mutations (which occurred over the course
    	 * 	of the simulation occurred in the pathogen carried by the current badger.
    	 * 
    	 * 	Example:
    	 * 		SequenceA = {1,	2,	3,	5,	7,	8,		13}
    	 * 		SequenceB = {1,	2,			7,		9};
    	 * 
    	 * 	noShared = 3
    	 * 	noDifferences = (A.length - noShared) + (B.length - noShared)
    	 * 	noDifferences = (7 - 3) + (4 - 3)
    	 * 
    	 *	noDifferences = 5
    	 *
    	 *	Number of Differences is used to define the genetic distance between two samples.
    	 */
    	
    	// Find the number of Shared Events
    	double noShared = 0;
    	for(int event : a){
    		if(ArrayMethods.found(b, event) == 1){
    			noShared++;
    		}
    	}
    	
    	// Return the number of Differences
    	return (a.length - noShared) + (b.length - noShared);
    }

    public static int calculatePopulationSize(Badger[][] population){
    	
    	int popSize = 0;
    	for(Badger[] group : population){
    		for(Badger badger : group){
    			popSize++;
    		}
    	}
    	
    	return popSize;
    }
}
