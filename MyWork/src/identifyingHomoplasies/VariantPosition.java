package identifyingHomoplasies;

import java.util.Hashtable;

import methods.HashtableMethods;

public class VariantPosition {

	public int position;
	public Hashtable<Character, Integer> alleleCounts;
	public int totalCounts;
	
	public VariantPosition(int pos, Hashtable<Character, Integer> counts){
    	this.position = pos;
		this.alleleCounts = counts;
		
		this.totalCounts = 0;
		for(char key : HashtableMethods.getKeysChar(this.alleleCounts)){
			this.totalCounts += this.alleleCounts.get(key);
		}
	}
	
	// Getting methods
	public int getPosition(){
		return this.position;
	}
	public int getAlleleCount(char allele){
		return this.alleleCounts.get(allele);
	}
	public double getAlleleSupport(char allele){
		
		return (double) this.alleleCounts.get(allele) / (double) this.totalCounts; 
	}
}
