package contactNetworks;
import java.util.Arrays;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import methods.MatrixMethods;
import methods.ArrayMethods;

public class NetworkMethods {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
			
	}
	
	public static Network createCompleteNetwork(Individual[] population){
		
		// Record the contacts of each individual in the population
		for(int i = 0; i < population.length; i++){
			population[i].setContacts(population);
			population[i].setIndex(i);
		}
		
		// Build the adjacency matrix
		int[][] adjacency = new int[population.length][population.length];
		for(int i = 0; i < population.length; i++){
			for(int j = 0; j < population.length; j++){
				if(i != j){
					adjacency[i][j] = 1;
				}
			}
		}
		
		return new Network(population, adjacency);
	}

	public static Network createGridNetwork(Individual[] population){
		/**
		 * A Network is built based upon a grid structure. The resulting contact network is recorded in each individuals
		 * list of contacts. Needs to be a Square Matrix - i.e. any Square: 4, 9, 16, 25, 36, 49, 64, 81
		 * 
		 * Network is also recorded as an adjacency Matrix.
		 * 
		 * Note that those individuals found on the top, bottom, left, and right are recognised to avoid torus
		 * structure.
		 */
		
		int popSize = population.length;
		
		// Initialise the Adjacency Matrix
		int[][] adjacency = new int[popSize][popSize];
		
		// Square Root Must be a whole number
		int rowLength = (int) Math.sqrt(popSize);
		
		// Individuals on Left Side
		int[] leftIndividuals = ArrayMethods.range(0, popSize, rowLength);
				
		// Individuals on Right Side
		int[] rightIndividuals = ArrayMethods.range(rowLength - 1, popSize, rowLength);
		
		// Build Network
		for(int i = 0; i < population.length; i++){
		
			/**
			 *  Use the individuals ID also as their index in the population (id - 1). 
			 *  NOTE: Assumes order hasn't changed since initialisation
			 */
						
			// Initialise contacts List
			Individual[] contacts = new Individual[0];
			
			// Above
			if(i - rowLength >= 0){
				contacts = IndividualMethods.append(contacts, population[i - rowLength]);
				adjacency[i][i - rowLength] = 1;
			}
			
			// Below
			if(i + rowLength <= popSize - 1){
				contacts = IndividualMethods.append(contacts, population[i + rowLength]);
				adjacency[i][i + rowLength] = 1;
			}
			
			// Left
			if(i - 1 >= 0 && ArrayMethods.found(leftIndividuals, i) == 0){
				contacts = IndividualMethods.append(contacts, population[i - 1]);
				adjacency[i][i - 1] = 1;
			}
			
			// Right
			if(i + 1 <= popSize - 1 && ArrayMethods.found(rightIndividuals, i) == 0){
				contacts = IndividualMethods.append(contacts, population[i + 1]);
				adjacency[i][i + 1] = 1;
			}
			
			population[i].setContacts(contacts);
			population[i].setIndex(i);
		}
		
		return new Network(population, adjacency);
	}

	public static Network createGroupedNetwork(Individual[] population, int noGroups, int noGroupConnections){
		
		/**
		 *  Create an Adjacency Matrix to Represent the Network
		 *  A set number of groups are created of a given size, individuals within which are connected to one another. 
		 *  Groups are connected to a set number of other groups by random selection of pairs. 
		 */
				
		// Initialise Adjacency Matrix
		int groupSize = population.length / noGroups;
		int[][] adjacency = new int[population.length][population.length];
		
		// Note Group Indexes and Individuals in each group
		int[] groupIds = ArrayMethods.range(0, noGroups - 1, 1);
		int[][] groups = new int[noGroups][groupSize];
		
		// Create Each Group
		int start = 0;
		int end = groupSize - 1;
		for(int x = 0; x < noGroups; x++){
			
			int pos = -1;
			for(int i = start; i <= end; i++){
				pos++;
				
				// Note the individuals in each group
				groups[x][pos] = i;
				
				// Connect Individual to all other members of group
				for(int j = start; j <= end; j++){
					
					if( i != j && adjacency[i][j] == 0){
						adjacency[i][j] = 1;
						adjacency[j][i] = 1;
					}
				}
			}
			
			// Move to next group
			start = start + groupSize;
			end = end + groupSize;
			
		}
		
		// Randomly Connect Groups
		for(int i = 0; i < noGroups; i++){
			
			int[] unconnectedGroups = ArrayMethods.deleteElement(ArrayMethods.copy(groupIds), i);
			for(int x = 0; x < noGroupConnections; x++){
				
				// Select Group to connect to
				int groupIndex = ArrayMethods.randomChoice(unconnectedGroups);
				
				// Select Pair to link groups
				int a = ArrayMethods.randomChoice(groups[i]);
				int b = ArrayMethods.randomChoice(groups[groupIndex]);
				
				// Record Link
				adjacency[a][b] = 1;
				adjacency[b][a] = 1;				
				
				// Note which Group chosen
				unconnectedGroups = ArrayMethods.deleteElement(unconnectedGroups, groupIndex);
			}
			
		}
		
		
		// Use Adjacency Matrix to Record each Individual's contacts within the network
		for(int i = 0; i < adjacency.length; i++){
			
			population[i].setIndex(i);
			
			for(int j = 0; j < adjacency.length; j++){
				
				if(adjacency[i][j] == 1){
					
					// Extract the Current Individuals Neighbours and Append the Current contact to this list
					Individual[] neighbours = population[i].getContacts();
					neighbours = IndividualMethods.append(neighbours, population[j]);
					population[i].setContacts(neighbours);
					
				}
			}
			
		}
		
		//MatrixMethods.print(adjacency);
		
		return new Network(population, adjacency);
	}

	public static Network createLine(Individual[] population){
		/**
		 * Network is just a simple line 1 -- 2 -- 3 -- 4
		 * Recorded as the contacts of each individual and in an adjacency matrix
		 */
		
		int[][] adjacency = new int[population.length][population.length];
		
		for(int i = 0; i < population.length; i++){
			
			population[i].setIndex(i);
			
			// Get Current Individual's Contacts
			Individual[] contacts = population[i].getContacts();
			
			// Left 
			if(i - 1 >= 0){
				adjacency[i][i - 1] = 1;
				contacts = IndividualMethods.append(contacts, population[i - 1]);
			}
			
			// Right
			if(i + 1 < population.length){
				adjacency[i][i + 1] = 1;
				contacts = IndividualMethods.append(contacts, population[i + 1]);
			}
			
			// Update Current Individual's Contacts
			population[i].setContacts(contacts);
		}
		
		return new Network(population, adjacency);
	}
}
