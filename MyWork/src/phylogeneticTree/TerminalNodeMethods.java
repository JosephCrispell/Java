package phylogeneticTree;

import java.util.Random;

import org.uncommons.maths.random.MersenneTwisterRNG;

import methods.ArrayMethods;

public class TerminalNodeMethods {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public static TerminalNode[] append(TerminalNode[] array, TerminalNode node){
		
		TerminalNode[] newArray = new TerminalNode[array.length + 1];
		
		for(int i = 0; i < array.length; i++){
			newArray[i] = array[i];
		}
		
		newArray[array.length] = node;
		
		return newArray;		
	}
	
	public static TerminalNode[] randomShuffleAll(TerminalNode[] array, Random random){
		TerminalNode[] newArray = new TerminalNode[array.length];
		
		// Create an Array of all the available indices in the new array
		int[] indices = ArrayMethods.seq(0, array.length - 1, 1);
		
		// Pick a random index for each of the Terminal nodes without replacement
		int index;
		for(TerminalNode node : array){
			
			// Randomly pick an index from the available indices
			index = ArrayMethods.randomChoice(indices, random);
			
			// Assign the terminal node
			newArray[index] = node;
			
			// Remove the index from the list of available indices
			indices = ArrayMethods.deleteElement(indices, index);
		}
		
		return newArray;
	}
}
