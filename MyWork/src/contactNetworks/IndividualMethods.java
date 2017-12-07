package contactNetworks;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Set;

import org.apache.commons.math3.random.MersenneTwister;

import methods.ArrayMethods;
import methods.MatrixMethods;
import methods.WriteToFile;

import filterSensitivity.DistanceMatrix;

public class IndividualMethods {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		double transitionProb = 0.7;
		double[] flattenedTransitionMatrix = generateTransitionMatrix(transitionProb);
		
		System.out.println(Arrays.toString(flattenedTransitionMatrix) + "\n");
		
		int[] mutationEvents = {1,2,5,6,8};
		int lastMutationEvent = 10;
		
		Hashtable<Integer, char[]> mutationEventInfo = defineEachMutationEvent(flattenedTransitionMatrix, lastMutationEvent);
		
		Set<Integer> keys = mutationEventInfo.keySet();
		for(int key : keys){
			System.out.println(key + "\t" + Arrays.toString(mutationEventInfo.get(key)));
		}
		
		char[] nucleotideSequence = convertMutationEventsSequence2NucleotideSequence(mutationEvents, mutationEventInfo, lastMutationEvent);
		
		System.out.println("\n" + Arrays.toString(mutationEvents) + "\n" + Arrays.toString(nucleotideSequence));
		
