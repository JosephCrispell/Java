package woodchesterBadgers;

import java.io.IOException;
import java.util.Calendar;
import java.util.Hashtable;

import methods.HashtableMethods;

public class DynamicGroupContactNetwork {

	public static int[][][] adjacencyMatrices;
	public static int[] window;
	public static Hashtable<String, Integer> groupIndices;
	public static String[] orderedGroups;
	
	public DynamicGroupContactNetwork(int[][][] matrices, int[] range, Hashtable<String, Integer> indexedGroups){

		this.adjacencyMatrices = matrices;
		this.window = range;
		this.groupIndices = indexedGroups;
		this.orderedGroups = orderGroupsByTheirIndex(indexedGroups);
	}

	// Setting Methods
	public void setAdjacencyMatrices(int[][][] matrices){
		this.adjacencyMatrices = matrices;
	}
	public void setWindow(int[] range){
		this.window = range;
	}
	public void setGroupIndices(Hashtable<String, Integer> indexedGroups){
		this.groupIndices = indexedGroups;
		this.orderedGroups = orderGroupsByTheirIndex(indexedGroups);
	}
	
	// Getting Methods
	public int[][][] getAdjacencyMatrices(){
		return this.adjacencyMatrices;
	}
	public int[] getWindow(){
		return this.window;
	}
	public Hashtable<String, Integer> getGroupIndices(){
		return this.groupIndices;
	}
	public String[] getOrderedGroups(){
		return this.orderedGroups;
	}
	
	// General Methods
	public static String[] orderGroupsByTheirIndex(Hashtable<String, Integer> groupIndices){
		
		// Get the Group names from the hashtable
		String[] names = HashtableMethods.getKeysString(groupIndices);
		
		// Initialise an array to store the order group names
		String[] orderedNames = new String[names.length];
		
		// Insert each of the groups into its position in the odered list
		for(String group : names){
			
			orderedNames[groupIndices.get(group)] = group;
		}
		
		return orderedNames;
	}
}
