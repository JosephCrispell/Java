package VNTR10;

import java.util.Hashtable;

public class Distances {

	public Hashtable<String, Integer> isolateIds;
	public double[][] spatialDistances;
	public int[][] temporalDistances;
	
	public Distances(String[] ids, double[][] spatial, int[][] temporal){
		
		indexIds(ids);
		this.spatialDistances = spatial;
		this.temporalDistances = temporal;
	}
	
	// Getting methods
	public double getSpatialDistanceBetweenTwoIsolates(String a, String b){
		
		return spatialDistances[isolateIds.get(a)][isolateIds.get(b)];
	}
	
	public int getTemproalDistanceBetweenTwoIsolates(String a, String b){
		return temporalDistances[isolateIds.get(a)][isolateIds.get(b)];
	}
	
	// General methods
	public void indexIds(String[] ids){
		this.isolateIds = new Hashtable<String, Integer>();
		
		for(int i = 0; i < ids.length; i++){
			this.isolateIds.put(ids[i], i);
		}
	}
}
