package VNTR10;

import java.util.Hashtable;

public class Network {

	public Hashtable<String, Integer> herdIndices;
	public int[][] nMovementsBetweenHerds;
	
	public Network(Hashtable<String, Integer> indexedHerds){
		
		this.herdIndices = indexedHerds;
		this.nMovementsBetweenHerds = new int[this.herdIndices.size()][this.herdIndices.size()];
	}
	
	// Setting methods
	public void setNMovementsBetweenHerds(int[][] matrix){
		this.nMovementsBetweenHerds = matrix;
	}
	public void addMovementToCountOfMovementsBetweenHerds(String off, String on){
		this.nMovementsBetweenHerds[this.herdIndices.get(off)][this.herdIndices.get(on)]++;
	}
	
	// Getting methods
	public Hashtable<String, Integer> getHerdIndices(){
		return this.herdIndices;
	}
	public int getNMovementsBetweenHerds(String a, String b){
		return this.nMovementsBetweenHerds[this.herdIndices.get(a)][this.herdIndices.get(b)];
	}
	public int[][] getNMovementsBetweenHerds(){
		return this.nMovementsBetweenHerds;
	}
}
