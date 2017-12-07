package identifyingHomoplasies;

import java.util.Hashtable;

import methods.ArrayMethods;
import methods.HashtableMethods;

public class Clusters {

	public Hashtable<String, Integer> isolateClusters = new Hashtable<String, Integer>();
	public Hashtable<Integer, String[]> clusterIsolates = new Hashtable<Integer, String[]>();
	public int clusterID = -1;
	
	public Clusters(int[][] geneticDistances, String[] isolates, int distance){
		
		// Define the clusters
		defineClusters(geneticDistances, isolates, distance);
	}

	// Setting methods
	public void setIsolateClusters(Hashtable<String, Integer> isolateClusters) {
		this.isolateClusters = isolateClusters;
	}
	public void setClusterIsolates(Hashtable<Integer, String[]> clusterIsolates) {
		this.clusterIsolates = clusterIsolates;
	}
	
	// Getting methods
	public Hashtable<String, Integer> getIsolateClusters() {
		return isolateClusters;
	}
	public Hashtable<Integer, String[]> getClusterIsolates() {
		return clusterIsolates;
	}

	// General methods
	public void defineClusters(int[][] geneticDistances, String[] isolates, int distance){
		
		// Examine the distances within the genetic distance matrix
		for(int i = 0; i < geneticDistances.length; i++){
			for(int j = 0; j < geneticDistances[0].length; j++){
				
				// Ignore diagonal
				if(i == j){
					continue;
				}
				
				// Compare the current pair of isolates - are <= the thresold distance apart?
				if(geneticDistances[i][j] <= distance){
					
					// Has isolate I already been assigned to a cluster and J not?
					if(checkIfIsolateAssignedToCluster(isolates[i]) == true && 
							checkIfIsolateAssignedToCluster(isolates[j]) == false){
						
						// Assign isolate J to isolate I's cluster
						assignIsolateToCluster(isolates[j], getIsolatesCluster(isolates[i]));
					
					// Has isolate J already been assigned to a cluster and I not?
					}else if(checkIfIsolateAssignedToCluster(isolates[j]) == true && 
							checkIfIsolateAssignedToCluster(isolates[i]) == false){
						
						// Assign isolate I to isolate J's cluster
						assignIsolateToCluster(isolates[i], getIsolatesCluster(isolates[j]));
						
					// Has neither been added to a cluster?
					}else if(checkIfIsolateAssignedToCluster(isolates[i]) == false && 
							checkIfIsolateAssignedToCluster(isolates[j]) == false){
						
						// Create a cluster and assign both isolates to it
						assignIsolatesToNewCluster(isolates[i],  isolates[j]);
						
					// Has both been added to a cluster that isn't the same cluster?	
					}else if(getIsolatesCluster(isolates[i]) != getIsolatesCluster(isolates[j])){
						
						// Merge the clusters
						mergeClusters(getIsolatesCluster(isolates[i]), getIsolatesCluster(isolates[j]));
					}
				}
			}
		}
	}
	public void assignIsolateToCluster(String isolate, int cluster){
		this.isolateClusters.put(isolate, cluster);
		clusterIsolates.put(cluster, ArrayMethods.append(this.clusterIsolates.get(cluster), isolate));
	}
	public void assignIsolatesToNewCluster(String isolateA, String isolateB){
		this.clusterID++;
		this.isolateClusters.put(isolateA, this.clusterID);
		this.isolateClusters.put(isolateB, this.clusterID);
		
		String[] isolates = new String[2];
		isolates[0] = isolateA;
		isolates[1] = isolateB;
		this.clusterIsolates.put(this.clusterID, isolates);
	}
	public void mergeClusters(int clusterA, int clusterB){
		
		// Create an array containing all the isolates from clusters A and B
		String[] isolates = ArrayMethods.combine(this.clusterIsolates.get(clusterA),
				this.clusterIsolates.get(clusterB));
				
		// Assign those isolates to cluster A and delete cluster B
		this.clusterIsolates.put(clusterA, isolates);
		this.clusterIsolates.remove(clusterB);
				
		// Update the clusters associated with each isolates
		for(String isolate : isolates){
			this.isolateClusters.put(isolate, clusterA);
		}
	}
	public String[] getIsolatesInCluster(int cluster){
		return this.clusterIsolates.get(cluster);
	}
	public int getIsolatesCluster(String isolate){
		return this.isolateClusters.get(isolate);
	}
	public boolean checkIfIsolateAssignedToCluster(String isolate){
		return this.isolateClusters.get(isolate) != null;
	}
	public int[] getClusters(){
		return HashtableMethods.getKeysInt(this.clusterIsolates);
	}
	public void printClusters(){
		
		for(int key : HashtableMethods.getKeysInt(this.clusterIsolates)){
			System.out.println(key + "\t" + ArrayMethods.toString(this.clusterIsolates.get(key), ", "));
		}
	}
}