		System.out.println();
		System.out.println();
		
	}
	
	public static void generateFastaFile4SampleMutationEventSequences(double transitionProb, String fastaFile, 
			String traitsFile, int lastMutationEvent, Sample[] samples) throws IOException{
		
		/**
		 * Method to Convert Mutation Event Sequences into Nucleotide Sequences
		 * 	Mutation Event Sequence: 1,2,3,5,6
		 * 		Hash	Ref	Alt
		 * 		1		A	G
		 * 		2		C	T
		 * 		3		G	A
		 * 		4		T	G
		 * 		5		C	T
		 * 		6		T	C
		 * 
		 *  Nucleotide Sequence: G, T, A, T, T, C
		 * 
		 * 	Transition Matrix:
		 * 			A	G	T	C
		 * 		A	-	ts	tv	tv
		 * 		G	ts	-	tv	tv
		 * 		T	tv	tv	-	ts
		 * 		C	tv	tv	ts	-
		 * 
		 * 	Probability of Transversion = 1 - Probability of Transition
		 * 		
		 */
		
		// Open and Wipe Fasta and Traits Files
		BufferedWriter fasta = WriteToFile.openFile(fastaFile, false);
		BufferedWriter traits = WriteToFile.openFile(traitsFile, false);
		
		// Create the Transition Matrix
		double[] flattenedTransitionMatrix = generateTransitionMatrix(transitionProb);
		
		// Generate a Mutation Event Hashtable
		Hashtable<Integer, char[]> mutationEventsInfo = defineEachMutationEvent(flattenedTransitionMatrix, lastMutationEvent);
		
				
		/**
		 * Print Nexus Header
		 * 	Nexus file format:
		 * 		#NEXUS
		 *
		 *
		 *		BEGIN DATA;
		 *		DIMENSIONS NTAX=3 NCHAR=20;
		 *		FORMAT MISSING=N GAP=- DATATYPE=DNA;
		 *		MATRIX
		 *		AgR111_1987       ATGCGATGCGTAGGAGTAGG
		 *		AgR121_1988       ATGCGATGCGTAGGNGTAGG
		 *		AgR122_1992       ATGCGATGCGNAGGAGTAGG
		 *		;
		 *		END;
		 */
		WriteToFile.writeLn(fasta, "#NEXUS\n\n\nBEGIN DATA;\nDIMENSIONS NTAX=" + samples.length + " NCHAR=" + (lastMutationEvent+1)
				+ "\nFORMAT MISSING=N GAP=- DATATYPE=DNA;\nMATRIX");
		
		/**
		 * Print Traits file Header:
		 * 	traits	Year	Latitude	Longitude
		 */
		WriteToFile.writeLn(traits, "traits\tYear\tLatitude\tLongitude\n");
		
		// Print the Sample Sequences and Information
		for(Sample sample : samples){
			
			char[] nucleotideSequence = convertMutationEventsSequence2NucleotideSequence(sample.getMutationEventSequence(), mutationEventsInfo, lastMutationEvent);
			
			String sampleName = "Sample_" + sample.getSampledIndividual().getId() + "_" + sample.getTimestepSampled();
			double[] latLongs = sample.getLatLongs();
			
			WriteToFile.write(fasta, sampleName + "\t");
			printSequence(fasta, nucleotideSequence);
			
			WriteToFile.writeLn(traits, sampleName + "\t" + sample.getTimestepSampled() + "\t" + latLongs[0] + "\t" + latLongs[1]);
			
		}
		WriteToFile.writeLn(fasta, ";\nEND;\n");
		
		WriteToFile.close(fasta);
		WriteToFile.close(traits);
	}
	
	public static void printSequence(BufferedWriter bWriter, char[] sequence) throws IOException{
		
		String seq = "";
		for(int character : sequence){
			seq = seq + character;
		}
		
		WriteToFile.writeLn(bWriter, seq);
		
	}
	
	public static double[] generateTransitionMatrix(double tsProb){
		double tvProb = 1 - tsProb;			// A		G		T		C
		double[] flattenedTransitionMatrix = { 0,		tsProb,	tvProb,	tvProb,
											 tsProb,	0,		tvProb,	tvProb,
											 tvProb,	tvProb,	0,		tsProb,
											 tvProb,	tvProb,	tsProb,	0	};
		
		return flattenedTransitionMatrix;
	}
	
	public static char[] convertMutationEventsSequence2NucleotideSequence(int[] mutationEvents, 
			Hashtable<Integer, char[]> mutationEventInfo, int lastMutationEvent){
		
		// Initialise an Empty array to store the nucleotide sequence
		char[] nucleotideSequence = new char[lastMutationEvent + 1];
		
		// Initialise counter to move through the Mutation Events Array
		int pos = -1;
		int finished = 0;
		
		// Loop through each Mutation Event
		for(int i = 0; i <= lastMutationEvent; i++){
			
			// Check Mutation Event is present in Mutation Events Sequence
			if(finished == 0 && i == mutationEvents[pos + 1]){
				
				// Move one position along in the Mutation Events Sequence
				pos++;
				
				// Insert the Alternate Allele
				nucleotideSequence[i] = mutationEventInfo.get(i)[1];
				
				if(pos + 1 == mutationEvents.length){
					finished = 1;
				}
				
			}else{
				
				// Insert the Reference Allele
				nucleotideSequence[i] = mutationEventInfo.get(i)[0];
			}
		}
		
		return nucleotideSequence;
		
	}
	
	public static char[] createMutationEvent(double[] flattenedTransitionMatrix){
		
		/**
		 * Method to Define a mutation event: Reference -> Alternate
		 * 	
		 * 	Transition Matrix:
		 * 			A	G	T	C
		 * 		A	-	ts	tv	tv
		 * 		G	ts	-	tv	tv
		 * 		T	tv	tv	-	ts
		 * 		C	tv	tv	ts	-
		 * 
		 */
		
		// Create an array of possible Mutation Events
		char[][] possibleEvents = { 	{'A', 'A'}, {'A', 'G'}, {'A', 'C'}, {'A', 'T'},
										{'G', 'A'}, {'G', 'G'}, {'G', 'C'}, {'G', 'T'},
										{'C', 'A'}, {'C', 'G'}, {'C', 'C'}, {'C', 'T'},
										{'T', 'A'}, {'T', 'G'}, {'T', 'C'}, {'T', 'T'}	};
		
		
		// Randomly Choose a Mutation Event realisation
		return ArrayMethods.randomWeightedChoice(possibleEvents, flattenedTransitionMatrix);
		
	}
	
	public static Hashtable<Integer, char[]> defineEachMutationEvent(double[] flattenedTransitionMatrix, int lastMutationEvent){
		Hashtable<Integer, char[]> mutationEvents = new Hashtable<Integer, char[]>();
		for(int i = 0; i <= lastMutationEvent; i++){
			
			// Define the Mutation Event: Reference -> Alternate
			mutationEvents.put(i, createMutationEvent(flattenedTransitionMatrix));
		}
		
		return mutationEvents;
	}
	
    public static double[][] buildGeneticDistanceMatrix(Individual[] individuals){
    	
    	// Initialise information for the Distance Matrix
    	String[] sampleNames = new String[individuals.length];
    	double[][] d = new double[individuals.length][individuals.length];
    	
    	// Record each comparison - genetic distance matrix is symmetric
    	int[][] examined = new int[individuals.length][individuals.length];
    	
    	for(int i = 0; i < individuals.length; i++){
    		
    		// Build the Sample Names
    		sampleNames[i] = Integer.toString(individuals[i].getId());
    		
    		for(int j = 0; j < individuals.length; j++){
    			
    			if(examined[i][j] == 0 && i != j){
    			
    				// Compare the two Sequences of Mutation Events
    				double pDistance = calculateDistance(individuals[i].getMutationEvents(), individuals[j].getMutationEvents());
    			    			
    				// Store the Distance
    				d[i][j] = pDistance;
    				d[j][i] = pDistance;
    				
    				// Record the Comparison
    				examined[i][j] = 1;
    				examined[j][i] = 1;
    			}
    		}
    	}
    	
    	return d;
    }

    public static double[][] buildSpatialDistanceMatrix(Individual[] sampled, double[][] spatialDistanceMatrix){
    	
    	/**
    	 * Method to create a Spatial Distance Matrix specific to the Sampled population
    	 * Each Sampled individual's index is recorded - this should be constant throughout the course of the
    	 * simulation for the static network.
    	 */
    	
    	// Initialise Distance Matrix
    	double[][] d = new double[sampled.length][sampled.length];
    	
    	// Compare each of the Sampled Individuals
    	for(int i = 0; i < sampled.length; i++){
    		int iIndex = sampled[i].getIndex();
    		
    		for(int j = 0; j < sampled.length; j++){
    			int jIndex = sampled[j].getIndex();
    			
    			d[i][j] = spatialDistanceMatrix[iIndex][jIndex];
    		}
    	}
    	
    	return d;    	
    }

    public static String[] convertIndividualIds(Individual[] sampled){
    	
    	// Storing the Individual's Ids as Strings
    	String[] ids = new String[sampled.length];
    	
    	for(int i = 0; i < sampled.length; i++){
    		ids[i] = Integer.toString(sampled[i].getId());
    	}
    	
    	return ids;
    }
    
    public static int find(Individual[] individuals, Individual individual){
    	
    	int pos = -1;
    	for(Individual x : individuals){
    		pos++;
    		if(x == individual){
    			break;
    		}
    	}
    	
    	return pos;
    }

    public static int found(Individual[] individuals, Individual individual){
    	int found = 0;
    	for(Individual x : individuals){
    		if(x == individual){
    			found = 1;
    			break;
    		}
    	}
    	
    	return found;
    }
    
    public static Individual[] subset(Individual[] array, int start, int end){
		Individual[] part = new Individual[end - start + 1];
		
		int pos = -1;
		for(int index = 0; index < array.length; index++){
			
			if(index >= start && index <= end){
				pos++;
				part[pos] = array[index];
			}
		}
		
		return part;
	}

	public static Individual randomWeightedChoice( Individual[] array, double[] weights ){
		
		/** Convert Integer Weights into Weights which can be used
		 * (1, 2, 4, 6, 4, 2, 1) => 1/20 = 0.05 => multiply and sum => (0.05, 0.15, 0.35, 0.65, 0.85, 0.95, 1)
		 * Previous and Current Define bounds in Bin e.g. 0.05 <= y < 0.15
		 */
		
		// Create Instance of a Random Number Generator
		MersenneTwister random = new MersenneTwister();
		
		double value = 0;
		for(double weight : weights){
			value += weight;
		}
	
		value = 1 / value; // Value by which to multiply each weight
		double[] actualWeights = new double[array.length];
		
		double previous = 0;
		for(int index = 0; index < array.length; index++){
			
			double calculatedWeight  = (weights[index] * value) + previous; // Weights move towards 1
			actualWeights[index] = calculatedWeight;
			previous = calculatedWeight;
		
		}
	
		double y = random.nextDouble();
		int pos = 99;
		for(int index = 0; index < array.length; index++){
		
			if(y < actualWeights[index]){
				pos = index;
				break;
			}
		}
	
		return array[pos];
	}

	public static int[] mutateSequence(double mutationRate, int timeStep, int[] mutationEvents, int timeLastChecked){
	    	
	   	// Create Instance of a Random Number Generator
	   	MersenneTwister random = new MersenneTwister();
	    	
	   	// When was the infected Individual last checked?
	   	int times = timeStep - timeLastChecked;
	    	
	   	// Have any mutations occurred?
	   	for(int i = 0; i < times; i++){
	    		
	   		// Did a mutation occur this time?
	   		if(random.nextDouble() < mutationRate){
	    			
	   			// Store this new Mutation Event - Recorded as when it came
	   			Global.mutationEventNo++;
	   			mutationEvents = ArrayMethods.append(mutationEvents, Global.mutationEventNo);
	   		}
	    }
	    	
	    return mutationEvents;
	}

	public static int[] mutateSequenceNew(double[] mutationRates, int timeStep, Individual individual){
    	
		// Retrieve the Mutation Event Sequence
		int[] mutationEvents = individual.getMutationEvents();
		double mutationRate = mutationRates[individual.getStatusIndex()];
		int timeLastChecked = individual.getMutationsLastChecked();
		int noMutationsSinceTransmission = individual.getNoMutationsSinceTransmission();
		
	   	// Create Instance of a Random Number Generator
	   	MersenneTwister random = new MersenneTwister();
	    	
	   	// When was the infected Individual last checked?
	   	int times = timeStep - timeLastChecked;
	    	
	   	// Have any mutations occurred?
	   	for(int i = 0; i < times; i++){
	    		
	   		// Did a mutation occur this time?
	   		if(random.nextDouble() < mutationRate){
	    			
	   			// Store this new Mutation Event - Recorded as when it came
	   			Global.mutationEventNo++;
	   			noMutationsSinceTransmission++;
	   			mutationEvents = ArrayMethods.append(mutationEvents, Global.mutationEventNo);
	   		}
	    }
	   	
	   	individual.setNoMutationsSinceTransmission(noMutationsSinceTransmission);
	    	
	    return mutationEvents;
	}
	
    public static double calculateDistance(int[] a, int[] b){

    	/**
    	 * Method to Compare two Sequences of Mutation Events
    	 * 	Mutation Event Sequences Record which of the Mutations (which occurred over the course
    	 * 	of the simulation occurred in the pathogen carried by the current individual.
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

	public static Individual randomChoice( Individual[] array ){
		
		int randomIndex = new MersenneTwister().nextInt(array.length);
		
		return array[randomIndex];
	}

	public static Individual[] findInfectiousIndividuals( Individual[] population, double[] infectiousness){
		
		Individual[] infectious = new Individual[population.length];
		int pos = -1;
		for(Individual individual : population){
			if(infectiousness[individual.getStatusIndex()] > 0){
				pos++;
				infectious[pos] = individual;
			}
		}
		
		return subset(infectious, 0, pos);
	}

	public static Individual[] append(Individual[] array, Individual individual){
		Individual[] newArray = new Individual[array.length + 1];
		
		for(int index = 0; index < array.length; index++){
			newArray[index] = array[index];
		}
		newArray[newArray.length - 1] = individual;
		
		return newArray;
	}

	public static Individual[] copy(Individual[] array){
		Individual[] copy = new Individual[array.length];
		
		for(int index = 0; index < array.length; index++){
			copy[index] = array[index];
		}
		
		return copy;
	}

	public static Individual[] deleteElement(Individual[] array, Individual element){
		
		// This method will remove all the occurrences of this element
		int count = 0;
		for(Individual x : array){
			if(element == x){
				count++;
			}
		}
		
		Individual[] newArray = new Individual[array.length - count];
		
		int pos = -1;
		for(Individual x : array){
			if(x != element){
				pos++;
				newArray[pos] = x;
			}
		}
		
		return newArray;
	}

	public static Individual[] selectStatus(Individual[] array, int statusIndex){
		
		Individual[] selected = new Individual[array.length];
		
		int pos = -1;
		for(int i = 0; i < array.length; i++){
			if(array[i].getStatusIndex() == statusIndex){
				pos++;
				
				selected[pos] = array[i];
			}
		}
		
		return subset(selected, 0, pos);
	}
}
